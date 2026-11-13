package com.example.intelligentfileorganizer.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MimeTypeUtilsTest {

    @Test
    fun testIsSupported_byMimeType() {
        assertTrue(MimeTypeUtils.isSupported("application/pdf"))
        assertTrue(MimeTypeUtils.isSupported("application/msword"))
        assertTrue(MimeTypeUtils.isSupported("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
        assertTrue(MimeTypeUtils.isSupported("image/jpeg"))
        assertTrue(MimeTypeUtils.isSupported("image/png"))

        assertFalse(MimeTypeUtils.isSupported("video/mp4"))
        assertFalse(MimeTypeUtils.isSupported("audio/mpeg"))
        assertFalse(MimeTypeUtils.isSupported("application/zip"))
    }

    @Test
    fun testIsSupported_byFilenameExtensionFallback() {
        assertTrue(MimeTypeUtils.isSupported(null, "lecture_notes.pdf"))
        assertTrue(MimeTypeUtils.isSupported("application/octet-stream", "document.docx"))
        assertTrue(MimeTypeUtils.isSupported(null, "photo.JPG"))
        assertFalse(MimeTypeUtils.isSupported(null, "archive.rar"))
    }

    @Test
    fun testResolveMimeType() {
        assertEquals("application/pdf", MimeTypeUtils.resolveMimeType(null, "document.pdf"))
        assertEquals("application/msword", MimeTypeUtils.resolveMimeType("application/octet-stream", "letter.doc"))
        assertEquals("image/png", MimeTypeUtils.resolveMimeType("image/png", "unnamed"))
    }

    @Test
    fun testGetReadableType() {
        assertEquals("PDF Document", MimeTypeUtils.getReadableType("application/pdf", "pdf"))
        assertEquals("Word Document (DOC)", MimeTypeUtils.getReadableType("application/msword", "doc"))
        assertEquals("Word Document (DOCX)", MimeTypeUtils.getReadableType("application/vnd.openxmlformats-officedocument.wordprocessingml.document", "docx"))
        assertEquals("JPEG Image", MimeTypeUtils.getReadableType("image/jpeg", "jpg"))
        assertEquals("PNG Image", MimeTypeUtils.getReadableType("image/png", "png"))
        assertEquals("Unsupported Document", MimeTypeUtils.getReadableType("application/zip", "zip"))
    }

    @Test
    fun testIsImage() {
        assertTrue(MimeTypeUtils.isImage("image/jpeg"))
        assertTrue(MimeTypeUtils.isImage("image/png"))
        assertFalse(MimeTypeUtils.isImage("application/pdf"))
        assertFalse(MimeTypeUtils.isImage(null))
    }
}

