package com.phdev.quantofalta.billing

import com.phdev.quantofalta.core.network.ApiClient
import org.json.JSONObject
import java.net.URLEncoder

data class PremiumStatusResponse(
    val success: Boolean,
    val isPremium: Boolean,
    val id: String?,
    val expiresAt: Long?,
    val features: String?,
    val planType: String?
)

data class ValidateCodeResponse(
    val success: Boolean,
    val codeValid: Boolean,
    val needsEmail: Boolean,
    val error: String?
)

data class LinkEmailResponse(
    val success: Boolean,
    val premiumActive: Boolean,
    val entitlementId: String?,
    val planType: String?,
    val expiresAt: Long?,
    val error: String?
)

data class RecoveryRequestResponse(
    val success: Boolean,
    val email: String?,
    val message: String?,
    val error: String?
)

data class RecoveryVerifyResponse(
    val success: Boolean,
    val premium: Boolean,
    val notFound: Boolean,
    val entitlementId: String?,
    val planType: String?,
    val features: String?,
    val expiresAt: Long?,
    val message: String?,
    val error: String?
)

object PremiumApi {
    private fun encode(value: String): String = URLEncoder.encode(value, "UTF-8")

    suspend fun getStatus(installationId: String, email: String?): PremiumStatusResponse {
        val url = if (!email.isNullOrBlank()) {
            "/api/v1/app/premium/status?installationId=${encode(installationId)}&userId=${encode(email)}"
        } else {
            "/api/v1/app/premium/status?installationId=${encode(installationId)}"
        }
        val response = ApiClient.get(url)
        if (response.isSuccess()) {
            val wrapper = runCatching { JSONObject(response.body) }.getOrNull()
            val success = wrapper?.optBoolean("success", false) == true
            if (success) {
                val data = wrapper?.optJSONObject("data") ?: return PremiumStatusResponse(true, false, null, null, null, null)
                return PremiumStatusResponse(
                    success = true,
                    isPremium = data.optBoolean("premium", false),
                    id = data.optString("entitlementId", "")
                        .ifBlank { data.optString("id", "") }
                        .takeIf { it.isNotBlank() },
                    expiresAt = if (!data.isNull("expiresAt")) data.getLong("expiresAt") else null,
                    features = data.opt("features")?.toString()?.takeIf { it.isNotBlank() && it != "null" },
                    planType = data.optString("planType", "").takeIf { it.isNotBlank() }
                )
            }
        }
        return PremiumStatusResponse(false, false, null, null, null, null)
    }

    suspend fun validateCode(code: String, installationId: String, appVersion: String): ValidateCodeResponse {
        val json = JSONObject().apply {
            put("code", code)
            put("installationId", installationId)
            put("appVersion", appVersion)
        }
        return try {
            val response = ApiClient.post("/api/v1/premium/activate-code/validate", json)
            val wrapper = JSONObject(response.body)
            val success = wrapper.optBoolean("success", false)
            val error = wrapper.optString("error", "").takeIf { it.isNotBlank() }
            val data = wrapper.optJSONObject("data")
            
            ValidateCodeResponse(
                success = success,
                codeValid = data?.optBoolean("codeValid", false) ?: false,
                needsEmail = data?.optBoolean("needsEmail", false) ?: false,
                error = error
            )
        } catch (e: Exception) {
            ValidateCodeResponse(false, false, false, "NETWORK_ERROR")
        }
    }

    suspend fun linkEmail(code: String, email: String, installationId: String, appVersion: String): LinkEmailResponse {
        val json = JSONObject().apply {
            put("code", code)
            put("email", email)
            put("installationId", installationId)
            put("appVersion", appVersion)
        }
        return try {
            val response = ApiClient.post("/api/v1/premium/activate-code/link-email", json)
            val wrapper = JSONObject(response.body)
            val success = wrapper.optBoolean("success", false)
            val error = wrapper.optString("error", "").takeIf { it.isNotBlank() }
            val data = wrapper.optJSONObject("data")
            
            LinkEmailResponse(
                success = success,
                premiumActive = data?.optBoolean("premiumActive", false) ?: false,
                entitlementId = data?.optJSONObject("entitlement")?.optString("id", "")?.takeIf { it.isNotBlank() },
                planType = data?.optJSONObject("entitlement")?.optString("plan", "")?.takeIf { it.isNotBlank() },
                expiresAt = if (data?.optJSONObject("entitlement")?.isNull("expiresAt") == false) data.optJSONObject("entitlement")?.getLong("expiresAt") else null,
                error = error
            )
        } catch (e: Exception) {
            LinkEmailResponse(false, false, null, null, null, "NETWORK_ERROR")
        }
    }

    suspend fun requestRecovery(
        email: String,
        installationId: String,
        appVersion: String,
        platform: String = "ANDROID"
    ): RecoveryRequestResponse {
        val json = JSONObject().apply {
            put("email", email)
            put("installationId", installationId)
            put("platform", platform)
            put("appVersion", appVersion)
        }
        return try {
            val response = ApiClient.post("/api/v1/app/premium/recover/request", json)
            val wrapper = JSONObject(response.body)
            val success = response.isSuccess() && wrapper.optBoolean("success", false)
            val data = wrapper.optJSONObject("data")

            RecoveryRequestResponse(
                success = success,
                email = data?.optString("email", "")?.takeIf { it.isNotBlank() },
                message = data?.optString("message", "")?.takeIf { it.isNotBlank() },
                error = if (success) null else ApiClient.errorMessage(response.body, "NETWORK_ERROR")
            )
        } catch (e: Exception) {
            RecoveryRequestResponse(false, null, null, "NETWORK_ERROR")
        }
    }

    suspend fun verifyRecovery(
        email: String,
        otp: String,
        installationId: String,
        appVersion: String,
        platform: String = "ANDROID"
    ): RecoveryVerifyResponse {
        val json = JSONObject().apply {
            put("email", email)
            put("otp", otp)
            put("installationId", installationId)
            put("platform", platform)
            put("appVersion", appVersion)
        }
        return try {
            val response = ApiClient.post("/api/v1/app/premium/recover/verify", json)
            val wrapper = JSONObject(response.body)
            val success = response.isSuccess() && wrapper.optBoolean("success", false)
            val data = wrapper.optJSONObject("data")
            val entitlement = data?.optJSONObject("entitlement")

            RecoveryVerifyResponse(
                success = success,
                premium = data?.optBoolean("premium", false) ?: false,
                notFound = data?.optBoolean("notFound", false) ?: false,
                entitlementId = entitlement?.optString("id", "")?.takeIf { it.isNotBlank() },
                planType = entitlement?.optString("planType", "")?.takeIf { it.isNotBlank() },
                features = entitlement?.opt("features")?.toString()?.takeIf { it.isNotBlank() && it != "null" },
                expiresAt = if (entitlement?.isNull("expiresAt") == false) entitlement.getLong("expiresAt") else null,
                message = data?.optString("message", "")?.takeIf { it.isNotBlank() },
                error = if (success) null else ApiClient.errorMessage(response.body, "NETWORK_ERROR")
            )
        } catch (e: Exception) {
            RecoveryVerifyResponse(false, false, false, null, null, null, null, null, "NETWORK_ERROR")
        }
    }
}
