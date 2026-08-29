package com.voicetodocs.cos.data

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
data class VoiceMemoAnalysis(
    val transcript: String,
    val summary: String
)

@Serializable
data class RecordingNote(
    val id: String,
    val createdAtMillis: Long,
    val summary: String,
    val open: Boolean = true,
    val notesDocId: String = ""
)

data class CalendarItem(
    val id: String,
    val title: String,
    val whenLabel: String,
    val location: String,
    val startDate: String
)

data class DriveStructure(
    val folderId: String,
    val audioInboxId: String,
    val notesDocId: String
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
