package com.voicetodocs.cos.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

enum class AppLanguage(val code: String, val bcp47: String) {
    ENGLISH("en", "en-US"),
    SPANISH("es", "es-US");

    companion object {
        fun fromCode(code: String): AppLanguage =
            entries.find { it.code == code } ?: ENGLISH
    }
}

@Serializable
enum class Domain {
    @SerialName("SIDE_WORK") SIDE_WORK,
    @SerialName("FAMILY") FAMILY,
    @SerialName("FINANCE") FINANCE,
    @SerialName("PERSONAL") PERSONAL,
    @SerialName("RELATIONSHIP") RELATIONSHIP;

    companion object {
        fun parse(raw: String?): Domain {
            val normalized = raw.orEmpty().trim().uppercase()
                .replace("-", "_")
                .replace(" ", "_")
            return entries.find { it.name == normalized } ?: PERSONAL
        }
    }
}

@Serializable
data class ActionItemDraft(
    val title: String,
    val notes: String = "",
    val priority: String = "NORMAL",
    val due_date: String = "",
    val people: String = ""
)

@Serializable
data class VoiceMemoAnalysis(
    val transcript: String,
    val domain: String,
    val bluf: String,
    val action_items: List<ActionItemDraft> = emptyList(),
    val strategic_notes: String = "",
    val clarifications_or_risks: String = "",
    val is_actionable: Boolean = false
) {
    fun domainEnum(): Domain = Domain.parse(domain)

    fun requiresHuman(): Boolean {
        if (domainEnum() == Domain.FINANCE) return true
        if (clarifications_or_risks.isNotBlank()) return true
        return false
    }
}

data class DriveStructure(
    val folderId: String,
    val audioInboxId: String,
    val transcriptsDocId: String,
    val summariesDocId: String,
    val actionSheetId: String
)

data class CalendarItem(
    val id: String,
    val title: String,
    val whenLabel: String,
    val location: String
)

data class MailThread(
    val id: String,
    val from: String,
    val subject: String,
    val snippet: String,
    val plainLanguage: String,
    val messageIdHeader: String,
    val toAddress: String
)

data class SignedInUser(
    val email: String,
    val displayName: String
)

data class EmailDraft(
    val thread: MailThread,
    val suggestedBody: String
)
