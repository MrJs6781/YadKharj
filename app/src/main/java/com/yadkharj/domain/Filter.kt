package com.yadkharj.domain

import com.yadkharj.data.Tx
import java.time.LocalDate
import java.time.ZoneId

/** فیلتر لیست تراکنش‌ها. مبلغ‌ها به «ریال» هستند؛ days = 0 یعنی همهٔ تاریخ‌ها. */
data class TxFilter(
    val query: String = "",
    val days: Int = 0,
    val minRial: Long? = null,
    val maxRial: Long? = null,
    val onlyPending: Boolean = false
) {
    val active get() = query.isNotBlank() || days > 0 || minRial != null || maxRial != null

    fun apply(all: List<Tx>, today: LocalDate = LocalDate.now()): List<Tx> {
        val q = SmsParser.normalize(query).trim()
        val from = if (days > 0) today.minusDays(days - 1L).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() else Long.MIN_VALUE
        return all.filter {
            (!onlyPending || it.reason == null) &&
                it.timestamp >= from &&
                (minRial == null || it.amount >= minRial) &&
                (maxRial == null || it.amount <= maxRial) &&
                (q.isEmpty() || (it.reason?.let { r -> SmsParser.normalize(r).contains(q, ignoreCase = true) } ?: false))
        }
    }

    companion object {
        /** ورودی کاربر (ممکن است ارقام فارسی داشته باشد) به ریال؛ خالی/نامعتبر = null. */
        fun rial(input: String, toman: Boolean): Long? =
            SmsParser.normalize(input).filter { it.isDigit() }.toLongOrNull()?.let { if (toman) it * 10 else it }
    }
}
