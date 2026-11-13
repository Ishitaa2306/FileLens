# Intelligent Offline File Organizer — Phase 1 MVP

A privacy-first, offline Android application designed to receive, organize, and manage files intelligently using Android's Storage Access Framework (SAF), Jetpack Compose, Room SQLite, and MVVM Clean Architecture.

---

## 1. Project Overview & Phase 1 Scope

This repository contains the complete implementation for **Phase 1 MVP** of the Intelligent Offline File Organizer.

### Implemented Capabilities:
- **First-Launch Storage Onboarding**: Guides the user to select their user-accessible `Documents` directory via SAF (`ACTION_OPEN_DOCUMENT_TREE`).
- **Persisted Tree Permissions**: Persists directory permissions across device reboots and app restarts.
- **Dedicated Root Workspace**: Automatically initializes an `Intelligent File Organizer` root directory inside the user's selected Documents folder.
- **Intent-Based File Intake**:
  - `ACTION_SEND`: Seamlessly receive shared single files from applications like WhatsApp, Telegram, or Gmail.
  - `ACTION_VIEW`: Direct file intake via Android "Open With".
- **In-App Manual File Picker**: `ACTION_OPEN_DOCUMENT` file intake directly from the Home screen.
- **Supported Initial Formats**:
  - PDF: `application/pdf`
  - DOC: `application/msword`
  - DOCX: `application/vnd.openxmlformats-officedocument.wordprocessingml.document`
  - JPEG: `image/jpeg`
  - PNG: `image/png`
- **Metadata Extraction & Preview**: Reads display name, MIME type, file size via `ContentResolver`, and renders basic image previews.
- **Safe Filename Editing**: Allows renaming the file while automatically preserving the file extension and sanitizing illegal filesystem characters.
- **Duplicate Protection**: Automatically resolves name collisions by appending counter suffixes (e.g. `document (1).pdf`) rather than overwriting existing files.
- **User-Controlled Folder Hierarchy**:
  - Create Major Folders at root.
  - Create Nested Subfolders inside any major or parent folder.
  - Navigate up and down the folder tree with readable breadcrumbs (e.g. `Documents / Intelligent File Organizer / Education / Cloud Computing`).
- **Atomic Two-Phase Persistence**:
  1. Stream copy from source URI to SAF destination document.
  2. Insert file metadata into Room database.
  3. Automatic rollback (deletion of incomplete destination document) if database insertion encounters an error.
- **Local Persistence**: Stores folder, file, and configuration metadata in local SQLite via Room (`organizer.db`).
- **File Browser**: Browse folders, view saved files with metadata, rename files, and delete records.
- **Zero Internet Requirement**: Completely offline; no network permissions (`android.permission.INTERNET` is not requested).
- **Phase 2 Extensibility**: Clean `DocumentAnalyzer` architectural interface ready for on-device AI/ML topic classification.

---

## 2. Technical Stack & Dependencies

| Layer | Technology |
|---|---|
| **Language** | Kotlin 2.0.21 |
| **UI Framework** | Jetpack Compose (BOM 2024.10.01) + Material 3 |
| **Architecture** | MVVM + Repository Pattern + Clean Domain Interfaces |
| **Local Database** | Room 2.6.1 (with KSP) |
| **Storage Engine** | Android Storage Access Framework (SAF) + `DocumentFile` |
| **Image Loading** | Coil Compose 2.7.0 (Local content URI previews) |
| **Minimum SDK** | API 26 (Android 8.0) |
| **Target / Compile SDK** | API 35 (Android 15) |
| **JDK Requirement** | Java 17 |

---

## 3. Project File Structure

