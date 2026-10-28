package dev.yaee.expert.testing

import org.junit.Assert.assertEquals
import org.junit.Test
import com.intellij.openapi.actionSystem.ActionUpdateThread

class MixTaskTest {
    @Test
    fun `Mix actions update on background thread`() {
        listOf(RunMixTestsAction(), MixFormatAction(), MixDepsGetAction()).forEach {
            assertEquals(ActionUpdateThread.BGT, it.actionUpdateThread)
        }
    }

    @Test
    fun `defines the main Mix development actions`() {
        assertEquals(listOf("test"), MixTask.TEST)
        assertEquals(listOf("format"), MixTask.FORMAT)
        assertEquals(listOf("deps.get"), MixTask.DEPENDENCIES)
    }
}
