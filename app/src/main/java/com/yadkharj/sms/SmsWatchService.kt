package com.yadkharj.sms

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.yadkharj.R

/**
 * سرویسِ دائمیِ سبک که برنامه را در پس‌زمینه زنده نگه می‌دارد تا پیامک بانکی همان لحظه شناسایی شود.
 * فقط یک اعلان بی‌صدا نشان می‌دهد؛ هیچ شبکه‌ای استفاده نمی‌کند.
 */
class SmsWatchService : Service() {
    companion object {
        private const val CH = "sms_watch"
        private const val ID = 0x5A4D

        fun start(c: Context) {
            val i = Intent(c, SmsWatchService::class.java)
            try {
                ContextCompat.startForegroundService(c, i)
            } catch (_: Exception) {
                // اندروید ۱۲+ ممکن است اجرای سرویس از پس‌زمینه را رد کند؛ در باز بعدیِ اپ دوباره تلاش می‌شود.
            }
        }

        fun stop(c: Context) {
            c.stopService(Intent(c, SmsWatchService::class.java))
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        val nm = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= 26 && nm.getNotificationChannel(CH) == null) {
            nm.createNotificationChannel(NotificationChannel(CH, "دریافت پیامک بانکی", NotificationManager.IMPORTANCE_MIN).apply {
                description = "برای شناسایی سریع پیامک بانکی در پس‌زمینه"
                setShowBadge(false)
            })
        }
        val n = NotificationCompat.Builder(this, CH)
            .setSmallIcon(R.drawable.ic_launcher_mono)
            .setContentTitle(getString(R.string.app_name))
            .setContentText("در حال آماده‌باش برای پیامک بانکی — بدون هیچ مصرف اینترنت")
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(ID, n)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY
}
