package com.phdev.quantofalta.billing

import androidx.compose.ui.graphics.toArgb
import com.phdev.quantofalta.core.designsystem.components.iconNamesList
import com.phdev.quantofalta.core.designsystem.theme.Colors

object PremiumPolicy {
    const val FREE_EVENT_LIMIT = 5
    const val FREE_ICON_COUNT = 40

    val freeIcons: Set<String> by lazy { iconNamesList.take(FREE_ICON_COUNT).toSet() }

    fun allowedIcon(requested: String, premium: Boolean): String =
        if (premium || requested in freeIcons) requested else freeIcons.first()

    val freeColors: Set<Int> by lazy { Colors.take(15).map { it.toArgb() }.toSet() }

    fun allowedColor(requestedArgb: Int, premium: Boolean): Int =
        if (premium || requestedArgb in freeColors) requestedArgb else freeColors.first()
}
