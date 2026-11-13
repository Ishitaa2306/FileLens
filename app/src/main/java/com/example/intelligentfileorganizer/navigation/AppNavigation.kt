package com.example.intelligentfileorganizer.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.intelligentfileorganizer.domain.model.FileItem
import com.example.intelligentfileorganizer.storage.IncomingMetadata
import com.example.intelligentfileorganizer.ui.screens.FileBrowserScreen
import com.example.intelligentfileorganizer.ui.screens.FileReviewScreen
import com.example.intelligentfileorganizer.ui.screens.FolderSelectionScreen
import com.example.intelligentfileorganizer.ui.screens.HomeScreen
import com.example.intelligentfileorganizer.ui.screens.SaveSuccessScreen
import com.example.intelligentfileorganizer.ui.screens.WelcomeScreen
import com.example.intelligentfileorganizer.viewmodel.AppViewModelProvider
import com.example.intelligentfileorganizer.viewmodel.FileBrowserViewModel
import com.example.intelligentfileorganizer.viewmodel.FileReviewViewModel
import com.example.intelligentfileorganizer.viewmodel.FolderViewModel
import com.example.intelligentfileorganizer.viewmodel.HomeViewModel
import com.example.intelligentfileorganizer.viewmodel.StorageSetupViewModel

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController(),
    incomingUri: Uri? = null,
    onIncomingUriConsumed: () -> Unit = {}
) {
    val homeViewModel: HomeViewModel = viewModel(factory = AppViewModelProvider.Factory)
    val storageSetupViewModel: StorageSetupViewModel = viewModel(factory = AppViewModelProvider.Factory)
    val fileReviewViewModel: FileReviewViewModel = viewModel(factory = AppViewModelProvider.Factory)
    val folderViewModel: FolderViewModel = viewModel(factory = AppViewModelProvider.Factory)
    val fileBrowserViewModel: FileBrowserViewModel = viewModel(factory = AppViewModelProvider.Factory)

    var currentIncomingMetadata by remember { mutableStateOf<IncomingMetadata?>(null) }
    var currentDesiredFileName by remember { mutableStateOf("") }
    var lastSavedFile by remember { mutableStateOf<FileItem?>(null) }
    var lastSavedPath by remember { mutableStateOf("") }

    // If an incoming URI was passed via Share or Open With
    LaunchedEffect(incomingUri) {
        if (incomingUri != null) {
            homeViewModel.handleIncomingUri(incomingUri)
            onIncomingUriConsumed()
        }
    }

    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Welcome.route) {
            WelcomeScreen(
                viewModel = storageSetupViewModel,
                onStorageConfigured = {
                    homeViewModel.refreshStorageStatus()
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Welcome.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Home.route) {
            HomeScreen(
                viewModel = homeViewModel,
                onNavigateToSetup = {
                    navController.navigate(Screen.Welcome.route)
                },
                onNavigateToReview = { metadata ->
                    currentIncomingMetadata = metadata
                    fileReviewViewModel.setIncomingMetadata(metadata)
                    navController.navigate(Screen.FileReview.route)
                },
                onNavigateToBrowser = {
                    fileBrowserViewModel.loadRoot()
                    navController.navigate(Screen.FileBrowser.route)
                }
            )
        }

        composable(Screen.FileReview.route) {
            FileReviewScreen(
                viewModel = fileReviewViewModel,
                onCancel = {
                    navController.popBackStack(Screen.Home.route, inclusive = false)
                },
                onProceedToFolderSelect = { finalFileName ->
                    currentDesiredFileName = finalFileName
                    folderViewModel.loadRootFolders()
                    navController.navigate(Screen.FolderSelection.route)
                }
            )
        }

        composable(Screen.FolderSelection.route) {
            val metadata = currentIncomingMetadata
            if (metadata != null) {
                FolderSelectionScreen(
                    viewModel = folderViewModel,
                    incomingMetadata = metadata,
                    desiredFileName = currentDesiredFileName,
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onSaveSuccess = { savedFile, folderPath ->
                        lastSavedFile = savedFile
                        lastSavedPath = folderPath
                        navController.navigate(Screen.SaveSuccess.route) {
                            popUpTo(Screen.Home.route) { inclusive = false }
                        }
                    }
                )
            }
        }

        composable(Screen.SaveSuccess.route) {
            val saved = lastSavedFile
            if (saved != null) {
                SaveSuccessScreen(
                    savedFile = saved,
                    folderPath = lastSavedPath,
                    onBrowseFolder = {
                        fileBrowserViewModel.loadRoot()
                        navController.navigate(Screen.FileBrowser.route) {
                            popUpTo(Screen.Home.route) { inclusive = false }
                        }
                    },
                    onImportAnother = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Home.route) { inclusive = true }
                        }
                    },
                    onGoHome = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Home.route) { inclusive = true }
                        }
                    }
                )
            }
        }

        composable(Screen.FileBrowser.route) {
            FileBrowserScreen(
                viewModel = fileBrowserViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}

