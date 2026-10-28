package dev.yaee.expert.lang

import com.intellij.codeInsight.editorActions.smartEnter.SmartEnterProcessor
import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.actionSystem.EditorActionManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile

class ElixirSmartEnterProcessor : SmartEnterProcessor() {
    override fun process(project: Project, editor: Editor, psiFile: PsiFile): Boolean {
        val document = editor.document
        val caret = editor.caretModel.offset
        val (offset, closers) = ElixirSmartEnterCompletion.find(document.charsSequence, caret) ?: return false
        val lineEnd = document.getLineEndOffset(document.getLineNumber(caret))
        document.insertString(offset, closers)
        editor.caretModel.moveToOffset(lineEnd + closers.length)
        val context = DataManager.getInstance().getDataContext(editor.contentComponent)
        EditorActionManager.getInstance().getActionHandler(IdeActions.ACTION_EDITOR_ENTER).execute(editor, null, context)
        return true
    }
}
