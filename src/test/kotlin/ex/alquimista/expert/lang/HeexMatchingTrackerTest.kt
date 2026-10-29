package ex.alquimista.expert.lang

import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.CaretModel
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.editor.event.CaretEvent
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.colors.CodeInsightColors
import com.intellij.openapi.editor.markup.MarkupModel
import com.intellij.openapi.editor.markup.RangeHighlighter
import org.junit.Assert.*
import org.junit.Test
import java.lang.reflect.Proxy

class HeexMatchingTrackerTest {
    @Test fun `caret and edits update theme highlights and disposal removes them`() {
        var source = "<div>hello</div>"
        var caretOffset = 2
        var stamp = 1L
        var reads = 0
        var removed = 0
        val added = mutableListOf<Pair<Int, Int>>()
        val document = proxy(Document::class.java) { method, _ -> when (method) {
            "getModificationStamp" -> stamp
            "getCharsSequence" -> { reads++; source }
            else -> null
        } }
        val caret = proxy(CaretModel::class.java) { method, _ -> if (method == "getOffset") caretOffset else null }
        val markup = proxy(MarkupModel::class.java) { method, args -> when (method) {
            "addRangeHighlighter" -> {
                assertEquals(CodeInsightColors.MATCHED_BRACE_ATTRIBUTES, args[0])
                added += (args[1] as Int) to (args[2] as Int)
                proxy(RangeHighlighter::class.java) { _, _ -> null }
            }
            "removeHighlighter" -> { removed++; null }
            else -> null
        } }
        val editor = proxy(Editor::class.java) { method, _ -> when (method) {
            "isDisposed" -> false
            "getDocument" -> document
            "getCaretModel" -> caret
            "getMarkupModel" -> markup
            else -> null
        } }
        val tracker = HeexMatchingTagTracker(editor)
        tracker.update()
        assertEquals(listOf(1 to 4, 12 to 15), added)
        assertEquals(1, reads)
        caretOffset = 7
        val caretEvent = CaretEvent(proxy(Caret::class.java) { method, _ -> if (method == "getEditor") editor else null }, LogicalPosition(0, 0), LogicalPosition(0, 7))
        tracker.caretListener.caretPositionChanged(caretEvent)
        assertEquals(2, removed)
        assertEquals(1, reads)
        source = "<p>x</p>"
        stamp++
        caretOffset = 1
        val event = object : DocumentEvent(document) {
            override fun getOffset() = 0
            override fun getOldLength() = 0
            override fun getNewLength() = 0
            override fun getOldFragment(): CharSequence = ""
            override fun getNewFragment(): CharSequence = ""
            override fun getOldTimeStamp() = 1L
        }
        tracker.documentListener.documentChanged(event)
        assertEquals(2, reads)
        assertEquals(listOf(1 to 2, 6 to 7), added.takeLast(2))
        tracker.dispose()
        assertEquals(4, removed)
        val count = added.size
        tracker.update()
        assertEquals("Disposed trackers must not add highlights", count, added.size)
    }

    private fun <T> proxy(type: Class<T>, handler: (String, Array<out Any?>) -> Any?): T =
        type.cast(Proxy.newProxyInstance(type.classLoader, arrayOf(type)) { _, method, args -> handler(method.name, args ?: emptyArray()) })
}
