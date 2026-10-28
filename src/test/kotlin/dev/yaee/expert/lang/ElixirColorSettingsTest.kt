package dev.yaee.expert.lang

import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import dev.yaee.expert.grammar.EX_ALIAS
import dev.yaee.expert.grammar.EX_DO
import org.junit.Assert.*
import org.junit.Test
import org.jetbrains.plugins.textmate.language.syntax.highlighting.TextMateDefaultColorsProvider
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

    @Test fun `HEEx settings expose the TextMate shared theme colors`() {
        val page = HeexColorSettingsPage()
        val keys = page.attributeDescriptors.map { it.key }.toSet()
        assertTrue(keys.containsAll(listOf(DefaultLanguageHighlighterColors.MARKUP_TAG,
            DefaultLanguageHighlighterColors.MARKUP_ATTRIBUTE, DefaultLanguageHighlighterColors.STRING)))
        assertTrue(page.displayName.contains("HEEx"))
        val textMate = TextMateDefaultColorsProvider()
        assertEquals(DefaultLanguageHighlighterColors.MARKUP_TAG, textMate.getTextAttributesKey("punctuation.definition.tag"))
        assertEquals(DefaultLanguageHighlighterColors.MARKUP_ATTRIBUTE, textMate.getTextAttributesKey("entity.other.attribute-name"))
        assertEquals(DefaultLanguageHighlighterColors.STRING, textMate.getTextAttributesKey("string"))
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
