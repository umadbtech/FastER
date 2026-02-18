package com.faster.festival.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.faster.festival.data.local.EncryptedSessionManager
import com.faster.festival.data.repository.AuthRepository
import com.faster.festival.ui.auth.signup.SignupScreen
import com.faster.festival.ui.auth.signup.SignupViewModel
import com.faster.festival.ui.auth.verification.CheckEmailScreen
import com.faster.festival.ui.screens.*

object Routes {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val HOME = "home"
    const val MAP = "map"
    const val SCHEDULE = "schedule"
    const val PROFILE = "profile"
    const val ARTIST_DETAIL = "artist/{artistId}"
    const val TICKETS = "tickets"
    const val WEB = "web/{type}"
    const val SIGNUP = "signup"
    const val CHECK_EMAIL = "check_email/{email}"
    const val AUTH_CALLBACK = "auth_callback/{accessToken}/{refreshToken}"
}

@Composable
fun NavGraph(
        navController: NavHostController = rememberNavController(),
        startDestination: String = Routes.SPLASH,
        authRepository: AuthRepository,
        sessionManager: EncryptedSessionManager
) {
    NavHost(navController = navController, startDestination = startDestination) {
        // Splash Screen
        composable(Routes.SPLASH) {
            SplashScreen(
                    onLoginClick = { navController.navigate(Routes.LOGIN) },
                    onSignupClick = { navController.navigate(Routes.SIGNUP) }
            )
        }

        // Login Screen (placeholder)
        composable(Routes.LOGIN) {
            LoginScreen(
                    onPhoneClick = {},
                    onEmailClick = {},
                    onGoogleClick = {}
            )
        }

        // Signup
        composable(Routes.SIGNUP) {
            val viewModel: SignupViewModel =
                    viewModel(factory = SignupViewModel.Factory(authRepository))
            SignupScreen(
                    viewModel = viewModel,
                    onNavigateToVerification = { email ->
                        navController.navigate("check_email/$email") {
                            popUpTo(Routes.SIGNUP) { inclusive = true }
                        }
                    }
            )
        }

        // Check Email (Magic Link Waiting Screen)
        composable(
                route = Routes.CHECK_EMAIL,
                arguments =
                        listOf(
                                androidx.navigation.navArgument("email") {
                                    type = androidx.navigation.NavType.StringType
                                }
                        )
        ) { backStackEntry ->
            val email = backStackEntry.arguments?.getString("email") ?: ""
            CheckEmailScreen(
                    email = email,
                    sessionManager = sessionManager,
                    onResendEmail = {
                        // Optional: implement resend email via API if needed
                    },
                    onSkip = {
                        navController.navigate(Routes.HOME) {
                            popUpTo(Routes.SIGNUP) { inclusive = true }
                        }
                    },
                    onVerificationComplete = {
                        navController.navigate(Routes.HOME) {
                            popUpTo(Routes.CHECK_EMAIL) { inclusive = true }
                        }
                    }
            )
        }

        // Auth Callback (Magic Link Handler)
        composable(
                route = Routes.AUTH_CALLBACK,
                arguments =
                        listOf(
                                androidx.navigation.navArgument("accessToken") {
                                    type = androidx.navigation.NavType.StringType
                                },
                                androidx.navigation.navArgument("refreshToken") {
                                    type = androidx.navigation.NavType.StringType
                                }
                        )
        ) { backStackEntry ->
            val accessToken = backStackEntry.arguments?.getString("accessToken") ?: ""
            val refreshToken = backStackEntry.arguments?.getString("refreshToken") ?: ""

            LaunchedEffect(accessToken, refreshToken) {
                if (accessToken.isNotEmpty() && refreshToken.isNotEmpty()) {
                    val result = authRepository.processMagicLinkCallback(
                        accessToken = accessToken,
                        refreshToken = refreshToken
                    )
                    result
                        .onSuccess {
                            navController.navigate(Routes.HOME) {
                                popUpTo(Routes.SIGNUP) { inclusive = true }
                            }
                        }
                        .onFailure {
                            navController.navigate(Routes.SIGNUP) {
                                popUpTo(Routes.CHECK_EMAIL) { inclusive = true }
                            }
                        }
                } else {
                    navController.navigate(Routes.SIGNUP) {
                        popUpTo(Routes.AUTH_CALLBACK) { inclusive = true }
                    }
                }
            }
        }

        // Home Tab
        composable(Routes.HOME) {
            HomeScreen(
                    onArtistClick = { artistId -> navController.navigate("artist/$artistId") },
                    onTicketsClick = { navController.navigate(Routes.TICKETS) },
                    onFestivalHomeClick = { navController.navigate("web/festival_home") },
                    onFaqsClick = { navController.navigate("web/faqs") }
            )
        }

        // Map Tab
        composable(Routes.MAP) {
            MapScreen(onTicketsClick = { navController.navigate(Routes.TICKETS) })
        }

        // Schedule Tab
        composable(Routes.SCHEDULE) {
            ScheduleScreen(onTicketsClick = { navController.navigate(Routes.TICKETS) })
        }

        // Profile Tab
        composable(Routes.PROFILE) {
            ProfileScreen(onTicketsClick = { navController.navigate(Routes.TICKETS) })
        }

        // Artist Detail
        composable(
                route = Routes.ARTIST_DETAIL,
                arguments =
                        listOf(
                                androidx.navigation.navArgument("artistId") {
                                    type = androidx.navigation.NavType.StringType
                                }
                        )
        ) { backStackEntry ->
            val artistId = backStackEntry.arguments?.getString("artistId") ?: return@composable
            ArtistDetailScreen(artistId = artistId, onBackClick = { navController.popBackStack() })
        }

        // Tickets
        composable(Routes.TICKETS) { TicketsScreen(onBackClick = { navController.popBackStack() }) }

        // Web Placeholder
        composable(
                route = Routes.WEB,
                arguments =
                        listOf(
                                androidx.navigation.navArgument("type") {
                                    type = androidx.navigation.NavType.StringType
                                }
                        )
        ) { backStackEntry ->
            val type = backStackEntry.arguments?.getString("type") ?: "Info"
            val title =
                    when (type) {
                        "festival_home" -> "Festival Home"
                        "faqs" -> "FAQs"
                        else -> type
                    }
            WebPlaceholderScreen(title = title, onBackClick = { navController.popBackStack() })
        }
    }
}
