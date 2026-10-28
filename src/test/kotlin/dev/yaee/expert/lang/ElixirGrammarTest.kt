package dev.yaee.expert.lang

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ElixirGrammarTest {
    @Test
    fun `lexer separates calls, punctuation and comments`() {
        val source = "def hello(name), do: name # greeting\n"
        val lexer = ElixirParserDefinition().createLexer(null)
        lexer.start(source)
        val tokens = mutableListOf<String>()
        while (lexer.tokenType != null) {
            tokens.add(source.substring(lexer.tokenStart, lexer.tokenEnd))
            lexer.advance()
        }
        assertEquals(source, tokens.joinToString(""))
        assertTrue(tokens.contains("hello"))
        assertTrue(tokens.contains("("))
        assertTrue(tokens.contains("# greeting"))
    }
}
