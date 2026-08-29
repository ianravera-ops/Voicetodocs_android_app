package com.voicetodocs.cos.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.dataStore by preferencesDataStore(name = "cos_prefs")

class CosPreferences(private val context: Context) {
    private val lang = stringPreferencesKey("language")
    private val email = stringPreferencesKey("email")
    private val name = stringPreferencesKey("display_name")
    private val setup = stringPreferencesKey("setup_complete")
    private val folderId = stringPreferencesKey("drive_folder_id")
    private val inboxId = stringPreferencesKey("drive_inbox_id")
    private val notesId = stringPreferencesKey("doc_notes_id")
    private val recordingNotes = stringPreferencesKey("recording_notes")
    private val vipEmailsKey = stringPreferencesKey("vip_emails")
    private val vipWatermarkKey = stringPreferencesKey("vip_watermark")
    private val vipDigestKey = stringPreferencesKey("vip_digest_items")
    private val vipDigestErrorKey = stringPreferencesKey("vip_digest_error")
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    val languageFlow: Flow<AppLanguage> =
        context.dataStore.data.map { AppLanguage.fromCode(it[lang] ?: "en") }

    suspend fun language(): AppLanguage = languageFlow.first()

    suspend fun setLanguage(value: AppLanguage) {
        context.dataStore.edit { it[lang] = value.code }
    }

    suspend fun user(): SignedInUser? {
        val prefs = context.dataStore.data.first()
        val e = prefs[email] ?: return null
        return SignedInUser(e, prefs[name].orEmpty())
    }

    suspend fun setUser(user: SignedInUser) {
        context.dataStore.edit {
            it[email] = user.email
            it[name] = user.displayName
        }
    }

    suspend fun isSetupComplete(): Boolean =
        context.dataStore.data.map { it[setup] == "1" }.first()

    suspend fun setSetupComplete(complete: Boolean) {
        context.dataStore.edit { it[setup] = if (complete) "1" else "0" }
    }

    suspend fun driveStructure(): DriveStructure? {
        val prefs = context.dataStore.data.first()
        val f = prefs[folderId] ?: return null
        val inbox = prefs[inboxId] ?: return null
        val notes = prefs[notesId] ?: return null
        return DriveStructure(f, inbox, notes)
    }

    suspend fun saveDriveStructure(structure: DriveStructure) {
        context.dataStore.edit {
            it[folderId] = structure.folderId
            it[inboxId] = structure.audioInboxId
            it[notesId] = structure.notesDocId
        }
    }

    suspend fun recordings(): List<RecordingNote> {
        val raw = context.dataStore.data.first()[recordingNotes] ?: return emptyList()
        return runCatching { json.decodeFromString<List<RecordingNote>>(raw) }.getOrDefault(emptyList())
    }

    suspend fun addRecording(note: RecordingNote) {
        val next = listOf(note) + recordings().filter { it.id != note.id }
        context.dataStore.edit { it[recordingNotes] = json.encodeToString(next) }
    }

    suspend fun setRecordingOpen(id: String, open: Boolean) {
        val next = recordings().map { if (it.id == id) it.copy(open = open) else it }
        context.dataStore.edit { it[recordingNotes] = json.encodeToString(next) }
    }

    suspend fun vipEmails(): List<String> {
        val raw = context.dataStore.data.first()[vipEmailsKey] ?: return emptyList()
        return runCatching { json.decodeFromString<List<String>>(raw) }.getOrDefault(emptyList())
    }

    suspend fun setVipEmails(emails: List<String>) {
        context.dataStore.edit {
            it[vipEmailsKey] = json.encodeToString(emails)
            if ((it[vipWatermarkKey] ?: "0").toLongOrNull() == 0L) {
                it[vipWatermarkKey] = System.currentTimeMillis().toString()
            }
        }
    }

    suspend fun vipWatermark(): Long =
        context.dataStore.data.first()[vipWatermarkKey]?.toLongOrNull() ?: 0L

    suspend fun setVipWatermark(millis: Long) {
        context.dataStore.edit { it[vipWatermarkKey] = millis.toString() }
    }

    suspend fun digestItems(): List<VipDigestItem> {
        val raw = context.dataStore.data.first()[vipDigestKey] ?: return emptyList()
        return runCatching { json.decodeFromString<List<VipDigestItem>>(raw) }.getOrDefault(emptyList())
    }

    suspend fun addDigestItems(items: List<VipDigestItem>) {
        val merged = (items + digestItems()).distinctBy { it.threadId }.take(15)
        context.dataStore.edit { it[vipDigestKey] = json.encodeToString(merged) }
    }

    suspend fun digestError(): String? =
        context.dataStore.data.first()[vipDigestErrorKey]?.takeIf { it.isNotBlank() }

    suspend fun setDigestError(message: String?) {
        context.dataStore.edit {
            if (message.isNullOrBlank()) it.remove(vipDigestErrorKey)
            else it[vipDigestErrorKey] = message
        }
    }

    suspend fun clearSession() {
        context.dataStore.edit {
            it.remove(email)
            it.remove(name)
            it.remove(setup)
            it.remove(folderId)
            it.remove(inboxId)
            it.remove(notesId)
            it.remove(recordingNotes)
            it.remove(vipEmailsKey)
            it.remove(vipWatermarkKey)
            it.remove(vipDigestKey)
            it.remove(vipDigestErrorKey)
        }
    }
}
