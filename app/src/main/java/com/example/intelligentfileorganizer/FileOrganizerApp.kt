package com.example.intelligentfileorganizer

import android.app.Application
import com.example.intelligentfileorganizer.data.local.OrganizerDatabase
import com.example.intelligentfileorganizer.data.repository.FileRepository
import com.example.intelligentfileorganizer.data.repository.FolderRepository
import com.example.intelligentfileorganizer.data.repository.StorageRepository
import com.example.intelligentfileorganizer.domain.analyzer.DocumentAnalyzer
import com.example.intelligentfileorganizer.domain.analyzer.OcrDocumentAnalyzer
import com.example.intelligentfileorganizer.storage.FileMetadataReader
import com.example.intelligentfileorganizer.storage.IncomingFileHandler
import com.example.intelligentfileorganizer.storage.PdfPreviewGenerator
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

    val pdfPreviewGenerator: PdfPreviewGenerator by lazy {
        PdfPreviewGenerator(application)
    }

    val incomingFileHandler: IncomingFileHandler by lazy {
        IncomingFileHandler(application, fileMetadataReader)
    }

    private val documentAnalyzerDelegate = lazy<DocumentAnalyzer> {
        OcrDocumentAnalyzer(application)
    }

    val documentAnalyzer: DocumentAnalyzer by documentAnalyzerDelegate

    fun releaseDocumentAnalyzer() {
        if (documentAnalyzerDelegate.isInitialized()) {
            (documentAnalyzerDelegate.value as? OcrDocumentAnalyzer)?.close()
        }
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

    override fun onTerminate() {
        super.onTerminate()
        if (::container.isInitialized) {
            container.releaseDocumentAnalyzer()
        }
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= TRIM_MEMORY_UI_HIDDEN && ::container.isInitialized) {
            container.releaseDocumentAnalyzer()
        }
    }
}

