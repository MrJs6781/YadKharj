package com.yadkharj.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.yadkharj.R
import com.yadkharj.data.AppDb
import com.yadkharj.domain.Fmt
import com.yadkharj.domain.Transfers
import com.yadkharj.domain.Period
import com.yadkharj.domain.Periods
import com.yadkharj.domain.Prefs
import com.yadkharj.domain.Reports
import com.yadkharj.domain.Senders
import com.yadkharj.ui.MainActivity
import com.yadkharj.ui.QuickAddActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate

/** ویجت خلاصهٔ خرج امروز/هفته + دکمهٔ افزودن سریع. */
class TxWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try { render(context.applicationContext, manager, ids) } finally { pending.finish() }
        }
    }

    companion object {
        /** بعد از هر تغییر در تراکنش‌ها صدا زده شود (از هر ترد). */
        fun refresh(c: Context) {
            val app = c.applicationContext
            val m = AppWidgetManager.getInstance(app)
            val ids = m.getAppWidgetIds(ComponentName(app, TxWidget::class.java))
            if (ids.isEmpty()) return
            CoroutineScope(Dispatchers.IO).launch { render(app, m, ids) }
        }

        private suspend fun render(c: Context, m: AppWidgetManager, ids: IntArray) {
            val all = Transfers.without(AppDb.get(c).dao().all().filter { it.sender !in Senders.blocked(c) })
            val today = LocalDate.now()
            val toman = Prefs.toman(c)
            val day = Reports.build(all, Period(today, today.plusDays(1), "", false)).expense
            val week = Reports.build(all, Periods.week(0, today)).expense
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            val v = RemoteViews(c.packageName, R.layout.widget).apply {
                setTextViewText(R.id.today, "${Fmt.money(day, toman)} ${Fmt.unit(toman)}")
                setTextViewText(R.id.week, "این هفته: ${Fmt.money(week, toman)}")
                setOnClickPendingIntent(R.id.root, PendingIntent.getActivity(c, 0, Intent(c, MainActivity::class.java), flags))
                setOnClickPendingIntent(R.id.add, PendingIntent.getActivity(c, 1, Intent(c, QuickAddActivity::class.java), flags))
            }
            m.updateAppWidget(ids, v)
        }
    }
}
