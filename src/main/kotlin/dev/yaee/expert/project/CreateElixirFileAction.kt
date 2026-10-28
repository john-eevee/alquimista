package dev.yaee.expert.project

import com.intellij.icons.AllIcons
import com.intellij.ide.actions.CreateFileFromTemplateAction
import com.intellij.ide.actions.CreateFileFromTemplateDialog
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDirectory

internal enum class ElixirFileKind(val label: String, val templateName: String, val extension: String) {
    SOURCE("Elixir source (.ex)", "Elixir Source", "ex"),
    SCRIPT("Elixir script (.exs)", "Elixir Script", "exs"),
    EEX("EEx template (.eex)", "Elixir EEx", "eex"),
    HEEX("HEEx template (.heex)", "Elixir HEEx", "heex"),
}

class CreateElixirFileAction : CreateFileFromTemplateAction() {
    override fun buildDialog(project: Project, directory: PsiDirectory, builder: CreateFileFromTemplateDialog.Builder) {
        builder.setTitle("New Elixir File")
        ElixirFileKind.entries.forEach { builder.addKind(it.label, AllIcons.FileTypes.Text, it.templateName) }
    }

    override fun getActionName(directory: PsiDirectory, newName: String, templateName: String): String =
        "Create Elixir File: $newName"
}
