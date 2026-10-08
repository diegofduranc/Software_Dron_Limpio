package com.drinix.gcs

import com.drinix.gcs.ui.home.formatDuration
import org.junit.Assert.assertEquals
import org.junit.Test

class FormatDurationTest {

    @Test
    fun `zero seconds formats as 00_00`() {
        assertEquals("00:00", formatDuration(0))
    }

    @Test
    fun `seconds only`() {
        assertEquals("00:45", formatDuration(45))
    }

    @Test
    fun `minutes and seconds`() {
        assertEquals("12:36", formatDuration(12 * 60 + 36))
    }

    @Test
    fun `hours roll into minutes`() {
        assertEquals("61:02", formatDuration(61 * 60 + 2))
    }
}
