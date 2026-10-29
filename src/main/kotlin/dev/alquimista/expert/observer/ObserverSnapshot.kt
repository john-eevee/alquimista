package dev.alquimista.expert.observer

import java.util.*
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

data class ObservedProcess(
    val pid: String,
    val name: String,
    val memoryBytes: Long,
    val queueLength: Long,
    val reductions: Long,
    val token: String = ""
)

data class ObservedAllocator(val name: String, val blockBytes: Long, val carrierBytes: Long, val details: String)

data class ObservedSocket(val id: String, val fd: String, val owner: String, val domain: String, val type: String, val protocol: String, val readState: String, val writeState: String, val details: String)

data class ObservedPort(val id: String, val name: String, val owner: String, val inputBytes: Long, val outputBytes: Long, val queueBytes: Long, val details: String)

data class SchedulerCounters(val id: Int, val active: Long, val total: Long)

data class ObservedTreeNode(val id: String, val parentId: String, val label: String, val pid: String, val kind: String)

data class ObservedTable(val name: String, val id: String, val owner: String, val size: Long, val memoryWords: Long, val protection: String, val type: String, val token: String = "")

data class ObserverMetrics(
    val processMemory: Long = 0,
    val atomMemory: Long = 0,
    val binaryMemory: Long = 0,
    val codeMemory: Long = 0,
    val etsMemory: Long = 0,
    val inputBytes: Long = 0,
    val outputBytes: Long = 0,
)

data class ObserverSnapshot(
    val node: String,
    val otpRelease: String,
    val memoryBytes: Long,
    val processCount: Int,
    val processes: List<ObservedProcess>,
    val metrics: ObserverMetrics = ObserverMetrics(),
    val tables: List<ObservedTable> = emptyList(),
    val supervision: List<ObservedTreeNode> = emptyList(),
    val schedulers: List<SchedulerCounters> = emptyList(),
    val ports: List<ObservedPort> = emptyList(),
    val sockets: List<ObservedSocket> = emptyList(),
    val allocators: List<ObservedAllocator> = emptyList(),
) {
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
                require(row.size == 6 || row.size == 7) { "Invalid Observer process response" }
                ObservedProcess(
                    row[1],
                    String(Base64.getDecoder().decode(row[2]), Charsets.UTF_8),
                    row[3].toLong(),
                    row[4].toLong(),
                    row[5].toLong(),
                    row.getOrElse(6) { "" }
                )
            }
            val metrics = lines.firstOrNull { it.startsWith("M\t") }?.let { line ->
                val values = line.split('\t')
                require(values.size == 8) { "Invalid Observer metrics response" }
                ObserverMetrics(values[1].toLong(), values[2].toLong(), values[3].toLong(), values[4].toLong(), values[5].toLong(), values[6].toLong(), values[7].toLong())
            } ?: ObserverMetrics()
            fun decode(value: String) = String(Base64.getDecoder().decode(value), Charsets.UTF_8)
            val tables = lines.filter { it.startsWith("T\t") }.map { line ->
                val row = line.split('\t')
                require(row.size == 8 || row.size == 9) { "Invalid Observer table response" }
                ObservedTable(decode(row[1]), decode(row[2]), decode(row[3]), row[4].toLong(), row[5].toLong(), decode(row[6]), decode(row[7]), row.getOrElse(8) { "" })
            }
            val supervision = lines.filter { it.startsWith("S\t") }.map { line ->
                val row = line.split('\t')
                require(row.size == 6) { "Invalid supervision response" }
                ObservedTreeNode(decode(row[1]), decode(row[2]), decode(row[3]), decode(row[4]), decode(row[5]))
            }
            val schedulers = lines.filter { it.startsWith("Q\t") }.map { line ->
                val row = line.split('\t')
                require(row.size == 4) { "Invalid scheduler response" }
                SchedulerCounters(row[1].toInt(), row[2].toLong(), row[3].toLong())
            }
            val ports = lines.filter { it.startsWith("R\t") }.map { line ->
                val row = line.split('\t')
                require(row.size == 8) { "Invalid port response" }
                ObservedPort(decode(row[1]), decode(row[2]), decode(row[3]), row[4].toLong(), row[5].toLong(), row[6].toLong(), decode(row[7]))
            }
            val sockets = lines.filter { it.startsWith("K\t") }.map { line ->
                val row = line.split('\t')
                require(row.size == 10) { "Invalid socket response" }
                ObservedSocket(decode(row[1]), decode(row[2]), decode(row[3]), decode(row[4]), decode(row[5]), decode(row[6]), decode(row[7]), decode(row[8]), decode(row[9]))
            }
            val allocators = lines.filter { it.startsWith("A\t") }.map { line ->
                val row = line.split('\t')
                require(row.size == 5) { "Invalid allocator response" }
                ObservedAllocator(decode(row[1]), row[2].toLong(), row[3].toLong(), decode(row[4]))
            }
            return ObserverSnapshot(fields[1], fields[2], fields[3].toLong(), fields[4].toInt(), processes, metrics, tables, supervision, schedulers, ports, sockets, allocators)
        }
    }
}

