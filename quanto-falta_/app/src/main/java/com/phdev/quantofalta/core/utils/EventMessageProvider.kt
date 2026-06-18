package com.phdev.quantofalta.core.utils

import kotlin.math.abs

object EventMessageProvider {

    enum class Category {
        TRAVEL, CELEBRATION, FINANCE, ENTERTAINMENT, WORK, HEALTH, SPORTS, EDUCATION, GENERAL
    }

    enum class TemporalBucket {
        COMPLETED, TODAY, VERY_SOON, SOON, DISTANT, VERY_DISTANT
    }

    /**
     * Tenta adivinhar a categoria do evento analisando o título (NLP simples em Português).
     * Se não encontrar palavras-chave, recai para a categoria do ícone.
     */
    private fun getCategoryFromContext(title: String, iconName: String): Category {
        val lower = title.lowercase()

        // 1. Viagens
        if (lower.containsAny(
                "viagem", "voo", "férias", "embarque", "intercâmbio", "trip",
                "disney", "passagem", "cruzeiro", "hotel", "hostel", "mochilão",
                "paris", "europa", "eua", "orlando", "japão", "miami", "cancún",
                "aeroporto", "passaporte", "visto", "mala", "roteiro", "tour"
            )
        ) return Category.TRAVEL

        // 2. Saúde
        if (lower.containsAny(
                "cirurgia", "consulta", "médico", "dentista", "exame", "operação",
                "tratamento", "quimio", "terapia", "psicólogo", "nutricionista",
                "academia", "dieta", "emagrecer", "musculação", "treino", "crossfit",
                "maratona", "corrida", "ciclismo", "natação", "yoga", "pilates"
            )
        ) return Category.HEALTH

        // 3. Esportes
        if (lower.containsAny(
                "futebol", "jogo", "partida", "campeonato", "copa", "final", "olímpicos",
                "olimpíadas", "mundial", "libertadores", "brasileirão", "clássico",
                "nba", "nfl", "mma", "luta", "ufc", "boxe", "tênis", "f1", "fórmula"
            )
        ) return Category.SPORTS

        // 4. Celebrações
        if (lower.containsAny(
                "aniversário", "niver", "casamento", "festa", "chá", "formatura",
                "reveillon", "natal", "bodas", "mêsversário", "mesversário",
                "noivado", "debutante", "15 anos", "quinze anos", "batizado",
                "confraternização", "reencontro", "despedida", "aposentadoria"
            )
        ) return Category.CELEBRATION

        // 5. Financeiro
        if (lower.containsAny(
                "pagar", "dívida", "juntar", "comprar", "parcela", "carro",
                "casa", "investimento", "milionário", "salário", "dinheiro", "fatura",
                "aposentadoria", "renda", "poupança", "ação", "bitcoin", "cripto",
                "imóvel", "apartamento", "terreno", "empréstimo", "financiamento",
                "quitação", "meta financeira", "riqueza", "patrimônio"
            )
        ) return Category.FINANCE

        // 6. Educação
        if (lower.containsAny(
                "prova", "enem", "concurso", "tcc", "faculdade", "escola",
                "entrevista", "apresentação", "mestrado", "doutorado", "pós",
                "vestibular", "oab", "cfc", "residência", "monografia", "dissertação",
                "defesa", "colação", "certificação", "curso", "aula", "módulo"
            )
        ) return Category.EDUCATION

        // 7. Trabalho
        if (lower.containsAny(
                "trabalho", "projeto", "reunião", "lançamento", "entrega", "prazo",
                "deadline", "sprint", "release", "produto", "startup", "empresa",
                "emprego", "promoção", "demissão", "cliente", "contrato", "proposta"
            )
        ) return Category.WORK

        // 8. Entretenimento
        if (lower.containsAny(
                "show", "cinema", "filme", "lollapalooza", "rock in rio",
                "estreia", "concerto", "festival", "série", "temporada", "livro",
                "teatro", "musical", "peça", "exposição", "evento", "convenção",
                "comic con", "game", "lançamento"
            )
        ) return Category.ENTERTAINMENT

        return getCategoryForIcon(iconName)
    }

