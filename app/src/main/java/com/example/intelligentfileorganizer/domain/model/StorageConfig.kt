package com.example.intelligentfileorganizer.domain.model

data class StorageConfig(
    val id: Int = 1,
    val treeUri: String,
    val isConfigured: Boolean,
    val updatedAt: Long = System.currentTimeMillis()
)

