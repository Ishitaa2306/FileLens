package com.example.intelligentfileorganizer.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.intelligentfileorganizer.data.local.dao.FileDao
import com.example.intelligentfileorganizer.data.local.dao.FolderDao
import com.example.intelligentfileorganizer.data.local.dao.StorageConfigDao
import com.example.intelligentfileorganizer.data.local.entity.FileEntity
import com.example.intelligentfileorganizer.data.local.entity.FolderEntity
import com.example.intelligentfileorganizer.data.local.entity.StorageConfigEntity

@Database(
    entities = [
        StorageConfigEntity::class,
        FolderEntity::class,
        FileEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class OrganizerDatabase : RoomDatabase() {

    abstract fun storageConfigDao(): StorageConfigDao
    abstract fun folderDao(): FolderDao
    abstract fun fileDao(): FileDao

    companion object {
        @Volatile
        private var INSTANCE: OrganizerDatabase? = null

        fun getInstance(context: Context): OrganizerDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    OrganizerDatabase::class.java,
                    "organizer.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

