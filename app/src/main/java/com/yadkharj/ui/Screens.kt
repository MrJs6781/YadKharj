package com.yadkharj.ui

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.yadkharj.data.Tx
import com.yadkharj.data.AppDb
import com.yadkharj.domain.TxCsv
import com.yadkharj.widget.TxWidget
import com.yadkharj.domain.Banks
import com.yadkharj.domain.Budgets
import com.yadkharj.domain.Senders
import com.yadkharj.domain.DayBar
import com.yadkharj.domain.Delta
import com.yadkharj.domain.Fmt
import com.yadkharj.domain.Periods
import com.yadkharj.domain.year
import com.yadkharj.domain.monthlyBars
import com.yadkharj.domain.Perms
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.yadkharj.domain.SmsImport
import com.yadkharj.domain.Recurring
import com.yadkharj.domain.RecurringStore
import com.yadkharj.domain.Report
import com.yadkharj.domain.Reports
import com.yadkharj.domain.Slice
import com.yadkharj.domain.TxFilter
import com.yadkharj.domain.TxProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate

/* ───────────── تراکنش‌ها ───────────── */

private sealed interface Entry {
    data class Head(val label: String) : Entry
    data class Item(val tx: Tx) : Entry
}

private fun buildEntries(list: List<Tx>): List<Entry> {
    val out = mutableListOf<Entry>()
    var last: LocalDate? = null
    for (t in list) {
        val d = Fmt.date(t.timestamp)
        if (d != last) { out += Entry.Head(Fmt.dayLabel(d)); last = d }
        out += Entry.Item(t)
    }
    return out
}

