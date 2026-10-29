package ex.alquimista.expert.lang

import com.intellij.openapi.editor.impl.DocumentImpl
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.ParsingTestCase

class HeexStructureTest : ParsingTestCase("", "heex", HeexParserDefinition()) {
    override fun getTestDataPath(): String = "src/test/testData"

    fun testNestedMarkupHasStructureAndMultilineTagsFold() {
        val text = """<.panel title={@title}>
  <p>Hello {@name}</p>
  <.card>
    <img src="x" />
  </.card>
</.panel>"""
        val file = parseFile("sample.heex", text)
        assertEquals(text, file.text)
        val tags = PsiTreeUtil.collectElements(file) { it.node.elementType == HEEX_ELEMENT }
        assertEquals(4, tags.size)
        assertTrue(tags.any { it.text.startsWith("<.panel") && it.text.endsWith("</.panel>") })
        assertTrue(tags.any { it.text.startsWith("<.card>") && it.text.endsWith("</.card>") })
        val folds = HeexFoldingBuilder().buildFoldRegions(file, DocumentImpl(text), false)
        assertEquals(2, folds.size)
        assertTrue(folds.any { text.substring(it.range.startOffset, it.range.endOffset).startsWith("<.card>") })
    }

    fun testUnclosedAndMismatchedTagsDoNotCreateInvalidFolds() {
        val text = "<div>\n  <span>text\n</div>\n<.unfinished>"
        val file = parseFile("sample.heex", text)
        assertEquals(text, file.text)
        val folds = HeexFoldingBuilder().buildFoldRegions(file, DocumentImpl(text), false)
        assertEquals(1, folds.size)
        assertEquals("<div>\n  <span>text\n</div>", text.substring(folds[0].range.startOffset, folds[0].range.endOffset))
    }
    fun testStructureViewShowsNestedTagsInSourceOrder() {
        val file = parseFile("sample.heex", "<.panel><p>Hi</p><.card><img /></.card></.panel>")
        val root = HeexStructureElement(file)
        val panel = root.children.single()
        assertEquals("<.panel>", panel.presentation.presentableText)
        assertEquals(listOf("<p>", "<.card>"), panel.children.map { it.presentation.presentableText })
        assertEquals("<img>", panel.children[1].children.single().presentation.presentableText)
    }
}
