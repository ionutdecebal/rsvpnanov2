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

    @Test
    fun draculaEpubsUseTocChapterTitles() {
        listOf("Dracula-epub.epub", "Dracula-epub3.epub").forEach { name ->
            val epub = testVectorFile(name)
            val converted = RsvpConverter.bookFile(epub.readBytes(), epub.name)
            val chapters = converted.data.decodeToString()
                .lineSequence()
                .filter { it.startsWith("@chapter ") }
                .toList()

            assertTrue(
                chapters.any { it.startsWith("@chapter CHAPTER I JONATHAN HARKER") },
                "Chapter I should come from the EPUB TOC in $name",
            )
            assertTrue(
                chapters.any { it.startsWith("@chapter CHAPTER II JONATHAN HARKER") },
                "Chapter II should come from the EPUB TOC in $name",
            )
            assertEquals(
                1,
                chapters.count { it.startsWith("@chapter CHAPTER I JONATHAN HARKER") },
                "Chapter I should not be duplicated in $name",
            )
            assertEquals(
                false,
                chapters.any { it.contains("7599939443149237915") },
                "Chapter titles should not fall back to generated XHTML filenames in $name",
            )
            assertEquals(
                false,
                chapters.any { it == "@chapter D R A C U L A" },
                "Book title pages should not become chapter titles in $name",
            )
        }
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
