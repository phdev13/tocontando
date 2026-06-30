package com.phdev.quantofalta.core.designsystem.components.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.phdev.quantofalta.core.designsystem.components.SmartClassicCountdown
import com.phdev.quantofalta.core.designsystem.components.SmartBlocksCountdown
import com.phdev.quantofalta.core.designsystem.components.bounceClick
import com.phdev.quantofalta.core.designsystem.components.getIconByName
import com.phdev.quantofalta.core.designsystem.theme.AppShapes
import com.phdev.quantofalta.core.designsystem.theme.AppSpacing
import com.phdev.quantofalta.core.designsystem.theme.AppTypography
import com.phdev.quantofalta.core.designsystem.theme.ContrastUtils
import com.phdev.quantofalta.domain.model.EventUiModel

@Composable
fun NextSalaryCard(event: EventUiModel, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val textColor = ContrastUtils.getContrastColor(event.color)
    val progress = event.salaryUiState?.cycleProgressPercentage?.toFloat()?.div(100f)?.coerceIn(0f, 1f) ?: event.progress
    Card(
        modifier = modifier.fillMaxWidth().height(228.dp).bounceClick(useHaptic = true, onClick = onClick),
        shape = AppShapes.large,
        colors = CardDefaults.cardColors(containerColor = event.color)
    ) {
        SalaryHeroSurface(event, textColor) {
            Column(modifier = Modifier.fillMaxSize().padding(AppSpacing.large)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = event.title,
                            style = AppTypography.titleLarge.copy(fontWeight = FontWeight.Black),
                            color = textColor,
                            maxLines = 1
                        )
                        Text(
                            text = "Próximo pagamento",
                            style = AppTypography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = textColor.copy(alpha = 0.78f)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(AppShapes.medium)
                            .background(textColor.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = getIconByName(event.iconName), contentDescription = null, tint = textColor, modifier = Modifier.size(26.dp))
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = event.primaryText,
                    style = AppTypography.displayMedium.copy(fontWeight = FontWeight.Black, fontSize = 38.sp, lineHeight = 42.sp),
                    color = textColor,
                    maxLines = 2
                )
                Spacer(modifier = Modifier.height(AppSpacing.small))
                Text(
                    text = event.secondaryText,
                    style = AppTypography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = textColor.copy(alpha = 0.84f)
                )
                Spacer(modifier = Modifier.height(AppSpacing.medium))
                Box(modifier = Modifier.fillMaxWidth().height(6.dp).clip(AppShapes.small).background(textColor.copy(alpha = 0.18f))) {
                    Box(modifier = Modifier.fillMaxWidth(progress).height(6.dp).background(textColor))
                }
            }
        }
    }
}

@Composable
private fun SalaryHeroSurface(
    event: EventUiModel,
    textColor: Color,
    content: @Composable BoxScope.() -> Unit
) {
    val hasCover = !event.coverImageUri.isNullOrBlank()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        event.color,
                        event.color.copy(
                            red = (event.color.red * 0.70f).coerceIn(0f, 1f),
                            green = (event.color.green * 0.86f).coerceIn(0f, 1f),
                            blue = (event.color.blue * 1.08f).coerceIn(0f, 1f)
                        )
                    )
                )
            )
    ) {
        if (hasCover) {
            AsyncImage(
                model = event.coverImageUri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.18f),
                                event.color.copy(alpha = 0.46f),
                                Color.Black.copy(alpha = 0.52f)
                            )
                        )
                    )
            )
        }
        Icon(
            imageVector = getIconByName(event.iconName),
            contentDescription = null,
            tint = textColor.copy(alpha = if (hasCover) 0.16f else 0.10f),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .offset(x = 34.dp, y = 12.dp)
                .size(180.dp)
        )
        content()
    }
}

@Composable
private fun SalaryMetric(
    label: String,
    value: String,
    textColor: Color,
    modifier: Modifier = Modifier,
    alignEnd: Boolean = false
) {
    Column(
        modifier = modifier,
        horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start
    ) {
        Text(text = label, style = AppTypography.labelSmall, color = textColor.copy(alpha = 0.72f), maxLines = 1)
        Text(
            text = value,
            style = AppTypography.titleMedium.copy(fontWeight = FontWeight.Black),
            color = textColor,
            textAlign = if (alignEnd) TextAlign.End else TextAlign.Start,
            maxLines = 1
        )
    }
}