@Composable
fun TxScreen(all: List<Tx>, toman: Boolean, smsGranted: Boolean, transferIds: Set<Long>, onEdit: (Tx) -> Unit, onOpenSettings: () -> Unit) {
    val ctx = LocalContext.current
    var onlyPending by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var days by rememberSaveable { mutableIntStateOf(0) }
    var minTxt by rememberSaveable { mutableStateOf("") }
    var maxTxt by rememberSaveable { mutableStateOf("") }
    var showFilters by rememberSaveable { mutableStateOf(false) }
    val pending = all.count { it.reason == null && it.id !in transferIds }
    val filter = TxFilter(query, days, TxFilter.rial(minTxt, toman), TxFilter.rial(maxTxt, toman), onlyPending)
    val entries = remember(all, filter) { buildEntries(filter.apply(all).let { l -> if (onlyPending) l.filter { it.id !in transferIds } else l }) }
    val cardBanks = remember(all) { Banks.byCard(all) }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)) {
        item {
            Column(Modifier.padding(top = 16.dp, bottom = 12.dp)) {
                Txt(Fmt.todayLong(), size = 12, color = P.muted)
                Txt("تراکنش‌ها", size = 28, weight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Chip("همه", selected = !onlyPending) { onlyPending = false }
                    val p = if (pending > 0) " · ${Fmt.digits(pending.toString())}" else ""
                    Chip("بدون دلیل$p", selected = onlyPending) { onlyPending = true }
                    Chip(if (filter.active) "فیلتر ●" else "فیلتر", selected = showFilters) { showFilters = !showFilters }
                    Chip("+ ثبت دستی") { ctx.startActivity(Intent(ctx, QuickAddActivity::class.java)) }
                }
                Spacer(Modifier.height(10.dp))
                Field(query, { query = it }, "جستجو در دلیل‌ها…", Modifier.fillMaxWidth())
                if (showFilters) {
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Chip("همه تاریخ‌ها", selected = days == 0) { days = 0 }
                        Chip("۷ روز اخیر", selected = days == 7) { days = 7 }
                        Chip("۳۰ روز اخیر", selected = days == 30) { days = 30 }
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Field(minTxt, { minTxt = it }, "حداقل مبلغ (${Fmt.unit(toman)})", Modifier.weight(1f), number = true)
                        Field(maxTxt, { maxTxt = it }, "حداکثر مبلغ", Modifier.weight(1f), number = true)
                    }
                }
            }
        }
        val balances = all.filter { it.balance != null }.groupBy { it.card }.map { (c, l) -> c to l.maxByOrNull { it.timestamp }!! }
        if (balances.isNotEmpty()) {
            item {
                Column(Modifier.fillMaxWidth().padding(bottom = 12.dp).clip(RoundedCornerShape(20.dp)).background(P.surface).padding(18.dp)) {
                    Txt("آخرین مانده", size = 12, color = P.muted)
                    balances.forEach { (c, t) ->
                        val b = c?.let { cardBanks[it] } ?: Banks.detect(t.sender, t.body)
                        Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            BankBadge(b, 24)
                            Spacer(Modifier.width(8.dp))
                            Txt(Banks.label(b, c), size = 13, modifier = Modifier.weight(1f))
                            Txt("${Fmt.money(t.balance!!, toman)} ${Fmt.unit(toman)}", size = 13, weight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
        if (!smsGranted) {
            item {
                Column(
                    Modifier.fillMaxWidth().padding(bottom = 12.dp).clip(RoundedCornerShape(20.dp))
                        .background(P.surface).clickable(onClick = onOpenSettings).padding(18.dp)
                ) {
                    Txt("برای شروع، دسترسی پیامک رو فعال کن", size = 14, weight = FontWeight.Medium)
                    Txt("لمس کن تا به تنظیمات بریم", size = 12, color = P.muted)
                }
            }
        }
        if (entries.isEmpty()) {
            item {
                Txt(
                    if (filter.active) "چیزی پیدا نشد." else if (onlyPending) "همهٔ تراکنش‌ها دلیل دارن 👌" else "هنوز تراکنشی نیست.\nوقتی پیامک بانکی بیاد، خودکار اینجا ثبت می‌شه.",
                    size = 14, color = P.muted, align = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 80.dp)
                )
            }
        }
        items(entries) { e ->
            when (e) {
                is Entry.Head -> Txt(e.label, size = 12, color = P.muted, modifier = Modifier.padding(top = 18.dp, bottom = 4.dp))
                is Entry.Item -> TxRow(e.tx, toman, e.tx.id in transferIds) { onEdit(e.tx) }
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun TxRow(tx: Tx, toman: Boolean, isTransfer: Boolean = false, onClick: () -> Unit) {
    val tone = if (tx.isIncome) P.income else P.expense
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable(onClick = onClick).padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.width(3.dp).height(34.dp).clip(RoundedCornerShape(2.dp)).background(tone))
        Spacer(Modifier.width(12.dp))
        val bank = remember(tx.sender, tx.body) { Banks.detect(tx.sender, tx.body) }
        BankBadge(bank, 28)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            if (tx.reason != null) {
                Txt(tx.reason, size = 15, weight = FontWeight.Medium, maxLines = 1)
            } else {
                Txt("بدون دلیل — لمس کن", size = 14, color = P.warn, weight = FontWeight.Medium, maxLines = 1)
            }
            val cardTag = tx.card?.let { " · کارت ${Fmt.digits(it)}" }.orEmpty()
            val xfer = if (isTransfer) " · انتقال بین کارت‌ها" else ""
            Txt("${Fmt.time(tx.timestamp)} · ${if (tx.isIncome) "واریز" else "برداشت"}$cardTag$xfer", size = 12, color = P.muted)
        }
        Spacer(Modifier.width(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Txt(if (tx.isIncome) "+" else "−", size = 15, color = tone, weight = FontWeight.Medium)
            Spacer(Modifier.width(3.dp))
            Txt(Fmt.money(tx.amount, toman), size = 15, weight = FontWeight.Medium)
            Spacer(Modifier.width(4.dp))
            Txt(Fmt.unit(toman), size = 11, color = P.muted)
        }
    }
}

/* ───────────── گزارش ───────────── */

@Composable
fun ReportScreen(all: List<Tx>, toman: Boolean) {
    var mode by rememberSaveable { mutableIntStateOf(0) }
    var offset by rememberSaveable { mutableIntStateOf(0) }
    val today = remember(all) { LocalDate.now() }
    val period = remember(mode, offset, today) {
        when (mode) { 0 -> Periods.week(offset, today); 2 -> Periods.year(offset, today); else -> Periods.month(offset, today) }
    }
    val report = remember(all, period) {
        Reports.build(all, period).let { if (mode == 2) it.copy(bars = Reports.monthlyBars(all, period)) else it }
    }
    val net = report.income - report.expense
    val deltas = remember(all, offset, mode) {
        if (mode == 1) Reports.compare(all, period, Periods.month(offset - 1, today)) else emptyList()
    }
    val rctx = LocalContext.current
    val budgetStatus = remember(all, period, mode) {
        if (mode == 1) Budgets.status(Budgets.load(rctx), all, period) else emptyList()
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(16.dp))
        Txt("گزارش", size = 28, weight = FontWeight.Bold)
        Spacer(Modifier.height(14.dp))
        Segmented(listOf("هفتگی", "ماهانه", "سالانه"), mode) { mode = it; offset = 0 }
        Spacer(Modifier.height(16.dp))

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Txt("قبلی", size = 13, color = P.muted, modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable { offset-- }.padding(8.dp))
            Txt(report.period.title, size = 15, weight = FontWeight.Medium, align = TextAlign.Center, modifier = Modifier.weight(1f))
            Txt(
                "بعدی", size = 13, color = P.muted,
                modifier = Modifier.alpha(if (offset < 0) 1f else 0.25f).clip(RoundedCornerShape(10.dp))
                    .clickable(enabled = offset < 0) { offset++ }.padding(8.dp)
            )
        }
        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SummaryCard("درآمد", Fmt.money(report.income, toman), Fmt.unit(toman), P.income, Modifier.weight(1f))
            SummaryCard("خرج", Fmt.money(report.expense, toman), Fmt.unit(toman), P.expense, Modifier.weight(1f))
        }
        Spacer(Modifier.height(12.dp))
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(P.surface).padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Txt("تراز این بازه", size = 13, color = P.muted, modifier = Modifier.weight(1f))
            Txt(if (net >= 0) "+" else "−", size = 15, color = if (net >= 0) P.income else P.expense, weight = FontWeight.Medium)
            Spacer(Modifier.width(3.dp))
            Txt(Fmt.money(kotlin.math.abs(net), toman), size = 16, weight = FontWeight.Bold)
            Spacer(Modifier.width(4.dp))
            Txt(Fmt.unit(toman), size = 11, color = P.muted)
        }

        if (budgetStatus.isNotEmpty()) {
            Spacer(Modifier.height(20.dp))
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(P.surface).padding(18.dp)) {
                Txt("بودجه", size = 12, color = P.muted)
                budgetStatus.forEach { BudgetRow(it, toman) }
            }
        }

        if (deltas.isNotEmpty()) {
            Spacer(Modifier.height(20.dp))
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(P.surface).padding(18.dp)) {
                Txt("نسبت به ماه قبل", size = 12, color = P.muted)
                deltas.forEach { DeltaRow(it) }
            }
        }

        Spacer(Modifier.height(20.dp))
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(P.surface).padding(18.dp)) {
            Bars(report.bars)
            Spacer(Modifier.height(10.dp))
            LabelsRow(report.bars, sparse = !report.period.isWeek && mode != 2)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Legend("درآمد", P.income)
                Legend("خرج", P.expense)
            }
        }

        if (mode == 2) YearlyTrend(report.bars, toman)
        SliceSection("خرج‌ها بر اساس دلیل", report.expenseSlices, report.expense, P.expense, toman)
        SliceSection("درآمدها بر اساس دلیل", report.incomeSlices, report.income, P.income, toman)
        SliceSection("خرج‌ها بر اساس دسته", remember(all, period) { com.yadkharj.domain.Categories.slices(all, period, com.yadkharj.domain.Categories.load(rctx)) }, report.expense, P.expense, toman)

        if (report.pendingCount > 0) {
            Txt(
                "${Fmt.digits(report.pendingCount.toString())} تراکنش این بازه هنوز دلیل ندارن؛ از تب «تراکنش‌ها» کامل‌شون کن تا گزارش دقیق‌تر بشه.",
                size = 12, color = P.warn, modifier = Modifier.padding(top = 20.dp)
            )
        }
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun DeltaRow(d: Delta) {
    val up = d.cur > d.prev
    val same = d.cur == d.prev
    val text = when {
        same -> "بدون تغییر"
        d.pct == null -> "جدید"
        else -> "${Fmt.digits(Math.abs(d.pct!!).toString())}٪ ${if (up) "بیشتر" else "کمتر"}"
    }
    Spacer(Modifier.height(10.dp))
    Row {
        Txt(d.label, size = 13, modifier = Modifier.weight(1f), maxLines = 1)
        Txt(text, size = 13, color = if (same) P.muted else if (up) P.expense else P.income)
    }
}

