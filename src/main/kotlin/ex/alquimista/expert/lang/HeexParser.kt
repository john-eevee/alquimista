package ex.alquimista.expert.lang

import com.intellij.lang.ASTNode
import com.intellij.lang.PsiBuilder
import com.intellij.lang.PsiParser
import com.intellij.lang.folding.FoldingBuilderEx
import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.project.DumbAware
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.IElementType
import com.intellij.psi.util.PsiTreeUtil

internal val HEEX_ELEMENT = IElementType("HEEX_ELEMENT", HeexLanguage.INSTANCE)

class HeexParser : PsiParser {
    private data class OpenTag(val name: String, val marker: PsiBuilder.Marker)

    override fun parse(root: IElementType, builder: PsiBuilder): ASTNode {
        val file = builder.mark()
        val stack = mutableListOf<OpenTag>()
        while (!builder.eof()) {
            if (builder.tokenType != HEEX_TAG_DELIMITER || builder.tokenText !in setOf("<", "</")) {
                builder.advanceLexer()
                continue
            }
            val closing = builder.tokenText == "</"
            val marker = if (!closing) builder.mark() else null
            builder.advanceLexer()
            if (builder.tokenType != HEEX_TAG_NAME) {
                marker?.drop()
                continue
            }
            val name = builder.tokenText.orEmpty()
            builder.advanceLexer()
            var ending: String? = null
            while (!builder.eof()) {
                if (builder.tokenType == HEEX_TAG_DELIMITER && builder.tokenText in setOf(">", "/>")) {
                    ending = builder.tokenText
                    builder.advanceLexer()
                    break
                }
                builder.advanceLexer()
            }
            if (closing) {
                val match = stack.indexOfLast { it.name == name }
                if (ending != null && match >= 0) {
                    while (stack.lastIndex > match) stack.removeAt(stack.lastIndex).marker.drop()
                    stack.removeAt(stack.lastIndex).marker.done(HEEX_ELEMENT)
                }
            } else if (ending == null) {
                marker!!.drop()
            } else if (ending == "/>" || name in HEEX_VOID_TAGS) {
                marker!!.done(HEEX_ELEMENT)
            } else {
                stack.add(OpenTag(name, marker!!))
            }
        }
        while (stack.isNotEmpty()) stack.removeAt(stack.lastIndex).marker.drop()
        file.done(root)
        return builder.treeBuilt
    }
}

class HeexFoldingBuilder : FoldingBuilderEx(), DumbAware {
    override fun buildFoldRegions(root: PsiElement, document: Document, quick: Boolean): Array<FoldingDescriptor> =
        PsiTreeUtil.collectElements(root) {
            it.node.elementType == HEEX_ELEMENT && it.textRange.length > 0 &&
                    document.getLineNumber(it.textRange.startOffset) < document.getLineNumber(it.textRange.endOffset - 1)
        }.map { FoldingDescriptor(it.node, it.textRange) }.toTypedArray()

    override fun getPlaceholderText(node: ASTNode): String =
        "<${node.text.substringAfter('<').takeWhile { it.isLetterOrDigit() || it in ".:-_" }}>…"

    override fun isCollapsedByDefault(node: ASTNode): Boolean = false
}
