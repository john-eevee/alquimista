package ex.alquimista.expert.observer

import java.awt.BorderLayout
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.table.DefaultTableModel

internal class ObserverDetailsTable<T>(columns: Array<String>, private val row: (T) -> Array<Any>, private val details: (T) -> String, private val key: (T) -> Any = { it.toString() }) : JPanel(BorderLayout()) {
    private val model = object : DefaultTableModel(columns, 0) {
        override fun isCellEditable(row: Int, column: Int) = false
        override fun getColumnClass(columnIndex: Int): Class<*> = (0 until rowCount).asSequence()
            .mapNotNull { getValueAt(it, columnIndex)?.javaClass }.firstOrNull() ?: String::class.java
    }
    private val table = JTable(model).apply { autoCreateRowSorter = true }
    private var items: List<T> = emptyList()
    init {
        fun showDetails() {
            val selected = table.selectedRow
            if (selected < 0) return
            val item = items.getOrNull(table.convertRowIndexToModel(selected)) ?: return
            val text = JTextArea(details(item), 25, 90).apply { isEditable = false; caretPosition = 0 }
            JOptionPane.showMessageDialog(this, JScrollPane(text), "Details", JOptionPane.INFORMATION_MESSAGE)
        }
        table.addMouseListener(object : MouseAdapter() { override fun mouseClicked(event: MouseEvent) { if (event.clickCount == 2) showDetails() } })
        add(JScrollPane(table), BorderLayout.CENTER)
        add(JPanel(java.awt.FlowLayout(java.awt.FlowLayout.LEADING)).apply {
            add(JButton("Show details").apply { addActionListener { showDetails() } })
        }, BorderLayout.SOUTH)
    }
    fun update(values: List<T>) {
        val previous = table.selectedRow.takeIf { it >= 0 }?.let { items.getOrNull(table.convertRowIndexToModel(it)) }?.let(key)
        items = values
        model.rowCount = 0
        values.forEach { model.addRow(row(it)) }
        val index = values.indexOfFirst { key(it) == previous }
        if (previous != null && index >= 0) {
            val selected = table.convertRowIndexToView(index)
            if (selected >= 0) table.setRowSelectionInterval(selected, selected)
        }
    }
    fun clear() = update(emptyList())
}