@Composable
private fun BudgetRow(st: Budgets.Status, toman: Boolean) {
    val tone = when { st.ratio >= 1 -> P.expense; st.ratio >= Budgets.WARN -> P.warn; else -> P.income }
    Spacer(Modifier.height(12.dp))
    Row {
        Txt(st.label, size = 13, modifier = Modifier.weight(1f), maxLines = 1)
        Txt("${Fmt.money(st.spent, toman)} از ${Fmt.money(st.limit, toman)} ${Fmt.unit(toman)}", size = 12, color = tone)
    }
    Spacer(Modifier.height(6.dp))
    Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(P.bg)) {
        Box(Modifier.fillMaxWidth(st.ratio.toFloat().coerceIn(0.02f, 1f)).height(6.dp).clip(RoundedCornerShape(3.dp)).background(tone))
    }
}

@Composable
private fun SummaryCard(title: String, value: String, unit: String, tone: Color, modifier: Modifier) {
    Column(modifier.clip(RoundedCornerShape(20.dp)).background(P.surface).padding(18.dp)) {
        Txt(title, size = 12, color = P.muted)
        Spacer(Modifier.height(6.dp))
        Txt(value, size = 20, weight = FontWeight.Bold, color = tone, maxLines = 1)
        Txt(unit, size = 11, color = P.muted)
    }
}

