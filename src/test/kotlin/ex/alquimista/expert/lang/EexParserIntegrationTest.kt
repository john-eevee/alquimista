package ex.alquimista.expert.lang

import com.intellij.testFramework.ParsingTestCase
import javax.xml.parsers.DocumentBuilderFactory

class EexParserIntegrationTest : ParsingTestCase("", "eex", EexParserDefinition()) {
    override fun getTestDataPath(): String = "src/test/testData"

    fun testEexFileRetainsItsTextAndFileType() {
        val text = "Hello <%= Enum.join(@names, \", \") %>!"
        val file = parseFile("sample.eex", text)
        assertEquals(text, file.text)
        assertEquals(EexLanguage.INSTANCE, file.language)
        assertEquals(EexFileType.INSTANCE, file.fileType)
    }

    fun testPluginRegistersNativeEexWithoutTextMate() {
        val xml = javaClass.getResourceAsStream("/META-INF/plugin.xml")!!.use {
            DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(it)
        }
        val types = xml.getElementsByTagName("fileType")
        assertTrue((0 until types.length).any {
            types.item(it).attributes.getNamedItem("language")?.nodeValue == "EEx" &&
                types.item(it).attributes.getNamedItem("extensions")?.nodeValue == "eex"
        })
        val highlighters = xml.getElementsByTagName("lang.syntaxHighlighterFactory")
        assertTrue((0 until highlighters.length).any { highlighters.item(it).attributes.getNamedItem("language")?.nodeValue == "EEx" })
        assertEquals(0, xml.getElementsByTagName("textmate.bundleProvider").length)
        assertNull(javaClass.getResource("/textmate/package.json"))
    }
}
