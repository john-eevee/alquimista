package dev.yaee.expert.testing

data class DetectedExUnitTest(val displayName: String, val line: Int)

/** Finds conventional ExUnit `test` and StreamData `property` declarations. */
object ExUnitTestDetector {
    private data class Describe(val indentation: Int, val name: String)

    private val describePattern = Regex("^\\s*describe\\s+(?:\"((?:\\\\.|[^\"])*)\"|:([\\w?!]+))\\s+do\\b")
    private val testPattern = Regex("^\\s*(?:test|property)\\s+(?:\"((?:\\\\.|[^\"])*)\"|:([\\w?!]+))\\s*(?:,.*)?\\bdo\\b")

    fun isTestFile(fileName: String): Boolean = fileName.endsWith("_test.exs") || fileName.endsWith("_test.ex")

    fun detect(source: String): List<DetectedExUnitTest> {
        val describeStack = mutableListOf<Describe>()
        val tests = mutableListOf<DetectedExUnitTest>()

        source.lineSequence().forEachIndexed { index, rawLine ->
            val line = stripComment(rawLine)
            val indentation = line.takeWhile(Char::isWhitespace).length
            while (describeStack.lastOrNull()?.indentation?.let { indentation <= it } == true) {
                describeStack.removeAt(describeStack.lastIndex)
            }

            val describeMatch = describePattern.find(line)
            if (describeMatch != null) {
                describeStack += Describe(indentation, label(describeMatch))
                return@forEachIndexed
            }

            val testMatch = testPattern.find(line) ?: return@forEachIndexed
            val testName = label(testMatch)
            val displayName = (describeStack.map { it.name } + testName).joinToString(": ")
            tests += DetectedExUnitTest(displayName, index + 1)
        }

        return tests
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
