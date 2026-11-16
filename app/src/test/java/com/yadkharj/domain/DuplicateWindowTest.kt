package com.yadkharj.domain

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
