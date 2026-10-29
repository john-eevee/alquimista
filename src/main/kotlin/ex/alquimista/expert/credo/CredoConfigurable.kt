package ex.alquimista.expert.credo

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import javax.swing.JCheckBox
import javax.swing.JComponent

class CredoConfigurable(private val project: Project) : Configurable {
    private val enabled = JCheckBox("Enable Credo diagnostics for this project")

    override fun getDisplayName(): String = "Credo"
    override fun createComponent(): JComponent = enabled
    override fun isModified(): Boolean = enabled.isSelected != CredoSettings.getInstance(project).state.enabled
    override fun reset() {
        enabled.isSelected = CredoSettings.getInstance(project).state.enabled
    }

    override fun apply() {
        if (isModified) {
            CredoSettings.getInstance(project).state.enabled = enabled.isSelected
            project.getService(CredoRunner::class.java).clear()
            DaemonCodeAnalyzer.getInstance(project).restart("Credo settings changed")
        }
    }
}
