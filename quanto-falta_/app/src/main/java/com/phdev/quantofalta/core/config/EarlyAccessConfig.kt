package com.phdev.quantofalta.core.config

data class EarlyAccessConfig(
    val active: Boolean = false,
    val title: String = "Acesso Antecipado",
    val subtitle: String = "Garanta recursos exclusivos antes do lançamento oficial na Play Store.",
    val price: String = "R$ 19,90",
    val description: String = "Ao participar do acesso antecipado, você ganha acesso vitalício a todos os recursos Premium.",
    val features: List<String> = listOf("Eventos ilimitados", "Fotos de capa personalizadas", "Cores e ícones extras", "Widgets ilimitados"),
    val buttonText: String = "Solicitar Atendimento",
    val instructions: String = "Abra um ticket para conversar com o desenvolvedor, enviar o comprovante de pagamento e receber seu token de ativação.",
    val allowTicket: Boolean = true
)
