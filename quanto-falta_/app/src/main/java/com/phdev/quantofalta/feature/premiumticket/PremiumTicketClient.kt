package com.phdev.quantofalta.feature.premiumticket

import android.content.Context
import android.net.Uri
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.phdev.quantofalta.core.analytics.InstallationManager
import com.phdev.quantofalta.core.auth.AuthManager
import com.phdev.quantofalta.core.network.ApiClient
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.io.ByteArrayOutputStream

data class TicketMessage(
    val id: Long,
    val sender: String,
    val messageType: String,
    val body: String?,
    val attachmentUrl: String?,
    val createdAt: Long
)

class PremiumTicketClient(private val context: Context) {
    private val auth = AuthManager(context)
    private val prefs = context.getSharedPreferences("support_tickets", Context.MODE_PRIVATE)

    suspend fun checkActiveTicket(): String? {
        prefs.getString("conversation_id", null)?.let { return it }
        val id = fetchActiveTicketFromServer()
        if (id != null) prefs.edit().putString("conversation_id", id).apply()
        return id
    }

    suspend fun getActiveTicketStatus(): String? {
        val id = checkActiveTicket() ?: return null
        return runCatching {
            val response = get("/api/v1/app/tickets/$id")
            if (response.isSuccess()) {
                ApiClient.unwrapDataObject(response.body)
                    .optJSONObject("ticket")
                    ?.optString("status")
            } else null
        }.getOrNull()
    }

    private suspend fun fetchActiveTicketFromServer(): String? {
        val installationId = InstallationManager.getOrCreateId(context)
        val email = auth.getEmail()
        var url = "/api/v1/app/tickets?device_id=$installationId"
        if (!email.isNullOrBlank()) {
            url += "&email=${Uri.encode(email)}"
        }
        
        val response = get(url)
        if (response.isSuccess()) {
            val tickets = ApiClient.unwrapDataObject(response.body).optJSONArray("tickets")
            if (tickets != null && tickets.length() > 0) {
                val activeStatus = listOf("novo", "em_analise", "aguardando_admin", "aguardando_usuario", "aguardando_sistema", "aguardando_pagamento", "pagamento_confirmado", "token_enviado")
                for (i in 0 until tickets.length()) {
                    val ticket = tickets.getJSONObject(i)
                    if (ticket.getString("status") in activeStatus) {
                        val id = ticket.getString("id")
                        prefs.edit().putString("conversation_id", id).apply()
                        return id
                    }
                }
            }
        }
        return null
    }

    suspend fun createTicket(
        nome: String, 
        email: String, 
        mensagemInicial: String,
        categoria: String? = null,
        prioridadeSugerida: String? = null,
        resumoTriagem: String? = null,
        respostasTriagem: String? = null,
        origemInstalacao: String? = null,
        versaoAndroid: String? = null,
        modeloAparelho: String? = null,
        isPremiumLocal: Boolean? = null
    ): String {
        val installationId = InstallationManager.getOrCreateId(context)
        val response = ApiClient.post("/api/v1/app/tickets", JSONObject().apply {
            put("nome", nome)
            put("email", email)
            put("origem", "app")
            if (mensagemInicial.isNotBlank()) put("mensagemInicial", mensagemInicial)
            put("deviceId", installationId)
            
            try {
                put("versaoApp", context.packageManager.getPackageInfo(context.packageName, 0).versionName)
            } catch (e: Exception) {
                put("versaoApp", "desconhecida")
            }
            put("plataforma", "ANDROID")
            
            origemInstalacao?.let { put("origemInstalacao", it) }
            versaoAndroid?.let { put("versaoAndroid", it) }
            modeloAparelho?.let { put("modeloAparelho", it) }
            isPremiumLocal?.let { put("statusPremiumAtual", if (it) "premium_local" else "free_local") }
            
            categoria?.let { put("categoria", it) }
            prioridadeSugerida?.let { put("prioridadeSugerida", it) }
            resumoTriagem?.let { put("resumoTriagem", it) }
            respostasTriagem?.let { put("respostasTriagem", it) }
            put("telaOrigem", "support_tickets_app")
            if (categoria != null) {
                put("versaoTriagem", 1)
                put("forceNew", true)
            }
            
            auth.getUserId()?.let { put("usuarioId", it) }
        })
        if (!response.isSuccess()) error(ApiClient.errorMessage(response.body, "Não foi possível abrir o atendimento."))
        val ticket = ApiClient.unwrapDataObject(response.body).getJSONObject("ticket")
        val id = ticket.getString("id")
        prefs.edit().putString("conversation_id", id).apply()
        return id
    }

