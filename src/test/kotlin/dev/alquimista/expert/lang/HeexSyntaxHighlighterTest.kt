package dev.alquimista.expert.lang

import com.intellij.openapi.editor.colors.TextAttributesKey
import org.junit.Assert.*
import org.junit.Test

class HeexSyntaxHighlighterTest {
    private val highlighter = HeexSyntaxHighlighter()

    private fun tokens(source: String, start: Int = 0): List<Pair<String, List<TextAttributesKey>>> {
        val lexer = highlighter.highlightingLexer
        lexer.start(source, start, source.length, 0)
        val found = mutableListOf<Pair<String, List<TextAttributesKey>>>()
        while (lexer.tokenType != null) {
            found += source.substring(lexer.tokenStart, lexer.tokenEnd) to highlighter.getTokenHighlights(lexer.tokenType!!).toList()
            lexer.advance()
        }
        assertEquals(source.substring(start), found.joinToString("") { it.first })
        return found
    }

    @Test fun `highlights markup components and embedded Elixir independently`() {
        val source = """<.card :if={@visible} title="Hi">
  Hello, {@user.name}! <%= Enum.map(@items, fn x -> x end) %>
</.card>"""
        val found = tokens(source)
        fun color(text: String, key: TextAttributesKey) = assertTrue("Missing $text with $key: $found", found.any { it.first == text && key in it.second })
        color(".card", HeexColors.TAG_NAME)
        color(":if", HeexColors.ATTRIBUTE)
        color("\"Hi\"", HeexColors.STRING)
        color("{", HeexColors.EXPRESSION_DELIMITER)
        color("<%=", HeexColors.EXPRESSION_DELIMITER)
        color("%>", HeexColors.EXPRESSION_DELIMITER)
        color("Enum", HeexColors.MODULE)
        color("map", HeexColors.FUNCTION_CALL)
        color("fn", HeexColors.KEYWORD)
    }

    @Test fun `handles nested braces strings and both comment forms`() {
        val source = """<%!-- {not code} --%><!-- <.not_a_tag> -->
<.item data={%{name: "}"}} />"""
        val found = tokens(source)
        assertTrue(found.any { it.first == "<%!-- {not code} --%>" && HeexColors.COMMENT in it.second })
        assertTrue(found.any { it.first == "<!-- <.not_a_tag> -->" && HeexColors.COMMENT in it.second })
        assertTrue(found.any { it.first == "}" && HeexColors.EXPRESSION_DELIMITER in it.second })
        assertTrue(found.any { it.first.contains("}") && HeexColors.STRING in it.second })
    }

    @Test fun `does not parse curly braces in script style or opt out blocks`() {
        val source = "<script>const x = {a: 1}; <%= @x %></script><style>.x {color: red}</style><div phx-no-curly-interpolation><p>{literal}</p></div>"
        val found = tokens(source)
        assertTrue(found.any { it.first.contains("{a: 1}") && HeexColors.EXPRESSION_DELIMITER !in it.second })
        assertTrue(found.any { it.first.contains("{color: red}") && HeexColors.EXPRESSION_DELIMITER !in it.second })
        assertTrue(found.any { it.first.contains("{literal}") && HeexColors.EXPRESSION_DELIMITER !in it.second })
        assertTrue(found.any { it.first == "<%=" && HeexColors.EXPRESSION_DELIMITER in it.second })
    }

    @Test fun `keeps script comparisons and resumes interpolation after raw content`() {
        val source = "<script>if(a<b){const x={};}</script><p>{@name}</p>"
        val found = tokens(source)
        assertTrue(found.any { it.first == "{" && HeexColors.EXPRESSION_DELIMITER in it.second })
        assertEquals(2, found.count { it.first == "script" && HeexColors.TAG_NAME in it.second })
    }

    @Test fun `restarts correctly inside tags and embedded expressions`() {
        val source = "<.card title=\"hello\">{@name}</.card>"
        for (offset in listOf(3, source.indexOf("hello") + 2, source.indexOf("@name") + 2)) {
            val full = tokens(source).first { source.indexOf(it.first) <= offset && source.indexOf(it.first) + it.first.length > offset }
            assertEquals(full.second, tokens(source, offset).first().second)
        }
    }
}
