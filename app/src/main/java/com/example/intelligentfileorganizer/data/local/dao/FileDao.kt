package com.example.intelligentfileorganizer.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.intelligentfileorganizer.data.local.entity.FileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FileDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFile(file: FileEntity): Long

    @Query("SELECT * FROM files WHERE id = :id LIMIT 1")
    suspend fun getFileById(id: Long): FileEntity?

    @Query("SELECT * FROM files WHERE documentUri = :documentUri LIMIT 1")
    suspend fun getFileByDocumentUri(documentUri: String): FileEntity?

    @Query("SELECT * FROM files WHERE folderId = :folderId ORDER BY storedName COLLATE NOCASE ASC")
    fun observeFilesByFolder(folderId: Long): Flow<List<FileEntity>>

    @Query("SELECT * FROM files WHERE folderId = :folderId ORDER BY storedName COLLATE NOCASE ASC")
    suspend fun getFilesByFolder(folderId: Long): List<FileEntity>

    @Query("SELECT * FROM files ORDER BY importedAt DESC LIMIT :limit")
    fun observeRecentFiles(limit: Int = 10): Flow<List<FileEntity>>

    @Query("SELECT * FROM files ORDER BY importedAt DESC")
    fun observeAllFiles(): Flow<List<FileEntity>>

    @Query("SELECT * FROM files ORDER BY importedAt DESC")
    suspend fun getAllFiles(): List<FileEntity>

    @Update
    suspend fun updateFile(file: FileEntity)

    @Query("DELETE FROM files WHERE id = :id")
    suspend fun deleteFileById(id: Long)

    @Query("SELECT storedName FROM files WHERE folderId = :folderId")
    suspend fun getExistingFileNamesInFolder(folderId: Long): List<String>
}

