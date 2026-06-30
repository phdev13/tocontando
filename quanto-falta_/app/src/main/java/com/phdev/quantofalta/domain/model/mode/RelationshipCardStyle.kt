package com.phdev.quantofalta.domain.model.mode

enum class RelationshipCardStyle(
    override val styleId: String,
    override val displayName: String,
    override val isPremium: Boolean
) : CardStyleItem {
    HEART("heart", "Coração", false),
    CHRONOLOGY("chronology", "Cronologia", true),
    PROGRESS("progress", "Progresso", true),
    CIRCLE("circle", "Circular", true),
    TIMELINE("timeline", "Linha do Tempo", true),
    MINIMAL("minimal", "Minimalista", false),
    POSTER("poster", "Cartaz", true),
    DATE_FOCUS("date_focus", "Data em Destaque", true),
    DETAILED("detailed", "Detalhado", true),
    EMOTIONAL("emotional", "Emocional", true);

    companion object {
        fun fromId(id: String?): RelationshipCardStyle {
            return entries.find { it.styleId == id } ?: HEART
        }
    }
}
