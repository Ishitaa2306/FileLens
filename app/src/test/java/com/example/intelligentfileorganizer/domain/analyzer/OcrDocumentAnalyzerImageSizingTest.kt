package com.example.intelligentfileorganizer.domain.analyzer

import org.junit.Assert.assertEquals
import org.junit.Test

class OcrDocumentAnalyzerImageSizingTest {

    @Test
    fun calculateImageSampleSizeUsesTheLargestSafePowerOfTwoSample() {
        assertEquals(1, OcrDocumentAnalyzer.calculateImageSampleSize(2200, 1000))
        assertEquals(1, OcrDocumentAnalyzer.calculateImageSampleSize(2201, 1000))
        assertEquals(2, OcrDocumentAnalyzer.calculateImageSampleSize(4401, 2200))
        assertEquals(4, OcrDocumentAnalyzer.calculateImageSampleSize(8796, 4398))
        assertEquals(4, OcrDocumentAnalyzer.calculateImageSampleSize(8801, 4400))
    }

    @Test
    fun calculateImageDimensionsPreservesAspectRatioAndCapsLongEdgeAtBoundarySizes() {
        assertEquals(2200 to 1000, OcrDocumentAnalyzer.calculateImageDimensions(2201, 1000))
        assertEquals(2200 to 1100, OcrDocumentAnalyzer.calculateImageDimensions(4401, 2200))
        assertEquals(2200 to 1100, OcrDocumentAnalyzer.calculateImageDimensions(8801, 4400))
        assertEquals(1000 to 2200, OcrDocumentAnalyzer.calculateImageDimensions(1000, 2200))
    }
}
