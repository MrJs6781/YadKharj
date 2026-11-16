package com.yadkharj.domain

/** تشخیص پیامک‌های بانکی (واریز / برداشت) و استخراج مبلغ. */
object SmsParser {
    data class Parsed(val amountRial: Long, val isIncome: Boolean, val card: String? = null, val balanceRial: Long? = null)

    private val incomeWords = listOf("واریز", "دریافت", "افزایش موجودی", "deposit", "credit", "بستانکار")
    private val outWords = listOf(
        "برداشت", "خرید", "پرداخت", "انتقال", "کارت به کارت", "حواله", "قبض",
        "withdraw", "debit", "بدهکار", "کسر"
    )
    private val skipWords = listOf("رمز", "پویا", "کد تایید", "کد تأیید", "کد فعال", "کد یکبار", "otp", "cvv2", "تخفیف", "قرعه")
    private val currencyWords = listOf("ریال", "تومان", "مانده", "موجودی", "irr", "rial")

    private val signRegex = Regex("""(?:^|\s)([+-])\s*\d|\d\s*([+-])(?:\s|$)""")
    private val balanceRegex = Regex("""(?i)(?:مانده|موجودی|balance|bal)[^\d\n]{0,12}[-+]?\s*\d[\d,]*(?:\.\d+)?""")
    private val balanceCapture = Regex("""(?i)(?:مانده|موجودی|balance|bal)[^\d\n]{0,12}([-+]?\s*\d[\d,]*)""")
    private val cardMasked = Regex("""[*•xX]{2,}\s*(\d{4})(?!\d)""")
    private val cardNamed = Regex("""(?i)(?:کارت|حساب|card)[^\d\n]{0,6}(\d{4})(?!\d)""")
    private val dateRegex = Regex("""\d{2,4}[/\-.]\d{1,2}[/\-.]\d{1,2}""")
    private val timeRegex = Regex("""\d{1,2}:\d{2}(?::\d{2})?""")
    private val maskRegex = Regex("""\d*[*•]+\d*|[xX]{3,}\d*""")
    private val primaryKey = Regex("""(?:مبلغ|amount)[^\d\n]{0,25}(\d[\d,]*)""", RegexOption.IGNORE_CASE)
    private val secondaryKey = Regex("""(?:برداشت|واریز|خرید|پرداخت|انتقال)[^\d\n]{0,25}(\d[\d,]*)""")
    private val numRegex = Regex("""\d[\d,]*""")
    private val valid = 1000L..999_999_999_999L

    fun normalize(s: String): String = buildString(s.length) {
        for (ch in s) {
            append(
                when (ch) {
                    in '\u06F0'..'\u06F9' -> '0' + (ch - '\u06F0')
                    in '\u0660'..'\u0669' -> '0' + (ch - '\u0660')
                    '\u066C', '\u060C' -> ','
                    '\u200C' -> ' '
                    '\u064A' -> '\u06CC'
                    '\u0643' -> '\u06A9'
                    else -> ch
                }
            )
        }
    }

    fun parse(raw: String): Parsed? {
        val text = normalize(raw)
        val lower = text.lowercase()
        if (skipWords.any { lower.contains(it) }) return null
        if (currencyWords.none { lower.contains(it) }) return null
        val isIncome = direction(text, lower) ?: return null
        val amount = amount(text) ?: return null
        val rial = if (lower.contains("تومان")) amount * 10 else amount
        val mul = if (lower.contains("تومان")) 10 else 1
        return Parsed(rial, isIncome, card(text), balance(text)?.times(mul))
    }

    private fun firstIndex(lower: String, words: List<String>): Int =
        words.map { lower.indexOf(it) }.filter { it >= 0 }.minOrNull() ?: -1

    private fun direction(text: String, lower: String): Boolean? {
        val i = firstIndex(lower, incomeWords)
        val o = firstIndex(lower, outWords)
        if (i >= 0 && o >= 0) return i < o
        if (i >= 0) return true
        if (o >= 0) return false
        val m = signRegex.find(text) ?: return null
        val sign = m.groupValues[1].ifEmpty { m.groupValues[2] }
        return sign == "+"
    }

    /** چهار رقم آخر کارت/حساب، اگر در متن باشد. */
    fun card(text: String): String? =
        (cardMasked.find(text) ?: cardNamed.find(text))?.groupValues?.get(1)

    /** مانده بعد از تراکنش (همان واحدِ متن، یعنی هنوز ضرب در ۱۰ برای تومان نشده). */
    fun balance(text: String): Long? =
        balanceCapture.find(text)?.groupValues?.get(1)?.replace(" ", "")?.replace(",", "")?.replace("+", "")?.toLongOrNull()

    private fun toLong(s: String): Long? = s.replace(",", "").toLongOrNull()

    private fun amount(text: String): Long? {
        val t = text.replace(balanceRegex, " ").replace(dateRegex, " ")
            .replace(timeRegex, " ").replace(maskRegex, " ")
        for (re in listOf(primaryKey, secondaryKey)) {
            val v = re.find(t)?.groupValues?.get(1)?.let { toLong(it) }
            if (v != null && v in valid) return v
        }
        return numRegex.findAll(t).mapNotNull { toLong(it.value) }.firstOrNull { it in valid }
    }
}
