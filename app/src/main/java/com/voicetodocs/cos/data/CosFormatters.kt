package com.voicetodocs.cos.data

import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

object CosFormatters {
    val SHEET_HEADERS = listOf(
        "id",
        "created_at",
        "source",
        "source_ref",
        "domain",
        "priority",
        "status",
        "title",
        "notes",
        "due_date",
        "people",
        "bluf",
        "draft_link",
        "master_log_ref",
        "requires_human"
    )

    fun timestamp(now: Instant = Instant.now(), zone: ZoneId = ZoneId.systemDefault()): String {
        return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm z")
            .withZone(zone)
            .format(now)
    }

    fun transcriptBlock(nowLabel: String, transcript: String, sourceRef: String): String {
        return buildString {
            append("=== ").append(nowLabel).append(" ===\n")
            append("Source: ").append(sourceRef).append("\n\n")
            append(transcript.trim()).append("\n\n")
            append("--------------------\n\n")
        }
    }

    fun executiveSummaryBlock(
        nowLabel: String,
        analysis: VoiceMemoAnalysis,
        sourceRef: String,
        language: AppLanguage
    ): String {
        val actions = if (analysis.action_items.isEmpty()) {
            "—"
        } else {
            analysis.action_items.joinToString("\n") { "- ${it.title}" }
        }
        val blufHeading = if (language == AppLanguage.SPANISH) "LO ESENCIAL" else "BLUF"
        val actionsHeading = if (language == AppLanguage.SPANISH) "ACCIONES" else "ACTIONS"
        val contextHeading = if (language == AppLanguage.SPANISH) "CONTEXTO" else "CONTEXT"
        val risksHeading = if (language == AppLanguage.SPANISH) "RIESGOS" else "RISKS"
        val financeNote = if (analysis.domainEnum() == Domain.FINANCE) {
            if (language == AppLanguage.SPANISH) {
                "\n(Tema de dinero: solo se marca para revisión humana. Sin banca en esta versión.)"
            } else {
                "\n(Finance flag only — human review required. No banking in this version.)"
            }
        } else {
            ""
        }
        return buildString {
            append("=== ").append(nowLabel).append(" ===\n")
            append("Domain: ").append(analysis.domainEnum().name).append(financeNote).append("\n")
            append("Source: ").append(sourceRef).append("\n\n")
            append(blufHeading).append("\n").append(analysis.bluf.trim()).append("\n\n")
            append(actionsHeading).append("\n").append(actions).append("\n\n")
            append(contextHeading).append("\n").append(analysis.strategic_notes.trim().ifBlank { "—" }).append("\n\n")
            append(risksHeading).append("\n").append(analysis.clarifications_or_risks.trim().ifBlank { "—" }).append("\n\n")
            append("--------------------\n\n")
        }
    }

    fun actionRows(
        analysis: VoiceMemoAnalysis,
        source: String,
        sourceRef: String,
        masterLogRef: String,
        createdAt: String
    ): List<List<String>> {
        val domain = analysis.domainEnum()
        val requiresHuman = if (analysis.requiresHuman()) "TRUE" else "FALSE"
        val items = analysis.action_items.ifEmpty {
            if (analysis.is_actionable) {
                listOf(ActionItemDraft(title = analysis.bluf.take(80), notes = analysis.strategic_notes))
            } else {
                emptyList()
            }
        }
        return items.map { item ->
            listOf(
                UUID.randomUUID().toString(),
                createdAt,
                source,
                sourceRef,
                domain.name,
                item.priority.ifBlank { "NORMAL" },
                "OPEN",
                item.title,
                item.notes,
                item.due_date,
                item.people,
                analysis.bluf,
                "",
                masterLogRef,
                requiresHuman
            )
        }
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
}
