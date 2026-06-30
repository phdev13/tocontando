package com.phdev.quantofalta.core.utils

import com.phdev.quantofalta.domain.model.CountdownDirection
import com.phdev.quantofalta.domain.model.CountdownResult
import com.phdev.quantofalta.domain.model.ProgressState
import java.util.Calendar
import kotlin.math.abs

/**
 * AppCopyProvider — Tô Contando
 *
 * Tom de voz: Próximo, caloroso e sofisticado. Como um amigo que acompanha
 * os momentos que importam. Nem frio demais, nem informal demais. Natural.
 *
 * Princípios:
 * - Fala com a pessoa, não pra ela
 * - Celebra a espera, não só o evento
 * - Varia bastante pra não soar robótico
 * - Cada categoria tem sua personalidade
 */
object AppCopyProvider {

    // =========================================================================
    // 1. FORMATAÇÃO DE CONTAGEM REGRESSIVA (Widgets / Cards)
    // =========================================================================

    private fun getDailySeed(): Int =
        Calendar.getInstance().get(Calendar.DAY_OF_YEAR)

    private fun unit(value: Long, singular: String, plural: String): String =
        if (value == 1L) singular else plural

    private fun remaining(value: Long, singular: String, plural: String): String {
        val label = unit(value, singular, plural)
        val seed = getDailySeed() % 4

        if (value == 1L) {
            return listOf(
                "Falta só 1 $label.",
                "Só 1 $label.",
                "Quase lá. 1 $label.",
                "Está chegando. 1 $label."
            )[seed]
        }

        val isShortTime = singular in listOf("dia", "hora", "minuto", "segundo")

        val phrases = when {
            isShortTime && value <= 7 -> listOf(
                "Faltam $value $label.",
                "Só $value $label.",
                "Falta pouco: $value $label.",
                "Está chegando: $value $label."
            )
            isShortTime && value <= 30 -> listOf(
                "Faltam $value $label.",
                "$value $label.",
                "Mais perto: $value $label.",
                "Contagem continua: $value $label."
            )
            isShortTime && value > 100 -> listOf(
                "Faltam $value $label.",
                "A espera continua: $value $label.",
                "$value $label pela frente.",
                "O tempo faz a parte dele: $value $label."
            )
            else -> listOf(
                "Faltam $value $label.",
                "$value $label.",
                "Contagem: $value $label.",
                "Ainda $value $label."
            )
        }

        return phrases[(value % phrases.size).toInt()]
    }

    private fun elapsed(value: Long, singular: String, plural: String): String {
        val label = unit(value, singular, plural)
        val seed = getDailySeed() % 4

        if (value == 1L) {
            return listOf(
                "Faz 1 $label.",
                "1 $label atrás.",
                "Passou 1 $label.",
                "Já faz 1 $label."
            )[seed]
        }

        val isShortTime = singular in listOf("dia", "hora", "minuto", "segundo")

        val phrases = when {
            isShortTime && value <= 7 -> listOf(
                "Faz $value $label.",
                "$value $label atrás.",
                "Passaram-se $value $label.",
                "Aconteceu há $value $label."
            )
            !isShortTime || value > 60 -> listOf(
                "Faz $value $label.",
                "Passaram-se $value $label.",
                "Uma lembrança de $value $label.",
                "Já faz $value $label."
            )
            else -> listOf(
                "Faz $value $label.",
                "Passaram-se $value $label.",
                "$value $label atrás.",
                "Já faz $value $label."
            )
        }

        return phrases[(value % phrases.size).toInt()]
    }

    // -------------------------------------------------------------------------

    fun getNumberText(result: CountdownResult): String {
        return when (result) {
            is CountdownResult.Days               -> result.days.toString()
            is CountdownResult.FullTime           -> result.days.toString()
            is CountdownResult.FullTimeWithSeconds -> result.days.toString()
            is CountdownResult.Weeks              -> result.weeks.toString()
            is CountdownResult.WeeksAndDays       -> result.weeks.toString()
            is CountdownResult.Months             -> result.months.toString()
            is CountdownResult.MonthsAndDays      -> result.months.toString()
            is CountdownResult.WorkingDays        -> result.days.toString()
            is CountdownResult.Percentage         -> String.format("%.0f", result.percent)
            is CountdownResult.ElapsedDetailed    -> result.years.toString()
            is CountdownResult.Age                -> result.years.toString()
            CountdownResult.Today                 -> ""
        }
    }

