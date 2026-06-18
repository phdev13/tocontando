package com.phdev.quantofalta.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import com.phdev.quantofalta.BuildConfig

sealed class BillingStatus {
    object Idle : BillingStatus()
    object LoadingProduct : BillingStatus()
    object ProductUnavailable : BillingStatus()
    object PurchaseInProgress : BillingStatus()
    object PurchasePending : BillingStatus()
    object PurchaseCompleted : BillingStatus()
    object PurchaseCancelled : BillingStatus()
    object Restoring : BillingStatus()
    object RestoreCompleted : BillingStatus()
    object RestoreEmpty : BillingStatus()
    object AlreadyPremium : BillingStatus()
    data class ConnectionFailed(val message: String) : BillingStatus()
    data class PurchaseFailed(val message: String) : BillingStatus()
}

class BillingClientWrapper(
    private val context: Context,
    private val entitlementManager: EntitlementManager,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) : PurchasesUpdatedListener {

    companion object {
        private const val LIFETIME_PRODUCT_ID = "premium_lifetime"
        private val LEGACY_SUBSCRIPTION_PRODUCT_IDS = setOf("premium_monthly", "premium_annual")
    }

    private val billingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases()
        .build()

    private val _isReady = MutableStateFlow(false)
    val isReady = _isReady.asStateFlow()

    private val _products = MutableStateFlow<List<ProductDetails>>(emptyList())
    val products = _products.asStateFlow()

    private val _purchases = MutableStateFlow<List<Purchase>>(emptyList())
    val purchases = _purchases.asStateFlow()

    private val _billingStatus = MutableStateFlow<BillingStatus>(BillingStatus.Idle)
    val billingStatus = _billingStatus.asStateFlow()

    private var isConnecting = false
    private val pendingReadyActions = mutableListOf<() -> Unit>()

    init {
        startConnection()
    }

    private fun startConnection(onReady: (() -> Unit)? = null) {
        if (_isReady.value) {
            onReady?.invoke()
            return
        }
        onReady?.let { pendingReadyActions.add(it) }
        if (isConnecting) return

        isConnecting = true
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                isConnecting = false
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    _isReady.value = true
                    queryProducts()
                    queryPurchases()
                    val actions = pendingReadyActions.toList()
                    pendingReadyActions.clear()
                    actions.forEach { it.invoke() }
                } else {
                    pendingReadyActions.clear()
                    _billingStatus.value = BillingStatus.ConnectionFailed(
                        billingResult.debugMessage.ifBlank { "Falha ao conectar com a Google Play." }
                    )
                }
            }

            override fun onBillingServiceDisconnected() {
                _isReady.value = false
                isConnecting = false
            }
        })
    }

    private fun runWhenReady(action: () -> Unit) {
        if (_isReady.value) action() else startConnection(action)
    }

    private fun queryProducts() {
        runWhenReady {
            coroutineScope.launch {
                _billingStatus.value = BillingStatus.LoadingProduct
                val inappParams = QueryProductDetailsParams.newBuilder()
                    .setProductList(
                        listOf(
                            QueryProductDetailsParams.Product.newBuilder()
                                .setProductId(LIFETIME_PRODUCT_ID)
                                .setProductType(BillingClient.ProductType.INAPP)
                                .build()
                        )
                    )
                    .build()

                val inappResult = billingClient.queryProductDetails(inappParams)
                if (inappResult.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    _products.value = inappResult.productDetailsList ?: emptyList()
                    _billingStatus.value = if (_products.value.isEmpty()) {
                        BillingStatus.ProductUnavailable
                    } else {
                        BillingStatus.Idle
                    }
                } else {
                    _products.value = emptyList()
                    _billingStatus.value = BillingStatus.ConnectionFailed(
                        inappResult.billingResult.debugMessage.ifBlank { "Produto indisponível no momento." }
                    )
                }
            }
        }
    }

    fun queryPurchases(isUserRestore: Boolean = false) {
        runWhenReady {
            coroutineScope.launch {
                if (isUserRestore) _billingStatus.value = BillingStatus.Restoring

                val subsResult = billingClient.queryPurchasesAsync(
                    QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build()
                )
                val inappResult = billingClient.queryPurchasesAsync(
                    QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build()
                )

                if (
                    subsResult.billingResult.responseCode != BillingClient.BillingResponseCode.OK &&
                    inappResult.billingResult.responseCode != BillingClient.BillingResponseCode.OK
                ) {
                    _billingStatus.value = BillingStatus.ConnectionFailed("Não foi possível consultar suas compras.")
                    return@launch
                }

                val allPurchases = mutableListOf<Purchase>()
                if (subsResult.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    allPurchases.addAll(subsResult.purchasesList)
                }
                if (inappResult.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    allPurchases.addAll(inappResult.purchasesList)
                }

                _purchases.value = allPurchases

                val activePurchases = allPurchases.filter { purchase ->
                    purchase.purchaseState == Purchase.PurchaseState.PURCHASED &&
                        purchase.products.any { it == LIFETIME_PRODUCT_ID || it in LEGACY_SUBSCRIPTION_PRODUCT_IDS }
                }

                if (activePurchases.isEmpty()) {
                    if (isUserRestore) _billingStatus.value = BillingStatus.RestoreEmpty
                    return@launch
                }

                var restored = false
                for (purchase in activePurchases) {
                    val result = verifyPurchaseOnServer(purchase)
                    if (result.success) {
                        entitlementManager.addEntitlement(result.toEntitlement(purchase))
                        if (!purchase.isAcknowledged) acknowledgePurchase(purchase)
                        restored = true
                    }
                }

                _billingStatus.value = when {
                    restored && isUserRestore -> BillingStatus.RestoreCompleted
                    restored -> BillingStatus.AlreadyPremium
                    isUserRestore -> BillingStatus.PurchaseFailed("Nenhuma compra válida foi confirmada pelo servidor.")
                    else -> BillingStatus.Idle
                }
            }
        }
    }

    fun restorePurchases() = queryPurchases(isUserRestore = true)

    private suspend fun verifyPurchaseOnServer(purchase: Purchase): ServerVerificationResult = withContext(Dispatchers.IO) {
        try {
            val installationId = context.getSharedPreferences("privacy_prefs", Context.MODE_PRIVATE)
                .getString("installation_id", "unknown_${System.currentTimeMillis()}") ?: "unknown"
                
            val urlStr = try {
                BuildConfig::class.java.getField("API_BASE_URL").get(null) as String
            } catch (_: Exception) {
                BuildConfig.API_BASE_URL
            } + "/api/v1/app/premium/verify-purchase"
            
            val url = URL(urlStr)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json; utf-8")
            connection.setRequestProperty("Accept", "application/json")
            connection.doOutput = true

            val jsonBody = JSONObject().apply {
                put("purchaseToken", purchase.purchaseToken)
                put("productId", purchase.products.firstOrNull() ?: "")
                put("installationId", installationId)
            }

            OutputStreamWriter(connection.outputStream).use { writer ->
                writer.write(jsonBody.toString())
                writer.flush()
            }

            val responseCode = connection.responseCode
            val responseBody = runCatching {
                val stream = if (responseCode in 200..299) connection.inputStream else connection.errorStream
                stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            }.getOrDefault("")

            if (responseCode !in 200..299 && responseCode != 409) {
                return@withContext ServerVerificationResult(false)
            }

            val body = runCatching { JSONObject(responseBody) }.getOrNull()
            val data = body?.optJSONObject("data") ?: body
            val entitlement = data?.optJSONObject("entitlement")
            return@withContext ServerVerificationResult(
                success = true,
                entitlementId = entitlement?.optString("id")?.takeIf { it.isNotBlank() },
                planType = entitlement?.optString("planType")?.takeIf { it.isNotBlank() },
                features = entitlement?.optString("features")?.takeIf { it.isNotBlank() && it != "null" },
                expiresAt = entitlement?.takeUnless { it.isNull("expiresAt") }?.optLong("expiresAt")
            )
        } catch (e: Exception) {
            Log.e("Billing", "Verification failed", e)
            return@withContext ServerVerificationResult(false)
        }
    }

    fun launchBillingFlow(activity: Activity, productDetails: ProductDetails, offerToken: String? = null) {
        _billingStatus.value = BillingStatus.PurchaseInProgress
        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder().apply {
                setProductDetails(productDetails)
                offerToken?.let { setOfferToken(it) }
            }.build()
        )

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()

        billingClient.launchBillingFlow(activity, billingFlowParams)
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: MutableList<Purchase>?) {
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                if (purchases.isNullOrEmpty()) {
                    _billingStatus.value = BillingStatus.PurchaseFailed("Compra não retornada pela Google Play.")
                    return
                }
                purchases.forEach { purchase ->
                    when (purchase.purchaseState) {
                        Purchase.PurchaseState.PURCHASED -> {
                            coroutineScope.launch {
                                val result = verifyPurchaseOnServer(purchase)
                                if (result.success) {
                                    entitlementManager.addEntitlement(result.toEntitlement(purchase))
                                    if (!purchase.isAcknowledged) acknowledgePurchase(purchase)
                                    _billingStatus.value = BillingStatus.PurchaseCompleted
                                    queryPurchases()
                                } else {
                                    _billingStatus.value = BillingStatus.PurchaseFailed("Compra recebida, mas a validação no servidor falhou.")
                                }
                            }
                        }
                        Purchase.PurchaseState.PENDING -> {
                            _billingStatus.value = BillingStatus.PurchasePending
                        }
                        else -> Unit
                    }
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                _billingStatus.value = BillingStatus.PurchaseCancelled
            }
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                restorePurchases()
            }
            else -> {
                _billingStatus.value = BillingStatus.PurchaseFailed(
                    billingResult.debugMessage.ifBlank { "Não foi possível concluir a compra." }
                )
            }
        }
    }

    private fun acknowledgePurchase(purchase: Purchase) {
        coroutineScope.launch {
            val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()
            billingClient.acknowledgePurchase(acknowledgePurchaseParams)
        }
    }

    private data class ServerVerificationResult(
        val success: Boolean,
        val entitlementId: String? = null,
        val planType: String? = null,
        val features: String? = null,
        val expiresAt: Long? = null
    ) {
        fun toEntitlement(purchase: Purchase): Entitlement {
            val productId = purchase.products.firstOrNull()
            val fallbackPlan = when (productId) {
                LIFETIME_PRODUCT_ID -> "VITALICIO"
                "premium_annual" -> "ANUAL"
                "premium_monthly" -> "MENSAL"
                else -> "VITALICIO"
            }
            return Entitlement(
                id = entitlementId ?: "play_store_${purchase.orderId ?: purchase.purchaseToken.hashCode()}",
                planType = planType ?: fallbackPlan,
                features = features,
                expiresAt = expiresAt
            )
        }
    }
}
