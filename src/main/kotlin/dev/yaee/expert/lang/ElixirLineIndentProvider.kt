package dev.yaee.expert.lang

import com.intellij.lang.Language
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.psi.TokenType
import com.intellij.psi.codeStyle.lineIndent.LineIndentProvider
import com.intellij.psi.tree.IElementType
import dev.yaee.expert.grammar.*
import dev.yaee.expert.grammar.lexer.ExLexer

class ElixirLineIndentProvider : LineIndentProvider, DumbAware {
    override fun isSuitableFor(language: Language?): Boolean = language?.isKindOf(ElixirLanguage.INSTANCE) == true
    override fun getLineIndent(project: Project, editor: Editor, language: Language?, offset: Int): String? =
        ElixirIndent.forNewLine(editor.document.charsSequence, offset)
}

internal object ElixirIndent {
    fun forNewLine(text: CharSequence, offset: Int): String? {
        if (offset <= 0 || offset > text.length) return null
        val newline = text.lastIndexOf('\n', offset - 1)
        if (newline < 0) return null
        if (text.subSequence(newline + 1, offset).any { !it.isWhitespace() }) return null
        val start = text.lastIndexOf('\n', newline - 1).let { if (it < 0) 0 else it + 1 }
        val indentation = text.subSequence(start, newline).takeWhile { it == ' ' || it == '\t' }.toString()
        val lexer = ExLexer()
        lexer.start(text)
        val delimiters = mutableListOf<IElementType>()
        var last: IElementType? = null
        while (lexer.tokenType != null && lexer.tokenStart < newline) {
            if (lexer.tokenStart >= start) {
                val token = lexer.tokenType
                if (token != TokenType.WHITE_SPACE && token != EX_COMMENT && token != EX_EOL) {
                    last = token
                    when (token) {
                        EX_LPAREN -> delimiters.add(EX_RPAREN)
                        EX_LBRACKET -> delimiters.add(EX_RBRACKET)
                        EX_LBRACE, EX_PERCENT_LBRACE -> delimiters.add(EX_RBRACE)
                        EX_LT_LT -> delimiters.add(EX_GT_GT)
                        else -> if (delimiters.lastOrNull() == token) delimiters.removeAt(delimiters.lastIndex)
                    }
                }
            }
            lexer.advance()
        }
        val addsLevel = delimiters.isNotEmpty() || last in setOf(EX_DO, EX_FN, EX_ARROW, EX_ELSE, EX_AFTER, EX_RESCUE, EX_CATCH)
        return indentation + if (addsLevel) "  " else ""
    }
}
