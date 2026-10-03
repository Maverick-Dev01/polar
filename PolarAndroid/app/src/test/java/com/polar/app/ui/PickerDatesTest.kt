package com.polar.app.ui

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class PickerDatesTest {
    private val picker = 1773532800000L // 2026-03-15T00:00:00Z

    private fun check(zoneId: String) {
        val zone = TimeZone.getTimeZone(zoneId)
        val stored = PickerDates.toStored(picker, zone)
        val c = Calendar.getInstance(zone).apply { timeInMillis = stored }
        assertEquals(2026, c.get(Calendar.YEAR))
        assertEquals(2, c.get(Calendar.MONTH))
        assertEquals(15, c.get(Calendar.DAY_OF_MONTH))
        assertEquals(12, c.get(Calendar.HOUR_OF_DAY))
        assertEquals(picker, PickerDates.toPicker(stored, zone))
    }

    @Test fun mexicoCity() = check("America/Mexico_City")
    @Test fun tokyo() = check("Asia/Tokyo")
}
