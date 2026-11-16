package com.yadkharj.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class RecurringTest {
    @Test fun dueOnDayAndClampedAtMonthEnd() {
        // 1 فروردین 1404 = 2025-03-21
        val r = Recurring("اجاره", 1, 1)
        assertTrue(r.dueOn(LocalDate.of(2025, 3, 21)))
        assertFalse(r.dueOn(LocalDate.of(2025, 3, 22)))
        // 31 در مهر (۳۰ روزه) به روز ۳۰ می‌افتد: 30 مهر 1404 = 2025-10-22
        assertTrue(Recurring("قسط", 1, 31).dueOn(LocalDate.of(2025, 10, 22)))
    }
}

class SmsCardBalanceTest {
    @Test fun parsesCardAndBalance() {
        val p = SmsParser.parse("بانک\nبرداشت 1,250,000 ریال\nکارت ****۱۲۳۴\nمانده 8,400,000 ریال")!!
        org.junit.Assert.assertEquals("1234", p.card)
        org.junit.Assert.assertEquals(8_400_000L, p.balanceRial)
        org.junit.Assert.assertEquals(1_250_000L, p.amountRial)
    }
}
