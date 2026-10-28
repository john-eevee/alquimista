package dev.yaee.expert.testing

import dev.yaee.expert.project.MixProjectTemplate
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.TimeUnit

class ExUnitFormatterIntegrationTest {
    @Test
    fun `reports passing failing and skipped tests with escaped names and source locations`() {
        val mix = System.getenv("PATH").orEmpty().split(File.pathSeparator).map { File(it, "mix") }
            .firstOrNull { it.isFile && it.canExecute() }
        assumeTrue("Mix is required", mix != null)
        val root = Files.createTempDirectory(Files.createDirectories(Path.of("build", "tmp")), "exunit-runner-").toAbsolutePath()
        MixProjectTemplate.files("runner_fixture").forEach { (path, content) ->
            val file = root.resolve(path)
            Files.createDirectories(file.parent)
            Files.writeString(file, content)
        }
        Files.writeString(root.resolve("test/runner_fixture_test.exs"), """
            defmodule RunnerFixtureTest do
              use ExUnit.Case, async: true
              test "passes | and ' quote" do
                assert true
              end
              test "fails" do
                assert 1 == 2
              end
              @tag :skip
              test "skipped" do
                flunk("must not run")
              end
            end
        """.trimIndent())
        val formatter = javaClass.getResourceAsStream("/exunit/yaee_formatter.exs")
            ?: throw AssertionError("ExUnit formatter is missing")
        val formatterFile = root.resolve("yaee_formatter.exs")
        formatter.use { Files.copy(it, formatterFile) }
        val outputFile = root.resolve("test-output.txt")
        val process = ProcessBuilder(mix!!.absolutePath, "run", "--no-start", "--no-compile", "-r", formatterFile.toString(),
            "-e", "Mix.Task.run(\"test\", System.argv())", "--", "--formatter", "Yaee.ExUnitFormatter")
            .directory(root.toFile()).redirectErrorStream(true).redirectOutput(outputFile.toFile())
            .apply { environment()["MIX_ENV"] = "test"; environment()["ERL_FLAGS"] = "+S 2:2" }.start()
        val finished = process.waitFor(120, TimeUnit.SECONDS)
        if (!finished) process.destroyForcibly()
        assertTrue("Mix timed out", finished)
        val output = Files.readString(outputFile)
        assertNotEquals("A failing test should make the process fail:\n$output", 0, process.exitValue())
        for (expected in listOf("##teamcity[testSuiteStarted", "##teamcity[testStarted", "##teamcity[testFailed",
            "##teamcity[testIgnored", "##teamcity[testFinished", "passes || and |' quote", "locationHint=", "runner_fixture_test.exs")) {
            assertTrue("Missing $expected in:\n$output", output.contains(expected))
        }
    }
}
