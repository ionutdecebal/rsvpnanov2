package com.rsvpnano.converters

import com.rsvpnano.models.PendingUpload

object ImportPreparation {
    fun titleForText(preferredTitle: String, text: String, fallback: String): String {
        return preferredTitle.trim().ifEmpty {
            RsvpConverter.titleFromText(text = text, fallback = fallback)
        }
    }

    fun titleForSharedUrl(preferredTitle: String, source: String, host: String): String {
        val cleanedTitle = preferredTitle.trim()
        if (
            cleanedTitle.isNotEmpty() &&
            cleanedTitle != host &&
            cleanedTitle != "www.$host" &&
            !source.contains(cleanedTitle)
        ) {
            return cleanedTitle
        }
        return source
    }

    fun rsvpFileForText(title: String, source: String, text: String, fallbackTitle: String): RsvpBookFile {
        return RsvpConverter.rsvpFile(
            title = titleForText(preferredTitle = title, text = text, fallback = fallbackTitle),
            source = source,
            text = text,
        )
    }

    fun pendingUploadForText(
        id: String,
        title: String,
        source: String,
        text: String,
        createdAt: String,
        fallbackTitle: String,
    ): PendingUpload {
        val cleanedText = text.trim()
        return PendingUpload(
            id = id,
            title = titleForText(preferredTitle = title, text = cleanedText, fallback = fallbackTitle),
            sourceUrl = source.trim().ifEmpty { null },
            body = cleanedText,
            createdAt = createdAt,
        )
    }

    fun pendingUploadForUrl(
        id: String,
        title: String,
        source: String,
        host: String,
        createdAt: String,
    ): PendingUpload {
        val cleanedSource = source.trim()
        return PendingUpload(
            id = id,
            title = titleForSharedUrl(preferredTitle = title, source = cleanedSource, host = host),
            sourceUrl = cleanedSource.ifEmpty { null },
            body = cleanedSource,
            createdAt = createdAt,
        )
    }
}
