package com.example.intelligentfileorganizer.storage

import android.content.Context
import android.net.Uri
import com.example.intelligentfileorganizer.util.AppResult
import com.example.intelligentfileorganizer.util.MimeTypeUtils

class IncomingFileHandler(
    private val context: Context,
    private val metadataReader: FileMetadataReader
) {

    fun validateAndProcess(uri: Uri): AppResult<IncomingMetadata> {
        return try {
            // Verify accessibility by opening a test stream
            val testStream = context.contentResolver.openInputStream(uri)
            if (testStream == null) {
                return AppResult.Error("Cannot access incoming file. Storage permission might be expired or the file was deleted.")
            }
            testStream.close()

            val metadata = metadataReader.readMetadata(uri)

            if (!MimeTypeUtils.isSupported(metadata.mimeType, metadata.displayName)) {
                return AppResult.Error(
                    "Unsupported file format (${metadata.extension.uppercase().ifEmpty { metadata.mimeType }}). " +
                            "Phase 1 supports PDF, DOC, DOCX, JPEG, and PNG."
                )
            }

            AppResult.Success(metadata)
        } catch (e: SecurityException) {
            AppResult.Error("Access denied by source application. The temporary permission has expired.", e)
        } catch (e: Exception) {
            AppResult.Error("Failed to read incoming file: ${e.localizedMessage ?: "Unknown error"}", e)
        }
    }
}

