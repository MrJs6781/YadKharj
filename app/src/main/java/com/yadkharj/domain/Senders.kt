package com.yadkharj.domain

import android.content.Context
import com.yadkharj.data.Tx
import com.yadkharj.widget.TxWidget
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * سرشماره‌هایی که کاربر گفته «بانک نیست»؛ پیامکشان ثبت نمی‌شود و تراکنش‌های قبلی‌شان فقط از لیست‌ها پنهان می‌شود،
 * پس با «بازگرداندن» دوباره برمی‌گردند.
 */
object Senders {
    private fun sp(c: Context) = c.getSharedPreferences("senders", Context.MODE_PRIVATE)

    private val _blocked = MutableStateFlow<Set<String>>(emptySet())
    val blockedFlow: StateFlow<Set<String>> = _blocked

    @Volatile private var loaded = false

    @Synchronized
    private fun ensure(c: Context) {
        if (!loaded) {
            _blocked.value = sp(c).getStringSet("blocked", emptySet()).orEmpty().toSet()
            loaded = true
        }
    }

    fun blocked(c: Context): Set<String> { ensure(c); return _blocked.value }

    fun isBlocked(c: Context, sender: String) = sender in blocked(c)

    /** فهرستِ دیدنی: تراکنش‌های فرستنده‌های «بانک نیست» فقط پنهان می‌شوند و در دیتابیس می‌مانند. */
    fun visible(all: List<Tx>, blocked: Set<String>): List<Tx> =
        if (blocked.isEmpty()) all else all.filter { it.sender !in blocked }

    suspend fun block(c: Context, sender: String) {
        if (sender.isEmpty()) return
        update(c, blocked(c) + sender)
        TxWidget.refresh(c)
    }

    /** برای بازیابی پشتیبان: فقط به فهرست اضافه می‌کند (تراکنشی پنهان می‌شود، نه پاک). */
    fun addAll(c: Context, senders: Set<String>) {
        if (senders.isEmpty()) return
        update(c, blocked(c) + senders)
        TxWidget.refresh(c)
    }

    fun unblock(c: Context, sender: String) {
        update(c, blocked(c) - sender)
        TxWidget.refresh(c)
    }

    @Synchronized
    private fun update(c: Context, next: Set<String>) {
        ensure(c)
        sp(c).edit().putStringSet("blocked", next).apply()
        _blocked.value = next
    }
}
