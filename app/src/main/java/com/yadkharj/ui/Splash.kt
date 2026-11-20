package com.yadkharj.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.yadkharj.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** اسپلش فقط یک بار در هر اجرای برنامه نمایش داده می‌شود (نه بعد از چرخش صفحه). */
object Splash {
    @Volatile var shown = false
}

private val SplashBg = Color(0xFF14171A)
private val SplashTile = Color(0xFF1E2327)
private val SplashInk = Color(0xFFF6F5F2)
private val SplashMuted = Color(0xFF8B9096)
private val SplashAccent = Color(0xFF4CC38A)

/** محتوای برنامه را از همان اول می‌سازد (تا دیر شروع نشود) و یک لایهٔ متحرک رویش نشان می‌دهد. */
@Composable
fun SplashGate(content: @Composable () -> Unit) {
    var overlay by rememberSaveable { mutableStateOf(!Splash.shown) }
    Box(Modifier.fillMaxSize()) {
        content()
        if (overlay) SplashOverlay { overlay = false; Splash.shown = true }
    }
}

@Composable
private fun SplashOverlay(onDone: () -> Unit) {
    val logo = remember { Animatable(0f) }
    val title = remember { Animatable(0f) }
    val fade = remember { Animatable(1f) }
    LaunchedEffect(Unit) {
        launch { logo.animateTo(1f, tween(600, easing = FastOutSlowInEasing)) }
        delay(200)
        launch { title.animateTo(1f, tween(450, easing = FastOutSlowInEasing)) }
        delay(850)
        fade.animateTo(0f, tween(260, easing = FastOutSlowInEasing))
        onDone()
    }
    Column(
        Modifier.fillMaxSize()
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
            .graphicsLayer { alpha = fade.value }.background(SplashBg),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier.size(96.dp)
                .graphicsLayer {
                    val s = 0.82f + 0.18f * logo.value
                    scaleX = s
                    scaleY = s
                    alpha = logo.value
                }
                .clip(RoundedCornerShape(26.dp))
                .background(SplashTile),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painterResource(R.drawable.ic_launcher_fg), contentDescription = null,
                Modifier.size(88.dp), contentScale = ContentScale.Fit
            )
        }
        Spacer(Modifier.height(26.dp))
        Column(
            Modifier.graphicsLayer {
                translationY = (1f - title.value) * 14.dp.toPx()
                alpha = title.value
            },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Txt("YadKharj", size = 30, weight = FontWeight.Bold, color = SplashInk, align = TextAlign.Center)
            Spacer(Modifier.height(6.dp))
            Txt(stringResource(R.string.app_tagline), size = 13, color = SplashMuted, align = TextAlign.Center)
            Spacer(Modifier.height(18.dp))
            Box(Modifier.height(2.dp).width((64 * logo.value).dp).background(SplashAccent))
        }
    }
}
