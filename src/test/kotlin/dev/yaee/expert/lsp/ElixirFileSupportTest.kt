package dev.yaee.expert.lsp

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ElixirFileSupportTest {
    @Test
    fun `supports Elixir and template extensions`() {
        listOf("ex", "exs", "heex", "eex").forEach { extension ->
            assertTrue(".$extension should use Expert", ElixirFileSupport.supportsExtension(extension))
        }
    }

    @Test
    fun `does not claim unrelated extensions`() {
        listOf("exfmt", "erl", "txt", "").forEach { extension ->
            assertFalse(".$extension should not use Expert", ElixirFileSupport.supportsExtension(extension))
        }
    }
}
