package dev.yaee.expert.credo

import org.junit.Assert.*
import org.junit.Test

class CredoEngineTest {
    @Test
    fun `disabled and missing dependency never launch credo and missing check is cached`() {
        val calls = mutableListOf<List<String>>()
        val engine = CredoEngine { args -> calls.add(args); "* jason 1.0\n  ok" }
        assertNull(engine.analyze(false, "config", "lib/a.ex", "v1"))
        assertTrue(calls.isEmpty())
        assertNull(engine.analyze(true, "config", "lib/a.ex", "v1"))
        assertNull(engine.analyze(true, "config", "lib/b.ex", "v2"))
        assertEquals(listOf(listOf("deps")), calls)
        engine.analyze(true, "changed", "lib/a.ex", "v1")
        assertEquals(2, calls.size)
    }

    @Test
    fun `runs once per saved file revision and parses the report`() {
        val calls = mutableListOf<List<String>>()
        val engine = CredoEngine { args ->
            calls.add(args)
            if (args == listOf("deps")) "* credo 1.7\n  ok"
            else """{"issues":[{"filename":"lib/a.ex","line_no":1,"message":"problem"}]}"""
        }
        val result = engine.analyze(true, "config", "lib/a.ex", "v1")
        assertEquals("problem", result!!.single().message)
        assertEquals(result, engine.analyze(true, "config", "lib/a.ex", "v1"))
        engine.analyze(true, "config", "lib/a.ex", "v2")
        assertEquals(3, calls.size)
        assertEquals(listOf("credo", "suggest", "lib/a.ex", "--format", "json", "--mute-exit-status", "--no-color"), calls[1])
    }

    @Test
    fun `failed dependency probes and failed credo runs are not repeatedly spawned`() {
        var probes = 0
        val unavailable = CredoEngine { probes++; null }
        repeat(3) { unavailable.analyze(true, "config", "lib/a.ex", "v1") }
        assertEquals(1, probes)

        var calls = 0
        val fails = CredoEngine { args -> calls++; if (args == listOf("deps")) "* credo 1.7\n  ok" else null }
        repeat(3) { assertNull(fails.analyze(true, "config", "lib/a.ex", "v1")) }
        assertEquals(2, calls)
        fails.analyze(true, "config", "lib/a.ex", "v2")
        assertEquals(3, calls)
    }
}
