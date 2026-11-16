package com.yadkharj.domain

import com.yadkharj.data.Tx
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class BudgetTest {
    @Test fun statusSumsPerReasonAndTotal() {
        val d = LocalDate.of(2025, 3, 25)
        val ts = d.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() + 1000
        val p = Period(d.minusDays(1), d.plusDays(1), "x", false)
        val tx = listOf(
            Tx(1, 100, false, ts, "s", "b", "غذا"), Tx(2, 50, false, ts, "s", "b", "قبض"), Tx(3, 999, true, ts, "s", "b", "غذا")
        )
        val st = Budgets.status(mapOf(Budgets.TOTAL to 300L, "غذا" to 100L), tx, p)
        assertEquals(listOf("کل ماه" to 150L, "غذا" to 100L), st.map { it.label to it.spent })
        assertEquals(1.0, st[1].ratio, 0.0)
    }
}

class CompareTest {
    @Test fun percentChange() {
        val z = ZoneId.systemDefault()
        val a = LocalDate.of(2025, 3, 1)
        val b = LocalDate.of(2025, 4, 1)
        fun ts(d: LocalDate) = d.atStartOfDay(z).toInstant().toEpochMilli() + 1000
        val prev = Period(a, b, "", false)
        val cur = Period(b, b.plusDays(30), "", false)
        val tx = listOf(Tx(1, 100, false, ts(a), "s", "b", "غذا"), Tx(2, 120, false, ts(b), "s", "b", "غذا"))
        val d = Reports.compare(tx, cur, prev)
        assertEquals(listOf("کل خرج", "غذا"), d.map { it.label })
        assertEquals(20, d[1].pct)
    }
}
