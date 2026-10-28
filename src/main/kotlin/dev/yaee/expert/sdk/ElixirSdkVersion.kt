package dev.yaee.expert.sdk

object ElixirSdkVersion {
    private val versionPattern = Regex("\\bElixir\\s+([0-9]+\\.[0-9]+(?:\\.[0-9]+)?(?:[-+][A-Za-z0-9.-]+)?)")

    fun parse(output: String): String? = versionPattern.find(output)?.groupValues?.get(1)
}
