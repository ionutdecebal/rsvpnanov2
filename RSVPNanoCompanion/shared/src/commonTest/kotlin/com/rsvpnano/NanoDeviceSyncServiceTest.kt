package com.rsvpnano

import com.rsvpnano.api.NanoClient
import com.rsvpnano.app.NanoDeviceSyncService
import com.rsvpnano.models.NanoBook
import com.rsvpnano.models.NanoInfo
import com.rsvpnano.models.NanoRssFeeds
import com.rsvpnano.models.NanoSettings
import com.rsvpnano.models.NanoWifiSettings
import kotlin.test.Test
import kotlin.test.assertEquals

class NanoDeviceSyncServiceTest {
    @Test
    fun connectBuildsSnapshotFromClientCalls() = kotlinx.coroutines.runBlocking {
        val service = NanoDeviceSyncService(FakeClient())
        val snapshot = service.connect("http://device.local")

        assertEquals("Nano", snapshot.info?.name)
        assertEquals(1, snapshot.books.size)
        assertEquals(true, snapshot.rssFeeds?.ok)
    }

    private class FakeClient : NanoClient {
        override suspend fun fetchInfo(baseUrl: String): NanoInfo = NanoInfo(name = "Nano")
        override suspend fun listBooks(baseUrl: String): List<NanoBook> = listOf(NanoBook(id = "1", title = "Book"))
        override suspend fun fetchSettings(baseUrl: String): NanoSettings = sampleSettings()
        override suspend fun updateSettings(baseUrl: String, settings: NanoSettings): NanoSettings = settings
        override suspend fun fetchWifiSettings(baseUrl: String): NanoWifiSettings = NanoWifiSettings(ok = true, configured = true, ssid = "RSSP", passwordSet = false)
        override suspend fun updateWifi(baseUrl: String, ssid: String, password: String): NanoWifiSettings = NanoWifiSettings(ok = true, configured = true, ssid = ssid, passwordSet = true)
        override suspend fun forgetWifi(baseUrl: String): NanoWifiSettings = NanoWifiSettings(ok = true, configured = false, ssid = "", passwordSet = false)
        override suspend fun fetchRssFeeds(baseUrl: String): NanoRssFeeds = NanoRssFeeds(ok = true, feeds = listOf("https://example.com/feed"))
        override suspend fun updateRssFeeds(baseUrl: String, feeds: List<String>): NanoRssFeeds = NanoRssFeeds(ok = true, feeds = feeds)
        override suspend fun uploadBook(baseUrl: String, name: String, data: ByteArray, category: String?): NanoBook = NanoBook(id = name, title = name, category = category)
        override suspend fun deleteBook(baseUrl: String, name: String) = com.rsvpnano.models.NanoUploadResponse(ok = true)
    }

    private fun sampleSettings(): NanoSettings = NanoSettings(
        ok = true,
        version = 1,
        reading = NanoSettings.Reading(
            wpm = 250,
            readerMode = "single",
            pauseMode = "sentence",
            accurateTimeEstimate = true,
            pacing = NanoSettings.Pacing(longWordMs = 0, complexWordMs = 0, punctuationMs = 0),
        ),
        display = NanoSettings.Display(
            brightnessIndex = 1,
            darkMode = false,
            nightMode = false,
            handedness = "right",
            footerMetric = "battery",
            batteryLabel = "battery",
            language = 0,
            phantomWords = false,
            fontSizeIndex = 1,
        ),
        typography = NanoSettings.Typography(
            typeface = "serif",
            focusHighlight = true,
            tracking = 0,
            anchorPercent = 50,
            guideWidth = 1,
            guideGap = 1,
        ),
    )
}
