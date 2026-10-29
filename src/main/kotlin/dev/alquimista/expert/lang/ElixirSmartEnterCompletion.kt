package dev.alquimista.expert.lang

import com.intellij.psi.tree.IElementType
import dev.alquimista.expert.grammar.*
import dev.alquimista.expert.grammar.lexer.ExLexer

internal object ElixirSmartEnterCompletion {
    private data class Opening(val offset: Int, val close: IElementType, val text: String)

    fun find(text: CharSequence, caret: Int): Pair<Int, String>? {
        val lineStart = text.lastIndexOf('\n', (caret - 1).coerceAtLeast(0)).let { if (it < 0) 0 else it + 1 }
        val lineEnd = text.indexOf('\n', caret).let { if (it < 0) text.length else it }
        var insertion = lineEnd
        val openings = mutableListOf<Opening>()
        val lexer = ExLexer()
        lexer.start(text)
        while (lexer.tokenType != null) {
            val type = lexer.tokenType
            val start = lexer.tokenStart
            if (start < caret && caret <= lexer.tokenEnd &&
                (type == EX_COMMENT || type == EX_STRING_PART || type == EX_CHARLIST_PART)
            ) return null
            if (type == EX_COMMENT && start in lineStart until lineEnd) insertion = minOf(insertion, start)
            val opening = when (type) {
                EX_LPAREN -> Opening(start, EX_RPAREN, ")")
                EX_LBRACKET -> Opening(start, EX_RBRACKET, "]")
                EX_LBRACE, EX_PERCENT_LBRACE -> Opening(start, EX_RBRACE, "}")
                EX_INTERPOLATION_START -> Opening(start, EX_INTERPOLATION_END, "}")
                else -> null
            }
            if (opening != null) openings.add(opening)
            else if (type == EX_RPAREN || type == EX_RBRACKET || type == EX_RBRACE || type == EX_INTERPOLATION_END) {
                if (openings.lastOrNull()?.close == type) openings.removeAt(openings.lastIndex)
            }
            lexer.advance()
        }
        while (insertion > lineStart && text[insertion - 1].isWhitespace()) insertion--
        val missing =
            openings.filter { it.offset in lineStart until insertion }.asReversed().joinToString("") { it.text }
        return if (missing.isEmpty()) null else insertion to missing
    }
}
