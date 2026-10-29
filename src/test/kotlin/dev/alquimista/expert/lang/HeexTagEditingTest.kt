package dev.alquimista.expert.lang

import org.junit.Assert.*
import org.junit.Test

class HeexTagEditingTest {
    @Test fun `closes ordinary tags and components`() {
        assertEquals("</div>", HeexTagEditing.closingTag("<div>", 5))
        assertEquals("</div>", HeexTagEditing.closingTag(StringBuilder("<div>"), 5))
        assertEquals("</.card>", HeexTagEditing.closingTag("<.card :if={@visible}>", 22))
        assertEquals("</:item>", HeexTagEditing.closingTag("<:item>", 7))
    }

    @Test fun `does not close void self closing closing or already matched tags`() {
        for (text in listOf("<br>", "<input disabled>", "<.card />", "</div>", "<div></div>", "<!-- <div> -->", "<%= \"<div>\" %>")) {
            val offset = text.indexOf('>') + 1
            assertNull(text, HeexTagEditing.closingTag(text, offset))
        }
    }

    @Test fun `does not steal the existing parent closing tag`() {
        val source = "<div><div></div>"
        assertEquals("</div>", HeexTagEditing.closingTag(source, "<div><div>".length))
        val closed = "<div><div></div></div>"
        assertNull(HeexTagEditing.closingTag(closed, "<div><div>".length))
    }

    @Test fun `indents nested markup and Elixir blocks without treating literals as tags`() {
        fun indent(text: String) = HeexIndent.forNewLine(text, text.length)
        assertEquals("  ", indent("<.card>\n"))
        assertEquals("    ", indent("  <div>hello\n"))
        assertEquals("  ", indent("  <span>hello</span>\n"))
        assertEquals("  ", indent("<%= if @visible do %>\n"))
        assertEquals("", indent("<img src=\"x\" />\n"))
        assertEquals("", indent("<!-- <div> -->\n"))
    }
    @Test fun `finds matching tags by name with nesting`() {
        val text = "<div><.card><span>Hi</span></.card></div>"
        assertEquals(text.indexOf("</.card>") + 2, HeexTagEditing.matchingTag(text, text.indexOf(".card")))
        assertEquals(text.indexOf(".card"), HeexTagEditing.matchingTag(text, text.indexOf("</.card>") + 2))
        assertNull(HeexTagEditing.matchingTag("<div><br></div>", 6))
        assertNull(HeexTagEditing.matchingTag("<div><p></div>", 6))
        assertNull(HeexTagEditing.matchingTag("<!-- <p></p> -->", 7))
    }
}
