package com.example.intelligentfileorganizer.navigation

sealed class Screen(val route: String) {
    object Welcome : Screen("welcome")
    object Home : Screen("home")
    object FileReview : Screen("file_review")
    object FolderSelection : Screen("folder_selection")
    object SaveSuccess : Screen("save_success")
    object FileBrowser : Screen("file_browser")
}

