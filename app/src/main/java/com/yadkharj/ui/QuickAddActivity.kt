package com.yadkharj.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.yadkharj.data.AppDb
import com.yadkharj.data.Tx
import com.yadkharj.domain.Budgets
import com.yadkharj.domain.Prefs
import com.yadkharj.domain.TxFilter
import com.yadkharj.widget.TxWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** ثبت دستی سریع یک تراکنش (از ویجت). */
class QuickAddActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = applicationContext
        val toman = Prefs.toman(app)
        setContent {
            AppTheme {
                var kind by remember { mutableIntStateOf(0) }
                var amount by remember { mutableStateOf("") }
                var reason by remember { mutableStateOf("") }
                val rial = TxFilter.rial(amount, toman)
                Box(
                    Modifier.fillMaxSize().clickable(remember { MutableInteractionSource() }, null) { finish() }
                        .navigationBarsPadding().imePadding().padding(16.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Column(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(P.surface)
                            .clickable(remember { MutableInteractionSource() }, null) {}.padding(24.dp)
                    ) {
                        Segmented(listOf("خرج", "درآمد"), kind) { kind = it }
                        Spacer(Modifier.height(14.dp))
                        Field(amount, { amount = it }, "مبلغ (${if (toman) "تومان" else "ریال"})", Modifier.fillMaxWidth(), number = true)
                        Spacer(Modifier.height(10.dp))
                        Field(reason, { reason = it }, "دلیل (اختیاری)", Modifier.fillMaxWidth())
                        Spacer(Modifier.height(18.dp))
                        Pill("ثبت", Modifier.fillMaxWidth(), enabled = rial != null && rial > 0) {
                            val tx = Tx(
                                amount = rial!!, isIncome = kind == 1, timestamp = System.currentTimeMillis(),
                                sender = "manual", body = "", reason = reason.trim().ifEmpty { null }
                            )
                            CoroutineScope(Dispatchers.IO).launch {
                                AppDb.get(app).dao().insert(tx)
                                TxWidget.refresh(app)
                                if (!tx.isIncome) Budgets.check(app, tx.reason)
                            }
                            finish()
                        }
                    }
                }
            }
        }
    }
}