@Composable
private fun Legend(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(8.dp).height(8.dp).clip(RoundedCornerShape(4.dp)).background(color))
        Spacer(Modifier.width(6.dp))
        Txt(label, size = 11, color = P.muted)
    }
}

@Composable
private fun Bars(bars: List<DayBar>) {
    val inc = P.income
    val exp = P.expense
    val line = P.line
    val max = (bars.maxOfOrNull { maxOf(it.income, it.expense) } ?: 0L).coerceAtLeast(1L).toFloat()
    Canvas(Modifier.fillMaxWidth().height(140.dp)) {
        val n = bars.size.coerceAtLeast(1)
        val slot = size.width / n
        val bw = (slot * 0.3f).coerceAtMost(10.dp.toPx())
        val gap = 1.5.dp.toPx()
        val minH = 3.dp.toPx()
        drawLine(line, Offset(0f, size.height), Offset(size.width, size.height), 1.dp.toPx())
        bars.forEachIndexed { i, b ->
            // روز اول سمت راست (جهت خواندن فارسی)
            val cx = size.width - (i + 0.5f) * slot
            if (b.income > 0) {
                val h = (b.income / max * size.height).coerceAtLeast(minH)
                drawRoundRect(inc, Offset(cx - bw - gap / 2, size.height - h), Size(bw, h), CornerRadius(bw / 2))
            }
            if (b.expense > 0) {
                val h = (b.expense / max * size.height).coerceAtLeast(minH)
                drawRoundRect(exp, Offset(cx + gap / 2, size.height - h), Size(bw, h), CornerRadius(bw / 2))
            }
        }
    }
}

