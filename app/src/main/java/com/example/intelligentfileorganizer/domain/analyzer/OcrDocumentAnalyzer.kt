package com.example.intelligentfileorganizer.domain.analyzer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.os.StatFs
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.io.MemoryUsageSetting
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.UUID
import kotlin.math.max
import kotlin.math.roundToInt

class OcrDocumentAnalyzer(
    private val context: Context
) : DocumentAnalyzer {

    init {
        PDFBoxResourceLoader.init(context.applicationContext)
    }

    private val recognizerMutex = Mutex()
    private val recognizerStateLock = Any()
    private var recognizer: TextRecognizer? = null
    private var activeRecognitions = 0
    private var recognizerCloseRequested = false

    override suspend fun analyze(documentUri: Uri, mimeType: String): DocumentAnalysisResult {
        return try {
            when {
                mimeType.equals("application/pdf", ignoreCase = true) -> analyzePdf(documentUri)
                mimeType.startsWith("image/", ignoreCase = true) -> analyzeImage(documentUri)
                else -> DocumentAnalysisResult(
                    status = DocumentAnalysisStatus.UNSUPPORTED_TYPE,
                    errorMessage = "Text extraction is not supported for this file type. DOC/DOCX files can still be saved normally."
                )
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (e: SecurityException) {
            DocumentAnalysisResult(
                status = DocumentAnalysisStatus.PERMISSION_DENIED,
                errorMessage = "The file could not be read because the source permission is no longer available."
            )
        } catch (e: IOException) {
            DocumentAnalysisResult(
                status = DocumentAnalysisStatus.UNREADABLE_FILE,
                errorMessage = "The document could not be read or is corrupted."
            )
        } catch (e: IllegalArgumentException) {
            DocumentAnalysisResult(
                status = DocumentAnalysisStatus.UNREADABLE_FILE,
                errorMessage = "The document could not be decoded."
            )
        } catch (e: OutOfMemoryError) {
            DocumentAnalysisResult(
                status = DocumentAnalysisStatus.UNEXPECTED_ERROR,
                errorMessage = "There is not enough memory to process this document. Try a smaller file."
            )
        } catch (e: Exception) {
            DocumentAnalysisResult(
                status = DocumentAnalysisStatus.UNEXPECTED_ERROR,
                errorMessage = "Text extraction failed unexpectedly."
            )
        }
    }

    fun close() {
        synchronized(recognizerStateLock) {
            if (activeRecognitions == 0) {
                closeRecognizerLocked()
            } else {
                recognizerCloseRequested = true
            }
        }
    }

    private suspend fun analyzeImage(uri: Uri): DocumentAnalysisResult = withContext(Dispatchers.IO) {
        val startMs = System.currentTimeMillis()
        var bitmap: Bitmap? = null
        try {
            currentCoroutineContext().ensureActive()
            val decodedBitmap = decodeBitmapFromUri(uri)
                ?: throw IOException("Image decoding failed")
            bitmap = decodedBitmap
            currentCoroutineContext().ensureActive()
            bitmap = adjustBitmapOrientation(uri, decodedBitmap)
            currentCoroutineContext().ensureActive()
            val text = recognizeBitmap(checkNotNull(bitmap))
            val normalizedResult = normalizeTextBounded(text, MAX_OCR_CHARACTERS)
            val normalized = normalizedResult.text
            if (normalized.isEmpty()) {
                return@withContext DocumentAnalysisResult(
                    extractedText = null,
                    status = DocumentAnalysisStatus.EMPTY,
                    source = DocumentExtractionSource.IMAGE_OCR,
                    characterCount = 0,
                    processingDurationMs = System.currentTimeMillis() - startMs,
                    errorMessage = "No readable text was found."
                )
            }
            return@withContext DocumentAnalysisResult(
                extractedText = normalized,
                status = DocumentAnalysisStatus.SUCCESS,
                source = DocumentExtractionSource.IMAGE_OCR,
                pageCount = 1,
                totalPages = 1,
                truncated = normalizedResult.truncated,
                truncatedByCharacterLimit = normalizedResult.truncated,
                characterCount = normalized.length,
                processingDurationMs = System.currentTimeMillis() - startMs,
                errorMessage = null
            )
        } finally {
            bitmap?.recycle()
        }
    }

    private suspend fun analyzePdf(uri: Uri): DocumentAnalysisResult = withContext(Dispatchers.IO) {
        val startMs = System.currentTimeMillis()
        if (!isPdfWithinSizeLimit(uri)) {
            return@withContext DocumentAnalysisResult(
                status = DocumentAnalysisStatus.FILE_TOO_LARGE,
                errorMessage = "This PDF exceeds the maximum supported size for on-device processing."
            )
        }

        var tempPdfFile: File? = null
        var scratchDirectory: File? = null
        var document: PDDocument? = null
        val extractedText = BoundedPdfTextAccumulator(MAX_OCR_CHARACTERS)
        val pageProvenance = mutableListOf<DocumentPageProvenance>()
        var totalPages = 0
        var processedPages = 0
        var cancellationFailure: CancellationException? = null

        try {
            val pdfFile = copyUriToCacheFile(uri)
            tempPdfFile = pdfFile
            val scratchSpace = createPdfScratchSpace()
            scratchDirectory = scratchSpace.directory
            document = PDDocument.load(pdfFile, scratchSpace.memoryUsageSetting)
            ensureActive()
            totalPages = document.numberOfPages
            val pagePlan = createPdfPageProcessingPlan(totalPages, MAX_PDF_PAGES)

            for (index in 0 until pagePlan.pagesToProcess) {
                ensureActive()
                val embeddedText = extractEmbeddedTextPage(
                    document = document,
                    pageIndex = index,
                    maxCharacters = extractedText.remainingPageCapacity()
                )
                ensureActive()
                if (embeddedText.truncated ||
                    embeddedText.meaningfulCharacterCount >= MIN_EMBEDDED_TEXT_CHARS
                ) {
                    pageProvenance.add(
                        DocumentPageProvenance(
                            pageNumber = index + 1,
                            source = DocumentPageExtractionSource.PDF_TEXT_LAYER
                        )
                    )
                    extractedText.appendPage(embeddedText.text, embeddedText.truncated)
                } else {
                    val pageBitmap = renderPdfPageToBitmap(pdfFile, index)
                    try {
                        if (isVisuallyBlank(pageBitmap)) {
                            pageProvenance.add(
                                DocumentPageProvenance(
                                    pageNumber = index + 1,
                                    source = DocumentPageExtractionSource.BLANK
                                )
                            )
                        } else {
                            pageProvenance.add(
                                DocumentPageProvenance(
                                    pageNumber = index + 1,
                                    source = DocumentPageExtractionSource.PDF_OCR
                                )
                            )
                            val pageText = normalizeTextBounded(
                                rawText = recognizeBitmap(pageBitmap),
                                maxCharacters = extractedText.remainingPageCapacity()
                            )
                            extractedText.appendPage(pageText.text, pageText.truncated)
                        }
                    } finally {
                        pageBitmap.recycle()
                    }
                }
                processedPages += 1
                if (extractedText.truncated) break
            }

            val truncatedByPageLimit = didReachPdfPageLimit(pagePlan, processedPages)
            val truncatedByCharacterLimit = extractedText.truncated
            val finalText = extractedText.toString()
            ensureActive()

            if (finalText.isEmpty()) {
                return@withContext DocumentAnalysisResult(
                    extractedText = null,
                    status = DocumentAnalysisStatus.EMPTY,
                    source = classifyPdfSource(pageProvenance),
                    pageCount = processedPages,
                    totalPages = totalPages,
                    pageProvenance = pageProvenance,
                    truncated = truncatedByPageLimit || truncatedByCharacterLimit,
                    truncatedByPageLimit = truncatedByPageLimit,
                    truncatedByCharacterLimit = truncatedByCharacterLimit,
                    characterCount = 0,
                    processingDurationMs = System.currentTimeMillis() - startMs,
                    errorMessage = "No readable text was found in the PDF."
                )
            }

            return@withContext DocumentAnalysisResult(
                extractedText = finalText,
                status = DocumentAnalysisStatus.SUCCESS,
                source = classifyPdfSource(pageProvenance),
                pageCount = processedPages,
                totalPages = totalPages,
                pageProvenance = pageProvenance,
                truncated = truncatedByPageLimit || truncatedByCharacterLimit,
                truncatedByPageLimit = truncatedByPageLimit,
                truncatedByCharacterLimit = truncatedByCharacterLimit,
                characterCount = finalText.length,
                processingDurationMs = System.currentTimeMillis() - startMs
            )
        } catch (cancelled: CancellationException) {
            cancellationFailure = cancelled
            throw cancelled
        } catch (e: PdfTooLargeException) {
            return@withContext DocumentAnalysisResult(
                status = DocumentAnalysisStatus.FILE_TOO_LARGE,
                errorMessage = "This PDF exceeds the maximum supported size for on-device processing."
            )
        } catch (e: SecurityException) {
            return@withContext DocumentAnalysisResult(
                status = DocumentAnalysisStatus.PERMISSION_DENIED,
                errorMessage = "Permission to read the PDF was denied."
            )
        } catch (e: InsufficientPdfScratchStorageException) {
            return@withContext DocumentAnalysisResult(
                status = DocumentAnalysisStatus.UNEXPECTED_ERROR,
                errorMessage = PDF_SCRATCH_STORAGE_ERROR
            )
        } catch (e: IOException) {
            if (isPdfScratchStorageError(e)) {
                return@withContext DocumentAnalysisResult(
                    status = DocumentAnalysisStatus.UNEXPECTED_ERROR,
                    errorMessage = PDF_SCRATCH_STORAGE_ERROR
                )
            }
            val status = mapPdfIOExceptionStatus(e)
            return@withContext DocumentAnalysisResult(
                status = status,
                errorMessage = if (status == DocumentAnalysisStatus.PASSWORD_PROTECTED_PDF) {
                    "This PDF is password-protected and cannot be processed."
                } else {
                    "The PDF could not be decoded."
                }
            )
        } catch (e: IllegalArgumentException) {
            return@withContext DocumentAnalysisResult(
                status = DocumentAnalysisStatus.UNREADABLE_FILE,
                errorMessage = "The PDF is not readable or is encrypted."
            )
        } finally {
            val cleanupFailure = closeResourcesSafely(
                primaryFailure = null,
                closeActions = listOf(
                    { document?.close() },
                    { scratchDirectory?.let(::deleteScratchDirectory) },
                    { tempPdfFile?.let(::deleteTempFile) }
                )
            )
            if (cleanupFailure != null) {
                if (cancellationFailure != null) {
                    if (cancellationFailure !== cleanupFailure) {
                        cancellationFailure?.addSuppressed(cleanupFailure)
                    }
                } else {
                    Log.e(TAG, "Failed to clean up PDF processing resources.", cleanupFailure)
                }
            }
        }
    }

    private suspend fun isPdfWithinSizeLimit(uri: Uri): Boolean {
        val input = context.contentResolver.openInputStream(uri)
            ?: throw IOException("The PDF input stream could not be opened.")
        return input.use {
            val buffer = ByteArray(COPY_BUFFER_SIZE)
            var totalBytes = 0L
            while (true) {
                currentCoroutineContext().ensureActive()
                val read = it.read(buffer)
                if (read < 0) break
                totalBytes += read.toLong()
                if (totalBytes > MAX_PDF_BYTES) {
                    return false
                }
            }
            true
        }
    }

    private suspend fun copyUriToCacheFile(uri: Uri): File {
        val tempFile = File.createTempFile("ocr_pdf_", ".pdf", context.cacheDir)
        var copyCompleted = false
        return try {
            val input = context.contentResolver.openInputStream(uri)
                ?: throw IOException("The PDF input stream could not be opened.")
            input.use {
                FileOutputStream(tempFile).use { output ->
                    val buffer = ByteArray(COPY_BUFFER_SIZE)
                    var totalBytes = 0L
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val read = it.read(buffer)
                        if (read < 0) break
                        if (totalBytes + read > MAX_PDF_BYTES) {
                            throw PdfTooLargeException()
                        }
                        output.write(buffer, 0, read)
                        totalBytes += read.toLong()
                    }
                }
            }
            copyCompleted = true
            tempFile
        } finally {
            if (!copyCompleted) {
                deleteTempFile(tempFile)
            }
        }
    }

    private suspend fun extractEmbeddedTextPage(
        document: PDDocument,
        pageIndex: Int,
        maxCharacters: Int
    ): BoundedTextResult {
        val coroutineContext = currentCoroutineContext()
        val stripper = PDFTextStripper()
        stripper.startPage = pageIndex + 1
        stripper.endPage = pageIndex + 1
        return runBoundedPdfTextExtraction(
            maxCharacters = maxCharacters,
            cancellationCheck = { coroutineContext.ensureActive() }
        ) { writer ->
            stripper.writeText(document, writer)
        }
    }

    private fun classifyPdfSource(
        pageProvenance: List<DocumentPageProvenance>
    ): DocumentExtractionSource {
        val contentPages = pageProvenance.filter { it.source != DocumentPageExtractionSource.BLANK }
        if (contentPages.isEmpty()) return DocumentExtractionSource.PDF_EMPTY
        val hasTextPage = contentPages.any { it.source == DocumentPageExtractionSource.PDF_TEXT_LAYER }
        val hasOcrPage = contentPages.any { it.source == DocumentPageExtractionSource.PDF_OCR }
        return when {
            hasTextPage && hasOcrPage -> DocumentExtractionSource.PDF_MIXED
            hasTextPage -> DocumentExtractionSource.PDF_TEXT_LAYER
            else -> DocumentExtractionSource.PDF_OCR
        }
    }

    private suspend fun normalizeTextBounded(
        rawText: String,
        maxCharacters: Int
    ): BoundedTextResult {
        val coroutineContext = currentCoroutineContext()
        val normalizer = BoundedTextNormalizer(maxCharacters) {
            coroutineContext.ensureActive()
        }
        normalizer.write(rawText, 0, rawText.length)
        return normalizer.result()
    }

    private suspend fun isVisuallyBlank(bitmap: Bitmap): Boolean {
        val rowsPerChunk = 16
        val pixels = IntArray(bitmap.width * rowsPerChunk)
        var y = 0
        while (y < bitmap.height) {
            currentCoroutineContext().ensureActive()
            val rowCount = minOf(rowsPerChunk, bitmap.height - y)
            bitmap.getPixels(pixels, 0, bitmap.width, 0, y, bitmap.width, rowCount)
            if (containsNonWhitePixel(pixels, bitmap.width * rowCount)) return false
            y += rowCount
        }
        return true
    }

    private fun renderPdfPageToBitmap(pdfFile: File, pageIndex: Int): Bitmap {
        val descriptor = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
        var renderer: PdfRenderer? = null
        var page: PdfRenderer.Page? = null
        var bitmap: Bitmap? = null
        var renderedBitmap: Bitmap? = null
        var failure: Throwable? = null
        try {
            renderer = PdfRenderer(descriptor)
            page = renderer.openPage(pageIndex)
            val pageScale = computePageScale(page.width, page.height)
            val width = max(1, (page.width * pageScale).toInt())
            val height = max(1, (page.height * pageScale).toInt())
            bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(Color.WHITE)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            renderedBitmap = checkNotNull(bitmap)
            bitmap = null
        } catch (renderFailure: Throwable) {
            failure = renderFailure
        }

        failure = closeResourcesSafely(
            primaryFailure = failure,
            closeActions = listOf(
                { bitmap?.recycle() },
                { page?.close() },
                { renderer?.close() },
                { descriptor.close() }
            )
        )
        if (failure != null) {
            renderedBitmap?.let { bitmapToRecycle ->
                failure = closeResourcesSafely(failure, listOf({ bitmapToRecycle.recycle() }))
            }
            throw checkNotNull(failure)
        }
        return checkNotNull(renderedBitmap)
    }

    private fun deleteTempFile(file: File) {
        try {
            if (file.exists() && !file.delete()) {
                Log.w(TAG, "Failed to delete temporary PDF file: ${file.absolutePath}")
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied while deleting a temporary PDF file.", e)
        }
    }

    private fun deleteScratchDirectory(directory: File) {
        val children = directory.listFiles()
        if (children == null) {
            Log.w(TAG, "Could not list PDF scratch directory contents.")
        } else {
            children.forEach { child ->
                if (child.isFile) {
                    deleteTempFile(child)
                } else {
                    Log.w(TAG, "Unexpected directory in PDF scratch storage: ${child.name}")
                }
            }
        }
        if (directory.exists() && !directory.delete()) {
            Log.w(TAG, "Failed to delete PDF scratch directory.")
        }
    }

    private fun createPdfScratchSpace(): PdfScratchSpace {
        val availableBytes = StatFs(context.cacheDir.absolutePath).availableBytes
        val scratchBudget = calculatePdfScratchBudget(availableBytes)
            ?: throw InsufficientPdfScratchStorageException()
        val directory = File(context.cacheDir, "pdfbox_${UUID.randomUUID()}")
        if (!directory.mkdir()) {
            throw IOException("The private PDF scratch directory could not be created.")
        }
        return try {
            PdfScratchSpace(
                directory = directory,
                memoryUsageSetting = MemoryUsageSetting.setupMixed(
                    PDFBOX_MAIN_MEMORY_LIMIT_BYTES,
                    scratchBudget
                ).setTempDir(directory)
            )
        } catch (failure: Throwable) {
            deleteScratchDirectory(directory)
            throw failure
        }
    }

    private fun isPdfScratchStorageError(error: IOException): Boolean {
        var cause: Throwable? = error
        while (cause != null) {
            val message = cause.message.orEmpty()
            if (message.contains(PDFBOX_SCRATCH_LIMIT_ERROR, ignoreCase = true) ||
                message.contains("no space left on device", ignoreCase = true) ||
                message.contains("not enough space", ignoreCase = true) ||
                message.contains("ENOSPC", ignoreCase = true)
            ) {
                return true
            }
            cause = cause.cause
        }
        return false
    }

    private data class PdfScratchSpace(
        val directory: File,
        val memoryUsageSetting: MemoryUsageSetting
    )

    private class InsufficientPdfScratchStorageException : IOException()

    private class PdfTooLargeException : IOException()

    private suspend fun decodeBitmapFromUri(uri: Uri): Bitmap? {
        currentCoroutineContext().ensureActive()
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        val boundsStream = context.contentResolver.openInputStream(uri)
            ?: throw IOException("The image input stream could not be opened.")
        boundsStream.use { BitmapFactory.decodeStream(it, null, bounds) }
        currentCoroutineContext().ensureActive()

        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            throw IOException("The image format is unsupported or the image is invalid.")
        }

        val sampleSize = calculateImageSampleSize(bounds.outWidth, bounds.outHeight)
        val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        val decodedStream = context.contentResolver.openInputStream(uri)
            ?: throw IOException("The image input stream could not be reopened.")
        var decoded: Bitmap? = null
        try {
            decodedStream.use {
                decoded = BitmapFactory.decodeStream(it, null, decodeOptions)
            }
            if (decoded == null) {
                throw IOException("The image could not be decoded.")
            }
            currentCoroutineContext().ensureActive()
            val decodedBitmap = checkNotNull(decoded)
            val boundedBitmap = resizeBitmapToMaximum(decodedBitmap)
            if (boundedBitmap !== decodedBitmap) decodedBitmap.recycle()
            decoded = null
            return boundedBitmap
        } finally {
            decoded?.recycle()
        }
    }

    private fun adjustBitmapOrientation(uri: Uri, bitmap: Bitmap): Bitmap {
        val exif = context.contentResolver.openInputStream(uri)?.use { stream ->
            ExifInterface(stream)
        }
        val rotationDegrees = exif?.rotationDegrees ?: 0
        if (rotationDegrees == 0) return bitmap
        val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        if (rotated !== bitmap) {
            bitmap.recycle()
        }
        return rotated
    }

    private suspend fun recognizeBitmap(bitmap: Bitmap): String =
        recognizerMutex.withLock {
            currentCoroutineContext().ensureActive()
            val activeRecognizer = acquireRecognizer()
            try {
                val task = activeRecognizer.process(InputImage.fromBitmap(bitmap, 0))
                val visionText = withContext(NonCancellable) { task.await() }
                currentCoroutineContext().ensureActive()
                visionText.text
            } finally {
                releaseRecognizer()
            }
        }

    private fun acquireRecognizer(): TextRecognizer = synchronized(recognizerStateLock) {
        val instance = recognizer
            ?: TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS).also {
                recognizer = it
            }
        activeRecognitions += 1
        instance
    }

    private fun releaseRecognizer() {
        synchronized(recognizerStateLock) {
            activeRecognitions -= 1
            if (activeRecognitions == 0 && recognizerCloseRequested) {
                closeRecognizerLocked()
            }
        }
    }

    private fun closeRecognizerLocked() {
        val currentRecognizer = recognizer
        recognizer = null
        recognizerCloseRequested = false
        try {
            currentRecognizer?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to close the text recognizer.", e)
        }
    }

    private fun resizeBitmapToMaximum(bitmap: Bitmap): Bitmap {
        val (width, height) = calculateImageDimensions(bitmap.width, bitmap.height)
        if (width == bitmap.width && height == bitmap.height) return bitmap
        return Bitmap.createScaledBitmap(bitmap, width, height, true)
    }

    private fun computePageScale(width: Int, height: Int): Float {
        val longSide = max(width, height).toFloat()
        if (longSide <= MAX_RENDER_LONG_EDGE) return 1f
        return MAX_RENDER_LONG_EDGE / longSide
    }

    companion object {
        const val MAX_OCR_CHARACTERS = 200000
        const val MAX_PDF_PAGES = 100
        const val MAX_PDF_BYTES = 25L * 1024 * 1024
        const val MAX_IMAGE_LONG_EDGE = 2200
        const val MAX_RENDER_LONG_EDGE = 2200
        const val MIN_EMBEDDED_TEXT_CHARS = 30
        private const val COPY_BUFFER_SIZE = 8192
        private const val PDFBOX_MAIN_MEMORY_LIMIT_BYTES = 8L * 1024 * 1024
        private const val PDF_SCRATCH_STORAGE_ERROR =
            "There is not enough private storage to process this PDF. Free some device storage and try again."
        private const val PDFBOX_SCRATCH_LIMIT_ERROR = "Maximum allowed scratch file memory exceeded"
        private const val TAG = "OcrDocumentAnalyzer"

        internal fun calculateImageSampleSize(width: Int, height: Int): Int {
            require(width > 0 && height > 0)
            val longSide = max(width, height).toLong()
            var sampleSize = 1
            while (sampleSize <= Int.MAX_VALUE / 2) {
                val nextSampleSize = sampleSize.toLong() * 2
                val sampledLongSide =
                    (longSide + nextSampleSize - 1) / nextSampleSize
                if (sampledLongSide < MAX_IMAGE_LONG_EDGE - 1) break
                sampleSize = nextSampleSize.toInt()
            }
            return sampleSize
        }

        internal fun calculateImageDimensions(width: Int, height: Int): Pair<Int, Int> {
            require(width > 0 && height > 0)
            val longSide = max(width, height)
            if (longSide <= MAX_IMAGE_LONG_EDGE) return width to height
            val scale = MAX_IMAGE_LONG_EDGE.toDouble() / longSide
            return max(1, (width * scale).roundToInt()) to
                max(1, (height * scale).roundToInt())
        }
    }
}
