package dev.yaee.expert.credo

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
        val toColumn = if (columnEnd == null) count else (columnEnd - 1).coerceIn(fromColumn + 1, count)
        return text.offsetByCodePoints(start, fromColumn) to text.offsetByCodePoints(start, toColumn)
    }
}
