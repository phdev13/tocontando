package com.phdev.quantofalta.core.utils

object TitleValidator {
    
    private const val FALLBACK_TITLE = "seu evento"
    
    /**
     * Sanitiza o título do evento para evitar exibição de placeholders de código ou strings inválidas.
     */
    fun sanitizeTitle(title: String?): String {
        if (title == null) return FALLBACK_TITLE
        
        val trimmed = title.trim()
        if (trimmed.isEmpty()) return FALLBACK_TITLE
        
        // Verifica se é o placeholder indesejado que ficou vazado em versões anteriores
        if (trimmed.contains("{prefillTitle}") || trimmed.contains("\${")) {
            return FALLBACK_TITLE
        }
        
        return trimmed
    }
}
