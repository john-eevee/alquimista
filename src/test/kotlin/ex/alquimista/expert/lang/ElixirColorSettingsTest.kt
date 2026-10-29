package ex.alquimista.expert.lang

import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import ex.alquimista.expert.grammar.EX_ALIAS
import ex.alquimista.expert.grammar.EX_DO
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

    @Test fun `all color pages are registered`() {
        val document = javaClass.getResourceAsStream("/META-INF/plugin.xml")!!.use {
            DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(it)
        }
        val pages = document.getElementsByTagName("colorSettingsPage")
        val implementations = (0 until pages.length).map { pages.item(it).attributes.getNamedItem("implementation").nodeValue }.toSet()
        assertTrue(implementations.contains("ex.alquimista.expert.lang.ElixirColorSettingsPage"))
        assertTrue(implementations.contains("ex.alquimista.expert.lang.HeexColorSettingsPage"))
        assertTrue(implementations.contains("ex.alquimista.expert.lang.EexColorSettingsPage"))
    }
    @Test fun `EEx delimiter uses an editable theme based color`() {
        val page = EexColorSettingsPage()
        assertEquals("EEx", page.displayName)
        assertTrue(page.attributeDescriptors.any { it.key == EexColors.DELIMITER })
        assertEquals(DefaultLanguageHighlighterColors.TEMPLATE_LANGUAGE_COLOR, EexColors.DELIMITER.fallbackAttributeKey)
    }
}
