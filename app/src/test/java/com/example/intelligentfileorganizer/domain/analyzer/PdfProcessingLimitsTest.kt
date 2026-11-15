package com.example.intelligentfileorganizer.domain.analyzer

import com.tom_roush.pdfbox.pdmodel.encryption.InvalidPasswordException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.ensureActive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class PdfProcessingLimitsTest {

    @Test
    fun pagePlanProcessesUpToLimitAndMarksOnlyExcessPagesTruncated() {
        val seventyFivePages = createPdfPageProcessingPlan(75, 100)
        val oneHundredPages = createPdfPageProcessingPlan(100, 100)
        val oneHundredOnePages = createPdfPageProcessingPlan(101, 100)

        assertEquals(PdfPageProcessingPlan(75, false), seventyFivePages)
        assertEquals(PdfPageProcessingPlan(100, false), oneHundredPages)
        assertEquals(PdfPageProcessingPlan(100, true), oneHundredOnePages)
        assertFalse(didReachPdfPageLimit(oneHundredPages, processedPages = 100))
        assertFalse(didReachPdfPageLimit(oneHundredOnePages, processedPages = 75))
        assertTrue(didReachPdfPageLimit(oneHundredOnePages, processedPages = 100))
    }

    @Test
    fun accumulatorPreservesPageOrderAndCapsTextIncrementally() {
        val accumulator = BoundedPdfTextAccumulator(maxCharacters = 16)

        accumulator.appendPage("page one")
        accumulator.appendPage("page two")
        accumulator.appendPage("page three")

        assertEquals("page one\n\npage t", accumulator.toString())
        assertEquals(16, accumulator.toString().length)
        assertTrue(accumulator.truncated)
    }

    @Test
    fun accumulatorDoesNotMarkExactLimitAsTruncatedUntilMoreContentExists() {
        val accumulator = BoundedPdfTextAccumulator(maxCharacters = 4)

        accumulator.appendPage("text")
        accumulator.appendPage("")

        assertEquals("text", accumulator.toString())
        assertFalse(accumulator.truncated)

        accumulator.appendPage("next")

        assertEquals("text", accumulator.toString())
        assertTrue(accumulator.truncated)
    }

    @Test
    fun accumulatorMarksContentOmittedFromAnOversizedPage() {
        val accumulator = BoundedPdfTextAccumulator(maxCharacters = 4)

        accumulator.appendPage("texts")

        assertEquals("text", accumulator.toString())
        assertTrue(accumulator.truncated)
    }

    @Test
    fun textNormalizerHandlesExactLimitAndTrailingWhitespaceWithoutFalseTruncation() {
        val normalizer = BoundedTextNormalizer(maxCharacters = 4)
        normalizer.write("text \r\n\n", 0, "text \r\n\n".length)

        val result = normalizer.result()
        assertEquals("text", result.text)
        assertFalse(result.truncated)
    }

    @Test
    fun textNormalizerCapsOutputAndRecordsOmittedContent() {
        val normalizer = BoundedTextNormalizer(maxCharacters = 4)
        normalizer.write("textx", 0, 5)

        val result = normalizer.result()
        assertEquals("text", result.text)
        assertTrue(result.truncated)
        assertEquals(5, result.meaningfulCharacterCount)
    }

    @Test
    fun boundedPdfExtractionPreservesTextBelowTheLimit() {
        val result = runBoundedPdfTextExtraction(maxCharacters = 5) { writer ->
            writer.write("text")
        }

        assertEquals("text", result.text)
        assertFalse(result.truncated)
    }

    @Test
    fun boundedPdfExtractionDoesNotTruncateTextExactlyAtTheLimit() {
        val result = runBoundedPdfTextExtraction(maxCharacters = 4) { writer ->
            writer.write("text")
        }

        assertEquals("text", result.text)
        assertFalse(result.truncated)
    }

    @Test
    fun boundedPdfExtractionStopsAndMarksTextBeyondTheLimit() {
        var extractionCompleted = false
        val result = runBoundedPdfTextExtraction(maxCharacters = 4) { writer ->
            writer.write("text")
            writer.write("x")
            extractionCompleted = true
        }

        assertEquals("text", result.text)
        assertTrue(result.truncated)
        assertFalse(extractionCompleted)
    }

    @Test
    fun boundedPdfExtractionKeepsPreviouslyExtractedTextWhenItStopsEarly() {
        val result = runBoundedPdfTextExtraction(maxCharacters = 7) { writer ->
            writer.write("prior ")
            writer.write("page")
        }

        assertEquals("prior p", result.text)
        assertTrue(result.truncated)
    }

    @Test
    fun boundedPdfExtractionDoesNotTreatGenuineIoFailuresAsTruncation() {
        val extractionFailure = IOException("PDF content stream failed")
        var actualFailure: IOException? = null

        try {
            runBoundedPdfTextExtraction(maxCharacters = 10) { writer ->
                writer.write("partial")
                throw extractionFailure
            }
        } catch (failure: IOException) {
            actualFailure = failure
        }

        assertSame(extractionFailure, actualFailure)
    }

    @Test
    fun textNormalizerCollapsesWhitespaceLikeThePreviousOutputNormalizer() {
        val input = " \tHello  world \r\n \n\nnext \t"
        val normalizer = BoundedTextNormalizer(maxCharacters = 100)
        normalizer.write(input, 0, input.length)

        assertEquals("Hello world\n\nnext", normalizer.result().text)
    }

    @Test
    fun blankPageDetectorTreatsPureWhiteRowsAsBlank() {
        assertFalse(containsNonWhitePixel(intArrayOf(-1, -1, -1)))
    }

    @Test
    fun blankPageDetectorKeepsSparseContentForOcr() {
        assertTrue(containsNonWhitePixel(intArrayOf(-1, -1, 0xFF000000.toInt(), -1)))
    }

    @Test
    fun blankPageDetectorKeepsFaintContentForOcr() {
        assertTrue(containsNonWhitePixel(intArrayOf(-1, 0xFFFEFEFE.toInt(), -1)))
    }

    @Test
    fun blankPageDetectorKeepsSmallContentForOcr() {
        assertTrue(containsNonWhitePixel(intArrayOf(0xFF000000.toInt())))
    }

    @Test
    fun scratchBudgetReservesFreeSpaceAndEnforcesItsUpperBound() {
        val megabyte = 1024L * 1024

        assertEquals(null, calculatePdfScratchBudget(23 * megabyte))
        assertEquals(48 * megabyte, calculatePdfScratchBudget(64 * megabyte))
        assertEquals(MAX_PDF_SCRATCH_BYTES, calculatePdfScratchBudget(512 * megabyte))
    }

    @Test
    fun passwordProtectedPdfExceptionsUseTheirDedicatedStatus() {
        val constructor = InvalidPasswordException::class.java
            .getDeclaredConstructor(String::class.java)
            .apply { isAccessible = true }
        val passwordException = constructor.newInstance("password required")

        assertEquals(
            DocumentAnalysisStatus.PASSWORD_PROTECTED_PDF,
            mapPdfIOExceptionStatus(passwordException)
        )
        assertEquals(
            DocumentAnalysisStatus.UNREADABLE_FILE,
            mapPdfIOExceptionStatus(IOException("invalid PDF"))
        )
    }

    @Test
    fun normalizerChecksCoroutineCancellationWhileStreaming() {
        val job = Job()
        val normalizer = BoundedTextNormalizer(maxCharacters = 5000) {
            job.ensureActive()
        }
        job.cancel()

        var cancellationObserved = false
        try {
            normalizer.write("x".repeat(4096), 0, 4096)
        } catch (_: CancellationException) {
            cancellationObserved = true
        }

        assertTrue(cancellationObserved)
    }

    @Test
    fun cleanupAttemptsEveryResourceAndPreservesTheOriginalFailure() {
        val originalFailure = IllegalStateException("render failed")
        val cleanupCalls = mutableListOf<Int>()
        val cleanupFailure = closeResourcesSafely(
            primaryFailure = originalFailure,
            closeActions = listOf(
                { cleanupCalls.add(1) },
                {
                    cleanupCalls.add(2)
                    throw IOException("page close failed")
                },
                {
                    cleanupCalls.add(3)
                    throw IOException("descriptor close failed")
                }
            )
        )

        assertEquals(originalFailure, cleanupFailure)
        assertEquals(listOf(1, 2, 3), cleanupCalls)
        assertEquals(2, originalFailure.suppressed.size)
    }
}
