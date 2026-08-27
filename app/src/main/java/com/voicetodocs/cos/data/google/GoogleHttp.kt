package com.voicetodocs.cos.data.google

import com.voicetodocs.cos.data.CosException
import com.voicetodocs.cos.data.TokenExpiredException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

class GoogleHttp(
    private val tokenProvider: () -> String?,
    private val missingNetworkMessage: String,
    private val httpErrorTemplate: String
) {
    val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS)
        .writeTimeout(180, TimeUnit.SECONDS)
        .build()

    suspend fun get(url: String): JsonObject = execute(
        Request.Builder().url(url).get().build()
    )

    suspend fun post(url: String, jsonBody: String): JsonObject = execute(
        Request.Builder()
            .url(url)
            .post(jsonBody.toRequestBody(JSON))
            .build()
    )

    suspend fun put(url: String, jsonBody: String): JsonObject = execute(
        Request.Builder()
            .url(url)
            .put(jsonBody.toRequestBody(JSON))
            .build()
    )

    suspend fun postRaw(url: String, body: RequestBody, extraHeaders: Map<String, String> = emptyMap()): JsonObject {
        val builder = Request.Builder().url(url).post(body)
        extraHeaders.forEach { (k, v) -> builder.addHeader(k, v) }
        return execute(builder.build())
    }

    private suspend fun execute(request: Request): JsonObject = withContext(Dispatchers.IO) {
        val token = tokenProvider() ?: throw TokenExpiredException()
        val authed = request.newBuilder()
            .header("Authorization", "Bearer $token")
            .build()
        val response = try {
            client.newCall(authed).execute()
        } catch (e: IOException) {
            throw CosException(missingNetworkMessage, e)
        }
        response.use { resp ->
            val body = resp.body?.string().orEmpty()
            if (resp.code == 401) {
                throw TokenExpiredException()
            }
            if (!resp.isSuccessful) {
                throw CosException(httpErrorTemplate.format("${resp.code} $body"))
            }
            if (body.isBlank()) return@use JsonObject(emptyMap())
            json.parseToJsonElement(body).jsonObject
        }
    }

    companion object {
        private val JSON = "application/json; charset=utf-8".toMediaType()
    }
}
