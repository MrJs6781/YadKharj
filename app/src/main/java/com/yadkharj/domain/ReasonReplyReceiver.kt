package com.yadkharj.domain

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.RemoteInput
import com.yadkharj.data.AppDb
import com.yadkharj.widget.TxWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** جوابِ فیلد «ثبت دلیل» داخل خود اعلان (بدون باز کردن برنامه). */
class ReasonReplyReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra(Notifier.EXTRA_ID, -1L)
        val reason = RemoteInput.getResultsFromIntent(intent)?.getCharSequence(Notifier.KEY_REASON)?.toString()?.trim()
        if (id < 0 || reason.isNullOrEmpty()) return
        val pending = goAsync()
        val app = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao = AppDb.get(app).dao()
                dao.setReason(id, reason)
                Notifier.cancel(app, id)
                TxWidget.refresh(app)
                if (dao.get(id)?.isIncome == false) Budgets.check(app, reason)
            } finally {
                pending.finish()
            }
        }
    }
}
