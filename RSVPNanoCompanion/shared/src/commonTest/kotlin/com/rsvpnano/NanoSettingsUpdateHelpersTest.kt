package com.rsvpnano

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotSame
import kotlin.test.assertTrue

class NanoSettingsUpdateHelpersTest {
    @Test
    fun readingHelpersReturnUpdatedCopiesWithoutMutatingOriginal() {
        val original = sampleSettings()
        val updated = original
            .withWpm(325)
            .withReaderMode("scroll")
            .withPauseMode("instant")
            .withAccurateTimeEstimate(false)
            .withPacingLongWordMs(120)
            .withPacingComplexWordMs(80)
            .withPacingPunctuationMs(200)

        assertNotSame(original, updated)
        assertEquals(250, original.reading.wpm)
        assertEquals(325, updated.reading.wpm)
        assertEquals("scroll", updated.reading.readerMode)
        assertEquals("instant", updated.reading.pauseMode)
        assertFalse(updated.reading.accurateTimeEstimate)
        assertEquals(120, updated.reading.pacing.longWordMs)
        assertEquals(80, updated.reading.pacing.complexWordMs)
        assertEquals(200, updated.reading.pacing.punctuationMs)
        assertEquals(original.display, updated.display)
        assertEquals(original.typography, updated.typography)
    }

    @Test
    fun displayHelpersReturnUpdatedCopiesWithoutMutatingOriginal() {
        val original = sampleSettings()
        val updated = original
            .withBrightnessIndex(4)
            .withHandedness("left")
            .withFooterMetric("chapter_time")
            .withBatteryLabel("time_remaining")
            .withAppearance(darkMode = true, nightMode = true)
            .withPhantomWords(true)
            .withFontSizeIndex(2)

        assertNotSame(original, updated)
        assertEquals(1, original.display.brightnessIndex)
        assertEquals(4, updated.display.brightnessIndex)
        assertEquals("left", updated.display.handedness)
        assertEquals("chapter_time", updated.display.footerMetric)
        assertEquals("time_remaining", updated.display.batteryLabel)
        assertTrue(updated.display.darkMode)
        assertTrue(updated.display.nightMode)
        assertTrue(updated.display.phantomWords)
        assertEquals(2, updated.display.fontSizeIndex)
        assertEquals(original.reading, updated.reading)
        assertEquals(original.typography, updated.typography)
    }

    @Test
    fun typographyHelpersReturnUpdatedCopiesWithoutMutatingOriginal() {
        val original = sampleSettings()
        val updated = original
            .withTypeface("atkinson")
            .withFocusHighlight(false)
            .withTracking(2)
            .withAnchorPercent(36)
            .withGuideWidth(18)
            .withGuideGap(4)

        assertNotSame(original, updated)
        assertEquals("serif", original.typography.typeface)
        assertEquals("atkinson", updated.typography.typeface)
        assertFalse(updated.typography.focusHighlight)
        assertEquals(2, updated.typography.tracking)
        assertEquals(36, updated.typography.anchorPercent)
        assertEquals(18, updated.typography.guideWidth)
        assertEquals(4, updated.typography.guideGap)
        assertEquals(original.reading, updated.reading)
        assertEquals(original.display, updated.display)
    }
}
