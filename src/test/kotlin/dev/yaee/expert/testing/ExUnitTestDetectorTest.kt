package dev.yaee.expert.testing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExUnitTestDetectorTest {
    @Test
    fun `detects named tests with describe context and one based lines`() {
        val source = """defmodule ExampleTest do
  describe "parsing" do
    test "accepts input" do
      assert true
    end
  end
end
"""

        assertEquals(
            listOf(DetectedExUnitTest("parsing: accepts input", 3)),
            ExUnitTestDetector.detect(source),
        )
    }

    @Test
    fun `detects atom tests and ignores comments and non test functions`() {
        val source = """# test "commented out" do
 def test_helper do
   :ok
 end
 test :handles_edge_case do
   :ok
 end
"""

        assertEquals(
            listOf(DetectedExUnitTest("handles_edge_case", 5)),
            ExUnitTestDetector.detect(source),
        )
    }

    @Test
    fun `recognizes conventional ExUnit test files`() {
        assertTrue(ExUnitTestDetector.isTestFile("example_test.exs"))
        assertTrue(ExUnitTestDetector.isTestFile("example_test.ex"))
        org.junit.Assert.assertFalse(ExUnitTestDetector.isTestFile("example.ex"))
    }

    @Test
    fun `returns no tests for ordinary source files`() {
        assertTrue(ExUnitTestDetector.detect("defmodule Example do\n  def run, do: :ok\nend\n").isEmpty())
    }
}
