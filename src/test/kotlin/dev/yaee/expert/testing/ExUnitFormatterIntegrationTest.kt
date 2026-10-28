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
    fun `describe line and module filters run only selected tests`() {
        val mix = System.getenv("PATH").orEmpty().split(File.pathSeparator).map { File(it, "mix") }
            .firstOrNull { it.isFile && it.canExecute() }
        assumeTrue("Mix is required", mix != null)
        val root = Files.createTempDirectory(Files.createDirectories(Path.of("build", "tmp")), "exunit-groups-").toAbsolutePath()
        MixProjectTemplate.files("runner_fixture").forEach { (path, content) ->
            val file = root.resolve(path)
            Files.createDirectories(file.parent)
            Files.writeString(file, content)
        }
        Files.writeString(root.resolve("test/runner_fixture_test.exs"), """defmodule FirstTest do
  use ExUnit.Case
  describe "group" do
    test "first", do: assert(true)
    test "second", do: assert(true)
  end
  test "outside", do: assert(true)
end
defmodule OtherTest do
  use ExUnit.Case
  test "other module", do: assert(true)
end
""")
        val formatter = root.resolve("formatter.exs")
        javaClass.getResourceAsStream("/exunit/yaee_formatter.exs")!!.use { Files.copy(it, formatter) }
        fun run(selection: ExUnitRunSelection, outputName: String): String {
            val outputFile = root.resolve(outputName)
            val args = ExUnitLaunchCommand.arguments(formatter.toString(), selection.path, selection.line, selection.module)
            val process = ProcessBuilder(listOf(mix!!.absolutePath) + args).directory(root.toFile())
                .redirectErrorStream(true).redirectOutput(outputFile.toFile())
                .apply { environment()["MIX_ENV"] = "test"; environment()["ERL_FLAGS"] = "+S 2:2" }.start()
            val finished = process.waitFor(120, TimeUnit.SECONDS)
            if (!finished) process.destroyForcibly()
            assertTrue("Mix timed out", finished)
            val output = Files.readString(outputFile)
            assertEquals(output, 0, process.exitValue())
            return output
        }
        val path = "test/runner_fixture_test.exs"
        val group = run(ExUnitRunSelection(path, 3, null), "group-output.txt")
        assertTrue(group, group.contains("test group first"))
        assertTrue(group, group.contains("test group second"))
        assertFalse(group, group.contains("test outside"))
        assertFalse(group, group.contains("test other module"))
        val module = run(ExUnitRunSelection(path, null, "FirstTest"), "module-output.txt")
        assertTrue(module, module.contains("test outside"))
        assertFalse(module, module.contains("test other module"))
    }

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
