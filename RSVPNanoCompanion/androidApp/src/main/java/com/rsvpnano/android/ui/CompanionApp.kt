package com.rsvpnano.android.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rsvpnano.app.RsvpSharedApp
import com.rsvpnano.converters.RsvpSupportedFileTypes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun CompanionApp(
    sharedApp: RsvpSharedApp,
    shareIntent: Intent? = null,
    onShareIntentHandled: () -> Unit = {},
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val viewModel: CompanionViewModel = viewModel(factory = CompanionViewModel.Factory(sharedApp))
    val uiState by viewModel.uiState.collectAsState()
    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            context.readSelectedFile(uri)?.let { file ->
                viewModel.uploadSelectedFile(displayName = file.displayName, data = file.data)
            }
        }
    }
    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.recheckConnectionAfterResume()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(shareIntent) {
        val intent = shareIntent ?: return@LaunchedEffect
        val imports = withContext(Dispatchers.IO) { context.sharedImportsFrom(intent) }
        if (imports.isNotEmpty() || intent.isAndroidShareIntent()) {
            viewModel.saveSharedImports(imports)
        }
        onShareIntentHandled()
    }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                HeaderCard(
                    status = uiState.status,
                    isConnected = uiState.isConnected,
                    onReconnect = viewModel::connect,
                )

                if (!uiState.isConnected) {
                    ConnectionPanel(
                        uiState = uiState,
                        onOpenWifiSettings = context::openWifiSettings,
                        onConnectDefault = viewModel::connectDefault,
                        onShowAddressEntry = viewModel::showAddressEntry,
                        onAddressChange = viewModel::setAddress,
                        onConnectCustom = viewModel::connect,
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(modifier = Modifier.weight(1f, fill = true)) {
                    item {
                        SectionCard(title = "Library") {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = viewModel::refresh) {
                                    Text(text = "Refresh")
                                }
                                Button(
                                    onClick = {
                                        filePicker.launch(
                                            arrayOf(
                                                "application/epub+zip",
                                                "text/*",
                                                "text/html",
                                                "application/octet-stream",
                                            ),
                                        )
                                    },
                                    enabled = uiState.isConnected,
                                ) {
                                    Text(text = "Upload")
                                }
                            }
                            if (uiState.books.isEmpty()) {
                                Text(
                                    text = if (uiState.isConnected) "No books on device." else "Connect to load books.",
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                        }
                    }
                    if (uiState.books.isNotEmpty()) {
                        items(uiState.books) { book ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 10.dp),
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Text(text = book.displayTitle, style = MaterialTheme.typography.titleSmall)
                                    TextButton(onClick = { viewModel.deleteDeviceBook(book) }) {
                                        Text(text = "Delete")
                                    }
                                }
                            }
                        }
                    }

                    item {
                        SectionCard(title = "Reader Settings") {
                            if (uiState.settings == null) {
                                Text(text = if (uiState.isConnected) "Settings unavailable." else "Connect to load settings.")
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = uiState.settingsWpmDraft,
                                        onValueChange = viewModel::setSettingsWpmDraft,
                                        label = { Text("Words per minute") },
                                        singleLine = true,
                                    )
                                    OutlinedTextField(
                                        value = uiState.settingsBrightnessDraft,
                                        onValueChange = viewModel::setSettingsBrightnessDraft,
                                        label = { Text("Brightness index") },
                                        singleLine = true,
                                    )
                                    Button(onClick = viewModel::saveDeviceSettings) {
                                        Text(text = "Save")
                                    }
                                }
                            }
                        }
                    }

                    item {
                        SectionCard(title = "Wi-Fi") {
                            val wifiStatus = uiState.wifiSettings?.let { wifi ->
                                if (wifi.configured) {
                                    "Configured for ${wifi.ssid}"
                                } else {
                                    "Not configured"
                                }
                            } ?: if (uiState.isConnected) {
                                "Wi-Fi settings unavailable."
                            } else {
                                "Connect to load Wi-Fi settings."
                            }
                            Text(text = wifiStatus)
                            OutlinedTextField(
                                value = uiState.wifiSsidDraft,
                                onValueChange = viewModel::setWifiSsidDraft,
                                label = { Text("Network name") },
                                singleLine = true,
                            )
                            OutlinedTextField(
                                value = uiState.wifiPasswordDraft,
                                onValueChange = viewModel::setWifiPasswordDraft,
                                label = { Text("Password") },
                                singleLine = true,
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = viewModel::saveWifiSettings) {
                                    Text(text = "Save")
                                }
                                TextButton(onClick = viewModel::clearWifiSettings) {
                                    Text(text = "Forget")
                                }
                            }
                        }
                    }

                    item {
                        SectionCard(title = "Saved Articles") {
                            if (uiState.editingDraftId != null) {
                                Text(text = "Editing saved article", style = MaterialTheme.typography.bodyMedium)
                            }
                            OutlinedTextField(
                                value = uiState.draftTitle,
                                onValueChange = viewModel::setDraftTitle,
                                label = { Text("Article title") },
                                singleLine = true,
                            )
                            OutlinedTextField(
                                value = uiState.draftSourceUrl,
                                onValueChange = viewModel::setDraftSourceUrl,
                                label = { Text("Source URL") },
                                singleLine = true,
                            )
                            OutlinedTextField(
                                value = uiState.draftBody,
                                onValueChange = viewModel::setDraftBody,
                                label = { Text("Article text") },
                                minLines = 3,
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = viewModel::saveTextDraft) {
                                    Text(text = if (uiState.editingDraftId == null) "Save text" else "Update")
                                }
                                TextButton(onClick = viewModel::saveLinkDraft) {
                                    Text(text = if (uiState.editingDraftId == null) "Save link" else "Update link")
                                }
                                if (uiState.editingDraftId != null) {
                                    TextButton(onClick = viewModel::cancelDraftEdit) {
                                        Text(text = "Cancel")
                                    }
                                }
                            }
                        }
                    }

                    if (uiState.drafts.isEmpty()) {
                        item {
                            Text(text = "No drafts yet.")
                        }
                    } else {
                        items(uiState.drafts) { draft ->
                            val suffix = if (viewModel.needsArticleFetch(draft)) " (URL only)" else ""
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 10.dp),
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                Text(text = draft.title + suffix, style = MaterialTheme.typography.titleSmall)
                                if (viewModel.needsArticleFetch(draft)) {
                                    Text(
                                        text = "Needs article text before sync.",
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    TextButton(onClick = { viewModel.editDraft(draft) }) {
                                        Text(text = "Edit")
                                    }
                                    TextButton(onClick = { viewModel.deleteDraft(draft) }) {
                                        Text(text = "Delete")
                                    }
                                }
                                }
                            }
                        }
                        item {
                            Button(onClick = viewModel::syncSavedArticles) {
                                Text(text = "Sync ready articles")
                            }
                        }
                    }

                    item {
                        SectionCard(title = "RSS Feeds") {
                            if (uiState.rssFeeds.isEmpty()) {
                                Text(text = "No feeds saved.")
                            } else {
                                uiState.rssFeeds.forEach { feed ->
                                    Text(text = feed, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            OutlinedTextField(
                                value = uiState.rssFeedDraft,
                                onValueChange = viewModel::setRssFeedDraft,
                                label = { Text("Feed URL") },
                                singleLine = true,
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = viewModel::addRssFeed) {
                                    Text(text = "Add")
                                }
                                TextButton(onClick = viewModel::syncRssFeeds) {
                                    Text(text = "Sync")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HeaderCard(
    status: String,
    isConnected: Boolean,
    onReconnect: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(text = "RSVP Nano", style = MaterialTheme.typography.headlineSmall)
            Text(
                text = if (isConnected) "Connected" else "Connect to sync books, articles, and settings.",
                style = MaterialTheme.typography.bodyLarge,
            )
            HorizontalDivider()
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = status,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodySmall,
                )
                if (isConnected) {
                    TextButton(onClick = onReconnect) {
                        Text(text = "Reconnect")
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 14.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
private fun ConnectionPanel(
    uiState: CompanionUiState,
    onOpenWifiSettings: () -> Unit,
    onConnectDefault: () -> Unit,
    onShowAddressEntry: () -> Unit,
    onAddressChange: (String) -> Unit,
    onConnectCustom: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
        Text(text = "Connect", style = MaterialTheme.typography.titleMedium)
        Text(
            text = "Open Companion sync on the reader, join its Wi-Fi, then return here.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onOpenWifiSettings) {
                Text(text = "Join Nano Wi-Fi")
            }
            Button(onClick = onConnectDefault) {
                Text(text = "Check Now")
            }
        }

        TextButton(onClick = onShowAddressEntry) {
            Text(text = "Reader not found?")
        }

        if (uiState.showAddressEntry) {
            Text(
                text = "Enter the address shown on the reader if it is not 192.168.4.1.",
                style = MaterialTheme.typography.bodySmall,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onOpenWifiSettings) {
                    Text(text = "Wi-Fi Settings")
                }
            }

            OutlinedTextField(
                value = uiState.address,
                onValueChange = onAddressChange,
                label = { Text("Reader address or IP") },
                singleLine = true,
            )
            Button(onClick = onConnectCustom) {
                Text(text = "Connect to This Address")
            }
        }
        }
    }
}

private data class SelectedFile(
    val displayName: String,
    val data: ByteArray,
)

private fun Context.readSelectedFile(uri: Uri): SelectedFile? {
    val displayName = displayNameFor(uri) ?: "selected-book"
    val data = contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
    return SelectedFile(displayName = displayName, data = data)
}

private fun Context.sharedImportsFrom(intent: Intent): List<SharedImport> {
    if (!intent.isAndroidShareIntent()) {
        return emptyList()
    }

    val preferredTitle = intent.sharedTitle()
    val imports = mutableListOf<SharedImport>()
    val sharedText = intent.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString()?.trim()
    if (!sharedText.isNullOrEmpty()) {
        imports += SharedImport(
            title = preferredTitle,
            text = sharedText,
            source = sharedText.takeIf { it.isHttpUrl() }.orEmpty(),
        )
    }

    intent.sharedStreamUris().forEach { uri ->
        readSharedText(uri, preferredTitle)?.let(imports::add)
    }
    return imports
}

private fun Context.readSharedText(uri: Uri, preferredTitle: String): SharedImport? {
    val displayName = displayNameFor(uri) ?: preferredTitle.ifEmpty { "Shared Text" }
    val mimeType = contentResolver.getType(uri).orEmpty()
    if (!mimeType.isTextMimeType() && !displayName.isTextFileName()) {
        return null
    }
    val text = contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() } ?: return null
    return SharedImport(
        title = preferredTitle.ifEmpty { displayName.substringBeforeLast('.', displayName) },
        text = text,
        source = uri.toString(),
    )
}

private fun Context.displayNameFor(uri: Uri): String? {
    contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0) {
                return cursor.getString(index)
            }
        }
    }
    return uri.lastPathSegment?.substringAfterLast('/')
}

