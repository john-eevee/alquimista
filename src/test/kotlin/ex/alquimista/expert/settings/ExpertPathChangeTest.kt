package ex.alquimista.expert.settings

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExpertPathChangeTest {
    @Test
    fun `ignores whitespace-only differences in the executable path`() {
        assertFalse(ExpertPathChange.hasChanged("/opt/expert", " /opt/expert "))
    }

    @Test
    fun `detects a new executable path`() {
        assertTrue(ExpertPathChange.hasChanged("expert", "/opt/expert"))
    }
}
