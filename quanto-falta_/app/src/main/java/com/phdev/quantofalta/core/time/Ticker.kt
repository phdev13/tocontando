package com.phdev.quantofalta.core.time

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

object Ticker {
    fun tickerFlow(intervalMillis: Long): Flow<Long> = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(intervalMillis)
        }
    }
}
