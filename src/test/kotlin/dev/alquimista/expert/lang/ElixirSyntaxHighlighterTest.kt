package dev.alquimista.expert.lang

import com.intellij.psi.StringEscapesTokenTypes
import dev.alquimista.expert.grammar.*
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class ElixirSyntaxHighlighterTest {
    private val highlighter = ElixirSyntaxHighlighter()

    @Test
    fun `tokens use theme language defaults`() {
        val cases = mapOf(
            EX_DO to ElixirColors.KEYWORD,
            EX_COMMENT to ElixirColors.COMMENT,
            EX_INTEGER to ElixirColors.NUMBER,
            EX_STRING_PART to ElixirColors.STRING,
            EX_ATOM to ElixirColors.ATOM,
            EX_PIPE_GT to ElixirColors.OPERATOR,
            EX_LPAREN to ElixirColors.PARENTHESES,
            StringEscapesTokenTypes.VALID_STRING_ESCAPE_TOKEN to ElixirColors.VALID_ESCAPE,
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
        val source = words.joinToString("\n") { if (it == "defcustom") "$it sample, do: :ok" else "$it sample" }
        val lexer = highlighter.highlightingLexer
        lexer.start(source)
        val highlighted = mutableListOf<String>()
        while (lexer.tokenType != null) {
            if (highlighter.getTokenHighlights(lexer.tokenType!!).contains(ElixirColors.KEYWORD)) {
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
            if (highlighter.getTokenHighlights(lexer.tokenType!!).contains(ElixirColors.KEYWORD)) {
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
            assertEquals(false, highlighter.getTokenHighlights(lexer.tokenType!!).contains(ElixirColors.KEYWORD))
            lexer.advance()
        }
    }

    @Test
    fun `does not highlight def prefixed variables and ordinary calls`() {
        val source = "if default, do: :ok\ndefault = 1\nvalue = default\ndefault(value)\ndefault value\nMod.defcustom(value)"
        val lexer = highlighter.highlightingLexer
        lexer.start(source)
        while (lexer.tokenType != null) {
            val tokenText = source.substring(lexer.tokenStart, lexer.tokenEnd)
            if (tokenText == "default" || tokenText == "defcustom") {
                assertEquals(tokenText, false, highlighter.getTokenHighlights(lexer.tokenType!!).contains(ElixirColors.KEYWORD))
            }
            lexer.advance()
        }
    }

    @Test
    fun `highlights declarations after inline statement boundaries`() {
        val source = "def first, do: :ok; defp second, do: :ok\nif enabled, do: defmodule Inline do\nend"
        val lexer = highlighter.highlightingLexer
        lexer.start(source)
        val found = mutableListOf<String>()
        while (lexer.tokenType != null) {
            val text = source.substring(lexer.tokenStart, lexer.tokenEnd)
            if (text.startsWith("def") && highlighter.getTokenHighlights(lexer.tokenType!!).contains(ElixirColors.KEYWORD)) found += text
            lexer.advance()
        }
        assertEquals(listOf("def", "defp", "defmodule"), found)
    }

    @Test
    fun `highlights parenthesized custom declarations with do blocks`() {
        val source = "defcustom(:example, do: :ok)\ndefcustom other do\n  :ok\nend\ndefcustom(\n  :third,\n  do: :ok\n)"
        val lexer = highlighter.highlightingLexer
        lexer.start(source)
        var count = 0
        while (lexer.tokenType != null) {
            if (source.substring(lexer.tokenStart, lexer.tokenEnd) == "defcustom" &&
                highlighter.getTokenHighlights(lexer.tokenType!!).contains(ElixirColors.KEYWORD)) count++
            lexer.advance()
        }
        assertEquals(3, count)
    }

    @Test
    fun `does not treat do in strings or comments as declaration syntax`() {
        val source = "default value, label: \"do\"\ndefault value # do\n"
        val lexer = highlighter.highlightingLexer
        lexer.start(source)
        while (lexer.tokenType != null) {
            if (source.substring(lexer.tokenStart, lexer.tokenEnd) == "default") {
                assertEquals(false, highlighter.getTokenHighlights(lexer.tokenType!!).contains(ElixirColors.KEYWORD))
            }
            lexer.advance()
        }
    }

    @Test
    fun `module aliases use the theme class reference color`() {
        assertArrayEquals(arrayOf(ElixirColors.MODULE), highlighter.getTokenHighlights(EX_ALIAS))
    }

    @Test
    fun `function declarations and calls use theme colors while variables stay uncolored`() {
        val source = """defmodule MyApp.Worker do
  def run(value), do: transform(value)
  defp transform value, do: Enum.map(value, &normalize/1)
  value = run
  output = transform value
  record.name
end"""
        val lexer = highlighter.highlightingLexer
        lexer.start(source)
        val found = mutableListOf<Pair<String, List<Any>>>()
        while (lexer.tokenType != null) {
            val word = source.substring(lexer.tokenStart, lexer.tokenEnd)
            if (word in setOf("run", "transform", "map", "normalize", "value", "name")) {
                found += word to highlighter.getTokenHighlights(lexer.tokenType!!).toList()
            }
            lexer.advance()
        }
        val declaration = listOf(ElixirColors.FUNCTION_DECLARATION)
        val call = listOf(ElixirColors.FUNCTION_CALL)
        assertEquals(listOf(
            "run" to declaration, "value" to emptyList(), "transform" to call, "value" to emptyList(),
            "transform" to declaration, "value" to emptyList(), "map" to call, "value" to emptyList(),
            "normalize" to call, "value" to emptyList(), "run" to emptyList(),
            "transform" to call, "value" to emptyList(), "name" to emptyList()
        ), found)
    }
}
