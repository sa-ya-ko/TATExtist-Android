package com.tatextist.utils

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ContrastHelperTest {

    @Test
    fun `fromContrastValue returns STANDARD for 0_0f or low values`() {
        assertEquals(ContrastLevel.STANDARD, ContrastHelper.fromContrastValue(0.0f))
        assertEquals(ContrastLevel.STANDARD, ContrastHelper.fromContrastValue(0.2f))
        assertEquals(ContrastLevel.STANDARD, ContrastHelper.fromContrastValue(-1.0f))
    }

    @Test
    fun `fromContrastValue returns MEDIUM for medium values around 0_5f`() {
        assertEquals(ContrastLevel.MEDIUM, ContrastHelper.fromContrastValue(0.5f))
        assertEquals(ContrastLevel.MEDIUM, ContrastHelper.fromContrastValue(0.4f))
        assertEquals(ContrastLevel.MEDIUM, ContrastHelper.fromContrastValue(0.6f))
    }

    @Test
    fun `fromContrastValue returns HIGH for high values up to 1_0f`() {
        assertEquals(ContrastLevel.HIGH, ContrastHelper.fromContrastValue(1.0f))
        assertEquals(ContrastLevel.HIGH, ContrastHelper.fromContrastValue(0.8f))
        assertEquals(ContrastLevel.HIGH, ContrastHelper.fromContrastValue(1.5f))
    }
}

