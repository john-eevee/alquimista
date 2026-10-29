package dev.alquimista.expert.lang

import com.intellij.openapi.editor.colors.TextAttributesKey
import org.junit.Assert.*
import org.junit.Test

class EexSyntaxHighlighterTest {
    private val highlighter = EexSyntaxHighlighter()
    private data class Part(val start: Int, val end: Int, val colors: List<TextAttributesKey>)

    private fun tokens(source: String, start: Int = 0): List<Part> {
        val lexer = highlighter.highlightingLexer
        lexer.start(source, start, source.length, 0)
        val parts = mutableListOf<Part>()
        var expected = start
        while (lexer.tokenType != null) {
            assertEquals(expected, lexer.tokenStart)
            assertTrue(lexer.tokenEnd > lexer.tokenStart)
            parts += Part(lexer.tokenStart, lexer.tokenEnd, highlighter.getTokenHighlights(lexer.tokenType!!).toList())
            expected = lexer.tokenEnd
            lexer.advance()
        }
        assertEquals(source.length, expected)
        return parts
    }

    @Test fun `highlights Elixir in tags while preserving surrounding text`() {
        val source = "Hello <%= Enum.map(@items, fn item -> item end) %>! <% IO.puts(1) %>"
        val parts = tokens(source)
        fun has(text: String, color: TextAttributesKey) = parts.any { source.substring(it.start, it.end) == text && color in it.colors }
        assertTrue(has("<%=", EexColors.DELIMITER))
        assertTrue(has("%>", EexColors.DELIMITER))
        assertTrue(has("Enum", ElixirColors.MODULE))
        assertTrue(has("map", ElixirColors.FUNCTION_CALL))
        assertTrue(has("fn", ElixirColors.KEYWORD))
        assertTrue(parts.any { source.substring(it.start, it.end).contains("Hello ") && it.colors.isEmpty() })
    }

    @Test fun `comments and escaped quotation stay out of embedded Elixir`() {
        val source = "<%!-- comment <%= x %> --%><%# old %><%%= Enum.map(1) %><%= :ok %>"
        val parts = tokens(source)
        fun has(text: String, color: TextAttributesKey) = parts.any { source.substring(it.start, it.end) == text && color in it.colors }
        assertTrue(has("<%!-- comment <%= x %> --%>", ElixirColors.COMMENT))
        assertTrue(has("<%# old %>", ElixirColors.COMMENT))
        assertFalse(has("Enum", ElixirColors.MODULE))
        assertTrue(has("<%=", EexColors.DELIMITER))
    }

    @Test fun `escaped opener does not hide a later real tag`() {
        val source = "<%% literal <%= Enum.join(@values) %> tail %>"
        val parts = tokens(source)
        assertTrue(parts.any { source.substring(it.start, it.end) == "Enum" && ElixirColors.MODULE in it.colors })
    }

    @Test fun `unfinished tags and custom engine markers retain all text`() {
        val source = "<%| foo %><%/ bar %><%= Enum.map("
        val parts = tokens(source)
        assertTrue(parts.any { source.substring(it.start, it.end) == "<%|" && EexColors.DELIMITER in it.colors })
        assertTrue(parts.any { source.substring(it.start, it.end) == "<%/" && EexColors.DELIMITER in it.colors })
        assertTrue(parts.any { source.substring(it.start, it.end) == "Enum" && ElixirColors.MODULE in it.colors })
    }

    @Test fun `restart at every offset preserves the original token colors`() {
        val source = "x<%!-- hi --%><%%= literal %><%= Enum.map(@x, fn y -> y end) %>z"
        val full = tokens(source)
        for (offset in source.indices) {
            val expected = full.first { offset >= it.start && offset < it.end }
            assertEquals("offset $offset", expected.colors, tokens(source, offset).first().colors)
        }
    }
}
