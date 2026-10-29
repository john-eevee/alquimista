package dev.alquimista.expert.observer

import org.junit.Assert.assertEquals
import org.junit.Test
import javax.swing.JScrollPane
import javax.swing.JTable
import javax.swing.SwingUtilities

class ObserverDetailsTableTest {
    private data class Item(val id: String, val value: Int)
    @Test fun `refresh preserves selected item after values reorder`() {
        SwingUtilities.invokeAndWait {
            val view = ObserverDetailsTable<Item>(arrayOf("ID", "Value"), { arrayOf(it.id, it.value) }, { it.id }, { it.id })
            val table = view.components.filterIsInstance<JScrollPane>().single().viewport.view as JTable
            view.update(listOf(Item("a", 1), Item("b", 2)))
            table.setRowSelectionInterval(1, 1)
            view.update(listOf(Item("b", 3), Item("a", 4)))
            assertEquals("b", table.getValueAt(table.selectedRow, 0))
            view.update(listOf(Item("a", 4)))
            assertEquals(-1, table.selectedRow)
        }
    }
}
