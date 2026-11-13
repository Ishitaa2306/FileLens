package com.example.intelligentfileorganizer.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "storage_config")
data class StorageConfigEntity(
    @PrimaryKey
    val id: Int = 1,
    val treeUri: String,
    val isConfigured: Boolean,
    val updatedAt: Long = System.currentTimeMillis()
)

