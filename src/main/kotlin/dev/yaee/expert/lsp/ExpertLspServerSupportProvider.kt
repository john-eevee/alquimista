package dev.yaee.expert.lsp

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.lsp.api.LspServerSupportProvider

internal class ExpertLspServerSupportProvider : LspServerSupportProvider {
    override fun fileOpened(
        project: Project,
        file: VirtualFile,
        serverStarter: LspServerSupportProvider.LspServerStarter,
    ) {
        if (ElixirFileSupport.supportsExtension(file.extension.orEmpty())) {
            serverStarter.ensureServerStarted(ExpertLspServerDescriptor(project))
        }
    }
}
