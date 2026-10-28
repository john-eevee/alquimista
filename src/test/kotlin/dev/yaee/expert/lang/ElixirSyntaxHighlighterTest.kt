package dev.yaee.expert.lang

import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.psi.StringEscapesTokenTypes
import dev.murek.elixirij.lang.*
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class ElixirSyntaxHighlighterTest {
    private val highlighter = ElixirSyntaxHighlighter()

    @Test
    fun `tokens use theme language defaults`() {
        val cases = mapOf(
            EX_DO to DefaultLanguageHighlighterColors.KEYWORD,
            EX_COMMENT to DefaultLanguageHighlighterColors.LINE_COMMENT,
            EX_INTEGER to DefaultLanguageHighlighterColors.NUMBER,
            EX_STRING_PART to DefaultLanguageHighlighterColors.STRING,
            EX_ATOM to DefaultLanguageHighlighterColors.CONSTANT,
            EX_PIPE_GT to DefaultLanguageHighlighterColors.OPERATION_SIGN,
            EX_LPAREN to DefaultLanguageHighlighterColors.PARENTHESES,
            StringEscapesTokenTypes.VALID_STRING_ESCAPE_TOKEN to DefaultLanguageHighlighterColors.VALID_STRING_ESCAPE,
        )
        cases.forEach { (token, key) -> assertArrayEquals(arrayOf(key), highlighter.getTokenHighlights(token)) }
    }

    @Test
    fun `ordinary identifiers do not receive keyword colors`() {
        assertArrayEquals(emptyArray(), highlighter.getTokenHighlights(EX_IDENTIFIER))
        assertArrayEquals(emptyArray(), highlighter.getTokenHighlights(EX_RAISE))
        assertArrayEquals(emptyArray(), highlighter.getTokenHighlights(EX_SEND))
        assertArrayEquals(emptyArray(), highlighter.getTokenHighlights(EX_THROW))
    }

    @Test
    fun `highlights Elixir declarations and control macros as theme keywords`() {
        val words = listOf("def", "defp", "defmodule", "defmacro", "defcustom", "case", "with", "if", "unless", "cond", "for", "receive", "try", "quote", "use", "alias", "import", "require")
        val source = words.joinToString("\n") { "$it sample" }
        val lexer = highlighter.highlightingLexer
        lexer.start(source)
        val highlighted = mutableListOf<String>()
        while (lexer.tokenType != null) {
            if (highlighter.getTokenHighlights(lexer.tokenType!!).contains(DefaultLanguageHighlighterColors.KEYWORD)) {
                highlighted += source.substring(lexer.tokenStart, lexer.tokenEnd)
            }
            lexer.advance()
        }
        assertEquals(words, highlighted)
    }

    @Test
    fun `highlights custom declaration forms`() {
        val source = "defcustom example, do: :ok\n  defp hidden(), do: :ok\n  defmodule Example do"
        val lexer = highlighter.highlightingLexer
        lexer.start(source)
        val found = mutableListOf<String>()
        while (lexer.tokenType != null) {
            if (highlighter.getTokenHighlights(lexer.tokenType!!).contains(DefaultLanguageHighlighterColors.KEYWORD)) {
                val word = source.substring(lexer.tokenStart, lexer.tokenEnd)
                if (word.startsWith("def")) found.add(word)
            }
            lexer.advance()
        }
        assertEquals(listOf("defcustom", "defp", "defmodule"), found)
    }

    @Test
    fun `does not color keyword list keys as control constructs`() {
        val lexer = highlighter.highlightingLexer
        lexer.start("case: value, with: value, defmodule: value")
        while (lexer.tokenType != null) {
            assertEquals(false, highlighter.getTokenHighlights(lexer.tokenType!!).contains(DefaultLanguageHighlighterColors.KEYWORD))
            lexer.advance()
        }
    }

    @Test
    fun `does not highlight def prefixed variables and ordinary calls`() {
        val source = "if default, do: :ok\ndefault = 1\nvalue = default\ndefault(value)\nMod.defcustom(value)"
        val lexer = highlighter.highlightingLexer
        lexer.start(source)
        while (lexer.tokenType != null) {
            val tokenText = source.substring(lexer.tokenStart, lexer.tokenEnd)
            if (tokenText == "default" || tokenText == "defcustom") {
                assertEquals(tokenText, emptyList<Any>(), highlighter.getTokenHighlights(lexer.tokenType!!).toList())
            }
            lexer.advance()
        }
    }
}