    private fun String.containsAny(vararg keywords: String): Boolean =
        keywords.any { this.contains(it) }

    /**
     * Retorna a categoria do evento baseada no ícone escolhido (usado como fallback).
     */
    fun getCategoryForIcon(iconName: String): Category {
        return when (iconName) {
            "Airplane", "FlightTakeoff", "BeachAccess", "Luggage", "Map" -> Category.TRAVEL
            "Cake", "Favorite", "Star", "Celebration", "CardGiftcard" -> Category.CELEBRATION
            "AttachMoney", "AccountBalance", "TrendingUp", "Savings" -> Category.FINANCE
            "MusicNote", "SportsEsports", "LocalCafe", "Movie", "Headphones" -> Category.ENTERTAINMENT
            "Work", "Business", "Assignment", "Schedule" -> Category.WORK
            "School", "MenuBook", "AutoStories", "Psychology" -> Category.EDUCATION
            "SportsFootball", "SportsSoccer", "SportsBasketball", "FitnessCenter" -> Category.SPORTS
            "LocalHospital", "HealthAndSafety", "FavoriteBorder", "Healing" -> Category.HEALTH
            else -> Category.GENERAL
        }
    }

    /**
     * Determina o balde temporal baseado nos dias restantes.
     */
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
            units.equals("minutos", ignoreCase = true) ||
            units.equals("horas", ignoreCase = true) -> TemporalBucket.TODAY

            units.equals("dias", ignoreCase = true) -> when {
                value == 0      -> TemporalBucket.TODAY
                value in 1..7   -> TemporalBucket.VERY_SOON
                value in 8..30  -> TemporalBucket.SOON
                value in 31..180 -> TemporalBucket.DISTANT
                else            -> TemporalBucket.VERY_DISTANT
            }

            units.equals("semanas", ignoreCase = true) -> when {
                value <= 1      -> TemporalBucket.VERY_SOON
                value in 2..4   -> TemporalBucket.SOON
                value in 5..26  -> TemporalBucket.DISTANT
                else            -> TemporalBucket.VERY_DISTANT
            }

            units.equals("meses", ignoreCase = true) -> when {
                value <= 1      -> TemporalBucket.SOON
                value in 2..6   -> TemporalBucket.DISTANT
                else            -> TemporalBucket.VERY_DISTANT
            }

            units.equals("anos", ignoreCase = true) -> TemporalBucket.VERY_DISTANT

