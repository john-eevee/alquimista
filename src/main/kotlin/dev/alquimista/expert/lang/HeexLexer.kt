package dev.alquimista.expert.lang

import com.intellij.lexer.LexerBase
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType
import dev.alquimista.expert.grammar.EX_LBRACE
import dev.alquimista.expert.grammar.EX_PERCENT_LBRACE
import dev.alquimista.expert.grammar.EX_RBRACE
import dev.alquimista.expert.grammar.lexer.ExLexer

private fun token(name: String) = IElementType(name, HeexLanguage.INSTANCE)
internal val HEEX_TEXT = token("HEEX_TEXT")
internal val HEEX_TAG_DELIMITER = token("HEEX_TAG_DELIMITER")
internal val HEEX_TAG_NAME = token("HEEX_TAG_NAME")
internal val HEEX_ATTRIBUTE = token("HEEX_ATTRIBUTE")
internal val HEEX_STRING = token("HEEX_STRING")
internal val HEEX_COMMENT = token("HEEX_COMMENT")
internal val HEEX_ENTITY = token("HEEX_ENTITY")
internal val HEEX_EXPRESSION_DELIMITER = token("HEEX_EXPRESSION_DELIMITER")

/** Reconstructs the enclosing template state on restart, including nested opt-out tags. */
class HeexLexer : LexerBase() {
    private data class Part(val start: Int, val end: Int, val type: IElementType)
    private data class Tag(val name: String, val noCurly: Boolean)
    private var buffer: CharSequence = ""
    private var endOffset = 0
    private var startOffset = 0
    private var parts: List<Part> = emptyList()
    private var index = 0

    override fun start(buffer: CharSequence, startOffset: Int, endOffset: Int, initialState: Int) {
        this.buffer = buffer
        this.endOffset = endOffset
        this.startOffset = startOffset
        parts = scan(buffer, endOffset)
        index = parts.indexOfFirst { it.end > startOffset }.let { if (it < 0) parts.size else it }
    }

    override fun getState(): Int = 0
    override fun getTokenType(): IElementType? = parts.getOrNull(index)?.type
    override fun getTokenStart(): Int = parts.getOrNull(index)?.start?.coerceAtLeast(startOffset) ?: endOffset
    override fun getTokenEnd(): Int = parts.getOrNull(index)?.end ?: endOffset
    override fun advance() { if (index < parts.size) index++ }
    override fun getBufferSequence(): CharSequence = buffer
    override fun getBufferEnd(): Int = endOffset

