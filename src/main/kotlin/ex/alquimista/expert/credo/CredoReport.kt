package ex.alquimista.expert.credo

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.stream.JsonReader
import java.io.StringReader

data class CredoIssue(
    val filename: String,
    val line: Int,
    val column: Int?,
    val columnEnd: Int?,
    val message: String,
    val category: String,
    val check: String,
)

object CredoReport {
    fun parse(output: String): List<CredoIssue>? {
        for (match in Regex("(?m)^\\s*\\{").findAll(output)) {
            val root = runCatching {
                JsonParser.parseReader(JsonReader(StringReader(output.substring(match.range.first)))).asJsonObject
            }.getOrNull() ?: continue
            val issues = root.get("issues")?.takeIf { it.isJsonArray }?.asJsonArray ?: continue
            return issues.mapNotNull { entry ->
                runCatching {
                    val issue = entry.asJsonObject
                    val filename = issue.string("filename")?.takeIf { it.isNotBlank() } ?: return@runCatching null
                    val line = issue.number("line_no")?.takeIf { it > 0 } ?: return@runCatching null
                    val message = issue.string("message")?.takeIf { it.isNotBlank() } ?: return@runCatching null
                    CredoIssue(
                        filename, line, issue.number("column"), issue.number("column_end"), message,
                        issue.string("category").orEmpty(), issue.string("check").orEmpty()
                    )
                }.getOrNull()
            }
        }
        return null
    }

    private fun JsonObject.string(key: String): String? = get(key)?.takeUnless { it.isJsonNull }?.asString
    private fun JsonObject.number(key: String): Int? = get(key)?.takeUnless { it.isJsonNull }?.asInt
}
