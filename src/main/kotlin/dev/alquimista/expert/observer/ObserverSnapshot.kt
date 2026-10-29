package dev.alquimista.expert.observer

import java.util.Base64
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.io.ByteArrayOutputStream

data class ObservedProcess(val pid: String, val name: String, val memoryBytes: Long, val queueLength: Long, val reductions: Long)
data class ObserverSnapshot(val node: String, val otpRelease: String, val memoryBytes: Long, val processCount: Int, val processes: List<ObservedProcess>) {
    companion object {
        fun parse(output: String): ObserverSnapshot {
            val lines = output.lineSequence().filter { it.isNotBlank() }.toList()
            val header = lines.firstOrNull { it.startsWith("OK\t") || it.startsWith("ERROR\t") }
                ?: error("Observer received no node information")
            if (header.startsWith("ERROR\t")) error(header.substringAfter('\t'))
            val fields = header.split('\t')
            require(fields.size == 5) { "Invalid Observer response" }
            val processes = lines.filter { it.startsWith("P\t") }.map { line ->
                val row = line.split('\t')
                require(row.size == 6) { "Invalid Observer process response" }
                ObservedProcess(row[1], String(Base64.getDecoder().decode(row[2]), Charsets.UTF_8), row[3].toLong(), row[4].toLong(), row[5].toLong())
            }
            return ObserverSnapshot(fields[1], fields[2], fields[3].toLong(), fields[4].toInt(), processes)
        }
    }
}

object ObserverConnection {
    fun isValidNode(node: String): Boolean = node.matches(Regex("[A-Za-z0-9_][A-Za-z0-9_.-]*@[A-Za-z0-9_][A-Za-z0-9_.-]*"))

    fun snapshot(command: List<String>, node: String, cookie: String, longNames: Boolean, directory: java.io.File? = null): ObserverSnapshot {
        require(isValidNode(node)) { "Enter a node name in name@host format" }
        require('\n' !in cookie && '\r' !in cookie) { "The cookie cannot contain a line break" }
        val script = ObserverConnection::class.java.getResourceAsStream("/observer/snapshot.erl")
            ?.bufferedReader()?.use { it.readText() } ?: error("Observer script is missing")
        val localName = "alquimista_observer_" + UUID.randomUUID().toString().replace("-", "").take(12)
        val args = command + listOf("+S", "2:2", "-hidden", if (longNames) "-name" else "-sname", localName, "-noshell", "-eval", script)
        val process = ProcessBuilder(args).directory(directory).redirectErrorStream(true).start()
        val output = ByteArrayOutputStream()
        val reader = Thread { process.inputStream.use { it.copyTo(output) } }.apply { isDaemon = true; start() }
        try {
            process.outputStream.bufferedWriter(Charsets.UTF_8).use { it.write("$node\n$cookie\n") }
            if (!process.waitFor(30, TimeUnit.SECONDS)) error("Observer connection timed out")
            reader.join(2000)
            val text = output.toString(Charsets.UTF_8)
            if (process.exitValue() != 0) error("Observer process failed: ${text.takeLast(2000)}")
            return ObserverSnapshot.parse(text)
        } finally {
            if (process.isAlive) process.destroyForcibly()
        }
    }
}
