package com.phdev.quantofalta.core

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.phdev.quantofalta.ToContandoApplication
import com.phdev.quantofalta.feature.home.HomeViewModel
import com.phdev.quantofalta.feature.standard.CreateEventViewModel
import com.phdev.quantofalta.feature.eventdetails.EventDetailsViewModel
import com.phdev.quantofalta.feature.completed.CompletedViewModel

object AppViewModelProvider {
    val Factory = viewModelFactory {
        initializer {
            val app = quantoFaltaApplication()
            HomeViewModel(
                app,
                app.container.eventRepository,
                app.container.billingClientWrapper,
                app.container.entitlementManager
            )
        }
        initializer {
            val app = quantoFaltaApplication()
            CreateEventViewModel(
                app,
                app.container.eventRepository,
                app.container.permissionsUseCase
            )
        }
        initializer {
            val app = quantoFaltaApplication()
            EventDetailsViewModel(
                app,
                app.container.eventRepository,
                app.container.entitlementManager
            )
        }
        initializer {
            val app = quantoFaltaApplication()
            CompletedViewModel(app, app.container.eventRepository)
        }
        initializer {
            val app = quantoFaltaApplication()
            com.phdev.quantofalta.feature.highlight.HighlightViewModel(app, app.container.eventRepository)
        }
        initializer {
            com.phdev.quantofalta.feature.intro.IntroViewModel(
                quantoFaltaApplication().container.introManager
            )
        }
        initializer {
            val app = quantoFaltaApplication()
            com.phdev.quantofalta.feature.more.MoreViewModel(
                app,
                app.container.eventRepository,
                app.container.themeManager,
                app.container.introManager,
                app.container.entitlementManager,
                app.container.privacySettings
            )
        }
        initializer {
            com.phdev.quantofalta.feature.timeline.TimelineViewModel(
                com.phdev.quantofalta.core.database.AppDatabase.getDatabase(quantoFaltaApplication())
            )
        }
        initializer {
            val app = quantoFaltaApplication()
            com.phdev.quantofalta.feature.more.SyncViewModel(
                app,
                app.container.authManager,
                app.container.database,
                app.container.entitlementManager
            )
        }
        initializer {
            val app = quantoFaltaApplication()
            com.phdev.quantofalta.feature.relationship.RelationshipViewModel(
                app,
                app.container.eventRepository,
                app.container.permissionsUseCase
            )
        }
        initializer {
            val app = quantoFaltaApplication()
            com.phdev.quantofalta.feature.finance.SalaryViewModel(
                app.container.eventRepository,
                app.container.analyticsManager,
                app.container.permissionsUseCase
            )
        }
        initializer {
            val app = quantoFaltaApplication()
            com.phdev.quantofalta.feature.celebration.CelebrationViewModel(
                app.container.eventRepository
            )
        }
    }
}

fun CreationExtras.quantoFaltaApplication(): ToContandoApplication =
    (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as ToContandoApplication)
