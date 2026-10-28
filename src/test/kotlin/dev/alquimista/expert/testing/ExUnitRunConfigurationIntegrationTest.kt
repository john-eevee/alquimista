package dev.alquimista.expert.testing

import com.intellij.testFramework.ParsingTestCase
import dev.alquimista.expert.lang.ElixirParserDefinition
import org.jdom.Element

class ExUnitRunConfigurationIntegrationTest : ParsingTestCase("", "ex", ElixirParserDefinition()) {
    override fun getTestDataPath(): String = "src/test/testData"

    fun testConfigurationRetainsGroupSelectionWhenSavedAndReloaded() {
        val type = ExUnitConfigurationType()
        val factory = type.configurationFactories.single()
        val first = factory.createTemplateConfiguration(project) as ExUnitRunConfiguration
        first.selection = ExUnitRunSelection("test/example_test.exs", 8, null)
        val xml = Element("configuration")
        first.writeExternal(xml)
        val restored = factory.createTemplateConfiguration(project) as ExUnitRunConfiguration
        restored.readExternal(xml)
        assertEquals(first.selection, restored.selection)
    }
}
