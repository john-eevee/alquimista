package dev.yaee.expert.settings

object ExpertReleaseAssets {
    fun assetName(osName: String, architecture: String): String? {
        val os = when {
            osName.contains("linux", ignoreCase = true) -> "linux"
            osName.contains("mac", ignoreCase = true) || osName.contains("darwin", ignoreCase = true) -> "darwin"
            osName.contains("windows", ignoreCase = true) -> "windows"
            else -> return null
        }
        val arch = when (architecture.lowercase()) {
            "aarch64", "arm64" -> "arm64"
            "amd64", "x86_64", "x64" -> "amd64"
            else -> return null
        }
        if (os == "windows" && arch != "amd64") return null
        return "expert_${os}_${arch}" + if (os == "windows") ".exe" else ""
    }

    fun checksumFor(assetName: String, manifest: String): String? = manifest.lineSequence()
        .map(String::trim)
        .mapNotNull { line ->
            val fields = line.split(Regex("\\s+"), limit = 2)
            if (fields.size == 2 && fields[1].substringAfterLast('/') == assetName) fields[0] else null
        }
        .firstOrNull { it.matches(Regex("[a-fA-F0-9]{64}")) }
}
