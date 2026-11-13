package com.example.intelligentfileorganizer.data.repository

import android.net.Uri
import com.example.intelligentfileorganizer.data.local.dao.FileDao
import com.example.intelligentfileorganizer.data.local.dao.FolderDao
import com.example.intelligentfileorganizer.data.local.entity.FileEntity
import com.example.intelligentfileorganizer.domain.model.FileItem
import com.example.intelligentfileorganizer.storage.IncomingMetadata
import com.example.intelligentfileorganizer.storage.SafStorageManager
import com.example.intelligentfileorganizer.util.AppResult
import com.example.intelligentfileorganizer.util.FileNameUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FileRepository(
    private val fileDao: FileDao,
    private val folderDao: FolderDao,
    private val safStorageManager: SafStorageManager
) {

    fun observeRecentFiles(limit: Int = 10): Flow<List<FileItem>> {
        return fileDao.observeRecentFiles(limit).map { list ->
            list.map { entity ->
                val folderName = folderDao.getFolderById(entity.folderId)?.name ?: "Root"
                entity.toDomain(folderPath = folderName)
            }
        }
    }

    fun observeFilesByFolder(folderId: Long): Flow<List<FileItem>> {
        return fileDao.observeFilesByFolder(folderId).map { list ->
            val folderName = folderDao.getFolderById(folderId)?.name ?: ""
            list.map { it.toDomain(folderPath = folderName) }
        }
    }

    suspend fun getFileById(id: Long): FileItem? {
        val entity = fileDao.getFileById(id) ?: return null
        val folderName = folderDao.getFolderById(entity.folderId)?.name ?: ""
        return entity.toDomain(folderPath = folderName)
    }

    /**
     * Executes the safe 2-phase save workflow:
     * 1. Validate destination folder exists in DB and SAF
     * 2. Stream-copy file into SAF destination with duplicate collision protection
     * 3. Insert metadata into Room
     * 4. If Room insert fails, delete copied SAF document to prevent orphaned files
     */
    suspend fun saveIncomingFile(
        metadata: IncomingMetadata,
        desiredFileName: String,
        targetFolderId: Long
    ): AppResult<FileItem> {
        val folderEntity = folderDao.getFolderById(targetFolderId)
            ?: return AppResult.Error("Selected folder could not be found.")

        val sanitizedDesiredName = FileNameUtils.sanitizeFileName(desiredFileName, fallback = metadata.displayName)
        val finalWithExtension = FileNameUtils.ensureExtension(sanitizedDesiredName, metadata.extension)

        // 1. Stream copy into SAF destination folder
        val saveDocResult = safStorageManager.saveFileToFolder(
            sourceUri = metadata.uri,
            destinationFolderUri = Uri.parse(folderEntity.documentUri),
            desiredFileName = finalWithExtension,
            mimeType = metadata.mimeType
        )

        if (saveDocResult is AppResult.Error) {
            return AppResult.Error(saveDocResult.message, saveDocResult.cause)
        }

        val savedDoc = (saveDocResult as AppResult.Success).data

        // 2. Insert into Room database
        val entity = FileEntity(
            originalName = metadata.displayName,
            storedName = savedDoc.storedName,
            mimeType = metadata.mimeType,
            extension = FileNameUtils.extractExtension(savedDoc.storedName),
            sizeBytes = savedDoc.sizeBytes,
            documentUri = savedDoc.documentUri.toString(),
            documentId = savedDoc.documentId,
            folderId = targetFolderId,
            importedAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        val insertedId = try {
            fileDao.insertFile(entity)
        } catch (e: Exception) {
            // Rollback: delete copied SAF document if database insertion fails
            safStorageManager.deleteDocument(savedDoc.documentUri)
            return AppResult.Error("Failed to record file metadata in database. File save was rolled back.", e)
        }

        return AppResult.Success(
            entity.copy(id = insertedId).toDomain(folderPath = folderEntity.name)
        )
    }

    suspend fun renameFile(fileId: Long, newName: String): AppResult<FileItem> {
        val entity = fileDao.getFileById(fileId)
            ?: return AppResult.Error("File record not found.")

        val sanitized = FileNameUtils.sanitizeFileName(newName, fallback = entity.storedName)
        val newFullName = FileNameUtils.ensureExtension(sanitized, entity.extension)

        if (newFullName == entity.storedName) {
            return AppResult.Success(entity.toDomain())
        }

        // Rename in SAF
        val renameResult = safStorageManager.renameDocument(Uri.parse(entity.documentUri), newFullName)
        if (renameResult is AppResult.Error) {
            return AppResult.Error(renameResult.message, renameResult.cause)
        }

        val updatedUri = (renameResult as AppResult.Success).data
        val updatedEntity = entity.copy(
            storedName = newFullName,
            documentUri = updatedUri.toString(),
            updatedAt = System.currentTimeMillis()
        )
        fileDao.updateFile(updatedEntity)

        return AppResult.Success(updatedEntity.toDomain())
    }

    suspend fun deleteFile(fileId: Long): Boolean {
        val entity = fileDao.getFileById(fileId) ?: return false
        safStorageManager.deleteDocument(Uri.parse(entity.documentUri))
        fileDao.deleteFileById(fileId)
        return true
    }

    private fun FileEntity.toDomain(folderPath: String = ""): FileItem {
        return FileItem(
            id = id,
            originalName = originalName,
            storedName = storedName,
            mimeType = mimeType,
            extension = extension,
            sizeBytes = sizeBytes,
            documentUri = documentUri,
            documentId = documentId,
            folderId = folderId,
            folderPath = folderPath,
            importedAt = importedAt,
            updatedAt = updatedAt
        )
    }
}

