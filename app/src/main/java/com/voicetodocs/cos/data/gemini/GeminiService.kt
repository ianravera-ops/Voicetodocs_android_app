package com.voicetodocs.cos.data.gemini

import com.voicetodocs.cos.BuildConfig
import com.voicetodocs.cos.data.AppLanguage
import com.voicetodocs.cos.data.CosException
import com.voicetodocs.cos.data.MailThread
import com.voicetodocs.cos.data.VoiceMemoAnalysis
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.Base64
import java.util.concurrent.TimeUnit

class GeminiService(
    private val missingKeyMessage: String,
    private val networkMessage: String,
    private val geminiErrorTemplate: String
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS)
        .writeTimeout(180, TimeUnit.SECONDS)
        .build()

    fun hasKey(): Boolean = BuildConfig.GEMINI_API_KEY.isNotBlank()

    suspend fun analyzeVoiceMemo(
        audioBytes: ByteArray,
        language: AppLanguage
    ): VoiceMemoAnalysis {
        ensureKey()
        val summaryLang = if (language == AppLanguage.SPANISH) "Spanish" else "English"
        val prompt = """
            Transcribe the voice memo. Keep the transcript in the spoken language (auto-detect; do not translate the transcript).
            Write a short executive summary in $summaryLang. Capture the point, any follow-ups, and anything unclear.
            Return JSON only matching the schema.
        """.trimIndent()
        val text = generate(
            prompt = prompt,
            inlineMime = "audio/mp4",
            inlineB64 = Base64.getEncoder().encodeToString(audioBytes),
            schema = MEMO_SCHEMA
        )
        return try {
            json.decodeFromString(VoiceMemoAnalysis.serializer(), text)
        } catch (e: Exception) {
            throw CosException(geminiErrorTemplate.format(text.take(180)), e)
        }
    }

    suspend fun plainLanguageEmails(
        threads: List<MailThread>,
        language: AppLanguage
    ): List<MailThread> {
        if (threads.isEmpty()) return threads
        ensureKey()
        val lang = if (language == AppLanguage.SPANISH) "Spanish" else "English"
        val payload = threads.mapIndexed { i, t ->
            "EMAIL ${i + 1}\nFrom: ${t.from}\nSubject: ${t.subject}\nBody:\n${t.plainLanguage.take(1200)}"
        }.joinToString("\n\n")
        val prompt = """
            Rewrite each email in plain $lang for someone who is about 65 and not technical.
            Skip marketing noise. One or two short sentences per email.
            Return JSON: {"items":[{"index":1,"plain":"..."}]}
            $payload
        """.trimIndent()
        val text = generate(prompt, schema = PLAIN_EMAIL_SCHEMA)
        return try {
            val obj = JSONObject(text)
            val items = obj.optJSONArray("items") ?: JSONArray()
            val byIndex = mutableMapOf<Int, String>()
            for (i in 0 until items.length()) {
                val item = items.getJSONObject(i)
                byIndex[item.optInt("index")] = item.optString("plain")
            }
            threads.mapIndexed { i, t ->
                val plain = byIndex[i + 1]
                if (plain.isNullOrBlank()) t else t.copy(plainLanguage = plain)
            }
        } catch (e: Exception) {
            throw CosException(geminiErrorTemplate.format(text.take(180)), e)
        }
    }

    private fun ensureKey() {
        if (!hasKey()) throw CosException(missingKeyMessage)
    }

    private suspend fun generate(
        prompt: String,
        inlineMime: String? = null,
        inlineB64: String? = null,
        schema: JSONObject
    ): String = withContext(Dispatchers.IO) {
        val parts = JSONArray()
        parts.put(JSONObject().put("text", prompt))
        if (inlineMime != null && inlineB64 != null) {
            parts.put(
                JSONObject().put(
                    "inline_data",
                    JSONObject()
                        .put("mime_type", inlineMime)
                        .put("data", inlineB64)
                )
            )
        }
        val body = JSONObject()
            .put(
                "contents",
                JSONArray().put(JSONObject().put("parts", parts))
            )
            .put(
                "generationConfig",
                JSONObject()
                    .put("temperature", 0.2)
                    .put("responseMimeType", "application/json")
                    .put("responseSchema", schema)
            )
        val request = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/$MODEL:generateContent")
            .header("x-goog-api-key", BuildConfig.GEMINI_API_KEY)
            .header("Content-Type", "application/json")
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()
        val response = try {
            client.newCall(request).execute()
        } catch (e: IOException) {
            throw CosException(networkMessage, e)
        }
        response.use { resp ->
            val raw = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                throw CosException(geminiErrorTemplate.format("${resp.code} ${raw.take(300)}"))
            }
            extractText(raw)
        }
    }

    private fun extractText(raw: String): String {
        val parsed = json.parseToJsonElement(raw).jsonObject
        val candidates = parsed["candidates"]?.jsonArray
            ?: throw CosException(geminiErrorTemplate.format(raw.take(200)))
        if (candidates.isEmpty()) {
            throw CosException(geminiErrorTemplate.format("empty candidates"))
        }
        val parts = candidates[0].jsonObject["content"]?.jsonObject?.get("parts")?.jsonArray
            ?: throw CosException(geminiErrorTemplate.format("no content"))
        val text = parts.joinToString("") { el ->
            el.jsonObject["text"]?.jsonPrimitive?.content.orEmpty()
        }.trim()
        val cleaned = text.removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        if (cleaned.isBlank()) throw CosException(geminiErrorTemplate.format("blank response"))
        return cleaned
    }

    companion object {
        const val MODEL = "gemini-3.6-flash"

        private val MEMO_SCHEMA = JSONObject(
            """
            {
              "type": "object",
              "properties": {
                "transcript": { "type": "string" },
                "summary": { "type": "string" }
              },
              "required": ["transcript","summary"]
            }
            """.trimIndent()
        )

        private val PLAIN_EMAIL_SCHEMA = JSONObject(
            """
            {
              "type": "object",
              "properties": {
                "items": {
                  "type": "array",
                  "items": {
                    "type": "object",
                    "properties": {
                      "index": { "type": "integer" },
                      "plain": { "type": "string" }
                    },
                    "required": ["index","plain"]
                  }
                }
              },
              "required": ["items"]
            }
            """.trimIndent()
        )
    }
}
