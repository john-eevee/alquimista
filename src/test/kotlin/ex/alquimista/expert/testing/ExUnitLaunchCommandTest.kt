package ex.alquimista.expert.testing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ExUnitLaunchCommandTest {
    @Test
    fun `launches all tests through the bundled formatter`() {
        assertEquals(listOf("run", "--no-start", "--no-compile", "-r", "/tmp/formatter.exs",
            "-e", "Mix.Task.run(\"test\", System.argv())", "--", "--formatter", "Alquimista.ExUnitFormatter"),
            ExUnitLaunchCommand.arguments("/tmp/formatter.exs", null, null))
    }

    @Test
    fun `launches a specific test without using a shell`() {
        assertEquals("test/my app_test.exs:42", ExUnitLaunchCommand.arguments("/tmp/f.exs", "test/my app_test.exs", 42).last())
        assertThrows(IllegalArgumentException::class.java) { ExUnitLaunchCommand.arguments("/tmp/f.exs", null, 1) }
        assertThrows(IllegalArgumentException::class.java) { ExUnitLaunchCommand.arguments("/tmp/f.exs", "test/a.exs", 0) }
    }

    @Test
    fun `selects an ExUnit module and its containing test file`() {
        val args = ExUnitLaunchCommand.arguments("/tmp/f.exs", "test/a_test.exs", null, "ExampleTest")
        assertEquals(listOf("--only", "module:ExampleTest", "test/a_test.exs"), args.takeLast(3))
        assertThrows(IllegalArgumentException::class.java) {
            ExUnitLaunchCommand.arguments("/tmp/f.exs", null, null, "ExampleTest")
        }
    }

    @Test
    fun `rerun failed adds the Mix failed filter`() {
        val args = ExUnitLaunchCommand.arguments("/tmp/f.exs", "test/a.exs", null, failedOnly = true)
        assertEquals(listOf("--failed", "test/a.exs"), args.takeLast(2))
    }
}
