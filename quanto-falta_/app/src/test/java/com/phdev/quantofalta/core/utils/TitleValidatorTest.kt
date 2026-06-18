package com.phdev.quantofalta.core.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class TitleValidatorTest {

    @Test
    fun `sanitizeTitle returns fallback for null`() {
        assertEquals("seu evento", TitleValidator.sanitizeTitle(null))
    }

    @Test
    fun `sanitizeTitle returns fallback for empty or blank strings`() {
        assertEquals("seu evento", TitleValidator.sanitizeTitle(""))
        assertEquals("seu evento", TitleValidator.sanitizeTitle("   "))
    }

    @Test
    fun `sanitizeTitle returns fallback for placeholders`() {
        assertEquals("seu evento", TitleValidator.sanitizeTitle("{prefillTitle}"))
        assertEquals("seu evento", TitleValidator.sanitizeTitle("para a {prefillTitle}"))
        assertEquals("seu evento", TitleValidator.sanitizeTitle("\${title}"))
    }

    @Test
    fun `sanitizeTitle returns trimmed valid title`() {
        assertEquals("Viagem", TitleValidator.sanitizeTitle("Viagem"))
        assertEquals("Festa de Aniversário", TitleValidator.sanitizeTitle("  Festa de Aniversário  "))
    }
}
