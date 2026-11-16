package com.yadkharj.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yadkharj.domain.DayBar
import com.yadkharj.domain.Fmt
import com.yadkharj.domain.Jalali
import com.yadkharj.domain.Reports
import com.yadkharj.domain.monthlyAverage

/** بخش «روند خرج»: خرج هر ماه به‌صورت میلهٔ افقی، پرخرج‌ترین ماه پررنگ. */
@Composable
fun YearlyTrend(bars: List<DayBar>, toman: Boolean) {
    Spacer(Modifier.height(24.dp))
    Txt("روند خرج", size = 14, weight = FontWeight.Medium)
    Spacer(Modifier.height(10.dp))
    val max = bars.maxOfOrNull { it.expense } ?: 0L
    if (max <= 0L) {
        Txt("امسال هنوز خرجی ثبت نشده", size = 12, color = P.muted)
        return
    }
    val top = bars.indexOfFirst { it.expense == max }
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(P.surface).padding(18.dp)) {
        Row {
            Txt("میانگین ماهانه", size = 13, color = P.muted, modifier = Modifier.weight(1f))
            Txt("${Fmt.money(Reports.monthlyAverage(bars), toman)} ${Fmt.unit(toman)}", size = 13, weight = FontWeight.Medium)
        }
        bars.forEachIndexed { i, b ->
            Spacer(Modifier.height(12.dp))
            val hot = i == top
            Row(verticalAlignment = Alignment.CenterVertically) {
                Txt(Jalali.months[i], size = 12, color = if (hot) P.ink else P.muted, modifier = Modifier.weight(1f), maxLines = 1)
                Txt(
                    "${Fmt.money(b.expense, toman)} ${Fmt.unit(toman)}", size = 12,
                    color = if (hot) P.expense else P.muted, weight = if (hot) FontWeight.Bold else FontWeight.Normal
                )
            }
            Spacer(Modifier.height(5.dp))
            val frac = if (b.expense > 0) (b.expense.toFloat() / max).coerceIn(0.02f, 1f) else 0f
            Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(P.bg)) {
                Box(
                    Modifier.fillMaxWidth(frac).height(6.dp).clip(RoundedCornerShape(3.dp))
                        .background(if (hot) P.expense else P.muted)
                )
            }
        }
    }
}
