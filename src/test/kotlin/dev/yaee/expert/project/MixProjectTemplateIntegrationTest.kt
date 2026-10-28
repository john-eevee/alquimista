package dev.yaee.expert.project

import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.Comparator
import java.util.concurrent.TimeUnit

class MixProjectTemplateIntegrationTest {
    @Test
    fun `generated project compiles and runs its ExUnit test`() {
        val mix = System.getenv("PATH").orEmpty().split(File.pathSeparator)
            .asSequence()
            .map { File(it, if (File.separatorChar == '\\') "mix.bat" else "mix") }
            .firstOrNull { it.isFile && it.canExecute() }
        assumeTrue("Mix is required for the generated-project integration check", mix != null)

        val workspace = Path.of("build", "tmp")
        Files.createDirectories(workspace)
        val projectDir = Files.createTempDirectory(workspace, "mix-wizard-")
        try {
            MixProjectTemplate.files("wizard-smoke").forEach { (relativePath, content) ->
                val file = projectDir.resolve(relativePath)
                Files.createDirectories(file.parent)
                Files.writeString(file, content)
            }

            val process = ProcessBuilder(mix!!.absolutePath, "test")
                .directory(projectDir.toFile())
                .redirectErrorStream(true)
                .start()
            val finished = process.waitFor(120, TimeUnit.SECONDS)
            if (!finished) process.destroyForcibly()
            val output = process.inputStream.bufferedReader().use { it.readText() }

            assertEquals("Mix test output:\n$output", 0, process.exitValue())
        } finally {
            Files.walk(projectDir).use { paths ->
                paths.sorted(Comparator.reverseOrder()).forEach(Files::deleteIfExists)
            }
        }
    }
}
