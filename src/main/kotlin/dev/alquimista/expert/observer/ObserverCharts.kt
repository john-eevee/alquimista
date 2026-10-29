package dev.alquimista.expert.observer

import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import java.awt.Graphics
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.UIManager
import kotlin.math.max

/** Memory samples and I/O bytes per second; a reset counter starts a new series. */
internal class ObserverHistory(private val limit: Int = 60) {
    data class Sample(val timeMillis: Long, val memory: Long, val inputPerSecond: Long, val outputPerSecond: Long, val metrics: ObserverMetrics, val schedulerPercent: Long)
    private var previous: Pair<Long, ObserverMetrics>? = null
    private var previousSchedulers: Map<Int, SchedulerCounters> = emptyMap()
    val samples = ArrayList<Sample>()
    fun add(time: Long, snapshot: ObserverSnapshot) {
        val old = previous
        val elapsed = if (old == null) 0 else time - old.first
        fun rate(value: Long, previousValue: Long): Long =
            if (elapsed <= 0 || value < previousValue) 0 else ((value - previousValue).toDouble() * 1000 / elapsed).toLong()
        var activeDelta = 0.0
        var totalDelta = 0.0
        for (current in snapshot.schedulers) {
            val before = previousSchedulers[current.id] ?: continue
            if (current.active >= before.active && current.total > before.total) {
                activeDelta += (current.active - before.active).toDouble()
                totalDelta += (current.total - before.total).toDouble()
            }
        }
        val schedulerPercent = if (totalDelta > 0) (100 * activeDelta / totalDelta).toLong().coerceIn(0, 100) else 0
        samples.add(Sample(time, snapshot.memoryBytes, rate(snapshot.metrics.inputBytes, old?.second?.inputBytes ?: snapshot.metrics.inputBytes), rate(snapshot.metrics.outputBytes, old?.second?.outputBytes ?: snapshot.metrics.outputBytes), snapshot.metrics, schedulerPercent))
        if (samples.size > limit) samples.removeAt(0)
        previous = time to snapshot.metrics
        previousSchedulers = snapshot.schedulers.associateBy { it.id }
    }
    fun clear() { samples.clear(); previous = null; previousSchedulers = emptyMap() }
}

internal class ObserverCharts : JPanel(BorderLayout()) {
    private val history = ObserverHistory()
    private val schedulers = Graph("Scheduler utilization (percent)") { it.schedulerPercent }
    private val memory = Graph("Total memory (bytes)") { it.memory }
    private val processMemory = Graph("Process memory (bytes)") { it.metrics.processMemory }
    private val atomMemory = Graph("Atom memory (bytes)") { it.metrics.atomMemory }
    private val binaryMemory = Graph("Binary memory (bytes)") { it.metrics.binaryMemory }
    private val codeMemory = Graph("Code memory (bytes)") { it.metrics.codeMemory }
    private val etsMemory = Graph("ETS memory (bytes)") { it.metrics.etsMemory }
    private val input = Graph("Input (bytes / second)") { it.inputPerSecond }
    private val output = Graph("Output (bytes / second)") { it.outputPerSecond }
    init {
        val graphs = JPanel(java.awt.GridLayout(9, 1, 0, 8))
        allGraphs().forEach(graphs::add)
        add(javax.swing.JScrollPane(graphs))
    }
    fun add(snapshot: ObserverSnapshot) {
        history.add(System.currentTimeMillis(), snapshot)
        allGraphs().forEach { it.samples = history.samples.toList(); it.repaint() }
    }
    fun clear() { history.clear(); allGraphs().forEach { it.samples = emptyList(); it.repaint() } }
    private fun allGraphs() = listOf(schedulers, memory, processMemory, atomMemory, binaryMemory, codeMemory, etsMemory, input, output)
    private class Graph(title: String, private val value: (ObserverHistory.Sample) -> Long) : JPanel(BorderLayout()) {
        var samples: List<ObserverHistory.Sample> = emptyList()
        init { add(JLabel(title), BorderLayout.NORTH); preferredSize = Dimension(400, 130) }
        override fun paintComponent(g: Graphics) {
            super.paintComponent(g)
            val values = samples.map(value)
            if (values.size < 2) return
            val ceiling = max(1L, values.maxOrNull() ?: 1L).toDouble()
            val startY = insets.top + 26
            val plotHeight = (height - startY - insets.bottom - 8).coerceAtLeast(1)
            val plotWidth = (width - insets.left - insets.right - 12).coerceAtLeast(1)
            g.color = UIManager.getColor("Label.foreground") ?: Color.GRAY
            g.drawString("Current: ${values.last()}  ·  Peak: ${ceiling.toLong()}", insets.left + 6, height - insets.bottom - 2)
            g.color = UIManager.getColor("Component.focusColor") ?: UIManager.getColor("Label.foreground") ?: Color.GRAY
            for (i in 1 until values.size) {
                fun x(index: Int) = insets.left + 6 + index * plotWidth / (values.size - 1)
                fun y(index: Int) = startY + plotHeight - (values[index] / ceiling * plotHeight).toInt()
                g.drawLine(x(i - 1), y(i - 1), x(i), y(i))
            }
        }
    }
}
