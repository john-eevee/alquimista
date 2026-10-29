package ex.alquimista.expert.testing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExUnitTestDetectorTest {
    @Test
    fun `detects describe groups and test modules as runnable targets`() {
        val source = """defmodule ExampleTest do
  use ExUnit.Case
  describe "parsing" do
    test "one" do
      assert true
    end

    test "two" do
      assert true
    end
  end
end
"""
        assertEquals(listOf(
            ExUnitTarget("ExampleTest", 1, ExUnitTargetKind.MODULE),
            ExUnitTarget("parsing", 3, ExUnitTargetKind.DESCRIBE),
            ExUnitTarget("parsing: one", 4, ExUnitTargetKind.TEST),
            ExUnitTarget("parsing: two", 8, ExUnitTargetKind.TEST),
        ), ExUnitTestDetector.targets(source))
    }
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
    fun `preserves hash characters inside quoted test names but strips comments`() {
        val source = "test \"parses # sections\" do # this is a comment\n  :ok\nend\n"

        assertEquals(listOf(DetectedExUnitTest("parses # sections", 1)), ExUnitTestDetector.detect(source))
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
