package dev.alquimista.expert.lang

import com.intellij.psi.PsiErrorElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.ParsingTestCase
import com.intellij.psi.util.elementType
import dev.alquimista.expert.grammar.EX_IDENTIFIER

class ElixirParserIntegrationTest : ParsingTestCase("", "ex", ElixirParserDefinition()) {
    override fun getTestDataPath(): String = "src/test/testData"

    fun testModuleWithMacroCallsAndPatternMatching() {
        val file = parseFile("sample.ex", """
            defmodule Example do
              def run({:ok, value}) when is_integer(value) do
                value |> IO.inspect()
              end
              custom_macro value, do: {:ok, value}
            end
        """.trimIndent())
        val errors = PsiTreeUtil.findChildrenOfType(file, PsiErrorElement::class.java)
        assertTrue("Unexpected parse errors: ${errors.map { it.errorDescription }}", errors.isEmpty())
        assertTrue("Expected structured PSI", file.node.getChildren(null).any { it.firstChildNode != null })
    }

    fun testExUnitDeclarationsExposeIdentifierLeaves() {
        val file = parseFile("sample_test.exs", """
            defmodule ExampleTest do
              use ExUnit.Case
              test "works" do
                assert true
              end
              property "holds" do
                assert true
              end
            end
        """.trimIndent())
        assertEmpty(PsiTreeUtil.findChildrenOfType(file, PsiErrorElement::class.java))
        val leaves = PsiTreeUtil.collectElements(file) { it.firstChild == null && it.elementType == EX_IDENTIFIER }
        assertEquals(listOf("test", "property"), leaves.map { it.text }.filter { it == "test" || it == "property" })
    }

    fun testCollectionsStringsAndControlFlow() {
        val file = parseFile("expressions.ex", """
            defmodule Sample do
              def describe(value) do
                case value do
                  %{name: name} -> "Hello, #{name}"
                  _ -> ~s(unknown)
                end
              end
            end
        """.trimIndent())
        val errors = PsiTreeUtil.findChildrenOfType(file, PsiErrorElement::class.java)
        assertTrue("Unexpected parse errors: ${errors.map { it.errorDescription }}", errors.isEmpty())
    }
}
