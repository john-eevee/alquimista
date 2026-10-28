package dev.yaee.expert.lsp

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.lsp.api.LspIntegrationProvider

internal class ExpertLspServerSupportProvider : LspIntegrationProvider {
    override fun fileOpened(
        project: Project,
        file: VirtualFile,
        clientStarter: LspIntegrationProvider.LspClientStarter,
    ) {
        if (ElixirFileSupport.supportsExtension(file.extension.orEmpty())) {
            clientStarter.ensureClientStarted(ExpertLspServerDescriptor(project))
        }
    }
}
