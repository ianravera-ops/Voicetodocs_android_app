package com.voicetodocs.cos.data.google

import com.voicetodocs.cos.data.CosFormatters
import com.voicetodocs.cos.data.DriveStructure
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
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
                fileExists(cached.transcriptsDocId) &&
                fileExists(cached.summariesDocId) &&
                fileExists(cached.actionSheetId)
            ) {
                return cached
            }
        }
        val folder = findOrCreateFolder("CoS", parentId = null)
        val inbox = findOrCreateFolder("Audio_Inbox", parentId = folder)
        val transcripts = findOrCreateGoogleFile(
            name = "CoS_Voice_Transcripts",
            mime = MIME_DOC,
            parentId = folder
        )
        val summaries = findOrCreateGoogleFile(
            name = "CoS_Executive_Summaries",
            mime = MIME_DOC,
            parentId = folder
        )
        val sheet = findOrCreateGoogleFile(
            name = "CoS_Action_Register",
            mime = MIME_SHEET,
            parentId = folder
        )
        ensureSheetHeaders(sheet)
        return DriveStructure(
            folderId = folder,
            audioInboxId = inbox,
            transcriptsDocId = transcripts,
            summariesDocId = summaries,
            actionSheetId = sheet
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

    private suspend fun ensureSheetHeaders(spreadsheetId: String) {
        val range = URLEncoder.encode("A1:O1", StandardCharsets.UTF_8)
        val existing = try {
            http.get(
                "https://sheets.googleapis.com/v4/spreadsheets/$spreadsheetId/values/$range"
            )
        } catch (_: Exception) {
            null
        }
        val first = existing?.get("values")?.jsonArray
            ?.firstOrNull()
            ?.jsonArray
            ?.map { it.jsonPrimitive.content }
            .orEmpty()
        if (first == CosFormatters.SHEET_HEADERS) return
        val body = buildJsonObject {
            put(
                "values",
                JsonArray(
                    listOf(
                        JsonArray(CosFormatters.SHEET_HEADERS.map { JsonPrimitive(it) })
                    )
                )
            )
        }.toString()
        http.put(
            "https://sheets.googleapis.com/v4/spreadsheets/$spreadsheetId/values/A1:O1?valueInputOption=RAW",
            body
        )
    }

    companion object {
        private const val MIME_FOLDER = "application/vnd.google-apps.folder"
        private const val MIME_DOC = "application/vnd.google-apps.document"
        private const val MIME_SHEET = "application/vnd.google-apps.spreadsheet"
    }
}

private fun JsonObject.string(key: String): String =
    this[key]?.jsonPrimitive?.content ?: error("Missing $key")
