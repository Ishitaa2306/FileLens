package com.example.intelligentfileorganizer.domain.analyzer

import com.tom_roush.pdfbox.pdmodel.encryption.InvalidPasswordException
import java.io.IOException
import java.io.Writer

internal data class PdfPageProcessingPlan(
    val pagesToProcess: Int,
    val truncated: Boolean
)

internal data class BoundedTextResult(
    val text: String,
    val truncated: Boolean,
    val meaningfulCharacterCount: Int
)

private class PdfTextLimitReachedException : IOException()

internal fun runBoundedPdfTextExtraction(
    maxCharacters: Int,
    cancellationCheck: (() -> Unit)? = null,
    extractText: (Writer) -> Unit
): BoundedTextResult {
    val writer = BoundedTextNormalizer(
        maxCharacters = maxCharacters,
        cancellationCheck = cancellationCheck,
        stopOnLimit = true
    )
    try {
        extractText(writer)
    } catch (_: PdfTextLimitReachedException) {
        // PDFTextStripper propagates writer IOExceptions; only this sentinel means truncation.
    }
    return writer.result()
}

internal fun createPdfPageProcessingPlan(
    totalPages: Int,
    pageLimit: Int
): PdfPageProcessingPlan {
    require(totalPages >= 0)
    require(pageLimit > 0)
    return PdfPageProcessingPlan(
        pagesToProcess = totalPages.coerceAtMost(pageLimit),
        truncated = totalPages > pageLimit
    )
}

internal fun didReachPdfPageLimit(
    plan: PdfPageProcessingPlan,
    processedPages: Int
): Boolean = plan.truncated && processedPages >= plan.pagesToProcess

internal fun calculatePdfScratchBudget(availableBytes: Long): Long? {
    if (availableBytes <= PDF_SCRATCH_FREE_SPACE_RESERVE_BYTES) return null
    val usableBytes = availableBytes - PDF_SCRATCH_FREE_SPACE_RESERVE_BYTES
    if (usableBytes < MIN_PDF_SCRATCH_BYTES) return null
    return usableBytes.coerceAtMost(MAX_PDF_SCRATCH_BYTES)
}

internal fun mapPdfIOExceptionStatus(error: IOException): DocumentAnalysisStatus =
    if (error is InvalidPasswordException) {
        DocumentAnalysisStatus.PASSWORD_PROTECTED_PDF
    } else {
        DocumentAnalysisStatus.UNREADABLE_FILE
    }

internal class BoundedTextNormalizer(
    private val maxCharacters: Int,
    private val stopOnLimit: Boolean = false,
    private val cancellationCheck: (() -> Unit)? = null
) : Writer() {
    private val builder = StringBuilder(minOf(maxCharacters, INITIAL_CAPACITY))
    private var pendingSpace = false
    private var pendingNewlines = 0
    private var previousWasCarriageReturn = false
    private var charactersSinceCancellationCheck = 0
    private var meaningfulCharacterCount = 0
    private var wasTruncated = false

    init {
        require(maxCharacters >= 0)
    }

    override fun write(cbuf: CharArray, off: Int, len: Int) {
        require(off >= 0 && len >= 0 && off + len <= cbuf.size)
        for (index in off until off + len) {
            appendCharacter(cbuf[index])
        }
    }

    override fun write(c: Int) {
        appendCharacter(c.toChar())
    }

    override fun write(str: String, off: Int, len: Int) {
        require(off >= 0 && len >= 0 && off + len <= str.length)
        for (index in off until off + len) {
            appendCharacter(str[index])
        }
    }

    override fun flush() = Unit

    override fun close() = Unit

    fun result(): BoundedTextResult = BoundedTextResult(
        text = builder.toString(),
        truncated = wasTruncated,
        meaningfulCharacterCount = meaningfulCharacterCount
    )

    private fun appendCharacter(character: Char) {
        charactersSinceCancellationCheck += 1
        if (charactersSinceCancellationCheck >= CANCELLATION_CHECK_INTERVAL) {
            cancellationCheck?.invoke()
            charactersSinceCancellationCheck = 0
        }

        when {
            character == '\r' -> {
                previousWasCarriageReturn = true
                appendNewline()
            }
            character == '\n' -> {
                if (!previousWasCarriageReturn) appendNewline()
                previousWasCarriageReturn = false
            }
            else -> {
                previousWasCarriageReturn = false
                when (character) {
                    ' ', '\t' -> {
                        if (pendingNewlines == 0 && builder.isNotEmpty()) {
                            pendingSpace = true
                        }
                    }
                    else -> {
                        if (character.isWhitespace()) {
                            if (pendingNewlines == 0 && builder.isNotEmpty()) {
                                pendingSpace = true
                            }
                        } else {
                            appendNonWhitespace(character)
                        }
                    }
                }
            }
        }
    }

    private fun appendNewline() {
        pendingSpace = false
        pendingNewlines = (pendingNewlines + 1).coerceAtMost(MAX_CONSECUTIVE_NEWLINES)
    }

    private fun appendNonWhitespace(character: Char) {
        if (character.isLetterOrDigit()) {
            if (meaningfulCharacterCount < Int.MAX_VALUE) meaningfulCharacterCount += 1
        }

        if (builder.isEmpty()) {
            pendingSpace = false
            pendingNewlines = 0
        } else {
            repeat(pendingNewlines) { appendBounded('\n') }
            if (pendingNewlines == 0 && pendingSpace) appendBounded(' ')
            pendingSpace = false
            pendingNewlines = 0
        }
        appendBounded(character)
    }

    private fun appendBounded(character: Char) {
        if (builder.length < maxCharacters) {
            builder.append(character)
        } else {
            wasTruncated = true
            if (stopOnLimit) throw PdfTextLimitReachedException()
        }
    }

    private companion object {
        const val INITIAL_CAPACITY = 1024
        const val MAX_CONSECUTIVE_NEWLINES = 2
        const val CANCELLATION_CHECK_INTERVAL = 4096
    }
}

