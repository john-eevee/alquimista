package ex.alquimista.expert.sdk

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ElixirSdkVersionTest {
    @Test
    fun `extracts the Elixir version from standard runtime output`() {
        val output = "Erlang/OTP 27 [erts-15.2]\nElixir 1.18.1 (compiled with Erlang/OTP 27)"
        assertEquals("1.18.1", ElixirSdkVersion.parse(output))
    }

    @Test
    fun `preserves prerelease version suffixes`() {
        assertEquals("1.19.0-rc.1", ElixirSdkVersion.parse("Elixir 1.19.0-rc.1"))
    }

    @Test
    fun `rejects output without an Elixir version`() {
        assertNull(ElixirSdkVersion.parse("Erlang/OTP 27"))
    }
}
