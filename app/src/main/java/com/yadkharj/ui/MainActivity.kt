package com.yadkharj.ui

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yadkharj.data.AppDb
import com.yadkharj.data.Tx
import com.yadkharj.domain.Notifier
import com.yadkharj.domain.Perms
import com.yadkharj.domain.Prefs
import com.yadkharj.domain.Suggestions
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainVm(app: Application) : AndroidViewModel(app) {
    private val dao = AppDb.get(app).dao()
    val all = dao.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setReason(id: Long, reason: String) {
        viewModelScope.launch { dao.setReason(id, reason.trim().ifEmpty { null }) }
        Notifier.cancel(getApplication(), id)
    }

    fun delete(id: Long) {
        viewModelScope.launch { dao.remove(id) }
        Notifier.cancel(getApplication(), id)
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Notifier.ensureChannel(this)
        setContent { AppTheme { App() } }
    }
}

@Composable
fun App(vm: MainVm = viewModel()) {
    val ctx = LocalContext.current
    val all by vm.all.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var toman by remember { mutableStateOf(Prefs.toman(ctx)) }
    var editing by remember { mutableStateOf<Tx?>(null) }
    var permTick by remember { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { permTick++ }
    val smsOk = remember(permTick) { Perms.sms(ctx) }

    Column(Modifier.fillMaxSize().background(P.bg).statusBarsPadding()) {
        Box(Modifier.weight(1f)) {
            when (tab) {
                0 -> TxScreen(all, toman, smsOk, onEdit = { editing = it }, onOpenSettings = { tab = 2 })
                1 -> ReportScreen(all, toman)
                else -> SettingsScreen(toman, { toman = it; Prefs.setToman(ctx, it) }, permTick) { permTick++ }
            }
        }
        BottomBar(tab) { tab = it }
    }

    editing?.let { tx ->
        Dialog(onDismissRequest = { editing = null }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Box(Modifier.padding(20.dp)) {
                val sugg = Suggestions.from(
                    all.filter { it.isIncome == tx.isIncome }.mapNotNull { it.reason }, tx.isIncome
                )
                ReasonCard(
                    tx = tx, suggestions = sugg, toman = toman, initial = tx.reason ?: "", laterLabel = "بستن",
                    onSave = { vm.setReason(tx.id, it); editing = null },
                    onLater = { editing = null },
                    onDelete = { vm.delete(tx.id); editing = null }
                )
            }
        }
    }
}

@Composable
private fun BottomBar(selected: Int, onSelect: (Int) -> Unit) {
    val labels = listOf("تراکنش‌ها", "گزارش", "تنظیمات")
    Column(Modifier.fillMaxWidth().background(P.bg)) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(P.line))
        Row(Modifier.fillMaxWidth().navigationBarsPadding()) {
            labels.forEachIndexed { i, l ->
                Column(
                    Modifier.weight(1f).clickable { onSelect(i) }.padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(Modifier.size(5.dp).clip(CircleShape).background(if (i == selected) P.accent else Color.Transparent))
                    Spacer(Modifier.height(4.dp))
                    Txt(
                        l, size = 13, color = if (i == selected) P.ink else P.muted,
                        weight = if (i == selected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}
