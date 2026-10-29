package dev.alquimista.expert.lang

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.extapi.psi.PsiFileBase
import com.intellij.lang.ASTNode
import com.intellij.lang.Language
import com.intellij.lang.ParserDefinition
import com.intellij.lang.PsiParser
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.IFileElementType
import com.intellij.psi.tree.TokenSet
import javax.swing.Icon

class EexLanguage private constructor() : Language("EEx") {
    companion object {
        @JvmField
        val INSTANCE = EexLanguage()
    }
}

class EexFileType private constructor() : LanguageFileType(EexLanguage.INSTANCE) {
    override fun getName(): String = "EEx"
    override fun getDescription(): String = "EEx template"
    override fun getDefaultExtension(): String = "eex"
    override fun getIcon(): Icon = ElixirIcons.FILE

    companion object {
        @JvmField
        val INSTANCE = EexFileType()
    }
}

object EexColors {
    val DELIMITER = TextAttributesKey.createTextAttributesKey(
        "ALQUIMISTA_EEX_DELIMITER",
        DefaultLanguageHighlighterColors.TEMPLATE_LANGUAGE_COLOR
    )
}

class EexSyntaxHighlighter : SyntaxHighlighterBase() {
    private val elixir = ElixirSyntaxHighlighter()
    override fun getHighlightingLexer() = EexLexer()
    override fun getTokenHighlights(tokenType: IElementType): Array<TextAttributesKey> = when (tokenType) {
        EEX_TEXT -> emptyArray()
        EEX_DELIMITER -> arrayOf(EexColors.DELIMITER)
        EEX_COMMENT -> arrayOf(ElixirColors.COMMENT)
        else -> elixir.getTokenHighlights(tokenType)
    }
}

class EexSyntaxHighlighterFactory : SyntaxHighlighterFactory() {
    override fun getSyntaxHighlighter(project: Project?, virtualFile: VirtualFile?): SyntaxHighlighter =
        EexSyntaxHighlighter()
}

class EexParserDefinition : ParserDefinition {
    override fun createLexer(project: Project?) = EexLexer()
    override fun createParser(project: Project?): PsiParser = PsiParser { root, builder ->
        val marker = builder.mark()
        while (!builder.eof()) builder.advanceLexer()
        marker.done(root)
        builder.treeBuilt
    }

    override fun getFileNodeType(): IFileElementType = FILE
    override fun getCommentTokens(): TokenSet = TokenSet.create(EEX_COMMENT)
    override fun getStringLiteralElements(): TokenSet = TokenSet.EMPTY
    override fun createElement(node: ASTNode): PsiElement = ASTWrapperPsiElement(node)
    override fun createFile(viewProvider: FileViewProvider): PsiFile = EexPsiFile(viewProvider)
    private class EexPsiFile(viewProvider: FileViewProvider) : PsiFileBase(viewProvider, EexLanguage.INSTANCE) {
        override fun getFileType() = EexFileType.INSTANCE
    }

    companion object {
        private val FILE = IFileElementType(EexLanguage.INSTANCE)
    }
}
