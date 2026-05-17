package com.rsvpnano.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.remember
import com.rsvpnano.app.createAndroidSharedApp
import com.rsvpnano.android.ui.CompanionApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val sharedApp = remember { createAndroidSharedApp(filesDir) }
            CompanionApp(sharedApp)
        }
    }
}
