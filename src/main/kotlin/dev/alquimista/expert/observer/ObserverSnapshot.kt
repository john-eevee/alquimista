package dev.alquimista.expert.observer

import java.util.Base64
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicBoolean

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

    fun open(command: List<String>, node: String, cookie: String, longNames: Boolean, directory: java.io.File? = null): ObserverSession {
        require(isValidNode(node)) { "Enter a node name in name@host format" }
        require('\n' !in cookie && '\r' !in cookie) { "The cookie cannot contain a line break" }
        val script = ObserverConnection::class.java.getResourceAsStream("/observer/snapshot.erl")
            ?.bufferedReader()?.use { it.readText() } ?: error("Observer script is missing")
        val localName = "alquimista_observer_" + UUID.randomUUID().toString().replace("-", "").take(12)
        val args = command + listOf("+S", "2:2", "-hidden", if (longNames) "-name" else "-sname", localName, "-noshell", "-eval", script)
        val process = ProcessBuilder(args).directory(directory).redirectErrorStream(true).start()
        try { return ObserverSession(process, node, cookie) }
        catch (error: Exception) { process.destroyForcibly(); throw error }
    }

}

class ObserverSession internal constructor(private val process: Process, node: String, cookie: String) : AutoCloseable {
    private val lines = LinkedBlockingQueue<String>()
    private val closed = AtomicBoolean(false)
    private val writer = process.outputStream.bufferedWriter(Charsets.UTF_8)
    val processId: Long get() = process.pid()
    val isAlive: Boolean get() = process.isAlive

    init {
        Thread({
            try { process.inputStream.bufferedReader(Charsets.UTF_8).useLines { sequence -> sequence.forEach { lines.put(it) } } }
            catch (_: java.io.IOException) { }
            finally { lines.offer(END_OF_STREAM) }
        }, "Alquimista Observer output").apply { isDaemon = true; start() }
        writer.write("$node\n$cookie\n")
        writer.flush()
    }

    @Synchronized fun snapshot(): ObserverSnapshot {
        check(!closed.get() && process.isAlive) { "Observer session is closed" }
        try {
            writer.write("refresh\n"); writer.flush()
            val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30)
            val response = StringBuilder()
            while (true) {
                val remaining = deadline - System.nanoTime()
                val line = if (remaining > 0) lines.poll(remaining, TimeUnit.NANOSECONDS) else null
                if (line == null) { close(); error("Observer connection timed out") }
                if (line == END_OF_STREAM) { close(); error("Observer process exited: ${response.toString().takeLast(1000)}") }
                if (line == "END") return ObserverSnapshot.parse(response.toString())
                response.append(line).append('\n')
            }
        } catch (error: java.io.IOException) {
            close()
            throw IllegalStateException("Observer process connection failed", error)
        }
    }

    override fun close() {
        if (closed.compareAndSet(false, true)) {
            process.destroyForcibly()
            lines.offer(END_OF_STREAM)
        }
    }

    companion object { private const val END_OF_STREAM = "\u0000observer-eof" }
}
