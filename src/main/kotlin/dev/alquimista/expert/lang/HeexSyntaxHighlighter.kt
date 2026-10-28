package dev.alquimista.expert.lang

import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.psi.tree.IElementType

class HeexSyntaxHighlighter : SyntaxHighlighterBase() {
    private val elixir = ElixirSyntaxHighlighter()
    override fun getHighlightingLexer() = HeexLexer()

    override fun getTokenHighlights(tokenType: IElementType): Array<TextAttributesKey> {
        val key = when (tokenType) {
            HEEX_TAG_DELIMITER -> HeexColors.TAG_DELIMITER
            HEEX_TAG_NAME -> HeexColors.TAG_NAME
            HEEX_ATTRIBUTE -> HeexColors.ATTRIBUTE
            HEEX_STRING -> HeexColors.STRING
            HEEX_COMMENT -> HeexColors.COMMENT
            HEEX_ENTITY -> HeexColors.ENTITY
            HEEX_EXPRESSION_DELIMITER -> HeexColors.EXPRESSION_DELIMITER
            else -> return elixir.getTokenHighlights(tokenType).map { ELIXIR_COLORS[it] ?: it }.toTypedArray()
        }
        return arrayOf(key)
    }

    companion object {
        private val ELIXIR_COLORS = mapOf(
            ElixirColors.KEYWORD to HeexColors.KEYWORD,
            ElixirColors.MODULE to HeexColors.MODULE,
            ElixirColors.FUNCTION_DECLARATION to HeexColors.FUNCTION_DECLARATION,
            ElixirColors.FUNCTION_CALL to HeexColors.FUNCTION_CALL,
            ElixirColors.STRING to HeexColors.STRING,
            ElixirColors.COMMENT to HeexColors.COMMENT,
            ElixirColors.NUMBER to HeexColors.NUMBER,
            ElixirColors.ATOM to HeexColors.ATOM,
            ElixirColors.OPERATOR to HeexColors.OPERATOR,
            ElixirColors.PARENTHESES to HeexColors.PARENTHESES,
            ElixirColors.BRACKETS to HeexColors.BRACKETS,
            ElixirColors.BRACES to HeexColors.BRACES,
            ElixirColors.COMMA to HeexColors.COMMA,
            ElixirColors.DOT to HeexColors.DOT,
            ElixirColors.SEMICOLON to HeexColors.SEMICOLON,
            ElixirColors.VALID_ESCAPE to HeexColors.VALID_ESCAPE,
            ElixirColors.INVALID_ESCAPE to HeexColors.INVALID_ESCAPE,
        )
    }
}
