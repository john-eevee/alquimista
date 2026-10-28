package dev.alquimista.expert.lsp

import dev.alquimista.expert.project.MixProjectTemplate
import dev.alquimista.expert.settings.ExpertConfiguration
import dev.alquimista.expert.settings.ExpertSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.TimeUnit

class ExpertLiveDiagnosticsIntegrationTest {
    @Test fun `Expert accepts all four document types and reports unsaved diagnostics`() {
        val executable = System.getenv("ALQUIMISTA_EXPERT_TEST_EXECUTABLE")
        assumeTrue("Set ALQUIMISTA_EXPERT_TEST_EXECUTABLE to an Expert 0.2 executable", !executable.isNullOrBlank())
        val root = Files.createTempDirectory(Files.createDirectories(Path.of("build", "tmp")), "expert-live-").toAbsolutePath()
        MixProjectTemplate.files("expert_live_fixture").forEach { (path, content) ->
            val target = root.resolve(path)
            Files.createDirectories(target.parent)
            Files.writeString(target, content)
        }
        val script = Path.of("src/test/resources/expert/live_diagnostics.py").toAbsolutePath()
        val output = root.resolve("protocol-test.log")
        val ids = """{"ex":"elixir","exs":"elixir","eex":"eex","heex":"heex"}"""
        val settings = ExpertConfiguration.toJson(ExpertSettings.State(minimumWorkspaceSymbolQueryLength = 0)).toString()
        val process = ProcessBuilder("python3", script.toString(), executable!!, root.toString(), ids, settings)
            .redirectErrorStream(true).redirectOutput(output.toFile()).start()
        val completed = process.waitFor(150, TimeUnit.SECONDS)
        if (!completed) process.destroyForcibly()
        assertTrue("Expert protocol test timed out", completed)
        val log = Files.readString(output)
        assertEquals(log, 0, process.exitValue())
        assertTrue(log, log.contains("eex, ex, exs"))
        assertTrue(log, log.contains("empty workspace symbol query"))
    }
}
