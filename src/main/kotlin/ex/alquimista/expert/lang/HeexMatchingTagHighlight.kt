package ex.alquimista.expert.lang

import com.intellij.openapi.Disposable
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.colors.CodeInsightColors
import com.intellij.openapi.editor.event.*
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.TextRange
import java.util.concurrent.ConcurrentHashMap

internal object HeexTagHighlights {
    fun nameBounds(tag: HeexTag): IntRange {
        val start = tag.start + if (tag.closing) 2 else 1
        return tag.start..(start + tag.name.length)
    }

    fun pair(tags: List<HeexTag>, offset: Int): Pair<HeexTag, HeexTag>? {
        val stack = mutableListOf<HeexTag>()
        val pairs = mutableMapOf<HeexTag, HeexTag>()
        for (tag in tags) {
            if (tag.closing) {
                val match = stack.indexOfLast { it.name == tag.name }
                if (match >= 0) {
                    while (stack.lastIndex > match) stack.removeAt(stack.lastIndex)
                    val opening = stack.removeAt(stack.lastIndex)
                    pairs[opening] = tag
                    pairs[tag] = opening
                }
            } else if (!tag.selfClosing) stack.add(tag)
        }
        val tag = tags.firstOrNull { offset in nameBounds(it) } ?: return null
        val other = pairs[tag] ?: return null
        return if (tag.closing) other to tag else tag to other
    }

    fun ranges(text: CharSequence, offset: Int): List<TextRange> = ranges(HeexTags.read(text), offset)
    fun ranges(tags: List<HeexTag>, offset: Int): List<TextRange> {
        val (opening, closing) = pair(tags, offset) ?: return emptyList()
        return listOf(opening, closing).map {
            val start = it.start + if (it.closing) 2 else 1
            TextRange(start, start + it.name.length)
        }
    }
}

/** Attaches a per-editor tracker only to real HEEx files. */
class HeexMatchingTagEditorListener : EditorFactoryListener, Disposable, DumbAware {
    private val trackers = ConcurrentHashMap<Editor, HeexMatchingTagTracker>()

    override fun editorCreated(event: EditorFactoryEvent) {
        val editor = event.editor
        val project = editor.project ?: return
        if (FileDocumentManager.getInstance().getFile(editor.document)?.extension != "heex") return
        val tracker = HeexMatchingTagTracker(editor) { trackers.remove(editor) }
        if (trackers.putIfAbsent(editor, tracker) != null) return
        if (!Disposer.tryRegister(project, tracker)) {
            trackers.remove(editor, tracker)
            return
        }
        editor.caretModel.addCaretListener(tracker.caretListener, tracker)
        editor.document.addDocumentListener(tracker.documentListener, tracker)
        tracker.update()
    }

    override fun editorReleased(event: EditorFactoryEvent) {
        trackers.remove(event.editor)?.let { if (!it.disposed) Disposer.dispose(it) }
    }

    override fun dispose() {
        trackers.values.forEach { if (!it.disposed) Disposer.dispose(it) }
        trackers.clear()
    }

}

internal class HeexMatchingTagTracker(
    private val editor: Editor,
    private val onDisposed: () -> Unit,
) : Disposable {
    constructor(editor: Editor) : this(editor, {})

    var disposed: Boolean = false
        private set
    private var stamp = Long.MIN_VALUE
    private var tags = emptyList<HeexTag>()
    private val highlights = mutableListOf<RangeHighlighter>()
    val caretListener = object : CaretListener {
        override fun caretPositionChanged(event: CaretEvent) = update()
    }
    val documentListener = object : DocumentListener {
        override fun documentChanged(event: DocumentEvent) = update()
    }

    fun update() {
        if (disposed || editor.isDisposed) return
        clearHighlights()
        val document = editor.document
        if (document.modificationStamp != stamp) {
            tags = HeexTags.read(document.charsSequence)
            stamp = document.modificationStamp
        }
        for (range in HeexTagHighlights.ranges(tags, editor.caretModel.offset)) {
            highlights += editor.markupModel.addRangeHighlighter(
                CodeInsightColors.MATCHED_BRACE_ATTRIBUTES,
                range.startOffset, range.endOffset, HighlighterLayer.ELEMENT_UNDER_CARET,
                HighlighterTargetArea.EXACT_RANGE,
            )
        }
    }

    private fun clearHighlights() {
        if (!editor.isDisposed) highlights.forEach { editor.markupModel.removeHighlighter(it) }
        highlights.clear()
    }

    override fun dispose() {
        if (disposed) return
        disposed = true
        clearHighlights()
        onDisposed()
    }
}
