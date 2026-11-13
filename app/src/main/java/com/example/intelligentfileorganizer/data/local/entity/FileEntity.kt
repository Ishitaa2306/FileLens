package com.example.intelligentfileorganizer.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "files",
    foreignKeys = [
        ForeignKey(
            entity = FolderEntity::class,
            parentColumns = ["id"],
            childColumns = ["folderId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["folderId"]),
        Index(value = ["documentUri"], unique = true)
    ]
)
data class FileEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val originalName: String,
    val storedName: String,
    val mimeType: String,
    val extension: String,
    val sizeBytes: Long,
    val documentUri: String,
    val documentId: String,
    val folderId: Long,
    val importedAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

