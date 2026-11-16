package com.yadkharj.domain

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** تبدیل تاریخ میلادی به شمسی (الگوریتم jdf). */
object Jalali {
    data class Date(val y: Int, val m: Int, val d: Int)

    val months = listOf(
        "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
        "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند"
    )
    val weekdaysShort = listOf("ش", "ی", "د", "س", "چ", "پ", "ج")
    val weekdaysLong = listOf("شنبه", "یکشنبه", "دوشنبه", "سه‌شنبه", "چهارشنبه", "پنجشنبه", "جمعه")

    fun of(g: LocalDate): Date {
        val gy = g.year
        val gm = g.monthValue
        val gd = g.dayOfMonth
        val gdm = intArrayOf(0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334)
        val gy2 = if (gm > 2) gy + 1 else gy
        var days = 355666 + 365 * gy + (gy2 + 3) / 4 - (gy2 + 99) / 100 + (gy2 + 399) / 400 + gd + gdm[gm - 1]
        var jy = -1595 + 33 * (days / 12053)
        days %= 12053
        jy += 4 * (days / 1461)
        days %= 1461
        if (days > 365) {
            jy += (days - 1) / 365
            days = (days - 1) % 365
        }
        val jm: Int
        val jd: Int
        if (days < 186) {
            jm = 1 + days / 31
            jd = 1 + days % 31
        } else {
            jm = 7 + (days - 186) / 30
            jd = 1 + (days - 186) % 30
        }
        return Date(jy, jm, jd)
    }

    /** شنبه = 0 ... جمعه = 6 */
    fun weekdayIndex(d: LocalDate): Int = (d.dayOfWeek.value + 1) % 7
}

object Fmt {
    private const val FA = "۰۱۲۳۴۵۶۷۸۹"
    private val zone: ZoneId get() = ZoneId.systemDefault()

    fun digits(s: String): String = buildString {
        for (c in s) append(if (c in '0'..'9') FA[c - '0'] else c)
    }

    fun num(n: Long): String {
        val s = kotlin.math.abs(n).toString().reversed().chunked(3).joinToString("٬").reversed()
        return digits(s)
    }

    fun money(rial: Long, toman: Boolean): String = num(if (toman) rial / 10 else rial)
    fun unit(toman: Boolean): String = if (toman) "تومان" else "ریال"

    fun date(ts: Long): LocalDate = Instant.ofEpochMilli(ts).atZone(zone).toLocalDate()

    fun time(ts: Long): String {
        val t = Instant.ofEpochMilli(ts).atZone(zone)
        return digits("%02d:%02d".format(t.hour, t.minute))
    }

    fun dayLabel(d: LocalDate, today: LocalDate = LocalDate.now()): String {
        if (d == today) return "امروز"
        if (d == today.minusDays(1)) return "دیروز"
        val j = Jalali.of(d)
        val base = "${digits(j.d.toString())} ${Jalali.months[j.m - 1]}"
        return if (j.y == Jalali.of(today).y) base else "$base ${digits(j.y.toString())}"
    }

    fun todayLong(): String {
        val t = LocalDate.now()
        val j = Jalali.of(t)
        return "${Jalali.weekdaysLong[Jalali.weekdayIndex(t)]} ${digits(j.d.toString())} ${Jalali.months[j.m - 1]} ${digits(j.y.toString())}"
    }
}
