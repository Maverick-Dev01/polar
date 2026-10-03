package com.polar.app.core.text

import com.polar.app.model.DateStyle
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class DateTextTest {
    private val utc = TimeZone.getTimeZone("UTC")
    private fun ms(y: Int, m: Int, d: Int) = Calendar.getInstance(utc).apply { clear(); set(y, m - 1, d, 12, 0) }.timeInMillis

    @Test
    fun formatsTheThreeStyles() {
        val v = ms(2026, 2, 14)
        assertEquals("14 feb 2026", DateText.format(v, DateStyle.DAY_MONTH_YEAR, utc))
        assertEquals("14.02.26", DateText.format(v, DateStyle.NUMERIC, utc))
        assertEquals("febrero 2026", DateText.format(v, DateStyle.MONTH_YEAR, utc))
    }

    @Test
    fun singleDigitDaysAndDecember() {
        val v = ms(2027, 12, 3)
        assertEquals("3 dic 2027", DateText.format(v, DateStyle.DAY_MONTH_YEAR, utc))
        assertEquals("03.12.27", DateText.format(v, DateStyle.NUMERIC, utc))
    }
}
