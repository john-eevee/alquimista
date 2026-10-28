package dev.yaee.expert.testing

data class DetectedExUnitTest(val displayName: String, val line: Int)

/** Finds conventional ExUnit `test` and StreamData `property` declarations. */
object ExUnitTestDetector {
    private data class Describe(val indentation: Int, val name: String)

    private val describePattern = Regex("^\\s*describe\\s+(?:\"((?:\\\\.|[^\"])*)\"|:([\\w?!]+))\\s+do\\b")
    private val testPattern = Regex("^\\s*(?:test|property)\\s+(?:\"((?:\\\\.|[^\"])*)\"|:([\\w?!]+))\\s*(?:,.*)?\\bdo\\b")

    fun detect(source: String): List<DetectedExUnitTest> {
        val describeStack = mutableListOf<Describe>()
        val tests = mutableListOf<DetectedExUnitTest>()

        source.lineSequence().forEachIndexed { index, rawLine ->
            val line = rawLine.substringBefore('#')
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
}
