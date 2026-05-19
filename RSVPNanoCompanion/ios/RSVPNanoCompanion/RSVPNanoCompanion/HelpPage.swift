import SwiftUI

struct HelpPage: View {
    var body: some View {
        List {
            Section("Connection") {
                VStack(alignment: .leading, spacing: 6) {
                    Label("Open Companion sync", systemImage: "wifi")
                    Text("On RSVP Nano, open the main menu and choose Companion sync. Join the RSVP-Nano Wi-Fi network on your iPhone, then return to the app.")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }

                VStack(alignment: .leading, spacing: 6) {
                    Label("Refresh after uploading", systemImage: "arrow.triangle.2.circlepath")
                    Text("After uploading, hold BOOT on the reader to exit Companion sync and refresh the on-device library.")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
            }

            Section("SD Card") {
                VStack(alignment: .leading, spacing: 6) {
                    Label("Recommended card", systemImage: "sdcard")
                    Text("Use a known-good microSD card. 8-32 GB is the most conservative range, and 64 GB cards can work well when formatted as FAT32 with a single partition.")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }

                VStack(alignment: .leading, spacing: 6) {
                    Label("Best file format", systemImage: "doc.text")
                    Text("The app uploads .rsvp files when it can. New books go in /books/books, shared and RSS articles go in /books/articles, and older files directly in /books still work.")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }

                VStack(alignment: .leading, spacing: 6) {
                    Label("If the card fails", systemImage: "exclamationmark.triangle")
                    Text("The usual causes are exFAT formatting, a missing /books folder, the card not being seated fully, a tired or counterfeit card, or files with unsupported extensions.")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }

                VStack(alignment: .leading, spacing: 6) {
                    Label("Try another card", systemImage: "arrow.uturn.forward")
                    Text("Intermittent mounts or failed writes usually point to a worn, counterfeit, or marginal card. A smaller brand-name card is often more reliable.")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
            }

            Section("Articles") {
                VStack(alignment: .leading, spacing: 6) {
                    Label("Safari and Chrome behave differently", systemImage: "safari")
                    Text("Chrome often shares a title immediately. Safari is handled as URL-first for stability, then the app fetches the article text and title after saving.")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }

                VStack(alignment: .leading, spacing: 6) {
                    Label("Large pages", systemImage: "doc.text.magnifyingglass")
                    Text("Very large pages may be rejected during article fetch so the app stays responsive.")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }

                VStack(alignment: .leading, spacing: 6) {
                    Label("RSS feeds", systemImage: "dot.radiowaves.left.and.right")
                    Text("Add feed URLs from the Articles page while connected to Companion sync. The reader saves them to /config/rss.conf and can check them from its main menu when Wi-Fi is configured.")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
            }
        }
    }
}
