package com.example.intelligentfileorganizer.data.repository

import android.net.Uri
import com.example.intelligentfileorganizer.data.local.dao.FolderDao
import com.example.intelligentfileorganizer.data.local.entity.FolderEntity
import com.example.intelligentfileorganizer.domain.model.FolderItem
import com.example.intelligentfileorganizer.storage.SafStorageManager
import com.example.intelligentfileorganizer.util.AppResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FolderRepository(
    private val folderDao: FolderDao,
    private val storageRepository: StorageRepository,
    private val safStorageManager: SafStorageManager
) {

    fun observeRootFolders(): Flow<List<FolderItem>> {
        return folderDao.observeRootFolders().map { list ->
            list.map { it.toDomain(subfolders = folderDao.countSubfolders(it.id), files = folderDao.countFilesInFolder(it.id)) }
        }
    }

    fun observeSubfolders(parentId: Long): Flow<List<FolderItem>> {
        return folderDao.observeSubfolders(parentId).map { list ->
            list.map { it.toDomain(subfolders = folderDao.countSubfolders(it.id), files = folderDao.countFilesInFolder(it.id)) }
        }
    }

    suspend fun getFolderById(folderId: Long): FolderItem? {
        val entity = folderDao.getFolderById(folderId) ?: return null
        return entity.toDomain(
            subfolders = folderDao.countSubfolders(entity.id),
            files = folderDao.countFilesInFolder(entity.id)
        )
    }

    suspend fun createFolder(name: String, parentId: Long?): AppResult<FolderItem> {
        val config = storageRepository.getStorageConfig()
            ?: return AppResult.Error("Storage is not configured yet. Please select a Documents folder first.")

        val parentUri: Uri = if (parentId == null) {
            // Major folder: parent is app root directory inside treeUri
            val rootResult = safStorageManager.getOrCreateAppRootDirectory(Uri.parse(config.treeUri))
            if (rootResult is AppResult.Error) {
                return AppResult.Error(rootResult.message, rootResult.cause)
            }
            (rootResult as AppResult.Success).data.uri
        } else {
            // Subfolder: parent is existing folder entity
            val parentEntity = folderDao.getFolderById(parentId)
                ?: return AppResult.Error("Parent folder not found in database.")
            Uri.parse(parentEntity.documentUri)
        }

        // Create directory in SAF
        val dirResult = safStorageManager.createDirectory(parentUri, name)
        if (dirResult is AppResult.Error) {
            return AppResult.Error(dirResult.message, dirResult.cause)
        }
        val createdDoc = (dirResult as AppResult.Success).data

        // Save folder metadata in Room
        val entity = FolderEntity(
            name = createdDoc.name ?: name,
            parentId = parentId,
            documentId = createdDoc.uri.toString(),
            documentUri = createdDoc.uri.toString(),
            createdAt = System.currentTimeMillis()
        )
        val insertedId = folderDao.insertFolder(entity)

        return AppResult.Success(
            entity.copy(id = insertedId).toDomain(subfolders = 0, files = 0)
        )
    }

    suspend fun getReadablePath(folderId: Long): String {
        val segments = mutableListOf<String>()
        var currentId: Long? = folderId
        while (currentId != null) {
            val folder = folderDao.getFolderById(currentId) ?: break
            segments.add(0, folder.name)
            currentId = folder.parentId
        }
        return if (segments.isEmpty()) {
            "Documents / ${SafStorageManager.ROOT_DIRECTORY_NAME}"
        } else {
            "Documents / ${SafStorageManager.ROOT_DIRECTORY_NAME} / " + segments.joinToString(" / ")
        }
    }

    private fun FolderEntity.toDomain(subfolders: Int, files: Int): FolderItem {
        return FolderItem(
            id = id,
            name = name,
            parentId = parentId,
            documentId = documentId,
            documentUri = documentUri,
            createdAt = createdAt,
            subfoldersCount = subfolders,
            filesCount = files
        )
    }
}

