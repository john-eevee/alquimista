package dev.alquimista.expert.lang

import com.intellij.lang.ASTNode
import com.intellij.lang.folding.FoldingBuilderEx
import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.project.DumbAware
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import dev.alquimista.expert.grammar.EX_END
import dev.alquimista.expert.grammar.psi.ExTypes

class ElixirFoldingBuilder : FoldingBuilderEx(), DumbAware {
    override fun buildFoldRegions(root: PsiElement, document: Document, quick: Boolean): Array<FoldingDescriptor> {
        val nodes = PsiTreeUtil.collectElements(root) {
            val node = it.node
            (node.elementType == ExTypes.DO_BLOCK || node.elementType == ExTypes.FN_EXPR) &&
                node.findChildByType(EX_END) != null &&
                document.getLineNumber(it.textRange.startOffset) < document.getLineNumber(it.textRange.endOffset - 1)
        }
        return nodes.map { FoldingDescriptor(it.node, it.textRange) }.toTypedArray()
    }

    override fun getPlaceholderText(node: ASTNode): String =
        if (node.elementType == ExTypes.FN_EXPR) "fn ... end" else "do ... end"

    override fun isCollapsedByDefault(node: ASTNode): Boolean = false
}
