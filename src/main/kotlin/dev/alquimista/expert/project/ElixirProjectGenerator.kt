package dev.alquimista.expert.project

import com.intellij.facet.ui.ValidationResult
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.application.WriteAction
import com.intellij.platform.DirectoryProjectGenerator
import com.intellij.platform.GeneratorPeerImpl
import com.intellij.platform.ProjectGeneratorPeer
import java.io.File
import javax.swing.Icon
import javax.swing.JPanel

class ElixirProjectGenerator : DirectoryProjectGenerator<Unit> {
    override fun getName(): String = "Elixir (Mix)"

    override fun getDescription(): String = "Create an Elixir application with Mix and ExUnit"

    override fun getLogo(): Icon? = null

    override fun createPeer(): ProjectGeneratorPeer<Unit> = GeneratorPeerImpl(Unit, JPanel())

    override fun validate(baseDirPath: String): ValidationResult {
        val destination = File(baseDirPath)
        return if (destination.exists() && destination.isDirectory && destination.listFiles()?.isNotEmpty() == true) {
            ValidationResult("The project directory must be empty")
        } else {
            ValidationResult.OK
        }
    }

    override fun generateProject(project: Project, baseDir: VirtualFile, settings: Unit, module: Module) {
        val files = MixProjectTemplate.files(baseDir.name)
        WriteAction.run<Throwable> {
            files.forEach { (relativePath, content) ->
                val parts = relativePath.split('/')
                var directory = baseDir
                parts.dropLast(1).forEach { name ->
                    directory = directory.findChild(name) ?: directory.createChildDirectory(this, name)
                }
                val file = directory.findChild(parts.last()) ?: directory.createChildData(this, parts.last())
                file.setBinaryContent(content.toByteArray(Charsets.UTF_8))
            }
        }
    }
}
