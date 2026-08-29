package com.voicetodocs.cos.data

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

object CosFormatters {
    fun timestamp(now: Instant = Instant.now(), zone: ZoneId = ZoneId.systemDefault()): String {
        return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm z")
            .withZone(zone)
            .format(now)
    }

    fun timeOfDay(epochMillis: Long, zone: ZoneId = ZoneId.systemDefault(), locale: Locale = Locale.getDefault()): String {
        return DateTimeFormatter.ofPattern("h:mm a", locale)
            .withZone(zone)
            .format(Instant.ofEpochMilli(epochMillis))
    }

    fun formatEventWhen(
        dateTimeIso: String?,
        dateIso: String?,
        allDayLabel: String,
        locale: Locale
    ): String {
        if (!dateTimeIso.isNullOrBlank()) {
            return try {
                val zdt = ZonedDateTime.parse(dateTimeIso)
                DateTimeFormatter.ofPattern("EEE d MMM, h:mm a", locale).format(zdt)
            } catch (_: Exception) {
                dateTimeIso
            }
        }
        if (!dateIso.isNullOrBlank()) {
            return "$allDayLabel · $dateIso"
        }
        return allDayLabel
    }

    fun eventStartDate(dateTimeIso: String?, dateIso: String?, zone: ZoneId = ZoneId.systemDefault()): String {
        if (!dateTimeIso.isNullOrBlank()) {
            return try {
                ZonedDateTime.parse(dateTimeIso).withZoneSameInstant(zone).toLocalDate().toString()
            } catch (_: Exception) {
                dateIso ?: LocalDate.now(zone).toString()
            }
        }
        return dateIso ?: LocalDate.now(zone).toString()
    }

    fun notesBlock(
        nowLabel: String,
        analysis: VoiceMemoAnalysis,
        sourceRef: String,
        language: AppLanguage
    ): String {
        val summaryHeading = if (language == AppLanguage.SPANISH) "Resumen" else "Summary"
        val transcriptHeading = if (language == AppLanguage.SPANISH) "Transcripción" else "Transcript"
        return buildString {
            append("=== ").append(nowLabel).append(" ===\n")
            append("Source: ").append(sourceRef).append("\n\n")
            append(summaryHeading).append("\n")
            append(analysis.summary.trim().ifBlank { "—" }).append("\n\n")
            append(transcriptHeading).append("\n")
            append(analysis.transcript.trim().ifBlank { "—" }).append("\n\n")
            append("--------------------\n\n")
        }
    }
}
