package dev.alquimista.expert.credo

object CredoDependency {
    fun available(mixDepsOutput: String): Boolean {
        val lines = mixDepsOutput.lines()
        val start = lines.indexOfFirst { it.matches(Regex("^\\* credo(?:\\s|$).*")) }
        if (start < 0) return false
        val details = lines.drop(start + 1).takeWhile { !it.startsWith("* ") }
        return details.none { it.contains("the dependency is not available", ignoreCase = true) }
    }
}
