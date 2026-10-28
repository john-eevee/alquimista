package dev.yaee.expert.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class MiseCommandResolverTest {
    @Test
    fun `wraps commands with mise exec when mise is available`() {
        assertEquals(
            MiseCommand("/home/user/.local/bin/mise", listOf("exec", "--", "/opt/expert", "--stdio")),
            MiseCommandResolver.resolve("/home/user/.local/bin/mise", "/opt/expert", listOf("--stdio")),
        )
    }

    @Test
    fun `uses the command directly when mise is unavailable`() {
        assertEquals(
            MiseCommand("expert", listOf("--stdio")),
            MiseCommandResolver.resolve(null, "expert", listOf("--stdio")),
        )
    }
}