    private fun scan(text: CharSequence, end: Int): List<Part> {
        val result = mutableListOf<Part>()
        val stack = mutableListOf<Tag>()
        fun emit(start: Int, stop: Int, type: IElementType) {
            if (stop > start) result += Part(start, stop, type)
        }
        fun starts(pos: Int, value: String): Boolean = pos + value.length <= end &&
            value.indices.all { text[pos + it] == value[it] }
        fun find(pos: Int, value: String): Int {
            val found = text.indexOf(value, pos)
            return if (found < 0 || found + value.length > end) -1 else found
        }
        fun addElixir(start: Int, stop: Int) {
            val lexer = ElixirSyntaxHighlighter().highlightingLexer
            lexer.start(text, start, stop, 0)
            while (lexer.tokenType != null) {
                emit(lexer.tokenStart, lexer.tokenEnd, lexer.tokenType!!)
                lexer.advance()
            }
        }
        fun curlyEnd(start: Int): Int {
            val lexer = ExLexer()
            lexer.start(text, start, end, 0)
            var depth = 0
            while (lexer.tokenType != null) {
                when (lexer.tokenType) {
                    EX_LBRACE, EX_PERCENT_LBRACE -> depth++
                    EX_RBRACE -> if (depth == 0) return lexer.tokenStart else depth--
                }
                lexer.advance()
            }
            return end
        }
        fun expression(start: Int, eex: Boolean): Int {
            val opener = if (!eex) 1 else if (starts(start, "<%=")) 3 else 2
            emit(start, start + opener, HEEX_EXPRESSION_DELIMITER)
            val body = start + opener
            val close = if (eex) find(body, "%>").let { if (it < 0) end else it } else curlyEnd(body)
            addElixir(body, close)
            val closeEnd = (close + if (eex) 2 else 1).coerceAtMost(end)
            if (close < end) emit(close, closeEnd, HEEX_EXPRESSION_DELIMITER)
            return closeEnd
        }
        fun comment(start: Int, marker: String): Int {
            val close = find(start, marker)
            val stop = if (close < 0) end else close + marker.length
            emit(start, stop, HEEX_COMMENT)
            return stop
        }

        var pos = 0
        while (pos < end) {
            val noCurly = stack.lastOrNull()?.noCurly == true
            val rawTag = stack.lastOrNull()?.name?.lowercase()?.takeIf { it == "script" || it == "style" }
            val closesRaw = rawTag != null && pos + rawTag.length + 2 <= end &&
                text.subSequence(pos, pos + rawTag.length + 2).toString().equals("</$rawTag", true) &&
                (pos + rawTag.length + 2 == end || text[pos + rawTag.length + 2].let { it.isWhitespace() || it == '>' })
            when {
                starts(pos, "<%!--") -> pos = comment(pos, "--%>")
                starts(pos, "<%#") -> pos = comment(pos, "%>")
                starts(pos, "<!--") -> pos = comment(pos, "-->")
                starts(pos, "<%") && !starts(pos, "<%%") -> pos = expression(pos, true)
                text[pos] == '{' && !noCurly -> pos = expression(pos, false)
                text[pos] == '<' && rawTag != null && !closesRaw -> { emit(pos, pos + 1, HEEX_TEXT); pos++ }
                text[pos] == '<' && pos + 1 < end && (text[pos + 1].isLetter() || text[pos + 1] in charArrayOf('.', ':', '/')) -> {
                    val closing = starts(pos, "</")
                    val delimiterEnd = pos + if (closing) 2 else 1
                    emit(pos, delimiterEnd, HEEX_TAG_DELIMITER)
                    pos = delimiterEnd
                    val nameStart = pos
                    while (pos < end && (text[pos].isLetterOrDigit() || text[pos] in charArrayOf('.', ':', '-', '_'))) pos++
                    if (pos == nameStart) continue
                    val name = text.subSequence(nameStart, pos).toString()
                    emit(nameStart, pos, HEEX_TAG_NAME)
                    var disabled = noCurly || name.equals("script", true) || name.equals("style", true)
                    var selfClosing = false
                    var closed = false
                    while (pos < end) {
                        val start = pos
                        when {
                            starts(pos, "/>") -> {
                                pos += 2; emit(start, pos, HEEX_TAG_DELIMITER); selfClosing = true; closed = true; break
                            }
                            text[pos] == '>' -> {
                                pos++; emit(start, pos, HEEX_TAG_DELIMITER); closed = true; break
                            }
                            text[pos].isWhitespace() -> {
                                while (pos < end && text[pos].isWhitespace()) pos++
                                emit(start, pos, TokenType.WHITE_SPACE)
                            }
                            text[pos] == '{' && !noCurly -> pos = expression(pos, false)
                            text[pos] == '"' || text[pos] == '\'' -> {
                                val quote = text[pos++]
                                while (pos < end && text[pos] != quote) pos++
                                if (pos < end) pos++
                                emit(start, pos, HEEX_STRING)
                            }
                            text[pos] == '=' -> { pos++; emit(start, pos, HEEX_TAG_DELIMITER) }
                            else -> {
                                while (pos < end && !text[pos].isWhitespace() && text[pos] !in charArrayOf('=', '>', '/', '{', '"', '\'')) pos++
                                if (pos == start) pos++
                                if (text.subSequence(start, pos).toString() == "phx-no-curly-interpolation") disabled = true
                                emit(start, pos, HEEX_ATTRIBUTE)
                            }
                        }
                    }
                    if (closed) {
                        if (closing) {
                            val match = stack.indexOfLast { it.name.equals(name, true) }
                            if (match >= 0) while (stack.size > match) stack.removeAt(stack.lastIndex)
                        } else if (!selfClosing && name.lowercase() !in VOID_TAGS) stack.add(Tag(name, disabled))
                    }
                }
                text[pos] == '&' -> {
                    val stop = find(pos + 1, ";")
                    if (stop > pos && stop - pos <= 32 && (pos + 1 until stop).all { text[it].isLetterOrDigit() || text[it] == '#' }) {
                        emit(pos, stop + 1, HEEX_ENTITY); pos = stop + 1
                    } else { emit(pos, pos + 1, HEEX_TEXT); pos++ }
                }
                else -> {
                    val start = pos++
                    while (pos < end && text[pos] != '<' && text[pos] != '&' && (text[pos] != '{' || noCurly)) pos++
                    emit(start, pos, HEEX_TEXT)
                }
            }
        }
        return result
    }

    companion object {
        private val VOID_TAGS = setOf("area", "base", "br", "col", "embed", "hr", "img", "input", "link", "meta", "param", "source", "track", "wbr")
    }
}
