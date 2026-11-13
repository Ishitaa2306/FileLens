# Stitch UI Prompts & Integration Guide for Intelligent File Organizer

This guide provides:
1. Ready-to-use prompts for **Google Stitch** (or any AI design tool) to generate polished, modern Material 3 Jetpack Compose UI screens.
2. Step-by-step instructions for plugging Stitch-generated UI into the project without touching the core storage or database logic.

---

## 1. How to Plug in Stitch UI into this Project

The architecture is built on **State Hoisting** and **Clean MVVM separation**:
- **Core Logic & Persistence**: Handled by `SafStorageManager`, `OrganizerDatabase`, `FileRepository`, and `FolderRepository`.
- **View Layer**: Pure Jetpack Compose functions that consume immutable `UiState` and emit events via lambdas.

### The Plug-in Contract:
To replace any screen with a Stitch-generated UI:
1. Open the target screen file in `app/src/main/java/com/example/intelligentfileorganizer/ui/screens/`.
2. Keep the existing function signature (the `uiState` parameters and callback lambdas).
3. Replace the internal layout code with your Stitch-generated composables.

#### Example (Home Screen):
```kotlin
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToSetup: () -> Unit,
    onNavigateToReview: (IncomingMetadata) -> Unit,
    onNavigateToBrowser: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val recentFiles by viewModel.recentFiles.collectAsStateWithLifecycle()

    // Plug in your Stitch Composable here:
    StitchHomeScreenContent(
        isStorageConfigured = uiState.isStorageConfigured,
        recentFiles = recentFiles,
        onImportFileClicked = { /* launch file picker */ },
        onBrowseFoldersClicked = onNavigateToBrowser,
        onSetupStorageClicked = onNavigateToSetup
    )
}
```

---

## 2. Prompts for Stitch UI Generation

You can copy and paste the prompts below into Stitch or any UI generation tool:

### Prompt 1: Welcome & Storage Onboarding Screen
```text
Role: Senior Android UI/UX Designer
Platform: Android Jetpack Compose, Material 3
Theme: Clean, modern, distraction-free productivity app.
Primary Palette: Deep Teal (#0C7489), Mint Accent (#04B494), Sand Background (#F7F9FA), Surface White (#FFFFFF), Text (#1A202C).

Design a Welcome and Storage Onboarding screen for an offline file organizer app:
1. Header: Elegant app icon with rounded circular background, bold title "Intelligent File Organizer", and friendly subtitle explaining offline document storage.
2. Information Card: A soft-bordered surface highlighting "Privacy & Offline Architecture", explaining that files are saved in the user's personal Documents directory and never uploaded to any cloud or external servers.
3. Feature Badges: 3 clean horizontal badges: "100% Offline", "Direct User Storage", "Zero Tracking".
4. Call to Action: A high-contrast primary pill button "Select Storage Folder" with an open folder icon.
5. Status: Subtle indicator showing permission status (Configured vs Not Configured).
Ensure responsive padding, edge-to-edge layout, and accessibility support.
```

---

### Prompt 2: Home Dashboard Screen
```text
Role: Senior Android UI/UX Designer
Platform: Android Jetpack Compose, Material 3
Theme: Clean, modern, distraction-free productivity app.

Design a Home Dashboard screen for "Intelligent File Organizer":
1. Top Bar: Title "File Organizer" with clean typography.
2. Storage Status Banner: Card showing current storage location ("Documents / Intelligent File Organizer") with an active green status pill. If unconfigured, show a warning badge with a "Configure" button.
3. Quick Action Row: Two side-by-side prominent cards:
   - "Import File": Primary teal button with "+" icon to open file picker.
   - "Browse Folders": Clean outlined card with folder icon.
4. Section Header: "Recently Saved Files" with item count.
5. Recent Files List:
   - Each item is a card showing file type icon (PDF in crimson, Word in navy, Image in teal), filename in semi-bold, subfolder path badge, file size, and timestamp.
6. Empty State: When no files are saved yet, display a subtle illustration with text: "No files saved yet. Import a file or share one from WhatsApp to get started."
```

---

### Prompt 3: Incoming File Review & Rename Screen
```text
Role: Senior Android UI/UX Designer
Platform: Android Jetpack Compose, Material 3

Design an Incoming File Review screen displayed when a file is received via Android Share ("Open With") or manual import:
1. Top Bar: "Review File" with back navigation.
2. File Information Card:
   - Preview Box: If image, show an aspect-fit rounded image preview. If PDF/Doc, show an icon with extension badge (e.g., [PDF] in bold pill).
   - Details: Original filename, MIME type, file size in KB/MB.
3. Rename Section:
   - Outlined text field pre-filled with the base filename.
   - Fixed trailing extension suffix badge (e.g., ".pdf") that stays locked so the user cannot accidentally corrupt the extension.
   - Helper text: "File will be saved with this name. Special characters are automatically sanitized."
4. Action Buttons (Bottom Bar):
   - "Cancel" button (outlined).
   - "Choose Folder" button (primary filled, leading folder icon) to proceed to folder selection.
```

---

### Prompt 4: Destination Folder Selection Screen
```text
Role: Senior Android UI/UX Designer
Platform: Android Jetpack Compose, Material 3

Design a Folder Selection & Navigation screen:
1. Top Bar: "Choose Destination Folder" with back arrow that steps up one folder level.
2. Breadcrumbs Bar: Interactive path bar (e.g., "Documents > Intelligent File Organizer > Education > Cloud Computing") with rounded chip styling.
3. Folder List:
   - Modern folder cards showing folder icon, folder name, subfolder count badge, and a chevron arrow to navigate inside.
4. Empty Folder State: If a folder has no subfolders, show a friendly card indicating: "You can save directly into this folder or create a subfolder below."
5. Persistent Bottom Bar:
   - "New Folder" / "New Subfolder" outlined button with folder-plus icon.
   - "Save Here" filled primary button with checkmark icon, disabled until a valid destination folder is open.
```

---

### Prompt 5: Save Success Screen
```text
Role: Senior Android UI/UX Designer
Platform: Android Jetpack Compose, Material 3

Design a Save Success confirmation screen:
1. Centered Success Hero: Large circular badge with an animated checkmark icon in vibrant teal.
2. Headline: "File Saved Successfully!" with a clean, celebratory subhead.
3. Summary Card:
   - File Name: Bold typography with file icon.
   - Saved Path: Full readable hierarchy path ("Documents / Intelligent File Organizer / Education / Cloud Computing").
   - Timestamp and file size badge.
4. Action Buttons:
   - Primary: "View in Folder" (opens the file browser inside this folder).
   - Secondary: "Import Another File" (outlined button).
   - Tertiary: "Go to Home" (text button).
```

---

### Prompt 6: Storage & File Browser Screen
```text
Role: Senior Android UI/UX Designer
Platform: Android Jetpack Compose, Material 3

Design a File & Folder Browser screen:
1. Top Bar: Current folder name with navigation back button.
2. Breadcrumb path indicator across the top.
3. Folders Section: List or 2-column grid of subfolders with folder item counts.
4. Files Section:
   - List of saved files inside the current folder.
   - Each file row displays: type icon, filename, human-readable size, import date, and an overflow menu (three dots) with "Rename" and "Delete" options.
5. Rename Dialog: Clean popup dialog with single-line text input and Save/Cancel buttons.
```

