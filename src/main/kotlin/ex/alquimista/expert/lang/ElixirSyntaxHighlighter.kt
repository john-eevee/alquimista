package ex.alquimista.expert.lang

import com.intellij.lexer.DelegateLexer
import com.intellij.lexer.Lexer
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.StringEscapesTokenTypes
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType
import ex.alquimista.expert.grammar.*
import ex.alquimista.expert.grammar.lexer.ExLexer

class ElixirSyntaxHighlighter : SyntaxHighlighterBase() {
    override fun getHighlightingLexer(): Lexer = ElixirHighlightingLexer()

    override fun getTokenHighlights(tokenType: IElementType): Array<TextAttributesKey> {
        val key = when {
            tokenType == TokenType.BAD_CHARACTER -> ElixirColors.BAD_CHARACTER
            tokenType == StringEscapesTokenTypes.VALID_STRING_ESCAPE_TOKEN -> ElixirColors.VALID_ESCAPE
            tokenType == StringEscapesTokenTypes.INVALID_CHARACTER_ESCAPE_TOKEN ||
                    tokenType == StringEscapesTokenTypes.INVALID_UNICODE_ESCAPE_TOKEN -> ElixirColors.INVALID_ESCAPE

            EX_COMMENTS.contains(tokenType) -> ElixirColors.COMMENT
            tokenType == EX_RAISE || tokenType == EX_SEND || tokenType == EX_THROW -> null
            tokenType == HIGHLIGHT_MACRO_KEYWORD -> ElixirColors.KEYWORD
            tokenType == HIGHLIGHT_MODULE_ATTRIBUTE -> ElixirColors.MODULE_ATTRIBUTE
            tokenType == HIGHLIGHT_FUNCTION_DECLARATION -> ElixirColors.FUNCTION_DECLARATION
            tokenType == HIGHLIGHT_FUNCTION_CALL -> ElixirColors.FUNCTION_CALL
            tokenType == EX_ALIAS -> ElixirColors.MODULE
            EX_KEYWORDS.contains(tokenType) -> ElixirColors.KEYWORD
            EX_NUMBERS.contains(tokenType) -> ElixirColors.NUMBER
            EX_STRINGS.contains(tokenType) || tokenType == EX_SIGIL -> ElixirColors.STRING
            tokenType == EX_ATOM || tokenType == EX_ATOM_QUOTED || tokenType == EX_KW_IDENTIFIER -> ElixirColors.ATOM
            EX_OPERATORS.contains(tokenType) -> ElixirColors.OPERATOR
            EX_PARENS.contains(tokenType) -> ElixirColors.PARENTHESES
            EX_BRACKETS.contains(tokenType) -> ElixirColors.BRACKETS
            EX_BRACES.contains(tokenType) || tokenType == EX_PERCENT || EX_BINARY_DELIMITERS.contains(tokenType) -> ElixirColors.BRACES
            tokenType == EX_COMMA -> ElixirColors.COMMA
            tokenType == EX_DOT -> ElixirColors.DOT
            tokenType == EX_SEMICOLON -> ElixirColors.SEMICOLON
            else -> null
        }
        return pack(key)
    }
}

private val HIGHLIGHT_MODULE_ATTRIBUTE = ExTokenType("HIGHLIGHT_MODULE_ATTRIBUTE")
private val HIGHLIGHT_MACRO_KEYWORD = ExTokenType("HIGHLIGHT_MACRO_KEYWORD")
private val HIGHLIGHT_FUNCTION_DECLARATION = ExTokenType("HIGHLIGHT_FUNCTION_DECLARATION")
private val HIGHLIGHT_FUNCTION_CALL = ExTokenType("HIGHLIGHT_FUNCTION_CALL")

private class ElixirHighlightingLexer : DelegateLexer(ExLexer()) {
    override fun getTokenType(): IElementType? {
        val token = super.getTokenType()
        if (token == EX_AT && tokenEnd < bufferEnd &&
            (bufferSequence[tokenEnd].isLetter() || bufferSequence[tokenEnd] == '_')
        ) return HIGHLIGHT_MODULE_ATTRIBUTE
        if (token == EX_IDENTIFIER && tokenStart > 0 && bufferSequence[tokenStart - 1] == '@') return HIGHLIGHT_MODULE_ATTRIBUTE
        if (token != EX_IDENTIFIER) return token
        val word = bufferSequence.subSequence(tokenStart, tokenEnd).toString()
        return when {
            word in CONTROL_MACROS || (word.startsWith("def") && isDeclaration(word)) -> HIGHLIGHT_MACRO_KEYWORD
            isFunctionDeclaration() -> HIGHLIGHT_FUNCTION_DECLARATION
            isFunctionCall() -> HIGHLIGHT_FUNCTION_CALL
            else -> token
        }
    }

