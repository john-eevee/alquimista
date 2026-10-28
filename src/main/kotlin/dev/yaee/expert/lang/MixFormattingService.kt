package dev.yaee.expert.lang

import com.intellij.formatting.service.AsyncDocumentFormattingService
import com.intellij.formatting.service.AsyncFormattingRequest
import com.intellij.formatting.service.FormattingService
import com.intellij.psi.PsiFile
import dev.yaee.expert.credo.CredoProject
import dev.yaee.expert.sdk.MixSdkCommand
import java.nio.file.Paths
import java.util.EnumSet
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

class MixFormattingService : AsyncDocumentFormattingService() {
    override fun getFeatures(): Set<FormattingService.Feature> = EnumSet.of(FormattingService.Feature.AD_HOC_FORMATTING)
    override fun getName(): String = "Mix formatter"
    override fun getNotificationGroupId(): String = "Elixir Expert"

    override fun canFormat(file: PsiFile): Boolean {
        if (file.fileType != ElixirFileType.INSTANCE) return false
        val base = file.project.basePath ?: return false
        val path = file.virtualFile?.path ?: return false
        return runCatching { CredoProject.findMixRoot(Paths.get(base), Paths.get(path)) != null }.getOrDefault(false)
    }

    override fun createFormattingTask(request: AsyncFormattingRequest): FormattingTask = object : FormattingTask {
        private val cancelled = AtomicBoolean(false)
        private val process = AtomicReference<Process>()

        override fun run() {
            if (cancelled.get()) return
            try {
                val project = request.context.project
                val file = (request.ioFile ?: error("The file has no local path.")).toPath().toAbsolutePath().normalize()
                val base = project.basePath ?: error("The project has no root directory.")
                val mixRoot = CredoProject.findMixRoot(Paths.get(base), file) ?: error("No Mix project contains this file.")
                val relative = mixRoot.relativize(file).toString()
                val command = MixSdkCommand.commandLine(project, MixFormatter.arguments(relative)).withWorkDirectory(mixRoot.toFile())
                val result = MixFormatter.run({ command.createProcess() }, request.documentText) {
                    process.set(it)
                    if (cancelled.get()) it.destroyForcibly()
                }
                if (!cancelled.get()) {
                    if (result.text != null) request.onTextReady(result.text)
                    else request.onError("Mix format failed", result.error ?: "Mix format failed.")
                }
            } catch (error: Exception) {
                if (!cancelled.get()) request.onError("Mix format failed", error.message ?: "Could not run mix format.")
            }
        }

        override fun cancel(): Boolean {
            cancelled.set(true)
            process.get()?.destroyForcibly()
            return true
        }
    }
}
