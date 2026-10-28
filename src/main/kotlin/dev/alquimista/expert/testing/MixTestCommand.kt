package dev.alquimista.expert.testing

object MixTestCommand {
    fun arguments(relativePath: String, line: Int? = null): List<String> {
        require(relativePath.isNotBlank()) { "A test file path is required" }
        require(line == null || line > 0) { "A test line must be positive" }
        val target = if (line == null) relativePath else "$relativePath:$line"
        return listOf("test", target)
    }
}
