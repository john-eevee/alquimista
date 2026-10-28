package dev.yaee.expert.lang

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ElixirSmartEnterTest {
    private fun completion(text: String): Pair<Int, String>? =
        ElixirSmartEnterCompletion.find(text, text.length)

    @Test fun `closes nested delimiters in reverse order`() {
        assertEquals(13 to "]})", completion("call(%{a: [42"))
    }

    @Test fun `ignores delimiters in strings and comments`() {
        assertNull(completion("IO.puts(\"[\") # {"))
    }

    @Test fun `places closers before a trailing comment`() {
        assertEquals(8 to ")", ElixirSmartEnterCompletion.find("call(arg # comment", 8))
    }

    @Test fun `does not duplicate delimiters closed later`() {
        assertNull(ElixirSmartEnterCompletion.find("call(\n  value\n)", 5))
    }

    @Test fun `does not close delimiters belonging to an outer line`() {
        assertNull(completion("call(\n  value"))
    }

    @Test fun `does not insert into an unfinished string`() {
        assertNull(completion("call(\"unfinished"))
    }

    @Test fun `does not complete when caret is inside a comment`() {
        assertNull(ElixirSmartEnterCompletion.find("call( # comment", 12))
    }
}
