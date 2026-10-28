package dev.yaee.expert.testing

import com.intellij.execution.process.CapturingProcessHandler
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import dev.yaee.expert.settings.MiseSupport

open class RunMixTaskAction(
    private val title: String,
    private val arguments: List<String>,
) : AnAction(title) {
    override fun update(event: AnActionEvent) {
        event.presentation.isEnabledAndVisible = event.project?.basePath != null
    }

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val projectRoot = project.basePath ?: return
        val command = MiseSupport.commandLine("mix", arguments).withWorkDirectory(projectRoot)
        object : Task.Backgroundable(project, title, true) {
            override fun run(indicator: ProgressIndicator) {
                indicator.isIndeterminate = true
                val result = try {
                    CapturingProcessHandler(command).runProcess()
                } catch (error: Exception) {
                    LOG.warn("Could not start $title", error)
                    notify(project, "Could not start $title", error.message ?: "Could not start mix.", NotificationType.ERROR)
                    return
                }
                val output = (result.stdout.ifBlank { result.stderr }).trim().takeLast(MAX_NOTIFICATION_CHARS)
                val type = if (result.exitCode == 0) NotificationType.INFORMATION else NotificationType.ERROR
                notify(project, "$title ${if (result.exitCode == 0) "completed" else "failed"}", output, type)
                LOG.info("$title exited ${result.exitCode}:\n${result.stdout}\n${result.stderr}")
            }
        }.queue()
    }

    private fun notify(project: com.intellij.openapi.project.Project, title: String, body: String, type: NotificationType) {
        NotificationGroupManager.getInstance().getNotificationGroup("Elixir Expert")
            .createNotification(title, body, type).notify(project)
    }

    companion object {
        private const val MAX_NOTIFICATION_CHARS = 1200
        private val LOG = Logger.getInstance(RunMixTaskAction::class.java)
    }
}

class RunMixTestsAction : RunMixTaskAction("Running Mix Tests", MixTask.TEST)
class MixFormatAction : RunMixTaskAction("Running Mix Format", MixTask.FORMAT)
class MixDepsGetAction : RunMixTaskAction("Fetching Mix Dependencies", MixTask.DEPENDENCIES)
