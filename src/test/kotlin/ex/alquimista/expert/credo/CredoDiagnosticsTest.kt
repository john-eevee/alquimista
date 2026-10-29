package ex.alquimista.expert.credo

import org.junit.Assert.*
import org.junit.Test
import java.nio.file.Paths

class CredoDiagnosticsTest {
    @Test
    fun `matches relative and absolute paths to the analyzed file`() {
        val root = Paths.get("/workspace/project")
        val file = root.resolve("lib/a.ex")
        assertTrue(CredoDiagnostics.matchesFile(root, file, "lib/a.ex"))
        assertTrue(CredoDiagnostics.matchesFile(root, file, file.toString()))
        assertFalse(CredoDiagnostics.matchesFile(root, file, "lib/b.ex"))
    }

    @Test
    fun `reports message and check name`() {
        val issue = CredoIssue("a.ex", 1, null, null, "Use a guard", "refactor", "Credo.Check.Refactor.Test")
        assertEquals("Credo: Use a guard (Credo.Check.Refactor.Test)", CredoDiagnostics.message(issue))
    }
}
