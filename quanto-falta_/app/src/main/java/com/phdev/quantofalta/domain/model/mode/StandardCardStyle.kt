package com.phdev.quantofalta.domain.model.mode

enum class StandardCardStyle(
    override val styleId: String,
    override val displayName: String,
    override val isPremium: Boolean
) : CardStyleItem {
    CLASSIC("classic", "Clássico Numérico", false),
    BLOCKS("blocks", "Contagem em Blocos", false),
    LINEAR_PROGRESS("linear_progress", "Progresso Linear", false),
    CIRCULAR_PROGRESS("circular_progress", "Progresso Circular", true),
    TIMELINE("timeline", "Linha do Tempo", true),
    MINIMAL("minimal", "Minimalista", false),
    POSTER("poster", "Cartaz de Evento", true),
    DATE_FOCUS("date_focus", "Destaque por Data", true),
    DETAILED("detailed", "Completo Detalhado", true),
    EMOTIONAL("emotional", "Emocional", true);

    companion object {
        fun fromId(id: String?): StandardCardStyle {
            return entries.find { it.styleId == id } ?: CLASSIC
        }
    }
}
