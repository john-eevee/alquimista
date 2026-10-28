package dev.alquimista.expert.templates

import dev.alquimista.expert.lang.ElixirPostfixExpansion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import javax.xml.parsers.DocumentBuilderFactory

class ElixirTemplateAssetsTest {
    @Test
    fun `bundles Elixir live templates for common idioms`() {
        val stream = javaClass.getResourceAsStream("/liveTemplates/Elixir.xml")
            ?: throw AssertionError("Elixir live template file is missing")
        val document = stream.use { DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(it) }
        val templates = document.getElementsByTagName("template")
        val names = (0 until templates.length).map { (templates.item(it) as Element).getAttribute("name") }

        assertTrue(names.containsAll(listOf("def", "defp", "case", "with", "pipe", "test", "describe")))
    }

    @Test
    fun `postfix templates wrap expressions and preserve caret positions`() {
        val inspect = ElixirPostfixExpansion.expand("inspect", "value")
        assertEquals("IO.inspect(value)", inspect.text)
        assertEquals(inspect.text.length - 1, inspect.caretOffset)

        val ok = ElixirPostfixExpansion.expand("ok", "fetch_user(id)")
        assertEquals("{:ok, fetch_user(id)}", ok.text)
        assertEquals(ok.text.length, ok.caretOffset)
    }
}
