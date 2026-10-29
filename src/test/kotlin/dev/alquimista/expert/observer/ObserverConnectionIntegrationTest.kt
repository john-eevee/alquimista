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
        val node = "alquimista_test_${UUID.randomUUID().toString().replace("-", "").take(10)}"
        val cookie = "alquimista_test_cookie"
        val target = ProcessBuilder(erl!!.absolutePath, "+S", "2:2", "-sname", node, "-setcookie", cookie, "-noshell", "-eval", "timer:sleep(infinity).")
            .redirectErrorStream(true).start()
        try {
            val host = java.net.InetAddress.getLocalHost().hostName.substringBefore('.')
            var snapshot: ObserverSnapshot? = null
            var lastError: Exception? = null
            repeat(20) {
                if (snapshot == null) {
                    try { snapshot = ObserverConnection.snapshot(listOf(erl.absolutePath), "$node@$host", cookie, false) }
                    catch (error: IllegalStateException) { lastError = error; Thread.sleep(250) }
                }
            }
            assertNotNull("Could not connect to the test node: ${lastError?.message}", snapshot)
            assertEquals("$node@$host", snapshot!!.node)
            assertTrue(snapshot!!.memoryBytes > 0)
            assertTrue(snapshot!!.processCount > 0)
            assertTrue(snapshot!!.processes.isNotEmpty())
            assertThrows(IllegalStateException::class.java) {
                ObserverConnection.snapshot(listOf(erl.absolutePath), "$node@$host", "wrong_cookie", false)
            }
        } finally {
            target.destroyForcibly()
            target.waitFor(5, TimeUnit.SECONDS)
        }
    }
}
