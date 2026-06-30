package com.phdev.quantofalta.core.designsystem.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Icon

val availableIcons = mapOf(
    // Viagens & Férias
    "Airplane" to Icons.Filled.Flight,
    "FlightTakeoff" to Icons.Filled.FlightTakeoff,
    "Beach" to Icons.Filled.BeachAccess,
    "Luggage" to Icons.Filled.Luggage,
    "Boat" to Icons.Filled.Sailing,
    "Car" to Icons.Filled.DirectionsCar,
    "Train" to Icons.Filled.Train,
    "Map" to Icons.Filled.Map,
    "Explore" to Icons.Filled.Explore,

    // Festas & Comemorações
    "Cake" to Icons.Filled.Cake,
    "Celebration" to Icons.Filled.Celebration,
    "Gift" to Icons.Filled.CardGiftcard,
    "Wine" to Icons.Filled.WineBar,
    "Bar" to Icons.Filled.LocalBar,
    "Trophy" to Icons.Filled.EmojiEvents,

    // Romance & Família
    "Favorite" to Icons.Filled.Favorite,
    "Baby" to Icons.Filled.ChildFriendly,
    "Pregnant" to Icons.Filled.PregnantWoman,
    "Family" to Icons.Filled.FamilyRestroom,
    "Diversity" to Icons.Filled.Diversity1,

    // Marcos de Vida
    "School" to Icons.Filled.School,
    "Work" to Icons.Filled.Work,
    "Home" to Icons.Filled.Home,
    "Apartment" to Icons.Filled.Apartment,
    "Business" to Icons.Filled.Business,

    // Saúde & Bem-Estar
    "Hospital" to Icons.Filled.LocalHospital,
    "Fitness" to Icons.Filled.FitnessCenter,
    "Yoga" to Icons.Filled.SelfImprovement,
    "Spa" to Icons.Filled.Spa,
    "Medical" to Icons.Filled.MedicalServices,

    // Esportes
    "Soccer" to Icons.Filled.SportsSoccer,
    "Basketball" to Icons.Filled.SportsBasketball,
    "Tennis" to Icons.Filled.SportsTennis,
    "Bike" to Icons.Filled.PedalBike,

    // Arte & Entretenimento
    "Music" to Icons.Filled.MusicNote,
    "Theater" to Icons.Filled.TheaterComedy,
    "Movie" to Icons.Filled.Movie,
    "Ticket" to Icons.Filled.LocalActivity,
    "Game" to Icons.Filled.VideogameAsset,
    "Palette" to Icons.Filled.Palette,
    "Camera" to Icons.Filled.CameraAlt,

    // Comida & Bebida
    "Restaurant" to Icons.Filled.Restaurant,
    "Cafe" to Icons.Filled.LocalCafe,
    "Bakery" to Icons.Filled.BakeryDining,
    "Pizza" to Icons.Filled.LocalPizza,
    "Fastfood" to Icons.Filled.Fastfood,

    // Natureza & Ar Livre
    "Sun" to Icons.Filled.WbSunny,
    "Eco" to Icons.Filled.Eco,
    "Terrain" to Icons.Filled.Terrain,
    "Park" to Icons.Filled.Park,
    "Landscape" to Icons.Filled.Landscape,
    "Pets" to Icons.Filled.Pets,

    // Finanças & Compras
    "Cart" to Icons.Filled.ShoppingCart,
    "Mall" to Icons.Filled.LocalMall,
    "Money" to Icons.Filled.AttachMoney,
    "Bank" to Icons.Filled.AccountBalance,

    // Gerais
    "Event" to Icons.Filled.Event,
    "Star" to Icons.Filled.Star,
    "Check" to Icons.Filled.CheckCircle,
    "Notifications" to Icons.Filled.Notifications
)

val iconNamesList = availableIcons.keys.toList()

val iconCategories = mapOf(
    "Básicos" to listOf("Event", "Star", "Favorite", "Cake", "Airplane", "School", "Work", "Check", "Notifications"),
    "Viagens" to listOf("Airplane", "FlightTakeoff", "Beach", "Luggage", "Boat", "Car", "Train", "Map", "Explore"),
    "Festas" to listOf("Cake", "Celebration", "Gift", "Wine", "Bar", "Trophy"),
    "Família" to listOf("Favorite", "Baby", "Pregnant", "Family", "Diversity"),
    "Rotina" to listOf("School", "Work", "Home", "Apartment", "Business"),
    "Saúde" to listOf("Hospital", "Fitness", "Yoga", "Spa", "Medical"),
    "Esportes" to listOf("Soccer", "Basketball", "Tennis", "Bike"),
    "Lazer" to listOf("Music", "Theater", "Movie", "Ticket", "Game", "Palette", "Camera"),
    "Comida" to listOf("Restaurant", "Cafe", "Bakery", "Pizza", "Fastfood"),
    "Natureza" to listOf("Sun", "Eco", "Terrain", "Park", "Landscape", "Pets"),
    "Finanças" to listOf("Cart", "Mall", "Money", "Bank")
)