    private fun linePrefix(): String {
        val start = bufferSequence.lastIndexOf('\n', tokenStart - 1).let { if (it < 0) 0 else it + 1 }
        return bufferSequence.subSequence(start, tokenStart).toString()
    }

    private fun isFunctionDeclaration(): Boolean = FUNCTION_HEAD.containsMatchIn(linePrefix())

    private fun isFunctionCall(): Boolean {
        if (linePrefix().trimEnd().endsWith('@')) return false
        val lexer = ExLexer()
        lexer.start(bufferSequence, tokenEnd, bufferEnd, 0)
        var separated = false
        while (lexer.tokenType == TokenType.WHITE_SPACE) {
            separated = true
            lexer.advance()
        }
        val next = lexer.tokenType
        if (next == EX_LPAREN) return true
        if (next == EX_SLASH && CAPTURE_HEAD.containsMatchIn(linePrefix())) {
            lexer.advance()
            return lexer.tokenType == EX_INTEGER
        }
        return separated && next in CALL_ARGUMENT_STARTS
    }

    private fun isDeclaration(word: String): Boolean {
        val text = bufferSequence
        val lineStart = text.lastIndexOf('\n', tokenStart - 1).let { if (it < 0) 0 else it + 1 }
        val prefix = text.subSequence(lineStart, tokenStart).toString().trimEnd()
        if (prefix.isNotEmpty() && !prefix.endsWith(';') && !prefix.endsWith("->") && !prefix.endsWith("do:")) return false
        var next = tokenEnd
        while (next < text.length && (text[next] == ' ' || text[next] == '\t')) next++
        if (next == text.length || text[next] == '\n' || text[next] == '\r') return word in BUILTIN_DECLARATIONS
        if (text[next] == '(') return word in BUILTIN_DECLARATIONS || hasDoArgument()
        if (next == tokenEnd) return false
        if (!text[next].isLetter() && text[next] != '_') return false
        return word in BUILTIN_DECLARATIONS || hasDoArgument()
    }

    private fun hasDoArgument(): Boolean {
        val lexer = ExLexer()
        lexer.start(bufferSequence, tokenEnd, bufferEnd, 0)
        var nesting = 0
        while (lexer.tokenType != null) {
            when (lexer.tokenType) {
                EX_DO -> return true
                EX_KW_IDENTIFIER -> if (bufferSequence.subSequence(lexer.tokenStart, lexer.tokenEnd)
                        .toString() == "do:"
                ) return true

                EX_LPAREN, EX_LBRACKET, EX_LBRACE, EX_PERCENT_LBRACE -> nesting++
                EX_RPAREN, EX_RBRACKET, EX_RBRACE -> if (nesting > 0) nesting--
                EX_EOL, EX_SEMICOLON, EX_COMMENT -> if (nesting == 0) return false
            }
            lexer.advance()
        }
        return false
    }

    companion object {
        private val CONTROL_MACROS = setOf(
            "case", "with", "if", "unless", "cond", "for", "receive", "try", "quote",
            "alias", "import", "require", "use"
        )
        private val BUILTIN_DECLARATIONS = setOf(
            "def", "defp", "defmodule", "defmacro", "defmacrop", "defguard", "defguardp",
            "defdelegate", "defexception", "defimpl", "defprotocol", "defstruct", "defoverridable"
        )
        private val FUNCTION_HEAD =
            Regex("(?:^|;|->|do:)\\s*(?:def|defp|defmacro|defmacrop|defguard|defguardp|defdelegate)\\s+$")
        private val CAPTURE_HEAD = Regex("&\\s*(?:[A-Z][\\w.]*\\.)?$")
        private val CALL_ARGUMENT_STARTS = setOf(
            EX_IDENTIFIER, EX_ALIAS, EX_ATOM, EX_ATOM_QUOTED, EX_KW_IDENTIFIER, EX_INTEGER, EX_FLOAT,
            EX_STRING_BEGIN, EX_CHARLIST_BEGIN, EX_SIGIL, EX_PERCENT, EX_PERCENT_LBRACE, EX_LBRACKET,
            EX_LBRACE, EX_LT_LT, EX_TRUE, EX_FALSE, EX_NIL
        )
    }
}

class ElixirSyntaxHighlighterFactory : SyntaxHighlighterFactory() {
    override fun getSyntaxHighlighter(project: Project?, virtualFile: VirtualFile?): SyntaxHighlighter =
        ElixirSyntaxHighlighter()
}
