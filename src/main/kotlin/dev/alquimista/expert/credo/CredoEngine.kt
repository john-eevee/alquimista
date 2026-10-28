package dev.alquimista.expert.credo

class CredoEngine(private val run: (List<String>) -> String?) {
    private var dependencyKey: String? = null
    private var dependencyAvailable = false
    private val reports = mutableMapOf<String, Pair<String, List<CredoIssue>?>>()

    @Synchronized
    fun analyze(enabled: Boolean, configKey: String, file: String, revision: String): List<CredoIssue>? {
        if (!enabled) return null
        if (dependencyKey != configKey) {
            dependencyKey = configKey
            dependencyAvailable = run(listOf("deps"))?.let(CredoDependency::available) == true
            reports.clear()
        }
        if (!dependencyAvailable) return null
        reports[file]?.let { (cachedRevision, result) -> if (cachedRevision == revision) return result }
        val output = run(listOf("credo", "suggest", file, "--format", "json", "--mute-exit-status", "--no-color"))
        val result = output?.let(CredoReport::parse)
        reports[file] = revision to result
        return result
    }
}
