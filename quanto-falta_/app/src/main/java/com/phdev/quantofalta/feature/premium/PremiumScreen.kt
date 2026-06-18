package com.phdev.quantofalta.feature.premium

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.WindowManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.billingclient.api.ProductDetails
import com.phdev.quantofalta.billing.BillingClientWrapper
import com.phdev.quantofalta.billing.BillingStatus

fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumScreen(
    billingClientWrapper: BillingClientWrapper,
    onDismiss: () -> Unit,
    onNavigateToRedeem: () -> Unit
) {
    val context = LocalContext.current
    val activity = context.findActivity()

    DisposableEffect(Unit) {
        activity?.window?.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    val products by billingClientWrapper.products.collectAsState()
    val billingStatus by billingClientWrapper.billingStatus.collectAsState()
    val lifetimeProduct = products.firstOrNull { it.productId == "premium_lifetime" }
    val price = lifetimeProduct?.oneTimePurchaseOfferDetails?.formattedPrice

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("") },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Fechar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item {
                PremiumHeader(price = price, billingStatus = billingStatus)
            }

            item {
                BillingStatusMessage(status = billingStatus)
            }

            item {
                PlanComparison()
            }

            item {
                BenefitsCard()
            }

            item {
                PurchaseActions(
                    product = lifetimeProduct,
                    status = billingStatus,
                    onBuy = {
                        val hostActivity = context.findActivity()
                        if (hostActivity != null && lifetimeProduct != null) {
                            billingClientWrapper.launchBillingFlow(hostActivity, lifetimeProduct)
                        }
                    },
                    onRestore = { billingClientWrapper.restorePurchases() },
                    onRedeem = onNavigateToRedeem
                )
            }

            item {
                Text(
                    text = "A compra é processada pela Google Play. Você pode restaurar o acesso usando a mesma conta Google.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
            }
        }
    }
}

@Composable
private fun PremiumHeader(price: String?, billingStatus: BillingStatus) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = Icons.Default.Star,
            contentDescription = null,
            tint = Color(0xFFFFC107),
            modifier = Modifier.size(56.dp)
        )
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = "Tô Contando Premium",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            lineHeight = 32.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Tudo liberado. Sem limites. Pagamento único.",
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 21.sp
        )
        Spacer(modifier = Modifier.height(14.dp))
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            shape = RoundedCornerShape(999.dp)
        ) {
            Text(
                text = "Pagamento único • Sem renovação automática",
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = when {
                price != null -> price
                billingStatus is BillingStatus.LoadingProduct -> "Carregando preço..."
                else -> "Preço indisponível"
            },
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun BillingStatusMessage(status: BillingStatus) {
    val message = when (status) {
        BillingStatus.LoadingProduct -> "Consultando o produto na Google Play..."
        BillingStatus.ProductUnavailable -> "O produto vitalício não está disponível no momento."
        BillingStatus.PurchaseInProgress -> "Abrindo a compra na Google Play..."
        BillingStatus.PurchasePending -> "Compra pendente. O Premium será liberado após a confirmação do pagamento."
        BillingStatus.PurchaseCompleted -> "Compra concluída. Premium Vitalício ativado."
        BillingStatus.PurchaseCancelled -> "Compra cancelada. Nenhuma cobrança foi feita."
        BillingStatus.Restoring -> "Restaurando compras pela Google Play..."
        BillingStatus.RestoreCompleted -> "Compra restaurada com sucesso."
        BillingStatus.RestoreEmpty -> "Nenhuma compra Premium foi encontrada nesta conta Google."
        BillingStatus.AlreadyPremium -> "Você já possui acesso Premium ativo."
        is BillingStatus.ConnectionFailed -> status.message
        is BillingStatus.PurchaseFailed -> status.message
        BillingStatus.Idle -> null
    }

    if (message != null) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(14.dp)
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (status is BillingStatus.LoadingProduct || status is BillingStatus.Restoring || status is BillingStatus.PurchaseInProgress) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(text = message, fontSize = 13.sp, lineHeight = 18.sp)
            }
        }
    }
}

@Composable
private fun PlanComparison() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        Text("Grátis vs Premium", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        ComparisonCard(
            title = "Grátis",
            subtitle = "Para começar sem nenhum custo",
            items = listOf(
                "Até 5 eventos ativos",
                "Cores e ícones básicos",
                "Widget básico",
                "Lembretes automáticos",
                "Backup local (JSON)",
                "Compartilhamento por texto"
            ),
            highlighted = false
        )
        ComparisonCard(
            title = "Premium — Tudo liberado",
            subtitle = "Acesso permanente em pagamento único",
            items = listOf(
                "Eventos ilimitados",
                "Todos os ícones e cores",
                "Personalização completa",
                "Proteção por biometria",
                "Histórico e estatísticas completas",
                "Todos os widgets e estilos",
                "Compartilhamento avançado",
                "Todos os recursos futuros"
            ),
            highlighted = true
        )
    }
}

@Composable
private fun ComparisonCard(
    title: String,
    subtitle: String,
    items: List<String>,
    highlighted: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (highlighted) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        border = if (highlighted) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            items.forEach { item ->
                FeatureRow(text = item)
            }
        }
    }
}

@Composable
private fun BenefitsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("O que o Premium libera", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            FeatureRow("Eventos ilimitados — acompanhe quantos momentos quiser")
            FeatureRow("Todos os ícones e todas as cores disponíveis")
            FeatureRow("Proteção individual de eventos com biometria")
            FeatureRow("Histórico completo e linha do tempo de cada evento")
            FeatureRow("Widgets adicionais com estilos e personalização")
            FeatureRow("Compartilhamento avançado: stories, wallpapers e imagens")
            FeatureRow("Restauração da compra pela Google Play em qualquer device")
            FeatureRow("Todos os recursos atuais e futuros incluídos")
        }
    }
}

@Composable
private fun FeatureRow(text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .padding(top = 1.dp)
                .size(18.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(text = text, fontSize = 14.sp, lineHeight = 20.sp)
    }
}

@Composable
private fun PurchaseActions(
    product: ProductDetails?,
    status: BillingStatus,
    onBuy: () -> Unit,
    onRestore: () -> Unit,
    onRedeem: () -> Unit
) {
    val busy = status is BillingStatus.LoadingProduct ||
        status is BillingStatus.PurchaseInProgress ||
        status is BillingStatus.Restoring

    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Button(
            onClick = onBuy,
            enabled = product != null && !busy,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text("Comprar Premium Vitalício", fontWeight = FontWeight.SemiBold)
        }
        OutlinedButton(
            onClick = onRestore,
            enabled = !busy,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text("Restaurar compras")
        }
        TextButton(
            onClick = onRedeem,
            enabled = !busy,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Possui um código? Resgatar")
        }
    }
}
