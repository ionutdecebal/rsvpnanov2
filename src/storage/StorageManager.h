#pragma once

#include <Arduino.h>
#include <FS.h>
#include <vector>

#include "reader/BookContent.h"
#include "storage/IndexedBookStore.h"

class StorageManager {
 public:
  using StatusCallback = void (*)(void *context, const char *title, const char *line1,
                                  const char *line2, int progressPercent);

  struct DiagnosticResult {
    bool mounted = false;
    bool booksDirectory = false;
    bool bookFilesDirectory = false;
    bool articleFilesDirectory = false;
    bool configDirectory = false;
    bool writable = false;
    bool booksWritable = false;
    bool articlesWritable = false;
    bool configWritable = false;
    bool foldersRepaired = false;
    size_t bookCount = 0;
    size_t unsupportedCount = 0;
    uint64_t sizeMb = 0;
    String cardType;
    String summary;
    String detail;
  };

  void setStatusCallback(StatusCallback callback, void *context);
  bool begin();
  void end();
  void listBooks();
  void refreshBooks(bool includeMetadata = true);
  bool loadFirstBookWords(std::vector<String> &words, String *loadedPath = nullptr);
  bool loadBookContent(size_t index, BookContent &book, String *loadedPath = nullptr,
                       size_t *loadedIndex = nullptr);
  bool loadIndexedBook(size_t index, IndexedBookStore &store, BookMetadata &metadata,
                       String *loadedPath = nullptr, size_t *loadedIndex = nullptr,
                       bool allowIndexBuild = true, bool allowEpubConversion = true);
  size_t bookCount() const;
  String bookPath(size_t index) const;
  bool bookIsArticle(size_t index) const;
  String bookDisplayName(size_t index) const;
  String bookAuthorName(size_t index) const;
  bool loadBookWords(size_t index, std::vector<String> &words, String *loadedPath = nullptr,
                     size_t *loadedIndex = nullptr);
  DiagnosticResult diagnoseSdCard();
  bool repairSdCardFolders();

 private:
  bool ensureIndexedBook(const String &path, BookMetadata &metadata, bool rsvpFormat,
                         bool allowIndexBuild);
  bool buildIndexedBook(const String &path, BookMetadata &metadata, bool rsvpFormat);
  bool readIndexedMetadata(const String &path, BookMetadata &metadata,
                           IndexedBookStore::Header *header = nullptr);
  bool parseFile(File &file, BookContent &book, bool rsvpFormat);
  bool ensureEpubConverted(const String &epubPath, String &rsvpPath);
  void refreshBookPaths(bool includeMetadata = true);
  void rebuildBookMetadataCache();
  void clearBookCache();
  void notifyStatus(const char *title, const char *line1 = "", const char *line2 = "",
                    int progressPercent = -1);

  bool mounted_ = false;
  bool listedOnce_ = false;
  StatusCallback statusCallback_ = nullptr;
  void *statusContext_ = nullptr;
  std::vector<String> bookPaths_;
  std::vector<String> bookTitles_;
  std::vector<String> bookAuthors_;
};
