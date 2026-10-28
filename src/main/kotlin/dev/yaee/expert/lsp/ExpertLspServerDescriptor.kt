package dev.yaee.expert.lsp

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.OSProcessHandler
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.lsp.api.ProjectWideLspServerDescriptor
import dev.yaee.expert.settings.ExpertDownloader
import dev.yaee.expert.settings.MiseSupport
import dev.yaee.expert.settings.ExpertSettings

internal class ExpertLspServerDescriptor(project: Project) :
    ProjectWideLspServerDescriptor(project, "Expert") {

    override fun isSupportedFile(file: VirtualFile): Boolean =
        ElixirFileSupport.supportsExtension(file.extension.orEmpty())

    override fun createCommandLine(): GeneralCommandLine {
        val configuredPath = ExpertSettings.getInstance().expertPath.trim()
        return MiseSupport.commandLine(configuredPath.ifEmpty { "expert" }, listOf("--stdio")).apply {
            project.basePath?.let(::withWorkDirectory)
        }
    }

    override fun startServerProcess(): OSProcessHandler {
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

    override fun createInitializationOptions(): Any {
        val settings = ExpertSettings.getInstance()
        return JsonObject().apply {
            add("workspaceSymbols", JsonObject().apply {
                addProperty("minQueryLength", settings.minimumWorkspaceSymbolQueryLength)
            })
            add("additionalWatchedExtensions", JsonArray().apply {
                add(JsonPrimitive(".heex"))
                add(JsonPrimitive(".eex"))
            })
        }
    }
}
