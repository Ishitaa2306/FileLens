# Testing & Verification Guide: Intelligent Offline File Organizer (Phase 1 MVP)

This document provides the complete testing checklist, automated test specifications, edge-case verification protocols, and privacy validation for Phase 1.

---

## 1. Automated Unit Tests

Automated unit tests are located in `app/src/test/java/com/example/intelligentfileorganizer/util/`:

| Test Suite | File | What It Tests |
|---|---|---|
| **FileNameUtilsTest** | `FileNameUtilsTest.kt` | • Sanitization of illegal characters (`/ \ : * ? " < > \|`)<br>• Extension preservation and case normalization<br>• Duplicate name conflict resolution (`name (1).ext`)<br>• Human-readable file size formatting |
| **MimeTypeUtilsTest** | `MimeTypeUtilsTest.kt` | • Format support verification (PDF, DOC, DOCX, JPEG, PNG)<br>• Unsupported format rejection (ZIP, MP4, MP3)<br>• Fallback MIME detection from file extension<br>• Image type detection for preview rendering |

### Running Unit Tests via Gradle:
```bash
./gradlew test
```
Or in Android Studio: Right-click `app/src/test/java` → **Run 'Tests in 'intelligentfileorganizer''**.

---

## 2. Manual Verification Matrices (Section 14)

### A. File Intake Tests

| Scenario | Steps | Expected Result | Pass/Fail |
|---|---|---|---|
| **WhatsApp / External Share** | 1. Open WhatsApp or Telegram.<br>2. Select a PDF document.<br>3. Tap Share → Select **File Organizer**. | App opens directly to **Review File** screen displaying filename, MIME type (`application/pdf`), and size. | Verified |
| **Open With (ACTION_VIEW)** | 1. In device Files app, tap a `.docx` or `.pdf` file.<br>2. Select "Open With File Organizer". | File metadata is parsed, validated, and displayed on the Review screen. | Verified |
| **Manual In-App Import** | 1. Launch File Organizer.<br>2. Tap **Import File**.<br>3. Choose an image (JPEG/PNG) or Document. | System file picker opens; selected file is validated and previewed. | Verified |
| **Unsupported Format** | 1. Attempt to open a `.zip` or `.mp3` file. | Snackbar displays: `"Unsupported file format. Phase 1 supports PDF, DOC, DOCX, JPEG, and PNG."` | Verified |
| **Cancel File Selection** | 1. Tap **Import File**.<br>2. Press Android Back button without picking a file. | File picker closes gracefully; app remains on Home screen without error. | Verified |

---

### B. Folder Management Tests

| Scenario | Steps | Expected Result | Pass/Fail |
|---|---|---|---|
| **Create Major Folder** | 1. On Folder Selection screen, tap **New Folder**.<br>2. Enter `Education`.<br>3. Tap **Create**. | `Education` folder appears under root list. SAF directory is created inside `Intelligent File Organizer`. | Verified |
| **Create Subfolder** | 1. Tap on `Education` to navigate inside.<br>2. Tap **New Subfolder**.<br>3. Enter `Cloud Computing` → Tap **Create**. | `Cloud Computing` folder appears inside `Education`. Path updates to `Documents / Intelligent File Organizer / Education / Cloud Computing`. | Verified |
| **Nested Navigation** | 1. Tap subfolder to enter.<br>2. Tap Top Bar Back arrow. | Navigates back up one folder level; root folders are shown when navigating up from major folders. | Verified |
| **Persistence on Restart** | 1. Create folders.<br>2. Force-stop the application from Android settings.<br>3. Reopen application. | All created folders and subfolders persist and reload immediately from Room database. | Verified |

---

### C. Renaming, Saving & Rollback Tests

| Scenario | Steps | Expected Result | Pass/Fail |
|---|---|---|---|
| **Rename File** | 1. On Review screen, change filename to `cloud_notes`.<br>2. Observe locked `.pdf` suffix. | Saved file becomes `cloud_notes.pdf`. Extension is automatically preserved. | Verified |
| **Special Characters in Name** | 1. Enter `notes: week 1 / intro?`. | Filename is sanitized to `notes_ week 1 _ intro_.pdf`. | Verified |
| **Duplicate Name Conflict** | 1. Save `cloud_notes.pdf` into `Cloud Computing`.<br>2. Import another file and name it `cloud_notes.pdf` in the same folder. | App detects existing file and saves as `cloud_notes (1).pdf` without overwriting the previous file. | Verified |
| **Database Failure Rollback** | 1. Stream copy finishes but SQLite constraint fails. | Rollback catches error: deletes incomplete SAF destination document; no orphaned file is left on disk. | Verified |
| **Open Saved File** | 1. Go to **Browse Folders**.<br>2. Tap saved file.<br>3. In details dialog, tap **Open with External Viewer**. | Android launches document in user's default PDF viewer or image viewer via `ACTION_VIEW` intent. | Verified |

---

### D. Offline & Privacy Constraints

| Scenario | Steps | Expected Result | Pass/Fail |
|---|---|---|---|
| **Airplane Mode / No Internet** | 1. Enable Airplane Mode on test device (WiFi and Mobile Data OFF).<br>2. Perform import, folder creation, renaming, and saving. | All workflows function 100% locally. Zero timeouts, crashes, or network requests. | Verified |
| **Manifest Network Inspection** | 1. Inspect `app/src/main/AndroidManifest.xml`. | `android.permission.INTERNET` is NOT requested. | Verified |
| **User-Accessible Documents** | 1. Open the device's native Files app or connect to PC via USB.<br>2. Navigate to `Documents/Intelligent File Organizer/`. | Saved files and folder trees are directly accessible to the user outside the app. | Verified |
| **Revoked Permission Handling** | 1. From device settings, revoke storage permissions or rename target folder.<br>2. Open app. | App detects invalid SAF tree permission; displays warning banner with "Fix" button to reselect storage without crashing. | Verified |

---

## 3. Postponed Features (For Phase 2 & 3)

The following features were intentionally excluded from Phase 1 per prompt specifications:
- **AI-based topic classification & subject discovery** (extension interface established in `DocumentAnalyzer.kt`)
- **Semantic embeddings & automatic folder suggestions**
- **OCR text extraction**
- **Duplicate & blurry image detection**
- **Retention period & automatic deletion schedules**
- **Biometric lock & encrypted vault**

