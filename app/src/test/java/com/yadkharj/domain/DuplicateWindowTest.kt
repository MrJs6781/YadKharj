package com.yadkharj.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DuplicateWindowTest {
    @Test
    fun window() {
        assertFalse(SmsImport.isDuplicate(null, 1_000L))
        assertTrue(SmsImport.isDuplicate(listOf(0L), 4 * 60_000L))
        assertFalse(SmsImport.isDuplicate(listOf(0L), 6 * 60_000L))
    }
}

class DedupeTest {
    private fun t(id: Long, ts: Long, body: String, reason: String? = null) =
        com.yadkharj.data.Tx(id, 100, false, ts, "s", body, reason)

    @Test fun removesWhitespaceVariantsAndKeepsReasoned() {
        val h = 3600_000L
        val l = listOf(
            t(1, 0, "برداشت 100\nمانده 5"), t(2, 2 * h, "برداشت 100 مانده 5 "),          // یک پیامک، ساعت‌ها اختلاف
            t(3, 0, "x y", "غذا"), t(4, h, "x  y"),                                      // یکی دلیل دارد
            t(5, 0, "same"), t(6, 30 * h, "same")                                          // روزهای متفاوت: واقعی
        )
        assertEquals(setOf(2L, 4L), Dedupe.duplicates(l).toSet())
    }
}
