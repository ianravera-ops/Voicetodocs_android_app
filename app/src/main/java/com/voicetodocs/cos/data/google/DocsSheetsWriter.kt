package com.voicetodocs.cos.data.google

import com.voicetodocs.cos.data.CosFormatters
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class DocsSheetsWriter(private val http: GoogleHttp) {

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

    suspend fun insertActionRows(spreadsheetId: String, rows: List<List<String>>) {
        if (rows.isEmpty()) return
        val insert = buildJsonObject {
            put(
                "requests",
                buildJsonArray {
                    add(
                        buildJsonObject {
                            put(
                                "insertDimension",
                                buildJsonObject {
                                    put(
                                        "range",
                                        buildJsonObject {
                                            put("sheetId", 0)
                                            put("dimension", "ROWS")
                                            put("startIndex", 1)
                                            put("endIndex", 1 + rows.size)
                                        }
                                    )
                                    put("inheritFromBefore", false)
                                }
                            )
                        }
                    )
                }
            )
        }.toString()
        http.post("https://sheets.googleapis.com/v4/spreadsheets/$spreadsheetId:batchUpdate", insert)

        val endCol = ('A'.code + CosFormatters.SHEET_HEADERS.size - 1).toChar()
        val range = "A2:${endCol}${1 + rows.size}"
        val body = buildJsonObject {
            put(
                "values",
                JsonArray(
                    rows.map { row -> JsonArray(row.map { JsonPrimitive(it) }) }
                )
            )
        }.toString()
        http.put(
            "https://sheets.googleapis.com/v4/spreadsheets/$spreadsheetId/values/$range?valueInputOption=RAW",
            body
        )
    }
}