    fun getUnitText(result: CountdownResult): String {
        return when (result) {
            is CountdownResult.Days -> {
                val label = unit(result.days, "dia", "dias")
                if (result.direction == CountdownDirection.REMAINING) label else "$label atrás"
            }
            is CountdownResult.FullTime           -> unit(result.days, "dia", "dias")
            is CountdownResult.FullTimeWithSeconds -> unit(result.days, "dia", "dias")
            is CountdownResult.Weeks -> {
                val label = unit(result.weeks, "semana", "semanas")
                if (result.direction == CountdownDirection.REMAINING) label else "$label atrás"
            }
            is CountdownResult.WeeksAndDays       -> unit(result.weeks, "semana", "semanas")
            is CountdownResult.Months -> {
                val label = unit(result.months, "mês", "meses")
                if (result.direction == CountdownDirection.REMAINING) label else "$label atrás"
            }
            is CountdownResult.MonthsAndDays      -> unit(result.months, "mês", "meses")
            is CountdownResult.WorkingDays        -> unit(result.days, "dia útil", "dias úteis")
            is CountdownResult.Percentage         -> "%"
            is CountdownResult.ElapsedDetailed    -> unit(result.years.toLong(), "ano", "anos")
            is CountdownResult.Age                -> unit(result.years.toLong(), "ano", "anos")
            CountdownResult.Today                 -> "hoje"
        }
    }

