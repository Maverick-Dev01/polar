package com.polar.app.ui.home

import android.content.res.Resources
import com.polar.app.R
import com.polar.app.core.text.DateText
import com.polar.app.model.DateStyle
import java.util.TimeZone

private const val MINUTE_MS = 60_000L
private const val HOUR_MS = 60 * MINUTE_MS
private const val DAY_MS = 24 * HOUR_MS

/** Tiempo transcurrido siempre en español, sin depender del idioma del teléfono. */
fun relativeTime(res: Resources, nowMs: Long, thenMs: Long, zone: TimeZone = TimeZone.getDefault()): String {
    val delta = nowMs - thenMs
    if (delta < MINUTE_MS) return res.getString(R.string.home_time_now)
    if (delta < HOUR_MS) return res.getString(R.string.home_time_minutes, (delta / MINUTE_MS).toInt())
    val days = dayNumber(nowMs, zone) - dayNumber(thenMs, zone)
    return when {
        days <= 0L -> res.getString(R.string.home_time_hours, (delta / HOUR_MS).toInt())
        days == 1L -> res.getString(R.string.home_time_yesterday)
        days < 7L -> res.getQuantityString(R.plurals.home_time_days, days.toInt(), days.toInt())
        else -> DateText.format(thenMs, DateStyle.DAY_MONTH_YEAR, zone)
    }
}

/** Días civiles transcurridos desde la época, en la zona dada. */
private fun dayNumber(epochMs: Long, zone: TimeZone): Long {
    val offset = zone.getOffset(epochMs).toLong()
    return Math.floorDiv(epochMs + offset, DAY_MS)
}
