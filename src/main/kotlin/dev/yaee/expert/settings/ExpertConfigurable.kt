package dev.yaee.expert.settings

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.ProjectManager
import com.intellij.platform.lsp.api.LspClientManager
import dev.yaee.expert.lsp.ExpertLspServerSupportProvider
import com.intellij.ui.components.JBLabel
import java.awt.GridLayout
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JTextField

class ExpertConfigurable : Configurable {
    private val expertPath = JTextField()
    private var panel: JPanel? = null

    override fun getDisplayName(): String = "Elixir Expert"

    override fun createComponent(): JComponent {
        return JPanel(GridLayout(0, 1, 0, 6)).also {
            it.add(JBLabel("Expert executable (blank uses PATH)"))
            it.add(expertPath)
            panel = it
        }
    }

    override fun isModified(): Boolean = expertPath.text != ExpertSettings.getInstance().expertPath

    override fun apply() {
        if (ExpertSettings.getInstance().updateExpertPath(expertPath.text)) {
            ProjectManager.getInstance().openProjects
                .filterNot { it.isDisposed || it.isDefault }
                .forEach { project ->
                    LspClientManager.getInstance(project)
                        .stopAndRestartClientsIfNeeded(ExpertLspServerSupportProvider::class.java)
                }
        }
    }

    override fun reset() {
        expertPath.text = ExpertSettings.getInstance().expertPath
    }

    override fun disposeUIResources() {
        panel = null
    }
}