@Composable
fun MonthProgressCard(event: EventUiModel, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val hasCover = !event.coverImageUri.isNullOrBlank()
    val baseTextColor = ContrastUtils.getContrastColor(event.color)
    val textColor = if (hasCover) Color.White else baseTextColor
    val progress = event.salaryUiState?.cycleProgressPercentage?.toFloat()?.div(100f) ?: 0f
    
    Card(
        modifier = modifier.fillMaxWidth().height(228.dp).bounceClick(onClick = onClick),
        shape = AppShapes.large,
        colors = CardDefaults.cardColors(containerColor = event.color)
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(AppSpacing.large)) {
            Text(text = event.title, style = AppTypography.titleLarge, color = textColor)
            Spacer(modifier = Modifier.weight(1f))
            Text(text = "Progresso do MÃªs", style = AppTypography.bodySmall, color = textColor.copy(alpha = 0.8f))
            Spacer(modifier = Modifier.height(4.dp))
            Box(modifier = Modifier.fillMaxWidth().height(16.dp).background(textColor.copy(0.2f), AppShapes.small)) {
                Box(modifier = Modifier.fillMaxWidth(progress).height(16.dp).background(textColor, AppShapes.small))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = "${(progress * 100).toInt()}% concluÃ­do", color = textColor)
                Text(text = event.primaryText, color = textColor, fontWeight = FontWeight.Bold)
            }
        }
    }}

@Composable
fun BusinessDaysCard(event: EventUiModel, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val hasCover = !event.coverImageUri.isNullOrBlank()
    val baseTextColor = ContrastUtils.getContrastColor(event.color)
    val textColor = if (hasCover) Color.White else baseTextColor
    Card(
        modifier = modifier.fillMaxWidth().height(228.dp).bounceClick(onClick = onClick),
        shape = AppShapes.large,
        colors = CardDefaults.cardColors(containerColor = event.color)
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(AppSpacing.large), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(text = event.title, style = AppTypography.titleMedium, color = textColor)
            Spacer(modifier = Modifier.height(AppSpacing.large))
            Text(text = "${event.salaryUiState?.businessDaysRemaining ?: 0}", style = AppTypography.displayLarge.copy(fontWeight = FontWeight.Black), color = textColor)
            Text(text = "Dias Ãšteis Restantes", style = AppTypography.bodyLarge, color = textColor)
        }
    }}

@Composable
fun ReceivingCycleCard(event: EventUiModel, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val hasCover = !event.coverImageUri.isNullOrBlank()
    val baseTextColor = ContrastUtils.getContrastColor(event.color)
    val textColor = if (hasCover) Color.White else baseTextColor
    Card(
        modifier = modifier.fillMaxWidth().height(228.dp).bounceClick(onClick = onClick),
        shape = AppShapes.large,
        colors = CardDefaults.cardColors(containerColor = event.color)
    ) {
        Box(modifier = Modifier.fillMaxWidth().padding(AppSpacing.large), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = event.title, color = textColor, style = AppTypography.titleMedium)
                Spacer(modifier = Modifier.height(AppSpacing.small))
                Text(text = "Ciclo de Recebimento", color = textColor.copy(alpha = 0.7f))
                Text(text = event.primaryText, color = textColor, style = AppTypography.displayMedium)
            }
        }
    }}

@Composable
fun SurvivalModeCard(event: EventUiModel, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val hasCover = !event.coverImageUri.isNullOrBlank()
    val baseTextColor = ContrastUtils.getContrastColor(event.color)
    val textColor = if (hasCover) Color.White else baseTextColor
    val phrase = event.salaryUiState?.salaryCustomPhrase.takeIf { !it.isNullOrBlank() } ?: "Aguenta firme!"
    Card(
        modifier = modifier.fillMaxWidth().height(228.dp).bounceClick(onClick = onClick),
        shape = AppShapes.large,
        colors = CardDefaults.cardColors(containerColor = event.color)
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(AppSpacing.large), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = phrase, style = AppTypography.headlineSmall, color = textColor, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(AppSpacing.medium))
            Text(text = event.primaryText, style = AppTypography.headlineLarge, color = textColor)
        }
    }}

@Composable
fun SalaryGoalCard(event: EventUiModel, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val hasCover = !event.coverImageUri.isNullOrBlank()
    val baseTextColor = ContrastUtils.getContrastColor(event.color)
    val textColor = if (hasCover) Color.White else baseTextColor
    val goal = event.salaryUiState?.salaryGoalTarget ?: 0.0
    val value = event.salaryUiState?.salaryValue ?: 0.0
    val remainingSalaries = if (value > 0) kotlin.math.ceil(goal / value).toInt() else 0

    Card(
        modifier = modifier.fillMaxWidth().height(228.dp).bounceClick(onClick = onClick),
        shape = AppShapes.large,
        colors = CardDefaults.cardColors(containerColor = event.color)
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(AppSpacing.large)) {
            Text(text = event.title, style = AppTypography.titleLarge, color = textColor)
            Spacer(modifier = Modifier.weight(1f))
            Text(text = "Para atingir a meta:", style = AppTypography.bodyMedium, color = textColor)
            Text(text = "Faltam $remainingSalaries recebimentos", style = AppTypography.headlineMedium.copy(fontWeight = FontWeight.Bold), color = textColor)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = event.primaryText, style = AppTypography.bodySmall, color = textColor.copy(alpha = 0.8f))
        }
    }}

