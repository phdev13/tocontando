package com.phdev.quantofalta.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue

import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phdev.quantofalta.core.designsystem.theme.AppTypography
import com.phdev.quantofalta.core.time.LocalScreenTicker

/**
 * Lógica base de contagem lifecycle-aware.
 * 
 * Regras:
 * Se compactCard e < 1h -> Não atualiza, retorna "menos de 1h" ou "< 1h".
 * Se > 24h -> Lê tickHour
 * Se > 5min -> Lê tickMinute
 * Se < 5min -> Lê tickSecond
 */
@Composable
fun getSmartCurrentTime(targetMillis: Long, isCompact: Boolean): Long {
    val ticker = LocalScreenTicker.current
    if (!isCompact) {
        return ticker.tickSecond.value
    }

    val now = System.currentTimeMillis() // Read once per composition frame
    val diff = (targetMillis - now).coerceAtLeast(0)

    val hourLimit = 60 * 60 * 1000L

    return when {
        diff > 24 * hourLimit -> ticker.tickHour.value
        else -> ticker.tickMinute.value
    }
}

@Composable
fun SmartClassicCountdown(
    targetMillis: Long,
    textColor: Color,
    modifier: Modifier = Modifier,
    isCompact: Boolean = false
) {
    val currentTime = getSmartCurrentTime(targetMillis, isCompact)
    val diff = (targetMillis - currentTime).coerceAtLeast(0)

    if (isCompact && diff <= 60 * 60 * 1000L) {
        Text(
            text = "menos de 1h",
            style = AppTypography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = textColor,
            modifier = modifier
        )
        return
    }

    val days = diff / (1000 * 60 * 60 * 24)
    val hours = (diff / (1000 * 60 * 60)) % 24
    val minutes = (diff / (1000 * 60)) % 60
    val seconds = (diff / 1000) % 60

    if (isCompact) {
        val compactText = when {
            days >= 365 -> {
                val years = days / 365
                if (years == 1L) "1 ano" else "$years anos"
            }
            days >= 30 -> {
                val months = days / 30
                if (months == 1L) "1 mês" else "$months meses"
            }
            days >= 7 -> {
                val weeks = days / 7
                if (weeks == 1L) "1 semana" else "$weeks semanas"
            }
            days > 0 -> if (days == 1L) "1 dia" else "$days dias"
            hours > 0 -> if (hours == 1L) "1 hora" else "$hours horas"
            else -> if (minutes == 1L) "1 min" else "$minutes min"
        }
        
        Text(
            text = compactText,
            style = AppTypography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = textColor,
            modifier = modifier
        )
    } else {
        Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (days > 0) {
                CountdownBlock(value = days.toString(), label = if (days == 1L) "dia" else "dias", textColor = textColor, weight = 1.2f)
            }
            CountdownBlock(value = hours.toString().padStart(2, '0'), label = "horas", textColor = textColor, weight = 1f)
            CountdownBlock(value = minutes.toString().padStart(2, '0'), label = "min", textColor = textColor, weight = 1f)
            CountdownBlock(value = seconds.toString().padStart(2, '0'), label = "seg", textColor = textColor, weight = 1f)
        }
    }
}

@Composable
fun RowScope.CountdownBlock(value: String, label: String, textColor: Color = Color.White, weight: Float) {
    Column(
        modifier = Modifier
            .weight(weight)
            .background(textColor.copy(alpha = 0.12f), RoundedCornerShape(14.dp))
            .padding(vertical = 12.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = value,
            style = AppTypography.displayLarge.copy(
                fontSize = if (value.length >= 3) 28.sp else 34.sp,
                lineHeight = if (value.length >= 3) 28.sp else 34.sp,
                fontWeight = FontWeight.Black,
                fontFeatureSettings = "tnum"
            ),
            color = textColor
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label.uppercase(),
            style = AppTypography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                fontSize = 10.sp
            ),
            color = textColor.copy(alpha = 0.85f)
        )
    }
}

@Composable
fun SmartBlocksCountdown(
    targetMillis: Long,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    val currentTime = getSmartCurrentTime(targetMillis, isCompact = false)
    val diff = (targetMillis - currentTime).coerceAtLeast(0)

    val days = diff / (1000 * 60 * 60 * 24)
    val hours = (diff / (1000 * 60 * 60)) % 24
    val minutes = (diff / (1000 * 60)) % 60
    val seconds = (diff / 1000) % 60

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        BlockItem(value = days.toString(), label = "DIAS", color = textColor, modifier = Modifier.weight(1f))
        BlockItem(value = hours.toString().padStart(2, '0'), label = "HORAS", color = textColor, modifier = Modifier.weight(1f))
        BlockItem(value = minutes.toString().padStart(2, '0'), label = "MIN", color = textColor, modifier = Modifier.weight(1f))
        BlockItem(value = seconds.toString().padStart(2, '0'), label = "SEG", color = textColor, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun BlockItem(value: String, label: String, color: Color, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .background(color.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
            .padding(vertical = 12.dp)
    ) {
        Text(text = value, style = AppTypography.headlineMedium.copy(fontWeight = FontWeight.Black), color = color)
        Text(text = label, style = AppTypography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp), color = color.copy(alpha = 0.8f))
    }
}
