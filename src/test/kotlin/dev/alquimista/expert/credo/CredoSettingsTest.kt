package dev.alquimista.expert.credo

import org.junit.Assert.*
import org.junit.Test
import javax.xml.parsers.DocumentBuilderFactory

class CredoSettingsTest {
    @Test
    fun `Credo is disabled until explicitly enabled and state can be restored`() {
        val settings = CredoSettings()
        assertFalse(settings.state.enabled)
        settings.state.enabled = true
        val restored = CredoSettings()
        restored.loadState(settings.state)
        assertTrue(restored.state.enabled)
    }

    @Test
    fun `plugin registers the Credo project settings and Elixir annotator`() {
        val descriptor = javaClass.getResourceAsStream("/META-INF/plugin.xml")!!
        val document = descriptor.use { DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(it) }
        val configurables = document.getElementsByTagName("projectConfigurable")
        assertTrue((0 until configurables.length).any {
            (configurables.item(it) as org.w3c.dom.Element).getAttribute("instance") == CredoConfigurable::class.java.name
        })
        val annotators = document.getElementsByTagName("externalAnnotator")
        assertTrue((0 until annotators.length).any {
            val element = annotators.item(it) as org.w3c.dom.Element
            element.getAttribute("language") == "Elixir" &&
                element.getAttribute("implementationClass") == CredoExternalAnnotator::class.java.name
        })
    }
}
