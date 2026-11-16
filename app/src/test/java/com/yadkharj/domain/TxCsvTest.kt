package com.yadkharj.domain

import com.yadkharj.data.Tx
import org.junit.Assert.assertEquals
import org.junit.Test

class TxCsvTest {
    @Test fun roundTripKeepsCommasQuotesAndNewlines() {
        val l = listOf(
            Tx(1, 1_250_000, false, 1000L, "BankX", "برداشت 1,250,000\nمانده \"8,400,000\"", "غذا, شام", "1234", 8_400_000),
            Tx(2, 5, true, 2000L, "", "b", null)
        )
        val back = TxCsv.read(TxCsv.write(l))
        assertEquals(l.map { it.copy(id = 0) }, back)
    }
}
