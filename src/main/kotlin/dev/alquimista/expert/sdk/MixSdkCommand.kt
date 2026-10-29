package dev.alquimista.expert.sdk

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.util.Computable
import dev.alquimista.expert.settings.MiseSupport
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

data class MixSdkInvocation(val executable: String, val arguments: List<String>, val path: String)

object MixSdkCommand {
    fun findMix(home: Path, windows: Boolean): Path? {
        val directory = if (Files.isRegularFile(home)) home.parent ?: return null else home
        val names = if (windows) listOf("mix.bat", "mix.cmd", "mix.exe") else listOf("mix")
        return listOf(directory.resolve("bin"), directory).asSequence()
            .flatMap { dir -> names.asSequence().map(dir::resolve) }
            .firstOrNull { Files.isRegularFile(it) }
    }

    fun commandLine(project: Project, arguments: List<String>): GeneralCommandLine {
        val sdk = ApplicationManager.getApplication().runReadAction(Computable {
            ProjectRootManager.getInstance(project).projectSdk?.takeIf { it.sdkType is ElixirSdkType }
        })
        if (sdk == null) return MiseSupport.commandLine("mix", arguments)
        val home = sdk.homePath ?: error("The selected Elixir SDK has no home path")
        val invocation = forSdk(
            home,
            arguments,
            System.getenv("PATH").orEmpty(),
            System.getProperty("os.name").contains("windows", true)
        )
        return GeneralCommandLine(invocation.executable).withParameters(invocation.arguments)
            .withEnvironment("PATH", invocation.path)
    }

    fun forSdk(home: String, arguments: List<String>, inheritedPath: String, windows: Boolean): MixSdkInvocation {
        val mix = findMix(Paths.get(home), windows)
            ?: error("The selected Elixir SDK does not contain mix: $home")
        val path = listOf(mix.parent.toString(), inheritedPath).joinToString(File.pathSeparator)
        return MixSdkInvocation(mix.toString(), arguments, path)
    }
}
