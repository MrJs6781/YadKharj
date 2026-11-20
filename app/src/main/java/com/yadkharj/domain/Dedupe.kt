package com.yadkharj.domain

import android.content.Context
import com.yadkharj.data.AppDb
import com.yadkharj.data.Tx
import com.yadkharj.widget.TxWidget

/** پاک‌سازی تراکنش‌های دوتایی که قبل از اصلاح ضدتکرار ثبت شده‌اند (یک بار). */
object Dedupe {
    /**
     * شناسهٔ تراکنش‌هایی که باید پاک شوند: داخل هر خوشه از پیامک‌های هم‌متن (فاصلهٔ کمتر از ۲۴ ساعت)،
     * اگه بعضی‌هاشون دلیل دارن همه‌ٔ بی‌دلیل‌ها پاک می‌شن (آنهایی که دلیل دارن به احتمال زیاد واقعی‌اند)، وگرنه فقط اولی می‌ماند.
     */
    fun duplicates(all: List<Tx>): List<Long> {
        val out = mutableListOf<Long>()
        for ((_, group) in all.groupBy { SmsImport.key(it.body) }) {
            if (group.size < 2) continue
            val sorted = group.sortedBy { it.timestamp }
            var cluster = mutableListOf(sorted[0])
            fun flush() {
                if (cluster.size > 1) {
                    val withReason = cluster.filter { it.reason != null }
                    out += (if (withReason.isNotEmpty()) cluster - withReason.toSet() else cluster.drop(1)).map { it.id }
                }
            }
            for (t in sorted.drop(1)) {
                if (t.timestamp - cluster.first().timestamp < SmsImport.IMPORT_SAME_MS) cluster += t
                else { flush(); cluster = mutableListOf(t) }
            }
            flush()
        }
        return out
    }

    /** هر بار صدا زده می‌شود (شروع برنامه، بعد از واردکردن و بازیابی)، چون پشتیبان CSV هم ممکن است دوتایی داشته باشد. تعداد پاک‌شده را برمی‌گرداند. */
    suspend fun clean(c: Context): Int {
        val dao = AppDb.get(c).dao()
        // تراکنش‌های پنهانِ «بانک نیست» در خوشهٔ تکرار شرکت نمی‌کنند تا نسخهٔ دیدنی حذف نشود.
        val ids = duplicates(dao.all().filter { it.sender !in Senders.blocked(c) })
        if (ids.isNotEmpty()) { dao.removeIds(ids); TxWidget.refresh(c) }
        return ids.size
    }
}
