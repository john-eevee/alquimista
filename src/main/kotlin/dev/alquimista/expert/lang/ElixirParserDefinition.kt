package dev.alquimista.expert.lang

import com.intellij.extapi.psi.PsiFileBase
import com.intellij.lang.ASTNode
import com.intellij.lang.ParserDefinition
import com.intellij.lang.PsiParser
import com.intellij.lexer.Lexer
import com.intellij.openapi.project.Project
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IFileElementType
import com.intellij.psi.tree.TokenSet
import dev.alquimista.expert.grammar.EX_COMMENTS
import dev.alquimista.expert.grammar.EX_STRINGS
import dev.alquimista.expert.grammar.lexer.ExLexer
import dev.alquimista.expert.grammar.parser.ExParser
import dev.alquimista.expert.grammar.psi.ExTypes

class ElixirParserDefinition : ParserDefinition {
    override fun createLexer(project: Project?): Lexer = ExLexer()

    override fun createParser(project: Project?): PsiParser = ExParser()

    override fun getFileNodeType(): IFileElementType = FILE
    override fun getCommentTokens(): TokenSet = EX_COMMENTS
    override fun getStringLiteralElements(): TokenSet = EX_STRINGS
    override fun createElement(node: ASTNode): PsiElement = ExTypes.Factory.createElement(node)
    override fun createFile(viewProvider: FileViewProvider): PsiFile = ElixirPsiFile(viewProvider)

    private class ElixirPsiFile(viewProvider: FileViewProvider) : PsiFileBase(viewProvider, ElixirLanguage.INSTANCE) {
        override fun getFileType() = ElixirFileType.INSTANCE
    }

    companion object {
        private val FILE = IFileElementType(ElixirLanguage.INSTANCE)
    }
}
