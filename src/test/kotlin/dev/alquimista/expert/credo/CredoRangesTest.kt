package dev.alquimista.expert.credo

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

    @Test
    fun `start columns count combining characters as separate code points`() {
        assertEquals(2 to 3, CredoRanges.offsets("e\u0301x", 1, 3, 4))
    }

    @Test
    fun `trigger length counts a combining sequence as one grapheme`() {
        assertEquals(0 to 2, CredoRanges.offsets("e\u0301x", 1, 1, 2))
    }
}
