package com.phdev.quantofalta.core.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EventMessageProviderTest {

    @Test
    fun `getCategoryForIcon maps correctly`() {
        assertEquals(EventMessageProvider.Category.TRAVEL, EventMessageProvider.getCategoryForIcon("Airplane"))
        assertEquals(EventMessageProvider.Category.CELEBRATION, EventMessageProvider.getCategoryForIcon("Cake"))
        assertEquals(EventMessageProvider.Category.FINANCE, EventMessageProvider.getCategoryForIcon("AttachMoney"))
        assertEquals(EventMessageProvider.Category.WORK, EventMessageProvider.getCategoryForIcon("Work"))
        assertEquals(EventMessageProvider.Category.GENERAL, EventMessageProvider.getCategoryForIcon("UnknownIcon"))
    }

    @Test
    fun `getTemporalBucket maps correctly`() {
        assertEquals(EventMessageProvider.TemporalBucket.COMPLETED, EventMessageProvider.getTemporalBucket("0", "dias", true, false))
        assertEquals(EventMessageProvider.TemporalBucket.TODAY, EventMessageProvider.getTemporalBucket("0", "dias", false, true))
        assertEquals(EventMessageProvider.TemporalBucket.VERY_SOON, EventMessageProvider.getTemporalBucket("5", "dias", false, true))
        assertEquals(EventMessageProvider.TemporalBucket.SOON, EventMessageProvider.getTemporalBucket("15", "dias", false, false))
        assertEquals(EventMessageProvider.TemporalBucket.DISTANT, EventMessageProvider.getTemporalBucket("40", "dias", false, false))
        assertEquals(EventMessageProvider.TemporalBucket.VERY_DISTANT, EventMessageProvider.getTemporalBucket("200", "dias", false, false))
        
        // Horas is always TODAY
        assertEquals(EventMessageProvider.TemporalBucket.TODAY, EventMessageProvider.getTemporalBucket("5", "horas", false, true))
    }

    @Test
    fun `getHighlightMessage is deterministic for same ID`() {
        val msg1 = EventMessageProvider.getHighlightMessage("id123", "Airplane", "5", "dias", false, true, "Viagem")
        val msg2 = EventMessageProvider.getHighlightMessage("id123", "Airplane", "5", "dias", false, true, "Viagem")
        assertEquals(msg1, msg2)
    }

    @Test
    fun `getHighlightMessage sanitizes placeholders`() {
        val msg = EventMessageProvider.getHighlightMessage("id123", "Airplane", "5", "dias", false, true, "{prefillTitle}")
        assertTrue(msg.contains("seu evento"))
    }
}
