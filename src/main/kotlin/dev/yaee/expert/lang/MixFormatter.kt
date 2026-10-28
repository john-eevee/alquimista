package dev.yaee.expert.lang

import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

internal data class MixFormatResult(val text: String? = null, val error: String? = null)

internal object MixFormatter {
    fun arguments(fileName: String): List<String> = listOf(
        "run", "--no-start", "--no-compile", "-e",
        "Mix.shell(Mix.Shell.Quiet); Mix.Task.run(\"format\", System.argv())",
        "--", "--stdin-filename", fileName, "-"
    )

    fun run(start: () -> Process, text: String, onStarted: (Process) -> Unit = {}): MixFormatResult {
        val process = start()
        onStarted(process)
        val readers = Executors.newFixedThreadPool(2)
        try {
            val output = readers.submit<String> { process.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() } }
            val error = readers.submit<String> { process.errorStream.bufferedReader(Charsets.UTF_8).use { it.readText() } }
            process.outputStream.bufferedWriter(Charsets.UTF_8).use { it.write(text) }
            if (!process.waitFor(120, TimeUnit.SECONDS)) {
                process.destroyForcibly()
                return MixFormatResult(error = "Mix formatting timed out.")
            }
            val stdout = output.get()
            val stderr = error.get()
            return if (process.exitValue() == 0) MixFormatResult(text = stdout)
            else MixFormatResult(error = stderr.ifBlank { stdout }.ifBlank { "Mix format failed." })
        } finally {
            if (process.isAlive) process.destroyForcibly()
            readers.shutdownNow()
        }
    }
}