    fun getFullText(result: CountdownResult): String {
        return when (result) {

            is CountdownResult.Days -> {
                if (result.direction == CountdownDirection.REMAINING) {
                    when (result.days) {
                        1L -> listOf(
                            "É amanhã!",
                            "Falta só 1 dia.",
                            "Amanhã chegou a hora.",
                            "Um dia e tá feito.",
                            "A véspera chegou."
                        )[getDailySeed() % 5]
                        else -> remaining(result.days, "dia", "dias")
                    }
                } else {
                    elapsed(result.days, "dia", "dias")
                }
            }

            is CountdownResult.FullTime -> {
                when {
                    result.days == 0L && result.hours == 0 && result.minutes == 0 ->
                        listOf(
                            "É hoje!",
                            "O dia chegou.",
                            "Hoje é o grande dia.",
                            "Chegou a hora.",
                            "O momento é agora."
                        )[getDailySeed() % 5]
                    result.days == 0L && result.hours == 0 ->
                        remaining(result.minutes.toLong(), "minuto", "minutos")
                    result.days == 0L ->
                        remaining(result.hours.toLong(), "hora", "horas")
                    result.days == 1L ->
                        listOf(
                            "É amanhã!",
                            "Falta só 1 dia.",
                            "Amanhã chegou a hora.",
                            "A véspera chegou."
                        )[getDailySeed() % 4]
                    else -> remaining(result.days, "dia", "dias")
                }
            }

            is CountdownResult.FullTimeWithSeconds -> {
                when {
                    result.days == 0L && result.hours == 0 && result.minutes == 0 ->
                        listOf(
                            "Acontecendo agora.",
                            "É agora!",
                            "O momento chegou.",
                            "Tá rolando agora."
                        )[getDailySeed() % 4]
                    result.days == 0L && result.hours == 0 ->
                        remaining(result.minutes.toLong(), "minuto", "minutos")
                    result.days == 0L ->
                        remaining(result.hours.toLong(), "hora", "horas")
                    result.days == 1L ->
                        listOf("É amanhã!", "Falta só 1 dia.", "A véspera chegou.")[getDailySeed() % 3]
                    else -> remaining(result.days, "dia", "dias")
                }
            }

            is CountdownResult.Weeks -> {
                if (result.direction == CountdownDirection.REMAINING)
                    remaining(result.weeks, "semana", "semanas")
                else
                    elapsed(result.weeks, "semana", "semanas")
            }

            is CountdownResult.WeeksAndDays -> {
                val wL = unit(result.weeks, "semana", "semanas")
                val dL = unit(result.days.toLong(), "dia", "dias")
                when {
                    result.weeks == 0L -> remaining(result.days.toLong(), "dia", "dias")
                    result.days == 0   -> remaining(result.weeks, "semana", "semanas")
                    else               -> "${result.weeks} $wL e ${result.days} $dL"
                }
            }

            is CountdownResult.Months -> {
                if (result.direction == CountdownDirection.REMAINING)
                    remaining(result.months, "mês", "meses")
                else
                    elapsed(result.months, "mês", "meses")
            }

            is CountdownResult.MonthsAndDays -> {
                val mL = unit(result.months, "mês", "meses")
                val dL = unit(result.days.toLong(), "dia", "dias")
                when {
                    result.months == 0L -> remaining(result.days.toLong(), "dia", "dias")
                    result.days == 0    -> remaining(result.months, "mês", "meses")
                    else                -> "${result.months} $mL e ${result.days} $dL"
                }
            }

            is CountdownResult.WorkingDays -> {
                if (result.direction == CountdownDirection.REMAINING)
                    remaining(result.days, "dia útil", "dias úteis")
                else
                    elapsed(result.days, "dia útil", "dias úteis")
            }

            is CountdownResult.Percentage -> {
                when (result.state) {
                    ProgressState.NOT_STARTED -> listOf(
                        "Ainda não começou.",
                        "Tudo pela frente.",
                        "A contagem nem começou.",
                        "Aguardando o início."
                    )[getDailySeed() % 4]
                    ProgressState.COMPLETED -> listOf(
                        "Concluído!",
                        "Feito e celebrado.",
                        "Meta atingida.",
                        "100% realizado.",
                        "Chegou lá."
                    )[getDailySeed() % 5]
                    ProgressState.IN_PROGRESS ->
                        "${String.format("%.0f", result.percent)}% do caminho percorrido"
                }
            }

            is CountdownResult.ElapsedDetailed -> {
                val yL = unit(result.years.toLong(), "ano", "anos")
                val mL = unit(result.months.toLong(), "mês", "meses")
                val dL = unit(result.days.toLong(), "dia", "dias")
                "${result.years} $yL, ${result.months} $mL e ${result.days} $dL"
            }

            is CountdownResult.Age -> {
                val yL = unit(result.years.toLong(), "ano", "anos")
                val dL = unit(result.nextBirthdayInDays, "dia", "dias")
                when {
                    result.nextBirthdayInDays == 0L ->
                        "${result.years} $yL de idade. Feliz aniversário hoje!"
                    result.nextBirthdayInDays == 1L ->
                        "${result.years} $yL de idade. O aniversário é amanhã!"
                    else ->
                        "${result.years} $yL de idade. Próximo aniversário em ${result.nextBirthdayInDays} $dL."
                }
            }

            CountdownResult.Today -> listOf(
                "É hoje!",
                "O dia chegou.",
                "Hoje é o grande dia.",
                "Chegou a hora.",
                "O momento é agora."
            )[getDailySeed() % 5]
        }
    }

    // =========================================================================
    // 2. MENSAGENS INSPIRACIONAIS (Telas de Destaque / Detalhes)
    // =========================================================================

    enum class Category {
        TRAVEL, CELEBRATION, FINANCE, ENTERTAINMENT, WORK, HEALTH, SPORTS, EDUCATION, GENERAL
    }

    enum class TemporalBucket {
        COMPLETED, TODAY, VERY_SOON, SOON, DISTANT, VERY_DISTANT
    }

