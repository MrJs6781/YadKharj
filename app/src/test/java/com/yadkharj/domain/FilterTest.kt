package com.yadkharj.domain

import com.yadkharj.data.Tx
import org.junit.Assert.assertEquals
import org.junit.Test

class FilterTest {
    private fun t(id: Long, amt: Long, r: String?) = Tx(id, amt, false, System.currentTimeMillis(), "s", "b", r)
    private val l = listOf(t(1, 10_000, "غذا"), t(2, 500_000, "قبض"), t(3, 20_000, null))

    @Test fun query() = assertEquals(listOf(1L), TxFilter(query = "غذ").apply(l).map { it.id })
    @Test fun min() = assertEquals(listOf(2L), TxFilter(minRial = 100_000).apply(l).map { it.id })
    @Test fun pending() = assertEquals(listOf(3L), TxFilter(onlyPending = true).apply(l).map { it.id })
    @Test fun tomanInput() = assertEquals(5000L, TxFilter.rial("۵۰۰", true))
}
