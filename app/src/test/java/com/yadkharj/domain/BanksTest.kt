package com.yadkharj.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BanksTest {
    @Test fun detectsByNameWithPrefix() {
        assertEquals("melli", Banks.detect("", "بانک ملی ایران\nبرداشت 1,000 ریال")?.key)
        assertEquals("mellat", Banks.detect("", "بانک ملت")?.key)
        assertEquals("tosee_saderat", Banks.detect("", "بانک توسعه صادرات")?.key)
    }

    @Test fun shortNamesNeedBankPrefixAndWordEnd() {
        assertNull(Banks.detect("", "خرید از فروشگاه ملی"))
        assertNull(Banks.detect("", "بانک دیگر"))
    }

    @Test fun detectsBareAndLatinNames() {
        assertEquals("pasargad", Banks.detect("", "پاسارگاد برداشت")?.key)
        assertEquals("saman", Banks.detect("SamanBank", "برداشت")?.key)
    }

    @Test fun trafficFineIsNotATransaction() {
        assertNull(SmsParser.parse("جریمه رانندگی شما 1,200,000 ریال است. پرداخت: راهور"))
    }
}
