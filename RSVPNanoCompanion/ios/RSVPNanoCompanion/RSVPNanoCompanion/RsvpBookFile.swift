import Foundation

struct RsvpBookFile {
    let filename: String
    let data: Data
    let title: String
    let wordCount: Int
    let chapterCount: Int

    init(filename: String, data: Data, title: String, wordCount: Int = 0, chapterCount: Int = 0) {
        self.filename = filename
        self.data = data
        self.title = title
        self.wordCount = wordCount
        self.chapterCount = chapterCount
    }
}
