package dev.yaee.expert.testing

import org.junit.Assert.assertEquals
import org.junit.Test

class MixTaskTest {
    @Test
    fun `defines the main Mix development actions`() {
        assertEquals(listOf("test"), MixTask.TEST)
        assertEquals(listOf("format"), MixTask.FORMAT)
        assertEquals(listOf("deps.get"), MixTask.DEPENDENCIES)
    }
}
