package dev.alquimista.expert.lang

import com.intellij.openapi.editor.CaretModel
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.Document
import com.intellij.testFramework.ParsingTestCase
import java.lang.reflect.Proxy

class HeexEditorIntegrationTest : ParsingTestCase("", "heex", HeexParserDefinition()) {
    override fun getTestDataPath(): String = "src/test/testData"

    fun testTypedHandlerUpdatesEditorDocument() {
        val source = "<.card>"
        val file = parseFile("page.heex", source)
        val buffer = StringBuilder(source)
        val document = Document::class.java.cast(Proxy.newProxyInstance(Document::class.java.classLoader, arrayOf(Document::class.java)) { _, method, args ->
            when (method.name) {
                "getCharsSequence" -> buffer
                "insertString" -> { buffer.insert(args!![0] as Int, args[1] as CharSequence); null }
                else -> null
            }
        })
        val caret = proxy(CaretModel::class.java) { method -> if (method == "getOffset") source.length else null }
        val editor = proxy(Editor::class.java) { method ->
            when (method) {
                "getCaretModel" -> caret
                "getDocument" -> document
                else -> null
            }
        }
        HeexTypedHandler().charTyped('>', project, editor, file)
        assertEquals("<.card></.card>", buffer.toString())
    }

    private fun <T> proxy(type: Class<T>, handler: (String) -> Any?): T =
        type.cast(Proxy.newProxyInstance(type.classLoader, arrayOf(type)) { _, method, _ -> handler(method.name) })
}
