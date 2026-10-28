package dev.yaee.expert.lang

import org.junit.Assert.assertEquals
import org.junit.Test

class ElixirIndentTest {
    @Test fun `indents after block and clause openings`() {
        assertEquals("  ", ElixirIndent.forNewLine("defmodule Example do\n", 21))
        val clause = "  case value do\n    :ok ->\n"
        assertEquals("      ", ElixirIndent.forNewLine(clause, clause.length))
        val alternative = "  else\n"
        assertEquals("    ", ElixirIndent.forNewLine(alternative, alternative.length))
    }

    @Test fun `preserves indentation after ordinary statements and comments`() {
        val text = "  value = \"do\" # do\n"
        assertEquals("  ", ElixirIndent.forNewLine(text, text.length))
    }

    @Test fun `indents after unclosed delimiters but not closed ones`() {
        val open = "  value = call(\n"
        assertEquals("    ", ElixirIndent.forNewLine(open, open.length))
        val closed = "  value = call()\n"
        assertEquals("  ", ElixirIndent.forNewLine(closed, closed.length))
    }
}
