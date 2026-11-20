package com.yadkharj.domain

import android.content.Context
import com.yadkharj.data.AppDb
import com.yadkharj.data.Tx
import java.time.LocalDate
import java.time.ZoneId

/** سقف ماهانهٔ خرج (ریال). کلید خالی = کل ماه، وگرنه متن دلیل. در SharedPreferences ذخیره می‌شود (تعدادش کم است). */
object Budgets {
    const val WARN = 0.8
    const val TOTAL = ""

    data class Status(val label: String, val spent: Long, val limit: Long) {
        val ratio get() = if (limit > 0) spent.toDouble() / limit else 0.0
    }

    private fun sp(c: Context) = c.getSharedPreferences("budgets", Context.MODE_PRIVATE)

    fun load(c: Context): Map<String, Long> =
        sp(c).getString("v", "").orEmpty().lines().filter { '\t' in it }
            .associate { it.substringBefore('\t') to (it.substringAfter('\t').toLongOrNull() ?: 0L) }
            .filterValues { it > 0 }

    fun save(c: Context, m: Map<String, Long>) =
        sp(c).edit().putString("v", m.entries.joinToString("\n") { "${it.key.trim()}\t${it.value}" }).apply()

    /** خرج‌های بازهٔ [p] را با سقف‌ها مقایسه می‌کند؛ کل ماه اول می‌آید. */
    fun status(budgets: Map<String, Long>, all: List<Tx>, p: Period): List<Status> {
        val zone = ZoneId.systemDefault()
        val s = p.start.atStartOfDay(zone).toInstant().toEpochMilli()
        val e = p.end.atStartOfDay(zone).toInstant().toEpochMilli()
        val out = Transfers.without(all).filter { !it.isIncome && it.timestamp in s until e }
        return budgets.map { (k, limit) ->
            val spent = if (k == TOTAL) out.sumOf { it.amount } else out.filter { it.reason?.trim() == k }.sumOf { it.amount }
            Status(if (k == TOTAL) "کل ماه" else k, spent, limit)
        }.sortedWith(compareBy({ it.label != "کل ماه" }, { -it.ratio }))
    }

    /** بعد از ثبت دلیل (یا ورود برداشت جدید) صدا زده شود؛ هر سطح (۸۰٪ / ۱۰۰٪) در هر ماه فقط یک بار اعلام می‌شود. */
    suspend fun check(c: Context, reason: String?, today: LocalDate = LocalDate.now()) {
        val budgets = load(c)
        if (budgets.isEmpty()) return
        val all = AppDb.get(c).dao().all().filter { it.sender !in Senders.blocked(c) }
        val month = Periods.month(0, today)
        val key = reason?.trim()
        for (st in status(budgets, all, month)) {
            if (st.label != "کل ماه" && st.label != key) continue
            val level = when { st.ratio >= 1 -> 100; st.ratio >= WARN -> 80; else -> continue }
            val mark = "${month.start}|${st.label}"
            val p = sp(c)
            if (p.getInt(mark, 0) >= level) continue
            p.edit().putInt(mark, level).apply()
            val toman = Prefs.toman(c)
            val txt = "${Fmt.money(st.spent, toman)} از ${Fmt.money(st.limit, toman)} ${Fmt.unit(toman)}"
            Notifier.budget(c, st.label, if (level == 100) "سقف «${st.label}» رد شد — $txt" else "به سقف «${st.label}» نزدیکی — $txt")
        }
    }
}
