package com.yadkharj.ui

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.content.ContextWrapper
import android.os.SystemClock
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect

/** قفل برنامه با قفل صفحهٔ خود گوشی (PIN/الگو/رمز/اثرانگشت). */
object AppLock {
    const val GRACE_MS = 60_000L

    /** آخرین ON_STOP (elapsedRealtime)؛ ۰ یعنی پردازهٔ تازه (مثلاً بعد از kill شدن) و باید قفل باشد. فقط در حافظهٔ پردازه می‌ماند، پس چرخش صفحه قفل نمی‌کند ولی kill شدن می‌کند. */
    @Volatile var lastStop = 0L

    /** قبل از باز کردن یه صفحهٔ بیرونی (انتخاب فایل، تنظیمات گوشی) true شود تا برگشتنِ دیرهنگام قفل نکند و نتیجهٔ آن صفحه گم نشود. */
    @Volatile var skipRelock = false

    fun shouldLock() = lastStop == 0L || SystemClock.elapsedRealtime() - lastStop > GRACE_MS

    private fun sp(c: Context) = c.getSharedPreferences("applock", Context.MODE_PRIVATE)

    fun enabled(c: Context) = sp(c).getBoolean("on", false)
    fun setEnabled(c: Context, v: Boolean) = sp(c).edit().putBoolean("on", v).apply()

    fun deviceSecure(c: Context) = c.getSystemService(KeyguardManager::class.java)?.isDeviceSecure == true

    /** اگه قفل روشنه ولی گوشی دیگه قفل نداره، کاربر بیرون نمی‌مونه. */
    fun active(c: Context) = enabled(c) && deviceSecure(c)

    fun applySecure(c: Context) {
        var x = c
        while (x is ContextWrapper && x !is Activity) x = x.baseContext
        val w = (x as? Activity)?.window ?: return
        if (active(c)) w.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        else w.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }
}

/** تا وقتی قفله فقط صفحهٔ قفل نشون داده می‌شه؛ [content] اصلاً کامپوز نمی‌شه. */
@Composable
fun AppLockGate(content: @Composable () -> Unit) {
    val ctx = LocalContext.current
    var locked by remember { mutableStateOf(AppLock.shouldLock()) }
    var authing by remember { mutableStateOf(false) }
    val active = AppLock.active(ctx)

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        authing = false
        if (r.resultCode == Activity.RESULT_OK) locked = false
    }
    fun unlock() {
        if (authing) return
        val i = ctx.getSystemService(KeyguardManager::class.java)
            ?.createConfirmDeviceCredentialIntent("قفل برنامه", "برای دیدن اطلاعات، هویتت رو تأیید کن")
        if (i == null) { locked = false; return }
        authing = true
        launcher.launch(i)
    }

    SideEffect {
        AppLock.applySecure(ctx)
        if (!active && locked) locked = false
    }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { if (!authing) AppLock.lastStop = SystemClock.elapsedRealtime() }
    LifecycleEventEffect(Lifecycle.Event.ON_START) {
        if (!authing && !AppLock.skipRelock && AppLock.active(ctx) && AppLock.lastStop > 0 && AppLock.shouldLock()) locked = true
        AppLock.skipRelock = false
    }
    LaunchedEffect(locked) { if (locked && active) unlock() }

    if (locked && active) {
        Column(
            Modifier.fillMaxSize().background(P.bg).padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Txt("برنامه قفله", size = 24, weight = FontWeight.Bold, align = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Txt("برای دیدن تراکنش‌ها قفل گوشی رو باز کن.", size = 13, color = P.muted, align = TextAlign.Center)
            Spacer(Modifier.height(24.dp))
            Pill("باز کردن قفل") { unlock() }
        }
    } else content()
}

/** بخش «قفل برنامه» در تنظیمات. */
@Composable
fun AppLockRow() {
    val ctx = LocalContext.current
    val secure = AppLock.deviceSecure(ctx)
    var on by remember { mutableStateOf(AppLock.enabled(ctx) && secure) }
    Txt("قفل برنامه", size = 12, color = P.muted)
    Spacer(Modifier.height(8.dp))
    if (!secure) {
        Txt("برای استفاده از قفل برنامه، اول توی تنظیمات گوشی یه قفل صفحه (PIN، الگو یا رمز) بذار.", size = 12, color = P.muted)
    } else {
        Segmented(listOf("خاموش", "روشن"), if (on) 1 else 0) {
            on = it == 1
            AppLock.setEnabled(ctx, on)
            AppLock.applySecure(ctx)
        }
        Txt("اگه روشن باشه، موقع باز کردن برنامه (و بعد از یه دقیقه دور بودن ازش) قفل گوشی رو می‌پرسه.", size = 11, color = P.muted, modifier = Modifier.padding(top = 8.dp))
    }
}