internal class BoundedPdfTextAccumulator(
    private val maxCharacters: Int
) {
    private val builder: StringBuilder

    var truncated: Boolean = false
        private set

    init {
        require(maxCharacters > 0)
        builder = StringBuilder(minOf(maxCharacters, INITIAL_CAPACITY))
    }

    fun remainingPageCapacity(): Int =
        (maxCharacters - builder.length - pageSeparatorLength()).coerceAtLeast(0)

    fun appendPage(pageText: String, pageTextTruncated: Boolean = false) {
        if (pageText.isEmpty()) {
            truncated = truncated || pageTextTruncated
            return
        }

        val remaining = maxCharacters - builder.length
        val separatorLength = pageSeparatorLength()
        val available = remaining - separatorLength
        if (available <= 0) {
            truncated = true
            return
        }

        if (separatorLength > 0) builder.append(PAGE_SEPARATOR)
        val appendedLength = pageText.length.coerceAtMost(available)
        builder.append(pageText, 0, appendedLength)
        truncated = truncated || pageTextTruncated || appendedLength < pageText.length
    }

    override fun toString(): String = builder.toString()

    private fun pageSeparatorLength(): Int = if (builder.isEmpty()) 0 else PAGE_SEPARATOR.length

    private companion object {
        const val INITIAL_CAPACITY = 1024
        const val PAGE_SEPARATOR = "\n\n"
    }
}

internal fun containsNonWhitePixel(pixels: IntArray, pixelCount: Int = pixels.size): Boolean {
    require(pixelCount in 0..pixels.size)
    for (index in 0 until pixelCount) {
        if ((pixels[index] and RGB_MASK) != RGB_WHITE) return true
    }
    return false
}

internal fun closeResourcesSafely(
    primaryFailure: Throwable?,
    closeActions: List<() -> Unit>
): Throwable? {
    var failure = primaryFailure
    closeActions.forEach { action ->
        try {
            action()
        } catch (closeFailure: Throwable) {
            if (failure == null) {
                failure = closeFailure
            } else if (failure !== closeFailure) {
                failure.addSuppressed(closeFailure)
            }
        }
    }
    return failure
}

private const val RGB_MASK = 0x00FFFFFF
private const val RGB_WHITE = 0x00FFFFFF
private const val PDF_SCRATCH_FREE_SPACE_RESERVE_BYTES = 16L * 1024 * 1024
private const val MIN_PDF_SCRATCH_BYTES = 8L * 1024 * 1024
internal const val MAX_PDF_SCRATCH_BYTES = 128L * 1024 * 1024
