package com.yadkharj.domain

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/** پرداخت تکراری ماهانه (قسط، اجاره، قبض): هر ماه جلالی، روز [day]. مبلغ به ریال. */
data class Recurring(val title: String, val amount: Long, val day: Int) {
    /** ponytail: اسفند همیشه ۲۹ روزه حساب می‌شود؛ سال کبیسه روز ۳۰ را به ۲۹ می‌برد. */
    private fun monthLen(m: Int) = if (m <= 6) 31 else if (m <= 11) 30 else 29

    fun dueOn(d: LocalDate): Boolean {
        val j = Jalali.of(d)
        return j.d == minOf(day, monthLen(j.m))
    }
}

object RecurringStore {
    private fun sp(c: Context) = c.getSharedPreferences("recurring", Context.MODE_PRIVATE)

    fun load(c: Context): List<Recurring> =
        sp(c).getString("v", "").orEmpty().lines().mapNotNull {
            val p = it.split('\t')
            if (p.size != 3) null else Recurring(p[0], p[1].toLongOrNull() ?: return@mapNotNull null, p[2].toIntOrNull() ?: return@mapNotNull null)
        }

    fun save(c: Context, l: List<Recurring>) =
        sp(c).edit().putString("v", l.joinToString("\n") { "${it.title.replace('\t', ' ')}\t${it.amount}\t${it.day}" }).apply()

    /** یک بار در روز: اگر امروز سررسید است «امروز»، اگر فردا سررسید است «فردا» اعلان می‌دهد. */
    fun check(c: Context, today: LocalDate = LocalDate.now()) {
        val toman = Prefs.toman(c)
        val tomorrow = today.plusDays(1)
        for (r in load(c)) {
            val level = when { r.dueOn(today) -> 0; r.dueOn(tomorrow) -> 1; else -> continue }
            val mark = "last|${r.title}"
            if (sp(c).getString(mark, null) == today.toString()) continue
            sp(c).edit().putString(mark, today.toString()).apply()
            val money = "${Fmt.money(r.amount, toman)} ${Fmt.unit(toman)}"
            Notifier.alert(c, "reminder", "یادآور پرداخت", "یادآور", r.title, if (level == 0) "امروز: ${r.title} — $money" else "فردا: ${r.title} — $money")
        }
    }

    /** هر روز ساعت ۹ بررسی می‌کند. چند بار صدا زدنش بی‌ضرر است (همان PendingIntent جایگزین می‌شود). */
    fun schedule(c: Context) {
        val am = c.getSystemService(AlarmManager::class.java)
        val pi = PendingIntent.getBroadcast(
            c, 0, Intent(c, ReminderReceiver::class.java).setAction(ReminderReceiver.ACTION),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        var at = LocalDateTime.now().withHour(9).withMinute(0).withSecond(0).withNano(0)
        if (at.isBefore(LocalDateTime.now())) at = at.plusDays(1)
        am.setInexactRepeating(AlarmManager.RTC, at.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(), AlarmManager.INTERVAL_DAY, pi)
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            RecurringStore.schedule(context)
            com.yadkharj.sms.SmsWatchService.start(context)
        }
        RecurringStore.check(context)
    }

    companion object { const val ACTION = "com.yadkharj.REMINDER" }
}
