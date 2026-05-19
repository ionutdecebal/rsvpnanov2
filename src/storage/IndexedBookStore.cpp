#include "storage/IndexedBookStore.h"

#include <SD_MMC.h>
#include <algorithm>

static_assert(sizeof(IndexedBookStore::Header) == 52, "RIDX header size changed");
static_assert(sizeof(IndexedBookStore::WordRecord) == 8, "RIDX word record size changed");
static_assert(sizeof(IndexedBookStore::ChapterRecord) == 72, "RIDX chapter record size changed");

bool IndexedBookStore::open(const String &indexPath, const String &dataPath,
                            const Header &header) {
  if (header.magic != kMagic || header.version != kVersion ||
      header.headerSize != sizeof(Header) || header.recordSize != sizeof(WordRecord) ||
      header.wordCount == 0) {
    return false;
  }

  File nextIndexFile = SD_MMC.open(indexPath, FILE_READ);
  if (!nextIndexFile || nextIndexFile.isDirectory()) {
    if (nextIndexFile) {
      nextIndexFile.close();
    }
    return false;
  }

  File nextDataFile = SD_MMC.open(dataPath, FILE_READ);
  if (!nextDataFile || nextDataFile.isDirectory()) {
    nextIndexFile.close();
    if (nextDataFile) {
      nextDataFile.close();
    }
    return false;
  }

  close();
  indexPath_ = indexPath;
  dataPath_ = dataPath;
  header_ = header;
  indexFile_ = nextIndexFile;
  dataFile_ = nextDataFile;
  cachedStart_ = static_cast<size_t>(-1);
  cachedCount_ = 0;
  cachedWords_.clear();
  return true;
}

void IndexedBookStore::close() {
  if (indexFile_) {
    indexFile_.close();
  }
  if (dataFile_) {
    dataFile_.close();
  }
  indexPath_ = "";
  dataPath_ = "";
  header_ = Header();
  cachedWords_.clear();
  cachedStart_ = static_cast<size_t>(-1);
  cachedCount_ = 0;
}

bool IndexedBookStore::isOpen() const {
  return indexFile_ && dataFile_ && header_.magic == kMagic && header_.wordCount > 0;
}

size_t IndexedBookStore::wordCount() const {
  return isOpen() ? static_cast<size_t>(header_.wordCount) : 0;
}

String IndexedBookStore::wordAt(size_t index) const {
  if (!isOpen() || index >= wordCount()) {
    return "";
  }

  if (cachedStart_ == static_cast<size_t>(-1) || index < cachedStart_ ||
      index >= cachedStart_ + cachedCount_) {
    if (!loadWordWindow(index)) {
      return "";
    }
  }

  return cachedWords_[index - cachedStart_];
}

void IndexedBookStore::prefetchAround(size_t index) const {
  if (!isOpen() || index >= wordCount()) {
    return;
  }
  if (cachedStart_ == static_cast<size_t>(-1) || index < cachedStart_ ||
      index >= cachedStart_ + cachedCount_) {
    (void)loadWordWindow(index);
  }
}

bool IndexedBookStore::readRecords(size_t startIndex, size_t count,
                                   std::vector<WordRecord> &records) const {
  records.clear();
  if (!isOpen() || count == 0 || startIndex >= wordCount()) {
    return false;
  }

  const size_t available = wordCount() - startIndex;
  count = std::min(count, available);
  records.resize(count);

  const uint32_t offset = header_.recordsOffset +
                          static_cast<uint32_t>(startIndex * sizeof(WordRecord));
  if (!indexFile_.seek(offset)) {
    records.clear();
    return false;
  }

  const size_t bytes = count * sizeof(WordRecord);
  const size_t read = indexFile_.read(reinterpret_cast<uint8_t *>(records.data()), bytes);
  if (read != bytes) {
    records.clear();
    return false;
  }

  return true;
}

bool IndexedBookStore::loadWordWindow(size_t index) const {
  if (!isOpen() || index >= wordCount()) {
    return false;
  }

  const size_t start = (index / kWordCacheSize) * kWordCacheSize;
  const size_t count = std::min(kWordCacheSize, wordCount() - start);
  std::vector<WordRecord> records;
  if (!readRecords(start, count, records) || records.empty()) {
    return false;
  }

  const uint32_t dataStart = records.front().offset;
  const WordRecord &last = records.back();
  const uint32_t dataEnd = last.offset + last.length;
  if (dataEnd < dataStart || dataEnd > header_.dataSize) {
    return false;
  }

  const size_t dataBytes = dataEnd - dataStart;
  std::vector<char> buffer(dataBytes);
  if (dataBytes > 0) {
    if (!dataFile_.seek(dataStart)) {
      return false;
    }
    const size_t read = dataFile_.read(reinterpret_cast<uint8_t *>(buffer.data()), dataBytes);
    if (read != dataBytes) {
      return false;
    }
  }

  cachedWords_.clear();
  cachedWords_.reserve(records.size());
  for (const WordRecord &record : records) {
    if (record.offset < dataStart || record.offset + record.length > dataEnd) {
      cachedWords_.clear();
      return false;
    }

    const size_t localOffset = record.offset - dataStart;
    String word;
    word.reserve(record.length);
    for (uint16_t i = 0; i < record.length; ++i) {
      word += buffer[localOffset + i];
    }
    cachedWords_.push_back(word);
  }

  cachedStart_ = start;
  cachedCount_ = cachedWords_.size();
  return cachedCount_ > 0;
}