    suspend fun sendMessage(conversationId: String, body: String) {
        val response = ApiClient.post("/api/v1/app/tickets/$conversationId/messages", JSONObject().apply {
            put("texto", body)
            put("origem", "app")
        })
        if (!response.isSuccess()) error(ApiClient.errorMessage(response.body, "Não foi possível enviar."))
    }

    data class TicketData(
        val messages: List<TicketMessage>,
        val status: String,
        val protocol: String
    )

    suspend fun fetchMessages(conversationId: String): TicketData {
        val path = "/api/v1/app/tickets/$conversationId"
        val response = get(path)
        if (!response.isSuccess()) error(ApiClient.errorMessage(response.body, "Não foi possível atualizar a conversa."))
        val data = ApiClient.unwrapDataObject(response.body)
        val ticket = data.optJSONObject("ticket")
        val array = data.optJSONArray("messages")
        val messages = buildList {
            if (array != null) for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                add(TicketMessage(
                    item.getLong("id"),
                    item.getString("autorTipo").let { if (it == "usuario") "USER" else "ADMIN" },
                    "TEXT", // Default since messageType doesn't exist on ticket_messages
                    item.optString("texto").takeIf { it.isNotBlank() },
                    item.optString("attachmentUrl").takeIf { it.isNotBlank() },
                    item.getLong("enviadoEm")
                ))
            }
        }
        val status = ticket?.optString("status") ?: "aguardando_admin"
        val protocol = ticket?.optString("protocolo") ?: ""
        return TicketData(messages, status, protocol)
    }

    suspend fun uploadProof(conversationId: String, uri: Uri) {
        val bytes = compressProof(uri)
        require(bytes.size <= 2_500_000) { "Escolha uma imagem de até 2,5 MB." }
        val boundary = "----QuantoFalta${System.currentTimeMillis()}"
        val connection = URL("${ApiClient.baseUrl}/api/v1/app/tickets/$conversationId/proof").openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 15_000
            connection.readTimeout = 30_000
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
            connection.outputStream.buffered().use { output ->
                fun field(name: String, value: String) {
                    output.write("--$boundary\r\nContent-Disposition: form-data; name=\"$name\"\r\n\r\n$value\r\n".toByteArray())
                }
                field("origem", "app")
                output.write("--$boundary\r\nContent-Disposition: form-data; name=\"image\"; filename=\"comprovante.jpg\"\r\nContent-Type: image/jpeg\r\n\r\n".toByteArray())
                output.write(bytes)
                output.write("\r\n--$boundary--\r\n".toByteArray())
            }
            if (connection.responseCode !in 200..299) error("Não foi possível enviar o comprovante.")
        } finally {
            connection.disconnect()
        }
    }

    private fun compressProof(uri: Uri): ByteArray {
        val original = context.contentResolver.openInputStream(uri)?.use(BitmapFactory::decodeStream)
            ?: error("Imagem inválida.")
        val maxSide = 1600
        val scale = minOf(1f, maxSide.toFloat() / maxOf(original.width, original.height))
        val resized = if (scale < 1f) {
            Bitmap.createScaledBitmap(original, (original.width * scale).toInt(), (original.height * scale).toInt(), true)
        } else original
        return ByteArrayOutputStream().use { output ->
            resized.compress(Bitmap.CompressFormat.JPEG, 78, output)
            if (resized !== original) resized.recycle()
            original.recycle()
            output.toByteArray()
        }
    }

    private suspend fun get(path: String): ApiClient.Response = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val connection = URL("${ApiClient.baseUrl}$path").openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 10_000
            connection.readTimeout = 15_000
            val status = connection.responseCode
            val stream = if (status >= 400) connection.errorStream else connection.inputStream
            ApiClient.Response(status, stream?.bufferedReader()?.use { it.readText() }.orEmpty())
        } finally {
            connection.disconnect()
        }
    }

    suspend fun markAsRead(conversationId: String) {
        runCatching {
            ApiClient.post("/api/v1/app/tickets/$conversationId/read", JSONObject())
        }
    }
}
