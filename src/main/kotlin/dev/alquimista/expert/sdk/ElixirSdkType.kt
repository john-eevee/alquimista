package dev.alquimista.expert.sdk

import com.intellij.openapi.projectRoots.*
import org.jdom.Element
import java.io.File
import java.nio.file.Paths
import java.util.concurrent.TimeUnit

class ElixirSdkType : SdkType("ELIXIR_SDK") {
    override fun getPresentableName(): String = "Elixir SDK"

    override fun isValidSdkHome(path: String): Boolean = executable(path)?.let { it.isFile && it.canExecute() } == true

    override fun suggestSdkName(currentSdkName: String?, sdkHome: String): String =
        currentSdkName?.takeIf(String::isNotBlank) ?: "Elixir ${getVersionString(sdkHome) ?: File(sdkHome).name}"

    override fun createAdditionalDataConfigurable(
        sdkModel: SdkModel,
        sdkModificator: SdkModificator,
    ): AdditionalDataConfigurable? = null

    override fun saveAdditionalData(additionalData: SdkAdditionalData, additional: Element) = Unit

    @Suppress("OVERRIDE_DEPRECATION")
    override fun suggestHomePath(): String? {
        val executableName = if (File.separatorChar == '\\') "elixir.bat" else "elixir"
        val fromPath = System.getenv("PATH").orEmpty().split(File.pathSeparator)
            .asSequence()
            .map { File(it, executableName) }
            .firstOrNull { it.isFile && it.canExecute() }
            ?.absolutePath
        if (fromPath != null) return fromPath

        val home = Paths.get(System.getProperty("user.home"))
        val installRoots = listOf(
            home.resolve(".local/share/mise/installs/elixir"),
            home.resolve(".asdf/installs/elixir"),
        )
        return installRoots.asSequence()
            .map { it.toFile() }
            .flatMap { it.listFiles()?.asSequence() ?: emptySequence() }
            .map { File(it, "bin/$executableName") }
            .firstOrNull { it.isFile && it.canExecute() }
            ?.absolutePath
    }

    override fun getVersionString(sdkHome: String): String? {
        val executable = executable(sdkHome)?.takeIf { it.isFile && it.canExecute() } ?: return null
        return runCatching {
            val process = ProcessBuilder(executable.absolutePath, "--version").redirectErrorStream(true).start()
            if (!process.waitFor(5, TimeUnit.SECONDS)) {
                process.destroyForcibly()
                return null
            }
            ElixirSdkVersion.parse(process.inputStream.bufferedReader().use { it.readText() })
        }.getOrNull()
    }

    override fun setupSdkPaths(sdk: Sdk, sdkModel: SdkModel): Boolean {
        val home = sdk.homePath ?: return false
        val version = getVersionString(home) ?: return false
        val modificator = sdk.sdkModificator
        modificator.versionString = version
        modificator.commitChanges()
        return true
    }

    private fun executable(path: String): File? {
        val home = File(path)
        if (home.isFile) return home
        val candidates = if (File.separatorChar == '\\') {
            listOf("bin/elixir.bat", "bin/elixir.exe", "elixir.bat")
        } else {
            listOf("bin/elixir", "elixir")
        }
        return candidates.asSequence().map(home::resolve).firstOrNull { it.isFile }
    }
}