            else -> if (isSoon) TemporalBucket.SOON else TemporalBucket.DISTANT
        }
    }

    /**
     * Seleciona uma mensagem de destaque determinística baseada no ID do evento.
     */
    fun getHighlightMessage(
        eventId: String,
        iconName: String,
        numberStr: String,
        units: String,
        isCompleted: Boolean,
        isSoon: Boolean,
        rawTitle: String
    ): String {
        val sanitizedTitle = TitleValidator.sanitizeTitle(rawTitle)
        val category = getCategoryFromContext(rawTitle, iconName)
        val bucket = getTemporalBucket(numberStr, units, isCompleted, isSoon)
        val messages = getMessagesFor(category, bucket, sanitizedTitle)
        val hash = abs(eventId.hashCode())
        return messages[hash % messages.size]
    }

    private fun getMessagesFor(
        category: Category,
        bucket: TemporalBucket,
        title: String
    ): List<String> {

        // --- COMPLETADO ---
        if (bucket == TemporalBucket.COMPLETED) {
            return listOf(
                "O momento chegou! Esperamos que $title tenha sido incrível. 🎉",
                "O grande dia de $title finalmente chegou! ✨",
                "Chegou a hora de $title! Aproveite ao máximo! 🌟",
                "A espera por $title valeu a pena! Que momento especial. 🏆",
                "É hoje! $title finalmente está acontecendo. Aproveite! 🥳",
                "A contagem chegou ao fim. Bem-vindo(a) ao dia de $title! 🎊",
                "Zero! É o dia de $title. Que tudo saia perfeito! 🌈"
            )
        }

        // --- HOJE ---
        if (bucket == TemporalBucket.TODAY) {
            return listOf(
                "A contagem para $title está quase no fim! ⏳",
                "Faltam apenas algumas horas para $title! 🚀",
                "Está chegando a hora de $title! Preparado(a)? 🔥",
                "Hoje é o dia! $title acontece nas próximas horas. 😍",
                "O grande momento de $title está batendo na porta! 🚪✨",
                "Quase lá! $title começa em poucas horas. Respira fundo! 🌬️",
                "Hoje é dia de $title! Que energia boa essa! ⚡",
                "Não há mais tempo pra esperar — $title é hoje! 🙌"
            )
        }

        // --- POR CATEGORIA ---
        return when (category) {

            // ===== VIAGEM =====
            Category.TRAVEL -> when (bucket) {
                TemporalBucket.VERY_SOON -> listOf(
                    "Quase na hora de arrumar as malas para $title! ✈️",
                    "Sua aventura em $title está logo ali! 🌎",
                    "Falta pouquíssimo para você aproveitar $title. 🏖️",
                    "Separe o passaporte, $title chegou na reta final! 🎒",
                    "Confirme os documentos: $title está chegando! 🗂️",
                    "Última semana antes de $title! Hora de fechar as malas. 👜",
                    "Já deu aquela ansiedade boa de véspera? $title está quase aí! 🌍",
                    "Que semana boa! $title está chegando e você merece. ✈️🎉"
                )
                TemporalBucket.SOON -> listOf(
                    "Comece a planejar os detalhes de $title! 🗺️",
                    "A contagem para $title segue firme e empolgante! ⏳",
                    "Já escolheu as roupas para $title? O tempo está passando! 👕",
                    "Que tal pesquisar os melhores restaurantes para $title? 🍽️",
                    "Reserve as atrações com antecedência para $title! 🎟️",
                    "O roteiro de $title está sendo preparado? Vai ser incrível! 🗺️✨",
                    "Anota os destinos imperdíveis para $title antes que se esqueça! 📝",
                    "Câmera carregada e coração acelerado para $title? 📸"
                )
                TemporalBucket.DISTANT -> listOf(
                    "Ainda há tempo, mas $title vai chegar num piscar de olhos! 🌅",
                    "Enquanto $title não chega, vá sonhando com o roteiro. 💭",
                    "Use esse tempo para economizar e deixar $title ainda melhor! 💰",
                    "Já pesquisou hospedagem para $title? Antecipe e economize! 🏨",
                    "O seguro viagem para $title já está nos planos? Não esqueça! 🛡️",
                    "Dicas de viagem para $title: pesquise, planeje e sonhe! 🌐",
                    "Cada dia que passa é um dia mais perto de $title. Paciência! 🧳"
                )
                TemporalBucket.VERY_DISTANT -> listOf(
                    "Uma longa jornada até $title, mas a espera vai valer a pena! 🛤️",
                    "Grandes viagens exigem planejamento. Foque em $title! 🗺️",
                    "Sonhe grande e economize: $title vai ser épico! 💫",
                    "Quanto mais você se planejar, melhor será $title. Comece já! 📋",
                    "Esse é o tipo de sonho que energiza os dias: $title te espera! 🌟",
                    "Ainda é longe, mas $title já vale cada centavo economizado! 🌏",
                    "Guarde o máximo que puder: $title vai compensar cada sacrifício! 🙏"
                )
                else -> emptyList()
            }

            // ===== SAÚDE =====
            Category.HEALTH -> when (bucket) {
                TemporalBucket.VERY_SOON -> listOf(
                    "Você está na reta final de preparação para $title! 💪",
                    "Falta pouco para $title. Descanse bem e se hidrate! 💧",
                    "Sua dedicação para $title vai dar frutos em breve! 🥗",
                    "Quase chegando em $title. Confie no processo! 🧘‍♀️",
                    "Últimos dias antes de $title. Cuide do corpo e da mente! 🌿"
                )
                TemporalBucket.SOON -> listOf(
                    "Mantenha a consistência: $title está chegando! 🏋️‍♂️",
                    "Cada escolha saudável te aproxima de $title. Continue! 🥦",
                    "Você está no caminho certo para $title. Não desanime! 🌱",
                    "Disciplina hoje, resultado amanhã. $title está perto! ⏱️",
                    "Seu corpo vai agradecer quando $title chegar! 💚"
                )
                TemporalBucket.DISTANT -> listOf(
                    "A jornada até $title é tão importante quanto o destino. 🌿",
                    "Hábitos construídos agora vão brilhar em $title! ✨",
                    "Paciência e constância: $title é o reflexo da sua dedicação. 💪",
                    "Pequenas mudanças diárias chegam longe até $title! 📈",
                    "O tempo que falta para $title é tempo de evolução! 🔄"
                )
                TemporalBucket.VERY_DISTANT -> listOf(
                    "Uma meta de saúde requer tempo. $title é o destino! 🏔️",
                    "Grandes transformações levam tempo. $title vai mostrar isso! 🌟",
                    "Comece hoje: cada passo te leva para $title! 👟",
                    "Meses de dedicação vão construir o melhor você para $title! 💎",
                    "A longa jornada até $title começa com um único passo. Dê-o! 🚶‍♂️"
                )
                else -> emptyList()
            }

            // ===== ESPORTES =====
            Category.SPORTS -> when (bucket) {
                TemporalBucket.VERY_SOON -> listOf(
                    "Tá chegando! $title está na reta final! ⚽🔥",
                    "A emoção de $title está quase no ar! 🏟️",
                    "Separa a camisa e esquenta a voz: $title vem aí! 📣",
                    "Últimos dias de ansiedade antes de $title! 🏆",
                    "Que semana boa! $title está logo ali! ⚡"
                )
                TemporalBucket.SOON -> listOf(
                    "A contagem regressiva para $title está esquentando! 🔥",
                    "Já garantiu seu lugar para $title? Corre! 🎟️",
                    "O clima de $title está no ar! 🌤️⚽",
                    "Cada dia a menos é um dia a mais de expectativa para $title! 📅",
                    "Vai ter muito esporte bom com $title chegando! 🥇"
                )
                TemporalBucket.DISTANT -> listOf(
                    "Ainda tem uma espera boa para $title, mas vai valer! 🏅",
                    "Use o tempo livre para acompanhar a preparação para $title! 📊",
                    "Curta a antecipação: $title promete muita emoção! 🎯",
                    "Quanto mais perto de $title, mais animado(a) você vai ficar! ⚡"
                )
                TemporalBucket.VERY_DISTANT -> listOf(
                    "Ainda é longe, mas $title já dá aquele frio na barriga! ❄️🔥",
                    "Guarda essa energia toda para $title! 💪",
                    "A espera longa deixa $title ainda mais especial. 🏆",
                    "Vai ser épico! $title é um evento pra contar histórias! 🌟"
                )
                else -> emptyList()
            }

            // ===== CELEBRAÇÃO =====
            Category.CELEBRATION -> when (bucket) {
                TemporalBucket.VERY_SOON -> listOf(
                    "Quase na hora de celebrar $title! Prepare a festa! 🎉",
                    "O grande momento de $title está prestes a acontecer! 🎁",
                    "A ansiedade está a mil para $title, falta bem pouco! ✨",
                    "Últimos preparativos para $title! Vai ser lindo! 🌹",
                    "Bate aquela emoção de véspera? $title está chegando! 😍",
                    "Tudo pronto para $title? Vai ser inesquecível! 🎊",
                    "O coração já acelerou? $title está logo ali! 💓"
                )
                TemporalBucket.SOON -> listOf(
                    "A ansiedade para $title já começou de vez! 🎂",
                    "Já mandou os convites? $title vem aí! 🎈",
                    "Os preparativos para $title estão a todo vapor! 🎀",
                    "Mal pode esperar por $title? A gente entende! 🥳",
                    "Cada dia mais perto de $title. Que alegria boa! 💛",
                    "Já começou a planejar $title até os mínimos detalhes? 📋🎊"
                )
                TemporalBucket.DISTANT -> listOf(
                    "A contagem regressiva para $title está rolando! ✨",
                    "Um brinde ao futuro! Estamos contando os dias para $title. 🥂",
                    "A data de $title ainda é distante, mas a animação é agora! 🎉",
                    "Vai sendo que $title vai ser um momento único! 💫",
                    "Sorria: $title está no horizonte e promete muito! 😊"
                )
                TemporalBucket.VERY_DISTANT -> listOf(
                    "Grandes celebrações merecem um longo planejamento! $title vai ser perfeito. 💍",
                    "Sonhe grande para $title: o tempo para planejar é esse! 🌈",
                    "Cada detalhe pensado agora transforma $title em magia. ✨",
                    "Guarda essa animação toda para $title: vai precisar! 🎊",
                    "O melhor está por vir. $title vai ser épico! 🥂"
                )
                else -> emptyList()
            }

            // ===== FINANÇAS =====
            Category.FINANCE -> when (bucket) {
                TemporalBucket.VERY_SOON -> listOf(
                    "Sua meta financeira para $title está quase sendo batida! 💰",
                    "Falta bem pouco para você alcançar $title! 📈",
                    "A reta final para liquidar $title chegou. Força! 💳",
                    "Quase lá! $title está ao alcance das suas mãos! 🙌",
                    "Os últimos esforços para $title vão valer muito! 💎",
                    "Você chegou longe! $title é questão de dias! 🚀",
                    "Foco total: $title está na reta final! 🎯"
                )
                TemporalBucket.SOON -> listOf(
                    "Foco no seu objetivo financeiro: $title! 🎯",
                    "Cada dia e cada centavo mais perto de conquistar $title. 💼",
                    "Economizar agora significa aproveitar $title depois! 🐖",
                    "A disciplina financeira de hoje constrói $title amanhã! 📊",
                    "Pequenos gastos evitados hoje = $title mais perto! ✂️💰",
                    "Continue no ritmo: $title está no horizonte! 🌅",
                    "Seu esforço financeiro tem um nome: $title! Não desista! 💪"
                )
                TemporalBucket.DISTANT -> listOf(
                    "Grandes metas pedem consistência. $title vai chegar! 🏗️",
                    "Invista com sabedoria: cada rendimento te aproxima de $title. 📈",
                    "A paciência financeira é o caminho certo para $title! ⏳",
                    "Revise seus gastos: cada corte é um passo em direção a $title. 🔍",
                    "O tempo trabalhando a seu favor te leva a $title mais rápido! ⚡",
                    "Cada mês poupado é um tijolo a mais na fundação de $title! 🧱"
                )
                TemporalBucket.VERY_DISTANT -> listOf(
                    "Grandes metas financeiras levam tempo. Continue firme em $title! 🧗‍♂️",
                    "Paciência e consistência para alcançar $title. 📊",
                    "O juros compostos estão trabalhando para $title. Deixa rodar! 💹",
                    "Anos de disciplina constroem $title. Você consegue! 🏛️",
                    "Que sonho grande! $title exige fôlego, mas vai valer cada sacrifício. 🌟",
                    "Divida $title em metas menores e celebre cada conquista no caminho! 🎯"
                )
                else -> emptyList()
            }

            // ===== EDUCAÇÃO =====
            Category.EDUCATION -> when (bucket) {
                TemporalBucket.VERY_SOON -> listOf(
                    "A reta final de $title chegou! Foco total e bons estudos! 📚",
                    "Revise os pontos principais: $title está batendo na porta! 💼",
                    "Você consegue! $title vai mostrar tudo o que você aprendeu. 🎓",
                    "Últimas revisões para $title. Confia no que você estudou! ✍️",
                    "Amanhã é dia de $title! Dorme cedo, foca amanhã! 🌙📖",
                    "Feche os cadernos cedo: $title precisa de você descansado(a)! 😴✅",
                    "Calma e confiança: você está pronto(a) para $title! 🧠💡"
                )
                TemporalBucket.SOON -> listOf(
                    "Mantenha o ritmo de estudos! $title está no horizonte. 📅",
                    "Organize-se e prepare-se: $title vai chegar logo. 🗓️",
                    "Disciplina! $title vai exigir o seu melhor. 📝",
                    "Monte um cronograma de estudos pensando em $title! ⏰",
                    "Que assunto está mais fraco para $title? Reforce agora! 🔍",
                    "Cada hora de estudo é um investimento certo em $title! 💡",
                    "Simule provas e prepare sua mente para $title! 📋"
                )
                TemporalBucket.DISTANT -> listOf(
                    "Construa a base sólida que $title vai exigir. 🏗️",
                    "O conhecimento acumulado agora vai brilhar em $title! ✨",
                    "Estude com inteligência: qualidade vale mais que quantidade para $title! 🧠",
                    "Crie hábitos de estudo consistentes pensando em $title. 📚",
                    "Cada leitura, cada exercício: tudo conta para $title! ✅"
                )
                TemporalBucket.VERY_DISTANT -> listOf(
                    "Um passo de cada vez constrói o caminho até $title. 📈",
                    "A jornada é longa! Continue se preparando para $title. 🏗️",
                    "Meses de preparo transformam $title em realidade. 💪",
                    "Invista em conhecimento agora: $title vai cobrar o melhor de você! 🎓",
                    "Grandes conquistas como $title começam muito antes do dia D! 🌱"
                )
                else -> emptyList()
            }

            // ===== TRABALHO =====
            Category.WORK -> when (bucket) {
                TemporalBucket.VERY_SOON -> listOf(
                    "A reta final de $title chegou! Foco e capricho! 🚀",
                    "Revise os detalhes: $title está batendo na porta! 💼",
                    "Últimos ajustes em $title. Você chegou longe! ✅",
                    "Hora de dar o seu melhor em $title! Vai ser incrível! 🔥",
                    "O time está pronto e $title está quase aqui! 💪",
                    "Confirme tudo, respira fundo e entrega $title com orgulho! 🎯",
                    "Poucos dias para $title. Cada detalhe conta agora! 🔍"
                )
                TemporalBucket.SOON -> listOf(
                    "Mantenha o ritmo! $title está no horizonte. 📅",
                    "Organize-se: $title vai exigir planejamento. 🗓️",
                    "Quebre $title em tarefas menores e avance todo dia! 📋",
                    "Quais são os maiores riscos para $title? Endereça-os agora! ⚠️",
                    "Alinhe o time e avance: $title não espera! 🏃‍♂️",
                    "Cada entrega parcial te deixa mais perto de $title! ✅",
                    "Foco no que importa: $title está no prazo! ⏰"
                )
                TemporalBucket.DISTANT -> listOf(
                    "Use o tempo bem: $title vai exigir planejamento sólido. 🏗️",
                    "Mapeie os riscos e desafios de $title com antecedência. 🗺️",
                    "Construa o caminho até $title passo a passo. 🧱",
                    "Líderes antecipam. Prepare-se bem para $title! 📊",
                    "Cada decisão tomada hoje afeta $title. Pense bem! 💡"
                )
                TemporalBucket.VERY_DISTANT -> listOf(
                    "Grandes projetos exigem visão de longo prazo. $title é o alvo! 🎯",
                    "A fundação sólida construída agora vai sustentar $title! 🏛️",
                    "A consistência diária transforma $title em realidade. 🔄",
                    "Pense lá na frente: $title vai exigir o seu melhor. 🌟",
                    "Meses de trabalho construem histórias de sucesso como $title! 📖"
                )
                else -> emptyList()
            }

            // ===== ENTRETENIMENTO =====
            Category.ENTERTAINMENT -> when (bucket) {
                TemporalBucket.VERY_SOON -> listOf(
                    "A diversão de $title está batendo na porta! 🎮",
                    "Prepare-se para curtir $title em breve! 🎵",
                    "Quase lá! $title vai ser sensacional! 🎤",
                    "Últimos dias antes de $title. A expectativa tá nas alturas! 🚀",
                    "Já separou o look para $title? Vai ser épico! 🎉",
                    "Conta os dias: $title chega em breve e promete demais! 🌟",
                    "Segura a ansiedade que $title está chegando! 🤩"
                )
                TemporalBucket.SOON -> listOf(
                    "A contagem para $title não para! 🎧",
                    "Falta pouco para você aproveitar $title. ☕",
                    "Cada dia a menos é um dia mais de expectativa com $title! 🎶",
                    "Garanta seu ingresso/acesso para $title antes que esgote! 🎟️",
                    "Convide alguém especial para $title. Vai ser ainda melhor! 👫"
                )
                TemporalBucket.DISTANT -> listOf(
                    "A espera faz parte da aventura que é $title! 🎢",
                    "Use o tempo para maratonar o que precisa antes de $title! 📺",
                    "Vai ter muita coisa boa para comentar depois de $title! 💬",
                    "Curta a antecipação: $title vai valer cada segundo de espera! ✨"
                )
                TemporalBucket.VERY_DISTANT -> listOf(
                    "É longe, mas $title já merece aquele lugar especial no coração! ❤️",
                    "Guarda essa energia toda para $title. Vai ser demais! 🔋",
                    "A expectativa longa torna $title ainda mais especial! 🌠",
                    "Imagina como vai ser incrível $title quando finalmente chegar! 🌟"
                )
                else -> emptyList()
            }

            // ===== GERAL =====
            Category.GENERAL -> when (bucket) {
                TemporalBucket.VERY_SOON -> listOf(
                    "Apenas alguns dias separam você de $title! ⏳",
                    "Falta muito pouco para $title! 🌟",
                    "A contagem regressiva para $title está na reta final! 🔥",
                    "Quase chegou a hora de $title! Prepare-se! 🎯",
                    "$title está a um passo. Você chegou longe! 🚀",
                    "Foco e energia: $title está chegando! 💪",
                    "Últimos dias antes de $title. Cada momento conta! ✨"
                )
                TemporalBucket.SOON -> listOf(
                    "O tempo voa e $title está cada vez mais perto! 🗓️",
                    "Fique de olho: $title vem aí! 👀",
                    "A contagem para $title segue seu ritmo! ⏱️",
                    "Planeje e se prepare: $title está chegando! 📋",
                    "Cada semana a menos é uma semana mais perto de $title! 🌅",
                    "Vai chegando devagar, mas $title vem firme e forte! 💫"
                )
                TemporalBucket.DISTANT -> listOf(
                    "A espera por $title continua, mas vai valer a pena! 🌈",
                    "Cada dia conta! Estamos de olho em $title por você. 🔭",
                    "Paciência! $title chegará no momento certo. ⏳",
                    "Use bem esse tempo que ainda tem antes de $title! 🌱",
                    "Ainda dá para se preparar muito bem para $title! 📅",
                    "Aproveite o processo: $title está construindo algo especial! 🏗️"
                )
                TemporalBucket.VERY_DISTANT -> listOf(
                    "O horizonte de $title ainda está distante, mas é real! 🌄",
                    "Grandes expectativas exigem paciência. $title vai chegar! ⌛",
                    "É longe, mas $title já merece todo o seu planejamento. 📋",
                    "Quem espera sempre alcança. $title é prova disso! 🌟",
                    "A distância de $title é proporcional ao tamanho do sonho! 💭",
                    "Cada mês que passa é um mês a menos para $title. Persista! 🔄"
                )
                else -> emptyList()
            }
        }
    }
}