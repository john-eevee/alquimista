package ex.alquimista.expert.observer

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
        fun startTarget() = ProcessBuilder(executable.absolutePath, "+S", "2:2", "-sname", node, "-setcookie", cookie, "-noshell", "-eval", "register(alquimista_observer_fixture, spawn(fun() -> receive stop -> ok end end)), ets:new(alquimista_test_table, [named_table, public]), ets:insert(alquimista_test_table, {example, 42}), ets:insert(alquimista_test_table, [{N,N} || N <- lists:seq(1,50)]), ets:new(alquimista_private_table, [named_table, private]), {ok, _Socket} = socket:open(inet, stream, tcp), timer:sleep(infinity).")
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
            assertTrue("Expected real ETS tables", initial.tables.isNotEmpty())
            assertTrue("Expected real ports", initial.ports.isNotEmpty())
            assertTrue("Expected real memory allocators", initial.allocators.any { it.carrierBytes > 0 && it.name.isNotBlank() })
            assertTrue("Expected the test socket", initial.sockets.any { it.domain == "inet" && it.type == "stream" && it.protocol == "tcp" })
            assertTrue("Expected applications and supervisors", initial.supervision.any { it.kind == "supervisor" })
            val session = ObserverConnection.open(listOf(executable.absolutePath), "$node@$host", cookie, false)
            val processId = session.processId
            try {
                val first = session.snapshot()
                val second = session.snapshot()
                assertTrue("Expected scheduler samples after initialization", second.schedulers.isNotEmpty())
                assertEquals(first.node, second.node)
                val kernelSupervisor = first.processes.single { it.name == "kernel_sup" }
                assertTrue(session.processState(kernelSupervisor).isNotEmpty())
                val etsTable = first.tables.single { it.name == "alquimista_test_table" }
                val contents = session.tableContents(etsTable)
                assertTrue(contents.contains("example"))
                assertTrue("The viewer should include all 50 small objects", (1..50).all { contents.contains("{$it,$it}") })
                val privateTable = first.tables.single { it.name == "alquimista_private_table" }
                assertThrows(IllegalStateException::class.java) { session.tableContents(privateTable) }
                val fixture = first.processes.single { it.name == "alquimista_observer_fixture" }
                assertTrue(session.inspectProcess(fixture).contains("registered_name"))
                ObserverConnection.open(listOf(executable.absolutePath), "$node@$host", cookie, false).use { otherSession ->
                otherSession.startTrace(otherSession.snapshot().processes.single { it.name == "kernel_sup" })
                session.startTrace(fixture)
                assertThrows(IllegalStateException::class.java) { session.processState(fixture) }
                val events = session.traceEvents()
                assertTrue("Expected a received system message in the trace: $events", events.contains("system"))
                session.stopTrace()
                assertEquals("", session.traceEvents())
                otherSession.processState(otherSession.snapshot().processes.single { it.name == "kernel_sup" })
                assertTrue("Stopping one trace must not stop the other", otherSession.traceEvents().contains("system"))
                otherSession.stopTrace()
                }
                session.killProcess(fixture)
                assertFalse(session.snapshot().processes.any { it.name == "alquimista_observer_fixture" })
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
