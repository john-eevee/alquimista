package dev.alquimista.expert.observer

import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.util.UUID
import java.util.concurrent.TimeUnit

class ObserverConnectionIntegrationTest {
    @Test fun `reads a real distributed node and rejects a wrong cookie`() {
        val erl = System.getenv("PATH").orEmpty().split(java.io.File.pathSeparator)
            .map { java.io.File(it, "erl") }.firstOrNull { it.isFile && it.canExecute() }
        assumeTrue("Erlang is required", erl != null)
        val executable = erl ?: return
        val node = "alquimista_test_${UUID.randomUUID().toString().replace("-", "").take(10)}"
        val cookie = "alquimista_test_cookie"
        fun startTarget() = ProcessBuilder(executable.absolutePath, "+S", "2:2", "-sname", node, "-setcookie", cookie, "-noshell", "-eval", "timer:sleep(infinity).")
            .redirectErrorStream(true).start()
        var target = startTarget()
        try {
            val host = java.net.InetAddress.getLocalHost().hostName.substringBefore('.')
            var snapshot: ObserverSnapshot? = null
            var lastError: Exception? = null
            repeat(20) {
                if (snapshot == null) {
                    try { snapshot = ObserverConnection.open(listOf(executable.absolutePath), "$node@$host", cookie, false).use { it.snapshot() } }
                    catch (error: IllegalStateException) { lastError = error; Thread.sleep(250) }
                }
            }
            assertNotNull("Could not connect to the test node: ${lastError?.message}", snapshot)
            val initial = requireNotNull(snapshot)
            assertEquals("$node@$host", initial.node)
            assertTrue(initial.memoryBytes > 0)
            assertTrue(initial.processCount > 0)
            assertTrue(initial.processes.isNotEmpty())
            assertTrue("Expected real memory metrics", initial.metrics.processMemory > 0)
            assertTrue("Expected real ETS memory metrics", initial.metrics.etsMemory > 0)
            val session = ObserverConnection.open(listOf(executable.absolutePath), "$node@$host", cookie, false)
            val processId = session.processId
            try {
                val first = session.snapshot()
                val second = session.snapshot()
                assertEquals(first.node, second.node)
                assertEquals(processId, session.processId)
                assertTrue(session.isAlive)
                target.destroyForcibly()
                assertTrue(target.waitFor(5, TimeUnit.SECONDS))
                assertThrows(IllegalStateException::class.java) { session.snapshot() }
                assertTrue("The observer should remain available while the target is down", session.isAlive)
                target = startTarget()
                var reconnected = false
                repeat(20) {
                    if (!reconnected) {
                        try { session.snapshot(); reconnected = true }
                        catch (_: IllegalStateException) { Thread.sleep(250) }
                    }
                }
                assertTrue("The same observer should reconnect after the target restarts", reconnected)
                assertEquals(processId, session.processId)
            } finally { session.close() }
            repeat(50) { if (session.isAlive) Thread.sleep(10) }
            assertFalse(session.isAlive)
            assertThrows(IllegalStateException::class.java) {
                ObserverConnection.open(listOf(executable.absolutePath), "$node@$host", "wrong_cookie", false).use { it.snapshot() }
            }
        } finally {
            target.destroyForcibly()
            target.waitFor(5, TimeUnit.SECONDS)
        }
    }
}