@Composable
private fun LabelsRow(bars: List<DayBar>, sparse: Boolean) {
    Row(Modifier.fillMaxWidth()) {
        bars.forEachIndexed { i, b ->
            val show = !sparse || i == 0 || (i + 1) % 5 == 0
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                if (show) Txt(b.label, size = if (sparse) 9 else 11, color = P.muted, maxLines = 1)
            }
        }
    }
}

@Composable
private fun SliceSection(title: String, slices: List<Slice>, total: Long, tone: Color, toman: Boolean) {
    Spacer(Modifier.height(24.dp))
    Txt(title, size = 14, weight = FontWeight.Medium)
    Spacer(Modifier.height(10.dp))
    if (slices.isEmpty()) {
        Txt("داده‌ای برای این بازه نیست", size = 12, color = P.muted)
        return
    }
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(P.surface).padding(18.dp)) {
        slices.forEachIndexed { i, s ->
            if (i > 0) Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Txt(s.label, size = 13, color = if (s.unlabeled) P.warn else P.ink, modifier = Modifier.weight(1f), maxLines = 1)
                Txt("${Fmt.money(s.amount, toman)} ${Fmt.unit(toman)}", size = 12, color = P.muted)
            }
            Spacer(Modifier.height(6.dp))
            val frac = if (total > 0) (s.amount.toFloat() / total).coerceIn(0.02f, 1f) else 0f
            Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(P.bg)) {
                Box(
                    Modifier.fillMaxWidth(frac).height(6.dp).clip(RoundedCornerShape(3.dp))
                        .background(if (s.unlabeled) P.warn else tone)
                )
            }
        }
    }
}

/* ───────────── تنظیمات ───────────── */

