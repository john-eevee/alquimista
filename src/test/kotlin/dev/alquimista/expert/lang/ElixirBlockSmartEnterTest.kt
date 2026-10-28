package dev.alquimista.expert.lang

import org.junit.Assert.*
import org.junit.Test

class ElixirBlockSmartEnterTest {
    @Test fun `completes a module block and places caret in body`() {
        val text = "defmodule Example do"
        val edit = ElixirBlockSmartEnter.find(text, text.length)!!
        assertEquals(text.length, edit.offset)
        assertEquals("\n  \nend", edit.text)
        assertEquals(3, edit.caretOffset)
    }

    @Test fun `inserts nested end before outer end`() {
        val text = "defmodule Example do\n  def hello do\nend"
        val caret = text.indexOf("\nend")
        val edit = ElixirBlockSmartEnter.find(text, caret)!!
        assertEquals("\n    \n  end", edit.text)
        assertEquals(5, edit.caretOffset)
    }

    @Test fun `does not add end when block already has one`() {
        val text = "defmodule Example do\n  :ok\nend"
        assertNull(ElixirBlockSmartEnter.find(text, text.indexOf('\n')))
    }

    @Test fun `does not close around existing body or keyword arguments`() {
        val body = "defmodule Example do\n  def existing, do: :ok\n"
        assertNull(ElixirBlockSmartEnter.find(body, body.indexOf('\n')))
        assertNull(ElixirBlockSmartEnter.find("def f, do: :ok", 14))
    }

    @Test fun `ignores do in comments and strings`() {
        assertNull(ElixirBlockSmartEnter.find("value = \"do\"", 12))
        assertNull(ElixirBlockSmartEnter.find("# do", 4))
    }
}
