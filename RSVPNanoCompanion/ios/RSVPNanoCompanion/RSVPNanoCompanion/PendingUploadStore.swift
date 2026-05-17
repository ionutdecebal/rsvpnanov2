import Foundation

enum SharedInbox {
    static let appGroupIdentifier = "group.com.rsvpnano.companion"
}

struct PendingUpload: Identifiable {
    let id: UUID
    let title: String
    let source: String
    let body: String
    let createdAt: Date

    var bytes: Int {
        Data(body.utf8).count
    }

    var needsArticleFetch: Bool {
        guard let url = URL(string: source), ["http", "https"].contains(url.scheme?.lowercased()) else {
            return false
        }
        return body.trimmingCharacters(in: .whitespacesAndNewlines) == source.trimmingCharacters(in: .whitespacesAndNewlines)
    }

    init(id: UUID = UUID(), title: String, source: String, body: String, createdAt: Date = Date()) {
        self.id = id
        self.title = title
        self.source = source
        self.body = body
        self.createdAt = createdAt
    }
}
