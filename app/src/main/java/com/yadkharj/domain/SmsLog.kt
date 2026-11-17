package com.yadkharj.domain

import android.content.Context

/** ۲۰ پیامکِ آخری که به برنامه رسیده + نتیجهٔ بررسی‌شان؛ برای اینکه معلوم باشد چرا یک پیامک ثبت نشد. فقط روی همین گوشی می‌ماند. */
object SmsLog {
    data class Entry(val time: Long, val sender: String, val body: String, val status: String)

    private const val MAX = 20
    private val RS = 30.toChar().toString()
    private val FS = 31.toChar().toString()

    private fun sp(c: Context) = c.getSharedPreferences("smslog", Context.MODE_PRIVATE)

    fun load(c: Context): List<Entry> =
        sp(c).getString("v", "").orEmpty().split(RS).mapNotNull {
            val p = it.split(FS)
            if (p.size != 4) null else Entry(p[0].toLongOrNull() ?: return@mapNotNull null, p[1], p[2], p[3])
        }

    @Synchronized
    fun add(c: Context, sender: String, body: String, status: String) {
        val clean = { s: String -> s.replace(RS, " ").replace(FS, " ") }
        val e = Entry(System.currentTimeMillis(), clean(sender), clean(body).take(400), clean(status))
        val all = (listOf(e) + load(c)).take(MAX)
        sp(c).edit().putString("v", all.joinToString(RS) { listOf(it.time, it.sender, it.body, it.status).joinToString(FS) }).apply()
    }

    fun clear(c: Context) = sp(c).edit().remove("v").apply()
}
