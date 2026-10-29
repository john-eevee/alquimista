package dev.alquimista.expert.observer

import org.junit.Assert.*
import org.junit.Test

class ObserverSnapshotTest {
    @Test fun `parses system information and process rows`() {
        val snapshot = ObserverSnapshot.parse("OK\tmyapp@host\t29\t4096\t3\nP\t<0.1.0>\td29ya2Vy\t2048\t5\t100\n")
        assertEquals("myapp@host", snapshot.node)
        assertEquals("29", snapshot.otpRelease)
        assertEquals(4096L, snapshot.memoryBytes)
        assertEquals(3, snapshot.processCount)
        assertEquals("worker", snapshot.processes.single().name)
        assertEquals(2048L, snapshot.processes.single().memoryBytes)
    }

    @Test fun `reports unreachable nodes`() {
        val exception = assertThrows(IllegalStateException::class.java) { ObserverSnapshot.parse("ERROR\tunreachable\n") }
        assertTrue(exception.message!!.contains("unreachable"))
    }

    @Test fun `rejects invalid node names`() {
        assertFalse(ObserverConnection.isValidNode("bad name@host"))
        assertFalse(ObserverConnection.isValidNode("missing-host"))
        assertTrue(ObserverConnection.isValidNode("my_app@localhost"))
    }
    @Test fun `tool window is registered`() {
        val document = javax.xml.parsers.DocumentBuilderFactory.newInstance().newDocumentBuilder()
            .parse(java.io.File("src/main/resources/META-INF/plugin.xml"))
        val windows = document.getElementsByTagName("toolWindow")
        assertTrue((0 until windows.length).any {
            val element = windows.item(it) as org.w3c.dom.Element
            element.getAttribute("id") == "Erlang Observer" &&
                element.getAttribute("factoryClass") == ObserverToolWindowFactory::class.java.name
        })
    }

    @Test fun `rejects malformed response and cookie with line breaks`() {
        assertThrows(IllegalArgumentException::class.java) { ObserverSnapshot.parse("OK\tnode\t29\tbad\t1\n") }
        assertThrows(IllegalArgumentException::class.java) {
            ObserverConnection.open(listOf("erl"), "app@host", "bad\ncookie", false)
        }
    }

    @Test fun `parses memory categories and io counters for load charts`() {
        val snapshot = ObserverSnapshot.parse("OK\tnode@host\t29\t4096\t3\nM\t2000\t100\t200\t300\t400\t1024\t2048\n")
        assertEquals(2000L, snapshot.metrics.processMemory)
        assertEquals(100L, snapshot.metrics.atomMemory)
        assertEquals(200L, snapshot.metrics.binaryMemory)
        assertEquals(300L, snapshot.metrics.codeMemory)
        assertEquals(400L, snapshot.metrics.etsMemory)
        assertEquals(1024L, snapshot.metrics.inputBytes)
        assertEquals(2048L, snapshot.metrics.outputBytes)
    }

    @Test fun `chart history computes rates and handles counter reset`() {
        val history = ObserverHistory(2)
        fun sample(input: Long, output: Long) = ObserverSnapshot("node", "29", 4096, 1, emptyList(), ObserverMetrics(inputBytes = input, outputBytes = output))
        history.add(1000, sample(100, 200))
        history.add(3000, sample(300, 600))
        assertEquals(100, history.samples.last().inputPerSecond)
        assertEquals(200, history.samples.last().outputPerSecond)
        history.add(4000, sample(10, 20))
        assertEquals(2, history.samples.size)
        assertEquals(0, history.samples.last().inputPerSecond)
        history.clear()
        assertTrue(history.samples.isEmpty())
    }

    @Test fun `parses ETS tables including opaque table identifier`() {
        val snapshot = ObserverSnapshot.parse("OK\tnode@host\t29\t4096\t3\nT\tdGFibGU=\tYWJj\tPDAuMS4wPg==\t7\t128\tcHVibGlj\tc2V0\n")
        val table = snapshot.tables.single()
        assertEquals("table", table.name)
        assertEquals("abc", table.id)
        assertEquals("<0.1.0>", table.owner)
        assertEquals(7L, table.size)
        assertEquals(128L, table.memoryWords)
        assertEquals("public", table.protection)
        assertEquals("set", table.type)
    }

}
