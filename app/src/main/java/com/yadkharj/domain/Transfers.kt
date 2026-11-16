package com.yadkharj.domain

import com.yadkharj.data.Tx
import kotlin.math.abs

/** انتقال بین کارت‌های خودم: یک برداشت و یک واریز هم‌مبلغ روی دو کارت متفاوت، حداکثر ۱۰ دقیقه فاصله. */
object Transfers {
    const val WINDOW_MS = 10 * 60_000L

    fun ids(all: List<Tx>): Set<Long> {
        val out = HashSet<Long>()
        val incomes = all.filter { it.isIncome && it.card != null }.toMutableList()
        // ponytail: greedy per expense, not globally optimal; fine for 1:1 realistic cases
        for (e in all.filter { !it.isIncome && it.card != null }.sortedBy { it.timestamp }) {
            val m = incomes
                .filter { it.amount == e.amount && it.card != e.card && abs(it.timestamp - e.timestamp) <= WINDOW_MS }
                .minByOrNull { abs(it.timestamp - e.timestamp) } ?: continue
            incomes.remove(m)
            out += e.id; out += m.id
        }
        return out
    }

    fun without(all: List<Tx>): List<Tx> = ids(all).let { t -> if (t.isEmpty()) all else all.filter { it.id !in t } }
}
