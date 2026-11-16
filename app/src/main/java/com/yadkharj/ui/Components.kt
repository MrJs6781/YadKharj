package com.yadkharj.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yadkharj.data.Tx
import com.yadkharj.domain.Fmt
import kotlinx.coroutines.delay

val P: Palette
    @Composable get() = LocalPalette.current

@Composable
fun Txt(
    text: String,
    modifier: Modifier = Modifier,
    size: Int = 14,
    color: Color = P.ink,
    weight: FontWeight = FontWeight.Normal,
    align: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE
) {
    Text(
        text = text, modifier = modifier, color = color, fontSize = size.sp,
        fontFamily = Vazir, fontWeight = weight, textAlign = align,
        maxLines = maxLines, overflow = TextOverflow.Ellipsis, lineHeight = (size * 1.65f).sp
    )
}

@Composable
fun Pill(text: String, modifier: Modifier = Modifier, filled: Boolean = true, enabled: Boolean = true, onClick: () -> Unit) {
    val shape = RoundedCornerShape(50)
    Box(
        modifier
            .alpha(if (enabled) 1f else 0.35f)
            .clip(shape)
            .background(if (filled) P.accent else Color.Transparent)
            .then(if (filled) Modifier else Modifier.border(1.dp, P.line, shape))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 13.dp),
        contentAlignment = Alignment.Center
    ) {
        Txt(text, size = 14, weight = FontWeight.Medium, color = if (filled) P.onAccent else P.ink, maxLines = 1)
    }
}

@Composable
fun Chip(text: String, selected: Boolean = false, onClick: () -> Unit) {
    val shape = RoundedCornerShape(50)
    Box(
        Modifier
            .clip(shape)
            .background(if (selected) P.accent else Color.Transparent)
            .border(1.dp, if (selected) P.accent else P.line, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp)
    ) {
        Txt(text, size = 13, color = if (selected) P.onAccent else P.ink, maxLines = 1)
    }
}

@Composable
fun Segmented(options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(50)).background(P.surface).padding(4.dp)
    ) {
        options.forEachIndexed { i, o ->
            Box(
                Modifier.weight(1f).clip(RoundedCornerShape(50))
                    .background(if (i == selected) P.accent else Color.Transparent)
                    .clickable { onSelect(i) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Txt(o, size = 13, weight = FontWeight.Medium, color = if (i == selected) P.onAccent else P.muted)
            }
        }
    }
}

/** کارت پرسیدن دلیل — هم در پاپ‌آپ لحظه‌ای و هم در داخل اپ استفاده می‌شود. */
@OptIn(ExperimentalLayoutApi::class, ExperimentalComposeUiApi::class)
@Composable
fun ReasonCard(
    tx: Tx,
    suggestions: List<String>,
    toman: Boolean,
    initial: String = "",
    laterLabel: String = "بعداً وارد می‌کنم",
    onSave: (String) -> Unit,
    onLater: () -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var text by remember(tx.id) { mutableStateOf(initial) }
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(tx.id) {
        delay(250)
        runCatching { focus.requestFocus() }
        keyboard?.show()
    }
    val tone = if (tx.isIncome) P.income else P.expense

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(P.surface)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
            .padding(24.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(tone))
            Spacer(Modifier.width(8.dp))
            Txt(if (tx.isIncome) "واریز جدید" else "برداشت جدید", size = 13, color = tone, weight = FontWeight.Medium)
            Spacer(Modifier.weight(1f))
            Txt(Fmt.time(tx.timestamp), size = 12, color = P.muted)
        }
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Txt(Fmt.money(tx.amount, toman), size = 34, weight = FontWeight.Bold)
            Spacer(Modifier.width(8.dp))
            Txt(Fmt.unit(toman), size = 13, color = P.muted, modifier = Modifier.padding(bottom = 9.dp))
        }
        Spacer(Modifier.height(18.dp))

        Box(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(P.bg)
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            if (text.isEmpty()) {
                Txt(if (tx.isIncome) "این پول از کجا اومد؟" else "برای چی خرج شد؟", size = 17, color = P.muted)
            }
            BasicTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                textStyle = TextStyle(fontFamily = Vazir, fontSize = 17.sp, color = P.ink),
                cursorBrush = SolidColor(P.ink),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { if (text.isNotBlank()) onSave(text) }),
                modifier = Modifier.fillMaxWidth().focusRequester(focus)
            )
        }
        Spacer(Modifier.height(12.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            suggestions.forEach { s -> Chip(s, selected = text == s) { text = s } }
        }
        Spacer(Modifier.height(20.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Pill("ثبت", Modifier.weight(1f), enabled = text.isNotBlank()) { onSave(text) }
            Spacer(Modifier.width(10.dp))
            Pill(laterLabel, filled = false, onClick = onLater)
        }
        if (onDelete != null) {
            Txt(
                "حذف این تراکنش", size = 12, color = P.muted, align = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 14.dp).clickable(onClick = onDelete)
            )
        }
    }
}
