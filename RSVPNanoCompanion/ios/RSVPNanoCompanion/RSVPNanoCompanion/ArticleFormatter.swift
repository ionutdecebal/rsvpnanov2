import Foundation
import shared

enum ArticleFetchError: LocalizedError {
    case invalidURL
    case articleTooLarge
    case emptyArticle

    var errorDescription: String? {
        switch self {
        case .invalidURL:
            return "The article URL is not valid."
        case .articleTooLarge:
            return "The page is too large to fetch safely."
        case .emptyArticle:
            return "No readable article text was found."
        }
    }
}

enum ArticleFetchService {
    private static let maxFetchedBytes = 900_000
    private static let maxTextCharacters = 250_000

    static func fetch(title: String, source: String) async throws -> shared.SharedArticle {
        guard let url = URL(string: source), ["http", "https"].contains(url.scheme?.lowercased()) else {
            throw ArticleFetchError.invalidURL
        }

        return try await Task.detached(priority: .userInitiated) {
            var request = URLRequest(url: url, timeoutInterval: 15)
            request.setValue("Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15 Safari/604.1", forHTTPHeaderField: "User-Agent")
            let (data, response) = try await URLSession.shared.data(for: request)
            if let response = response as? HTTPURLResponse,
               let length = response.value(forHTTPHeaderField: "Content-Length"),
               let bytes = Int(length),
               bytes > maxFetchedBytes {
                throw ArticleFetchError.articleTooLarge
            }
            guard data.count <= maxFetchedBytes else {
                throw ArticleFetchError.articleTooLarge
            }
            let limited = data.prefix(maxFetchedBytes)
            let decoded = shared.RsvpConverter.shared.decodeText(data: kotlinByteArray(from: Data(limited)))
                ?? String(data: limited, encoding: .utf8)
                ?? ""
            let clipped = String(decoded.prefix(maxTextCharacters))
            let article = shared.ArticleFormatter.shared.article(title: title, source: source, htmlOrText: clipped)
            if article.text.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                throw ArticleFetchError.emptyArticle
            }
            return article
        }.value
    }

    private static func kotlinByteArray(from data: Data) -> KotlinByteArray {
        let array = KotlinByteArray(size: Int32(data.count))
        data.withUnsafeBytes { buffer in
            guard let baseAddress = buffer.baseAddress else { return }
            for index in 0..<data.count {
                let value = baseAddress.advanced(by: index).assumingMemoryBound(to: UInt8.self).pointee
                array.set(index: Int32(index), value: Int8(bitPattern: value))
            }
        }
        return array
    }
}