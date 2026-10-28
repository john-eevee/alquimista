package dev.yaee.expert.lang

import com.intellij.openapi.application.PathManager
import com.intellij.openapi.diagnostic.Logger
import org.jetbrains.plugins.textmate.api.TextMateBundleProvider
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

class ElixirTextMateBundleProvider : TextMateBundleProvider {
    override fun getBundles(): List<TextMateBundleProvider.PluginBundle> = try {
        listOf(TextMateBundleProvider.PluginBundle("Elixir", extractBundle()))
    } catch (error: Exception) {
        LOG.warn("Could not load bundled Elixir TextMate grammars", error)
        emptyList()
    }

    private fun extractBundle(): Path {
        val root = Paths.get(PathManager.getSystemPath(), "yaee", "textmate")
        for (relativePath in RESOURCES) {
            val target = root.resolve(relativePath)
            Files.createDirectories(target.parent)
            val resource = javaClass.getResourceAsStream("/textmate/$relativePath")
                ?: error("Missing TextMate resource $relativePath")
            resource.use { Files.copy(it, target, StandardCopyOption.REPLACE_EXISTING) }
        }
        return root
    }

    companion object {
        private val LOG = Logger.getInstance(ElixirTextMateBundleProvider::class.java)
        private val RESOURCES = listOf(
            "package.json",
            "syntaxes/elixir.json",
            "syntaxes/eex.json",
            "syntaxes/html-eex.json",
        )
    }
}
