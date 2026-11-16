package com.yadkharj.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.yadkharj.domain.TxProcessor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        val msgs = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
        val bySender = msgs.groupBy { it.originatingAddress ?: "" }
        val pending = goAsync()
        val app = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                for ((sender, parts) in bySender) {
                    val body = parts.joinToString("") { it.messageBody ?: "" }
                    TxProcessor.process(app, sender, body)
                }
            } finally {
                pending.finish()
            }
        }
    }
}
