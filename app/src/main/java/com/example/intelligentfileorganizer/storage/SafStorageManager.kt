package com.example.intelligentfileorganizer.storage

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import com.example.intelligentfileorganizer.util.AppResult
import com.example.intelligentfileorganizer.util.FileNameUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

class SafStorageManager(private val context: Context) {

    companion object {
        const val ROOT_DIRECTORY_NAME = "Intelligent File Organizer"
    }

    /**
     * Persists URI read/write permissions for the user-selected folder tree.
     */
    fun takePersistablePermissions(treeUri: Uri): Boolean {
        return try {
            val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(treeUri, takeFlags)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Checks if persisted permissions are still valid.
     */
    fun hasValidPermission(treeUri: Uri): Boolean {
        val permissions = context.contentResolver.persistedUriPermissions
        for (perm in permissions) {
            if (perm.uri == treeUri && perm.isWritePermission && perm.isReadPermission) {
                val doc = DocumentFile.fromTreeUri(context, treeUri)
                return doc != null && doc.exists() && doc.canWrite()
            }
        }
        return false
    }

    /**
     * Ensures the "Intelligent File Organizer" root directory exists in the selected tree.
     * Returns its DocumentFile and content URI.
     */
    suspend fun getOrCreateAppRootDirectory(treeUri: Uri): AppResult<DocumentFile> = withContext(Dispatchers.IO) {
        try {
            val rootDoc = DocumentFile.fromTreeUri(context, treeUri)
                ?: return@withContext AppResult.Error("Unable to access the selected storage folder.")

            if (!rootDoc.exists() || !rootDoc.canWrite()) {
                return@withContext AppResult.Error("Storage folder is not writable or access was revoked.")
            }

            // Look for existing root folder
            val existingRoot = rootDoc.findFile(ROOT_DIRECTORY_NAME)
            if (existingRoot != null && existingRoot.isDirectory) {
                return@withContext AppResult.Success(existingRoot)
            }

            // Create root folder
            val created = rootDoc.createDirectory(ROOT_DIRECTORY_NAME)
                ?: return@withContext AppResult.Error("Failed to create '$ROOT_DIRECTORY_NAME' directory in Documents.")

            AppResult.Success(created)
        } catch (e: Exception) {
            AppResult.Error("Storage initialization failed: ${e.localizedMessage ?: "Unknown error"}", e)
        }
    }

    /**
     * Creates a subdirectory inside the given parent folder URI.
     */
    suspend fun createDirectory(parentFolderUri: Uri, folderName: String): AppResult<DocumentFile> = withContext(Dispatchers.IO) {
        try {
            val parentDoc = DocumentFile.fromTreeUri(context, parentFolderUri)
                ?: DocumentFile.fromSingleUri(context, parentFolderUri)
                ?: return@withContext AppResult.Error("Parent folder is inaccessible.")

            val sanitized = FileNameUtils.sanitizeFileName(folderName, fallback = "New Folder")
            val existing = parentDoc.findFile(sanitized)
            if (existing != null && existing.isDirectory) {
                return@withContext AppResult.Success(existing)
            }

            val newDir = parentDoc.createDirectory(sanitized)
                ?: return@withContext AppResult.Error("Could not create directory '$sanitized'. Check storage permissions.")

            AppResult.Success(newDir)
        } catch (e: Exception) {
            AppResult.Error("Directory creation failed: ${e.localizedMessage ?: "Unknown error"}", e)
        }
    }

    /**
     * Copies a file from source URI to the destination folder using streaming.
     * Avoids overwriting by generating a unique name if a duplicate exists.
     */
    suspend fun saveFileToFolder(
        sourceUri: Uri,
        destinationFolderUri: Uri,
        desiredFileName: String,
        mimeType: String
    ): AppResult<SavedDocumentResult> = withContext(Dispatchers.IO) {
        val destFolderDoc = DocumentFile.fromTreeUri(context, destinationFolderUri)
            ?: DocumentFile.fromSingleUri(context, destinationFolderUri)
            ?: return@withContext AppResult.Error("Destination folder cannot be found.")

        // Collect existing file names in destination to prevent collision
        val existingFiles = destFolderDoc.listFiles().mapNotNull { it.name }.toSet()
        val finalFileName = FileNameUtils.generateUniqueName(desiredFileName, existingFiles)

        var createdDoc: DocumentFile? = null
        try {
            createdDoc = destFolderDoc.createFile(mimeType, finalFileName)
                ?: return@withContext AppResult.Error("Could not create destination file '$finalFileName' via SAF.")

            val targetUri = createdDoc.uri
            var bytesWritten = 0L

            context.contentResolver.openInputStream(sourceUri).use { inStream ->
                if (inStream == null) {
                    throw IOException("Could not open input stream from source file.")
                }
                context.contentResolver.openOutputStream(targetUri, "w").use { outStream ->
                    if (outStream == null) {
                        throw IOException("Could not open output stream to target document.")
                    }
                    val buffer = ByteArray(8192)
                    var read: Int
                    while (inStream.read(buffer).also { read = it } != -1) {
                        outStream.write(buffer, 0, read)
                        bytesWritten += read
                    }
                    outStream.flush()
                }
            }

            val documentId = DocumentsContract.getDocumentId(createdDoc.uri)
                ?: createdDoc.uri.toString()

            AppResult.Success(
                SavedDocumentResult(
                    documentUri = createdDoc.uri,
                    documentId = documentId,
                    storedName = createdDoc.name ?: finalFileName,
                    sizeBytes = bytesWritten
                )
            )
        } catch (e: Exception) {
            // Clean up incomplete file on copy failure
            try {
                createdDoc?.delete()
            } catch (_: Exception) {}
            AppResult.Error("Failed to save file: ${e.localizedMessage ?: "I/O copy failure"}", e)
        }
    }

    /**
     * Renames an existing SAF document.
     */
    suspend fun renameDocument(documentUri: Uri, newName: String): AppResult<Uri> = withContext(Dispatchers.IO) {
        try {
            val sanitized = FileNameUtils.sanitizeFileName(newName)
            val updatedUri = DocumentsContract.renameDocument(context.contentResolver, documentUri, sanitized)
            if (updatedUri != null) {
                AppResult.Success(updatedUri)
            } else {
                AppResult.Error("Document provider failed to rename file.")
            }
        } catch (e: Exception) {
            AppResult.Error("Rename failed: ${e.localizedMessage ?: "Unknown error"}", e)
        }
    }

    /**
     * Deletes a SAF document (used for rollback or file management).
     */
    suspend fun deleteDocument(documentUri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            DocumentsContract.deleteDocument(context.contentResolver, documentUri)
        } catch (e: Exception) {
            val doc = DocumentFile.fromSingleUri(context, documentUri)
            doc?.delete() ?: false
        }
    }
}

data class SavedDocumentResult(
    val documentUri: Uri,
    val documentId: String,
    val storedName: String,
    val sizeBytes: Long
)

