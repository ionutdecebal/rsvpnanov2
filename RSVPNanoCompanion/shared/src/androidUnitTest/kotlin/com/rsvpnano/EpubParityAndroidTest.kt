package com.rsvpnano

import com.rsvpnano.converters.RsvpConverter
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EpubParityAndroidTest {

    @Test
    fun convertsRealEpubToRsvp() {
        val epub = testVectorFile("sample.epub")
        val data = epub.readBytes()
        val converted = RsvpConverter.bookFile(data, epub.name)

        // Mirrors docs/conversion-spec.md: the title comes from OPF metadata.
        assertEquals("Letter", converted.title)
        assertTrue(converted.filename.endsWith(".rsvp"))
        
        // Verify basic conversion results
        assertTrue(converted.wordCount > 0)
        assertTrue(converted.chapterCount >= 1)
        assertEquals(
            testVectorFile("sample-expected.rsvp").readText().replace("\r\n", "\n"),
            converted.data.decodeToString(),
        )
    }

    private fun testVectorFile(name: String): File {
        val candidates = listOf(
            File("RSVPNanoCompanion/testdata/conversion", name),
            File("../testdata/conversion", name),
            File("testdata/conversion", name),
        )
        return candidates.firstOrNull { it.isFile }
            ?: error("Test vector not found. Checked: ${candidates.joinToString { it.path }}")
    }
}
