package com.voicetodocs.cos.data.google

import com.voicetodocs.cos.data.DriveStructure
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class DriveWorkspace(private val http: GoogleHttp) {

    suspend fun ensureStructure(existing: DriveStructure?): DriveStructure {
        existing?.let { cached ->
            if (fileExists(cached.folderId) &&
                fileExists(cached.audioInboxId) &&
                fileExists(cached.notesDocId)
            ) {
                return cached
            }
        }
        val folder = findOrCreateFolder(FOLDER_NAME, parentId = null)
        val inbox = findOrCreateFolder(AUDIO_FOLDER_NAME, parentId = folder)
        val notes = findOrCreateGoogleFile(
            name = NOTES_DOC_NAME,
            mime = MIME_DOC,
            parentId = folder
        )
        return DriveStructure(
            folderId = folder,
            audioInboxId = inbox,
            notesDocId = notes
        )
    }

    suspend fun uploadAudio(parentId: String, fileName: String, bytes: ByteArray): String {
        val metadata = buildJsonObject {
            put("name", fileName)
            put("parents", JsonArray(listOf(JsonPrimitive(parentId))))
            put("mimeType", "audio/mp4")
        }.toString()
        val body = MultipartBody.Builder()
            .setType("multipart/related".toMediaType())
            .addPart(
                headers = okhttp3.Headers.headersOf("Content-Type", "application/json; charset=UTF-8"),
                body = metadata.toRequestBody("application/json; charset=UTF-8".toMediaType())
            )
            .addPart(
                headers = okhttp3.Headers.headersOf("Content-Type", "audio/mp4"),
                body = bytes.toRequestBody("audio/mp4".toMediaType())
            )
            .build()
        val json = http.postRaw(
            "https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart&fields=id,name",
            body
        )
        return json.string("id")
    }

    private suspend fun fileExists(id: String): Boolean {
        return try {
            http.get("https://www.googleapis.com/drive/v3/files/$id?fields=id,trashed")
            true
        } catch (_: Exception) {
            false
        }
    }

    private suspend fun findOrCreateFolder(name: String, parentId: String?): String {
        findFile(name, MIME_FOLDER, parentId)?.let { return it }
        val body = buildJsonObject {
            put("name", name)
            put("mimeType", MIME_FOLDER)
            if (parentId != null) {
                put("parents", JsonArray(listOf(JsonPrimitive(parentId))))
            }
        }.toString()
        return http.post("https://www.googleapis.com/drive/v3/files?fields=id,name", body).string("id")
    }

    private suspend fun findOrCreateGoogleFile(name: String, mime: String, parentId: String): String {
        findFile(name, mime, parentId)?.let { return it }
        val body = buildJsonObject {
            put("name", name)
            put("mimeType", mime)
            put("parents", JsonArray(listOf(JsonPrimitive(parentId))))
        }.toString()
        return http.post("https://www.googleapis.com/drive/v3/files?fields=id,name", body).string("id")
    }

    private suspend fun findFile(name: String, mime: String, parentId: String?): String? {
        val q = buildString {
            append("name='").append(name.replace("'", "\\'")).append("'")
            append(" and mimeType='").append(mime).append("'")
            append(" and trashed=false")
            if (parentId != null) {
                append(" and '").append(parentId).append("' in parents")
            }
        }
        val encoded = URLEncoder.encode(q, StandardCharsets.UTF_8)
        val json = http.get(
            "https://www.googleapis.com/drive/v3/files?q=$encoded&pageSize=1&fields=files(id,name)&spaces=drive"
        )
        val files = json["files"]?.jsonArray ?: return null
        if (files.isEmpty()) return null
        return files[0].jsonObject.string("id")
    }

    companion object {
        const val FOLDER_NAME = "Voice notes"
        const val AUDIO_FOLDER_NAME = "Audio"
        const val NOTES_DOC_NAME = "Voice notes"
        private const val MIME_FOLDER = "application/vnd.google-apps.folder"
        private const val MIME_DOC = "application/vnd.google-apps.document"
    }
}

private fun JsonObject.string(key: String): String =
    this[key]?.jsonPrimitive?.content ?: error("Missing $key")
