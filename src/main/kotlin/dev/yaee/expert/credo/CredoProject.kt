package dev.yaee.expert.credo

import java.nio.file.Files
import java.nio.file.Path

object CredoProject {
    fun isConfigurationChange(projectRoot: Path, changed: Path): Boolean {
        val root = projectRoot.toAbsolutePath().normalize()
        val path = changed.toAbsolutePath().normalize()
        if (!path.startsWith(root)) return false
        val name = path.fileName?.toString() ?: return false
        return name in setOf("mix.exs", "mix.lock", ".credo.exs", "deps") ||
            (name == "credo" && path.parent?.fileName?.toString() == "deps")
    }

    fun findMixRoot(projectRoot: Path, file: Path): Path? {
        val root = projectRoot.toAbsolutePath().normalize()
        val target = file.toAbsolutePath().normalize()
        if (!target.startsWith(root)) return null
        var directory = target.parent
        while (directory != null && directory.startsWith(root)) {
            if (Files.isRegularFile(directory.resolve("mix.exs"))) return directory
            directory = directory.parent
        }
        return null
    }

    fun fingerprint(root: Path, mixRoot: Path, sdk: String): String {
        val paths = listOf(root, mixRoot).distinct().flatMap { dir ->
            listOf("mix.exs", "mix.lock", ".credo.exs", "deps", "deps/credo", "deps/credo/mix.exs")
                .map(dir::resolve)
        }
        return buildString {
            append(sdk)
            paths.forEach { path ->
                append('|').append(path).append(':')
                append(runCatching { Files.getLastModifiedTime(path).toMillis() }.getOrDefault(-1))
                append(':').append(runCatching { if (Files.isRegularFile(path)) Files.size(path) else -1 }.getOrDefault(-1))
            }
        }
    }
}
