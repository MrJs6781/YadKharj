package com.yadkharj.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SenderArchiveTest {
    private fun msg(t: Long, b: String) = SenderArchive.Msg(t, b)

    @Test
    fun roundTrip() {
        val list = listOf(msg(1000, "برداشت ۱۰۰,۰۰۰ ریال"), msg(2000, "سلام\nدنیا"))
        assertEquals(list, SenderArchive.decode(SenderArchive.encode(list)))
    }

    @Test
    fun ignoresGarbage() {
        assertTrue(SenderArchive.decode("").isEmpty())
        assertTrue(SenderArchive.decode("not-a-time\u001fbody").isEmpty())
        assertTrue(SenderArchive.decode("123").isEmpty())
    }

    @Test
    fun sanitizesSeparators() {
        val raw = "a\u001eb\u001fc"
        val decoded = SenderArchive.decode(SenderArchive.encode(listOf(msg(5, raw))))
        assertEquals(1, decoded.size)
        assertEquals(5L, decoded[0].time)
        assertTrue('\u001e' !in decoded[0].body)
        assertTrue('\u001f' !in decoded[0].body)
    }
}
