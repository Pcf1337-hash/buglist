package com.buglist.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.buglist.BugListApplication
import com.buglist.di.DatabaseProvider
import com.buglist.domain.repository.TagRepository
import com.buglist.presentation.add_person.AddPersonSheet
import com.buglist.presentation.auth.AuthScreen
import com.buglist.presentation.auth.AuthUiState
import com.buglist.presentation.auth.AuthViewModel
import com.buglist.presentation.dashboard.DashboardScreen
import com.buglist.presentation.person_detail.PersonDetailScreen
import com.buglist.presentation.settings.SettingsScreen
import com.buglist.presentation.statistics.StatisticsScreen
import com.buglist.security.BiometricAuthManager
import com.buglist.security.SessionManager

/**
 * Navigation routes used throughout the app.
 */
object Routes {
    const val AUTH = "auth"
    const val DASHBOARD = "dashboard"
    const val PERSON_DETAIL = "person_detail/{personId}"
    const val STATISTICS = "statistics"
    const val SETTINGS = "settings"

    /** Builds the concrete PersonDetail route with the given [personId]. */
    fun personDetail(personId: Long) = "person_detail/$personId"
}

/**
 * Root NavHost for BugList.
 *
 * Auth guard: every route except [Routes.AUTH] requires the session to be authenticated.
 * [SessionManager.isAuthenticated] is observed — when it drops to false (auto-lock or
 * manual sign-out), the user is redirected back to [Routes.AUTH].
 *
 * ## Cold-start optimisation (L-075)
 *
 * On authentication success, [DatabaseProvider.initializeAsync] is called BEFORE navigating
 * to the Dashboard. This starts the SQLCipher DB construction on the IO dispatcher while the
 * navigation transition animation plays (~300 ms). By the time DashboardViewModel requests its
 * first DAO injection, the database is ready and no blocking occurs.
 *
 * Bottom sheets (AddPersonSheet, AddDebtSheet, AddPaymentSheet) are shown as overlay within
 * each destination composable using [remember] state — this avoids navigation nesting
 * and keeps sheet state scoped to the owning screen's lifecycle.
 *
 * @param activity          The hosting [FragmentActivity] passed down for BiometricPrompt.
 * @param biometricManager  [BiometricAuthManager] injected by MainActivity.
 * @param sessionManager    [SessionManager] for auth-state guard and session tracking.
 * @param databaseProvider  [DatabaseProvider] whose async init is triggered after auth success.
 * @param tagRepository     [TagRepository] for seeding default tags after first auth.
 */
@Composable
fun BugListNavHost(
    activity: FragmentActivity,
    biometricManager: BiometricAuthManager,
    sessionManager: SessionManager,
    databaseProvider: DatabaseProvider,
    tagRepository: TagRepository
) {
    val navController = rememberNavController()
    val isAuthenticated by sessionManager.isAuthenticated.collectAsStateWithLifecycle()

    // Auth guard: if session expires while on an inner screen, redirect to auth.
    LaunchedEffect(isAuthenticated) {
        if (!isAuthenticated) {
            val currentRoute = navController.currentDestination?.route
            if (currentRoute != null && currentRoute != Routes.AUTH) {
                navController.navigate(Routes.AUTH) {
                    popUpTo(0) { inclusive = true }
                }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = Routes.AUTH
    ) {
        // ── Auth ─────────────────────────────────────────────────────────────
        composable(Routes.AUTH) {
            val viewModel: AuthViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            // Once authenticated: start DB construction in background, then navigate.
            // The DB init runs while the navigation transition animation plays (~300 ms),
            // so by the time DashboardViewModel is created the database is ready.
            LaunchedEffect(uiState) {
                if (uiState is AuthUiState.Authenticated) {
                    sessionManager.onAuthenticated()

                    // L-075: kick off async DB construction now — runs on IO dispatcher.
                    // initializeAsync() is idempotent so safe to call multiple times.
                    val application = activity.application as BugListApplication
                    databaseProvider.initializeAsync(application)

                    // Seed default tags once the DB is open (idempotent — no-op if already seeded).
                    withContext(Dispatchers.IO) {
                        tagRepository.insertDefaultTagsIfEmpty()
                    }

                    navController.navigate(Routes.DASHBOARD) {
                        popUpTo(Routes.AUTH) { inclusive = true }
                    }
                }
            }

            AuthScreen(
                viewModel = viewModel,
                activity = activity,
                biometricManager = biometricManager,
                onAuthenticated = {
                    // Handled via LaunchedEffect above.
                }
            )
        }

        // ── Dashboard ────────────────────────────────────────────────────────
        composable(Routes.DASHBOARD) {
            var showAddPerson by remember { mutableStateOf(false) }

            DashboardScreen(
                onPersonClick = { personId ->
                    navController.navigate(Routes.personDetail(personId))
                },
                onAddPerson = { showAddPerson = true },
                onStatistics = { navController.navigate(Routes.STATISTICS) },
                onSettings = { navController.navigate(Routes.SETTINGS) }
            )

            if (showAddPerson) {
                AddPersonSheet(
                    onDismiss = { showAddPerson = false },
                    onSaved = { _ -> showAddPerson = false }
                )
            }
        }

        // ── Person Detail ────────────────────────────────────────────────────
        composable(
            route = Routes.PERSON_DETAIL,
            arguments = listOf(navArgument("personId") { type = NavType.LongType })
        ) {
            PersonDetailScreen(
                onBack = { navController.popBackStack() }
            )
        }

        // ── Statistics ───────────────────────────────────────────────────────
        composable(Routes.STATISTICS) {
            StatisticsScreen(
                onBack = { navController.popBackStack() },
                onNavigateToPerson = { personId ->
                    navController.navigate(Routes.personDetail(personId))
                }
            )
        }

        // ── Settings ─────────────────────────────────────────────────────────
        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onDeleteAll = {
                    // Pop everything off the back-stack up to (and including) the dashboard,
                    // then navigate fresh to dashboard so it shows an empty state.
                    navController.navigate(Routes.DASHBOARD) {
                        popUpTo(Routes.AUTH) { inclusive = false }
                        launchSingleTop = true
                    }
                }
            )
        }
    }
}
