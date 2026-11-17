package com.yadkharj.ui

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.yadkharj.domain.Fmt
import com.yadkharj.domain.Perms
import com.yadkharj.domain.SmsImport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** دکمهٔ «بروزرسانی»: پیامک‌های بانکیِ جدیدِ صندوق ورودی را که به هر دلیل (مثلاً کشته شدن برنامه) از دست رفته‌اند ثبت می‌کند. */
class SmsRefresh(val busy: Boolean, val msg: String?, val run: () -> Unit)

@Composable
fun rememberSmsRefresh(): SmsRefresh {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var msg by remember { mutableStateOf<String?>(null) }
    fun go() {
        busy = true
        scope.launch(Dispatchers.IO) {
            val n = runCatching { SmsImport.sync(ctx.applicationContext) }.getOrNull()
            msg = when {
                n == null -> "خواندن پیامک‌ها ناموفق بود."
                n == 0 -> "همه‌چیز به‌روزه؛ پیامک بانکی جدیدی نبود."
                else -> "${Fmt.digits(n.toString())} تراکنش جدید اضافه شد."
            }
            busy = false
        }
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) go() else {
            msg = "دسترسی خواندن پیامک داده نشد. تنظیمات برنامه ← سه‌نقطه ← Allow restricted settings."
            AppLock.skipRelock = true
            ctx.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${ctx.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }
    return SmsRefresh(busy, msg) { if (Perms.readSms(ctx)) go() else launcher.launch(Manifest.permission.READ_SMS) }
}
