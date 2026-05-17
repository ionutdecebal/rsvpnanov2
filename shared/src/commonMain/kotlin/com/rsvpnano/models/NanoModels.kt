package com.rsvpnano.models

import kotlinx.serialization.Serializable

@Serializable
data class NanoBook(
    val id: String? = null,
    val title: String,
    val author: String? = null,
    val progressPercent: Int? = null,
    val category: String? = null
)

@Serializable
data class NanoBooksResponse(
    val books: List<NanoBook>,
)

@Serializable
data class PendingUpload(
    val id: String,
    val title: String,
    val sourceUrl: String? = null,
    val body: String,
    val createdAt: String // ISO-8601 timestamp string; keep simple for portability
)

@Serializable
data class NanoInfo(
    val name: String,
    val mode: String? = null,
    val baseUrl: String? = null,
    val networkSsid: String? = null,
    val pairingCode: String? = null
)

@Serializable
data class NanoUploadResponse(
    val ok: Boolean,
    val path: String? = null,
    val error: String? = null,
)

@Serializable
data class NanoRssFeeds(
    val ok: Boolean,
    val feeds: List<String>,
)

@Serializable
data class NanoWifiSettings(
    val ok: Boolean,
    val configured: Boolean,
    val ssid: String,
    val passwordSet: Boolean,
)

@Serializable
data class NanoWifiUpdate(
    val ssid: String,
    val password: String,
)

@Serializable
data class NanoSettings(
    val ok: Boolean,
    val version: Int,
    val reading: Reading,
    val display: Display,
    val typography: Typography,
    val limits: Limits? = null,
) {
    @Serializable
    data class Reading(
        val wpm: Int,
        val readerMode: String,
        val pauseMode: String,
        val accurateTimeEstimate: Boolean,
        val pacing: Pacing,
    )

    @Serializable
    data class Pacing(
        val longWordMs: Int,
        val complexWordMs: Int,
        val punctuationMs: Int,
    )

    @Serializable
    data class Display(
        val brightnessIndex: Int,
        val darkMode: Boolean,
        val nightMode: Boolean,
        val handedness: String,
        val footerMetric: String,
        val batteryLabel: String,
        val language: Int,
        val phantomWords: Boolean,
        val fontSizeIndex: Int,
    )

    @Serializable
    data class Typography(
        val typeface: String,
        val focusHighlight: Boolean,
        val tracking: Int,
        val anchorPercent: Int,
        val guideWidth: Int,
        val guideGap: Int,
    )

    @Serializable
    data class Limits(
        val wpm: RangeLimit? = null,
        val brightnessIndex: RangeLimit? = null,
        val pacingMs: RangeLimit? = null,
        val tracking: RangeLimit? = null,
        val anchorPercent: RangeLimit? = null,
        val guideWidth: RangeLimit? = null,
        val guideGap: RangeLimit? = null,
    )

    @Serializable
    data class RangeLimit(
        val min: Int,
        val max: Int,
    )
}
