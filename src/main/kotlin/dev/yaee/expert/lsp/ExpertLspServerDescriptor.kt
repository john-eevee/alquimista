package dev.yaee.expert.lsp

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.lsp.api.ProjectWideLspServerDescriptor
import dev.yaee.expert.settings.ExpertSettings

internal class ExpertLspServerDescriptor(project: Project) :
    ProjectWideLspServerDescriptor(project, "Expert") {

    override fun isSupportedFile(file: VirtualFile): Boolean =
        ElixirFileSupport.supportsExtension(file.extension.orEmpty())

    override fun createCommandLine(): GeneralCommandLine {
        val configuredPath = ExpertSettings.getInstance().expertPath.trim()
        return GeneralCommandLine(configuredPath.ifEmpty { "expert" }, "--stdio").apply {
            project.basePath?.let(::withWorkDirectory)
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
