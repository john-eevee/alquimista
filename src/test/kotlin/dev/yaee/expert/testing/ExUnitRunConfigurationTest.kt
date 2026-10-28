package dev.yaee.expert.testing

import org.junit.Assert.*
import org.junit.Test
import org.jdom.Element
import javax.xml.parsers.DocumentBuilderFactory

class ExUnitRunConfigurationTest {
    @Test
    fun `persists a test selection for later reruns`() {
        val selection = ExUnitRunSelection("test/a_test.exs", 12, null)
        val element = Element("configuration")
        selection.write(element)
        assertEquals(selection, ExUnitRunSelection.read(element))
    }

    @Test
    fun `preserves an all tests or module selection`() {
        for (selection in listOf(ExUnitRunSelection(null, null, null), ExUnitRunSelection("test/a_test.exs", null, "AppTest"))) {
            val element = Element("configuration")
            selection.write(element)
            assertEquals(selection, ExUnitRunSelection.read(element))
        }
    }

    @Test
    fun `registers an ExUnit run configuration type`() {
        val descriptor = javaClass.getResourceAsStream("/META-INF/plugin.xml")!!
        val document = descriptor.use { DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(it) }
        val extensions = document.getElementsByTagName("configurationType")
        assertTrue((0 until extensions.length).any {
            (extensions.item(it) as org.w3c.dom.Element).getAttribute("implementation") == ExUnitConfigurationType::class.java.name
        })
    }
}
