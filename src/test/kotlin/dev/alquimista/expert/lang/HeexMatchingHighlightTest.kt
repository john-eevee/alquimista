package dev.alquimista.expert.lang

import com.intellij.openapi.util.TextRange
import org.junit.Assert.*
import org.junit.Test

class HeexMatchingHighlightTest {
    @Test fun `matches nested tags from either opening or closing name`() {
        val text = "<.card><div><div>value</div></div></.card>"
        val open = text.indexOf("<div>", text.indexOf("<div>") + 1) + 1
        val close = text.indexOf("</div>") + 2
        val expected = listOf(TextRange(open, open + 3), TextRange(close, close + 3))
        assertEquals(expected, HeexTagHighlights.ranges(text, open))
        assertEquals(expected, HeexTagHighlights.ranges(text, close + 3))
    }

    @Test fun `ignores comments attributes void tags and broken pairs`() {
        for ((text, offset) in listOf(
            "<!-- <div></div> -->" to 7,
            "<div title=\"test\"></div>" to 7,
            "<img />" to 2,
            "<div><p></div>" to 6,
            "<div>text" to 2,
        )) assertTrue(text, HeexTagHighlights.ranges(text, offset).isEmpty())
    }

    @Test fun `handles slots components and markup`() {
        val text = "<:item><.card><p>hi</p></.card></:item>"
        for (name in listOf(":item", ".card", "p")) {
            val start = text.indexOf("<$name>") + 1
            val end = text.indexOf("</$name>") + 2
            assertEquals(name, listOf(TextRange(start, start + name.length), TextRange(end, end + name.length)), HeexTagHighlights.ranges(text, start))
        }
    }
    @Test fun `tag names and components match with exact case`() {
        assertTrue(HeexTagHighlights.ranges("<.Card></.card>", 2).isEmpty())
        assertTrue(HeexTagHighlights.ranges("<Foo></foo>", 2).isEmpty())
        assertTrue(HeexTagHighlights.ranges("<Div></div>", 2).isEmpty())
        assertEquals(2, HeexTagHighlights.ranges("<Input></Input>", 2).size)
    }
}
