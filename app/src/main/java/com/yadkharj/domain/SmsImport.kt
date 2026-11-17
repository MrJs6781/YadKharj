package com.yadkharj.domain

import android.content.Context
import android.provider.Telephony
import com.yadkharj.data.AppDb
import com.yadkharj.data.Tx
import com.yadkharj.widget.TxWidget
import kotlin.math.abs

/** واردکردن یک‌بارهٔ پیامک‌های بانکیِ صندوق ورودی. نیاز به مجوز READ_SMS دارد. */
object SmsImport {
    /** ponytail: فقط ۵۰۰۰ پیامک آخر خوانده می‌شود؛ برای صندوق بزرگ‌تر صفحه‌بندی لازم است. */
    private const val MAX_ROWS = 5000
    const val SAME_MS = 5 * 60_000L

    /** برای واردکردن از صندوق پیامک: ساعت ثبت‌شدهٔ صندوق ممکن است با ساعت دریافت زنده ساعت‌ها فرق داشته باشد (ساعت سرور اپراتور)، پس پنجره بزرگ است. */
    const val IMPORT_SAME_MS = 24 * 3600_000L

    /** متن بدون فاصله/خط جدید/نیم‌فاصله؛ تا دو نسخهٔ یک پیامک که فقط در فاصله‌ها فرق دارند یکی حساب شوند. */
    fun key(body: String): String = body.filter { !it.isWhitespace() && it.code != 0x200C && it.code != 0x200F && it.code != 0x200E }

    /** فقط پیامک‌های جدیدتر از آخرین همگام‌سازی (با ۱۰ دقیقه هم‌پوشانی) را می‌خواند، تا تراکنش‌هایی که کاربر پاک کرده برنگردند. */
    suspend fun sync(c: Context): Int {
        val sp = c.getSharedPreferences("smsimport", Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        val last = sp.getLong("last", now - 3 * 24 * 3600_000L)
        val n = run(c, last - 10 * 60_000L)
        sp.edit().putLong("last", now).apply()
        return n
    }

    /** آیا زمان [ts] کمتر از [window] با یکی از زمان‌های [known] فاصله دارد؟ */
    fun isDuplicate(known: List<Long>?, ts: Long, window: Long = SAME_MS): Boolean = known?.any { abs(it - ts) < window } == true

    /** تعداد تراکنش‌های جدید را برمی‌گرداند. پیامکی که قبلاً ثبت شده (همان متن، فاصلهٔ کمتر از ۵ دقیقه) رد می‌شود. */
    suspend fun run(c: Context, sinceMs: Long = 0L): Int {
        val dao = AppDb.get(c).dao()
        val known = dao.all().groupBy({ key(it.body) }, { it.timestamp }).mapValues { it.value.toMutableList() }.toMutableMap()
        val blocked = Senders.blocked(c)
        val fresh = mutableListOf<Tx>()
        c.contentResolver.query(
            Telephony.Sms.Inbox.CONTENT_URI,
            arrayOf(Telephony.Sms.ADDRESS, Telephony.Sms.BODY, Telephony.Sms.DATE),
            if (sinceMs > 0) "${Telephony.Sms.DATE} > ?" else null,
            if (sinceMs > 0) arrayOf(sinceMs.toString()) else null,
            "${Telephony.Sms.DATE} DESC"
        )?.use { cur ->
            var rows = 0
            while (cur.moveToNext() && rows++ < MAX_ROWS) {
                val body = cur.getString(1) ?: continue
                val ts = cur.getLong(2)
                if (cur.getString(0).orEmpty() in blocked) continue
                if (isDuplicate(known[key(body)], ts, IMPORT_SAME_MS)) continue
                val p = SmsParser.parse(body) ?: continue
                known.getOrPut(key(body)) { mutableListOf() }.add(ts)
                fresh += Tx(
                    amount = p.amountRial, isIncome = p.isIncome, timestamp = ts, sender = cur.getString(0).orEmpty(),
                    body = body, card = p.card, balance = p.balanceRial
                )
            }
        }
        dao.insertAll(fresh)
        TxWidget.refresh(c)
        return fresh.size
    }
}
