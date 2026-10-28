package dev.yaee.expert.sdk

import org.junit.Assert.*
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Paths

class MixSdkCommandTest {
    @Test
    fun `finds mix beside elixir for installation root and executable homes`() {
        val root = temporaryDirectory("mix-sdk-test")
        val bin = Files.createDirectories(root.resolve("bin"))
        val elixir = Files.createFile(bin.resolve("elixir"))
        val mix = Files.createFile(bin.resolve("mix"))
        assertEquals(mix, MixSdkCommand.findMix(root, false))
        assertEquals(mix, MixSdkCommand.findMix(elixir, false))
        assertEquals(mix, MixSdkCommand.findMix(bin, false))
    }

    @Test
    fun `does not substitute PATH when selected SDK has no mix`() {
        val root = temporaryDirectory("mix-sdk-missing")
        assertNull(MixSdkCommand.findMix(root, false))
    }

    @Test
    fun `finds windows batch mix executable`() {
        val root = temporaryDirectory("mix-sdk-windows")
        val bin = Files.createDirectories(root.resolve("bin"))
        val mix = Files.createFile(bin.resolve("mix.bat"))
        assertEquals(mix, MixSdkCommand.findMix(root, true))
    }

    @Test
    fun `selected SDK command puts its bin ahead of the inherited PATH`() {
        val root = temporaryDirectory("mix-sdk-command")
        val bin = Files.createDirectories(root.resolve("bin"))
        val mix = Files.createFile(bin.resolve("mix"))
        val command = MixSdkCommand.forSdk(root.toString(), listOf("test"), "/other/bin", false)
        assertEquals(mix.toString(), command.executable)
        assertEquals(listOf("test"), command.arguments)
        assertEquals("${bin}${java.io.File.pathSeparator}/other/bin", command.path)
    }

    private fun temporaryDirectory(prefix: String) =
        Files.createTempDirectory(Files.createDirectories(Paths.get("build", "tmp")), prefix)
}
