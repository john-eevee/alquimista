package ex.alquimista.expert.lang

import com.intellij.openapi.editor.impl.DocumentImpl
import com.intellij.testFramework.ParsingTestCase

class ElixirFoldingTest : ParsingTestCase("", "ex", ElixirParserDefinition()) {
    override fun getTestDataPath(): String = "src/test/testData"

    fun testFoldsMultilineBlocksButNotSingleLineOrIncompleteBlocks() {
        val text = """defmodule Example do
  def run do
    :ok
  end
  def short, do: :ok
  action = fn x ->
    x
  end
end
"""
        val file = parseFile("example.ex", text)
        val regions = ElixirFoldingBuilder().buildFoldRegions(file, DocumentImpl(text), false)
        val folded = regions.map { text.substring(it.range.startOffset, it.range.endOffset) }
        assertEquals(3, folded.size)
        assertTrue(folded.any { it.startsWith("do\n  def run") && it.endsWith("end") })
        assertTrue(folded.any { it.startsWith("do\n    :ok") && it.endsWith("end") })
        assertTrue(folded.any { it.startsWith("fn x ->") && it.endsWith("end") })
    }

    fun testDoesNotFoldMissingEnd() {
        val text = "defmodule Example do\n  :ok\n"
        val file = parseFile("example.ex", text)
        assertEmpty(ElixirFoldingBuilder().buildFoldRegions(file, DocumentImpl(text), false).toList())
    }
}
