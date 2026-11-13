package com.example.intelligentfileorganizer.util

import org.junit.Assert.assertEquals
import org.junit.Test

class FileNameUtilsTest {

    @Test
    fun testSanitizeFileName_removesIllegalCharacters() {
        val input = "my:file/name*is?awesome<>.pdf"
        val expected = "my_file_name_is_awesome__.pdf"
        assertEquals(expected, FileNameUtils.sanitizeFileName(input))
    }

    @Test
    fun testSanitizeFileName_fallbackWhenEmptyOrDots() {
        assertEquals("fallback", FileNameUtils.sanitizeFileName("", fallback = "fallback"))
        assertEquals("fallback", FileNameUtils.sanitizeFileName("   ", fallback = "fallback"))
        assertEquals("fallback", FileNameUtils.sanitizeFileName(".", fallback = "fallback"))
        assertEquals("fallback", FileNameUtils.sanitizeFileName("..", fallback = "fallback"))
    }

    @Test
    fun testExtractExtension() {
        assertEquals("pdf", FileNameUtils.extractExtension("report.pdf"))
        assertEquals("docx", FileNameUtils.extractExtension("Assignment.DOCX"))
        assertEquals("png", FileNameUtils.extractExtension("screenshot.final.png"))
        assertEquals("", FileNameUtils.extractExtension("no_extension"))
    }

    @Test
    fun testStripExtension() {
        assertEquals("report", FileNameUtils.stripExtension("report.pdf"))
        assertEquals("assignment.v1", FileNameUtils.stripExtension("assignment.v1.docx"))
        assertEquals("plain_file", FileNameUtils.stripExtension("plain_file"))
    }

    @Test
    fun testEnsureExtension() {
        // When extension is missing
        assertEquals("my_notes.pdf", FileNameUtils.ensureExtension("my_notes", "pdf"))
        // When extension is already present
        assertEquals("my_notes.pdf", FileNameUtils.ensureExtension("my_notes.pdf", "pdf"))
        // When extension is case-insensitive
        assertEquals("my_notes.PDF", FileNameUtils.ensureExtension("my_notes.PDF", "pdf"))
    }

    @Test
    fun testGenerateUniqueName_collisionHandling() {
        val existing = setOf("report.pdf", "report (1).pdf", "notes.docx")

        // Non-colliding name remains untouched
        assertEquals("invoice.pdf", FileNameUtils.generateUniqueName("invoice.pdf", existing))

        // Colliding name receives incremented counter
        assertEquals("report (2).pdf", FileNameUtils.generateUniqueName("report.pdf", existing))
    }

    @Test
    fun testFormatFileSize() {
        assertEquals("0 B", FileNameUtils.formatFileSize(0))
        assertEquals("500 B", FileNameUtils.formatFileSize(500))
        assertEquals("1.5 KB", FileNameUtils.formatFileSize(1536))
        assertEquals("10 MB", FileNameUtils.formatFileSize(10 * 1024 * 1024))
    }
}
