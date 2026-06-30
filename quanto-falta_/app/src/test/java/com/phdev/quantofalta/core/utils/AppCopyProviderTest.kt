package com.phdev.quantofalta.core.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class AppCopyProviderTest {

    @Test
    fun testGetCategoryForIcon() {
        assertEquals(AppCopyProvider.Category.TRAVEL, AppCopyProvider.getCategoryForIcon("Airplane"))
        assertEquals(AppCopyProvider.Category.CELEBRATION, AppCopyProvider.getCategoryForIcon("Cake"))
        assertEquals(AppCopyProvider.Category.FINANCE, AppCopyProvider.getCategoryForIcon("AttachMoney"))
        assertEquals(AppCopyProvider.Category.WORK, AppCopyProvider.getCategoryForIcon("Work"))
        assertEquals(AppCopyProvider.Category.GENERAL, AppCopyProvider.getCategoryForIcon("UnknownIcon"))
    }

    @Test
    fun testGetTemporalBucket() {
        assertEquals(AppCopyProvider.TemporalBucket.COMPLETED, AppCopyProvider.getTemporalBucket("0", "dias", true, false))
        assertEquals(AppCopyProvider.TemporalBucket.TODAY, AppCopyProvider.getTemporalBucket("0", "dias", false, true))
        assertEquals(AppCopyProvider.TemporalBucket.VERY_SOON, AppCopyProvider.getTemporalBucket("5", "dias", false, true))
        assertEquals(AppCopyProvider.TemporalBucket.SOON, AppCopyProvider.getTemporalBucket("15", "dias", false, false))
        assertEquals(AppCopyProvider.TemporalBucket.DISTANT, AppCopyProvider.getTemporalBucket("40", "dias", false, false))
        assertEquals(AppCopyProvider.TemporalBucket.VERY_DISTANT, AppCopyProvider.getTemporalBucket("200", "dias", false, false))

        // Hours should be today
        assertEquals(AppCopyProvider.TemporalBucket.TODAY, AppCopyProvider.getTemporalBucket("5", "horas", false, true))
    }

    @Test
    fun testGetHighlightMessageIsDeterministic() {
        val msg1 = AppCopyProvider.getHighlightMessage("id123", "Airplane", "5", "dias", false, true, "Viagem")
        val msg2 = AppCopyProvider.getHighlightMessage("id123", "Airplane", "5", "dias", false, true, "Viagem")
        assertEquals("Highlight message should be deterministic for the same inputs.", msg1, msg2)
    }

    @Test
    fun testSanitizeTitleIsApplied() {
        val msg = AppCopyProvider.getHighlightMessage("id123", "Airplane", "5", "dias", false, true, "{prefillTitle}")
        assert(!msg.contains("{prefillTitle}"))
    }
}
