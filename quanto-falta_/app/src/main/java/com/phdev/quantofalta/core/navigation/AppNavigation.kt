package com.phdev.quantofalta.core.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch
import com.phdev.quantofalta.ToContandoApplication
import com.phdev.quantofalta.feature.home.HomeScreen
import com.phdev.quantofalta.feature.createevent.CreateEventScreen
import com.phdev.quantofalta.feature.eventdetails.EventDetailsScreen
import com.phdev.quantofalta.feature.completed.CompletedScreen
import com.phdev.quantofalta.feature.highlight.HighlightScreen
import com.phdev.quantofalta.feature.more.MoreScreen
import com.phdev.quantofalta.feature.intro.IntroScreen
import com.phdev.quantofalta.feature.testers.TestersScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.phdev.quantofalta.feature.more.*
import com.phdev.quantofalta.feature.premium.PremiumScreen
import com.phdev.quantofalta.feature.premium.RedeemCodeScreen
import com.phdev.quantofalta.feature.sponsor.SponsorScreen

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController(),
    onScreenChange: (String) -> Unit = {}
) {
    androidx.compose.runtime.LaunchedEffect(navController) {
        navController.addOnDestinationChangedListener { _, destination, _ ->
            destination.route?.let { route ->
                val screenName = route.substringBefore("?")
                onScreenChange(screenName)
            }
        }
    }

    val context = LocalContext.current
    val appContainer = (context.applicationContext as ToContandoApplication).container
    val isIntroCompleted by appContainer.introManager.isIntroCompleted.collectAsStateWithLifecycle(initialValue = null)

    val isSponsorSeen by appContainer.introManager.isSponsorSeen.collectAsStateWithLifecycle(initialValue = null)

    if (isIntroCompleted == null || isSponsorSeen == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    val startDestination = if (isSponsorSeen == true) {
        if (isIntroCompleted == true) Screen.Home.route else Screen.Intro.route
    } else {
        Screen.Sponsor.route
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Sponsor.route) {
            val scope = androidx.compose.runtime.rememberCoroutineScope()
            SponsorScreen(
                onTimeout = {
                    scope.launch {
                        appContainer.introManager.setSponsorSeen(true)
                    }
                    val nextRoute = if (isIntroCompleted == true) Screen.Home.route else Screen.Intro.route
                    navController.navigate(nextRoute) {
                        popUpTo(Screen.Sponsor.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.Intro.route) {
            IntroScreen(
                onNavigate = { route ->
                    if (route == Screen.Home.route) {
                        navController.navigate(route) {
                            popUpTo(Screen.Intro.route) { inclusive = true }
                        }
                    } else if (route.startsWith("create_event")) {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Intro.route) { inclusive = true }
                        }
                        navController.navigate(route)
                    } else {
                        navController.navigate(route)
                    }
                }
            )
        }
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigate = { route -> navController.navigate(route) }
            )
        }
        composable(
            route = Screen.CreateEvent.route,
            arguments = listOf(
                androidx.navigation.navArgument("eventId") { 
                    nullable = true
                    type = androidx.navigation.NavType.StringType
                    defaultValue = null 
                },
                androidx.navigation.navArgument("prefillTitle") {
                    nullable = true
                    type = androidx.navigation.NavType.StringType
                    defaultValue = null
                },
                androidx.navigation.navArgument("prefillColor") {
                    nullable = true
                    type = androidx.navigation.NavType.StringType
                    defaultValue = null
                },
                androidx.navigation.navArgument("prefillIconName") {
                    nullable = true
                    type = androidx.navigation.NavType.StringType
                    defaultValue = null
                },
                androidx.navigation.navArgument("prefillDaysLeft") {
                    nullable = true
                    type = androidx.navigation.NavType.StringType
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val eventId = backStackEntry.arguments?.getString("eventId")?.takeIf { it != "{eventId}" }
            val prefillTitle = backStackEntry.arguments?.getString("prefillTitle")?.takeIf { it != "{prefillTitle}" }
            val prefillColorHex = backStackEntry.arguments?.getString("prefillColor")?.takeIf { it != "{prefillColor}" }
            val prefillIconName = backStackEntry.arguments?.getString("prefillIconName")?.takeIf { it != "{prefillIconName}" }
            val prefillDaysLeft = backStackEntry.arguments?.getString("prefillDaysLeft")?.takeIf { it != "{prefillDaysLeft}" }?.toIntOrNull()
            CreateEventScreen(
                eventId = eventId,
                prefillTitle = prefillTitle,
                prefillColorHex = prefillColorHex,
                prefillIconName = prefillIconName,
                onBack = { navController.popBackStack() },
                onNavigate = { route -> navController.navigate(route) }
            )
        }
        composable(Screen.EventDetails.route) { backStackEntry ->
            val eventId = backStackEntry.arguments?.getString("eventId") ?: ""
            EventDetailsScreen(
                eventId = eventId,
                onBack = { navController.popBackStack() },
                onNavigate = { route -> navController.navigate(route) }
            )
        }
        composable(Screen.Completed.route) {
            CompletedScreen(
                onNavigate = { route -> navController.navigate(route) }
            )
        }
        composable(Screen.Highlight.route) { backStackEntry ->
            val eventId = backStackEntry.arguments?.getString("eventId") ?: ""
            HighlightScreen(
                eventId = eventId,
                onBack = { navController.popBackStack() },
                onNavigate = { route -> navController.navigate(route) }
            )
        }
        composable(Screen.More.route) {
            MoreScreen(
                onNavigate = { route -> navController.navigate(route) }
            )
        }
        composable(Screen.Testers.route) {
            TestersScreen(
                onBack = { navController.popBackStack() },
                onNavigate = { route -> navController.navigate(route) }
            )
        }
        composable(Screen.Premium.route) {
            PremiumScreen(
                billingClientWrapper = appContainer.billingClientWrapper,
                onDismiss = { navController.popBackStack() },
                onNavigateToRedeem = { navController.navigate(Screen.RedeemCode.route) }
            )
        }
        composable(Screen.RedeemCode.route) {
            RedeemCodeScreen(
                entitlementManager = appContainer.entitlementManager,
                onBack = { navController.popBackStack() },
                onCodeRedeemed = {
                    // Após resgatar, atualiza e volta
                    appContainer.billingClientWrapper.queryPurchases()
                    navController.popBackStack()
                }
            )
        }
        composable(Screen.Diagnostics.route) {
            com.phdev.quantofalta.feature.diagnostics.DiagnosticsScreen(
                onBack = { navController.popBackStack() }
            )
        }
        
        // Novas subtelas de ajustes
        composable(Screen.SettingsAppearance.route) {
            SettingsAppearanceScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.SettingsNotifications.route) {
            SettingsNotificationsScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.SettingsBackup.route) {
            SettingsBackupScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.SettingsClearData.route) {
            SettingsClearDataScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.SettingsSupport.route) {
            SettingsSupportScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.SettingsUpdates.route) {
            SettingsUpdatesScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.SettingsPrivacy.route) {
            SettingsPrivacyScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.SettingsSync.route) {
            val syncViewModel: SyncViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                factory = com.phdev.quantofalta.core.AppViewModelProvider.Factory
            )
            SettingsSyncScreen(
                viewModel = syncViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.SettingsAbout.route) {
            SettingsAboutScreen(onBack = { navController.popBackStack() })
        }
    }
}
