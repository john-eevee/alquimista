package dev.alquimista.expert.testing

import org.junit.Assert.assertEquals
import org.junit.Test

class MixTestCommandTest {
    @Test
    fun `builds a Mix target for a specific ExUnit source line`() {
        assertEquals(
            listOf("test", "test/my_app/example_test.ex:17"),
            MixTestCommand.arguments("test/my_app/example_test.ex", 17),
        )
    }

    @Test
    fun `runs a test file when no line is selected`() {
        assertEquals(
            listOf("test", "test/my_app/example_test.ex"),
            MixTestCommand.arguments("test/my_app/example_test.ex", null),
        )
    }
}
