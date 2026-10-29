package ex.alquimista.expert.testing

import org.junit.Assert.*
import org.junit.Test

class ExUnitStacktraceTest {
    @Test
    fun `extracts Elixir source paths and lines from stack traces`() {
        val result = ExUnitStacktrace.parse("    test/my example_test.exs:17: ExampleTest.test/1\n")!!
        assertEquals("test/my example_test.exs", result.path)
        assertEquals(17, result.line)
        assertEquals("test/my example_test.exs:17", "    test/my example_test.exs:17: ExampleTest.test/1\n".substring(result.start, result.end))
    }

    @Test
    fun `ignores unrelated text and invalid line numbers`() {
        assertNull(ExUnitStacktrace.parse("ordinary output\n"))
        assertNull(ExUnitStacktrace.parse("    test/example.exs:0: bad\n"))
    }
}
