package com.yadkharj.domain

import com.yadkharj.data.Tx
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class YearlyTest {
    private val today = LocalDate.of(2025, 6, 1)

    @Test fun yearBoundaries() {
        val p = Periods.year(0, today)
        assertEquals(LocalDate.of(2025, 3, 21), p.start)
        assertEquals(LocalDate.of(2026, 3, 21), p.end)
        assertEquals("۱۴۰۴", p.title)
        val prev = Periods.year(-1, today)
        assertEquals(p.start, prev.end)
        assertEquals(Jalali.Date(1403, 1, 1), Jalali.of(prev.start))
        assertEquals(Periods.year(1, today).start, p.end)
    }

    @Test fun monthlyBarsAndAverage() {
        val p = Periods.year(0, today)
        fun ts(d: LocalDate) = d.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() + 1000
        val tx = listOf(
            Tx(1, 100, false, ts(LocalDate.of(2025, 3, 21)), "s", "b", "x"),
            Tx(2, 300, false, ts(LocalDate.of(2025, 4, 25)), "s", "b", "x"),
            Tx(3, 50, true, ts(LocalDate.of(2026, 3, 20)), "s", "b", "x"),
            Tx(4, 999, false, ts(LocalDate.of(2026, 3, 21)), "s", "b", "x")
        )
        val bars = Reports.monthlyBars(tx, p)
        assertEquals(12, bars.size)
        assertEquals(100L, bars[0].expense)
        assertEquals(300L, bars[1].expense)
        assertEquals(50L, bars[11].income)
        assertEquals(200L, Reports.monthlyAverage(bars))
    }
}
