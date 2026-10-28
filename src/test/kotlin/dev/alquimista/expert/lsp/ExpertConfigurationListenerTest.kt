package dev.alquimista.expert.lsp

import com.google.gson.JsonObject
import com.intellij.platform.lsp.api.LspClient
import dev.alquimista.expert.settings.ExpertSettings
import org.eclipse.lsp4j.DidChangeConfigurationParams
import org.eclipse.lsp4j.InitializeResult
import org.eclipse.lsp4j.services.LanguageServer
import org.eclipse.lsp4j.services.WorkspaceService
import org.junit.Assert.assertEquals
import org.junit.Test
import java.lang.reflect.Proxy

class ExpertConfigurationListenerTest {
    @Test fun `initialization sends the current settings as a configuration notification`() {
        var received: DidChangeConfigurationParams? = null
        val workspace = proxy(WorkspaceService::class.java) { method, args ->
            if (method == "didChangeConfiguration") received = args[0] as DidChangeConfigurationParams
            null
        }
        val server = proxy(LanguageServer::class.java) { method, _ -> if (method == "getWorkspaceService") workspace else null }
        val client = proxy(LspClient::class.java) { method, args ->
            if (method == "sendNotification") {
                @Suppress("UNCHECKED_CAST")
                (args[0] as (LanguageServer) -> Unit)(server)
            }
            null
        }
        val state = ExpertSettings.State(minimumWorkspaceSymbolQueryLength = 0, compileOnType = false)
        ExpertConfigurationListener({ client }, { state }).serverInitialized(InitializeResult())
        val settings = received!!.settings as JsonObject
        assertEquals(0, settings.getAsJsonObject("workspaceSymbols").get("minQueryLength").asInt)
        assertEquals(false, settings.get("compileOnType").asBoolean)
    }

    @Test fun `initialization tolerates a stopped client`() {
        ExpertConfigurationListener({ null }, { ExpertSettings.State() }).serverInitialized(InitializeResult())
    }

    private fun <T> proxy(type: Class<T>, handler: (String, Array<out Any?>) -> Any?): T =
        type.cast(Proxy.newProxyInstance(type.classLoader, arrayOf(type)) { _, method, args -> handler(method.name, args ?: emptyArray()) })
}
