package com.rsvpnano.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rsvpnano.app.createAndroidSharedApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val sharedApp = remember { createAndroidSharedApp(filesDir) }
            var draftCount by remember { mutableStateOf<Int?>(null) }
            var status by remember { mutableStateOf("Loading drafts...") }

            LaunchedEffect(Unit) {
                val drafts = sharedApp.facade.loadDrafts()
                draftCount = drafts.size
                status = "Loaded drafts from shared storage."
            }

            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.Start,
                    ) {
                        Text(text = "RSVP Nano (Android)", style = MaterialTheme.typography.headlineSmall)
                        Text(text = status, style = MaterialTheme.typography.bodyMedium)
                        Text(text = "Drafts: ${draftCount ?: 0}", style = MaterialTheme.typography.bodyLarge)
                        Button(onClick = {
                            status = "Refresh requested"
                        }) {
                            Text(text = "Refresh")
                        }
                    }
                }
            }
        }
    }
}