    /**
     * Detecta categoria pelo título do evento (NLP simples em português).
     * Recai para o ícone se não encontrar correspondência.
     */
    private fun getCategoryFromContext(title: String, iconName: String): Category {
        val lower = title.lowercase()

        if (lower.containsAny(
                "viagem", "voo", "férias", "embarque", "intercâmbio", "trip", "disney",
                "passagem", "cruzeiro", "hotel", "hostel", "mochilão", "paris", "europa",
                "eua", "orlando", "japão", "miami", "cancún", "aeroporto", "passaporte",
                "visto", "mala", "roteiro", "tour", "excursão", "mapa", "destino", "embarcar",
                "aterrissagem", "conexão", "escala", "resort", "pousada", "camping", "road trip"
            )) return Category.TRAVEL

        if (lower.containsAny(
                "cirurgia", "consulta", "médico", "dentista", "exame", "operação",
                "tratamento", "quimio", "terapia", "psicólogo", "nutricionista",
                "academia", "dieta", "emagrecer", "musculação", "treino", "crossfit",
                "maratona", "corrida", "ciclismo", "natação", "yoga", "pilates",
                "exercício", "esporte", "ginástica", "saúde", "bem-estar", "detox",
                "jejum", "consulta médica", "check-up", "vacina", "internação"
            )) return Category.HEALTH

        if (lower.containsAny(
                "futebol", "jogo", "partida", "campeonato", "copa", "final",
                "olímpicos", "olimpíadas", "mundial", "libertadores", "brasileirão",
                "clássico", "nba", "nfl", "mma", "luta", "ufc", "boxe", "tênis",
                "f1", "fórmula", "basquete", "vôlei", "handebol", "atletismo",
                "natação competitiva", "ciclismo de estrada", "grand prix", "torneio",
                "liga", "playoff", "semifinal", "derby", "confronto"
            )) return Category.SPORTS

        if (lower.containsAny(
                "aniversário", "niver", "casamento", "festa", "chá", "formatura",
                "reveillon", "natal", "bodas", "mêsversário", "mesversário", "noivado",
                "debutante", "15 anos", "quinze anos", "batizado", "confraternização",
                "reencontro", "despedida", "aposentadoria", "surpresa", "celebração",
                "inauguração", "comemoração", "páscoa", "carnaval", "são joão",
                "réveillon", "formaturas", "colação", "baile de gala", "encontro"
            )) return Category.CELEBRATION

        if (lower.containsAny(
                "pagar", "dívida", "juntar", "comprar", "parcela", "carro", "casa",
                "investimento", "milionário", "salário", "dinheiro", "fatura",
                "poupança", "ação", "bitcoin", "cripto", "imóvel", "apartamento",
                "terreno", "empréstimo", "financiamento", "quitação", "meta financeira",
                "riqueza", "patrimônio", "reserva", "renda passiva", "independência financeira",
                "fgts", "décimo terceiro", "bônus", "metas", "orçamento", "poupar"
            )) return Category.FINANCE

        if (lower.containsAny(
                "prova", "enem", "concurso", "tcc", "faculdade", "escola", "entrevista",
                "apresentação", "mestrado", "doutorado", "pós", "vestibular", "oab",
                "cfc", "residência", "monografia", "dissertação", "defesa", "colação",
                "certificação", "curso", "aula", "módulo", "banca", "simulado",
                "gabarito", "resultado", "bolsa", "processo seletivo", "redação",
                "graduação", "pós-graduação", "bootcamp", "workshop", "palestra"
            )) return Category.EDUCATION

        if (lower.containsAny(
                "trabalho", "projeto", "reunião", "lançamento", "entrega", "prazo",
                "deadline", "sprint", "release", "produto", "startup", "empresa",
                "emprego", "promoção", "demissão", "cliente", "contrato", "proposta",
                "meta", "resultado", "trimestre", "apresentação", "pitch", "feedback",
                "avaliação", "revisão", "aprovação", "relatório", "campanha", "parceria"
            )) return Category.WORK

        if (lower.containsAny(
                "show", "cinema", "filme", "lollapalooza", "rock in rio", "estreia",
                "concerto", "festival", "série", "temporada", "livro", "teatro",
                "musical", "peça", "exposição", "evento", "convenção", "comic con",
                "game", "lançamento", "netflix", "disney+", "premiere", "sarau",
                "stand up", "comédia", "espetáculo", "ópera", "ballet", "dança",
                "galeria", "museu", "exposição de arte", "podcast", "live"
            )) return Category.ENTERTAINMENT

        return getCategoryForIcon(iconName)
    }

    private fun String.containsAny(vararg keywords: String): Boolean =
        keywords.any { this.contains(it) }