val iconDisplayNames = mapOf(
    // Viagens
    "Airplane" to "Avião / Viagem / Voo / Aeroporto / Passagem / Férias",
    "FlightTakeoff" to "Decolagem / Embarque / Viagem / Partida",
    "Beach" to "Praia / Férias / Mar / Verão / Sol / Litoral",
    "Luggage" to "Malas / Bagagem / Viagem / Arrumação",
    "Boat" to "Barco / Cruzeiro / Navio / Lancha / Marítimo / Viagem",
    "Car" to "Carro / Estrada / Viagem / Veículo / Direção / Automóvel",
    "Train" to "Trem / Metrô / Viagem / Estação / Trilhos",
    "Map" to "Mapa / Destino / Localização / Roteiro / Viagem",
    "Explore" to "Bússola / Aventura / Explorar / Viagem / Trilha",

    // Festas
    "Cake" to "Bolo / Aniversário / Festa / Comemoração / Doces",
    "Celebration" to "Festa / Celebração / Balada / Comemoração / Alegria",
    "Gift" to "Presente / Surpresa / Aniversário / Doação / Natal",
    "Wine" to "Taça / Brinde / Vinho / Festa / Bebida / Casamento",
    "Bar" to "Festa / Bar / Balada / Bebida / Chopp / Cerveja / Drinks",
    "Trophy" to "Troféu / Conquista / Vitória / Prêmio / Campeão / Sucesso",

    // Romance e Família
    "Favorite" to "Coração / Romance / Amor / Namoro / Casamento / Paixão",
    "Baby" to "Bebê / Nascimento / Criança / Filho / Chá de Bebê / Maternidade",
    "Pregnant" to "Gravidez / Grávida / Gestante / Maternidade",
    "Family" to "Família / Pais / Filhos / Encontro Familiar",
    "Diversity" to "Amigos / Reunião / Grupo / Pessoas / Amizade / Galera",

    // Vida
    "School" to "Formatura / Escola / Faculdade / Estudos / Curso / Aula",
    "Work" to "Trabalho / Emprego / Profissão / Escritório / Maleta / Entrevista",
    "Home" to "Casa / Mudança / Lar / Moradia / Imóvel",
    "Apartment" to "Apartamento / Prédio / Mudança / Condomínio / Imóvel",
    "Business" to "Negócios / Reunião / Empresa / Corporativo",

    // Saúde
    "Hospital" to "Saúde / Médico / Hospital / Clínica / Cirurgia / Tratamento",
    "Fitness" to "Academia / Treino / Musculação / Exercício / Malhar / Peso",
    "Yoga" to "Yoga / Meditação / Relaxamento / Alongamento",
    "Spa" to "Bem-Estar / Spa / Massagem / Relax / Estética",
    "Medical" to "Exame / Consulta / Saúde / Check-up / Médico / Maleta",

    // Esportes
    "Soccer" to "Futebol / Jogo / Partida / Bola / Esporte / Pelada",
    "Basketball" to "Basquete / Bola / Jogo / Esporte / Quadra",
    "Tennis" to "Tênis / Raquete / Jogo / Esporte",
    "Bike" to "Bicicleta / Pedal / Ciclismo / Esporte / Passeio",

    // Entretenimento
    "Music" to "Música / Show / Concerto / Festival / Banda",
    "Theater" to "Teatro / Cultura / Peça / Apresentação / Comédia / Arte",
    "Movie" to "Cinema / Filme / Pipoca / Estreia / Ingresso / Tela",
    "Ticket" to "Ingresso / Evento / Bilhete / Entrada / Show / Passe",
    "Game" to "Jogos / Videogame / Controle / Gamer / Partida",
    "Palette" to "Artes / Pintura / Cultura / Desenho / Criatividade",
    "Camera" to "Foto / Ensaio / Câmera / Fotografia / Sessão",

    // Comida
    "Restaurant" to "Restaurante / Jantar / Almoço / Refeição / Comer / Comida",
    "Cafe" to "Café / Encontro / Cafeteria / Bebida Quente / Xícara",
    "Bakery" to "Padaria / Café / Pão / Lanche / Croissant",
    "Pizza" to "Pizza / Lanche / Comida / Delivery / Pizzaria",
    "Fastfood" to "Fast Food / Hambúrguer / Lanche / Comida / Batata",

    // Natureza
    "Sun" to "Sol / Verão / Calor / Dia / Praia / Luz",
    "Eco" to "Natureza / Planta / Folha / Meio Ambiente / Sustentável / Verde",
    "Terrain" to "Montanha / Trilha / Natureza / Paisagem / Aventura / Serra",
    "Park" to "Parque / Ar Livre / Praça / Árvore / Natureza / Bosque",
    "Landscape" to "Paisagem / Campo / Interior / Vista / Horizonte",
    "Pets" to "Pet / Animal / Cachorro / Gato / Pata / Veterinário",

    // Finanças
    "Cart" to "Compras / Mercado / Carrinho / Supermercado",
    "Mall" to "Shopping / Loja / Compras / Vendas / Passeio",
    "Money" to "Dinheiro / Custo / Finanças / Pagamento / Salário",
    "Bank" to "Banco / Finanças / Conta / Economia / Instituição",

    // Gerais
    "Event" to "Calendário / Evento / Data / Agenda / Compromisso",
    "Star" to "Destaque / Especial / Estrela / Favorito / Importante",
    "Check" to "Conclusão / Meta / Feito / Completo / Sucesso / Pronto",
    "Notifications" to "Lembrete / Sino / Alarme / Aviso / Notificação"
)


fun getIconDisplayName(name: String): String {
    return iconDisplayNames[name] ?: name
}

fun getIconByName(name: String): ImageVector {
    return availableIcons[name] ?: Icons.Filled.Star
}

@Composable
fun AdaptiveIcon(
    iconName: String,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    tint: Color = LocalContentColor.current
) {
    Icon(
        imageVector = getIconByName(iconName),
        contentDescription = contentDescription,
        modifier = modifier,
        tint = tint
    )
}
