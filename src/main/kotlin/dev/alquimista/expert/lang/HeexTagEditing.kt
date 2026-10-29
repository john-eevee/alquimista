package dev.alquimista.expert.lang

import com.intellij.codeInsight.editorActions.TypedHandlerDelegate
import com.intellij.lang.Language
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.TokenType
import com.intellij.psi.codeStyle.lineIndent.LineIndentProvider
import dev.alquimista.expert.grammar.EX_AFTER
import dev.alquimista.expert.grammar.EX_ARROW
import dev.alquimista.expert.grammar.EX_CATCH
import dev.alquimista.expert.grammar.EX_COMMENT
import dev.alquimista.expert.grammar.EX_DO
import dev.alquimista.expert.grammar.EX_ELSE
import dev.alquimista.expert.grammar.EX_EOL
import dev.alquimista.expert.grammar.EX_FN
import dev.alquimista.expert.grammar.EX_RESCUE
import dev.alquimista.expert.grammar.lexer.ExLexer

internal data class HeexTag(val name: String, val start: Int, val end: Int, val closing: Boolean, val selfClosing: Boolean)

internal object HeexTags {
    fun read(text: CharSequence): List<HeexTag> {
        val lexer = HeexLexer()
        lexer.start(text)
        val tags = mutableListOf<HeexTag>()
        while (lexer.tokenType != null) {
            val opening = lexer.tokenType == HEEX_TAG_DELIMITER && text.subSequence(lexer.tokenStart, lexer.tokenEnd).toString() in listOf("<", "</")
            if (!opening) { lexer.advance(); continue }
            val start = lexer.tokenStart
            val closing = text.subSequence(lexer.tokenStart, lexer.tokenEnd).toString() == "</"
            lexer.advance()
            if (lexer.tokenType != HEEX_TAG_NAME) continue
            val name = text.subSequence(lexer.tokenStart, lexer.tokenEnd).toString()
            lexer.advance()
            while (lexer.tokenType != null) {
                val token = text.subSequence(lexer.tokenStart, lexer.tokenEnd).toString()
                if (lexer.tokenType == HEEX_TAG_DELIMITER && token in listOf(">", "/>")) {
                    tags += HeexTag(name, start, lexer.tokenEnd, closing, token == "/>" || name.lowercase() in HEEX_VOID_TAGS)
                    lexer.advance()
                    break
                }
                lexer.advance()
            }
        }
        return tags
    }

    fun stack(tags: List<HeexTag>, offset: Int): List<String> {
        val stack = mutableListOf<String>()
        for (tag in tags) {
            if (tag.end > offset) break
            if (tag.closing) {
                val match = stack.indexOfLast { it.equals(tag.name, true) }
                if (match >= 0) while (stack.size > match) stack.removeAt(stack.lastIndex)
            } else if (!tag.selfClosing) stack += tag.name
        }
        return stack
    }
}

internal object HeexTagEditing {
    fun closingTag(text: CharSequence, offset: Int): String? {
        if (offset <= 0 || offset > text.length || text[offset - 1] != '>') return null
        val tags = HeexTags.read(text)
        val tag = tags.firstOrNull { it.end == offset } ?: return null
        if (tag.closing || tag.selfClosing) return null
        val openCount = HeexTags.stack(tags, offset).count { it.equals(tag.name, true) }
        var available = 0
        for (later in tags.filter { it.start >= offset && it.name.equals(tag.name, true) }) {
            if (later.closing) available++ else if (!later.selfClosing) available--
        }
        return if (available >= openCount) null else "</${tag.name}>"
    }
}

class HeexTypedHandler : TypedHandlerDelegate(), DumbAware {
    override fun charTyped(c: Char, project: Project, editor: Editor, file: PsiFile): Result {
        if (c != '>' || file.language != HeexLanguage.INSTANCE) return Result.CONTINUE
        val offset = editor.caretModel.offset
        val closing = HeexTagEditing.closingTag(editor.document.charsSequence, offset) ?: return Result.CONTINUE
        editor.document.insertString(offset, closing)
        return Result.STOP
    }
}

class HeexLineIndentProvider : LineIndentProvider, DumbAware {
    override fun isSuitableFor(language: Language?): Boolean = language?.isKindOf(HeexLanguage.INSTANCE) == true
    override fun getLineIndent(project: Project, editor: Editor, language: Language?, offset: Int): String? =
        HeexIndent.forNewLine(editor.document.charsSequence, offset)
}

internal object HeexIndent {
    fun forNewLine(text: CharSequence, offset: Int): String? {
        if (offset <= 0 || offset > text.length) return null
        val newline = text.lastIndexOf('\n', offset - 1)
        if (newline < 0 || text.subSequence(newline + 1, offset).any { !it.isWhitespace() }) return null
        val lineStart = text.lastIndexOf('\n', newline - 1).let { if (it < 0) 0 else it + 1 }
        val indentation = text.subSequence(lineStart, newline).takeWhile { it == ' ' || it == '\t' }.toString()
        val tags = HeexTags.read(text)
        val opensTag = HeexTags.stack(tags, newline).size > HeexTags.stack(tags, lineStart).size
        val lexer = HeexLexer()
        lexer.start(text)
        var expressionStart = -1
        var opensBlock = false
        while (lexer.tokenType != null && lexer.tokenStart < newline) {
            if (lexer.tokenType == HEEX_EXPRESSION_DELIMITER) {
                val token = text.subSequence(lexer.tokenStart, lexer.tokenEnd).toString()
                if (token.startsWith("<%") && lexer.tokenStart >= lineStart) expressionStart = lexer.tokenEnd
                else if (token == "%>" && expressionStart >= lineStart) {
                    val ex = ExLexer()
                    ex.start(text, expressionStart, lexer.tokenStart, 0)
                    var last = ex.tokenType
                    while (ex.tokenType != null) {
                        if (ex.tokenType !in setOf(TokenType.WHITE_SPACE, EX_EOL, EX_COMMENT)) last = ex.tokenType
                        ex.advance()
                    }
                    opensBlock = last in setOf(EX_DO, EX_FN, EX_ARROW, EX_ELSE, EX_AFTER, EX_RESCUE, EX_CATCH)
                    expressionStart = -1
                }
            }
            lexer.advance()
        }
        return indentation + if (opensTag || opensBlock) "  " else ""
    }
}