    fun getCategoryForIcon(iconName: String): Category {
        return when (iconName) {
            "Airplane", "FlightTakeoff", "BeachAccess", "Luggage", "Map",
            "Explore", "TravelExplore", "Flight", "DirectionsBoat" -> Category.TRAVEL

            "Cake", "Favorite", "Star", "Celebration", "CardGiftcard",
            "Confetti", "PartyMode", "EmojiEvents", "Champagne" -> Category.CELEBRATION

            "AttachMoney", "AccountBalance", "TrendingUp", "Savings",
            "MonetizationOn", "CreditCard", "PriceCheck", "BarChart" -> Category.FINANCE

            "MusicNote", "SportsEsports", "LocalCafe", "Movie", "Headphones",
            "TheaterComedy", "LiveTv", "Album", "VideoLibrary", "Tv" -> Category.ENTERTAINMENT

            "Work", "Business", "Assignment", "Schedule", "Laptop",
            "Briefcase", "Handshake", "PresentToAll", "Timeline" -> Category.WORK

            "School", "MenuBook", "AutoStories", "Psychology",
            "EmojiObjects", "Science", "Biotech", "Calculate" -> Category.EDUCATION

            "SportsFootball", "SportsSoccer", "SportsBasketball",
            "FitnessCenter", "SportsVolleyball", "SportsTennis",
            "DirectionsBike", "Pool", "SelfImprovement" -> Category.SPORTS

            "LocalHospital", "HealthAndSafety", "FavoriteBorder",
            "Healing", "MonitorHeart", "MedicalServices", "Medication" -> Category.HEALTH

            else -> Category.GENERAL
        }
    }

    fun getTemporalBucket(
        numberStr: String,
        units: String,
        isCompleted: Boolean,
        isSoon: Boolean
    ): TemporalBucket {
        if (isCompleted) return TemporalBucket.COMPLETED

        val value = numberStr.toIntOrNull() ?: 0

        return when {
            units.equals("segundos", ignoreCase = true) ||
            units.equals("segundo", ignoreCase = true) ||
            units.equals("minutos",  ignoreCase = true) ||
            units.equals("minuto",  ignoreCase = true) ||
            units.equals("horas",    ignoreCase = true) ||
            units.equals("hora",    ignoreCase = true) -> TemporalBucket.TODAY

            units.equals("dias", ignoreCase = true) ||
            units.equals("dia", ignoreCase = true) -> when {
                value == 0        -> TemporalBucket.TODAY
                value in 1..7     -> TemporalBucket.VERY_SOON
                value in 8..30    -> TemporalBucket.SOON
                value in 31..180  -> TemporalBucket.DISTANT
                else              -> TemporalBucket.VERY_DISTANT
            }

            units.equals("semanas", ignoreCase = true) ||
            units.equals("semana", ignoreCase = true) -> when {
                value <= 1        -> TemporalBucket.VERY_SOON
                value in 2..4     -> TemporalBucket.SOON
                value in 5..26    -> TemporalBucket.DISTANT
                else              -> TemporalBucket.VERY_DISTANT
            }

            units.equals("meses", ignoreCase = true) ||
            units.equals("mês", ignoreCase = true) ||
            units.equals("mes", ignoreCase = true) -> when {
                value <= 1        -> TemporalBucket.SOON
                value in 2..6     -> TemporalBucket.DISTANT
                else              -> TemporalBucket.VERY_DISTANT
            }

            units.equals("anos", ignoreCase = true) ||
            units.equals("ano", ignoreCase = true) -> TemporalBucket.VERY_DISTANT

            else -> if (isSoon) TemporalBucket.SOON else TemporalBucket.DISTANT
        }
    }

