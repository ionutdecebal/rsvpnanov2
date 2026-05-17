package com.rsvpnano.app

import com.rsvpnano.persistence.FilePendingUploadStorage
import com.rsvpnano.persistence.FileRssFeedStorage
import java.io.File

private const val PendingUploadRelativePath = "pending-uploads/drafts.json"
private const val RssFeedsRelativePath = "rss/feeds.json"

/**
 * Creates shared dependencies for Android using app-private storage paths.
 */
fun createAndroidSharedDependencies(
    appFilesDir: File,
): RsvpSharedDependencies {
    return RsvpSharedDependencies(
        pendingUploadStorage = FilePendingUploadStorage(File(appFilesDir, PendingUploadRelativePath)),
        rssFeedStorage = FileRssFeedStorage(File(appFilesDir, RssFeedsRelativePath)),
    )
}

fun createAndroidSharedFacade(
    appFilesDir: File,
): RsvpSharedFacade {
    return createAndroidSharedDependencies(appFilesDir).createFacade()
}

fun createAndroidSharedApp(
    appFilesDir: File,
): RsvpSharedApp {
    return createAndroidSharedDependencies(appFilesDir).createApp()
}
