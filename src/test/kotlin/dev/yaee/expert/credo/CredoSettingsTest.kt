package dev.yaee.expert.credo

import org.junit.Assert.*
import org.junit.Test

class CredoSettingsTest {
    @Test
    fun `Credo is disabled until explicitly enabled and state can be restored`() {
        val settings = CredoSettings()
        assertFalse(settings.state.enabled)
        settings.state.enabled = true
        val restored = CredoSettings()
        restored.loadState(settings.state)
        assertTrue(restored.state.enabled)
    }
}
