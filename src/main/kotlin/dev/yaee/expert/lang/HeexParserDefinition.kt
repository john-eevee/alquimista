package dev.yaee.expert.lang

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.extapi.psi.PsiFileBase
import com.intellij.lang.ASTNode
import com.intellij.lang.ParserDefinition
import com.intellij.lang.PsiParser
import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IFileElementType
import com.intellij.psi.tree.TokenSet
import javax.swing.Icon

class HeexFileType private constructor() : LanguageFileType(HeexLanguage.INSTANCE) {
    override fun getName(): String = "HEEx"
    override fun getDescription(): String = "HEEx template"
    override fun getDefaultExtension(): String = "heex"
    override fun getIcon(): Icon = ElixirIcons.FILE

    companion object {
        @JvmField val INSTANCE = HeexFileType()
    }
}

class HeexParserDefinition : ParserDefinition {
    override fun createLexer(project: Project?) = HeexLexer()
    override fun createParser(project: Project?): PsiParser = PsiParser { root, builder ->
        val marker = builder.mark()
        while (!builder.eof()) builder.advanceLexer()
        marker.done(root)
        builder.treeBuilt
    }
    override fun getFileNodeType(): IFileElementType = FILE
    override fun getCommentTokens(): TokenSet = TokenSet.create(HEEX_COMMENT)
    override fun getStringLiteralElements(): TokenSet = TokenSet.create(HEEX_STRING)
    override fun createElement(node: ASTNode): PsiElement = ASTWrapperPsiElement(node)
    override fun createFile(viewProvider: FileViewProvider): PsiFile = HeexPsiFile(viewProvider)

    private class HeexPsiFile(viewProvider: FileViewProvider) : PsiFileBase(viewProvider, HeexLanguage.INSTANCE) {
        override fun getFileType() = HeexFileType.INSTANCE
    }

    companion object {
        private val FILE = IFileElementType(HeexLanguage.INSTANCE)
    }
}

class HeexSyntaxHighlighterFactory : SyntaxHighlighterFactory() {
    override fun getSyntaxHighlighter(project: Project?, virtualFile: VirtualFile?): SyntaxHighlighter = HeexSyntaxHighlighter()
}
