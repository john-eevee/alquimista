package dev.yaee.expert.lsp

internal object ElixirFileSupport {
    private val extensions = setOf("ex", "exs", "heex", "eex")

    fun supportsExtension(extension: String): Boolean = extension in extensions
}
