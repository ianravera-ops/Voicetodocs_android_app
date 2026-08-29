package com.voicetodocs.cos.data.google

import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class DocsWriter(private val http: GoogleHttp) {

    suspend fun prependDocument(documentId: String, text: String) {
        val body = buildJsonObject {
            put(
                "requests",
                buildJsonArray {
                    add(
                        buildJsonObject {
                            put(
                                "insertText",
                                buildJsonObject {
                                    put(
                                        "location",
                                        buildJsonObject { put("index", 1) }
                                    )
                                    put("text", text)
                                }
                            )
                        }
                    )
                    val headingEnd = text.indexOf('\n').let { if (it > 0) it + 1 else text.length.coerceAtLeast(2) }
                    add(
                        buildJsonObject {
                            put(
                                "updateTextStyle",
                                buildJsonObject {
                                    put(
                                        "range",
                                        buildJsonObject {
                                            put("startIndex", 1)
                                            put("endIndex", headingEnd + 1)
                                        }
                                    )
                                    put(
                                        "textStyle",
                                        buildJsonObject {
                                            put("bold", true)
                                            put(
                                                "fontSize",
                                                buildJsonObject {
                                                    put("magnitude", 14)
                                                    put("unit", "PT")
                                                }
                                            )
                                        }
                                    )
                                    put("fields", "bold,fontSize")
                                }
                            )
                        }
                    )
                }
            )
        }.toString()
        http.post("https://docs.googleapis.com/v1/documents/$documentId:batchUpdate", body)
    }
}
