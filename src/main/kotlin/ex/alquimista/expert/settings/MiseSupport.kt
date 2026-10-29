package ex.alquimista.expert.settings

import com.intellij.execution.configurations.GeneralCommandLine
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

data class MiseCommand(val executable: String, val arguments: List<String>)

object MiseCommandResolver {
    fun resolve(miseExecutable: String?, executable: String, arguments: List<String>): MiseCommand =
        if (miseExecutable.isNullOrBlank()) {
            MiseCommand(executable, arguments)
        } else {
            MiseCommand(miseExecutable, listOf("exec", "--", executable) + arguments)
        }
}

object MiseSupport {
    fun commandLine(executable: String, arguments: List<String>): GeneralCommandLine {
        val command = MiseCommandResolver.resolve(findExecutable(), executable, arguments)
        return GeneralCommandLine(command.executable).withParameters(command.arguments)
    }

    fun findExecutable(): String? {
        val windows = isWindows
        val names = if (windows) listOf("mise.exe", "mise.cmd", "mise.bat") else listOf("mise")
        val configured = System.getenv("MISE_BIN")?.let(Paths::get)
        if (configured != null && isExecutable(configured)) return configured.toString()

        val pathDirectories = System.getenv("PATH").orEmpty().split(File.pathSeparator)
            .map(String::trim).filter(String::isNotEmpty).map(Paths::get)
        val home = Paths.get(System.getProperty("user.home"))
        val commonDirectories = if (windows) {
            listOf(home.resolve("scoop/shims"), home.resolve(".local/bin"))
        } else {
            listOf(
                home.resolve(".local/bin"),
                home.resolve(".cargo/bin"),
                Paths.get("/opt/homebrew/bin"),
                Paths.get("/usr/local/bin")
            )
        }
        return (pathDirectories + commonDirectories)
            .flatMap { directory -> names.map(directory::resolve) }
            .firstOrNull(::isExecutable)
            ?.toString()
    }

    private fun isExecutable(path: Path): Boolean = Files.isRegularFile(path) && (isWindows || Files.isExecutable(path))

    private val isWindows: Boolean
        get() = System.getProperty("os.name").contains("windows", ignoreCase = true)
}
