package com.yadkharj.domain

import com.yadkharj.data.Tx
import java.time.LocalDate
import java.time.ZoneId

/** سال شمسی: از اول فروردین تا اول فروردین سال بعد. offset = 0 سال جاری. */
fun Periods.year(offset: Int, today: LocalDate): Period {
    val base = -(Jalali.of(today).m - 1) + 12 * offset // فاصلهٔ ماهِ فروردینِ آن سال از ماه جاری
    val s = month(base, today).start
    val e = month(base + 12, today).start
    return Period(s, e, Fmt.digits(Jalali.of(s).y.toString()), false)
}

/** ۱۲ میله (یکی برای هر ماه شمسی)؛ برچسب = شمارهٔ ماه. */
fun Reports.monthlyBars(all: List<Tx>, year: Period): List<DayBar> {
    val z = ZoneId.systemDefault()
    var s = year.start
    return (1..12).map { i ->
        var n = s.plusDays(28)
        while (Jalali.of(n).d != 1) n = n.plusDays(1)
        val a = s.atStartOfDay(z).toInstant().toEpochMilli()
        val b = n.atStartOfDay(z).toInstant().toEpochMilli()
        val l = all.filter { it.timestamp in a until b }
        s = n
        DayBar(Fmt.digits(i.toString()), l.filter { it.isIncome }.sumOf { it.amount }, l.filter { !it.isIncome }.sumOf { it.amount })
    }
}

/** میانگین خرج ماهانه روی ماه‌هایی که خرجی داشته‌اند. ponytail: ماه‌های خالیِ وسط سال حساب نمی‌شوند. */
fun Reports.monthlyAverage(bars: List<DayBar>): Long {
    val m = bars.filter { it.expense > 0 }
    return if (m.isEmpty()) 0 else m.sumOf { it.expense } / m.size
}
