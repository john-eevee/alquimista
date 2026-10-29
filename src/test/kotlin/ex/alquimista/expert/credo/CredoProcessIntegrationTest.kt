package ex.alquimista.expert.credo

import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Paths
import java.io.File
import java.util.concurrent.TimeUnit

class CredoProcessIntegrationTest {
    @Test
    fun `real Mix and Credo produce a report through the engine`() {
        val mix = System.getenv("PATH").orEmpty().split(File.pathSeparator).map { File(it, "mix") }
            .firstOrNull { it.isFile && it.canExecute() }
        assumeTrue("Mix is required for the Credo integration test", mix != null)
        val fixture = Paths.get("src/test/fixtures/credo").toAbsolutePath()
        assertTrue("Bundled Credo Mix fixture is missing", Files.isRegularFile(fixture.resolve("mix.exs")))
        assertTrue("Credo fixture lockfile is missing", Files.isRegularFile(fixture.resolve("mix.lock")))
        val root = Paths.get("build", "test-fixtures", "credo").toAbsolutePath()
        Files.createDirectories(root.resolve("lib"))
        for (file in listOf("mix.exs", "mix.lock", "lib/sample.ex")) {
            Files.copy(fixture.resolve(file), root.resolve(file), java.nio.file.StandardCopyOption.REPLACE_EXISTING)
        }
        fun runMix(args: List<String>, input: String? = null): String {
            val outputFile = root.resolve("process-output.log")
            val process = ProcessBuilder(listOf(mix!!.absolutePath) + args).directory(root.toFile())
                .redirectErrorStream(true).redirectOutput(outputFile.toFile())
                .apply { environment()["MIX_ENV"] = "test"; environment()["ERL_FLAGS"] = "+S 2:2" }.start()
            val writer = input?.let { text ->
                Thread { process.outputStream.bufferedWriter(Charsets.UTF_8).use { it.write(text) } }.apply { start() }
            }
            val completed = process.waitFor(120, TimeUnit.SECONDS)
            writer?.join(1_000)
            if (!completed) process.destroyForcibly()
            assertTrue("Mix command timed out: $args", completed)
            val output = Files.readString(outputFile)
            assertEquals(output, 0, process.exitValue())
            return output
        }
        val installedLock = root.resolve(".installed-lock")
        val lockContents = Files.readString(root.resolve("mix.lock"))
        if (!Files.isRegularFile(root.resolve("deps/credo/mix.exs")) ||
            !Files.isRegularFile(installedLock) || Files.readString(installedLock) != lockContents
        ) {
            runMix(listOf("deps.get", "--check-locked"))
            Files.writeString(installedLock, lockContents)
        }
        val calls = mutableListOf<List<String>>()
        val engine = CredoEngine { args ->
            calls.add(args)
            runMix(args)
        }
        val report = engine.analyze(true, "fixture", "lib/sample.ex", "v1")
        assertNotNull(report)
        assertTrue("Expected a real Credo finding", report!!.any { it.filename.endsWith("lib/sample.ex") })
        engine.analyze(true, "fixture", "lib/sample.ex", "v1")
        assertEquals(2, calls.size)
        val diskText = Files.readString(root.resolve("lib/sample.ex"))
        val unsaved = "defmodule Fresh do\n  def run, do: IO.inspect(42)\nend\n"
        val liveEngine = CredoEngine({ args -> runMix(args) }, { args, input -> runMix(args, input) })
        val liveReport = liveEngine.analyze(true, "fixture", "lib/sample.ex", unsaved, unsaved)
        assertNotNull(liveReport)
        assertTrue("Expected the unsaved IO.inspect finding", liveReport!!.any {
            it.filename.endsWith("lib/sample.ex") && it.check.contains("IoInspect")
        })
        assertEquals("Analyzing the buffer must not change the file on disk", diskText, Files.readString(root.resolve("lib/sample.ex")))
    }
}
