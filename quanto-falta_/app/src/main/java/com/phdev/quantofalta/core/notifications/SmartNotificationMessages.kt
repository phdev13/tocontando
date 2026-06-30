package com.phdev.quantofalta.core.notifications

import com.phdev.quantofalta.core.database.EventEntity

object SmartNotificationMessages {

    data class NotificationContent(
        val title: String,
        val text: String
    )

    fun getMessageForStandard(event: EventEntity, milestone: String): NotificationContent {
        val title = event.title
        return when (milestone) {
            "30d_before" -> NotificationContent(title, "Falta 1 mês para $title. Agora começou a ficar real.")
            "7d_before" -> NotificationContent(title, "Falta 1 semana para $title. A reta final começou.")
            "3d_before" -> NotificationContent(title, "Faltam 3 dias para $title. Tá perto de verdade.")
            "1d_before" -> NotificationContent(title, "É amanhã. $title finalmente chegou na porta.")
            "0d_day" -> NotificationContent(title, "É hoje. A contagem para $title acabou.")
            "1d_after" -> NotificationContent(title, "$title foi ontem. Agora virou memória.")
            else -> NotificationContent(title, "Lembrete para $title.")
        }
    }

    fun getMessageForRelationship(event: EventEntity, milestone: String): NotificationContent {
        val title = event.title
        return when (milestone) {
            "7_days" -> NotificationContent(title, "Hoje vocês completam 7 dias dessa história.")
            "1_month" -> NotificationContent(title, "Hoje vocês completam 1 mês juntos.")
            "3_months" -> NotificationContent(title, "3 meses de $title. Cada dia conta.")
            "6_months" -> NotificationContent(title, "Meio ano juntos. Tem data que merece carinho.")
            "1_year" -> NotificationContent(title, "1 ano. Essa data merece ser lembrada.")
            "500_days" -> NotificationContent(title, "500 dias. Tem coisa que o tempo só deixa mais bonita.")
            "1000_days" -> NotificationContent(title, "1000 dias. Uma bela marca de tempo.")
            else -> {
                if (milestone.endsWith("_years")) {
                    val years = milestone.replace("_years", "")
                    NotificationContent(title, "Hoje essa história completa mais $years ano(s).")
                } else {
                    NotificationContent(title, "Comemorando $title hoje.")
                }
            }
        }
    }

    fun getMessageForSalary(event: EventEntity, milestone: String): NotificationContent {
        val title = event.title
        return when (milestone) {
            "half_cycle" -> NotificationContent(title, "Metade do caminho até o próximo salário. Seu dinheiro ainda respira?")
            "7d_before" -> NotificationContent(title, "Falta 1 semana para o salário. Não inventa moda agora.")
            "3d_before" -> NotificationContent(title, "3 dias para o salário. O pix da esperança tá aquecendo.")
            "1d_before" -> NotificationContent(title, "É amanhã. Confere as contas antes de comemorar.")
            "0d_day" -> NotificationContent(title, "Pagamento previsto para hoje. Confere se caiu.")
            "1d_after" -> NotificationContent(title, "Salário caiu ontem. Já separou o das contas?")
            else -> NotificationContent(title, "Lembrete financeiro: $title.")
        }
    }
}
