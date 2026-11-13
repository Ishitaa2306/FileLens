package com.example.intelligentfileorganizer

import android.app.Application
import com.example.intelligentfileorganizer.data.local.OrganizerDatabase
import com.example.intelligentfileorganizer.data.repository.FileRepository
import com.example.intelligentfileorganizer.data.repository.FolderRepository
import com.example.intelligentfileorganizer.data.repository.StorageRepository
import com.example.intelligentfileorganizer.storage.FileMetadataReader
import com.example.intelligentfileorganizer.storage.IncomingFileHandler
import com.example.intelligentfileorganizer.storage.SafStorageManager

class AppContainer(private val application: Application) {
    val database: OrganizerDatabase by lazy {
        OrganizerDatabase.getInstance(application)
    }

    val safStorageManager: SafStorageManager by lazy {
        SafStorageManager(application)
    }

    val fileMetadataReader: FileMetadataReader by lazy {
        FileMetadataReader(application)
    }

    val incomingFileHandler: IncomingFileHandler by lazy {
        IncomingFileHandler(application, fileMetadataReader)
    }

    val storageRepository: StorageRepository by lazy {
        StorageRepository(
            storageConfigDao = database.storageConfigDao(),
            safStorageManager = safStorageManager
        )
    }

    val folderRepository: FolderRepository by lazy {
        FolderRepository(
            folderDao = database.folderDao(),
            storageRepository = storageRepository,
            safStorageManager = safStorageManager
        )
    }

    val fileRepository: FileRepository by lazy {
        FileRepository(
            fileDao = database.fileDao(),
            folderDao = database.folderDao(),
            safStorageManager = safStorageManager
        )
    }
}

class FileOrganizerApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

