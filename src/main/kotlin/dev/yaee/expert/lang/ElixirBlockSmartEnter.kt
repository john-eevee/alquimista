package dev.yaee.expert.lang

import com.intellij.psi.TokenType
import dev.murek.elixirij.lang.EX_COMMENT
import dev.murek.elixirij.lang.EX_DO
import dev.murek.elixirij.lang.EX_EOL
import dev.murek.elixirij.lang.lexer.ExLexer

internal data class BlockCompletion(val offset: Int, val text: String, val caretOffset: Int)

internal object ElixirBlockSmartEnter {
    fun find(text: CharSequence, caret: Int): BlockCompletion? {
        val lineStart = text.lastIndexOf('\n', caret - 1).let { if (it < 0) 0 else it + 1 }
        val lineEnd = text.indexOf('\n', caret).let { if (it < 0) text.length else it }
        val line = text.subSequence(lineStart, lineEnd).toString()
        val indentation = line.takeWhile { it == ' ' || it == '\t' }
        val lexer = ExLexer()
        lexer.start(text)
        var lastToken: com.intellij.psi.tree.IElementType? = null
        while (lexer.tokenType != null && lexer.tokenStart < lineEnd) {
            if (lexer.tokenStart >= lineStart) {
                val token = lexer.tokenType
                if (token == EX_COMMENT && caret > lexer.tokenStart) return null
                if (token != TokenType.WHITE_SPACE && token != EX_EOL && token != EX_COMMENT) lastToken = token
            }
            lexer.advance()
        }
        if (lastToken != EX_DO) return null

        val nextLine = text.subSequence(lineEnd, text.length).lineSequence().drop(1)
            .firstOrNull { it.isNotBlank() && !it.trimStart().startsWith("#") }
        if (nextLine != null) {
            val trimmed = nextLine.trimStart()
            val nextIndent = nextLine.length - trimmed.length
            if (!Regex("^end(?:\\s|#|$)").containsMatchIn(trimmed) || nextIndent >= indentation.length) return null
        }
        val bodyIndent = indentation + "  "
        val insertion = "\n$bodyIndent\n${indentation}end"
        return BlockCompletion(lineEnd, insertion, 1 + bodyIndent.length)
    }
}