object ObserverConnection {
    fun isValidNode(node: String): Boolean =
        node.matches(Regex("[A-Za-z0-9_][A-Za-z0-9_.-]*@[A-Za-z0-9_][A-Za-z0-9_.-]*"))

    fun open(
        command: List<String>,
        node: String,
        cookie: String,
        longNames: Boolean,
        directory: java.io.File? = null
    ): ObserverSession {
        require(isValidNode(node)) { "Enter a node name in name@host format" }
        require('\n' !in cookie && '\r' !in cookie) { "The cookie cannot contain a line break" }
        val script = ObserverConnection::class.java.getResourceAsStream("/observer/snapshot.erl")
            ?.bufferedReader()?.use { it.readText() } ?: error("Observer script is missing")
        val localName = "alquimista_observer_" + UUID.randomUUID().toString().replace("-", "").take(12)
        val args = command + listOf(
            "+S",
            "2:2",
            "-hidden",
            if (longNames) "-name" else "-sname",
            localName,
            "-noshell",
            "-eval",
            script
        )
        val process = ProcessBuilder(args).directory(directory).redirectErrorStream(true).start()
        try {
            return ObserverSession(process, node, cookie)
        } catch (error: Exception) {
            process.destroyForcibly(); throw error
        }
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
            try {
                process.inputStream.bufferedReader(Charsets.UTF_8)
                    .useLines { sequence -> sequence.forEach { lines.put(it) } }
            } catch (_: java.io.IOException) {
            } finally {
                lines.offer(END_OF_STREAM)
            }
        }, "Alquimista Observer output").apply { isDaemon = true; start() }
        writer.write("$node\n$cookie\n")
        writer.flush()
    }

    @Synchronized
    fun snapshot(): ObserverSnapshot = ObserverSnapshot.parse(request("refresh"))

    @Synchronized
    fun inspectProcess(process: ObservedProcess): String = action("inspect", process)

    @Synchronized
    fun killProcess(process: ObservedProcess) { action("kill", process) }

    @Synchronized
    fun processState(process: ObservedProcess): String = action("state", process)

    @Synchronized
    fun startTrace(process: ObservedProcess) { action("trace_start", process) }

    @Synchronized
    fun traceEvents(): String = readResult(request("trace_events"))

    @Synchronized
    fun stopTrace() { readResult(request("trace_stop")) }

    @Synchronized
    fun tableContents(table: ObservedTable): String = action("table", table.token)

    private fun action(operation: String, process: ObservedProcess): String = action(operation, process.token)

    private fun action(operation: String, token: String): String {
        require(token.matches(Regex("[A-Za-z0-9+/=]+"))) { "Invalid identifier" }
        return readResult(request("$operation\t$token"))
    }

    private fun readResult(response: String): String {
        val error = response.lineSequence().firstOrNull { it.startsWith("ERROR\t") }
        if (error != null) error(error.substringAfter('\t'))
        val result = response.lineSequence().firstOrNull { it.startsWith("RESULT\t") } ?: error("Invalid Observer response")
        return String(Base64.getDecoder().decode(result.substringAfter('\t')), Charsets.UTF_8)
    }

    private fun request(command: String): String {
        check(!closed.get() && process.isAlive) { "Observer session is closed" }
        try {
            writer.write("$command\n"); writer.flush()
            val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30)
            val response = StringBuilder()
            while (true) {
                val remaining = deadline - System.nanoTime()
                val line = if (remaining > 0) lines.poll(remaining, TimeUnit.NANOSECONDS) else null
                if (line == null) {
                    close(); error("Observer connection timed out")
                }
                if (line == END_OF_STREAM) {
                    close(); error("Observer process exited: ${response.toString().takeLast(1000)}")
                }
                if (line == "END") return response.toString()
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

    companion object {
        private const val END_OF_STREAM = "\u0000observer-eof"
    }
}
