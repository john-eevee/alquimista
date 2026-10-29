package dev.alquimista.expert.observer

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import dev.alquimista.expert.settings.MiseCommandResolver
import dev.alquimista.expert.settings.MiseSupport
import java.awt.BorderLayout
import java.awt.FlowLayout
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import javax.swing.*
import javax.swing.table.DefaultTableModel

class ObserverToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val view = ObserverToolWindow(project)
        val content = ContentFactory.getInstance().createContent(view.panel, "", false)
        content.setDisposer(view)
        toolWindow.contentManager.addContent(content)
    }
}

private class ObserverToolWindow(private val project: Project) : Disposable {
    val panel = JPanel(BorderLayout())
    private val node = JTextField(20)
    private val cookie = JPasswordField(12)
    private val longNames = JCheckBox("Long node names")
    private val connect = JButton("Connect")
    private val disconnect = JButton("Disconnect")
    private val status = JLabel("Enter a distributed node name and connect.")
    private val summary = JLabel(" ")
    private val model = object :
        DefaultTableModel(arrayOf("PID", "Registered name", "Memory (bytes)", "Message queue", "Reductions"), 0) {
        override fun isCellEditable(row: Int, column: Int) = false
        override fun getColumnClass(columnIndex: Int): Class<*> =
            if (columnIndex >= 2) java.lang.Long::class.java else String::class.java
    }
    private val executor = Executors.newSingleThreadScheduledExecutor { task ->
        Thread(task, "Alquimista Observer").apply {
            isDaemon = true
        }
    }
    private var job: ScheduledFuture<*>? = null
    private val sessionLock = Any()
    private var session: ObserverSession? = null
    @Volatile
    private var generation = 0

    init {
        val controls = JPanel(FlowLayout(FlowLayout.LEADING)).apply {
            add(JLabel("Node:")); add(node); add(JLabel("Cookie:")); add(cookie); add(longNames); add(connect); add(
            disconnect
        )
        }
        val information = JPanel().apply { layout = BoxLayout(this, BoxLayout.Y_AXIS); add(status); add(summary) }
        val top =
            JPanel(BorderLayout()).apply { add(controls, BorderLayout.NORTH); add(information, BorderLayout.SOUTH) }
        val table = JTable(model).apply { autoCreateRowSorter = true }
        panel.add(top, BorderLayout.NORTH)
        panel.add(JScrollPane(table), BorderLayout.CENTER)
        disconnect.isEnabled = false
        connect.addActionListener { start() }
        disconnect.addActionListener { stop() }
    }

    private fun start() {
        val target = node.text.trim()
        if (!ObserverConnection.isValidNode(target)) {
            status.text = "Enter a node name in name@host format."; return
        }
        val secret = String(cookie.password)
        if (secret.contains('\n') || secret.contains('\r')) {
            status.text = "The cookie cannot contain a line break."; return
        }
        val useLongNames = longNames.isSelected
        val current = ++generation
        connect.isEnabled = false
        disconnect.isEnabled = true
        node.isEnabled = false; cookie.isEnabled = false; longNames.isEnabled = false
        status.text = "Connecting to $target…"
        val resolved = MiseCommandResolver.resolve(MiseSupport.findExecutable(), "erl", emptyList())
        val command = listOf(resolved.executable) + resolved.arguments
        job = executor.scheduleWithFixedDelay({
            try {
                val active = synchronized(sessionLock) { session } ?: run {
                    val created = ObserverConnection.open(
                        command,
                        target,
                        secret,
                        useLongNames,
                        project.basePath?.let { java.io.File(it) })
                    synchronized(sessionLock) {
                        if (current != generation) {
                            created.close(); return@scheduleWithFixedDelay
                        }
                        session = created
                    }
                    created
                }
                val snapshot = active.snapshot()
                ApplicationManager.getApplication().invokeLater {
                    if (current == generation && !project.isDisposed) {
                        status.text = "Connected to ${snapshot.node}. Refreshing every 5 seconds."
                        summary.text =
                            "OTP ${snapshot.otpRelease}   |   Memory: ${snapshot.memoryBytes} bytes   |   Processes: ${snapshot.processCount}"
                        model.rowCount = 0
                        snapshot.processes.forEach {
                            model.addRow(
                                arrayOf<Any>(
                                    it.pid,
                                    it.name,
                                    it.memoryBytes,
                                    it.queueLength,
                                    it.reductions
                                )
                            )
                        }
                    }
                }
            } catch (error: Exception) {
                synchronized(sessionLock) {
                    if (session?.isAlive == false) {
                        session?.close(); session = null
                    }
                }
                ApplicationManager.getApplication().invokeLater {
                    if (current == generation && !project.isDisposed) {
                        status.text = "Connection failed: ${error.message ?: error.javaClass.simpleName}. Retrying…"
                        summary.text = " "; model.rowCount = 0
                    }
                }
            }
        }, 0, 5, TimeUnit.SECONDS)
    }

    private fun stop() {
        generation++
        job?.cancel(true); job = null
        synchronized(sessionLock) { session?.close(); session = null }
        connect.isEnabled = true; disconnect.isEnabled = false
        node.isEnabled = true; cookie.isEnabled = true; longNames.isEnabled = true
        cookie.text = ""
        status.text = "Disconnected."; summary.text = " "; model.rowCount = 0
    }

    override fun dispose() {
        generation++
        job?.cancel(true)
        synchronized(sessionLock) { session?.close(); session = null }
        executor.shutdownNow()
    }
}
