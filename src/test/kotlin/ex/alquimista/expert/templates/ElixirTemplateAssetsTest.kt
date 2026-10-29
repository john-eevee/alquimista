package ex.alquimista.expert.templates

import ex.alquimista.expert.lang.ElixirPostfixExpansion
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

        assertTrue(names.containsAll(listOf("def", "defp", "do", "fn", "defmodule", "defmacro", "defmacrop", "defimpl", "case", "with", "pipe", "test", "describe")))
    }

    @Test
    fun `live template variables use IntelliJ placeholder syntax`() {
        val document = javaClass.getResourceAsStream("/liveTemplates/Elixir.xml")!!.use {
            DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(it)
        }
        val templates = document.getElementsByTagName("template")
        for (i in 0 until templates.length) {
            val template = templates.item(i) as Element
            val value = template.getAttribute("value")
            val variables = template.getElementsByTagName("variable")
            for (j in 0 until variables.length) {
                val variable = variables.item(j) as Element
                val name = variable.getAttribute("name")
                assertTrue("${template.getAttribute("name")} must contain variable $name", value.contains("$$name$"))
            }
        }
    }

    @Test
    fun `anonymous function template visits arguments before the body`() {
        val document = javaClass.getResourceAsStream("/liveTemplates/Elixir.xml")!!.use {
            DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(it)
        }
        val templates = document.getElementsByTagName("template")
        val fn = (0 until templates.length).map { templates.item(it) as Element }.single { it.getAttribute("name") == "fn" }
        assertEquals("fn $" + "ARGS$ ->\n  $" + "END$\nend", fn.getAttribute("value"))
        val args = fn.getElementsByTagName("variable").item(0) as Element
        assertEquals("ARGS", args.getAttribute("name"))
        assertEquals("true", args.getAttribute("alwaysStopAt"))
    }

    @Test
    fun `definition templates have editable names and a body`() {
        val document = javaClass.getResourceAsStream("/liveTemplates/Elixir.xml")!!.use {
            DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(it)
        }
        val templates = document.getElementsByTagName("template")
        val byName = (0 until templates.length).map { templates.item(it) as Element }.associateBy { it.getAttribute("name") }
        val expected = mapOf(
            "def" to "def $" + "NAME$($" + "ARGS$) do\n  $" + "END$\nend",
            "defp" to "defp $" + "NAME$($" + "ARGS$) do\n  $" + "END$\nend",
            "defmacro" to "defmacro $" + "NAME$($" + "ARGS$) do\n  $" + "END$\nend",
            "defmacrop" to "defmacrop $" + "NAME$($" + "ARGS$) do\n  $" + "END$\nend",
            "defmodule" to "defmodule $" + "NAME$ do\n  $" + "END$\nend",
            "defimpl" to "defimpl $" + "PROTOCOL$, for: $" + "TYPE$ do\n  $" + "END$\nend",
            "do" to "do\n  $" + "END$\nend",
        )
        for ((name, value) in expected) {
            val template = byName.getValue(name)
            assertEquals(name, value, template.getAttribute("value"))
            val context = template.getElementsByTagName("option").item(0) as Element
            assertEquals("ELIXIR", context.getAttribute("name"))
            assertEquals("true", context.getAttribute("value"))
        }
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
