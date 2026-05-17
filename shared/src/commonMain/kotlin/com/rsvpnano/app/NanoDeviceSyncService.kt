package com.rsvpnano.app

import com.rsvpnano.api.NanoClient
import com.rsvpnano.models.NanoSettings
import com.rsvpnano.models.NanoWifiSettings

/**
 * Shared orchestration for the device-facing screen state.
 */
class NanoDeviceSyncService(
    private val client: NanoClient,
) {
    suspend fun connect(baseUrl: String): NanoDeviceSnapshot {
        val info = client.fetchInfo(baseUrl)
        val books = runCatching { client.listBooks(baseUrl) }.getOrDefault(emptyList())
        val settings = runCatching { client.fetchSettings(baseUrl) }.getOrNull()
        val wifiSettings = runCatching { client.fetchWifiSettings(baseUrl) }.getOrNull()
        val rssFeeds = runCatching { client.fetchRssFeeds(baseUrl) }.getOrNull()
        return NanoDeviceSnapshot(
            info = info,
            books = books,
            settings = settings,
            wifiSettings = wifiSettings,
            rssFeeds = rssFeeds,
        )
    }

    suspend fun refreshBooks(baseUrl: String): List<com.rsvpnano.models.NanoBook> = client.listBooks(baseUrl)

    suspend fun refreshSettings(baseUrl: String): NanoSettings = client.fetchSettings(baseUrl)

    suspend fun refreshWifiSettings(baseUrl: String): NanoWifiSettings = client.fetchWifiSettings(baseUrl)

    suspend fun refreshRssFeeds(baseUrl: String) = client.fetchRssFeeds(baseUrl)

    suspend fun saveSettings(baseUrl: String, settings: NanoSettings): NanoSettings = client.updateSettings(baseUrl, settings)

    suspend fun saveWifiSettings(baseUrl: String, ssid: String, password: String): NanoWifiSettings =
        client.updateWifi(baseUrl, ssid, password)

    suspend fun clearWifiSettings(baseUrl: String): NanoWifiSettings = client.forgetWifi(baseUrl)
}
