package ex.alquimista.expert.lang

import com.intellij.ide.FileIconProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.IconLoader
import com.intellij.openapi.vfs.VirtualFile
import java.util.*
import javax.swing.Icon

object ElixirIcons {
    @JvmField
    val FILE: Icon = IconLoader.getIcon("/icons/elixir.svg", ElixirIcons::class.java)
}

class ElixirFileIconProvider : FileIconProvider {
    override fun getIcon(file: VirtualFile, flags: Int, project: Project?): Icon? {
        if (file.isDirectory) return null
        return if (file.extension?.lowercase(Locale.ROOT) in EXTENSIONS) ElixirIcons.FILE else null
    }

    companion object {
        private val EXTENSIONS = setOf("ex", "exs", "eex", "heex")
    }
}
