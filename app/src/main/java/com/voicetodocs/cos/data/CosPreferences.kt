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

    suspend fun clearSession() {
        context.dataStore.edit {
            it.remove(email)
            it.remove(name)
            it.remove(setup)
            it.remove(folderId)
            it.remove(inboxId)
            it.remove(notesId)
            it.remove(recordingNotes)
        }
    }
}
