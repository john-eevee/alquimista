package ex.alquimista.expert.lang

import com.intellij.lexer.LexerBase
import com.intellij.psi.tree.IElementType

internal val EEX_TEXT = IElementType("EEX_TEXT", EexLanguage.INSTANCE)
internal val EEX_DELIMITER = IElementType("EEX_DELIMITER", EexLanguage.INSTANCE)
internal val EEX_COMMENT = IElementType("EEX_COMMENT", EexLanguage.INSTANCE)

/** Reconstructs template context when IntelliJ restarts the lexer inside a token. */
class EexLexer : LexerBase() {
    private data class Part(val start: Int, val end: Int, val type: IElementType)

    private var buffer: CharSequence = ""
    private var endOffset = 0
    private var startOffset = 0
    private var parts: List<Part> = emptyList()
    private var index = 0

    override fun start(buffer: CharSequence, startOffset: Int, endOffset: Int, initialState: Int) {
        this.buffer = buffer
        this.startOffset = startOffset
        this.endOffset = endOffset
        parts = scan(buffer, endOffset)
        index = parts.indexOfFirst { it.end > startOffset }.let { if (it < 0) parts.size else it }
    }

    override fun getState(): Int = 0
    override fun getTokenType(): IElementType? = parts.getOrNull(index)?.type
    override fun getTokenStart(): Int = parts.getOrNull(index)?.start?.coerceAtLeast(startOffset) ?: endOffset
    override fun getTokenEnd(): Int = parts.getOrNull(index)?.end ?: endOffset
    override fun advance() {
        if (index < parts.size) index++
    }

    override fun getBufferSequence(): CharSequence = buffer
    override fun getBufferEnd(): Int = endOffset

    private fun scan(text: CharSequence, end: Int): List<Part> {
        val parts = mutableListOf<Part>()
        fun emit(start: Int, stop: Int, type: IElementType) {
            if (stop > start) parts += Part(start, stop, type)
        }

        fun starts(pos: Int, value: String): Boolean = pos + value.length <= end &&
                value.indices.all { text[pos + it] == value[it] }

        fun find(pos: Int, value: String): Int {
            val index = text.indexOf(value, pos)
            return if (index >= 0 && index + value.length <= end) index else -1
        }

        fun endOf(start: Int, marker: String): Int = find(start, marker).let { if (it < 0) end else it + marker.length }
        var pos = 0
        while (pos < end) {
            when {
                starts(pos, "<%%") -> {
                    emit(pos, pos + 3, EEX_TEXT)
                    pos += 3
                }

                starts(pos, "<%!--") -> {
                    val stop = endOf(pos + 5, "--%>")
                    emit(pos, stop, EEX_COMMENT)
                    pos = stop
                }

                starts(pos, "<%#") -> {
                    val stop = endOf(pos + 3, "%>")
                    emit(pos, stop, EEX_COMMENT)
                    pos = stop
                }

                starts(pos, "<%") -> {
                    val openerEnd = pos + if (pos + 2 < end && text[pos + 2] in charArrayOf('=', '|', '/')) 3 else 2
                    emit(pos, openerEnd, EEX_DELIMITER)
                    val close = find(openerEnd, "%>").let { if (it < 0) end else it }
                    val elixir = ElixirSyntaxHighlighter().highlightingLexer
                    elixir.start(text, openerEnd, close, 0)
                    while (elixir.tokenType != null) {
                        emit(elixir.tokenStart, elixir.tokenEnd, elixir.tokenType!!)
                        elixir.advance()
                    }
                    pos = close
                    if (pos < end) {
                        emit(pos, pos + 2, EEX_DELIMITER)
                        pos += 2
                    }
                }

                else -> {
                    val next = find(pos + 1, "<%")
                    val stop = if (next < 0) end else next
                    emit(pos, stop, EEX_TEXT)
                    pos = stop
                }
            }
        }
        return parts
    }
}
