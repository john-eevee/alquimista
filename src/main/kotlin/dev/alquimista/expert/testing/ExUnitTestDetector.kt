package dev.alquimista.expert.testing

data class DetectedExUnitTest(val displayName: String, val line: Int)
enum class ExUnitTargetKind { MODULE, DESCRIBE, TEST }
data class ExUnitTarget(val displayName: String, val line: Int, val kind: ExUnitTargetKind)

/** Finds conventional ExUnit `test` and StreamData `property` declarations. */
object ExUnitTestDetector {
    private data class Describe(val indentation: Int, val name: String)

    private val describePattern = Regex("^\\s*describe\\s+(?:\"((?:\\\\.|[^\"])*)\"|:([\\w?!]+))\\s+do\\b")
    private val testPattern = Regex("^\\s*(?:test|property)\\s+(?:\"((?:\\\\.|[^\"])*)\"|:([\\w?!]+))\\s*(?:,.*)?\\bdo\\b")
    private val modulePattern = Regex("^\\s*defmodule\\s+([A-Z][\\w.]*)\\s+do\\b")

    fun isTestFile(fileName: String): Boolean = fileName.endsWith("_test.exs") || fileName.endsWith("_test.ex")

    fun detect(source: String): List<DetectedExUnitTest> = targets(source)
        .filter { it.kind == ExUnitTargetKind.TEST }
        .map { DetectedExUnitTest(it.displayName, it.line) }

    fun targets(source: String): List<ExUnitTarget> {
        val describeStack = mutableListOf<Describe>()
        val targets = mutableListOf<ExUnitTarget>()

        source.lineSequence().forEachIndexed { index, rawLine ->
            val line = stripComment(rawLine)
            if (line.isBlank()) return@forEachIndexed
            val indentation = line.takeWhile(Char::isWhitespace).length
            while (describeStack.lastOrNull()?.indentation?.let { indentation <= it } == true) {
                describeStack.removeAt(describeStack.lastIndex)
            }

            val module = modulePattern.find(line)
            if (module != null) {
                targets += ExUnitTarget(module.groupValues[1], index + 1, ExUnitTargetKind.MODULE)
                return@forEachIndexed
            }

            val describeMatch = describePattern.find(line)
            if (describeMatch != null) {
                describeStack += Describe(indentation, label(describeMatch))
                targets += ExUnitTarget(describeStack.joinToString(": ") { it.name }, index + 1, ExUnitTargetKind.DESCRIBE)
                return@forEachIndexed
            }

            val testMatch = testPattern.find(line) ?: return@forEachIndexed
            val testName = label(testMatch)
            val displayName = (describeStack.map { it.name } + testName).joinToString(": ")
            targets += ExUnitTarget(displayName, index + 1, ExUnitTargetKind.TEST)
        }

        return targets
    }

    private fun label(match: MatchResult): String =
        match.groupValues.getOrNull(1)?.takeIf(String::isNotEmpty)
            ?: match.groupValues.getOrNull(2).orEmpty()

    private fun stripComment(line: String): String {
        var quote: Char? = null
        var escaped = false
        line.forEachIndexed { index, char ->
            if (escaped) {
                escaped = false
            } else if (char == '\\' && quote != null) {
                escaped = true
            } else if (quote != null && char == quote) {
                quote = null
            } else if (quote == null && (char == '"' || char == '\'')) {
                quote = char
            } else if (quote == null && char == '#') {
                return line.substring(0, index)
            }
        }
        return line
    }
}
