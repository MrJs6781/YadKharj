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
import androidx.core.app.RemoteInput
import androidx.core.content.ContextCompat
import com.yadkharj.R
import kotlinx.coroutines.sync.withLock
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

    /** اعلان‌ها روشن‌اند و کانال «تراکنش جدید» هدزآپ می‌دهد؟ بدون این، وقتی برنامه بسته است دلیل پرسیده نمی‌شود. */
    fun notifReady(c: Context): Boolean {
        val ch = c.getSystemService(NotificationManager::class.java).getNotificationChannel(Notifier.CH)
        return NotificationManagerCompat.from(c).areNotificationsEnabled() &&
            (ch?.importance ?: NotificationManager.IMPORTANCE_HIGH) >= NotificationManager.IMPORTANCE_HIGH
    }

    fun ignoringBattery(c: Context) =
        c.getSystemService(android.os.PowerManager::class.java).isIgnoringBatteryOptimizations(c.packageName)

    /** اندروید ۱۴+: نمایش اعلان روی صفحهٔ قفل/خاموش مجوز جدا دارد. */
    fun fullScreen(c: Context) = Build.VERSION.SDK_INT < 34 ||
        c.getSystemService(NotificationManager::class.java).canUseFullScreenIntent()

    fun readSms(c: Context) =
        ContextCompat.checkSelfPermission(c, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED
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
    const val CH = "new_tx"
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

    /** هشدار بودجه؛ شناسهٔ اعلان از برچسب ساخته می‌شود تا هر سقف یک اعلان داشته باشد. */
    fun budget(c: Context, label: String, text: String) = alert(c, "budget", "هشدار بودجه", "بودجه", label, text)

    fun alert(c: Context, channel: String, channelName: String, title: String, label: String, text: String) {
        val nm = c.getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(channel) == null) {
            nm.createNotificationChannel(NotificationChannel(channel, channelName, NotificationManager.IMPORTANCE_DEFAULT))
        }
        val n = NotificationCompat.Builder(c, channel)
            .setSmallIcon(R.drawable.ic_notif).setContentTitle(title).setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text)).setAutoCancel(true).build()
        try { NotificationManagerCompat.from(c).notify(-(label.hashCode() and 0x7fffffff) - 1, n) } catch (e: SecurityException) { }
    }

    const val KEY_REASON = "reason"

    /**
     * همیشه اعلانِ هدزآپ می‌دهد (با فیلد «ثبت دلیل» داخل خودِ اعلان)، چون startActivity از پس‌زمینه روی
     * اندروید ۱۰+ و پوسته‌هایی مثل MIUI بی‌صدا شکست می‌خورد. اگر «نمایش روی سایر برنامه‌ها» هم داده شده باشد،
     * پنجرهٔ دلیل را هم مستقیم باز می‌کند.
     */
    fun announce(c: Context, tx: Tx): String {
        val intent = Intent(c, ReasonActivity::class.java)
            .putExtra(EXTRA_ID, tx.id)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)

        ensureChannel(c)
        val pi = PendingIntent.getActivity(
            c, tx.id.toInt(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val replyFlags = PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= 31) PendingIntent.FLAG_MUTABLE else 0)
        val replyPi = PendingIntent.getBroadcast(
            c, tx.id.toInt(), Intent(c, ReasonReplyReceiver::class.java).putExtra(EXTRA_ID, tx.id), replyFlags
        )
        val reply = NotificationCompat.Action.Builder(R.drawable.ic_notif, "ثبت دلیل", replyPi)
            .addRemoteInput(RemoteInput.Builder(KEY_REASON).setLabel("دلیلش چی بود؟").build())
            .build()
        val toman = Prefs.toman(c)
        val kind = if (tx.isIncome) "واریز" else "برداشت"
        val bank = Banks.label(Banks.detect(tx.sender, tx.body, Banks.loadOverrides(c)), tx.card)
        val n = NotificationCompat.Builder(c, CH)
            .setSmallIcon(R.drawable.ic_notif)
            .setContentTitle("$kind ${Fmt.money(tx.amount, toman)} ${Fmt.unit(toman)}")
            .setContentText("$bank · دلیلش چی بود؟")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .setFullScreenIntent(pi, true)
            .addAction(reply)
            .build()
        try { NotificationManagerCompat.from(c).notify(tx.id.toInt(), n) } catch (e: SecurityException) { }

        if (Perms.overlay(c)) {
            try { c.startActivity(intent) } catch (e: Exception) { }
        }

        val nm = c.getSystemService(NotificationManager::class.java)
        val importance = nm.getNotificationChannel(CH)?.importance ?: NotificationManager.IMPORTANCE_HIGH
        return when {
            !NotificationManagerCompat.from(c).areNotificationsEnabled() -> "اعلان‌ها برای برنامه خاموشه"
            importance == NotificationManager.IMPORTANCE_NONE -> "کانال «تراکنش جدید» خاموشه"
            importance < NotificationManager.IMPORTANCE_HIGH -> "کانال اعلان بی‌صدا/کم‌اهمیته (هدزآپ نمی‌شه)"
            else -> "اعلان ارسال شد"
        }
    }
}

object TxProcessor {
    /** پیامک را بررسی می‌کند؛ اگر تراکنش بانکی بود ذخیره و پنجرهٔ پرسیدن دلیل را باز می‌کند. */
    // ponytail: قفل سراسری برای بررسی+درج؛ فقط چند ms نگه داشته می‌شود.
    private val lock = kotlinx.coroutines.sync.Mutex()

    suspend fun process(c: Context, sender: String, body: String, time: Long = System.currentTimeMillis(), dedupe: Boolean = true): Long? {
        if (Senders.isBlocked(c, sender)) { SmsLog.add(c, sender, body, "نادیده: فرستنده «بانک نیست» شده"); return null }
        val p = SmsParser.parse(body)
        if (p == null) { SmsLog.add(c, sender, body, "تراکنش نیست: ${SmsParser.explain(body) ?: "تشخیص داده نشد"}"); return null }
        val tx = Tx(amount = p.amountRial, isIncome = p.isIncome, timestamp = time, sender = sender, body = body, card = p.card, balance = p.balanceRial)
        val dao = AppDb.get(c).dao()
        val id = lock.withLock {
            if (dedupe && dao.bodiesBetween(time - SmsImport.SAME_MS, time + SmsImport.SAME_MS).any { SmsImport.key(it) == SmsImport.key(body) }) {
                SmsLog.add(c, sender, body, "تکراری (قبلاً ثبت شده)")
                return null
            }
            dao.insert(tx)
        }
        val note = runCatching { Notifier.announce(c, tx.copy(id = id)) }.getOrElse { "خطا در اعلان: ${it.javaClass.simpleName}" }
        SmsLog.add(c, sender, body, "ثبت شد · $note")
        com.yadkharj.widget.TxWidget.refresh(c)
        if (!p.isIncome) Budgets.check(c, null)
        return id
    }
}
