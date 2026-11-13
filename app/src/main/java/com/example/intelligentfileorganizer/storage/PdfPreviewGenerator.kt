package com.example.intelligentfileorganizer.storage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Generates local, offline thumbnail previews for PDF documents
 * using Android's native PdfRenderer (no external libraries needed).
 */
class PdfPreviewGenerator(private val context: Context) {

    suspend fun generateFirstPageThumbnail(uri: Uri, maxWidth: Int = 400): Bitmap? = withContext(Dispatchers.IO) {
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        try {
            pfd = context.contentResolver.openFileDescriptor(uri, "r") ?: return@withContext null
            renderer = PdfRenderer(pfd)

            if (renderer.pageCount == 0) return@withContext null

            val page = renderer.openPage(0)

            // Scale maintaining aspect ratio
            val scale = maxWidth.toFloat() / page.width.toFloat()
            val targetWidth = maxWidth
            val targetHeight = (page.height * scale).toInt().coerceAtLeast(1)

            val bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
            // Fill with white background before rendering
            bitmap.eraseColor(android.graphics.Color.WHITE)

            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()

            bitmap
        } catch (e: Exception) {
            // Could fail if password protected or corrupt
            null
        } finally {
            try {
                renderer?.close()
                pfd?.close()
            } catch (_: Exception) {}
        }
    }
}

