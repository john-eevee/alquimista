package ex.alquimista.expert.lang

import com.intellij.testFramework.LightVirtualFile
import com.intellij.testFramework.ParsingTestCase
import com.intellij.openapi.util.IconLoader
import java.awt.image.BufferedImage
import javax.swing.Icon
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

    fun testPlatformLoadsDifferentLightAndDarkVariants() {
        fun render(icon: Icon): BufferedImage {
            val image = BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB)
            val graphics = image.createGraphics()
            try { icon.paintIcon(null, graphics, 0, 0) } finally { graphics.dispose() }
            return image
        }
        val light = render(IconLoader.getDarkIcon(ElixirIcons.FILE, false))
        val dark = render(IconLoader.getDarkIcon(ElixirIcons.FILE, true))
        assertTrue("Light and dark icons should render differently", light.getRGB(8, 5) != dark.getRGB(8, 5))
    }
}
