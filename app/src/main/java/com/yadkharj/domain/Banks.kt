package com.yadkharj.domain

import com.yadkharj.data.Tx

/** color = رنگ تقریبی برند (ARGB) برای نشان دایره‌ای؛ لوگوی واقعی را می‌شود با فایل drawable به نام bank_<key> جایگزین کرد. */
data class Bank(val key: String, val name: String, val color: Long, val aliases: List<String>, val bare: Boolean = false)

object Banks {
    /**
     * تشخیص از روی نام بانک در متن/فرستنده (نه سرشماره، چون سرشماره‌ها را مطمئن نمی‌دانم).
     * نام فارسی کوتاه فقط با پیشوند «بانک» می‌خواند (تا «ملی/ملت/دی» در متن‌های دیگر اشتباه نشود)؛
     * نام لاتین و نام‌های bare=true بدون پیشوند.
     */
    val all = listOf(
        Bank("tosee_saderat", "توسعه صادرات", 0xFF283593, listOf("توسعه صادرات", "edbi")),
        Bank("melli", "ملی", 0xFF1E5AA8, listOf("ملی", "bank melli", "bmi")),
        Bank("mellat", "ملت", 0xFFD32F2F, listOf("ملت", "mellat")),
        Bank("tejarat", "تجارت", 0xFF1F4E9D, listOf("تجارت", "tejarat")),
        Bank("saderat", "صادرات", 0xFF2E7DD1, listOf("صادرات", "saderat")),
        Bank("sepah", "سپه", 0xFFF57C00, listOf("سپه", "sepah")),
        Bank("pasargad", "پاسارگاد", 0xFFD4A017, listOf("پاسارگاد", "pasargad"), bare = true),
        Bank("saman", "سامان", 0xFF0097D7, listOf("سامان", "saman")),
        Bank("blu", "بلو", 0xFF2962FF, listOf("blu", "بلو بانک"), bare = true),
        Bank("parsian", "پارسیان", 0xFF8E1B3A, listOf("پارسیان", "parsian"), bare = true),
        Bank("eghtesad_novin", "اقتصاد نوین", 0xFF6A1B9A, listOf("اقتصاد نوین", "enbank"), bare = true),
        Bank("keshavarzi", "کشاورزی", 0xFF2E7D32, listOf("کشاورزی", "keshavarzi")),
        Bank("maskan", "مسکن", 0xFFEF6C00, listOf("مسکن", "maskan")),
        Bank("refah", "رفاه", 0xFF00897B, listOf("رفاه", "refah")),
        Bank("ayandeh", "آینده", 0xFF7B1FA2, listOf("آینده", "ayandeh")),
        Bank("shahr", "شهر", 0xFFE53935, listOf("شهر", "shahr")),
        Bank("sina", "سینا", 0xFF0288D1, listOf("سینا", "sina")),
        Bank("day", "دی", 0xFF3949AB, listOf("دی", "bank day")),
        Bank("karafarin", "کارآفرین", 0xFF00796B, listOf("کارآفرین", "karafarin"), bare = true),
        Bank("sarmayeh", "سرمایه", 0xFF5D4037, listOf("سرمایه", "sarmayeh")),
        Bank("resalat", "رسالت", 0xFF388E3C, listOf("رسالت", "resalat"), bare = true),
        Bank("mehr_iran", "مهر ایران", 0xFF43A047, listOf("مهر ایران", "mehr iran"), bare = true),
        Bank("iran_zamin", "ایران زمین", 0xFF8D6E63, listOf("ایران زمین", "iran zamin"), bare = true),
        Bank("gardeshgari", "گردشگری", 0xFF0097A7, listOf("گردشگری", "tourism"), bare = true),
        Bank("post", "پست بانک", 0xFF1976D2, listOf("پست بانک", "postbank"), bare = true),
    )

    fun detect(sender: String, body: String): Bank? {
        val t = SmsParser.normalize("$sender $body").lowercase()
        return all.firstOrNull { b -> b.aliases.any { match(t, it, b.bare) } }
    }

    private fun match(t: String, alias: String, bare: Boolean): Boolean {
        val latin = alias.all { it.code < 128 }
        return if (latin || bare) t.contains(alias) else Regex("بانک\\s*" + Regex.escape(alias) + "(?![\\u0600-\\u06FF])").containsMatchIn(t)
    }

    /** بانکِ هر کارت: از جدیدترین تراکنشی که بانکش تشخیص داده می‌شود. */
    fun byCard(all: List<Tx>): Map<String, Bank> {
        val out = mutableMapOf<String, Bank>()
        for (t in all.sortedByDescending { it.timestamp }) {
            val c = t.card ?: continue
            if (c in out) continue
            detect(t.sender, t.body)?.let { out[c] = it }
        }
        return out
    }

    fun label(bank: Bank?, card: String?): String = when {
        bank != null && card != null -> "${bank.name} · ${Fmt.digits(card)}"
        bank != null -> bank.name
        card != null -> "کارت ${Fmt.digits(card)}"
        else -> "حساب"
    }
}
