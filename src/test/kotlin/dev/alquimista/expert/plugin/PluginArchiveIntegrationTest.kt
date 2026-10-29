package dev.alquimista.expert.plugin

import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.jar.JarInputStream
import java.util.zip.ZipFile
import javax.xml.parsers.DocumentBuilderFactory

class PluginArchiveIntegrationTest {
    @Test fun `packaged plugin includes registered classes and native language integrations`() {
        val archive = System.getProperty("alquimista.plugin.archive")
        assertNotNull("Gradle must supply the built plugin archive", archive)
        assertTrue("Plugin archive does not exist", Files.isRegularFile(Path.of(archive)))
        val names = mutableSetOf<String>()
        var descriptor: ByteArray? = null
        ZipFile(archive).use { zip ->
            val jars = zip.entries().asSequence().filter { it.name.endsWith(".jar") && !it.name.contains("searchableOptions") }.toList()
            assertEquals("Expected one main plugin JAR", 1, jars.size)
            JarInputStream(zip.getInputStream(jars.single())).use { jar ->
                while (true) {
                    val entry = jar.nextJarEntry ?: break
                    names += entry.name
                    if (entry.name == "META-INF/plugin.xml") descriptor = jar.readAllBytes()
                }
            }
        }
        assertFalse("Removed TextMate resources must not be packaged", names.any { it.contains("textmate", ignoreCase = true) })
        assertNotNull("Packaged plugin descriptor is missing", descriptor)
        val xml = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(ByteArrayInputStream(descriptor!!))
        val elements = xml.getElementsByTagName("*")
        val attributes = setOf("implementation", "implementationClass", "serviceImplementation", "instance", "class")
        var checked = 0
        for (i in 0 until elements.length) {
            val node = elements.item(i)
            for (attribute in attributes) {
                val name = node.attributes?.getNamedItem(attribute)?.nodeValue ?: continue
                if (!name.startsWith("dev.alquimista.expert.")) continue
                assertTrue("Registered class is absent from the plugin JAR: $name", names.contains(name.replace('.', '/') + ".class"))
                checked++
            }
        }
        assertTrue("Expected the plugin integrations to be registered", checked >= 25)
        for (language in listOf("Elixir", "EEx", "HEEx")) {
            val types = xml.getElementsByTagName("fileType")
            assertTrue("Missing $language file type", (0 until types.length).any { types.item(it).attributes.getNamedItem("language")?.nodeValue == language })
        }
    }
}
