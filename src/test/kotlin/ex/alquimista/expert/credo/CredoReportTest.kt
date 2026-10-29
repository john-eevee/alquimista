package ex.alquimista.expert.credo

import org.junit.Assert.*
import org.junit.Test

class CredoReportTest {
    @Test
    fun `reads issues after compiler output and tolerates missing columns`() {
        val report = CredoReport.parse("""Compiling 1 file (.ex)
            |{"issues":[{"filename":"lib/a.ex","line_no":3,"column":2,"column_end":5,"message":"Avoid this","category":"warning","check":"Credo.Check.Warning.Test"},
            |{"filename":"lib/b.ex","line_no":1,"column":null,"message":"Document this","category":"readability"}]}""".trimMargin())
        assertNotNull(report)
        assertEquals(2, report!!.size)
        assertEquals(CredoIssue("lib/a.ex", 3, 2, 5, "Avoid this", "warning", "Credo.Check.Warning.Test"), report[0])
        assertEquals(null, report[1].column)
    }

    @Test
    fun `distinguishes failed commands from an empty report`() {
        assertNull(CredoReport.parse("** (Mix) task credo not found"))
        assertEquals(emptyList<CredoIssue>(), CredoReport.parse("{\"issues\":[]}"))
    }

    @Test
    fun `ignores malformed issues without discarding valid ones`() {
        val result = CredoReport.parse("""{"issues":[{"message":"missing location"},{"filename":"a.ex","line_no":1,"message":"ok"}]}""")
        assertEquals(1, result!!.size)
        assertEquals("ok", result.single().message)
    }
}
