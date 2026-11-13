package com.example.intelligentfileorganizer.data.repository

import android.net.Uri
import com.example.intelligentfileorganizer.data.local.dao.StorageConfigDao
import com.example.intelligentfileorganizer.data.local.entity.StorageConfigEntity
import com.example.intelligentfileorganizer.domain.model.StorageConfig
import com.example.intelligentfileorganizer.storage.SafStorageManager
import com.example.intelligentfileorganizer.util.AppResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class StorageRepository(
    private val storageConfigDao: StorageConfigDao,
    private val safStorageManager: SafStorageManager
) {

    fun observeStorageConfig(): Flow<StorageConfig?> {
        return storageConfigDao.observeStorageConfig().map { entity ->
            entity?.let {
                StorageConfig(
                    id = it.id,
                    treeUri = it.treeUri,
                    isConfigured = it.isConfigured,
                    updatedAt = it.updatedAt
                )
            }
        }
    }

    suspend fun getStorageConfig(): StorageConfig? {
        val entity = storageConfigDao.getStorageConfig() ?: return null
        return StorageConfig(
            id = entity.id,
            treeUri = entity.treeUri,
            isConfigured = entity.isConfigured,
            updatedAt = entity.updatedAt
        )
    }

    suspend fun setupStorage(treeUri: Uri): AppResult<StorageConfig> {
        // 1. Persist permissions
        val hasPersisted = safStorageManager.takePersistablePermissions(treeUri)
        if (!hasPersisted) {
            return AppResult.Error("Could not persist access permissions for the selected directory.")
        }

        // 2. Ensure the "Intelligent File Organizer" root directory exists in SAF
        val rootDirResult = safStorageManager.getOrCreateAppRootDirectory(treeUri)
        if (rootDirResult is AppResult.Error) {
            return AppResult.Error(rootDirResult.message, rootDirResult.cause)
        }

        // 3. Save configuration in database
        val entity = StorageConfigEntity(
            id = 1,
            treeUri = treeUri.toString(),
            isConfigured = true,
            updatedAt = System.currentTimeMillis()
        )
        storageConfigDao.insertOrUpdate(entity)

        return AppResult.Success(
            StorageConfig(
                id = entity.id,
                treeUri = entity.treeUri,
                isConfigured = entity.isConfigured,
                updatedAt = entity.updatedAt
            )
        )
    }

    suspend fun verifyPermission(): Boolean {
        val config = getStorageConfig() ?: return false
        val treeUri = Uri.parse(config.treeUri)
        return safStorageManager.hasValidPermission(treeUri)
    }

    suspend fun clearStorageConfig() {
        storageConfigDao.clearConfig()
    }
}

