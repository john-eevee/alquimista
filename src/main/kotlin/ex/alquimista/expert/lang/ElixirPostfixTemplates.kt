package ex.alquimista.expert.lang

import com.intellij.codeInsight.template.postfix.templates.PostfixTemplate
import com.intellij.codeInsight.template.postfix.templates.PostfixTemplateProvider
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile

data class PostfixExpansion(val text: String, val caretOffset: Int)

object ElixirPostfixExpansion {
    private val transforms: Map<String, (String) -> PostfixExpansion> = mapOf(
        "inspect" to { expression ->
            PostfixExpansion(
                "IO.inspect($expression)",
                "IO.inspect($expression)".length - 1
            )
        },
        "dbg" to { expression -> PostfixExpansion("dbg($expression)", "dbg($expression)".length - 1) },
        "ok" to { expression -> PostfixExpansion("{:ok, $expression}", "{:ok, $expression}".length) },
        "error" to { expression -> PostfixExpansion("{:error, $expression}", "{:error, $expression}".length) },
        "pipe" to { expression -> PostfixExpansion("$expression |> ", "$expression |> ".length) },
        "if" to { expression ->
            val text = "if $expression do\n  \nend"
            PostfixExpansion(text, text.indexOf("\n  ") + 3)
        },
    )

    fun expand(key: String, expression: String): PostfixExpansion =
        requireNotNull(transforms[key]) { "Unknown Elixir postfix template: $key" }(expression)

    fun keys(): Set<String> = transforms.keys
}

class ElixirPostfixTemplateProvider : PostfixTemplateProvider {
    override fun getTemplates(): Set<PostfixTemplate> = LinkedHashSet(
        ElixirPostfixExpansion.keys().map { ElixirPostfixTemplate(it, this) },
    )

    override fun isTerminalSymbol(currentChar: Char): Boolean = currentChar == '.'
    override fun preExpand(file: PsiFile, editor: Editor) = Unit
    override fun afterExpand(file: PsiFile, editor: Editor) = Unit
    override fun preCheck(copyFile: PsiFile, realEditor: Editor, currentOffset: Int): PsiFile = copyFile
}

private class ElixirPostfixTemplate(
    private val key: String,
    provider: PostfixTemplateProvider,
) : PostfixTemplate(key, key, ".$key", ElixirPostfixExpansion.expand(key, "value").text, provider) {
    override fun isApplicable(context: PsiElement, copyDocument: Document, newOffset: Int): Boolean {
        if (context.containingFile.language != ElixirLanguage.INSTANCE || newOffset !in 0..copyDocument.textLength) return false
        val line = copyDocument.getLineNumber(newOffset)
        val lineStart = copyDocument.getLineStartOffset(line)
        val prefix = copyDocument.getText(TextRange(lineStart, newOffset))
        val expression = prefix.trim()
        return expression.isNotEmpty() && !expression.endsWithAny("=", "->", "|>", ",", "do")
    }

    override fun expand(context: PsiElement, editor: Editor) {
        val document = editor.document
        val caret = editor.caretModel.offset.coerceAtMost(document.textLength)
        val lineStart = document.getLineStartOffset(document.getLineNumber(caret))
        val prefix = document.getText(TextRange(lineStart, caret))
        val indentation = prefix.takeWhile(Char::isWhitespace)
        val expressionStart = lineStart + indentation.length
        val expression = document.getText(TextRange(expressionStart, caret)).trim()
        if (expression.isEmpty()) return
        val expansion = ElixirPostfixExpansion.expand(key, expression)
        document.replaceString(expressionStart, caret, expansion.text)
        editor.caretModel.moveToOffset(expressionStart + expansion.caretOffset)
    }

    private fun String.endsWithAny(vararg suffixes: String): Boolean = suffixes.any(::endsWith)
}
