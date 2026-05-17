package com.rsvpnano.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rsvpnano.app.RsvpSharedApp
import com.rsvpnano.models.PendingUpload
import kotlinx.coroutines.launch

private data class CompanionUiState(
    val drafts: List<PendingUpload> = emptyList(),
    val rssFeeds: List<String> = emptyList(),
    val status: String = "Ready",
)

@Composable
fun CompanionApp(sharedApp: RsvpSharedApp) {
    val scope = rememberCoroutineScope()
    var uiState by remember { mutableStateOf(CompanionUiState(status = "Loading shared data...")) }

    fun refresh() {
        scope.launch {
            uiState = uiState.copy(status = "Refreshing...")
            val drafts = sharedApp.facade.loadDrafts()
            val feeds = sharedApp.facade.loadRssFeeds()
            uiState = uiState.copy(drafts = drafts, rssFeeds = feeds, status = "Loaded ${drafts.size} drafts.")
        }
    }

    LaunchedEffect(Unit) {
        refresh()
    }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(text = "RSVP Nano Companion", style = MaterialTheme.typography.headlineSmall)
                Text(text = uiState.status, style = MaterialTheme.typography.bodyMedium)
                Button(onClick = { refresh() }) {
                    Text(text = "Refresh")
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(text = "Saved Articles", style = MaterialTheme.typography.titleMedium)
                LazyColumn(modifier = Modifier.weight(1f, fill = true)) {
                    if (uiState.drafts.isEmpty()) {
                        item {
                            Text(text = "No drafts yet.")
                        }
                    } else {
                        items(uiState.drafts) { draft ->
                            Text(text = draft.title)
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(text = "RSS Feeds", style = MaterialTheme.typography.titleMedium)
                    }

                    if (uiState.rssFeeds.isEmpty()) {
                        item { Text(text = "No feeds saved.") }
                    } else {
                        items(uiState.rssFeeds) { feed ->
                            Text(text = feed)
                        }
                    }
                }
            }
        }
    }
}
