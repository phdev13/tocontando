package com.phdev.quantofalta.core.auth

import android.content.Context
import android.os.Build
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.phdev.quantofalta.core.network.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

class AuthManager(context: Context) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "auth_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun getAccessToken(): String? = prefs.getString("access_token", null)
    fun getRefreshToken(): String? = prefs.getString("refresh_token", null)
    fun getUserId(): String? = prefs.getString("user_id", null)
    fun getDeviceId(): String? = prefs.getString("device_id", null)
    fun getEmail(): String? = prefs.getString("email", null)
    
    fun getSyncCursor(): String = prefs.getString("sync_cursor", "") ?: ""
    fun saveSyncCursor(cursor: String) {
        prefs.edit().putString("sync_cursor", cursor).apply()
    }
    
    fun getSyncGeneration(): Int = prefs.getInt("sync_generation", 1)
    fun saveSyncGeneration(generation: Int) {
        prefs.edit().putInt("sync_generation", generation).apply()
    }

    fun saveTokens(accessToken: String, refreshToken: String, userId: String, deviceId: String, email: String? = null) {
        val editor = prefs.edit()
            .putString("access_token", accessToken)
            .putString("refresh_token", refreshToken)
            .putString("user_id", userId)
            .putString("device_id", deviceId)
        
        if (email != null) {
            editor.putString("email", email)
        }
        editor.apply()
    }

    fun clearAuth() {
        prefs.edit().clear().apply()
    }

    suspend fun requestOtp(email: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val normalizedEmail = email.trim()
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(normalizedEmail).matches() || normalizedEmail.length > 254) {
                return@withContext Result.failure(IllegalArgumentException("E-mail inválido"))
            }
            val payload = JSONObject().apply {
                put("email", normalizedEmail)
            }
            val response = ApiClient.post("/api/v1/auth/otp/request", payload)
            if (response.isSuccess()) {
                Result.success(Unit)
            } else {
                val errorMsg = ApiClient.errorMessage(response.body, "Erro ao solicitar código")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun verifyOtp(email: String, code: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val normalizedEmail = email.trim()
            val normalizedCode = code.trim()
            if (
                !android.util.Patterns.EMAIL_ADDRESS.matcher(normalizedEmail).matches() ||
                normalizedEmail.length > 254 ||
                normalizedCode.length != 6 ||
                normalizedCode.any { !it.isDigit() }
            ) return@withContext Result.failure(IllegalArgumentException("Dados de verificação inválidos"))
            val payload = JSONObject().apply {
                put("email", normalizedEmail)
                put("code", normalizedCode)
                put("device_name", "${Build.MANUFACTURER} ${Build.MODEL}")
            }
            val response = ApiClient.post("/api/v1/auth/otp/verify", payload)
            if (response.isSuccess()) {
                val json = JSONObject(response.body)
                saveTokens(
                    json.getString("access_token"),
                    json.getString("refresh_token"),
                    json.getString("user_id"),
                    json.getString("device_id"),
                    normalizedEmail
                )
                Result.success(Unit)
            } else {
                val errorMsg = ApiClient.errorMessage(response.body, "Código inválido")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun refreshTokens(): Result<String> = withContext(Dispatchers.IO) {
        try {
            val refreshToken = getRefreshToken()
            val deviceId = getDeviceId()
            if (refreshToken == null || deviceId == null) {
                return@withContext Result.failure(Exception("Sessão não encontrada"))
            }

            val payload = JSONObject().apply {
                put("refresh_token", refreshToken)
                put("device_id", deviceId)
            }
            
            val response = ApiClient.post("/api/v1/auth/refresh", payload)
            if (response.isSuccess()) {
                val json = JSONObject(response.body)
                val newAccess = json.getString("access_token")
                val newRefresh = json.getString("refresh_token")
                prefs.edit()
                    .putString("access_token", newAccess)
                    .putString("refresh_token", newRefresh)
                    .apply()
                Result.success(newAccess)
            } else {
                if (response.statusCode == 401) {
                    clearAuth()
                }
                Result.failure(Exception("Sessão expirada"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun logout(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val token = getAccessToken() ?: return@withContext Result.success(Unit)
            ApiClient.post("/api/v1/auth/logout", JSONObject(), mapOf("Authorization" to "Bearer $token"))
            clearAuth()
            Result.success(Unit)
        } catch (e: Exception) {
            clearAuth() // Clear locally even if remote fails
            Result.success(Unit)
        }
    }
}
