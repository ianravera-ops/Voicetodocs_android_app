package com.voicetodocs.cos.data

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object CosFormatters {
    fun timestamp(now: Instant = Instant.now(), zone: ZoneId = ZoneId.systemDefault()): String {
        return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm z")
            .withZone(zone)
            .format(now)
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
