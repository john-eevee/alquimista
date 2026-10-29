package ex.alquimista.expert.credo

import org.junit.Assert.*
import org.junit.Test

class CredoDependencyTest {
    @Test
    fun `accepts available direct or transitive credo including an uncompiled dependency`() {
        assertTrue(CredoDependency.available("* credo 1.7.19 (Hex package) (mix)\n  the dependency build is outdated, please run mix deps.compile\n* jason 1.4"))
        assertTrue(CredoDependency.available("* credo (../local/credo) (mix)\n  ok"))
    }

    @Test
    fun `rejects absent or unfetched credo`() {
        assertFalse(CredoDependency.available("* jason 1.4\n  ok"))
        assertFalse(CredoDependency.available("* credo (Hex package)\n  the dependency is not available, run \"mix deps.get\""))
        assertFalse(CredoDependency.available("* credo_extra 1.0\n  ok"))
    }
}
