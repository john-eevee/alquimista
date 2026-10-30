package ex.alquimista.expert.lang

import com.intellij.testFramework.LightVirtualFile
import com.intellij.testFramework.ParsingTestCase
import javax.xml.parsers.DocumentBuilderFactory

class ElixirFileIconsTest : ParsingTestCase("", "ex", ElixirParserDefinition()) {
    override fun getTestDataPath(): String = "src/test/testData"

    fun testElixirFilesHaveIconsAndOtherFilesAreNotOverridden() {
        val provider = ElixirFileIconProvider()
        assertNotNull(ElixirFileType.INSTANCE.icon)
        for (name in listOf("sample.ex", "sample.exs", "sample.eex", "sample.heex", "SAMPLE.EX")) {
            assertNotNull(name, provider.getIcon(LightVirtualFile(name), 0, project))
        }
        assertNull(provider.getIcon(LightVirtualFile("sample.txt"), 0, project))
    }

    fun testLightAndDarkIconResourcesHaveTheSameSize() {
        for (name in listOf("elixir.svg", "elixir_dark.svg")) {
            val resource = javaClass.getResourceAsStream("/icons/$name")
            assertNotNull(name, resource)
            val root = resource!!.use { DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(it).documentElement }
            assertEquals("16", root.getAttribute("width"))
            assertEquals("16", root.getAttribute("height"))
        }
    }

    fun testFileIconProviderIsRegistered() {
        val resource = javaClass.getResourceAsStream("/META-INF/plugin.xml")!!
        val document = resource.use { DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(it) }
        val providers = document.getElementsByTagName("fileIconProvider")
        assertTrue((0 until providers.length).any {
            (providers.item(it) as org.w3c.dom.Element).getAttribute("implementation") == ElixirFileIconProvider::class.java.name
        })
    }

    fun testLightAndDarkIconResourcesUseDifferentColors() {
        fun fills(name: String): List<String> {
            val resource = javaClass.getResourceAsStream("/icons/$name")!!
            val paths = resource.use {
                DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(it).getElementsByTagName("path")
            }
            return (0 until paths.length).map {
                (paths.item(it) as org.w3c.dom.Element).getAttribute("fill")
            }
        }
        assertFalse("Light and dark icon resources should use different colors", fills("elixir.svg") == fills("elixir_dark.svg"))
    }
}
