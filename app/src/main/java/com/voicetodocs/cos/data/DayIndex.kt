package com.voicetodocs.cos.data

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

object DayIndex {
    fun onDate(
        notes: List<RecordingNote>,
        day: LocalDate,
        zone: ZoneId
    ): List<RecordingNote> {
        return notes
            .filter { localDate(it.createdAtMillis, zone) == day }
            .sortedByDescending { it.createdAtMillis }
    }

    fun openItems(notes: List<RecordingNote>): List<RecordingNote> {
        return notes.filter { it.open }.sortedByDescending { it.createdAtMillis }
    }

    fun firstEventToday(events: List<CalendarItem>, today: LocalDate): CalendarItem? {
        return events.firstOrNull { it.startDate == today.toString() }
    }

    fun localDate(epochMillis: Long, zone: ZoneId): LocalDate {
        return Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalDate()
    }
}
