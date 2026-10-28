package dev.yaee.expert.credo

import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Paths
import java.util.concurrent.TimeUnit

class CredoProcessIntegrationTest {
    @Test
    fun `real Mix and Credo produce a report through the engine`() {
        val fixture = System.getenv("YAEE_CREDO_FIXTURE")
        assumeTrue("Set YAEE_CREDO_FIXTURE to a Mix project with Credo installed", !fixture.isNullOrBlank())
        val root = Paths.get(fixture!!).toAbsolutePath()
        assertTrue(Files.isRegularFile(root.resolve("mix.exs")))
        val calls = mutableListOf<List<String>>()
        val engine = CredoEngine { args ->
            calls.add(args)
            val process = ProcessBuilder(listOf("mix") + args).directory(root.toFile()).redirectErrorStream(true).start()
            val output = process.inputStream.bufferedReader().readText()
            assertTrue(process.waitFor(120, TimeUnit.SECONDS))
            assertEquals(output, 0, process.exitValue())
            output
        }
        val report = engine.analyze(true, "fixture", "lib/sample.ex", "v1")
        assertNotNull(report)
        assertTrue("Expected a real Credo finding", report!!.any { it.filename.endsWith("lib/sample.ex") })
        engine.analyze(true, "fixture", "lib/sample.ex", "v1")
        assertEquals(2, calls.size)
    }
}
