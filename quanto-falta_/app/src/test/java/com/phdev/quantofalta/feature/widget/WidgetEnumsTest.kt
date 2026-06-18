package com.phdev.quantofalta.feature.widget

import com.phdev.quantofalta.feature.widget.state.WidgetTheme
import com.phdev.quantofalta.feature.widget.state.WidgetUnitMode
import org.junit.Assert.assertEquals
import org.junit.Test

class WidgetEnumsTest {

    @Test
    fun `test WidgetTheme fromString maps correctly including legacy strings`() {
        assertEquals(WidgetTheme.MINIMALIST, WidgetTheme.fromString("Minimalist"))
        assertEquals(WidgetTheme.TRANSPARENT, WidgetTheme.fromString("transparent"))
        assertEquals(WidgetTheme.COMPACT, WidgetTheme.fromString("COMPACT"))
        assertEquals(WidgetTheme.FULLSCREEN, WidgetTheme.fromString("FullScreen"))
        assertEquals(WidgetTheme.FULLSCREEN, WidgetTheme.fromString("Image")) // Legacy
        assertEquals(WidgetTheme.COMPACT, WidgetTheme.fromString("UnknownString")) // Fallback
        assertEquals(WidgetTheme.COMPACT, WidgetTheme.fromString(null))
    }

    @Test
    fun `test WidgetUnitMode fromString maps correctly including legacy strings`() {
        assertEquals(WidgetUnitMode.AUTO, WidgetUnitMode.fromString("auto"))
        assertEquals(WidgetUnitMode.DAYS, WidgetUnitMode.fromString("dias"))
        assertEquals(WidgetUnitMode.MONTHS, WidgetUnitMode.fromString("meses"))
        assertEquals(WidgetUnitMode.YEARS, WidgetUnitMode.fromString("anos"))
        assertEquals(WidgetUnitMode.AUTO, WidgetUnitMode.fromString("UnknownString")) // Fallback
        assertEquals(WidgetUnitMode.AUTO, WidgetUnitMode.fromString(null))
    }
}
