package com.phdev.quantofalta.domain.model.mode

enum class SalaryCardStyle(
    override val styleId: String,
    override val displayName: String,
    override val isPremium: Boolean
) : CardStyleItem {
    NEXT_SALARY("next_salary", "Próximo recebimento", false),
    MONTH_PROGRESS("month_progress", "Progresso do ciclo", true),
    BUSINESS_DAYS("business_days", "Dias úteis", true),
    RECEIVING_CYCLE("receiving_cycle", "Painel do ciclo", true),
    SURVIVAL_MODE("survival_mode", "Modo motivacional", true),
    SALARY_GOAL("salary_goal", "Meta financeira", true),
    SALARY_BLOCKS("salary_blocks", "Blocos do ciclo", true),
    VALUE_AND_TIME("value_and_time", "Valor e tempo", true),
    MINIMALIST_FINANCE("minimalist_finance", "Finanças minimalista", false),
    FINANCE_DASHBOARD("finance_dashboard", "Dashboard financeiro", true);

    companion object {
        fun fromId(id: String): SalaryCardStyle {
            return entries.find { it.styleId == id } ?: NEXT_SALARY
        }
    }
}
