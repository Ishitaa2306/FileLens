package com.example.intelligentfileorganizer.viewmodel

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.intelligentfileorganizer.FileOrganizerApp

object AppViewModelProvider {
    val Factory: ViewModelProvider.Factory = viewModelFactory {
        initializer {
            val app = fileOrganizerApp()
            StorageSetupViewModel(app.container.storageRepository)
        }
        initializer {
            val app = fileOrganizerApp()
            HomeViewModel(
                storageRepository = app.container.storageRepository,
                fileRepository = app.container.fileRepository,
                incomingFileHandler = app.container.incomingFileHandler
            )
        }
        initializer {
            val app = fileOrganizerApp()
            FileReviewViewModel(
                pdfPreviewGenerator = app.container.pdfPreviewGenerator,
                documentAnalyzer = app.container.documentAnalyzer
            )
        }
        initializer {
            val app = fileOrganizerApp()
            FolderViewModel(
                folderRepository = app.container.folderRepository,
                fileRepository = app.container.fileRepository
            )
        }
        initializer {
            val app = fileOrganizerApp()
            FileBrowserViewModel(
                folderRepository = app.container.folderRepository,
                fileRepository = app.container.fileRepository,
                storageRepository = app.container.storageRepository
            )
        }
    }
}

fun CreationExtras.fileOrganizerApp(): FileOrganizerApp =
    (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as FileOrganizerApp)

