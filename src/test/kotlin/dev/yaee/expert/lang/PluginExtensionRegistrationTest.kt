package dev.yaee.expert.lang

import org.junit.Assert.assertEquals
import org.junit.Test
import javax.xml.parsers.DocumentBuilderFactory

class PluginExtensionRegistrationTest {
    @Test
    fun `registers parser and postfix provider with platform extension points`() {
        val descriptor = javaClass.getResourceAsStream("/META-INF/plugin.xml")!!
        val document = descriptor.use { DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(it) }
        assertEquals(1, document.getElementsByTagName("lang.parserDefinition").length)
        assertEquals(1, document.getElementsByTagName("codeInsight.template.postfixTemplateProvider").length)
    }
}
