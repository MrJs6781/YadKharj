package com.yadkharj.domain

import android.content.Context
import com.yadkharj.data.AppDb
import com.yadkharj.widget.TxWidget

/** سرشماره‌هایی که کاربر گفته «بانک نیست»؛ پیامکشان نادیده گرفته می‌شود. */
object Senders {
    private fun sp(c: Context) = c.getSharedPreferences("senders", Context.MODE_PRIVATE)

    fun blocked(c: Context): Set<String> = sp(c).getStringSet("blocked", emptySet()).orEmpty().toSet()

    fun isBlocked(c: Context, sender: String) = sender in blocked(c)

    /** فرستنده را مسدود و تراکنش‌های قبلی‌اش را پاک می‌کند (قابل بازگشت نیست؛ فقط پیامک‌های بعدی دوباره ثبت می‌شوند). */
    suspend fun block(c: Context, sender: String) {
        if (sender.isEmpty()) return
        sp(c).edit().putStringSet("blocked", blocked(c) + sender).apply()
        AppDb.get(c).dao().removeBySender(sender)
        TxWidget.refresh(c)
    }

    fun unblock(c: Context, sender: String) =
        sp(c).edit().putStringSet("blocked", blocked(c) - sender).apply()
}
