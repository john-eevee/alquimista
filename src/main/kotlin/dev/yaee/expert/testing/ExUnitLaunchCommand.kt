package dev.yaee.expert.testing

object ExUnitLaunchCommand {
    fun arguments(formatter: String, path: String?, line: Int?): List<String> {
        require(formatter.isNotBlank()) { "The ExUnit formatter path is required" }
        require(path == null || path.isNotBlank()) { "The test file path cannot be blank" }
        require(line == null || (path != null && line > 0)) { "A test line requires a file and must be positive" }
        val target = if (path == null) emptyList() else listOf(if (line == null) path else "$path:$line")
        return listOf("run", "--no-start", "--no-compile", "-r", formatter,
            "-e", "Mix.Task.run(\"test\", System.argv())", "--", "--formatter", "Yaee.ExUnitFormatter") + target
    }
}
