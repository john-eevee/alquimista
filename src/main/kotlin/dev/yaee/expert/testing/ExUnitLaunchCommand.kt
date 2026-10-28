package dev.yaee.expert.testing

object ExUnitLaunchCommand {
    fun arguments(formatter: String, path: String?, line: Int?, module: String? = null): List<String> {
        require(formatter.isNotBlank()) { "The ExUnit formatter path is required" }
        require(path == null || path.isNotBlank()) { "The test file path cannot be blank" }
        require(line == null || (path != null && line > 0)) { "A test line requires a file and must be positive" }
        require(module == null || (path != null && line == null && module.matches(Regex("[A-Z][A-Za-z0-9_.]*")))) {
            "A module selection requires a file and a valid module name"
        }
        val target = if (path == null) emptyList() else listOf(if (line == null) path else "$path:$line")
        val filter = if (module == null) emptyList() else listOf("--only", "module:$module")
        return listOf("run", "--no-start", "--no-compile", "-r", formatter,
            "-e", "Mix.Task.run(\"test\", System.argv())", "--", "--formatter", "Yaee.ExUnitFormatter") + filter + target
    }
}
