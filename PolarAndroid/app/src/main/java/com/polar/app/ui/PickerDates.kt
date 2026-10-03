package com.polar.app.ui

import java.util.Calendar
import java.util.TimeZone

/** El DatePicker de Material trabaja en medianoche UTC; Polar guarda el mediodía local de ese día. */
object PickerDates {
    fun toStored(pickerUtcMs: Long, zone: TimeZone = TimeZone.getDefault()): Long {
        val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = pickerUtcMs }
        return Calendar.getInstance(zone).apply { clear(); set(utc.get(Calendar.YEAR), utc.get(Calendar.MONTH), utc.get(Calendar.DAY_OF_MONTH), 12, 0) }.timeInMillis
    }

    fun toPicker(storedMs: Long, zone: TimeZone = TimeZone.getDefault()): Long {
        val local = Calendar.getInstance(zone).apply { timeInMillis = storedMs }
        return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { clear(); set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH)) }.timeInMillis
    }
}
