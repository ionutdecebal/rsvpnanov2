package com.rsvpnano.app

import com.rsvpnano.persistence.FilePendingUploadStorage
import com.rsvpnano.persistence.FileRssFeedStorage

private const val DefaultAppGroupIdentifier = "group.com.rsvpnano.companion"

/**
 * Creates shared dependencies for the iOS app and share extension.
 *
 * Keeping this wiring in `iosMain` avoids duplicating storage setup in Swift adapters.
 */
fun createIosSharedDependencies(
    appGroupIdentifier: String = DefaultAppGroupIdentifier,
): RsvpSharedDependencies {
    return RsvpSharedDependencies(
        pendingUploadStorage = FilePendingUploadStorage(appGroupIdentifier = appGroupIdentifier),
        rssFeedStorage = FileRssFeedStorage(appGroupIdentifier = appGroupIdentifier),
    )
}

fun createIosSharedFacade(
    appGroupIdentifier: String = DefaultAppGroupIdentifier,
): RsvpSharedFacade {
    return createIosSharedDependencies(appGroupIdentifier).createFacade()
}

fun createIosSharedApp(
    appGroupIdentifier: String = DefaultAppGroupIdentifier,
): RsvpSharedApp {
    return createIosSharedDependencies(appGroupIdentifier).createApp()
}
