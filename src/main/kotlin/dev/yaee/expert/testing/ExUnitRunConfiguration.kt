package dev.yaee.expert.testing

import com.intellij.execution.ExecutionException
import com.intellij.execution.Executor
import com.intellij.execution.configurations.CommandLineState
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationTypeBase
import com.intellij.execution.configurations.RunConfigurationBase
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.process.OSProcessHandler
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.testframework.sm.FileUrlProvider
import com.intellij.execution.testframework.sm.SMTestRunnerConnectionUtil
import com.intellij.execution.testframework.sm.runner.SMTRunnerConsoleProperties
import com.intellij.execution.testframework.sm.runner.SMTestLocator
import com.intellij.execution.testframework.actions.AbstractRerunFailedTestsAction
import com.intellij.execution.ui.ConsoleView
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import dev.yaee.expert.lang.ElixirIcons
import dev.yaee.expert.sdk.MixSdkCommand
import org.jdom.Element
import java.awt.GridLayout
import java.nio.file.Files
import java.nio.file.Paths
import java.security.MessageDigest
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTextField

class ExUnitConfigurationType : ConfigurationTypeBase("YaeeExUnit", "ExUnit", "Run Elixir ExUnit tests", ElixirIcons.FILE) {
    init { addFactory(ExUnitConfigurationFactory(this)) }
}

class ExUnitConfigurationFactory(type: ExUnitConfigurationType) : ConfigurationFactory(type) {
    override fun getId(): String = "ExUnit"
    override fun createTemplateConfiguration(project: Project) = ExUnitRunConfiguration(project, this, "ExUnit")
}

class ExUnitRunConfiguration(project: Project, factory: ConfigurationFactory, name: String) :
    RunConfigurationBase<Any>(project, factory, name) {
    var selection = ExUnitRunSelection(null, null, null)

    override fun getConfigurationEditor(): SettingsEditor<out com.intellij.execution.configurations.RunConfiguration> = ExUnitSettingsEditor()

    override fun readExternal(element: Element) {
        super.readExternal(element)
        selection = ExUnitRunSelection.read(element)
    }

    override fun writeExternal(element: Element) {
        super.writeExternal(element)
        selection.write(element)
    }

    override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState =
        ExUnitRunState(environment, this)
}

private class ExUnitSettingsEditor : SettingsEditor<ExUnitRunConfiguration>() {
    private val path = JTextField()
    private val line = JTextField()
    private val module = JTextField()

    override fun createEditor(): JComponent = JPanel(GridLayout(0, 1)).apply {
        add(JLabel("Test file (blank runs all tests)")); add(path)
        add(JLabel("Line (optional)")); add(line)
        add(JLabel("Module (optional)")); add(module)
    }

    override fun resetEditorFrom(configuration: ExUnitRunConfiguration) {
        path.text = configuration.selection.path.orEmpty()
        line.text = configuration.selection.line?.toString().orEmpty()
        module.text = configuration.selection.module.orEmpty()
    }

    override fun applyEditorTo(configuration: ExUnitRunConfiguration) {
        val lineText = line.text.trim()
        if (lineText.isNotEmpty() && (lineText.toIntOrNull() ?: 0) <= 0) {
            throw com.intellij.openapi.options.ConfigurationException("Line must be a positive number")
        }
        configuration.selection = ExUnitRunSelection(path.text.trim().ifEmpty { null }, lineText.toIntOrNull(), module.text.trim().ifEmpty { null })
    }
}

private class ExUnitRunState(
    environment: ExecutionEnvironment,
    private val configuration: ExUnitRunConfiguration,
    private val failedOnly: Boolean = false,
) : CommandLineState(environment) {
    override fun startProcess(): ProcessHandler {
        val project = configuration.project
        val root = project.basePath ?: throw ExecutionException("The project has no base directory")
        val formatter = extractFormatter()
        val selection = configuration.selection
        val args = try { ExUnitLaunchCommand.arguments(formatter, selection.path, selection.line, selection.module, failedOnly) }
        catch (error: IllegalArgumentException) { throw ExecutionException(error.message ?: "Invalid ExUnit selection", error) }
        val command = MixSdkCommand.commandLine(project, args).withWorkDirectory(root).withEnvironment("MIX_ENV", "test")
        return OSProcessHandler(command)
    }

    override fun createConsole(executor: Executor): ConsoleView {
        val runConfiguration = configuration
        val properties = object : SMTRunnerConsoleProperties(runConfiguration, "ExUnit", executor) {
            override fun getTestLocator(): SMTestLocator = FileUrlProvider.INSTANCE
            override fun createRerunFailedTestsAction(consoleView: ConsoleView): AbstractRerunFailedTestsAction {
                val action = object : AbstractRerunFailedTestsAction(consoleView) {
                    override fun getRunProfile(environment: ExecutionEnvironment): MyRunProfile =
                        object : MyRunProfile(runConfiguration) {
                            override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState =
                                ExUnitRunState(environment, runConfiguration, failedOnly = true)
                        }
                }
                action.init(this)
                return action
            }
        }
        val filter = ExUnitStacktraceFilter(configuration.project)
        properties.addStackTraceFilter(filter)
        return SMTestRunnerConnectionUtil.createConsole(properties).also { it.addMessageFilter(filter) }
    }

    private fun extractFormatter(): String {
        val bytes = javaClass.getResourceAsStream("/exunit/yaee_formatter.exs")?.use { it.readBytes() }
            ?: throw ExecutionException("The ExUnit formatter is missing")
        val hash = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
        val dir = Paths.get(PathManager.getSystemPath(), "yaee", "exunit")
        try {
            Files.createDirectories(dir)
            val path = dir.resolve("formatter-$hash.exs")
            if (!Files.exists(path)) {
                try { Files.write(path, bytes, java.nio.file.StandardOpenOption.CREATE_NEW) }
                catch (_: java.nio.file.FileAlreadyExistsException) { }
            }
            return path.toString()
        } catch (error: java.io.IOException) {
            throw ExecutionException("Could not prepare the ExUnit formatter", error)
        }
    }
}
