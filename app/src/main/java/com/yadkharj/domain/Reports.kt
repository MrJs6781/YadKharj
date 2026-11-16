package com.yadkharj.domain

import com.yadkharj.data.Tx
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

data class Period(val start: LocalDate, val end: LocalDate, val title: String, val isWeek: Boolean)
data class DayBar(val label: String, val income: Long, val expense: Long)
data class Slice(val label: String, val amount: Long, val unlabeled: Boolean)
data class Report(
    val period: Period,
    val income: Long,
    val expense: Long,
    val bars: List<DayBar>,
    val expenseSlices: List<Slice>,
    val incomeSlices: List<Slice>,
    val count: Int,
    val pendingCount: Int
)

object Periods {
    /** هفته از شنبه شروع می‌شود. offset = 0 هفتهٔ جاری، -1 هفتهٔ قبل. */
    fun week(offset: Int, today: LocalDate): Period {
        val start = today.minusDays(Jalali.weekdayIndex(today).toLong()).plusWeeks(offset.toLong())
        val end = start.plusDays(7)
        val a = Jalali.of(start)
        val b = Jalali.of(end.minusDays(1))
        val title = if (a.m == b.m) {
            "${Fmt.digits(a.d.toString())} تا ${Fmt.digits(b.d.toString())} ${Jalali.months[a.m - 1]}"
        } else {
            "${Fmt.digits(a.d.toString())} ${Jalali.months[a.m - 1]} تا ${Fmt.digits(b.d.toString())} ${Jalali.months[b.m - 1]}"
        }
        return Period(start, end, title, true)
    }

    fun month(offset: Int, today: LocalDate): Period {
        var first = monthStart(today)
        var k = offset
        while (k < 0) { first = monthStart(first.minusDays(1)); k++ }
        while (k > 0) { first = nextMonthStart(first); k-- }
        val j = Jalali.of(first)
        return Period(first, nextMonthStart(first), "${Jalali.months[j.m - 1]} ${Fmt.digits(j.y.toString())}", false)
    }

    private fun monthStart(d: LocalDate): LocalDate = d.minusDays((Jalali.of(d).d - 1).toLong())

    private fun nextMonthStart(first: LocalDate): LocalDate {
        var d = first.plusDays(28)
        while (Jalali.of(d).d != 1) d = d.plusDays(1)
        return d
    }
}

object Reports {
    fun build(all: List<Tx>, p: Period): Report {
        val zone = ZoneId.systemDefault()
        val s = p.start.atStartOfDay(zone).toInstant().toEpochMilli()
        val e = p.end.atStartOfDay(zone).toInstant().toEpochMilli()
        val list = all.filter { it.timestamp in s until e }
        val byDay = list.groupBy { Instant.ofEpochMilli(it.timestamp).atZone(zone).toLocalDate() }
        val days = ChronoUnit.DAYS.between(p.start, p.end).toInt()
        val bars = (0 until days).map { i ->
            val d = p.start.plusDays(i.toLong())
            val l = byDay[d].orEmpty()
            val label = if (p.isWeek) Jalali.weekdaysShort[i] else Fmt.digits(Jalali.of(d).d.toString())
            DayBar(label, l.filter { it.isIncome }.sumOf { it.amount }, l.filter { !it.isIncome }.sumOf { it.amount })
        }
        val inc = list.filter { it.isIncome }
        val out = list.filter { !it.isIncome }
        return Report(
            period = p,
            income = inc.sumOf { it.amount },
            expense = out.sumOf { it.amount },
            bars = bars,
            expenseSlices = slices(out),
            incomeSlices = slices(inc),
            count = list.size,
            pendingCount = list.count { it.reason.isNullOrBlank() }
        )
    }

    private fun slices(list: List<Tx>): List<Slice> {
        val labeled = list.filter { !it.reason.isNullOrBlank() }
            .groupBy { it.reason!!.trim() }
            .map { (k, v) -> Slice(k, v.sumOf { t -> t.amount }, false) }
            .sortedByDescending { it.amount }
            .take(7)
        val un = list.filter { it.reason.isNullOrBlank() }.sumOf { it.amount }
        return if (un > 0) labeled + Slice("بدون دلیل", un, true) else labeled
    }
}
