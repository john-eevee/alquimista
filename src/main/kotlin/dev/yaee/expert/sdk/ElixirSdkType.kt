package dev.yaee.expert.sdk

import com.intellij.openapi.projectRoots.AdditionalDataConfigurable
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.projectRoots.SdkModificator
import com.intellij.openapi.projectRoots.SdkModel
import com.intellij.openapi.projectRoots.SdkAdditionalData
import com.intellij.openapi.projectRoots.SdkType
import org.jdom.Element
import java.io.File
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

    @Suppress("DEPRECATION")
    override fun suggestHomePath(): String? {
        val executableName = if (File.separatorChar == '\\') "elixir.bat" else "elixir"
        return System.getenv("PATH").orEmpty().split(File.pathSeparator)
            .asSequence()
            .map { File(it, executableName) }
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
        modificator.setVersionString(version)
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
