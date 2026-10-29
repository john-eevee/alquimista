package ex.alquimista.expert.lang

import ex.alquimista.expert.grammar.*
import com.intellij.psi.TokenType
import org.junit.Assert.*
import org.junit.Test
import javax.xml.parsers.DocumentBuilderFactory

class ElixirEditorSupportTest {
    @Test fun `line comments use hash and no block comment syntax`() {
        val commenter = ElixirCommenter()
        assertEquals("#", commenter.lineCommentPrefix)
        assertNull(commenter.blockCommentPrefix)
        assertNull(commenter.blockCommentSuffix)
    }

    @Test fun `matches Elixir delimiters`() {
        val pairs = ElixirBraceMatcher().pairs.associate { it.leftBraceType to it.rightBraceType }
        assertEquals(EX_RPAREN, pairs[EX_LPAREN])
        assertEquals(EX_RBRACKET, pairs[EX_LBRACKET])
        assertEquals(EX_RBRACE, pairs[EX_LBRACE])
        assertEquals(EX_RBRACE, pairs[EX_PERCENT_LBRACE])
        assertEquals(EX_GT_GT, pairs[EX_LT_LT])
        assertEquals(EX_INTERPOLATION_END, pairs[EX_INTERPOLATION_START])
    }

    @Test fun `only auto-pairs before safe token types`() {
        val matcher = ElixirBraceMatcher()
        assertTrue(matcher.isPairedBracesAllowedBeforeType(EX_LPAREN, null))
        assertTrue(matcher.isPairedBracesAllowedBeforeType(EX_LPAREN, TokenType.WHITE_SPACE))
        assertTrue(matcher.isPairedBracesAllowedBeforeType(EX_LPAREN, EX_RPAREN))
        assertFalse(matcher.isPairedBracesAllowedBeforeType(EX_LPAREN, EX_IDENTIFIER))
        assertFalse(matcher.isPairedBracesAllowedBeforeType(EX_LPAREN, EX_INTEGER))
    }

    @Test fun `editor extensions are registered for Elixir`() {
        val document = javaClass.getResourceAsStream("/META-INF/plugin.xml")!!.use {
            DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(it)
        }
        for (name in listOf("lang.commenter", "lang.braceMatcher", "lang.foldingBuilder")) {
            val elements = document.getElementsByTagName(name)
            assertEquals(name, 1, (0 until elements.length).count {
                elements.item(it).attributes.getNamedItem("language")?.nodeValue == "Elixir"
            })
        }
        assertEquals(2, document.getElementsByTagName("lineIndentProvider").length)
        assertTrue(ElixirLineIndentProvider().isSuitableFor(ElixirLanguage.INSTANCE))
        assertFalse(ElixirLineIndentProvider().isSuitableFor(null))
    }
}
