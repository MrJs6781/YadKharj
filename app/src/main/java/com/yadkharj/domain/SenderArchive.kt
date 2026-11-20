package com.yadkharj.domain

import android.content.Context

/**
 * پیامک‌هایی که از فرستنده‌های «بانک نیست» می‌آیند؛ چون تراکنش نمی‌شوند، برای بازبینی و تصمیمِ بازگرداندن نگه داشته می‌شوند.
 * حداکثر [MAX] پیام آخر هر فرستنده در SharedPreferences می‌ماند.
 */
object SenderArchive {
    data class Msg(val time: Long, val body: String)

    private const val MAX = 50
    private const val MAX_BODY = 1500
    private val RS = 30.toChar().toString()
    private val FS = 31.toChar().toString()

    private fun sp(c: Context) = c.getSharedPreferences("senderlog", Context.MODE_PRIVATE)
    private fun key(sender: String) = "s|$sender"

    fun add(c: Context, sender: String, body: String, time: Long = System.currentTimeMillis()) {
        val msg = Msg(time, sanitize(body).take(MAX_BODY))
        val all = (listOf(msg) + load(c, sender)).take(MAX)
        sp(c).edit().putString(key(sender), encode(all)).apply()
    }

    fun load(c: Context, sender: String): List<Msg> = decode(sp(c).getString(key(sender), "").orEmpty())

    fun clear(c: Context, sender: String) = sp(c).edit().remove(key(sender)).apply()

    fun encode(list: List<Msg>): String = list.joinToString(RS) { "${it.time}$FS${sanitize(it.body)}" }

    fun decode(raw: String): List<Msg> =
        raw.split(RS).mapNotNull {
            val p = it.split(FS, limit = 2)
            if (p.size != 2) null else p[0].toLongOrNull()?.let { ms -> Msg(ms, p[1]) }
        }

    private fun sanitize(s: String) = s.replace(RS, " ").replace(FS, " ")
}
