package com.voicetodocs.cos.data.google

import android.util.Base64
import com.voicetodocs.cos.data.AppLanguage
import com.voicetodocs.cos.data.CalendarItem
import com.voicetodocs.cos.data.CosFormatters
import com.voicetodocs.cos.data.MailThread
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.Locale

class GmailCalendarClient(private val http: GoogleHttp) {

    suspend fun upcomingEvents(language: AppLanguage, allDayLabel: String): List<CalendarItem> {
        val timeMin = URLEncoder.encode(Instant.now().toString(), StandardCharsets.UTF_8)
        val url =
            "https://www.googleapis.com/calendar/v3/calendars/primary/events" +
                "?timeMin=$timeMin&singleEvents=true&orderBy=startTime&maxResults=5" +
                "&fields=items(id,summary,location,start)"
        val json = http.get(url)
        val items = json["items"]?.jsonArray ?: return emptyList()
        val locale = if (language == AppLanguage.SPANISH) Locale("es", "US") else Locale.US
        return items.mapNotNull { el ->
            val obj = el.jsonObject
            val start = obj["start"]?.jsonObject
            val dateTime = start?.stringOrNull("dateTime")
            val date = start?.stringOrNull("date")
            CalendarItem(
                id = obj.stringOrNull("id") ?: return@mapNotNull null,
                title = obj.stringOrNull("summary") ?: "(No title)",
                whenLabel = CosFormatters.formatEventWhen(dateTime, date, allDayLabel, locale),
                location = obj.stringOrNull("location").orEmpty()
            )
        }
    }

    suspend fun importantThreads(): List<MailThread> {
        val q = URLEncoder.encode(
            "in:inbox category:primary -category:promotions -category:social newer_than:14d",
            StandardCharsets.UTF_8
        )
        val list = http.get(
            "https://gmail.googleapis.com/gmail/v1/users/me/threads?q=$q&maxResults=8"
        )
        val threads = list["threads"]?.jsonArray ?: return emptyList()
        val out = mutableListOf<MailThread>()
        for (el in threads) {
            if (out.size >= 3) break
            val id = el.jsonObject.stringOrNull("id") ?: continue
            val thread = fetchThread(id) ?: continue
            if (isNoise(thread.from, thread.subject)) continue
            out += thread
        }
        return out
    }

    suspend fun fetchThread(threadId: String): MailThread? {
        val json = http.get(
            "https://gmail.googleapis.com/gmail/v1/users/me/threads/$threadId?format=full"
        )
        val messages = json["messages"]?.jsonArray ?: return null
        val last = messages.last().jsonObject
        val payload = last["payload"]?.jsonObject ?: return null
        val headers = payload["headers"]?.jsonArray.orEmpty()
        fun header(name: String): String {
            for (h in headers) {
                val obj = h.jsonObject
                if (obj.stringOrNull("name").equals(name, ignoreCase = true)) {
                    return obj.stringOrNull("value").orEmpty()
                }
            }
            return ""
        }
        val from = header("From")
        val subject = header("Subject")
        val messageId = header("Message-ID")
        val snippet = json.stringOrNull("snippet").orEmpty()
        val body = extractText(payload).ifBlank { snippet }
        val to = extractEmail(from)
        return MailThread(
            id = threadId,
            from = from,
            subject = subject.ifBlank { "(No subject)" },
            snippet = snippet,
            plainLanguage = body.take(2500),
            messageIdHeader = messageId,
            toAddress = to
        )
    }

    suspend fun sendReply(
        userEmail: String,
        thread: MailThread,
        bodyText: String
    ) {
        val subject = if (thread.subject.startsWith("Re:", ignoreCase = true)) {
            thread.subject
        } else {
            "Re: ${thread.subject}"
        }
        val rfc = buildString {
            append("From: ").append(userEmail).append("\r\n")
            append("To: ").append(thread.toAddress.ifBlank { thread.from }).append("\r\n")
            append("Subject: ").append(subject).append("\r\n")
            if (thread.messageIdHeader.isNotBlank()) {
                append("In-Reply-To: ").append(thread.messageIdHeader).append("\r\n")
                append("References: ").append(thread.messageIdHeader).append("\r\n")
            }
            append("MIME-Version: 1.0\r\n")
            append("Content-Type: text/plain; charset=UTF-8\r\n")
            append("\r\n")
            append(bodyText)
        }
        val raw = Base64.encodeToString(
            rfc.toByteArray(StandardCharsets.UTF_8),
            Base64.URL_SAFE or Base64.NO_WRAP
        )
        val body = """{"raw":"$raw","threadId":"${thread.id}"}"""
        http.post("https://gmail.googleapis.com/gmail/v1/users/me/messages/send", body)
    }

    private fun isNoise(from: String, subject: String): Boolean {
        val blob = "$from $subject".lowercase(Locale.US)
        val skip = listOf(
            "noreply", "no-reply", "do-not-reply", "newsletter", "promo",
            "unsubscribe", "notification", "mailer-daemon"
        )
        return skip.any { blob.contains(it) }
    }

    private fun extractEmail(from: String): String {
        val match = Regex("<([^>]+)>").find(from)
        return match?.groupValues?.get(1) ?: from.trim()
    }

    private fun extractText(payload: JsonObject): String {
        val mime = payload.stringOrNull("mimeType").orEmpty()
        val data = payload["body"]?.jsonObject?.stringOrNull("data")
        if (mime.startsWith("text/") && !data.isNullOrBlank()) {
            return decode(data)
        }
        val parts = payload["parts"]?.jsonArray ?: return ""
        var htmlFallback = ""
        for (part in parts) {
            val obj = part.jsonObject
            val partMime = obj.stringOrNull("mimeType").orEmpty()
            val nested = extractText(obj)
            if (partMime.equals("text/plain", true) && nested.isNotBlank()) return nested
            if (partMime.equals("text/html", true) && nested.isNotBlank()) htmlFallback = nested
            if (nested.isNotBlank() && htmlFallback.isBlank()) htmlFallback = nested
        }
        return htmlFallback.replace(Regex("<[^>]+>"), " ").replace(Regex("\\s+"), " ").trim()
    }

    private fun decode(data: String): String {
        val bytes = Base64.decode(data, Base64.URL_SAFE)
        return String(bytes, StandardCharsets.UTF_8)
    }
}

private fun JsonObject.stringOrNull(key: String): String? =
    this[key]?.jsonPrimitive?.content
