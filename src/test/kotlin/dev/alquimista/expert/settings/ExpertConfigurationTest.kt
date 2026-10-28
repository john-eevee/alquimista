package dev.alquimista.expert.settings

import com.google.gson.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExpertConfigurationTest {
    @Test fun `defaults match Expert configuration`() {
        val state = ExpertSettings.State()
        val options = ExpertConfiguration.toJson(state)
        assertEquals(2, options.getAsJsonObject("workspaceSymbols").get("minQueryLength").asInt)
        assertEquals("info", options.get("logLevel").asString)
        assertEquals("debug", options.get("fileLogLevel").asString)
        assertTrue(options.get("autoFetchDependencies").asBoolean)
        assertTrue(options.get("compileOnType").asBoolean)
        assertTrue(options.get("elixirSourcePath").isJsonNull)
    }

    @Test fun `configuration contains changed values and allows zero symbol query length`() {
        val state = ExpertSettings.State(
            minimumWorkspaceSymbolQueryLength = 0,
            logLevel = "warning", fileLogLevel = "error",
            elixirSourcePath = "/src/elixir", elixirExecutablePath = "/bin/elixir", erlangExecutablePath = "/bin/erl",
            autoFetchDependencies = false, compileOnType = false,
        )
        val options: JsonObject = ExpertConfiguration.toJson(state)
        assertEquals(0, options.getAsJsonObject("workspaceSymbols").get("minQueryLength").asInt)
        assertEquals("warning", options.get("logLevel").asString)
        assertEquals("error", options.get("fileLogLevel").asString)
        assertEquals("/src/elixir", options.get("elixirSourcePath").asString)
        assertEquals("/bin/elixir", options.get("elixirExecutablePath").asString)
        assertEquals("/bin/erl", options.get("erlangExecutablePath").asString)
        assertFalse(options.get("autoFetchDependencies").asBoolean)
        assertFalse(options.get("compileOnType").asBoolean)
        assertEquals(listOf(".heex", ".eex"), options.getAsJsonArray("additionalWatchedExtensions").map { it.asString })
    }

    @Test fun `state persists zero and all changed values`() {
        val settings = ExpertSettings()
        val state = ExpertSettings.State(minimumWorkspaceSymbolQueryLength = 0, logLevel = "error", compileOnType = false)
        settings.loadState(state)
        assertEquals(state, settings.state)
        assertEquals(0, settings.minimumWorkspaceSymbolQueryLength)
    }
}
