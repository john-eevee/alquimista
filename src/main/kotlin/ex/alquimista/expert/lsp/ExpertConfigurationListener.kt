package ex.alquimista.expert.lsp

import com.intellij.platform.lsp.api.LspClient
import com.intellij.platform.lsp.api.LspServerListener
import ex.alquimista.expert.settings.ExpertConfiguration
import ex.alquimista.expert.settings.ExpertSettings
import org.eclipse.lsp4j.DidChangeConfigurationParams
import org.eclipse.lsp4j.InitializeResult

internal class ExpertConfigurationListener(
    private val findClient: () -> LspClient?,
    private val settings: () -> ExpertSettings.State,
) : LspServerListener {
    override fun serverInitialized(params: InitializeResult) {
        findClient()?.sendNotification { server ->
            server.workspaceService.didChangeConfiguration(
                DidChangeConfigurationParams(
                    ExpertConfiguration.toJson(
                        settings()
                    )
                )
            )
        }
    }
}
