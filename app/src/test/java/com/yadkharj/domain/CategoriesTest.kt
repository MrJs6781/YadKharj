package com.yadkharj.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class CategoriesTest {
    @Test fun keywordsAndOverrides() {
        assertEquals("غذا و رستوران", Categories.of("ناهار با بچه‌ها"))
        assertEquals("حمل‌ونقل", Categories.of("تاکسی"))
        assertEquals("غذا و رستوران", Categories.of("اسنپ فود"))
        assertEquals("قبض و شارژ", Categories.of("قبض برق"))
        assertEquals("سلامت", Categories.of("دارو"))
        assertEquals(Categories.OTHER, Categories.of("چیز عجیب"))
        assertEquals("تفریح", Categories.of("چیز عجیب", mapOf("چیز عجیب" to "تفریح")))
    }
}
