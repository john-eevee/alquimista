package dev.alquimista.expert.settings

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.project.ProjectManager
import com.intellij.platform.lsp.api.LspClientManager
import dev.alquimista.expert.lsp.ExpertLspServerSupportProvider
import java.awt.GridLayout
import javax.swing.JCheckBox
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTextField

class ExpertConfigurable(
    private val settingsProvider: () -> ExpertSettings = { ExpertSettings.getInstance() },
    private val restartClients: () -> Unit = ::restartExpertClients,
) : Configurable {
    private val expertPath = JTextField()
    private val minimumQueryLength = JTextField()
    private val logLevel = JComboBox(arrayOf("error", "warning", "info", "log"))
    private val fileLogLevel = JComboBox(arrayOf("debug", "info", "warning", "error"))
    private val elixirSourcePath = JTextField()
    private val elixirExecutablePath = JTextField()
    private val erlangExecutablePath = JTextField()
    private val autoFetchDependencies = JCheckBox("Automatically fetch missing dependencies")
    private val compileOnType = JCheckBox("Compile eligible documents while typing")

    override fun getDisplayName(): String = "Elixir Expert"

    override fun createComponent(): JComponent = JPanel(GridLayout(0, 1, 0, 6)).apply {
        add(JLabel("Expert executable (blank uses PATH)")); add(expertPath)
        add(JLabel("Minimum workspace symbol query length (0 allows empty queries)")); add(minimumQueryLength)
        add(JLabel("Editor log level")); add(logLevel)
        add(JLabel("File log level")); add(fileLogLevel)
        add(JLabel("Elixir source directory (optional)")); add(elixirSourcePath)
        add(JLabel("Elixir executable for Expert (optional)")); add(elixirExecutablePath)
        add(JLabel("Erlang executable for Expert (optional)")); add(erlangExecutablePath)
        add(autoFetchDependencies)
        add(compileOnType)
    }

    private fun currentInput(): ExpertSettings.State? {
        val length = minimumQueryLength.text.trim().toIntOrNull()?.takeIf { it >= 0 } ?: return null
        return ExpertSettings.State(
            expertPath = expertPath.text.trim(),
            minimumWorkspaceSymbolQueryLength = length,
            logLevel = logLevel.selectedItem as String,
            fileLogLevel = fileLogLevel.selectedItem as String,
            elixirSourcePath = elixirSourcePath.text.trim(),
            elixirExecutablePath = elixirExecutablePath.text.trim(),
            erlangExecutablePath = erlangExecutablePath.text.trim(),
            autoFetchDependencies = autoFetchDependencies.isSelected,
            compileOnType = compileOnType.isSelected,
        )
    }

    override fun isModified(): Boolean = currentInput() != settingsProvider().state

    override fun apply() {
        val input = currentInput() ?: throw ConfigurationException("Minimum workspace symbol query length must be a non-negative integer")
        val settings = settingsProvider()
        if (input == settings.state) return
        settings.loadState(input)
        restartClients()
    }

    override fun reset() {
        val state = settingsProvider().state
        expertPath.text = state.expertPath
        minimumQueryLength.text = state.minimumWorkspaceSymbolQueryLength.toString()
        logLevel.selectedItem = state.logLevel
        fileLogLevel.selectedItem = state.fileLogLevel
        elixirSourcePath.text = state.elixirSourcePath
        elixirExecutablePath.text = state.elixirExecutablePath
        erlangExecutablePath.text = state.erlangExecutablePath
        autoFetchDependencies.isSelected = state.autoFetchDependencies
        compileOnType.isSelected = state.compileOnType
    }
}

private fun restartExpertClients() {
    ProjectManager.getInstance().openProjects
        .filterNot { it.isDisposed || it.isDefault }
        .forEach { project ->
            LspClientManager.getInstance(project)
                .stopAndRestartClientsIfNeeded(ExpertLspServerSupportProvider::class.java)
        }
}
