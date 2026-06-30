package com.phdev.quantofalta.core.time

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import java.util.Calendar

class ScreenTicker(
    val tickHour: State<Long>,
    val tickMinute: State<Long>,
    val tickSecond: State<Long>
)

val LocalScreenTicker = staticCompositionLocalOf<ScreenTicker> {
    error("No ScreenTicker provided")
}

@Composable
fun ProvideScreenTicker(content: @Composable () -> Unit) {
    val lifecycleOwner = LocalLifecycleOwner.current

    val tickHour = remember(lifecycleOwner) { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                val now = System.currentTimeMillis()
                tickHour.value = now
                val cal = Calendar.getInstance()
                cal.timeInMillis = now
                cal.add(Calendar.HOUR_OF_DAY, 1)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val untilNextHour = cal.timeInMillis - now
                delay(untilNextHour.coerceAtLeast(16L))
            }
        }
    }

    val tickMinute = remember(lifecycleOwner) { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                val now = System.currentTimeMillis()
                tickMinute.value = now
                val cal = Calendar.getInstance()
                cal.timeInMillis = now
                cal.add(Calendar.MINUTE, 1)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val untilNextMinute = cal.timeInMillis - now
                delay(untilNextMinute.coerceAtLeast(16L))
            }
        }
    }

    val tickSecond = remember(lifecycleOwner) { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                val now = System.currentTimeMillis()
                tickSecond.value = now
                val untilNextSecond = 1000L - (now % 1000L)
                delay(untilNextSecond.coerceAtLeast(16L))
            }
        }
    }

    val screenTicker = remember(tickHour, tickMinute, tickSecond) {
        ScreenTicker(tickHour, tickMinute, tickSecond)
    }

    CompositionLocalProvider(LocalScreenTicker provides screenTicker) {
        content()
    }
}