@Composable
fun SalaryBlocksCard(event: EventUiModel, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val hasCover = !event.coverImageUri.isNullOrBlank()
    val baseTextColor = ContrastUtils.getContrastColor(event.color)
    val textColor = if (hasCover) Color.White else baseTextColor
    Card(
        modifier = modifier.fillMaxWidth().height(228.dp).bounceClick(onClick = onClick),
        shape = AppShapes.large,
        colors = CardDefaults.cardColors(containerColor = event.color)
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(AppSpacing.large)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(textColor.copy(alpha = 0.15f), AppShapes.medium),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = getIconByName(event.iconName), contentDescription = null, tint = textColor)
                }
                Spacer(modifier = Modifier.width(AppSpacing.medium))
                Text(
                    text = event.title, 
                    style = AppTypography.titleLarge.copy(fontWeight = FontWeight.Bold), 
                    color = textColor,
                    maxLines = 2
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            SmartBlocksCountdown(targetMillis = event.dateMillis, textColor = textColor)
        }
    }}

@Composable
fun ValueAndTimeCard(event: EventUiModel, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val hasCover = !event.coverImageUri.isNullOrBlank()
    val baseTextColor = ContrastUtils.getContrastColor(event.color)
    val textColor = if (hasCover) Color.White else baseTextColor
    val value = event.salaryUiState?.salaryValue ?: 0.0
    val valueStr = if (value > 0) String.format("R$ %.2f", value) else "Valor nÃ£o definido"
    Card(
        modifier = modifier.fillMaxWidth().height(228.dp).bounceClick(onClick = onClick),
        shape = AppShapes.large,
        colors = CardDefaults.cardColors(containerColor = event.color)
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(AppSpacing.large), verticalArrangement = Arrangement.SpaceBetween) {
            Text(text = event.title, style = AppTypography.titleMedium, color = textColor)
            Column {
                Text(text = valueStr, style = AppTypography.headlineMedium.copy(fontWeight = FontWeight.Bold), color = textColor)
                Text(text = event.primaryText, style = AppTypography.bodyLarge, color = textColor.copy(alpha = 0.8f))
            }
        }
    }}

@Composable
fun MinimalistFinanceCard(event: EventUiModel, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val hasCover = !event.coverImageUri.isNullOrBlank()
    val baseTextColor = ContrastUtils.getContrastColor(event.color)
    val textColor = if (hasCover) Color.White else baseTextColor
    Card(
        modifier = modifier.fillMaxWidth().height(228.dp).bounceClick(onClick = onClick),
        shape = AppShapes.large,
        colors = CardDefaults.cardColors(containerColor = event.color)
    ) {
        Box(modifier = Modifier.fillMaxWidth().padding(AppSpacing.large), contentAlignment = Alignment.BottomEnd) {
            Column(horizontalAlignment = Alignment.End) {
                Text(text = event.title, color = textColor, style = AppTypography.bodySmall)
                Text(text = event.primaryText, color = textColor, style = AppTypography.headlineMedium)
            }
        }
    }}

@Composable
fun FinanceDashboardCard(event: EventUiModel, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val textColor = ContrastUtils.getContrastColor(event.color)
    val progress = event.salaryUiState?.cycleProgressPercentage?.toFloat()?.div(100f)?.coerceIn(0f, 1f) ?: event.progress
    val progressText = "${(progress * 100).toInt()}% do ciclo"
    Card(
        modifier = modifier.fillMaxWidth().height(228.dp).bounceClick(onClick = onClick),
        shape = AppShapes.large,
        colors = CardDefaults.cardColors(containerColor = event.color)
    ) {
        SalaryHeroSurface(event, textColor) {
            Column(modifier = Modifier.fillMaxSize().padding(AppSpacing.large)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = event.title,
                            style = AppTypography.titleLarge.copy(fontWeight = FontWeight.Black),
                            color = textColor,
                            maxLines = 1
                        )
                        Text(
                            text = progressText,
                            style = AppTypography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = textColor.copy(alpha = 0.76f),
                            maxLines = 1
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(textColor.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = getIconByName(event.iconName),
                            contentDescription = null,
                            tint = textColor,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = event.primaryText,
                    style = AppTypography.headlineLarge.copy(fontWeight = FontWeight.Black),
                    color = textColor,
                    maxLines = 1
                )
                Text(
                    text = event.secondaryText,
                    style = AppTypography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    color = textColor.copy(alpha = 0.78f),
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(AppSpacing.medium))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    SalaryMetric(
                        label = "Dias uteis",
                        value = "${event.salaryUiState?.businessDaysRemaining ?: 0}",
                        textColor = textColor,
                        modifier = Modifier.weight(1f)
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(text = "Tempo restante", style = AppTypography.labelSmall, color = textColor.copy(alpha = 0.72f), maxLines = 1)
                        SmartClassicCountdown(targetMillis = event.dateMillis, textColor = textColor, isCompact = true)
                    }
                }
                Spacer(modifier = Modifier.height(AppSpacing.medium))
                Box(modifier = Modifier.fillMaxWidth().height(6.dp).clip(AppShapes.small).background(textColor.copy(alpha = 0.18f))) {
                    Box(modifier = Modifier.fillMaxWidth(progress).height(6.dp).background(textColor))
                }
            }
        }
    }
}
