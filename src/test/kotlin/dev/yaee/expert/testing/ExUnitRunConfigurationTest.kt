package dev.yaee.expert.testing

import org.junit.Assert.*
import org.junit.Test
import org.jdom.Element

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
}
