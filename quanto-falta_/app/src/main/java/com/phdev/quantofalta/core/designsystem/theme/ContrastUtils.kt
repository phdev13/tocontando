package com.phdev.quantofalta.core.designsystem.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

object ContrastUtils {
    /**
     * Retorna Branco ou Preto dependendo da luminância da cor de fundo.
     * Ideal para manter contraste legível em cards com cores customizadas pelo usuário.
     */
    fun getContrastColor(backgroundColor: Color): Color {
        // A luminância varia de 0.0 (preto absoluto) a 1.0 (branco absoluto).
        // Um threshold de 0.5 é comum, mas 0.45 costuma dar melhores resultados visuais em UIs modernas.
        return if (backgroundColor.luminance() > 0.45f) {
            Color(0xFF14141F) // Texto muito escuro para fundos claros
        } else {
            Color.White // Texto branco para fundos escuros
        }
    }
}