@Composable
fun SettingsScreen(all: List<Tx>, toman: Boolean, onToman: (Boolean) -> Unit, permTick: Int, onChanged: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { res ->
        onChanged()
        // اندروید ۱۳+: برای APKِ نصب‌شده از فایل، مجوز پیامک «محدود» است و دیالوگ بدون نمایش گزینه رد می‌شود.
        // گزینهٔ «Allow restricted settings» فقط در صفحهٔ اطلاعات برنامه (سه‌نقطه) پیدا می‌شود؛ مستقیم بازش می‌کنیم.
        if (res[Manifest.permission.RECEIVE_SMS] == false && !Perms.sms(ctx)) {
            AppLock.skipRelock = true
            ctx.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${ctx.packageName}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }
    var testSent by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(16.dp))
        Txt("تنظیمات", size = 28, weight = FontWeight.Bold)
        Spacer(Modifier.height(20.dp))

        Txt("دسترسی‌ها", size = 12, color = P.muted)
        Spacer(Modifier.height(8.dp))
        PermRow("خواندن پیامک", "برای تشخیص پیامک‌های بانکی", Perms.sms(ctx)) {
            launcher.launch(arrayOf(Manifest.permission.RECEIVE_SMS))
        }
        PermRow("اعلان‌ها", "برای وقتی که پنجرهٔ فوری باز نشه", Perms.notif(ctx)) {
            if (Build.VERSION.SDK_INT >= 33) launcher.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS))
        }
        PermRow("نمایش روی سایر برنامه‌ها", "تا پنجرهٔ «دلیلش چی بود؟» همون لحظه باز بشه", Perms.overlay(ctx)) {
            AppLock.skipRelock = true
            ctx.startActivity(
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${ctx.packageName}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
        Txt(
            "اگه گفت «App was denied access»: بعد از رد شدن، صفحهٔ اطلاعات برنامه باز می‌شه ← سه‌نقطهٔ بالا ← «Allow restricted settings» ← برگرد و دوباره «فعال‌سازی» رو بزن.",
            size = 11, color = P.muted, modifier = Modifier.padding(top = 8.dp)
        )

        Spacer(Modifier.height(28.dp))
        BackupRow(all)

        Spacer(Modifier.height(28.dp))
        SendersEditor(all)

        Spacer(Modifier.height(28.dp))
        ImportRow()

        Spacer(Modifier.height(28.dp))
        BudgetEditor(toman)

        Spacer(Modifier.height(28.dp))
        CategoryEditor(all)

        Spacer(Modifier.height(28.dp))
        RecurringEditor(toman)

        Spacer(Modifier.height(28.dp))
        AppLockRow()

        Spacer(Modifier.height(28.dp))
        Txt("واحد نمایش مبلغ", size = 12, color = P.muted)
        Spacer(Modifier.height(8.dp))
        Segmented(listOf("تومان", "ریال"), if (toman) 0 else 1) { onToman(it == 0) }

        Spacer(Modifier.height(28.dp))
        Txt("آزمایش", size = 12, color = P.muted)
        Spacer(Modifier.height(8.dp))
        Pill(if (testSent) "ارسال شد — پنجره باید باز بشه" else "ارسال پیامک آزمایشی", filled = false, modifier = Modifier.fillMaxWidth()) {
            testSent = true
            scope.launch(Dispatchers.IO) {
                TxProcessor.process(
                    ctx.applicationContext, "TEST",
                    "بانک آزمایشی\nبرداشت 1,250,000 ریال\nمانده 8,400,000 ریال", dedupe = false
                )
            }
        }
        Spacer(Modifier.height(24.dp))
        Txt("همهٔ داده‌ها فقط روی همین گوشی ذخیره می‌شن و جایی ارسال نمی‌شن.", size = 11, color = P.muted)
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun BudgetEditor(toman: Boolean) {
    val ctx = LocalContext.current
    var budgets by remember { mutableStateOf(Budgets.load(ctx)) }
    var reason by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    val rial = TxFilter.rial(amount, toman)
    fun update(m: Map<String, Long>) { budgets = m; Budgets.save(ctx, m) }

    Txt("سقف بودجهٔ ماهانه", size = 12, color = P.muted)
    Spacer(Modifier.height(8.dp))
    budgets.forEach { (k, v) ->
        Row(Modifier.fillMaxWidth().padding(bottom = 8.dp).clip(RoundedCornerShape(18.dp)).background(P.surface).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Txt(if (k == Budgets.TOTAL) "کل ماه" else k, size = 14, modifier = Modifier.weight(1f), maxLines = 1)
            Txt("${Fmt.money(v, toman)} ${Fmt.unit(toman)}", size = 12, color = P.muted)
            Txt("  حذف", size = 12, color = P.warn, modifier = Modifier.clickable { update(budgets - k) })
        }
    }
    Field(reason, { reason = it }, "دلیل (خالی = کل ماه)", Modifier.fillMaxWidth())
    Spacer(Modifier.height(8.dp))
    Field(amount, { amount = it }, "سقف (${Fmt.unit(toman)})", Modifier.fillMaxWidth(), number = true)
    Spacer(Modifier.height(8.dp))
    Pill("افزودن سقف", filled = false, enabled = rial != null && rial > 0, modifier = Modifier.fillMaxWidth()) {
        update(budgets + (reason.trim() to rial!!)); reason = ""; amount = ""
    }
    Txt("وقتی خرج به ۸۰٪ یا ۱۰۰٪ سقف برسه، اعلان می‌گیری.", size = 11, color = P.muted, modifier = Modifier.padding(top = 8.dp))
}

@Composable
private fun BackupRow(all: List<Tx>) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var msg by remember { mutableStateOf<String?>(null) }
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) scope.launch(Dispatchers.IO) {
            msg = runCatching {
                ctx.contentResolver.openOutputStream(uri)!!.use { it.write(TxCsv.write(all).toByteArray(Charsets.UTF_8)) }
                "${Fmt.digits(all.size.toString())} تراکنش ذخیره شد."
            }.getOrElse { "ذخیره نشد." }
        }
    }
    val restore = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch(Dispatchers.IO) {
            msg = runCatching {
                val list = TxCsv.read(ctx.contentResolver.openInputStream(uri)!!.use { it.readBytes().toString(Charsets.UTF_8) })
                val dao = AppDb.get(ctx).dao()
                val have = dao.all().map { Triple(it.timestamp, it.amount, it.isIncome) }.toHashSet()
                val fresh = list.filter { Triple(it.timestamp, it.amount, it.isIncome) !in have }
                dao.insertAll(fresh)
                TxWidget.refresh(ctx)
                "${Fmt.digits(fresh.size.toString())} تراکنش جدید بازیابی شد."
            }.getOrElse { "فایل خوانده نشد." }
        }
    }
    Txt("پشتیبان‌گیری", size = 12, color = P.muted)
    Spacer(Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Pill("خروجی CSV", filled = false, modifier = Modifier.weight(1f)) { AppLock.skipRelock = true; export.launch("yadkharj.csv") }
        Pill("بازیابی از CSV", filled = false, modifier = Modifier.weight(1f)) { AppLock.skipRelock = true; restore.launch(arrayOf("*/*")) }
    }
    msg?.let { Txt(it, size = 11, color = P.muted, modifier = Modifier.padding(top = 8.dp)) }
    Txt("فایل CSV همهٔ تراکنش‌ها رو داره؛ هر جا خواستی نگهش دار و بعداً بازیابی کن. بودجه و یادآورها داخلش نیست.", size = 11, color = P.muted, modifier = Modifier.padding(top = 8.dp))
}

