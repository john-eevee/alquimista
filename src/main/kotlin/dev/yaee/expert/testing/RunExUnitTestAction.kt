package dev.yaee.expert.testing

import com.intellij.execution.process.CapturingProcessHandler
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project

internal class RunExUnitTestAction(
    private val relativePath: String,
    private val line: Int,
    private val displayName: String,
) : AnAction() {
    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val projectRoot = project.basePath ?: return
        val command = GeneralCommandLine("mix")
            .withParameters(MixTestCommand.arguments(relativePath, line))
            .withWorkDirectory(projectRoot)

        object : Task.Backgroundable(project, "Running $displayName", true) {
            override fun run(indicator: ProgressIndicator) {
                indicator.isIndeterminate = true
                val result = try {
                    CapturingProcessHandler(command).runProcess()
                } catch (error: Exception) {
                    LOG.warn("Could not start Mix test process", error)
                    NotificationGroupManager.getInstance().getNotificationGroup("Elixir Expert")
                        .createNotification("Could not run $displayName", error.message ?: "Could not start mix.", NotificationType.ERROR)
                        .notify(project)
                    return
                }
                LOG.info("mix ${command.parametersList.parameters.joinToString(" ")} exited ${result.exitCode}:\n${result.stdout}\n${result.stderr}")
                val type = if (result.exitCode == 0) NotificationType.INFORMATION else NotificationType.ERROR
                NotificationGroupManager.getInstance().getNotificationGroup("Elixir Expert")
                    .createNotification(
                        "${if (result.exitCode == 0) "Passed" else "Failed"}: $displayName",
                        result.stdout.trim().takeLast(MAX_NOTIFICATION_CHARS).ifEmpty { result.stderr.trim().takeLast(MAX_NOTIFICATION_CHARS) },
                        type,
                    )
                    .notify(project)
            }
        }.queue()
    }

    companion object {
        private const val MAX_NOTIFICATION_CHARS = 1200
        private val LOG = Logger.getInstance(RunExUnitTestAction::class.java)
    }
}
