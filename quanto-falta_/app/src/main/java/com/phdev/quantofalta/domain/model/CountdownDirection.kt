package com.phdev.quantofalta.domain.model

enum class CountdownDirection {
    REMAINING,  // mostra quanto falta
    ELAPSED,    // mostra quanto passou
    AUTO        // REMAINING antes da data, ELAPSED depois
}
