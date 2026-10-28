package dev.yaee.expert.credo

import org.junit.Assert.*
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Paths

class CredoProjectTest {
    @Test
    fun `locates nearest Mix project within project root`() {
        val root = Files.createTempDirectory(Files.createDirectories(Paths.get("build", "tmp")), "credo-roots")
        Files.createFile(root.resolve("mix.exs"))
        val child = Files.createDirectories(root.resolve("apps/app/lib"))
        val app = child.parent
        Files.createFile(app.resolve("mix.exs"))
        assertEquals(app.toAbsolutePath().normalize(), CredoProject.findMixRoot(root, child.resolve("a.ex")))
        assertNull(CredoProject.findMixRoot(root, root.parent.resolve("elsewhere.ex")))
    }

    @Test
    fun `dependency changes invalidate the configuration fingerprint`() {
        val root = Files.createTempDirectory(Files.createDirectories(Paths.get("build", "tmp")), "credo-config")
        Files.writeString(root.resolve("mix.exs"), "project")
        val before = CredoProject.fingerprint(root, root, "sdk")
        Files.createDirectories(root.resolve("deps/credo"))
        Files.writeString(root.resolve("deps/credo/mix.exs"), "credo")
        assertNotEquals(before, CredoProject.fingerprint(root, root, "sdk"))
        assertNotEquals(before, CredoProject.fingerprint(root, root, "other-sdk"))
    }
}
