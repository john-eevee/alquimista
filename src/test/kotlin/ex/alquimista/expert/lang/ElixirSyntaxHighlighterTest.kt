package ex.alquimista.expert.lang

import com.intellij.psi.StringEscapesTokenTypes
import ex.alquimista.expert.grammar.*
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
    @Test
    fun `source tokens use their expected theme categories`() {
        val source = "@answer = %{key: :ok, other: :\"quoted\", n: 0xCA_FE, b: 0b10_01, o: 0o755, d: 1_000, f: 1.2e-3}\n%MyApp.User{} |> work() # comment\n~r/foo/i ~S|raw|"
        val lexer = highlighter.highlightingLexer
        lexer.start(source)
        val tokens = mutableMapOf<String, List<com.intellij.openapi.editor.colors.TextAttributesKey>>()
        while (lexer.tokenType != null) {
            tokens[source.substring(lexer.tokenStart, lexer.tokenEnd)] = highlighter.getTokenHighlights(lexer.tokenType!!).toList()
            lexer.advance()
        }
        val attribute = com.intellij.openapi.editor.colors.TextAttributesKey.createTextAttributesKey("ALQUIMISTA_ELIXIR_MODULE_ATTRIBUTE")
        val expected = mapOf(
            "@" to attribute, "answer" to attribute, "%{" to ElixirColors.BRACES, "%" to ElixirColors.BRACES,
            "MyApp" to ElixirColors.MODULE, "User" to ElixirColors.MODULE, "." to ElixirColors.DOT,
            "{" to ElixirColors.BRACES, "}" to ElixirColors.BRACES, "," to ElixirColors.COMMA,
            "key:" to ElixirColors.ATOM, ":ok" to ElixirColors.ATOM, ":\"quoted\"" to ElixirColors.ATOM,
            "0xCA_FE" to ElixirColors.NUMBER, "0b10_01" to ElixirColors.NUMBER, "0o755" to ElixirColors.NUMBER,
            "1_000" to ElixirColors.NUMBER, "1.2e-3" to ElixirColors.NUMBER, "|>" to ElixirColors.OPERATOR, "# comment" to ElixirColors.COMMENT,
            "~r/" to ElixirColors.STRING, "foo" to ElixirColors.STRING, "/i" to ElixirColors.STRING,
            "~S|raw|" to ElixirColors.STRING,
        )
        expected.forEach { (text, color) -> assertEquals(text, listOf(color), tokens[text]) }
    }

    @Test
    fun `embedded Elixir keeps attribute and struct colors in templates`() {
        val cases = listOf(
            Triple(EexSyntaxHighlighter(), "<%= %MyApp.User{value: @answer} %>", ElixirColors.MODULE_ATTRIBUTE),
            Triple(HeexSyntaxHighlighter(), "<div>{%MyApp.User{value: @answer}}</div>", HeexColors.MODULE_ATTRIBUTE),
        )
        for ((template, source, attribute) in cases) {
            val lexer = template.highlightingLexer
            lexer.start(source)
            val found = mutableMapOf<String, List<com.intellij.openapi.editor.colors.TextAttributesKey>>()
            while (lexer.tokenType != null) {
                found[source.substring(lexer.tokenStart, lexer.tokenEnd)] = template.getTokenHighlights(lexer.tokenType!!).toList()
                lexer.advance()
            }
            assertEquals(source, listOf(attribute), found["@"])
            assertEquals(source, listOf(attribute), found["answer"])
            val module = if (template is HeexSyntaxHighlighter) HeexColors.MODULE else ElixirColors.MODULE
            val braces = if (template is HeexSyntaxHighlighter) HeexColors.BRACES else ElixirColors.BRACES
            assertEquals(source, listOf(module), found["MyApp"])
            assertEquals(source, listOf(module), found["User"])
            assertEquals(source, listOf(braces), found["%"])

        }
    }

    @Test
    fun `attribute markers in strings and comments retain their own colors`() {
        val source = "\"@answer\" # @answer"
        val lexer = highlighter.highlightingLexer
        lexer.start(source)
        while (lexer.tokenType != null) {
            assertEquals(false, highlighter.getTokenHighlights(lexer.tokenType!!).contains(ElixirColors.MODULE_ATTRIBUTE))
            lexer.advance()
        }
    }

}