@Composable
private fun SendersEditor(all: List<Tx>) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var blocked by remember { mutableStateOf(Senders.blocked(ctx)) }
    var confirm by remember { mutableStateOf<String?>(null) }
    val rows = remember(all) {
        all.groupBy { it.sender }.filterKeys { it.isNotEmpty() && it != "manual" && it != "TEST" }
            .map { (s, l) -> Triple(s, l.size, Banks.detect(s, l.first().body)) }
            .sortedByDescending { it.second }
    }

    Txt("فرستنده‌های پیامک", size = 12, color = P.muted)
    Spacer(Modifier.height(8.dp))
    if (rows.isEmpty()) Txt("هنوز پیامکی ثبت نشده.", size = 12, color = P.muted)
    rows.forEach { (s, n, bank) ->
        Row(Modifier.fillMaxWidth().padding(bottom = 8.dp).clip(RoundedCornerShape(18.dp)).background(P.surface).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            BankBadge(bank, 28)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Txt(s, size = 14, maxLines = 1)
                Txt("${Fmt.digits(n.toString())} تراکنش${bank?.let { " · ${it.name}" }.orEmpty()}", size = 11, color = P.muted)
            }
            Txt(
                if (confirm == s) "تأیید حذف" else "بانک نیست", size = 12, color = P.warn,
                modifier = Modifier.clickable {
                    if (confirm != s) confirm = s
                    else {
                        confirm = null
                        scope.launch(Dispatchers.IO) { Senders.block(ctx.applicationContext, s); blocked = Senders.blocked(ctx) }
                    }
                }.padding(6.dp)
            )
        }
    }
    if (blocked.isNotEmpty()) {
        Spacer(Modifier.height(8.dp))
        Txt("نادیده‌گرفته‌شده‌ها", size = 12, color = P.muted)
        blocked.forEach { s ->
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Txt(s, size = 13, modifier = Modifier.weight(1f), maxLines = 1)
                Txt("بازگرداندن", size = 12, color = P.muted, modifier = Modifier.clickable { Senders.unblock(ctx, s); blocked = Senders.blocked(ctx) }.padding(6.dp))
            }
        }
    }
    Txt("با «بانک نیست»، تراکنش‌های قبلی این فرستنده پاک می‌شن و پیامک‌های بعدی‌اش ثبت نمی‌شن.", size = 11, color = P.muted, modifier = Modifier.padding(top = 8.dp))
}

