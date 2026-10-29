package ex.alquimista.expert.lsp

import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.BaseProcessHandler
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.lsp.api.LspClientManager
import com.intellij.platform.lsp.api.LspServerListener
import com.intellij.platform.lsp.api.ProjectWideLspClientDescriptor
import ex.alquimista.expert.settings.ExpertDownloader
import ex.alquimista.expert.settings.ExpertSettings
import ex.alquimista.expert.settings.MiseSupport

internal class ExpertLspServerDescriptor(project: Project) :
    ProjectWideLspClientDescriptor(project, "Expert") {

    override fun isSupportedFile(file: VirtualFile): Boolean =
        ElixirFileSupport.supportsExtension(file.extension.orEmpty())

    override fun getLanguageId(file: VirtualFile): String = when (file.extension) {
        "ex", "exs" -> "elixir"
        "eex" -> "eex"
        "heex" -> "heex"
        else -> super.getLanguageId(file)
    }

    override fun createCommandLine(): GeneralCommandLine {
        val configuredPath = ExpertSettings.getInstance().expertPath.trim()
        return MiseSupport.commandLine(configuredPath.ifEmpty { "expert" }, listOf("--stdio")).apply {
            project.basePath?.let(::withWorkDirectory)
        }
    }

    override fun startServerProcess(): BaseProcessHandler<*> {
        try {
            return super.startServerProcess()
        } catch (error: ExecutionException) {
            if (ExpertSettings.getInstance().expertPath.isBlank()) {
                NotificationGroupManager.getInstance().getNotificationGroup("Elixir Expert")
                    .createNotification(
                        "Expert is not installed",
                        "Downloading the latest Expert release for this machine.",
                        NotificationType.INFORMATION,
                    )
                    .notify(project)
                ExpertDownloader.download(project) { downloadError ->
                    NotificationGroupManager.getInstance().getNotificationGroup("Elixir Expert")
                        .createNotification(
                            "Could not download Expert",
                            downloadError.message ?: "The release download failed.",
                            NotificationType.ERROR,
                        )
                        .notify(project)
                }
            }
            throw error
        }
    }

    override val lspServerListener: LspServerListener = ExpertConfigurationListener(
        findClient = {
            LspClientManager.getInstance(project)
                .getClients(ExpertLspServerSupportProvider::class.java)
                .firstOrNull { it.descriptor === this@ExpertLspServerDescriptor }
        },
        settings = { ExpertSettings.getInstance().state },
    )
}
