package dev.yaee.expert.credo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CredoRangesTest {
    @Test
    fun `maps one based unicode columns into UTF-16 offsets`() {
        val range = CredoRanges.offsets("first\n😀abc\n", 2, 2, 4)
        assertEquals(8 to 10, range)
    }

    @Test
    fun `handles missing and out of range locations`() {
        assertEquals(2 to 5, CredoRanges.offsets("a\nabc", 2, null, null))
        assertEquals(4 to 5, CredoRanges.offsets("a\nabc", 2, 100, null))
        assertNull(CredoRanges.offsets("a", 2, 1, 2))
    }
}
