package com.phdev.quantofalta.core.utils

import android.content.ActivityNotFoundException
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast

object SupportEmailUtils {

    fun openSupportEmail(context: Context) {
        val appVersion = try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName ?: "Desconhecida"
        } catch (e: Exception) {
            "Desconhecida"
        }

        val emailBody = """
            Olá, equipe do Tô Contando

            Descreva abaixo sua dúvida, sugestão ou problema:

            [Escreva aqui]

            Informações técnicas:
            - Versão do app: $appVersion
            - Versão do Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})
            - Modelo do dispositivo: ${Build.MANUFACTURER} ${Build.MODEL}

            Essas informações foram adicionadas automaticamente para facilitar o atendimento.
        """.trimIndent()

        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:")
            putExtra(Intent.EXTRA_EMAIL, arrayOf("contato@tocontando.com.br"))
            putExtra(Intent.EXTRA_SUBJECT, "Suporte — Tô Contando")
            putExtra(Intent.EXTRA_TEXT, emailBody)
        }

        try {
            context.startActivity(Intent.createChooser(intent, "Enviar e-mail de suporte"))
        } catch (e: ActivityNotFoundException) {
            // Fallback se não houver cliente de e-mail
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = android.content.ClipData.newPlainText("E-mail de suporte", "contato@tocontando.com.br")
            clipboard.setPrimaryClip(clip)
            
            Toast.makeText(
                context,
                "Nenhum app de e-mail encontrado. O endereço contato@tocontando.com.br foi copiado.",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}
