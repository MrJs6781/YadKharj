package com.yadkharj.ui

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.yadkharj.data.AppDb
import com.yadkharj.data.Tx
import com.yadkharj.domain.Notifier
import com.yadkharj.domain.Prefs
import com.yadkharj.domain.Suggestions
import kotlinx.coroutines.launch

/** پنجرهٔ شناور و مینیمال که بلافاصله بعد از پیامک بانکی دلیل را می‌پرسد. */
class ReasonActivity : ComponentActivity() {
    private var txId by mutableLongStateOf(-1L)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        txId = intent.getLongExtra(Notifier.EXTRA_ID, -1L)
        setContent { AppTheme { PopupHost(txId) { finish() } } }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        txId = intent.getLongExtra(Notifier.EXTRA_ID, -1L)
    }
}

@Composable
private fun PopupHost(txId: Long, onDone: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var tx by remember(txId) { mutableStateOf<Tx?>(null) }
    var sugg by remember(txId) { mutableStateOf(emptyList<String>()) }

    LaunchedEffect(txId) {
        if (txId < 0) { onDone(); return@LaunchedEffect }
        val dao = AppDb.get(ctx).dao()
        val t = dao.get(txId)
        if (t == null) { onDone(); return@LaunchedEffect }
        sugg = Suggestions.from(dao.recentReasons(t.isIncome), t.isIncome)
        tx = t
    }

    Box(
        Modifier.fillMaxSize()
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onDone() }
            .navigationBarsPadding().imePadding().padding(16.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        tx?.let { t ->
            ReasonCard(
                tx = t, suggestions = sugg, toman = Prefs.toman(ctx),
                onSave = { reason ->
                    scope.launch {
                        AppDb.get(ctx).dao().setReason(t.id, reason.trim())
                        Notifier.cancel(ctx, t.id)
                        onDone()
                    }
                },
                onLater = { Notifier.cancel(ctx, t.id); onDone() }
            )
        }
    }
}
