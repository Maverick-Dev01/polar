package com.polar.app.ui.home

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.util.TimeZone

/** El teléfono está en inglés a propósito: el texto debe salir siempre en español. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "en")
class RelativeTimeTest {
    private val res = ApplicationProvider.getApplicationContext<android.content.Context>().resources
    private val zone = TimeZone.getTimeZone("UTC")
    private val now = 1_789_300_000_000L // 2026-09-13T11:46:40Z
    private val min = 60_000L
    private val hour = 60 * min
    private val day = 24 * hour

    private fun at(thenMs: Long) = relativeTime(res, now, thenMs, zone)

    @Test fun lessThanAMinute() {
        assertEquals("Hace un momento", at(now))
        assertEquals("Hace un momento", at(now - 59_000))
        assertEquals("Hace un momento", at(now + 5_000))
    }

    @Test fun minutes() {
        assertEquals("Hace 1 min", at(now - min))
        assertEquals("Hace 59 min", at(now - 59 * min - 30_000))
    }

    @Test fun hoursSameDay() {
        assertEquals("Hace 1 h", at(now - hour))
        assertEquals("Hace 11 h", at(now - 11 * hour))
    }

    @Test fun yesterdayByCalendarDay() {
        assertEquals("Ayer", at(now - 12 * hour))
        assertEquals("Ayer", at(now - day))
    }

    @Test fun daysWithPlural() {
        assertEquals("Hace 2 días", at(now - 2 * day))
        assertEquals("Hace 6 días", at(now - 6 * day))
    }

    @Test fun olderUsesSpanishDate() {
        assertEquals("6 sep 2026", at(now - 7 * day))
        assertEquals("12 may 2026", at(now - 124 * day))
    }

    @Test fun exactBoundaries() {
        assertEquals("Hace un momento", at(now - 59_999))
        assertEquals("Hace 1 min", at(now - 60_000))
        assertEquals("Hace 59 min", at(now - hour + 1))
        assertEquals("Hace 1 h", at(now - hour))
        assertEquals("Hace 6 días", at(now - 6 * day))
        assertEquals("6 sep 2026", at(now - 7 * day))
    }

    @Test fun usesTheLocalDayNotTheUtcDay() {
        val mexico = TimeZone.getTimeZone("America/Mexico_City") // UTC-6
        val utc = java.time.Instant.parse("2026-09-13T05:30:00Z").toEpochMilli() // 12 sep 23:30 local
        // Mismo día local (12 sep 14:00) aunque en UTC sean días distintos: horas, no «Ayer».
        assertEquals("Hace 9 h", relativeTime(res, utc, java.time.Instant.parse("2026-09-12T20:00:00Z").toEpochMilli(), mexico))
        // Cruza la medianoche local (12 sep 23:30 -> 13 sep 01:00) dentro del mismo día UTC: «Ayer».
        val after = java.time.Instant.parse("2026-09-13T07:00:00Z").toEpochMilli()
        assertEquals("Ayer", relativeTime(res, after, utc, mexico))
    }
}
