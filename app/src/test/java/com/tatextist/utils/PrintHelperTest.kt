package com.tatextist.utils

import android.print.PrintAttributes
import com.tatextist.ui.PaperSize
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PrintHelperTest {

    @Test
    fun `getMediaSizeId for A4 should return ISO_A4`() {
        val id = PrintHelper.getMediaSizeId(PaperSize.A4)
        assertEquals("ISO_A4", id)
    }

    @Test
    fun `getMediaSizeId for B5 should return JIS_B5`() {
        val id = PrintHelper.getMediaSizeId(PaperSize.B5)
        assertEquals("JIS_B5", id)
    }

    @Test
    fun `getMediaSizeId for FIT should return UNKNOWN_LANDSCAPE`() {
        val id = PrintHelper.getMediaSizeId(PaperSize.FIT)
        assertEquals("UNKNOWN_LANDSCAPE", id)
    }
}
