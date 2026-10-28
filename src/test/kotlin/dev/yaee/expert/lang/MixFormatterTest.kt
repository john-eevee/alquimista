package dev.yaee.expert.lang

import dev.yaee.expert.project.MixProjectTemplate
import dev.yaee.expert.sdk.MixSdkCommand
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import javax.xml.parsers.DocumentBuilderFactory

class MixFormatterTest {
    private fun project(): Path {
        val root = Files.createTempDirectory(Files.createDirectories(Path.of("build", "tmp")), "mix-format-").toAbsolutePath()
        MixProjectTemplate.files("format_fixture").forEach { (path, content) ->
            val target = root.resolve(path)
            Files.createDirectories(target.parent)
            Files.writeString(target, content)
        }
        return root
    }

    private fun mix(): File {
        val mix = System.getenv("PATH").orEmpty().split(File.pathSeparator).map { File(it, "mix") }
            .firstOrNull { it.isFile && it.canExecute() }
        assumeTrue("Mix is required", mix != null)
        return mix!!
    }

    @Test fun `formats unsaved text with project formatter configuration`() {
        val root = project()
        Files.writeString(root.resolve(".formatter.exs"), "[line_length: 20]\n")
        val command = ProcessBuilder(listOf(mix().absolutePath) + MixFormatter.arguments("lib/example.ex"))
            .directory(root.toFile()).apply { environment()["ERL_FLAGS"] = "+S 2:2" }
        val result = MixFormatter.run({ command.start() }, "defmodule Example do\ndef f, do: [1,2,3,4,5,6,7,8]\nend\n")
        assertTrue(result.error.orEmpty(), result.text!!.contains("defmodule Example do\n  def f"))
        assertTrue(result.text.contains("\n    "))
    }

    @Test fun `syntax errors are returned instead of formatted text`() {
        val root = project()
        val command = ProcessBuilder(listOf(mix().absolutePath) + MixFormatter.arguments("lib/example.ex"))
            .directory(root.toFile()).apply { environment()["ERL_FLAGS"] = "+S 2:2" }
        val result = MixFormatter.run({ command.start() }, "defmodule Broken do\n")
        assertNull(result.text)
        assertFalse(result.error.isNullOrBlank())
    }

    @Test fun `formatter uses selected SDK mix and its sibling executables`() {
        assumeTrue(File.separatorChar != '\\')
        val root = project()
        val bin = Files.createDirectories(root.resolve("sdk/bin"))
        val elixir = Files.writeString(bin.resolve("elixir"), "#!/bin/sh\nprintf 'sdk:'\n").toFile()
        val mix = Files.writeString(bin.resolve("mix"), "#!/bin/sh\nelixir\ncat\n").toFile()
        assertTrue(elixir.setExecutable(true))
        assertTrue(mix.setExecutable(true))
        val invocation = MixSdkCommand.forSdk(root.resolve("sdk").toString(), MixFormatter.arguments("lib/example.ex"), System.getenv("PATH").orEmpty(), false)
        val command = ProcessBuilder(listOf(invocation.executable) + invocation.arguments)
            .directory(root.toFile()).apply { environment()["PATH"] = invocation.path }
        assertEquals("sdk:unsaved text", MixFormatter.run({ command.start() }, "unsaved text").text)
    }

    @Test fun `compiles formatter plugins when necessary without polluting the formatted text`() {
        val root = project()
        Files.writeString(root.resolve("lib/custom_formatter.ex"), """defmodule CustomFormatter do
  @behaviour Mix.Tasks.Format
  def features(_opts), do: [extensions: [".ex"]]
  def format(contents, _opts), do: "# plugin applied\n" <> contents
end
""")
        Files.writeString(root.resolve(".formatter.exs"), "[plugins: [CustomFormatter]]\n")
        val command = ProcessBuilder(listOf(mix().absolutePath) + MixFormatter.arguments("lib/example.ex"))
            .directory(root.toFile()).apply { environment()["ERL_FLAGS"] = "+S 2:2" }
        val result = MixFormatter.run({ command.start() }, "value=1\n")
        assertEquals(result.error, "# plugin applied\nvalue=1\n", result.text)
    }

    @Test fun `registers Mix formatting service`() {
        val xml = javaClass.getResourceAsStream("/META-INF/plugin.xml")!!.use {
            DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(it)
        }
        val services = xml.getElementsByTagName("formattingService")
        assertTrue((0 until services.length).any {
            services.item(it).attributes.getNamedItem("implementation").nodeValue == "dev.yaee.expert.lang.MixFormattingService"
        })
    }
}
