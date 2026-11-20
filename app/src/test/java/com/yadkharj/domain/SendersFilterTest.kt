package com.yadkharj.domain

import com.yadkharj.data.Tx
import org.junit.Assert.assertEquals
import org.junit.Test

class SendersFilterTest {
    private fun tx(id: Long, sender: String) = Tx(id = id, amount = 100, isIncome = false, timestamp = 0, sender = sender, body = "b")

    @Test fun hidesBlockedSender() {
        val all = listOf(tx(1, "BANK"), tx(2, "AD"), tx(3, "BANK"))
        assertEquals(listOf(1L, 3L), Senders.visible(all, setOf("AD")).map { it.id })
    }

    @Test fun hiddenRowsReturnAfterUnblock() {
        val all = listOf(tx(1, "BANK"), tx(2, "AD"))
        assertEquals(listOf(1L), Senders.visible(all, setOf("AD")).map { it.id })
        assertEquals(listOf(1L, 2L), Senders.visible(all, emptySet()).map { it.id })
    }

    @Test fun nothingBlockedKeepsOrder() {
        val all = listOf(tx(3, "A"), tx(1, "B"))
        assertEquals(listOf(3L, 1L), Senders.visible(all, emptySet()).map { it.id })
    }
}