    /**
     * Retorna uma mensagem de destaque determinística baseada no ID do evento.
     * O mesmo evento sempre exibe a mesma mensagem, sem aleatoriedade por sessão.
     */
    fun getHighlightMessage(
        eventId: String,
        iconName: String,
        numberStr: String,
        units: String,
        isCompleted: Boolean,
        isSoon: Boolean,
        rawTitle: String,
        eventType: com.phdev.quantofalta.domain.model.EventType = com.phdev.quantofalta.domain.model.EventType.STANDARD
    ): String {
        val sanitizedTitle = TitleValidator.sanitizeTitle(rawTitle)
        val category = getCategoryFromContext(rawTitle, iconName)
        val bucket   = getTemporalBucket(numberStr, units, isCompleted, isSoon)
        val messages = getMessagesFor(category, bucket, sanitizedTitle, eventType)
        val hash     = abs(eventId.hashCode())
        return messages[hash % messages.size]
    }

    private fun getMessagesFor(
        category: Category,
        bucket:   TemporalBucket,
        title:    String,
        type:     com.phdev.quantofalta.domain.model.EventType
    ): List<String> {

        if (bucket == TemporalBucket.COMPLETED) {
            return when (type) {
                com.phdev.quantofalta.domain.model.EventType.SALARY -> listOf("Mais um mês concluído.")
                com.phdev.quantofalta.domain.model.EventType.RELATIONSHIP -> listOf("Mais um marco dessa história.")
                else -> listOf(
                    "Virou memória.",
                    "Esse dia ficou na história.",
                    "O dia chegou. Agora virou memória.",
                    "Guarde esse momento."
                )
            }
        }

        if (bucket == TemporalBucket.TODAY) {
            return when (type) {
                com.phdev.quantofalta.domain.model.EventType.SALARY -> listOf("O mês respira um pouco.", "Hoje o saldo muda.")
                com.phdev.quantofalta.domain.model.EventType.RELATIONSHIP -> listOf("Dia de celebrar vocês.", "Hoje o dia é especial.")
                else -> listOf(
                    "Hoje chegou o grande dia.",
                    "O dia chegou.",
                    "É hoje.",
                    "A espera acabou.",
                    "Agora é viver."
                )
            }
        }

        if (type == com.phdev.quantofalta.domain.model.EventType.SALARY) {
            return when (bucket) {
                TemporalBucket.VERY_SOON -> listOf("O alívio tá chegando.", "Falta pouco pro pagamento.")
                TemporalBucket.SOON -> listOf("O mês tá na metade.", "Já já o saldo muda.")
                else -> listOf("Ainda falta um pouco.", "Paciência, o mês acabou de virar.")
            }
        }

        if (type == com.phdev.quantofalta.domain.model.EventType.RELATIONSHIP) {
            return when (bucket) {
                TemporalBucket.VERY_SOON -> listOf("Falta pouco pra comemorar.", "O amor só cresce.")
                TemporalBucket.SOON -> listOf("Mais perto de celebrar vocês.", "Construindo memórias.")
                else -> listOf("O tempo faz bem pra essa história.", "Um dia de cada vez, juntos.")
            }
        }

        return when (category) {
            Category.TRAVEL -> when (bucket) {
                TemporalBucket.VERY_SOON -> listOf("A mala já tá quase pronta.", "Reta final antes de embarcar.")
                TemporalBucket.SOON -> listOf("A espera faz parte da viagem.", "O roteiro tá tomando forma.")
                else -> listOf("Ainda longe, mas já dá pra sonhar.", "Um longo caminho até o destino.")
            }
            Category.HEALTH -> when (bucket) {
                TemporalBucket.VERY_SOON -> listOf("Falta pouco.", "Quase lá.", "Na reta final do cuidado.")
                TemporalBucket.SOON -> listOf("Um passo por vez.", "O cuidado continua.")
                else -> listOf("Uma etapa de cada vez.", "Foco e paciência.")
            }
            Category.SPORTS -> when (bucket) {
                TemporalBucket.VERY_SOON -> listOf("O evento tá quase aí.", "Prepare-se.", "Reta final.")
                TemporalBucket.SOON -> listOf("A expectativa já começou.", "No radar.")
                else -> listOf("Aguardando o espetáculo.", "Com calma e paciência.")
            }
            Category.CELEBRATION -> when (bucket) {
                TemporalBucket.VERY_SOON -> listOf("A festa tá na porta.", "Dias contados pra comemorar.")
                TemporalBucket.SOON -> listOf("Pensando em cada detalhe.", "A comemoração se aproxima.")
                else -> listOf("A espera é longa, mas especial.", "Deixando tudo perfeito.")
            }
            Category.FINANCE -> when (bucket) {
                TemporalBucket.VERY_SOON -> listOf("O alívio tá chegando.", "Na reta final.")
                TemporalBucket.SOON -> listOf("O esforço continua.", "Um passo de cada vez.")
                else -> listOf("Uma longa etapa financeira.", "Um tijolo por vez.")
            }
            Category.EDUCATION -> when (bucket) {
                TemporalBucket.VERY_SOON -> listOf("A reta final do esforço.", "Últimos dias de foco.")
                TemporalBucket.SOON -> listOf("Os estudos continuam.", "Cada dia de preparo conta.")
                else -> listOf("Um longo caminho de preparo.", "Construindo com calma.")
            }
            Category.WORK -> when (bucket) {
                TemporalBucket.VERY_SOON -> listOf("O prazo tá logo aí.", "Quase entregando.")
                TemporalBucket.SOON -> listOf("O trabalho continua.", "Foco nas entregas.")
                else -> listOf("Uma longa jornada pela frente.", "Foco no planejamento.")
            }
            Category.ENTERTAINMENT -> when (bucket) {
                TemporalBucket.VERY_SOON -> listOf("Falta muito pouco.", "O ingresso já tá na mão.")
                TemporalBucket.SOON -> listOf("A expectativa tá aumentando.", "Tá chegando a hora.")
                else -> listOf("Ainda distante, mas aguardando.", "A paciência faz parte.")
            }
            Category.GENERAL -> when (bucket) {
                TemporalBucket.VERY_SOON -> listOf("Falta pouco.", "A espera quase no fim.", "Últimos dias.")
                TemporalBucket.SOON -> listOf("Está chegando.", "Mais perto a cada dia.", "A contagem continua.")
                else -> listOf("Esse dia está no radar.", "O tempo está fazendo a parte dele.")
            }
        }
    }

