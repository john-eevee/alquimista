package dev.yaee.expert.lang

import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.extensions.PluginId
import org.jetbrains.plugins.textmate.api.TextMateBundleProvider

class ElixirTextMateBundleProvider : TextMateBundleProvider {
    override fun getBundles(): List<TextMateBundleProvider.PluginBundle> {
        val plugin = PluginManagerCore.getPlugin(PluginId.getId("dev.yaee.expert")) ?: return emptyList()
        return listOf(TextMateBundleProvider.PluginBundle("Elixir", plugin.pluginPath.resolve("textmate")))
    }
}
