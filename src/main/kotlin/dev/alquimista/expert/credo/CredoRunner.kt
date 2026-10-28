package dev.alquimista.expert.credo

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.execution.process.CapturingProcessHandler
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileDocumentManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.psi.PsiManager
import dev.alquimista.expert.sdk.MixSdkCommand
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.ConcurrentHashMap

@Service(Service.Level.PROJECT)
class CredoRunner(private val project: Project) {
    private val engines = ConcurrentHashMap<Path, CredoEngine>()

    init {
        val connection = ApplicationManager.getApplication().messageBus.connect(project)
        connection.subscribe(
            FileDocumentManagerListener.TOPIC,
            object : FileDocumentManagerListener {
                override fun afterDocumentSaved(document: Document) {
                    val file = FileDocumentManager.getInstance().getFile(document) ?: return
                    val root = project.basePath?.let(Paths::get)?.toAbsolutePath()?.normalize() ?: return
                    if (!runCatching { Paths.get(file.path).toAbsolutePath().normalize().startsWith(root) }.getOrDefault(false)) return
                    if (file.extension !in setOf("ex", "exs") && file.name !in setOf("mix.lock", ".credo.exs")) return
                    ApplicationManager.getApplication().invokeLater {
                        if (project.isDisposed || !CredoSettings.getInstance(project).state.enabled) return@invokeLater
                        val daemon = DaemonCodeAnalyzer.getInstance(project)
                        if (file.name in setOf("mix.exs", "mix.lock", ".credo.exs")) {
                            engines.clear()
                            daemon.restart("Credo configuration changed")
                        } else {
                            PsiManager.getInstance(project).findFile(file)?.let { daemon.restart(it, "Elixir file saved") }
                        }
                    }
                }
            },
        )
        connection.subscribe(VirtualFileManager.VFS_CHANGES, object : BulkFileListener {
            override fun after(events: List<VFileEvent>) {
                if (project.isDisposed || !CredoSettings.getInstance(project).state.enabled) return
                val root = project.basePath?.let(Paths::get) ?: return
                if (events.none { event -> runCatching { CredoProject.isConfigurationChange(root, Paths.get(event.path)) }.getOrDefault(false) }) return
                ApplicationManager.getApplication().invokeLater {
                    if (project.isDisposed || !CredoSettings.getInstance(project).state.enabled) return@invokeLater
                    engines.clear()
                    DaemonCodeAnalyzer.getInstance(project).restart("Credo dependencies changed")
                }
            }
        })
    }

    fun clear() = engines.clear()

    fun analyze(root: Path, mixRoot: Path, file: Path, sdkKey: String, revision: String): List<CredoIssue>? {
        if (project.isDisposed || !CredoSettings.getInstance(project).state.enabled) return null
        val engine = engines.computeIfAbsent(mixRoot) { directory ->
            CredoEngine { arguments ->
                try {
                    val command = MixSdkCommand.commandLine(project, arguments)
                        .withWorkDirectory(directory.toFile())
                        .withEnvironment("NO_COLOR", "1")
                    val result = CapturingProcessHandler(command).runProcess(120_000, true)
                    if (result.isTimeout || result.isCancelled || result.exitCode != 0) {
                        LOG.warn("Credo command failed: ${result.stderr.takeLast(1000)}")
                        null
                    } else result.stdout
                } catch (error: Exception) {
                    LOG.warn("Could not run Credo command", error)
                    null
                }
            }
        }
        val relative = mixRoot.relativize(file).toString()
        return engine.analyze(true, CredoProject.fingerprint(root, mixRoot, sdkKey), relative, revision)
    }

    companion object {
        private val LOG = Logger.getInstance(CredoRunner::class.java)
    }
}
