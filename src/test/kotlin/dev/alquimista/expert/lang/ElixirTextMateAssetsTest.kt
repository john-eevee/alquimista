package dev.alquimista.expert.lang

import com.google.gson.JsonParser
import org.junit.Assert.assertTrue
import org.junit.Test

class ElixirTextMateAssetsTest {
    @Test
    fun `declares grammars for Elixir and template files`() {
        val manifest = javaClass.getResourceAsStream("/textmate/package.json")
            ?: throw AssertionError("TextMate bundle manifest is missing")
        val root = manifest.use { JsonParser.parseReader(it.reader()).asJsonObject }
        val languages = root.getAsJsonObject("contributes").getAsJsonArray("languages")
        val extensions = languages.flatMap { it.asJsonObject.getAsJsonArray("extensions").map { ext -> ext.asString } }

        assertTrue(extensions.containsAll(listOf(".ex", ".exs", ".eex")))
        assertTrue("HEEx uses its native file type", !extensions.contains(".heex"))
    }
}
