package com.voicetodocs.cos

import com.voicetodocs.cos.data.CalendarItem
import com.voicetodocs.cos.data.DayIndex
import com.voicetodocs.cos.data.RecordingNote
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class DayIndexTest {
    private val zone = ZoneId.of("America/Los_Angeles")
    private val today = LocalDate.of(2026, 8, 29)

    private fun note(id: String, day: LocalDate, hour: Int, open: Boolean, summary: String): RecordingNote {
        val millis = ZonedDateTime.of(day.year, day.monthValue, day.dayOfMonth, hour, 0, 0, 0, zone)
            .toInstant()
            .toEpochMilli()
        return RecordingNote(id = id, createdAtMillis = millis, summary = summary, open = open)
    }

    @Test
    fun splitsYesterdayAndTodayAndKeepsMainPoints() {
        val notes = listOf(
            note("t1", today, 9, true, "Buy paper for class"),
            note("t2", today, 15, true, "Call the office"),
            note("y1", today.minusDays(1), 16, true, "Prep Monday copies"),
            note("old", today.minusDays(3), 10, true, "Ignore this")
        )
        val yesterday = DayIndex.onDate(notes, today.minusDays(1), zone)
        val todayNotes = DayIndex.onDate(notes, today, zone)
        assertEquals(listOf("y1"), yesterday.map { it.id })
        assertEquals("Prep Monday copies", yesterday.single().summary)
        assertEquals(listOf("t2", "t1"), todayNotes.map { it.id })
        assertTrue(todayNotes.all { it.summary.isNotBlank() })
    }

    @Test
    fun openItemsSkipDoneNotes() {
        val notes = listOf(
            note("open", today, 9, true, "Still open"),
            note("done", today, 10, false, "Finished")
        )
        assertEquals(listOf("open"), DayIndex.openItems(notes).map { it.id })
    }

    @Test
    fun firstEventTodayIgnoresLaterDays() {
        val events = listOf(
            CalendarItem("1", "Later this week", "Tue", "", "2026-09-01"),
            CalendarItem("2", "Standup", "9:00 AM", "Room 2", "2026-08-29"),
            CalendarItem("3", "Afternoon meeting", "3:00 PM", "", "2026-08-29")
        )
        val first = DayIndex.firstEventToday(events, today)
        assertEquals("Standup", first?.title)
        assertNull(DayIndex.firstEventToday(events, LocalDate.of(2026, 8, 30)))
    }
}
