package dev.yaee.expert.lang

import com.intellij.lexer.Lexer
import com.intellij.lang.PsiParser
import org.junit.Assert.assertTrue
import org.junit.Assert.assertThrows
import org.junit.Test

class ElixirGrammarNamespaceTest {
    @Test fun `bundled lexer and parser use the Yaee namespace`() {
        assertTrue(Lexer::class.java.isAssignableFrom(Class.forName("dev.yaee.expert.grammar.lexer.ExLexer")))
        assertTrue(PsiParser::class.java.isAssignableFrom(Class.forName("dev.yaee.expert.grammar.parser.ExParser")))
        assertThrows(ClassNotFoundException::class.java) { Class.forName("dev.murek.elixirij.lang.lexer.ExLexer") }
    }
}
