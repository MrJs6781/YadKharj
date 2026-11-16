package com.yadkharj.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.yadkharj.data.Tx
import com.yadkharj.domain.Categories

/** دلیل‌هایی که به «سایر» افتادن؛ برای هرکدوم می‌شه دسته انتخاب کرد. */
@Composable
fun CategoryEditor(all: List<Tx>) {
    val ctx = LocalContext.current
    var overrides by remember { mutableStateOf(Categories.load(ctx)) }
    // ponytail: لیست از روی تراکنش‌ها و overrideها حساب می‌شه؛ بعد از انتخاب، دلیل از لیست می‌ره.
    val rows = remember(all, overrides) { Categories.uncategorized(all, overrides) }

    Txt("دسته‌ها", size = 12, color = P.muted)
    Spacer(Modifier.height(8.dp))
    if (rows.isEmpty()) Txt("همهٔ دلیل‌ها یه دسته دارن.", size = 12, color = P.muted)
    rows.forEach { r ->
        Column(Modifier.fillMaxWidth().padding(bottom = 8.dp).clip(RoundedCornerShape(18.dp)).background(P.surface).padding(14.dp)) {
            Txt(r, size = 14, maxLines = 1)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Categories.ALL.filter { it != Categories.OTHER }.forEach { c ->
                    Chip(c, selected = false) {
                        val m = overrides + (r to c)
                        overrides = m
                        Categories.save(ctx, m)
                    }
                }
            }
        }
    }
    Txt("دلیل‌هایی که خودکار دسته‌شون رو نفهمیدیم؛ یه دسته بهشون بده تا تو گزارش درست بشمارن.", size = 11, color = P.muted)
}
