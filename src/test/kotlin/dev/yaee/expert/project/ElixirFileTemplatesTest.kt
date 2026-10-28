package dev.yaee.expert.project

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import javax.xml.parsers.DocumentBuilderFactory

class ElixirFileTemplatesTest {
    @Test
    fun `new file kinds provide the four requested extensions`() {
        assertEquals(setOf("ex", "exs", "eex", "heex"), ElixirFileKind.entries.map { it.extension }.toSet())
        for (kind in ElixirFileKind.entries) {
            assertNotNull(kind.templateName, javaClass.getResource("/fileTemplates/internal/${kind.templateName}.${kind.extension}.ft"))
        }
    }

    @Test
    fun `action appears in the new file menu`() {
        val descriptor = javaClass.getResourceAsStream("/META-INF/plugin.xml")!!
        val document = descriptor.use { DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(it) }
        val actions = document.getElementsByTagName("action")
        val action = (0 until actions.length).map { actions.item(it) as org.w3c.dom.Element }
            .single { it.getAttribute("id") == "ElixirExpert.NewFile" }
        assertEquals("NewGroup", (action.getElementsByTagName("add-to-group").item(0) as org.w3c.dom.Element).getAttribute("group-id"))
    }
}
