package dev.yaee.expert.lang

import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import dev.yaee.expert.grammar.EX_ALIAS
import dev.yaee.expert.grammar.EX_DO
import org.junit.Assert.*
import org.junit.Test
import javax.xml.parsers.DocumentBuilderFactory

class ElixirColorSettingsTest {
    @Test fun `Elixir colors are independently configurable and inherit theme defaults`() {
        val highlighter = ElixirSyntaxHighlighter()
        assertArrayEquals(arrayOf(ElixirColors.KEYWORD), highlighter.getTokenHighlights(EX_DO))
        assertArrayEquals(arrayOf(ElixirColors.MODULE), highlighter.getTokenHighlights(EX_ALIAS))
        assertEquals(DefaultLanguageHighlighterColors.KEYWORD, ElixirColors.KEYWORD.fallbackAttributeKey)
        assertEquals(DefaultLanguageHighlighterColors.CLASS_REFERENCE, ElixirColors.MODULE.fallbackAttributeKey)
        val keys = ElixirColorSettingsPage().attributeDescriptors.map { it.key }.toSet()
        assertTrue(keys.containsAll(listOf(ElixirColors.KEYWORD, ElixirColors.MODULE, ElixirColors.FUNCTION_DECLARATION, ElixirColors.FUNCTION_CALL)))
    }

    @Test fun `HEEx colors are independent and inherit theme defaults`() {
        val page = HeexColorSettingsPage()
        val keys = page.attributeDescriptors.map { it.key }.toSet()
        assertTrue(keys.containsAll(listOf(HeexColors.TAG_NAME, HeexColors.ATTRIBUTE, HeexColors.STRING, HeexColors.KEYWORD)))
        assertEquals("HEEx", page.displayName)
        assertEquals(DefaultLanguageHighlighterColors.MARKUP_TAG, HeexColors.TAG_NAME.fallbackAttributeKey)
        assertNotEquals(ElixirColors.KEYWORD, HeexColors.KEYWORD)
    }

    @Test fun `both color pages are registered`() {
        val document = javaClass.getResourceAsStream("/META-INF/plugin.xml")!!.use {
            DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(it)
        }
        val pages = document.getElementsByTagName("colorSettingsPage")
        val implementations = (0 until pages.length).map { pages.item(it).attributes.getNamedItem("implementation").nodeValue }.toSet()
        assertTrue(implementations.contains("dev.yaee.expert.lang.ElixirColorSettingsPage"))
        assertTrue(implementations.contains("dev.yaee.expert.lang.HeexColorSettingsPage"))
    }
}
