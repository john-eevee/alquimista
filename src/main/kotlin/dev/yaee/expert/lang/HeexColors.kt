package dev.yaee.expert.lang

import com.intellij.openapi.editor.DefaultLanguageHighlighterColors as Defaults
import com.intellij.openapi.editor.colors.TextAttributesKey

object HeexColors {
    private fun key(name: String, fallback: TextAttributesKey): TextAttributesKey =
        TextAttributesKey.createTextAttributesKey("YAEE_HEEX_$name", fallback)

    val TAG_DELIMITER = key("TAG_DELIMITER", Defaults.MARKUP_TAG)
    val TAG_NAME = key("TAG_NAME", Defaults.MARKUP_TAG)
    val ATTRIBUTE = key("ATTRIBUTE", Defaults.MARKUP_ATTRIBUTE)
    val STRING = key("STRING", Defaults.STRING)
    val COMMENT = key("COMMENT", Defaults.BLOCK_COMMENT)
    val ENTITY = key("ENTITY", Defaults.MARKUP_ENTITY)
    val EXPRESSION_DELIMITER = key("EXPRESSION_DELIMITER", Defaults.TEMPLATE_LANGUAGE_COLOR)
    val KEYWORD = key("KEYWORD", Defaults.KEYWORD)
    val MODULE = key("MODULE", Defaults.CLASS_REFERENCE)
    val FUNCTION_DECLARATION = key("FUNCTION_DECLARATION", Defaults.FUNCTION_DECLARATION)
    val FUNCTION_CALL = key("FUNCTION_CALL", Defaults.FUNCTION_CALL)
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
}