@Composable
private fun ImportRow() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var msg by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    fun go() {
        busy = true
        scope.launch(Dispatchers.IO) {
            val n = runCatching { SmsImport.run(ctx.applicationContext) }.getOrNull()
            msg = if (n == null) "خواندن پیامک‌ها ناموفق بود." else if (n == 0) "تراکنش جدیدی پیدا نشد." else "${Fmt.digits(n.toString())} تراکنش وارد شد."
            busy = false
        }
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) go() else {
            msg = "دسترسی داده نشد. اگه «App was denied access» دیدی: اطلاعات برنامه ← سه‌نقطه ← Allow restricted settings."
            AppLock.skipRelock = true
            ctx.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${ctx.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }
    Txt("پیامک‌های قدیمی", size = 12, color = P.muted)
    Spacer(Modifier.height(8.dp))
    Pill(if (busy) "در حال خواندن…" else "واردکردن از صندوق پیامک", filled = false, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED) go()
        else launcher.launch(Manifest.permission.READ_SMS)
    }
    msg?.let { Txt(it, size = 11, color = P.muted, modifier = Modifier.padding(top = 8.dp)) }
}

@Composable
private fun RecurringEditor(toman: Boolean) {
    val ctx = LocalContext.current
    var items by remember { mutableStateOf(RecurringStore.load(ctx)) }
    var title by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var day by remember { mutableStateOf("") }
    val rial = TxFilter.rial(amount, toman)
    val d = TxFilter.rial(day, false)?.toInt()
    fun update(l: List<Recurring>) { items = l; RecurringStore.save(ctx, l) }

    Txt("پرداخت‌های تکراری (یادآور)", size = 12, color = P.muted)
    Spacer(Modifier.height(8.dp))
    items.forEach { r ->
        Row(Modifier.fillMaxWidth().padding(bottom = 8.dp).clip(RoundedCornerShape(18.dp)).background(P.surface).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Txt(r.title, size = 14, maxLines = 1)
                Txt("هر ماه روز ${Fmt.digits(r.day.toString())} · ${Fmt.money(r.amount, toman)} ${Fmt.unit(toman)}", size = 11, color = P.muted)
            }
            Txt("حذف", size = 12, color = P.warn, modifier = Modifier.clickable { update(items - r) })
        }
    }
    Field(title, { title = it }, "عنوان (مثلاً اجاره)", Modifier.fillMaxWidth())
    Spacer(Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Field(amount, { amount = it }, "مبلغ (${Fmt.unit(toman)})", Modifier.weight(1f), number = true)
        Field(day, { day = it }, "روز ماه (۱–۳۱)", Modifier.weight(1f), number = true)
    }
    Spacer(Modifier.height(8.dp))
    val ok = title.isNotBlank() && rial != null && rial > 0 && d != null && d in 1..31
    Pill("افزودن یادآور", filled = false, enabled = ok, modifier = Modifier.fillMaxWidth()) {
        update(items + Recurring(title.trim(), rial!!, d!!)); title = ""; amount = ""; day = ""
    }
    Txt("یک روز قبل و روز سررسید، ساعت ۹ صبح اعلان می‌گیری.", size = 11, color = P.muted, modifier = Modifier.padding(top = 8.dp))
}

@Composable
private fun PermRow(title: String, desc: String, granted: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(bottom = 8.dp).clip(RoundedCornerShape(18.dp)).background(P.surface)
            .clickable(enabled = !granted, onClick = onClick).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Txt(title, size = 14, weight = FontWeight.Medium)
            Txt(desc, size = 11, color = P.muted)
        }
        Spacer(Modifier.width(10.dp))
        Txt(if (granted) "فعال ✓" else "فعال‌سازی", size = 12, weight = FontWeight.Medium, color = if (granted) P.income else P.warn)
    }
}
