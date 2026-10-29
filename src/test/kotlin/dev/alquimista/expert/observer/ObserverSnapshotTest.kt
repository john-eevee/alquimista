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
            ObserverConnection.snapshot(listOf("erl"), "app@host", "bad\ncookie", false)
        }
    }

}
