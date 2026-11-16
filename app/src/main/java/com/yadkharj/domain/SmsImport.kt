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
    private const val SAME_MS = 5 * 60_000L

    /** تعداد تراکنش‌های جدید را برمی‌گرداند. پیامکی که قبلاً ثبت شده (همان متن، فاصلهٔ کمتر از ۵ دقیقه) رد می‌شود. */
    suspend fun run(c: Context): Int {
        val dao = AppDb.get(c).dao()
        val known = dao.all().groupBy({ it.body }, { it.timestamp })
        val fresh = mutableListOf<Tx>()
        c.contentResolver.query(
            Telephony.Sms.Inbox.CONTENT_URI,
            arrayOf(Telephony.Sms.ADDRESS, Telephony.Sms.BODY, Telephony.Sms.DATE),
            null, null, "${Telephony.Sms.DATE} DESC"
        )?.use { cur ->
            var rows = 0
            while (cur.moveToNext() && rows++ < MAX_ROWS) {
                val body = cur.getString(1) ?: continue
                val ts = cur.getLong(2)
                if (known[body]?.any { abs(it - ts) < SAME_MS } == true) continue
                val p = SmsParser.parse(body) ?: continue
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
