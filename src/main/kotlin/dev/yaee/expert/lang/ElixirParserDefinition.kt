package dev.yaee.expert.lang

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.extapi.psi.PsiFileBase
import com.intellij.lang.ASTNode
import com.intellij.lang.ParserDefinition
import com.intellij.lang.PsiParser
import com.intellij.lexer.Lexer
import com.intellij.lexer.LexerBase
import com.intellij.openapi.project.Project
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.IFileElementType
import com.intellij.psi.tree.TokenSet

internal object ElixirLineToken : IElementType("ELIXIR_LINE", ElixirLanguage.INSTANCE)

class ElixirLexer : LexerBase() {
    private var buffer: CharSequence = ""
    private var bufferEnd = 0
    private var tokenStart = 0
    private var tokenEnd = 0

    override fun start(buffer: CharSequence, startOffset: Int, endOffset: Int, initialState: Int) {
        this.buffer = buffer
        bufferEnd = endOffset
        tokenStart = startOffset
        tokenEnd = nextLineEnd(startOffset)
    }

    override fun getState(): Int = 0
    override fun getTokenType(): IElementType? = if (tokenStart < bufferEnd) ElixirLineToken else null
    override fun getTokenStart(): Int = tokenStart
    override fun getTokenEnd(): Int = tokenEnd
    override fun getBufferSequence(): CharSequence = buffer
    override fun getBufferEnd(): Int = bufferEnd

    override fun advance() {
        tokenStart = tokenEnd
        tokenEnd = if (tokenStart < bufferEnd) nextLineEnd(tokenStart) else tokenStart
    }

    private fun nextLineEnd(from: Int): Int {
        var offset = from
        while (offset < bufferEnd && buffer[offset] != '\n') offset++
        return if (offset < bufferEnd) offset + 1 else bufferEnd
    }
}

class ElixirParserDefinition : ParserDefinition {
    override fun createLexer(project: Project?): Lexer = ElixirLexer()

    override fun createParser(project: Project?): PsiParser = PsiParser { _, builder ->
        val file = builder.mark()
        while (!builder.eof()) builder.advanceLexer()
        file.done(FILE)
        builder.treeBuilt
    }

    override fun getFileNodeType(): IFileElementType = FILE
    override fun getCommentTokens(): TokenSet = TokenSet.EMPTY
    override fun getStringLiteralElements(): TokenSet = TokenSet.EMPTY
    override fun createElement(node: ASTNode): PsiElement = ASTWrapperPsiElement(node)
    override fun createFile(viewProvider: FileViewProvider): PsiFile = ElixirPsiFile(viewProvider)

    private class ElixirPsiFile(viewProvider: FileViewProvider) : PsiFileBase(viewProvider, ElixirLanguage.INSTANCE) {
        override fun getFileType() = ElixirFileType.INSTANCE
    }

    companion object {
        private val FILE = IFileElementType(ElixirLanguage.INSTANCE)
    }
}
