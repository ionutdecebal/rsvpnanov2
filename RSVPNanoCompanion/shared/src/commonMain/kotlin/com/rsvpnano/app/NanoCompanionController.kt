package com.rsvpnano.app

import com.rsvpnano.api.NanoClient
import com.rsvpnano.models.NanoBook
import com.rsvpnano.models.NanoSettings
import com.rsvpnano.models.NanoWifiSettings
import com.rsvpnano.models.PendingUpload

/**
 * Shared workflow controller for app-level device operations.
 *
 * Platform ViewModels should own UI state, but this class owns the repeated sequencing between
 * local companion data, device sync calls, and post-mutation refreshes.
 */
class NanoCompanionController(
    private val facade: RsvpSharedFacade,
    private val deviceSyncService: NanoDeviceSyncService,
    private val client: NanoClient,
) {
    suspend fun refreshLocal(): CompanionLocalSnapshot {
        return CompanionLocalSnapshot(
            drafts = facade.loadDrafts(),
            rssFeeds = facade.loadRssFeeds(),
        )
    }

    suspend fun connect(baseUrl: String, localRssFeeds: List<String>): CompanionConnectSnapshot {
        val device = deviceSyncService.connect(baseUrl)
        val mergedFeeds = saveMergedRssFeeds(localRssFeeds, device.rssFeeds?.feeds.orEmpty())
        return CompanionConnectSnapshot(
            device = device,
            rssFeeds = mergedFeeds,
            drafts = facade.loadDrafts(),
        )
    }

    suspend fun refreshDevice(baseUrl: String, localRssFeeds: List<String>): CompanionDeviceRefreshSnapshot {
        val books = deviceSyncService.refreshBooks(baseUrl)
        val settings = runCatching { deviceSyncService.refreshSettings(baseUrl) }.getOrNull()
        val wifiSettings = runCatching { deviceSyncService.refreshWifiSettings(baseUrl) }.getOrNull()
        val deviceFeeds = runCatching { deviceSyncService.refreshRssFeeds(baseUrl).feeds }.getOrDefault(emptyList())
        val mergedFeeds = saveMergedRssFeeds(localRssFeeds, deviceFeeds)
        return CompanionDeviceRefreshSnapshot(
            books = books,
            settings = settings,
            wifiSettings = wifiSettings,
            rssFeeds = mergedFeeds,
            syncedRssFeeds = facade.mergeRssFeeds(localFeeds = emptyList(), deviceFeeds = deviceFeeds),
            drafts = facade.loadDrafts(),
        )
    }

    suspend fun syncPendingUploads(baseUrl: String, items: List<PendingUpload>): CompanionPendingSyncSnapshot {
        val remaining = facade.syncPendingUploads(client = client, baseUrl = baseUrl, items = items)
        return CompanionPendingSyncSnapshot(
            drafts = remaining,
            books = deviceSyncService.refreshBooks(baseUrl),
            syncedCount = items.size,
        )
    }

    private suspend fun saveMergedRssFeeds(localFeeds: List<String>, deviceFeeds: List<String>): List<String> {
        return facade.saveRssFeeds(
            facade.mergeRssFeeds(
                localFeeds = localFeeds,
                deviceFeeds = deviceFeeds,
            )
        )
    }
}

data class CompanionLocalSnapshot(
    val drafts: List<PendingUpload>,
    val rssFeeds: List<String>,
)

data class CompanionConnectSnapshot(
    val device: NanoDeviceSnapshot,
    val rssFeeds: List<String>,
    val drafts: List<PendingUpload>,
)

data class CompanionDeviceRefreshSnapshot(
    val books: List<NanoBook>,
    val settings: NanoSettings?,
    val wifiSettings: NanoWifiSettings?,
    val rssFeeds: List<String>,
    val syncedRssFeeds: List<String>,
    val drafts: List<PendingUpload>,
)

data class CompanionPendingSyncSnapshot(
    val drafts: List<PendingUpload>,
    val books: List<NanoBook>,
    val syncedCount: Int,
)
