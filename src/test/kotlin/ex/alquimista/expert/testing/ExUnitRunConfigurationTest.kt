package ex.alquimista.expert.testing

import org.junit.Assert.*
import org.junit.Test
import org.jdom.Element
import javax.xml.parsers.DocumentBuilderFactory

class ExUnitRunConfigurationTest {
    @Test
    fun `group and individual gutters select their line while module gutters select the module`() {
        val file = "test/example_test.exs"
        assertEquals(ExUnitRunSelection(file, 3, null), ExUnitRunSelection.forTarget(file, ExUnitTarget("group", 3, ExUnitTargetKind.DESCRIBE)))
        assertEquals(ExUnitRunSelection(file, 4, null), ExUnitRunSelection.forTarget(file, ExUnitTarget("test", 4, ExUnitTargetKind.TEST)))
        assertEquals(ExUnitRunSelection(file, null, "ExampleTest"), ExUnitRunSelection.forTarget(file, ExUnitTarget("ExampleTest", 1, ExUnitTargetKind.MODULE)))
    }

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
