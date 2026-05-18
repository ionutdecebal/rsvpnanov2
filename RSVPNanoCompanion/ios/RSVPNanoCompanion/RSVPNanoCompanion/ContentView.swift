import SwiftUI
import UIKit

private enum CompanionPage: String, CaseIterable, Identifiable {
    case library = "Library"
    case articles = "Articles"
    case settings = "Settings"
    case help = "Help"

    var id: String { rawValue }

    var systemImage: String {
        switch self {
        case .library:
            return "books.vertical"
        case .articles:
            return "doc.text"
        case .settings:
            return "slider.horizontal.3"
        case .help:
            return "questionmark.circle"
        }
    }
}

struct ContentView: View {
    @StateObject private var viewModel = NanoViewModel()
    @State private var selectedPage: CompanionPage = .library

    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                pageSelector
                Divider()
                selectedPageContent
            }
            .navigationTitle(selectedPage.rawValue)
            .toolbar {
                if selectedPage == .articles || (selectedPage == .library && viewModel.isConnected) {
                    EditButton()
                }
            }
        }
        .safeAreaInset(edge: .bottom) {
            statusBar
        }
        .sheet(isPresented: $viewModel.showingPicker) {
            BookDocumentPicker { file in
                viewModel.showingPicker = false
                viewModel.upload(file)
            } onCancel: {
                viewModel.showingPicker = false
            }
        }
        .sheet(isPresented: $viewModel.showingTextImport) {
            TextImportView { file in
                viewModel.showingTextImport = false
                viewModel.upload(file)
            } onCancel: {
                viewModel.showingTextImport = false
            }
        }
        .sheet(item: $viewModel.editingArticle) { item in
            ArticleEditorView(item: item) { title, body in
                viewModel.savePendingUpload(item, title: title, body: body)
            } onCancel: {
                viewModel.editingArticle = nil
            }
        }
        .task {
            await viewModel.startAutoConnect()
        }
        .onReceive(NotificationCenter.default.publisher(for: UIApplication.willEnterForegroundNotification)) { _ in
            Task { await viewModel.refreshPendingUploads() }
        }
        .onReceive(NotificationCenter.default.publisher(for: UIApplication.didBecomeActiveNotification)) { _ in
            Task {
                await viewModel.refreshPendingUploads()
                if !viewModel.isConnected {
                    viewModel.connect(showBusy: false)
                }
            }
        }
        .onOpenURL { url in
            if url.scheme == "rsvpnano", url.host == "inbox" {
                viewModel.handleSharedInboxOpen()
            }
        }
        .onDisappear {
            viewModel.stopAutoConnect()
        }
    }

    private var pageSelector: some View {
        Picker("Page", selection: $selectedPage) {
            ForEach(CompanionPage.allCases) { page in
                Label(page.rawValue, systemImage: page.systemImage)
                    .tag(page)
            }
        }
        .pickerStyle(.segmented)
        .padding(.horizontal)
        .padding(.vertical, 8)
        .background(.bar)
    }

    @ViewBuilder
    private var selectedPageContent: some View {
        switch selectedPage {
        case .library:
            LibraryPage(
                viewModel: viewModel,
                openWifiSettings: openWifiSettings
            )
        case .articles:
            ArticlesPage(viewModel: viewModel)
        case .settings:
            SettingsPage(viewModel: viewModel)
        case .help:
            HelpPage()
        }
    }

    private var statusBar: some View {
        HStack(spacing: 8) {
            if viewModel.isBusy {
                ProgressView()
                    .scaleEffect(0.75)
            }
            Text(viewModel.status)
                .font(.caption2)
                .foregroundStyle(.secondary)
                .lineLimit(1)
                .truncationMode(.tail)
                .frame(maxWidth: .infinity, alignment: .leading)
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 5)
        .background(.bar)
    }

    private func openWifiSettings() {
        guard let url = URL(string: UIApplication.openSettingsURLString) else {
            return
        }
        UIApplication.shared.open(url)
    }
}
