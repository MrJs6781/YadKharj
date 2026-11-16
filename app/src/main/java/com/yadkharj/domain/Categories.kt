package com.yadkharj.domain

import android.content.Context
import com.yadkharj.data.Tx
import java.time.ZoneId

/** دسته‌بندی خودکار دلیل‌ها با کلمه‌های کلیدی؛ کاربر می‌تونه برای هر دلیل دسته رو عوض کنه (در SharedPreferences). */
object Categories {
    const val OTHER = "سایر"

    // ترتیب مهمه: اولین دسته‌ای که کلمه‌اش پیدا بشه برنده‌ست. (ponytail: تطبیق ساده با contains؛ برای دقت بیشتر ریشه‌یابی لازمه)
    private val rules: List<Pair<String, List<String>>> = listOf(
        "قسط و بدهی" to listOf("قسط", "وام", "بدهی", "بدهکار", "اجاره", "رهن", "چک", "قرض", "تسهیلات"),
        "قبض و شارژ" to listOf("قبض", "برق", "گاز", " آب ", "اینترنت", "شارژ", "تلفن", "موبایل", "همراه اول", "ایرانسل", "رایتل", "مخابرات", "ایتا", "نت ", "عوارض", "بیمه", "شهرداری", "پسماند"),
        "غذا و رستوران" to listOf("نان", "ناهار", "شام", "صبحانه", "رستوران", "کافه", "غذا", "پیتزا", "ساندویچ", "برگر", "کباب", "چلو", "فست فود", "میوه", "سبزی", "گوشت", "مرغ", "ماهی", "برنج", "شیر", "ماست", "پنیر", "تخم", "قهوه", "چای", "نوشیدنی", "بستنی", "شیرینی", "آبمیوه", "اسنپ فود", "snappfood", "دلستر", "سوپرمارکت", "سوپر", "نهار", "میان وعده", "اسنک", "خواربار", "پروتئین", "لبنیات", "آجیل", "قنادی", "نانوایی", "رستوران", "فروشگاه مواد غذایی"),
        "حمل‌ونقل" to listOf("تاکسی", "اسنپ", "snapp", "تپسی", "tapsi", "بنزین", "مترو", "اتوبوس", "بلیط", "کرایه", "آژانس", "پارکینگ", "اتوبان", "عوارض", "تعمیرگاه", "تعمیر ماشین", "لاستیک", "روغن", "کارواش", "قطار", "هواپیما", "پرواز", "سوخت", "ماشین"),
        "سلامت" to listOf("دارو", "دکتر", "ویزیت", "درمان", "بیمارستان", "آزمایش", "عینک", "دندان", "پزشک", "کلینیک", "داروخانه", "فیزیوتراپی", "سونوگرافی", "رادیولوژی", "مطب", "ماساژ", "آرایشگاه"),
        "آموزش" to listOf("کتاب", "کلاس", "دانشگاه", "شهریه", "دوره", "آموزش", "مدرسه", "کنکور", "دانشکده", "ترم", "زبان", "کلاس", "لوازم تحریر", "تحریر", "دفتر", "جزوه", "کپی"),
        "تفریح" to listOf("سینما", "فیلم", "نتفلیکس", "netflix", "بازی", "پارک آبی", "تفریح", "کنسرت", "تئاتر", "سفر", "مسافرت", "هتل", "استخر", "باشگاه", "ورزش", "اشتراک", "اسپاتیفای", "spotify", "شهربازی", "پیک نیک", "گردش", "قهوه خانه"),
        "خرید" to listOf("خرید", "لباس", "کفش", "کیف", "دیجی کالا", "دیجیکالا", "دیجی", "پوشاک", "لوازم", "هدیه", "کادو", "مبل", "یخچال", "گوشی", "لپ تاپ", "ظرف", "پتو", "فرش", "ابزار", "بازار", "مغازه", "شامپو", "آرایش", "عطر", "ساعت", "طلا")
    )

    val ALL: List<String> = listOf("غذا و رستوران", "حمل‌ونقل", "خرید", "قبض و شارژ", "قسط و بدهی", "سلامت", "تفریح", "آموزش", OTHER)

    private fun key(reason: String) = SmsParser.normalize(reason).trim()

    /** دستهٔ یک دلیل؛ اول override کاربر، بعد کلمه‌های کلیدی، وگرنه «سایر». */
    fun of(reason: String, overrides: Map<String, String> = emptyMap()): String {
        val k = key(reason)
        overrides[reason.trim()]?.let { if (it in ALL) return it }
        overrides[k]?.let { if (it in ALL) return it }
        val t = " $k "
        return rules.firstOrNull { (_, ws) -> ws.any { t.contains(it) } }?.first ?: OTHER
    }

    /** خرج‌های بازه به تفکیک دسته؛ بدون دلیل جدا می‌ماند. */
    fun slices(all: List<Tx>, p: Period, overrides: Map<String, String>): List<Slice> {
        val z = ZoneId.systemDefault()
        val s = p.start.atStartOfDay(z).toInstant().toEpochMilli()
        val e = p.end.atStartOfDay(z).toInstant().toEpochMilli()
        val out = all.filter { !it.isIncome && it.timestamp in s until e }
        val labeled = out.filter { !it.reason.isNullOrBlank() }
            .groupBy { of(it.reason!!, overrides) }
            .map { (k, v) -> Slice(k, v.sumOf { t -> t.amount }, false) }
            .sortedByDescending { it.amount }
        val un = out.filter { it.reason.isNullOrBlank() }.sumOf { it.amount }
        return if (un > 0) labeled + Slice("بدون دلیل", un, true) else labeled
    }

    /** دلیل‌هایی که به «سایر» افتادن، پرتکرارترها اول. */
    fun uncategorized(all: List<Tx>, overrides: Map<String, String>, max: Int = 8): List<String> =
        all.mapNotNull { it.reason?.trim()?.takeIf { r -> r.isNotEmpty() } }
            .groupingBy { it }.eachCount().entries
            .filter { of(it.key, overrides) == OTHER }
            .sortedByDescending { it.value }.take(max).map { it.key }

    private fun sp(c: Context) = c.getSharedPreferences("categories", Context.MODE_PRIVATE)

    fun load(c: Context): Map<String, String> =
        sp(c).getString("v", "").orEmpty().lines().filter { '\t' in it }
            .associate { it.substringBefore('\t') to it.substringAfter('\t') }

    fun save(c: Context, m: Map<String, String>) =
        sp(c).edit().putString("v", m.entries.joinToString("\n") { "${it.key.trim()}\t${it.value}" }).apply()
}
