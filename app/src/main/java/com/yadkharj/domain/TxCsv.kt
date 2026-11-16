package com.yadkharj.domain

import com.yadkharj.data.Tx

/** خروجی/ورودی CSV (UTF-8 با BOM تا اکسل فارسی را درست بخواند). همین فایل هم پشتیبان است هم برای بازیابی. */
object TxCsv {
    private const val HEADER = "timestamp,amount,type,sender,card,balance,reason,body"

    private fun q(s: String) = "\"" + s.replace("\"", "\"\"") + "\""

    fun write(list: List<Tx>): String = buildString {
        append('﻿').append(HEADER).append("\r\n")
        for (t in list.sortedBy { it.timestamp }) {
            append(
                listOf(
                    t.timestamp, t.amount, if (t.isIncome) "in" else "out", q(t.sender),
                    t.card.orEmpty(), t.balance ?: "", q(t.reason.orEmpty()), q(t.body)
                ).joinToString(",")
            ).append("\r\n")
        }
    }

    /** ردیف‌های نامعتبر نادیده گرفته می‌شوند. id همیشه ۰ است (دیتابیس خودش می‌دهد). */
    fun read(text: String): List<Tx> =
        rows(text.removePrefix("﻿")).drop(1).mapNotNull { c ->
            if (c.size < 8) return@mapNotNull null
            Tx(
                amount = c[1].toLongOrNull() ?: return@mapNotNull null,
                isIncome = c[2] == "in",
                timestamp = c[0].toLongOrNull() ?: return@mapNotNull null,
                sender = c[3], body = c[7],
                reason = c[6].ifBlank { null },
                card = c[4].ifBlank { null },
                balance = c[5].toLongOrNull()
            )
        }

    private fun rows(s: String): List<List<String>> {
        val out = mutableListOf<List<String>>()
        var row = mutableListOf<String>()
        val cell = StringBuilder()
        var quoted = false
        var i = 0
        fun endRow() { row += cell.toString(); cell.clear(); if (row.any { it.isNotEmpty() }) out += row; row = mutableListOf() }
        while (i < s.length) {
            val ch = s[i]
            when {
                quoted && ch == '"' && s.getOrNull(i + 1) == '"' -> { cell.append('"'); i++ }
                ch == '"' -> quoted = !quoted
                quoted -> cell.append(ch)
                ch == ',' -> { row += cell.toString(); cell.clear() }
                ch == '\n' -> endRow()
                ch == '\r' -> {}
                else -> cell.append(ch)
            }
            i++
        }
        if (cell.isNotEmpty() || row.isNotEmpty()) endRow()
        return out
    }
}
