package dev.yaee.expert.lang

import com.intellij.lexer.Lexer
import com.intellij.lexer.DelegateLexer
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors as Colors
import com.intellij.openapi.editor.HighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.StringEscapesTokenTypes
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType
import dev.murek.elixirij.lang.*
import dev.murek.elixirij.lang.lexer.ExLexer

class ElixirSyntaxHighlighter : SyntaxHighlighterBase() {
    override fun getHighlightingLexer(): Lexer = ElixirHighlightingLexer()

    override fun getTokenHighlights(tokenType: IElementType): Array<TextAttributesKey> {
        val key = when {
            tokenType == TokenType.BAD_CHARACTER -> HighlighterColors.BAD_CHARACTER
            tokenType == StringEscapesTokenTypes.VALID_STRING_ESCAPE_TOKEN -> Colors.VALID_STRING_ESCAPE
            tokenType == StringEscapesTokenTypes.INVALID_CHARACTER_ESCAPE_TOKEN ||
                tokenType == StringEscapesTokenTypes.INVALID_UNICODE_ESCAPE_TOKEN -> Colors.INVALID_STRING_ESCAPE
            EX_COMMENTS.contains(tokenType) -> Colors.LINE_COMMENT
            tokenType == EX_RAISE || tokenType == EX_SEND || tokenType == EX_THROW -> null
            tokenType == HIGHLIGHT_MACRO_KEYWORD -> Colors.KEYWORD
            EX_KEYWORDS.contains(tokenType) -> Colors.KEYWORD
            EX_NUMBERS.contains(tokenType) -> Colors.NUMBER
            EX_STRINGS.contains(tokenType) || tokenType == EX_SIGIL -> Colors.STRING
            tokenType == EX_ATOM || tokenType == EX_ATOM_QUOTED || tokenType == EX_KW_IDENTIFIER -> Colors.CONSTANT
            EX_OPERATORS.contains(tokenType) -> Colors.OPERATION_SIGN
            EX_PARENS.contains(tokenType) -> Colors.PARENTHESES
            EX_BRACKETS.contains(tokenType) -> Colors.BRACKETS
            EX_BRACES.contains(tokenType) || EX_BINARY_DELIMITERS.contains(tokenType) -> Colors.BRACES
            tokenType == EX_COMMA -> Colors.COMMA
            tokenType == EX_DOT -> Colors.DOT
            tokenType == EX_SEMICOLON -> Colors.SEMICOLON
            else -> null
        }
        return pack(key)
    }
}

private val HIGHLIGHT_MACRO_KEYWORD = ExTokenType("HIGHLIGHT_MACRO_KEYWORD")

private class ElixirHighlightingLexer : DelegateLexer(ExLexer()) {
    override fun getTokenType(): IElementType? {
        val token = super.getTokenType()
        if (token != EX_IDENTIFIER) return token
        val word = bufferSequence.subSequence(tokenStart, tokenEnd).toString()
        return if (word.startsWith("def") || word in CONTROL_MACROS) HIGHLIGHT_MACRO_KEYWORD else token
    }

    companion object {
        private val CONTROL_MACROS = setOf(
            "case", "with", "if", "unless", "cond", "for", "receive", "try", "quote",
            "alias", "import", "require", "use"
        )
    }
}

class ElixirSyntaxHighlighterFactory : SyntaxHighlighterFactory() {
    override fun getSyntaxHighlighter(project: Project?, virtualFile: VirtualFile?): SyntaxHighlighter =
        ElixirSyntaxHighlighter()
}
