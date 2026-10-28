package dev.yaee.expert.credo

import java.nio.file.Path
import java.nio.file.Paths

object CredoDiagnostics {
    fun matchesFile(root: Path, file: Path, reported: String): Boolean = runCatching {
        root.resolve(Paths.get(reported)).toAbsolutePath().normalize() == file.toAbsolutePath().normalize()
    }.getOrDefault(false)

    fun message(issue: CredoIssue): String = buildString {
        append("Credo: ").append(issue.message)
        if (issue.check.isNotBlank()) append(" (").append(issue.check).append(')')
    }
}