    // =========================================================================
    // 3. MÉTODOS DE MEMÓRIAS (Tela de Concluídos)
    // =========================================================================

    /**
     * Retorna a frase de memória exibida nos cards de eventos concluídos.
     * Exemplo: "Virou memória há 3 meses", "Recebido há 5 dias".
     */
    fun getMemoryText(
        result: CountdownResult,
        type: com.phdev.quantofalta.domain.model.EventType,
        isCompletedManual: Boolean = false
    ): String {
        // Se for salário, a frase é diferente
        if (type == com.phdev.quantofalta.domain.model.EventType.SALARY) {
            return when (result) {
                is CountdownResult.Today -> "Recebido hoje"
                else -> {
                    val tempo = getFullText(result).replace("Concluído há ", "").replace("Há ", "").replace("Passaram-se ", "").replace("Uma lembrança de ", "")
                    "Recebido há $tempo"
                }
            }
        }
        
        // Relacionamento
        if (type == com.phdev.quantofalta.domain.model.EventType.RELATIONSHIP) {
            return when (result) {
                is CountdownResult.Today -> "Celebrado hoje"
                else -> {
                    val tempo = getFullText(result).replace("Concluído há ", "").replace("Há ", "").replace("Passaram-se ", "").replace("Uma lembrança de ", "")
                    "Celebrado há $tempo"
                }
            }
        }

        // Padrão
        return when (result) {
            is CountdownResult.Today -> if (isCompletedManual) "Concluído hoje" else "Chegou hoje"
            else -> {
                // Tenta extrair a parte principal da string de tempo decorrido
                val elapsedStr = getFullText(result)
                val timeOnly = elapsedStr
                    .replace("Concluído há ", "")
                    .replace("Uma memória de ", "")
                    .replace("Passaram-se ", "")
                    .replace("Celebrado há ", "")
                    .replace(" desde o momento", "")
                    .replace("Há ", "")
                    .replace(" desde então", "")
                
                val seed = getDailySeed() % 3
                when (seed) {
                    0 -> "Virou memória há $timeOnly"
                    1 -> "Aconteceu há $timeOnly"
                    else -> "Celebrado há $timeOnly"
                }
            }
        }
    }
}