```
app/src/main/
├── AndroidManifest.xml
├── java/com/example/intelligentfileorganizer/
│   ├── FileOrganizerApp.kt             # Application class & AppContainer (DI)
│   ├── MainActivity.kt                 # Intent intake (SEND/VIEW) & Compose Host
│   │
│   ├── navigation/
│   │   ├── Screen.kt                   # Sealed destination routes
│   │   └── AppNavigation.kt            # NavHost graph & state orchestration
│   │
│   ├── ui/
│   │   ├── theme/
│   │   │   ├── Color.kt                # Custom Teal & Sand palette
│   │   │   ├── Theme.kt                # Material 3 Light/Dark setup
│   │   │   └── Type.kt                 # Responsive typography scale
│   │   ├── components/
│   │   │   ├── AppTopBar.kt            # Consistent navigation header
│   │   │   ├── CreateFolderDialog.kt   # Dialog for folder/subfolder creation
│   │   │   ├── FileInfoCard.kt         # Card with metadata & image preview
│   │   │   ├── FolderItemView.kt       # Card representing a directory item
│   │   │   └── LoadingIndicator.kt     # Safe asynchronous progress feedback
│   │   └── screens/
│   │       ├── WelcomeScreen.kt        # SAF onboarding & directory grant
│   │       ├── HomeScreen.kt           # Recent files, storage chip, picker
│   │       ├── FileReviewScreen.kt     # Metadata, editable name, preview
│   │       ├── FolderSelectionScreen.kt# Breadcrumb navigation & save trigger
│   │       ├── SaveSuccessScreen.kt    # Completion confirmation & folder location
│   │       └── FileBrowserScreen.kt    # Folder and saved file manager
│   │
│   ├── viewmodel/
│   │   ├── AppViewModelProvider.kt     # ViewModelProvider.Factory
│   │   ├── StorageSetupViewModel.kt    # Storage grant & verification
│   │   ├── HomeViewModel.kt            # Home dashboard & incoming URI handling
│   │   ├── FileReviewViewModel.kt      # File review state & rename logic
│   │   ├── FolderViewModel.kt          # Folder creation, traversal & save execution
│   │   └── FileBrowserViewModel.kt     # Saved items browser & file renaming
│   │
│   ├── data/
│   │   ├── local/
│   │   │   ├── OrganizerDatabase.kt    # Room database (organizer.db)
│   │   │   ├── entity/
│   │   │   │   ├── StorageConfigEntity.kt
│   │   │   │   ├── FolderEntity.kt
│   │   │   │   └── FileEntity.kt
│   │   │   └── dao/
│   │   │       ├── StorageConfigDao.kt
│   │   │       ├── FolderDao.kt
│   │   │       └── FileDao.kt
│   │   └── repository/
│   │       ├── StorageRepository.kt
│   │       ├── FolderRepository.kt
│   │       └── FileRepository.kt
│   │
│   ├── domain/
│   │   ├── model/
│   │   │   ├── StorageConfig.kt
│   │   │   ├── FolderItem.kt
│   │   │   └── FileItem.kt
│   │   └── analyzer/
│   │       ├── DocumentAnalyzer.kt      # Phase 2 extension interface
│   │       └── DocumentAnalysisResult.kt# Phase 2 result contract
│   │
│   ├── storage/
│   │   ├── SafStorageManager.kt        # SAF directory creation, stream copy & rename
│   │   ├── FileMetadataReader.kt       # ContentResolver metadata query
│   │   └── IncomingFileHandler.kt      # URI accessibility & MIME type validation
│   │
│   └── util/
│       ├── Result.kt                   # AppResult sealed interface
│       ├── MimeTypeUtils.kt            # Supported format detection & MIME helpers
│       └── FileNameUtils.kt            # Conflict resolution & path sanitization
│
└── res/
    ├── drawable/                       # Vector assets & adaptive icons
    ├── mipmap-anydpi-v26/              # Adaptive launcher icons
    ├── values/                         # strings.xml, colors.xml, themes.xml
    └── xml/                            # Backup & data extraction rules
```

---

## 4. Setup, Build & Run in Android Studio

### Prerequisites
1. **Android Studio**: Android Studio Ladybug (2024.2.1) or Hedgehog / Iguana / Jellyfish / Koala.
2. **Android SDK**: Android 15 (API 35) or Android 14 (API 34). Minimum SDK supported is API 26 (Android 8.0).
3. **JDK**: JDK 17 (bundled in Android Studio under Settings > Build, Execution, Deployment > Build Tools > Gradle > Gradle JDK).

### Step-by-Step Instructions:
1. Launch Android Studio.
2. Select **Open** and choose the `c:\Filelens` project directory.
3. Allow Gradle to perform an initial sync.
4. Connect an Android device or launch an Android Virtual Device (AVD with API 26+).
5. Click **Run 'app'** (`Shift + F10`).

---

## 5. End-to-End Verification Scenario

To test the acceptance criteria:
1. **Launch App**: Open the app. The Welcome screen appears.
2. **Grant Storage**: Click `Select Storage Folder` and choose the Android `Documents` directory. The app creates the `Intelligent File Organizer` folder.
3. **Receive a File via Share**:
   - Open WhatsApp / Gmail / File Manager, select a PDF, and tap **Share**.
   - Select **Intelligent File Organizer** from the Android system share sheet.
4. **Review & Rename**:
   - The app opens with file details (size, MIME type).
   - Change the filename to a custom name (e.g., `cloud_computing_notes`). The `.pdf` extension is preserved.
5. **Create Major Folder**:
   - Tap `Choose Folder`.
   - Tap `New Folder`, enter `Education`, and tap `Create`.
6. **Create Subfolder**:
   - Tap `Education` to navigate inside it.
   - Tap `New Subfolder`, enter `Cloud Computing`, and tap `Create`.
7. **Save**:
   - Tap `Cloud Computing` to open it.
   - Tap `Save Here`.
8. **Confirmation**:
   - The success screen confirms the saved filename and location:
     `Documents / Intelligent File Organizer / Education / Cloud Computing`.
9. **Persistence Check**:
   - Force close the application and reopen it.
   - The home screen displays the newly saved file in **Recently Saved Files**, and **Browse Folders** reflects the created folder tree.

