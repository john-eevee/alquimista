package dev.alquimista.expert.settings

import com.intellij.openapi.options.ConfigurationException
import org.junit.Assert.*
import org.junit.Test
import javax.swing.JCheckBox
import javax.swing.JComboBox
import javax.swing.JPanel
import javax.swing.JTextField

class ExpertConfigurableTest {
    @Test fun `page saves values and restarts only for valid changes`() {
        val settings = ExpertSettings()
        var restarts = 0
        val configurable = ExpertConfigurable({ settings }, { restarts++ })
        val panel = configurable.createComponent() as JPanel
        configurable.reset()
        val fields = panel.components.filterIsInstance<JTextField>()
        val combos = panel.components.filterIsInstance<JComboBox<*>>()
        val checks = panel.components.filterIsInstance<JCheckBox>()
        fields[1].text = "0"
        fields[2].text = " /opt/elixir "
        combos[0].selectedItem = "warning"
        checks[1].isSelected = false
        assertTrue(configurable.isModified)
        configurable.apply()
        assertEquals(0, settings.state.minimumWorkspaceSymbolQueryLength)
        assertEquals("/opt/elixir", settings.state.elixirSourcePath)
        assertEquals("warning", settings.state.logLevel)
        assertFalse(settings.state.compileOnType)
        assertEquals(1, restarts)
        configurable.reset()
        assertFalse(configurable.isModified)
        configurable.apply()
        assertEquals(1, restarts)
        fields[1].text = "-1"
        try {
            configurable.apply()
            fail("Negative query lengths must be rejected")
        } catch (_: ConfigurationException) {
            assertEquals(0, settings.state.minimumWorkspaceSymbolQueryLength)
            assertEquals(1, restarts)
        }
    }
}
