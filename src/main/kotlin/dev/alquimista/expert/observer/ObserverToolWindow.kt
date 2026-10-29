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
    private val tableModel = object : DefaultTableModel(arrayOf("Name", "Identifier", "Owner", "Objects", "Memory (words)", "Protection", "Type"), 0) {
        override fun isCellEditable(row: Int, column: Int) = false
        override fun getColumnClass(columnIndex: Int): Class<*> = if (columnIndex == 3 || columnIndex == 4) java.lang.Long::class.java else String::class.java
    }
    private val executor = Executors.newSingleThreadScheduledExecutor { task ->
        Thread(task, "Alquimista Observer").apply {
            isDaemon = true
        }
    }
    private var job: ScheduledFuture<*>? = null
    @Volatile private var tracing = false
    private val traceText = JTextArea().apply { isEditable = false }
    private var processes: List<ObservedProcess> = emptyList()
    private var tables: List<ObservedTable> = emptyList()
    private lateinit var processTable: JTable
    private val portsView = ObserverDetailsTable<ObservedPort>(arrayOf("Port", "Name", "Owner", "Input bytes", "Output bytes", "Queue bytes"), { arrayOf(it.id, it.name, it.owner, it.inputBytes, it.outputBytes, it.queueBytes) }, { it.details })
    private val socketsView = ObserverDetailsTable<ObservedSocket>(arrayOf("Socket", "FD", "Owner", "Domain", "Type", "Protocol", "Read state", "Write state"), { arrayOf(it.id, it.fd, it.owner, it.domain, it.type, it.protocol, it.readState, it.writeState) }, { it.details })
    private val charts = ObserverCharts()
    private val treeRoot = javax.swing.tree.DefaultMutableTreeNode("Applications")
    private val treeModel = javax.swing.tree.DefaultTreeModel(treeRoot)
    private val tree = JTree(treeModel)
    private data class TreeEntry(val item: ObservedTreeNode) {
        override fun toString(): String = if (item.pid.isEmpty()) item.label else "${item.label}  ${item.pid} (${item.kind})"
    }
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
        processTable = table
        val inspect = JButton("Process info").apply { addActionListener { selectedProcess()?.let { processAction(it, false) } } }
        val state = JButton("Process state").apply { addActionListener { selectedProcess()?.let { stateAction(it) } } }
        val trace = JButton("Trace process").apply { addActionListener { selectedProcess()?.let { startTrace(it) } } }
        val kill = JButton("Kill process").apply { addActionListener { selectedProcess()?.let { processAction(it, true) } } }
        table.addMouseListener(object : java.awt.event.MouseAdapter() {
            override fun mouseClicked(event: java.awt.event.MouseEvent) { if (event.clickCount == 2) selectedProcess()?.let { processAction(it, false) } }
        })
        val processPanel = JPanel(BorderLayout()).apply {
            add(JScrollPane(table), BorderLayout.CENTER)
            add(JPanel(FlowLayout(FlowLayout.LEADING)).apply { add(inspect); add(state); add(trace); add(kill) }, BorderLayout.SOUTH)
        }
        panel.add(top, BorderLayout.NORTH)
        fun selectedTreeProcess(): ObservedProcess? {
            val entry = (tree.lastSelectedPathComponent as? javax.swing.tree.DefaultMutableTreeNode)?.userObject as? TreeEntry
            val item = entry?.item ?: return null
            return processes.firstOrNull { it.pid == item.pid }.also { if (it == null) status.text = "The selected process is no longer available." }
        }
        val treeMenu = JPopupMenu().apply {
            add(JMenuItem("Process info").apply { addActionListener { selectedTreeProcess()?.let { processAction(it, false) } } })
            add(JMenuItem("Process state").apply { addActionListener { selectedTreeProcess()?.let { stateAction(it) } } })
            add(JMenuItem("Trace process").apply { addActionListener { selectedTreeProcess()?.let { startTrace(it) } } })
            add(JMenuItem("Kill process").apply { addActionListener { selectedTreeProcess()?.let { processAction(it, true) } } })
        }
        tree.addMouseListener(object : java.awt.event.MouseAdapter() {
            private fun showMenu(event: java.awt.event.MouseEvent) {
                if (!event.isPopupTrigger) return
                val path = tree.getPathForLocation(event.x, event.y) ?: return
                tree.selectionPath = path
                treeMenu.show(tree, event.x, event.y)
            }
            override fun mousePressed(event: java.awt.event.MouseEvent) = showMenu(event)
            override fun mouseReleased(event: java.awt.event.MouseEvent) = showMenu(event)
        })
        val etsTable = JTable(tableModel).apply { autoCreateRowSorter = true }
        fun showTable() {
            val selected = etsTable.selectedRow
            if (selected < 0) { status.text = "Select an ETS table first."; return }
            val item = tables.getOrNull(etsTable.convertRowIndexToModel(selected)) ?: return
            val active = synchronized(sessionLock) { session } ?: return
            val current = generation
            executor.execute {
                try {
                    val contents = active.tableContents(item)
                    ApplicationManager.getApplication().invokeLater {
                        if (current == generation && !project.isDisposed) {
                            val text = JTextArea(contents, 25, 90).apply { isEditable = false; caretPosition = 0 }
                            JOptionPane.showMessageDialog(panel, JScrollPane(text), "${item.name} — first 200 objects", JOptionPane.INFORMATION_MESSAGE)
                        }
                    }
                } catch (error: Exception) {
                    ApplicationManager.getApplication().invokeLater { if (current == generation && !project.isDisposed) status.text = "ETS inspection failed: ${error.message}" }
                }
            }
        }
        etsTable.addMouseListener(object : java.awt.event.MouseAdapter() {
            override fun mouseClicked(event: java.awt.event.MouseEvent) { if (event.clickCount == 2) showTable() }
        })
        val etsPanel = JPanel(BorderLayout()).apply {
            add(JScrollPane(etsTable), BorderLayout.CENTER)
            add(JPanel(FlowLayout(FlowLayout.LEADING)).apply { add(JButton("Show table contents").apply { addActionListener { showTable() } }) }, BorderLayout.SOUTH)
        }
        val tracePanel = JPanel(BorderLayout()).apply {
            add(JScrollPane(traceText), BorderLayout.CENTER)
            add(JPanel(FlowLayout(FlowLayout.LEADING)).apply {
                add(JButton("Stop tracing").apply { addActionListener { stopTrace() } })
                add(JButton("Clear").apply { addActionListener { traceText.text = "" } })
            }, BorderLayout.SOUTH)
        }
        val tabs = JTabbedPane().apply { addTab("Processes", processPanel); addTab("Load charts", charts); addTab("ETS", etsPanel); addTab("Ports", portsView); addTab("Sockets", socketsView); addTab("Applications", JScrollPane(tree)); addTab("Trace", tracePanel) }
        panel.add(tabs, BorderLayout.CENTER)
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
                val traceEvents = if (tracing) active.traceEvents() else ""
                ApplicationManager.getApplication().invokeLater {
                    if (current == generation && !project.isDisposed) {
                        status.text = "Connected to ${snapshot.node}. Refreshing every 5 seconds."
                        summary.text =
                            "OTP ${snapshot.otpRelease}   |   Memory: ${snapshot.memoryBytes} bytes   |   Processes: ${snapshot.processCount}"
                        if (traceEvents.isNotEmpty()) {
                            traceText.append(traceEvents + "\n")
                            if (traceText.document.length > 200_000) traceText.document.remove(0, traceText.document.length - 200_000)
                        }
                        portsView.update(snapshot.ports)
                        socketsView.update(snapshot.sockets)
                        charts.add(snapshot)
                        val expanded = tree.getExpandedDescendants(javax.swing.tree.TreePath(treeRoot.path))?.toList()
                            ?.mapNotNull { ((it.lastPathComponent as? javax.swing.tree.DefaultMutableTreeNode)?.userObject as? TreeEntry)?.item?.id }
                            ?.toSet() ?: emptySet()
                        val nodes = mutableMapOf<String, javax.swing.tree.DefaultMutableTreeNode>()
                        treeRoot.removeAllChildren()
                        snapshot.supervision.forEach { item ->
                            val child = javax.swing.tree.DefaultMutableTreeNode(TreeEntry(item))
                            (nodes[item.parentId] ?: treeRoot).add(child)
                            nodes[item.id] = child
                        }
                        treeModel.reload()
                        expanded.forEach { id -> nodes[id]?.let { tree.expandPath(javax.swing.tree.TreePath(it.path)) } }
                        tableModel.rowCount = 0
                        tables = snapshot.tables
                        snapshot.tables.forEach { tableModel.addRow(arrayOf<Any>(it.name, it.id, it.owner, it.size, it.memoryWords, it.protection, it.type)) }
                        model.rowCount = 0
                        processes = snapshot.processes
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

    private fun selectedProcess(): ObservedProcess? {
        val row = processTable.selectedRow
        if (row < 0) { status.text = "Select a process first."; return null }
        return processes.getOrNull(processTable.convertRowIndexToModel(row))
    }

    private fun processAction(process: ObservedProcess, kill: Boolean) {
        if (kill && JOptionPane.showConfirmDialog(panel, "Kill ${process.pid} (${process.name})? This can stop or restart application components.", "Kill process", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION) return
        val active = synchronized(sessionLock) { session } ?: run { status.text = "Connect to a node first."; return }
        val current = generation
        executor.execute {
            try {
                val information = if (kill) { active.killProcess(process); "Termination signal sent to ${process.pid}." } else active.inspectProcess(process)
                ApplicationManager.getApplication().invokeLater {
                    if (current == generation && !project.isDisposed) {
                        if (kill) status.text = information else {
                            val text = JTextArea(information, 25, 90).apply { isEditable = false; caretPosition = 0 }
                            JOptionPane.showMessageDialog(panel, JScrollPane(text), "Process ${process.pid}", JOptionPane.INFORMATION_MESSAGE)
                        }
                    }
                }
            } catch (error: Exception) {
                ApplicationManager.getApplication().invokeLater {
                    if (current == generation && !project.isDisposed) status.text = "Process action failed: ${error.message}"
                }
            }
        }
    }

    private fun startTrace(process: ObservedProcess) {
        val active = synchronized(sessionLock) { session } ?: return
        val current = generation
        executor.execute {
            try {
                active.startTrace(process)
                if (current != generation) { active.stopTrace(); return@execute }
                tracing = true
                ApplicationManager.getApplication().invokeLater { if (current == generation && !project.isDisposed) status.text = "Tracing ${process.pid}; events appear on the Trace tab." }
            } catch (error: Exception) {
                ApplicationManager.getApplication().invokeLater { if (current == generation && !project.isDisposed) status.text = "Tracing failed: ${error.message}" }
            }
        }
    }

    private fun stopTrace() {
        val active = synchronized(sessionLock) { session } ?: return
        tracing = false
        val current = generation
        executor.execute {
            try {
                active.stopTrace()
                ApplicationManager.getApplication().invokeLater { if (current == generation && !project.isDisposed) status.text = "Tracing stopped." }
            } catch (error: Exception) {
                ApplicationManager.getApplication().invokeLater { if (current == generation && !project.isDisposed) status.text = "Could not stop tracing: ${error.message}" }
            }
        }
    }

    private fun stateAction(process: ObservedProcess) {
        val active = synchronized(sessionLock) { session } ?: return
        val current = generation
        executor.execute {
            try {
                val state = active.processState(process)
                ApplicationManager.getApplication().invokeLater {
                    if (current == generation && !project.isDisposed) {
                        val text = JTextArea(state, 25, 90).apply { isEditable = false; caretPosition = 0 }
                        JOptionPane.showMessageDialog(panel, JScrollPane(text), "State of ${process.pid}", JOptionPane.INFORMATION_MESSAGE)
                    }
                }
            } catch (error: Exception) {
                ApplicationManager.getApplication().invokeLater { if (current == generation && !project.isDisposed) status.text = "Process state failed: ${error.message}" }
            }
        }
    }

    private fun stop() {
        generation++
        tracing = false
        traceText.text = ""
        job?.cancel(true); job = null
        synchronized(sessionLock) { session?.close(); session = null }
        connect.isEnabled = true; disconnect.isEnabled = false
        node.isEnabled = true; cookie.isEnabled = true; longNames.isEnabled = true
        cookie.text = ""
        status.text = "Disconnected."; summary.text = " "; model.rowCount = 0; tableModel.rowCount = 0; processes = emptyList(); tables = emptyList(); treeRoot.removeAllChildren(); treeModel.reload(); charts.clear(); portsView.clear(); socketsView.clear()
    }

    override fun dispose() {
        generation++
        job?.cancel(true)
        synchronized(sessionLock) { session?.close(); session = null }
        executor.shutdownNow()
    }
}
