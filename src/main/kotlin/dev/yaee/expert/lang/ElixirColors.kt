package dev.yaee.expert.lang

import com.intellij.openapi.editor.DefaultLanguageHighlighterColors as Defaults
import com.intellij.openapi.editor.HighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey

object ElixirColors {
    private fun key(name: String, fallback: TextAttributesKey): TextAttributesKey =
        TextAttributesKey.createTextAttributesKey("YAEE_ELIXIR_$name", fallback)

    val KEYWORD = key("KEYWORD", Defaults.KEYWORD)
    val MODULE = key("MODULE", Defaults.CLASS_REFERENCE)
    val FUNCTION_DECLARATION = key("FUNCTION_DECLARATION", Defaults.FUNCTION_DECLARATION)
    val FUNCTION_CALL = key("FUNCTION_CALL", Defaults.FUNCTION_CALL)
    val COMMENT = key("COMMENT", Defaults.LINE_COMMENT)
    val STRING = key("STRING", Defaults.STRING)
    val NUMBER = key("NUMBER", Defaults.NUMBER)
    val ATOM = key("ATOM", Defaults.CONSTANT)
    val OPERATOR = key("OPERATOR", Defaults.OPERATION_SIGN)
    val PARENTHESES = key("PARENTHESES", Defaults.PARENTHESES)
    val BRACKETS = key("BRACKETS", Defaults.BRACKETS)
    val BRACES = key("BRACES", Defaults.BRACES)
    val COMMA = key("COMMA", Defaults.COMMA)
    val DOT = key("DOT", Defaults.DOT)
    val SEMICOLON = key("SEMICOLON", Defaults.SEMICOLON)
    val VALID_ESCAPE = key("VALID_ESCAPE", Defaults.VALID_STRING_ESCAPE)
    val INVALID_ESCAPE = key("INVALID_ESCAPE", Defaults.INVALID_STRING_ESCAPE)
    val BAD_CHARACTER = key("BAD_CHARACTER", HighlighterColors.BAD_CHARACTER)
}
