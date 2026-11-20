package com.yadkharj.ui

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
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
import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import com.yadkharj.domain.Banks
import com.yadkharj.domain.SmsImport
import kotlinx.coroutines.Dispatchers
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.rememberCoroutineScope
import com.yadkharj.domain.Budgets
import com.yadkharj.domain.Senders
import com.yadkharj.domain.Fmt
import com.yadkharj.domain.Notifier
import com.yadkharj.domain.Perms
import com.yadkharj.domain.Prefs
import com.yadkharj.domain.Suggestions
import com.yadkharj.domain.Transfers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainVm(app: Application) : AndroidViewModel(app) {
    private val dao = AppDb.get(app).dao()

    init { Senders.blocked(app) }

    /** فرستنده‌های «بانک نیست» از همه‌جا پنهان می‌شوند ولی ردیف‌هایشان در دیتابیس می‌ماند. */
    val all = combine(dao.observeAll(), Senders.blockedFlow) { list, blocked -> Senders.visible(list, blocked) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setReason(id: Long, reason: String) {
        viewModelScope.launch {
            dao.setReason(id, reason.trim().ifEmpty { null })
            if (dao.get(id)?.isIncome == false) Budgets.check(getApplication(), reason)
        }
        Notifier.cancel(getApplication(), id)
    }

    fun blockSender(sender: String) {
        viewModelScope.launch { Senders.block(getApplication(), sender) }
    }

    fun delete(id: Long) {
        viewModelScope.launch { dao.remove(id); com.yadkharj.widget.TxWidget.refresh(getApplication()) }
        Notifier.cancel(getApplication(), id)
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Notifier.ensureChannel(this)
        com.yadkharj.sms.SmsWatchService.start(this)
        val app = applicationContext
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch { runCatching { com.yadkharj.domain.Dedupe.clean(app) } }
        com.yadkharj.domain.RecurringStore.schedule(this)
        setContent { AppTheme { SplashGate { AppLockGate { AppRoot() } } } }
    }
}

/** آیا صفحهٔ اصلی الان دیده می‌شود؟ اجازهٔ باز کردن پنجرهٔ دلیل از پس‌زمینه فقط با «نمایش روی سایر برنامه‌ها» بود؛ در پیش‌زمینه لازم نیست. */
object AppVisible {
    @Volatile var visible = false
}

/** بانک‌هایی که کاربر دستی برای هر فرستنده تعیین کرده را برای همهٔ صفحه‌ها فراهم می‌کند. */
@Composable
fun AppRoot() {
    val ctx = LocalContext.current
    var bankMap by remember { mutableStateOf(Banks.loadOverrides(ctx)) }
    val prefs = remember(bankMap) {
        BankPrefs(bankMap, set = { s, k -> Banks.setOverride(ctx, s, k); bankMap = Banks.loadOverrides(ctx) }, reload = { bankMap = Banks.loadOverrides(ctx) })
    }
    CompositionLocalProvider(LocalBankPrefs provides prefs) { App() }
}

@Composable
fun App(vm: MainVm = viewModel()) {
    val ctx = LocalContext.current
    val all by vm.all.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var toman by remember { mutableStateOf(Prefs.toman(ctx)) }
    var editing by remember { mutableStateOf<Tx?>(null) }
    var permTick by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    LifecycleEventEffect(Lifecycle.Event.ON_START) { AppVisible.visible = true }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { AppVisible.visible = false }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        permTick++
        // پیامک‌هایی که برنامه موقع بسته بودن/کشته شدن از دست داده را از صندوق پیامک می‌خواند (اگه مجوز خواندن داده شده باشد).
        if (Perms.readSms(ctx)) scope.launch(Dispatchers.IO) { runCatching { SmsImport.sync(ctx.applicationContext) } }
    }
    val smsOk = remember(permTick) { Perms.sms(ctx) }

    val notifOk = remember(permTick) { Perms.notifReady(ctx) }
    var askedNotif by remember { mutableStateOf(false) }
    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { permTick++ }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33 && !Perms.notif(ctx)) { askedNotif = true; notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }
    }
    val fixNotif = {
        if (Build.VERSION.SDK_INT >= 33 && !Perms.notif(ctx) && !askedNotif) {
            askedNotif = true
            notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            AppLock.skipRelock = true
            ctx.startActivity(
                Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName)
                    .putExtra(Settings.EXTRA_CHANNEL_ID, Notifier.CH)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    val bankPrefs = LocalBankPrefs.current
    val bankOf = remember(all, bankPrefs.map) { all.associate { it.id to (Banks.detect(it.sender, it.body, bankPrefs.map)?.key ?: "") } }
    val bankKeys = remember(bankOf) { bankOf.values.groupingBy { it }.eachCount().entries.sortedByDescending { it.value }.map { it.key } }
    var pickedBank by rememberSaveable { mutableStateOf<String?>(null) }
    val bank = pickedBank?.takeIf { it in bankKeys }
    val bankView = remember(all, bank, bankOf) { if (bank == null) all else all.filter { bankOf[it.id] == bank } }

    val cards = remember(bankView) { bankView.mapNotNull { it.card }.distinct().sorted() }
    val cardBanks = remember(all, bankPrefs.map) { Banks.byCard(all, bankPrefs.map) }
    var pickedCard by rememberSaveable { mutableStateOf<String?>(null) }
    val card = pickedCard?.takeIf { it in cards }
    val xfer = remember(all) { Transfers.ids(all) }
    val view = remember(bankView, card) { if (card == null) bankView else bankView.filter { it.card == card } }

    Column(Modifier.fillMaxSize().background(P.bg).statusBarsPadding()) {
        if (tab < 2 && bankKeys.size >= 2) {
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Chip("همه بانک‌ها", selected = bank == null) { pickedBank = null; pickedCard = null }
                bankKeys.forEach { k ->
                    val b = Banks.all.firstOrNull { it.key == k }
                    Chip(b?.name ?: "نامشخص", selected = bank == k, leading = { BankBadge(b, 20) }) { pickedBank = k; pickedCard = null }
                }
            }
        }
        if (tab < 2 && cards.isNotEmpty()) {
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Chip("همه کارت‌ها", selected = card == null) { pickedCard = null }
                cards.forEach { c ->
                    val b = cardBanks[c]
                    Chip(Banks.label(b, c), selected = card == c, leading = if (b != null) ({ BankBadge(b, 20) }) else null) { pickedCard = c }
                }
            }
        }
        Box(Modifier.weight(1f)) {
            when (tab) {
                0 -> TxScreen(view, toman, smsOk, notifOk, xfer, onEdit = { editing = it }, onOpenSettings = { tab = 2 }, onFixNotif = fixNotif)
                1 -> ReportScreen(remember(view, xfer) { view.filter { it.id !in xfer } }, toman)
                else -> SettingsScreen(all, toman, { toman = it; Prefs.setToman(ctx, it); com.yadkharj.widget.TxWidget.refresh(ctx) }, permTick) { permTick++ }
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
                    onDelete = { vm.delete(tx.id); editing = null },
                    onNotBank = tx.sender.takeIf { it.isNotEmpty() && it != "manual" && it != "TEST" }
                        ?.let { s -> { vm.blockSender(s); editing = null } }
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
