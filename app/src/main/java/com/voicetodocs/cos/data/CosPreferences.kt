package com.voicetodocs.cos.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "cos_prefs")

class CosPreferences(private val context: Context) {
    private val lang = stringPreferencesKey("language")
    private val email = stringPreferencesKey("email")
    private val name = stringPreferencesKey("display_name")
    private val setup = stringPreferencesKey("setup_complete")
    private val folderId = stringPreferencesKey("drive_folder_id")
    private val inboxId = stringPreferencesKey("drive_inbox_id")
    private val transcriptsId = stringPreferencesKey("doc_transcripts_id")
    private val summariesId = stringPreferencesKey("doc_summaries_id")
    private val sheetId = stringPreferencesKey("sheet_actions_id")

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
        val t = prefs[transcriptsId] ?: return null
        val s = prefs[summariesId] ?: return null
        val sheet = prefs[sheetId] ?: return null
        return DriveStructure(f, inbox, t, s, sheet)
    }

    suspend fun saveDriveStructure(structure: DriveStructure) {
        context.dataStore.edit {
            it[folderId] = structure.folderId
            it[inboxId] = structure.audioInboxId
            it[transcriptsId] = structure.transcriptsDocId
            it[summariesId] = structure.summariesDocId
            it[sheetId] = structure.actionSheetId
        }
    }

    suspend fun clearSession() {
        context.dataStore.edit {
            it.remove(email)
            it.remove(name)
            it.remove(setup)
            it.remove(folderId)
            it.remove(inboxId)
            it.remove(transcriptsId)
            it.remove(summariesId)
            it.remove(sheetId)
        }
    }
}
