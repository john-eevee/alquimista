package dev.alquimista.expert.lang

import com.intellij.testFramework.ParsingTestCase
import javax.xml.parsers.DocumentBuilderFactory

class HeexParserIntegrationTest : ParsingTestCase("", "heex", HeexParserDefinition()) {
    override fun getTestDataPath(): String = "src/test/testData"

    fun testHeexFileParsesWithoutLosingText() {
        val text = "<.card title={@title}>Hello <%= @name %></.card>"
        val file = parseFile("example.heex", text)
        assertEquals(text, file.text)
        assertEquals(HeexLanguage.INSTANCE, file.language)
        assertEquals(HeexFileType.INSTANCE, file.fileType)
    }

    fun testNativeFileTypeAndHighlighterAreRegistered() {
        val xml = javaClass.getResourceAsStream("/META-INF/plugin.xml")!!.use {
            DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(it)
        }
        val types = xml.getElementsByTagName("fileType")
        assertTrue((0 until types.length).any {
            types.item(it).attributes.getNamedItem("language")?.nodeValue == "HEEx" &&
                types.item(it).attributes.getNamedItem("extensions")?.nodeValue == "heex"
        })
        val views = xml.getElementsByTagName("lang.psiStructureViewFactory")
        assertTrue((0 until views.length).any { views.item(it).attributes.getNamedItem("language")?.nodeValue == "HEEx" })
        val listeners = xml.getElementsByTagName("editorFactoryListener")
        assertTrue((0 until listeners.length).any {
            listeners.item(it).attributes.getNamedItem("implementation")?.nodeValue ==
                "dev.alquimista.expert.lang.HeexMatchingTagEditorListener"
        })
        val highlighters = xml.getElementsByTagName("lang.syntaxHighlighterFactory")
        assertTrue((0 until highlighters.length).any { highlighters.item(it).attributes.getNamedItem("language")?.nodeValue == "HEEx" })
    }
}
