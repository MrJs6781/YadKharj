package com.yadkharj.domain

import com.yadkharj.data.Tx
import org.junit.Assert.assertEquals
import org.junit.Test

class TransfersTest {
    private val t0 = 1_000_000_000L
    private fun tx(id: Long, inc: Boolean, card: String?, at: Long = t0, amt: Long = 500) =
        Tx(id = id, amount = amt, isIncome = inc, timestamp = at, sender = "s", body = "b", card = card)

    @Test fun pairDetected() {
        val all = listOf(tx(1, false, "1111"), tx(2, true, "2222", t0 + 60_000), tx(3, false, "1111", t0, 9))
        assertEquals(setOf(1L, 2L), Transfers.ids(all))
        assertEquals(listOf(3L), Transfers.without(all).map { it.id })
    }

    @Test fun differentAmountNotPaired() {
        assertEquals(emptySet<Long>(), Transfers.ids(listOf(tx(1, false, "1"), tx(2, true, "2", amt = 501))))
    }

    @Test fun sameOrNullCardNotPaired() {
        assertEquals(emptySet<Long>(), Transfers.ids(listOf(tx(1, false, "1"), tx(2, true, "1"))))
        assertEquals(emptySet<Long>(), Transfers.ids(listOf(tx(1, false, null), tx(2, true, "2"))))
    }

    @Test fun outsideWindowNotPaired() {
        assertEquals(emptySet<Long>(), Transfers.ids(listOf(tx(1, false, "1"), tx(2, true, "2", t0 + 10 * 60_000 + 1))))
        assertEquals(setOf(1L, 2L), Transfers.ids(listOf(tx(1, false, "1"), tx(2, true, "2", t0 + 10 * 60_000))))
    }

    @Test fun oneToOneClosestFirst() {
        val all = listOf(tx(1, false, "1"), tx(2, true, "2", t0 + 120_000), tx(3, true, "2", t0 + 30_000))
        assertEquals(setOf(1L, 3L), Transfers.ids(all))
        val b = listOf(tx(1, false, "1"), tx(2, false, "1"), tx(3, true, "2"))
        assertEquals(2, Transfers.ids(b).size)
    }
}
