package com.example.intelligentfileorganizer.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.intelligentfileorganizer.data.local.entity.StorageConfigEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StorageConfigDao {

    @Query("SELECT * FROM storage_config WHERE id = 1 LIMIT 1")
    fun observeStorageConfig(): Flow<StorageConfigEntity?>

    @Query("SELECT * FROM storage_config WHERE id = 1 LIMIT 1")
    suspend fun getStorageConfig(): StorageConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(config: StorageConfigEntity)

    @Query("DELETE FROM storage_config")
    suspend fun clearConfig()
}