private fun Intent.isAndroidShareIntent(): Boolean {
    return action == Intent.ACTION_SEND || action == Intent.ACTION_SEND_MULTIPLE
}

private fun Intent.sharedTitle(): String {
    return getStringExtra(Intent.EXTRA_TITLE)
        ?: getStringExtra(Intent.EXTRA_SUBJECT)
        ?: "Shared Text"
}

private fun Intent.sharedStreamUris(): List<Uri> {
    val uris = mutableListOf<Uri>()
    clipData?.let { data ->
        for (index in 0 until data.itemCount) {
            data.getItemAt(index).uri?.let(uris::add)
        }
    }
    extraStreamUri()?.let(uris::add)
    extraStreamUris().forEach(uris::add)
    return uris.distinctBy { it.toString() }
}

@Suppress("DEPRECATION")
private fun Intent.extraStreamUri(): Uri? {
    return runCatching { getParcelableExtra<Uri>(Intent.EXTRA_STREAM) }.getOrNull()
}

@Suppress("DEPRECATION")
private fun Intent.extraStreamUris(): List<Uri> {
    return runCatching { getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM).orEmpty() }.getOrDefault(emptyList())
}

private fun String.isHttpUrl(): Boolean {
    val value = trim()
    return value.startsWith("http://") || value.startsWith("https://")
}

private fun String.isTextMimeType(): Boolean = startsWith("text/")

private fun String.isTextFileName(): Boolean {
    return RsvpSupportedFileTypes.isTextLike(this)
}

private fun Context.openWifiSettings() {
    val intent = Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { startActivity(intent) }
        .recover {
            startActivity(
                Intent(Settings.ACTION_WIFI_SETTINGS)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
}
