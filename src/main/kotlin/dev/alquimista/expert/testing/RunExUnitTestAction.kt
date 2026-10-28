package dev.alquimista.expert.testing

import com.intellij.execution.ExecutionException
import com.intellij.execution.RunManager
import com.intellij.execution.configurations.ConfigurationTypeUtil
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.runners.ExecutionEnvironmentBuilder
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.project.Project

internal class RunExUnitTestAction(private val selection: ExUnitRunSelection, private val displayName: String) : AnAction("Run $displayName") {
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun actionPerformed(event: AnActionEvent) {
        event.project?.let { ExUnitRunner.run(it, displayName, selection) }
    }
}

internal object ExUnitRunner {
    fun run(project: Project, name: String, selection: ExUnitRunSelection) {
        try {
            val manager = RunManager.getInstance(project)
            val type = ConfigurationTypeUtil.findConfigurationType(ExUnitConfigurationType::class.java)
            val settings = manager.createConfiguration(name, type.configurationFactories.single())
            (settings.configuration as ExUnitRunConfiguration).selection = selection
            manager.setTemporaryConfiguration(settings)
            manager.selectedConfiguration = settings
            ExecutionEnvironmentBuilder.create(DefaultRunExecutor.getRunExecutorInstance(), settings).buildAndExecute()
        } catch (error: ExecutionException) {
            NotificationGroupManager.getInstance().getNotificationGroup("Elixir Expert")
                .createNotification("Could not run ExUnit", error.message ?: "Could not start the test run.", NotificationType.ERROR)
                .notify(project)
        }
    }
}
