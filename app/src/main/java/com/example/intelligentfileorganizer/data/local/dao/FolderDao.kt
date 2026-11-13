package com.example.intelligentfileorganizer.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.intelligentfileorganizer.data.local.entity.FolderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FolderDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolder(folder: FolderEntity): Long

    @Query("SELECT * FROM folders WHERE id = :id LIMIT 1")
    suspend fun getFolderById(id: Long): FolderEntity?

    @Query("SELECT * FROM folders WHERE documentUri = :documentUri LIMIT 1")
    suspend fun getFolderByDocumentUri(documentUri: String): FolderEntity?

    @Query("SELECT * FROM folders WHERE parentId IS NULL ORDER BY name COLLATE NOCASE ASC")
    fun observeRootFolders(): Flow<List<FolderEntity>>

    @Query("SELECT * FROM folders WHERE parentId = :parentId ORDER BY name COLLATE NOCASE ASC")
    fun observeSubfolders(parentId: Long): Flow<List<FolderEntity>>

    @Query("SELECT * FROM folders WHERE parentId = :parentId ORDER BY name COLLATE NOCASE ASC")
    suspend fun getSubfolders(parentId: Long): List<FolderEntity>

    @Query("SELECT * FROM folders WHERE parentId IS NULL ORDER BY name COLLATE NOCASE ASC")
    suspend fun getRootFolders(): List<FolderEntity>

    @Query("SELECT * FROM folders ORDER BY name COLLATE NOCASE ASC")
    fun observeAllFolders(): Flow<List<FolderEntity>>

    @Query("SELECT * FROM folders ORDER BY name COLLATE NOCASE ASC")
    suspend fun getAllFolders(): List<FolderEntity>

    @Query("SELECT COUNT(*) FROM folders WHERE parentId = :parentId")
    suspend fun countSubfolders(parentId: Long): Int

    @Query("SELECT COUNT(*) FROM files WHERE folderId = :folderId")
    suspend fun countFilesInFolder(folderId: Long): Int

    @Update
    suspend fun updateFolder(folder: FolderEntity)

    @Query("DELETE FROM folders WHERE id = :id")
    suspend fun deleteFolderById(id: Long)
}

