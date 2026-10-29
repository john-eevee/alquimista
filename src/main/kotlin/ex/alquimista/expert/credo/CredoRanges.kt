package ex.alquimista.expert.credo

object CredoRanges {
    fun offsets(text: String, line: Int, column: Int?, columnEnd: Int?): Pair<Int, Int>? {
        if (line < 1) return null
        var start = 0
        repeat(line - 1) {
            val newline = text.indexOf('\n', start)
            if (newline < 0) return null
            start = newline + 1
        }
        val end = text.indexOf('\n', start).let { if (it < 0) text.length else it }
        val contentEnd = if (end > start && text[end - 1] == '\r') end - 1 else end
        if (contentEnd == start) return start to start
        val count = text.codePointCount(start, contentEnd)
        val fromColumn = ((column ?: 1) - 1).coerceIn(0, count - 1)
        val from = text.offsetByCodePoints(start, fromColumn)
        if (columnEnd == null) return from to contentEnd
        val graphemes = (columnEnd.toLong() - (column ?: 1).toLong()).coerceAtLeast(1)
        val matcher = GRAPHEME.matcher(text).region(from, contentEnd)
        var to = from
        var remaining = graphemes
        while (remaining > 0 && matcher.find()) {
            to = matcher.end()
            remaining--
        }
        return from to to
    }

    private val GRAPHEME = java.util.regex.Pattern.compile("\\X")
}
