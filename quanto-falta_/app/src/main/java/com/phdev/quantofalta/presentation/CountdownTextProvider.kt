package com.phdev.quantofalta.presentation

import android.content.Context
import com.phdev.quantofalta.R
import com.phdev.quantofalta.domain.model.CountdownDirection
import com.phdev.quantofalta.domain.model.CountdownResult
import com.phdev.quantofalta.domain.model.ProgressState

class CountdownTextProvider(private val context: Context) {

    fun getNumberText(result: CountdownResult): String {
        return when (result) {
            is CountdownResult.Days -> result.days.toString()
            is CountdownResult.FullTime -> "${result.days}"
            is CountdownResult.FullTimeWithSeconds -> "${result.days}"
            is CountdownResult.Weeks -> result.weeks.toString()
            is CountdownResult.WeeksAndDays -> result.weeks.toString()
            is CountdownResult.Months -> result.months.toString()
            is CountdownResult.MonthsAndDays -> result.months.toString()
            is CountdownResult.WorkingDays -> result.days.toString()
            is CountdownResult.Percentage -> String.format("%.0f", result.percent)
            is CountdownResult.ElapsedDetailed -> result.years.toString()
            is CountdownResult.Age -> result.years.toString()
            CountdownResult.Today -> "0"
        }
    }

    fun getUnitText(result: CountdownResult): String {
        return when (result) {
            is CountdownResult.Days -> if (result.direction == CountdownDirection.REMAINING) "dias" else "dias atrás"
            is CountdownResult.FullTime -> "dias"
            is CountdownResult.FullTimeWithSeconds -> "dias"
            is CountdownResult.Weeks -> if (result.direction == CountdownDirection.REMAINING) "semanas" else "semanas atrás"
            is CountdownResult.WeeksAndDays -> "semanas"
            is CountdownResult.Months -> if (result.direction == CountdownDirection.REMAINING) "meses" else "meses atrás"
            is CountdownResult.MonthsAndDays -> "meses"
            is CountdownResult.WorkingDays -> "dias úteis"
            is CountdownResult.Percentage -> "%"
            is CountdownResult.ElapsedDetailed -> "anos"
            is CountdownResult.Age -> "anos"
            CountdownResult.Today -> "Hoje"
        }
    }

    fun getFullText(result: CountdownResult): String {
        return when (result) {
            is CountdownResult.Days -> {
                if (result.direction == CountdownDirection.REMAINING) {
                    context.resources.getQuantityString(R.plurals.countdown_days_remaining, result.days.toInt(), result.days.toInt())
                } else {
                    context.resources.getQuantityString(R.plurals.countdown_days_elapsed, result.days.toInt(), result.days.toInt())
                }
            }
            is CountdownResult.FullTime -> {
                "Faltam ${result.days}d ${result.hours}h ${result.minutes}m"
            }
            is CountdownResult.FullTimeWithSeconds -> {
                "Faltam ${result.days}d ${result.hours}h ${result.minutes}m ${result.seconds}s"
            }
            is CountdownResult.Weeks -> {
                if (result.direction == CountdownDirection.REMAINING) {
                    context.resources.getQuantityString(R.plurals.countdown_weeks_remaining, result.weeks.toInt(), result.weeks.toInt())
                } else {
                    context.resources.getQuantityString(R.plurals.countdown_weeks_elapsed, result.weeks.toInt(), result.weeks.toInt())
                }
            }
            is CountdownResult.WeeksAndDays -> {
                "${result.weeks} semanas e ${result.days} dias"
            }
            is CountdownResult.Months -> {
                if (result.direction == CountdownDirection.REMAINING) {
                    context.resources.getQuantityString(R.plurals.countdown_months_remaining, result.months.toInt(), result.months.toInt())
                } else {
                    context.resources.getQuantityString(R.plurals.countdown_months_elapsed, result.months.toInt(), result.months.toInt())
                }
            }
            is CountdownResult.MonthsAndDays -> {
                "${result.months} meses e ${result.days} dias"
            }
            is CountdownResult.WorkingDays -> {
                if (result.direction == CountdownDirection.REMAINING) {
                    "Faltam ${result.days} dias úteis"
                } else {
                    "${result.days} dias úteis decorridos"
                }
            }
            is CountdownResult.Percentage -> {
                when (result.state) {
                    ProgressState.NOT_STARTED -> "Ainda não começou"
                    ProgressState.COMPLETED -> "Concluído (100%)"
                    ProgressState.IN_PROGRESS -> "Completou ${String.format("%.1f", result.percent)}%"
                }
            }
            is CountdownResult.ElapsedDetailed -> {
                "${result.years} anos, ${result.months} meses e ${result.days} dias"
            }
            is CountdownResult.Age -> {
                "${result.years} anos de idade. Próximo niver em ${result.nextBirthdayInDays} dias"
            }
            CountdownResult.Today -> "É hoje!"
        }
    }
}
