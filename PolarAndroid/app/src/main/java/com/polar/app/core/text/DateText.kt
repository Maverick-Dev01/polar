package com.polar.app.core.text

import com.polar.app.model.DateStyle
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/** Meses escritos a mano: los nombres de `Locale.forLanguageTag("es")` cambian entre versiones de Android ("feb." vs "feb"). */
object DateText {
    private val SHORT = listOf("ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic")
    private val LONG = listOf("enero", "febrero", "marzo", "abril", "mayo", "junio", "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre")

    fun format(epochMs: Long, style: DateStyle, zone: TimeZone = TimeZone.getDefault()): String {
        val c = Calendar.getInstance(zone).apply { timeInMillis = epochMs }
        val day = c.get(Calendar.DAY_OF_MONTH)
        val month = c.get(Calendar.MONTH)
        val year = c.get(Calendar.YEAR)
        return when (style) {
            DateStyle.DAY_MONTH_YEAR -> "$day ${SHORT[month]} $year"
            DateStyle.NUMERIC -> String.format(Locale.ROOT, "%02d.%02d.%02d", day, month + 1, year % 100)
            DateStyle.MONTH_YEAR -> "${LONG[month]} $year"
        }
    }
}
