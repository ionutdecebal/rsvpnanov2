import SwiftUI
import UIKit
import shared

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
            settingsPage
        case .help:
            HelpPage()
        }
    }

    private var settingsSummarySection: some View {
        Section("Device Settings") {
            Button {
                viewModel.refreshSettings()
            } label: {
                Label("Load Settings", systemImage: "slider.horizontal.3")
            }
            .disabled(viewModel.info == nil || viewModel.isBusy)
        }
    }

    private var wordPacingSettingsSection: some View {
        Section("Word Pacing") {
            if let settings = viewModel.deviceSettings {
                VStack(alignment: .leading, spacing: 8) {
                    settingsControlLabel("Reading Mode")
                    Picker("Reading Mode", selection: readerModeBinding(for: settings)) {
                        Text("One Word").tag("rsvp")
                        Text("Scroll Text").tag("scroll")
                    }
                    .pickerStyle(.segmented)
                }
                .disabled(viewModel.isBusy)

                VStack(alignment: .leading, spacing: 8) {
                    settingsControlLabel("Pause Behaviour")
                    Picker("Pause Behaviour", selection: pauseModeBinding(for: settings)) {
                        Text("At Sentence End").tag("sentence_end")
                        Text("Immediately").tag("instant")
                    }
                    .pickerStyle(.segmented)
                }
                .disabled(viewModel.isBusy)

                Stepper(value: wpmBinding(for: settings), in: 100...1000, step: 25) {
                    LabeledContent("Base Speed", value: "\(settings.reading.wpm) WPM")
                }
                .disabled(viewModel.isBusy)

                Stepper(value: pacingLongBinding(for: settings), in: 0...600, step: 50) {
                    LabeledContent("Long Words", value: "\(settings.reading.pacing.longWordMs) ms")
                }
                .disabled(viewModel.isBusy)

                Stepper(value: pacingComplexBinding(for: settings), in: 0...600, step: 50) {
                    LabeledContent("Complexity", value: "\(settings.reading.pacing.complexWordMs) ms")
                }
                .disabled(viewModel.isBusy)

                Stepper(value: pacingPunctuationBinding(for: settings), in: 0...600, step: 50) {
                    LabeledContent("Punctuation", value: "\(settings.reading.pacing.punctuationMs) ms")
                }
                .disabled(viewModel.isBusy)
            }
        }
    }

    private var displaySettingsSection: some View {
        Section("Display") {
            if let settings = viewModel.deviceSettings {
                VStack(alignment: .leading, spacing: 8) {
                    settingsControlLabel("Display Mode")
                    Picker("Display Mode", selection: appearanceModeBinding(for: settings)) {
                        Text("Light").tag("light")
                        Text("Dark").tag("dark")
                        Text("Night").tag("night")
                    }
                    .pickerStyle(.segmented)
                }
                .disabled(viewModel.isBusy)

                Stepper(value: brightnessBinding(for: settings), in: 0...4) {
                    LabeledContent("Brightness", value: "\(settings.display.brightnessIndex + 1) / 5")
                }
                .disabled(viewModel.isBusy)

                VStack(alignment: .leading, spacing: 8) {
                    settingsControlLabel("Reader Hand")
                    Picker("Reader Hand", selection: handednessBinding(for: settings)) {
                        Text("Left").tag("left")
                        Text("Right").tag("right")
                    }
                    .pickerStyle(.segmented)
                }
                .disabled(viewModel.isBusy)

                VStack(alignment: .leading, spacing: 8) {
                    settingsControlLabel("Footer Label")
                    Picker("Footer Label", selection: footerMetricBinding(for: settings)) {
                        Text("Percent Read").tag("percentage")
                        Text("Chapter Time").tag("chapter_time")
                        Text("Book Time").tag("book_time")
                    }
                }
                .disabled(viewModel.isBusy)

                VStack(alignment: .leading, spacing: 8) {
                    settingsControlLabel("Battery Label")
                    Picker("Battery Label", selection: batteryLabelBinding(for: settings)) {
                        Text("Percentage").tag("percent")
                        Text("Time Remaining").tag("time_remaining")
                    }
                    .pickerStyle(.segmented)
                }
                .disabled(viewModel.isBusy)
            }
        }
    }

    private var typographySettingsSection: some View {
        Section("Typography") {
            if let settings = viewModel.deviceSettings {
                Picker("Typeface", selection: typefaceBinding(for: settings)) {
                    Text("Standard").tag("standard")
                    Text("Atkinson").tag("atkinson")
                    Text("OpenDyslexic").tag("open_dyslexic")
                }
                .disabled(viewModel.isBusy)

                Toggle("Focus Highlight", isOn: focusHighlightBinding(for: settings))
                    .disabled(viewModel.isBusy)

                Toggle("Phantom Words", isOn: phantomWordsBinding(for: settings))
                    .disabled(viewModel.isBusy)

                Stepper(value: fontSizeBinding(for: settings), in: 0...2) {
                    LabeledContent("Font Size", value: "\(settings.display.fontSizeIndex + 1) / 3")
                }
                .disabled(viewModel.isBusy)

                Stepper(value: trackingBinding(for: settings), in: -2...3) {
                    LabeledContent("Tracking", value: "\(settings.typography.tracking)")
                }
                .disabled(viewModel.isBusy)

                Stepper(value: anchorBinding(for: settings), in: 30...40) {
                    LabeledContent("Anchor", value: "\(settings.typography.anchorPercent)%")
                }
                .disabled(viewModel.isBusy)

                Stepper(value: guideWidthBinding(for: settings), in: 12...30, step: 2) {
                    LabeledContent("Guide Width", value: "\(settings.typography.guideWidth)")
                }
                .disabled(viewModel.isBusy)

                Stepper(value: guideGapBinding(for: settings), in: 2...8) {
                    LabeledContent("Guide Gap", value: "\(settings.typography.guideGap)")
                }
                .disabled(viewModel.isBusy)
            }
        }
    }

    private func settingsControlLabel(_ text: String) -> some View {
        Text(text)
            .font(.caption.weight(.semibold))
            .foregroundStyle(.secondary)
    }

    private var settingsPage: some View {
        List {
            if viewModel.deviceSettings == nil {
                settingsSummarySection
            } else {
                wordPacingSettingsSection
                displaySettingsSection
                wifiSettingsSection
                typographySettingsSection

                Section {
                    Button {
                        viewModel.refreshSettings()
                    } label: {
                        Label("Refresh Settings", systemImage: "arrow.clockwise")
                    }
                    .disabled(viewModel.isBusy)

                    Text("Changes are saved to the reader. Exit sync on the device to apply every setting on-screen.")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
            }
        }
    }

    private var wifiSettingsSection: some View {
        Section {
            if let wifi = viewModel.wifiSettings {
                LabeledContent("Saved Network", value: wifi.configured ? wifi.ssid : "Not set")
                if wifi.passwordSet {
                    Text("A password is saved on the reader. The app cannot read it back.")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
            }

            TextField("Network name", text: $viewModel.wifiSsidDraft)
                .textInputAutocapitalization(.never)
                .autocorrectionDisabled()

            SecureField("Password", text: $viewModel.wifiPasswordDraft)
                .textInputAutocapitalization(.never)
                .autocorrectionDisabled()

            HStack(spacing: 10) {
                Button {
                    viewModel.saveWifiSettings()
                } label: {
                    Label("Save Wi-Fi", systemImage: "wifi")
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(.borderedProminent)
                .disabled(viewModel.isBusy || viewModel.wifiSsidDraft.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)

                Button(role: .destructive) {
                    viewModel.forgetWifiSettings()
                } label: {
                    Label("Forget", systemImage: "trash")
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(.bordered)
                .disabled(viewModel.isBusy || viewModel.wifiSettings?.configured != true)
            }

            Button {
                viewModel.refreshWifiSettings()
            } label: {
                Label("Refresh Wi-Fi", systemImage: "arrow.clockwise")
            }
            .disabled(viewModel.isBusy)
        } header: {
            Text("Wi-Fi")
        } footer: {
            Text("Used for RSS and OTA. You can still configure Wi-Fi directly on the reader if you want the standalone path.")
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

    private func wpmBinding(for settings: NanoSettings) -> Binding<Int> {
        Binding(
            get: { viewModel.deviceSettings?.reading.wpm ?? settings.reading.wpm },
            set: { value in
                guard let current = viewModel.deviceSettings else { return }
                viewModel.saveSettings(current.withWpm(value: Int32(value)))
            }
        )
    }

    private func pauseModeBinding(for settings: NanoSettings) -> Binding<String> {
        Binding(
            get: { viewModel.deviceSettings?.reading.pauseMode ?? settings.reading.pauseMode },
            set: { value in
                guard let current = viewModel.deviceSettings else { return }
                viewModel.saveSettings(current.withPauseMode(value: value))
            }
        )
    }

    private func readerModeBinding(for settings: NanoSettings) -> Binding<String> {
        Binding(
            get: { viewModel.deviceSettings?.reading.readerMode ?? settings.reading.readerMode },
            set: { value in
                guard let current = viewModel.deviceSettings else { return }
                viewModel.saveSettings(current.withReaderMode(value: value))
            }
        )
    }

    private func pacingLongBinding(for settings: NanoSettings) -> Binding<Int> {
        Binding(
            get: { viewModel.deviceSettings?.reading.pacing.longWordMs ?? settings.reading.pacing.longWordMs },
            set: { value in
                guard let current = viewModel.deviceSettings else { return }
                viewModel.saveSettings(current.withPacingLongWordMs(value: Int32(value)))
            }
        )
    }

    private func pacingComplexBinding(for settings: NanoSettings) -> Binding<Int> {
        Binding(
            get: { viewModel.deviceSettings?.reading.pacing.complexWordMs ?? settings.reading.pacing.complexWordMs },
            set: { value in
                guard let current = viewModel.deviceSettings else { return }
                viewModel.saveSettings(current.withPacingComplexWordMs(value: Int32(value)))
            }
        )
    }

    private func pacingPunctuationBinding(for settings: NanoSettings) -> Binding<Int> {
        Binding(
            get: { viewModel.deviceSettings?.reading.pacing.punctuationMs ?? settings.reading.pacing.punctuationMs },
            set: { value in
                guard let current = viewModel.deviceSettings else { return }
                viewModel.saveSettings(current.withPacingPunctuationMs(value: Int32(value)))
            }
        )
    }

    private func brightnessBinding(for settings: NanoSettings) -> Binding<Int> {
        Binding(
            get: { viewModel.deviceSettings?.display.brightnessIndex ?? settings.display.brightnessIndex },
            set: { value in
                guard let current = viewModel.deviceSettings else { return }
                viewModel.saveSettings(current.withBrightnessIndex(value: Int32(value)))
            }
        )
    }

    private func handednessBinding(for settings: NanoSettings) -> Binding<String> {
        Binding(
            get: { viewModel.deviceSettings?.display.handedness ?? settings.display.handedness },
            set: { value in
                guard let current = viewModel.deviceSettings else { return }
                viewModel.saveSettings(current.withHandedness(value: value))
            }
        )
    }

    private func footerMetricBinding(for settings: NanoSettings) -> Binding<String> {
        Binding(
            get: { viewModel.deviceSettings?.display.footerMetric ?? settings.display.footerMetric },
            set: { value in
                guard let current = viewModel.deviceSettings else { return }
                viewModel.saveSettings(current.withFooterMetric(value: value))
            }
        )
    }

    private func batteryLabelBinding(for settings: NanoSettings) -> Binding<String> {
        Binding(
            get: { viewModel.deviceSettings?.display.batteryLabel ?? settings.display.batteryLabel },
            set: { value in
                guard let current = viewModel.deviceSettings else { return }
                viewModel.saveSettings(current.withBatteryLabel(value: value))
            }
        )
    }

    private func appearanceModeBinding(for settings: NanoSettings) -> Binding<String> {
        Binding(
            get: {
                let display = viewModel.deviceSettings?.display ?? settings.display
                if display.nightMode {
                    return "night"
                }
                return display.darkMode ? "dark" : "light"
            },
            set: { value in
                guard let current = viewModel.deviceSettings else { return }
                viewModel.saveSettings(
                    current.withAppearance(
                        darkMode: value == "dark" || value == "night",
                        nightMode: value == "night"
                    )
                )
            }
        )
    }

    private func phantomWordsBinding(for settings: NanoSettings) -> Binding<Bool> {
        Binding(
            get: { viewModel.deviceSettings?.display.phantomWords ?? settings.display.phantomWords },
            set: { value in
                guard let current = viewModel.deviceSettings else { return }
                viewModel.saveSettings(current.withPhantomWords(value: value))
            }
        )
    }

    private func fontSizeBinding(for settings: NanoSettings) -> Binding<Int> {
        Binding(
            get: { viewModel.deviceSettings?.display.fontSizeIndex ?? settings.display.fontSizeIndex },
            set: { value in
                guard let current = viewModel.deviceSettings else { return }
                viewModel.saveSettings(current.withFontSizeIndex(value: Int32(value)))
            }
        )
    }

    private func typefaceBinding(for settings: NanoSettings) -> Binding<String> {
        Binding(
            get: { viewModel.deviceSettings?.typography.typeface ?? settings.typography.typeface },
            set: { value in
                guard let current = viewModel.deviceSettings else { return }
                viewModel.saveSettings(current.withTypeface(value: value))
            }
        )
    }

    private func focusHighlightBinding(for settings: NanoSettings) -> Binding<Bool> {
        Binding(
            get: { viewModel.deviceSettings?.typography.focusHighlight ?? settings.typography.focusHighlight },
            set: { value in
                guard let current = viewModel.deviceSettings else { return }
                viewModel.saveSettings(current.withFocusHighlight(value: value))
            }
        )
    }

    private func trackingBinding(for settings: NanoSettings) -> Binding<Int> {
        Binding(
            get: { viewModel.deviceSettings?.typography.tracking ?? settings.typography.tracking },
            set: { value in
                guard let current = viewModel.deviceSettings else { return }
                viewModel.saveSettings(current.withTracking(value: Int32(value)))
            }
        )
    }

    private func anchorBinding(for settings: NanoSettings) -> Binding<Int> {
        Binding(
            get: { viewModel.deviceSettings?.typography.anchorPercent ?? settings.typography.anchorPercent },
            set: { value in
                guard let current = viewModel.deviceSettings else { return }
                viewModel.saveSettings(current.withAnchorPercent(value: Int32(value)))
            }
        )
    }

    private func guideWidthBinding(for settings: NanoSettings) -> Binding<Int> {
        Binding(
            get: { viewModel.deviceSettings?.typography.guideWidth ?? settings.typography.guideWidth },
            set: { value in
                guard let current = viewModel.deviceSettings else { return }
                viewModel.saveSettings(current.withGuideWidth(value: Int32(value)))
            }
        )
    }

    private func guideGapBinding(for settings: NanoSettings) -> Binding<Int> {
        Binding(
            get: { viewModel.deviceSettings?.typography.guideGap ?? settings.typography.guideGap },
            set: { value in
                guard let current = viewModel.deviceSettings else { return }
                viewModel.saveSettings(current.withGuideGap(value: Int32(value)))
            }
        )
    }

}
