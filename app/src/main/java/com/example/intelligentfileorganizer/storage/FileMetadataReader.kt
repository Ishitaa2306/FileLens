package com.example.intelligentfileorganizer.storage

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.example.intelligentfileorganizer.util.FileNameUtils
import com.example.intelligentfileorganizer.util.MimeTypeUtils

data class IncomingMetadata(
    val uri: Uri,
    val displayName: String,
    val mimeType: String,
    val extension: String,
    val sizeBytes: Long
)

class FileMetadataReader(private val context: Context) {

    fun readMetadata(uri: Uri): IncomingMetadata {
        val contentResolver = context.contentResolver
        var displayName: String? = null
        var sizeBytes: Long = 0L

        if (uri.scheme == "content") {
            try {
                contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (nameIndex != -1) {
                            displayName = cursor.getString(nameIndex)
                        }
                        val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                        if (sizeIndex != -1 && !cursor.isNull(sizeIndex)) {
                            sizeBytes = cursor.getLong(sizeIndex)
                        }
                    }
                }
            } catch (e: Exception) {
                // If query fails, fallback below
            }
        }

        if (displayName.isNullOrBlank()) {
            displayName = uri.lastPathSegment?.substringAfterLast('/') ?: "unnamed_file"
        }

        // Sanitize display name
        displayName = FileNameUtils.sanitizeFileName(displayName!!, fallback = "incoming_file")

        // Resolve MIME type
        val declaredMime = try {
            contentResolver.getType(uri)
        } catch (e: Exception) {
            null
        }
        val resolvedMime = MimeTypeUtils.resolveMimeType(declaredMime, displayName)
        val extension = FileNameUtils.extractExtension(displayName!!)

        return IncomingMetadata(
            uri = uri,
            displayName = displayName!!,
            mimeType = resolvedMime,
            extension = extension,
            sizeBytes = sizeBytes
        )
    }
}

