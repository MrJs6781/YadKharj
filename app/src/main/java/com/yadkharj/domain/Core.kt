package com.yadkharj.domain

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.yadkharj.R
import com.yadkharj.data.AppDb
import com.yadkharj.data.Tx
import com.yadkharj.ui.ReasonActivity

object Prefs {
    private fun sp(c: Context) = c.getSharedPreferences("prefs", Context.MODE_PRIVATE)
    fun toman(c: Context): Boolean = sp(c).getBoolean("toman", true)
    fun setToman(c: Context, v: Boolean) = sp(c).edit().putBoolean("toman", v).apply()
}

object Perms {
    fun sms(c: Context) =
        ContextCompat.checkSelfPermission(c, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED

    fun notif(c: Context) = Build.VERSION.SDK_INT < 33 ||
        ContextCompat.checkSelfPermission(c, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun overlay(c: Context) = Settings.canDrawOverlays(c)
}

object Suggestions {
    private val outDefaults = listOf("غذا", "رفت‌وآمد", "خرید", "قبض", "قسط", "تفریح")
    private val inDefaults = listOf("حقوق", "طلب", "فروش", "هدیه", "برگشت پول", "کارت به کارت")

    fun from(recent: List<String>, income: Boolean): List<String> {
        val top = recent.map { it.trim() }
            .filter { it.isNotEmpty() && it.length <= 20 }
            .groupingBy { it }.eachCount()
            .entries.sortedByDescending { it.value }.map { it.key }
        return (top + if (income) inDefaults else outDefaults).distinct().take(6)
    }
}

object Notifier {
    private const val CH = "new_tx"
    const val EXTRA_ID = "tx_id"

    fun ensureChannel(c: Context) {
        val nm = c.getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(CH) == null) {
            val ch = NotificationChannel(CH, "تراکنش جدید", NotificationManager.IMPORTANCE_HIGH)
            ch.description = "وقتی پیامک بانکی میاد، دلیلش رو می‌پرسه"
            nm.createNotificationChannel(ch)
        }
    }

    fun cancel(c: Context, id: Long) = NotificationManagerCompat.from(c).cancel(id.toInt())

    /** اگر مجوز «نمایش روی سایر برنامه‌ها» داده شده باشد، پنجره مستقیم باز می‌شود؛ وگرنه نوتیف تمام‌صفحه/هدزآپ. */
    fun announce(c: Context, tx: Tx) {
        val intent = Intent(c, ReasonActivity::class.java)
            .putExtra(EXTRA_ID, tx.id)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)

        if (Perms.overlay(c)) {
            try { c.startActivity(intent); return } catch (_: Exception) { }
        }

        ensureChannel(c)
        val pi = PendingIntent.getActivity(
            c, tx.id.toInt(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val toman = Prefs.toman(c)
        val kind = if (tx.isIncome) "واریز" else "برداشت"
        val n = NotificationCompat.Builder(c, CH)
            .setSmallIcon(R.drawable.ic_notif)
            .setContentTitle("$kind ${Fmt.money(tx.amount, toman)} ${Fmt.unit(toman)}")
            .setContentText("دلیلش چی بود؟ لمس کن و بنویس")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .setFullScreenIntent(pi, true)
            .build()
        try { NotificationManagerCompat.from(c).notify(tx.id.toInt(), n) } catch (_: SecurityException) { }
    }
}

object TxProcessor {
    /** پیامک را بررسی می‌کند؛ اگر تراکنش بانکی بود ذخیره و پنجرهٔ پرسیدن دلیل را باز می‌کند. */
    suspend fun process(c: Context, sender: String, body: String, time: Long = System.currentTimeMillis()): Long? {
        val p = SmsParser.parse(body) ?: return null
        val tx = Tx(amount = p.amountRial, isIncome = p.isIncome, timestamp = time, sender = sender, body = body)
        val id = AppDb.get(c).dao().insert(tx)
        Notifier.announce(c, tx.copy(id = id))
        return id
    }
}
