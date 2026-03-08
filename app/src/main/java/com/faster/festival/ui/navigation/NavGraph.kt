package com.faster.festival.ui.navigation

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.faster.festival.data.local.EncryptedSessionManager
import com.faster.festival.data.repository.AuthRepository
import com.faster.festival.ui.auth.forgot.ForgotPasswordScreen
import com.faster.festival.ui.auth.forgot.ForgotPasswordViewModel
import com.faster.festival.ui.auth.reset.ResetPasswordScreen
import com.faster.festival.ui.auth.reset.ResetPasswordViewModel
import com.faster.festival.ui.auth.signup.SignupScreen
import com.faster.festival.ui.auth.signup.SignupViewModel
import com.faster.festival.ui.auth.verification.CheckEmailScreen
import com.faster.festival.ui.auth.verification.VerificationSuccessScreen
import com.faster.festival.ui.onboarding.OnboardingScreen
import com.faster.festival.ui.screens.*
import com.faster.festival.ui.viewmodel.EnhancedProfileViewModel
import com.faster.festival.ui.viewmodel.ProfileState
import kotlinx.coroutines.launch

object Routes {
    const val LOGIN = "login"
    const val LOGIN_EMAIL = "login/email"
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val MAP = "map"
    const val SCHEDULE = "schedule"
    const val PROFILE = "profile"
    const val ARTIST_DETAIL = "artist/{artistId}"
    const val TICKETS = "tickets"
    const val WEB = "web/{type}"
    const val SIGNUP = "signup"
    const val CHECK_EMAIL = "otp/{email}"
    const val ENTER_CODE = "enter_code/{email}/{password}/{fullName}"
    const val SIGNUP_SUCCESS = "signup_success"
    const val FORGOT_PASSWORD = "forgot_password"
    const val RESET_PASSWORD = "reset_password/{email}/{token}"
    const val PHONE_LOGIN = "phone_login"
    const val PHONE_OTP = "phone_otp/{phone}"

    // Profile Screen Navigation Routes
    const val PERSONAL_INFO = "personal_info"
    const val EDIT_DEMOGRAPHICS = "edit_demographics"
    const val EMERGENCY_CONTACTS = "emergency_contacts"
    const val HEALTH_SETTINGS = "health_settings"
    const val NOTIFICATION_SETTINGS = "notification_settings"
    const val LOCATION_SETTINGS = "location_settings"
    const val PAYMENT_SETTINGS = "payment_settings"
    const val REPORT_ISSUE = "report_issue"
    const val TERMS_CONDITIONS = "terms_conditions"
    const val PRIVACY_POLICY = "privacy_policy"
    const val FAQ = "faq"
    const val ACCOUNT_MANAGEMENT = "account_management"
    const val ABOUT_FASTER = "about_faster"
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun NavGraph(
        navController: NavHostController = rememberNavController(),
        startDestination: String = Routes.LOGIN,
        authRepository: AuthRepository,
        sessionManager: EncryptedSessionManager
) {
    NavHost(navController = navController, startDestination = startDestination) {

        // Login Screen (placeholder)
        composable(Routes.LOGIN) {
            LoginScreen(
                    onPhoneClick = { navController.navigate(Routes.PHONE_LOGIN) },
                    onEmailClick = { navController.navigate(Routes.LOGIN_EMAIL) },
                    onSignUpClick = { navController.navigate(Routes.SIGNUP) }
            )
        }

        // Email Login
        composable(Routes.LOGIN_EMAIL) {
            val loginViewModel: com.faster.festival.ui.auth.login.LoginViewModel =
                    viewModel(
                            factory =
                                    com.faster.festival.ui.auth.login.LoginViewModel.Factory(
                                            authRepository
                                    )
                    )
            com.faster.festival.ui.auth.login.EmailLoginScreen(
                    viewModel = loginViewModel,
                    onForgotPassword = { navController.navigate(Routes.FORGOT_PASSWORD) },
                    onBackToSignup = { navController.navigate(Routes.SIGNUP) },
                    onCancel = {
                        // Navigate back to the main Auth screen
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(Routes.LOGIN) { inclusive = true }
                        }
                    },
                    onLoginSuccess = {
                        navController.navigate(Routes.HOME) {
                            popUpTo(Routes.LOGIN) { inclusive = true }
                        }
                    }
            )
        }

        // Signup
        composable(Routes.SIGNUP) {
            val viewModel: SignupViewModel =
                    viewModel(factory = SignupViewModel.Factory(authRepository))
            SignupScreen(
                    viewModel = viewModel,
                    onNavigateToVerification = { email, password, fullName ->
                        // Pass email, password, and fullName to OTP screen
                        navController.navigate("enter_code/$email/$password/$fullName") {
                            popUpTo(Routes.SIGNUP) { inclusive = true }
                        }
                    },
                    onBackClick = { navController.popBackStack() }
            )
        }

        // Check Email (OTP Waiting Screen)
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
                    onNavigateToEnterCode = { e ->
                        // Navigate with empty password/fullName since this is a resend flow
                        navController.navigate("enter_code/$e//")
                    },
                    onSkip = {
                        navController.navigate(Routes.HOME) {
                            popUpTo(Routes.SIGNUP) { inclusive = true }
                        }
                    },
                    onVerificationComplete = {
                        navController.navigate(Routes.HOME) {
                            popUpTo(Routes.SIGNUP) { inclusive = true }
                        }
                    }
            )
        }

        // Enter Code Screen (OTP input)
        composable(
                route = Routes.ENTER_CODE,
                arguments =
                        listOf(
                                androidx.navigation.navArgument("email") {
                                    type = androidx.navigation.NavType.StringType
                                },
                                androidx.navigation.navArgument("password") {
                                    type = androidx.navigation.NavType.StringType
                                    defaultValue = ""
                                },
                                androidx.navigation.navArgument("fullName") {
                                    type = androidx.navigation.NavType.StringType
                                    defaultValue = ""
                                }
                        )
        ) { backStackEntry ->
            val email = backStackEntry.arguments?.getString("email") ?: ""
            val password = backStackEntry.arguments?.getString("password") ?: ""
            val fullName = backStackEntry.arguments?.getString("fullName") ?: ""
            val otpViewModel: com.faster.festival.ui.auth.verification.OtpViewModel =
                    viewModel(
                            factory =
                                    com.faster.festival.ui.auth.verification.OtpViewModel.Factory(
                                            authRepository = authRepository
                                    )
                    )

            com.faster.festival.ui.auth.verification.OtpVerificationScreen(
                    email = email,
                    password = password, // Pass password from signup
                    fullName = fullName, // Pass fullName from signup
                    viewModel = otpViewModel,
                    onVerified = {
                        // Legacy callback - not used in current flow
                    },
                    onCancel = { navController.popBackStack() },
                    onProceedToOnboarding = {
                        // Navigate to onboarding flow (DateOfBirthScreen)
                        navController.navigate(Routes.ONBOARDING) {
                            popUpTo(Routes.SIGNUP) { inclusive = true }
                        }
                    }
            )
        }

        // Signup success screen
        composable(Routes.SIGNUP_SUCCESS) {
            VerificationSuccessScreen(
                    onContinue = {
                        navController.navigate(Routes.HOME) {
                            popUpTo(Routes.SIGNUP) { inclusive = true }
                        }
                    }
            )
        }

        // Onboarding flow (post-login)
        composable(Routes.ONBOARDING) {
            OnboardingScreen(
                    sessionManager = sessionManager,
                    onOnboardingComplete = {
                        navController.navigate(Routes.HOME) { popUpTo(0) { inclusive = true } }
                    }
            )
        }

        // Forgot Password
        composable(Routes.FORGOT_PASSWORD) {
            val vm: ForgotPasswordViewModel =
                    viewModel(factory = ForgotPasswordViewModel.Factory(authRepository))
            ForgotPasswordScreen(
                    viewModel = vm,
                    onBack = { navController.popBackStack() },
                    onSent = { email: String ->
                        // navigate to reset screen — token will be provided by email/deep link in
                        // real flow; here we navigate to reset without token for dev flow
                        navController.navigate("reset_password/$email/")
                    }
            )
        }

        // Reset Password with email and token
        composable(
                route = Routes.RESET_PASSWORD,
                arguments =
                        listOf(
                                androidx.navigation.navArgument("email") {
                                    type = androidx.navigation.NavType.StringType
                                },
                                androidx.navigation.navArgument("token") {
                                    type = androidx.navigation.NavType.StringType
                                }
                        )
        ) { backStackEntry ->
            val email = backStackEntry.arguments?.getString("email") ?: ""
            val token = backStackEntry.arguments?.getString("token") ?: ""
            val vm: ResetPasswordViewModel =
                    viewModel(factory = ResetPasswordViewModel.Factory(authRepository))
            ResetPasswordScreen(
                    email = email,
                    token = token,
                    viewModel = vm,
                    onBack = { navController.popBackStack() },
                    onResetSuccess = {
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(Routes.LOGIN) { inclusive = true }
                        }
                    }
            )
        }

        // Home Tab
        composable(Routes.HOME) {
            val accessToken = sessionManager.getAccessToken()
            // TODO: festivalSlug could be fetched from API config or deep link
            val festivalSlug = "floydfest-26" // Using default for now

            HomeScreen(
                    onTicketsClick = { navController.navigate(Routes.TICKETS) },
                    onFestivalHomeClick = { navController.navigate("web/festival_home") },
                    onFaqsClick = { navController.navigate("web/faqs") },
                    onDeepLink = { url ->
                        // Only navigate to known valid routes
                        val isValidRoute =
                                url.startsWith("artist/") ||
                                        url.startsWith("schedule") ||
                                        url.startsWith("web/") ||
                                        url.startsWith("tickets") ||
                                        url == Routes.MAP ||
                                        url == Routes.PROFILE

                        if (isValidRoute) {
                            try {
                                navController.navigate(url)
                            } catch (e: IllegalArgumentException) {
                                println("Navigation error for '$url': ${e.message}")
                            }
                        } else {
                            println("Invalid navigation route: $url - skipping navigation")
                        }
                    },
                    accessToken = accessToken,
                    festivalSlug = festivalSlug
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

        // Profile Tab - UPDATED WITH FULL NAVIGATION & API DATA
        composable(Routes.PROFILE) {
            val accessToken = sessionManager.getAccessToken() ?: return@composable

            // Load profile data from API using ViewModel
            val profileViewModel: EnhancedProfileViewModel =
                    viewModel(
                            factory =
                                    EnhancedProfileViewModel.createFactory(
                                            profileRepository =
                                                    com.faster.festival.di.NetworkModule
                                                            .profileRepository
                                    )
                    )

            // Trigger profile load on screen entry
            LaunchedEffect(accessToken) { profileViewModel.loadProfile(accessToken) }

            val profileState = profileViewModel.profileState.collectAsState()
            val fullName = profileViewModel.fullName.collectAsState()

            // Extract username from profile state if available
            val username = (profileState.value as? ProfileState.Success)?.profile?.username

            EnhancedProfileScreenWithNavigation(
                    accessToken = accessToken,
                    fullName = fullName.value, // ✅ From API via ViewModel
                    username = username, // ✅ From API response
                    onNavigateToPersonalInfo = { navController.navigate(Routes.PERSONAL_INFO) },
                    onNavigateToEditDemographics = {
                        navController.navigate(Routes.EDIT_DEMOGRAPHICS)
                    },
                    onNavigateToEmergencyContacts = {
                        navController.navigate(Routes.EMERGENCY_CONTACTS)
                    },
                    onNavigateToHealth = { navController.navigate(Routes.HEALTH_SETTINGS) },
                    onNavigateToNotifications = {
                        navController.navigate(Routes.NOTIFICATION_SETTINGS)
                    },
                    onNavigateToLocation = { navController.navigate(Routes.LOCATION_SETTINGS) },
                    onNavigateToPayments = { navController.navigate(Routes.PAYMENT_SETTINGS) },
                    onNavigateToReportIssue = { navController.navigate(Routes.REPORT_ISSUE) },
                    onNavigateToTerms = { navController.navigate(Routes.TERMS_CONDITIONS) },
                    onNavigateToPrivacy = { navController.navigate(Routes.PRIVACY_POLICY) },
                    onNavigateToFAQ = { navController.navigate(Routes.FAQ) },
                    onNavigateToManageAccount = {
                        navController.navigate(Routes.ACCOUNT_MANAGEMENT)
                    },
                    onNavigateToLogin = {
                        // ✅ Logout: Clear session and redirect immediately
                        // Call logout API in background without blocking navigation
                        sessionManager.clearSession()

                        // Call logout API asynchronously
                        kotlinx.coroutines.GlobalScope.launch {
                            try {
                                authRepository.logout(accessToken)
                            } catch (e: Exception) {
                                // Logout API failed, but user is already logged out locally
                                println("Logout API error: ${e.message}")
                            }
                        }

                        // Navigate to login screen immediately
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(Routes.HOME) { inclusive = true }
                        }
                    }
            )
        }

        // NEW DESTINATION ROUTES
        composable(Routes.PERSONAL_INFO) {
            PersonalInfoEditScreen(onBackClick = { navController.popBackStack() })
        }

        composable(Routes.EDIT_DEMOGRAPHICS) {
            DemographicsEditScreen(onBackClick = { navController.popBackStack() })
        }

        composable(Routes.EMERGENCY_CONTACTS) {
            EmergencyContactsScreen(onBackClick = { navController.popBackStack() })
        }

        composable(Routes.HEALTH_SETTINGS) {
            HealthSettingsScreen(onBackClick = { navController.popBackStack() })
        }

        composable(Routes.NOTIFICATION_SETTINGS) {
            NotificationSettingsScreen(onBackClick = { navController.popBackStack() })
        }

        composable(Routes.LOCATION_SETTINGS) {
            LocationSettingsScreen(onBackClick = { navController.popBackStack() })
        }

        composable(Routes.PAYMENT_SETTINGS) {
            PaymentSettingsScreen(onBackClick = { navController.popBackStack() })
        }

        composable(Routes.REPORT_ISSUE) {
            SupportTicketScreen(onBackClick = { navController.popBackStack() })
        }

        composable(Routes.TERMS_CONDITIONS) {
            TermsScreen(onBackClick = { navController.popBackStack() })
        }

        composable(Routes.PRIVACY_POLICY) {
            PrivacyPolicyScreen(onBackClick = { navController.popBackStack() })
        }

        composable(Routes.FAQ) { FAQScreen(onBackClick = { navController.popBackStack() }) }

        composable(Routes.ACCOUNT_MANAGEMENT) {
            AccountManagementScreen(onBackClick = { navController.popBackStack() })
        }

        composable(Routes.ABOUT_FASTER) {
            AboutFasterScreen(onBackClick = { navController.popBackStack() })
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
            ArtistDetailScreen(
                    artistId = android.net.Uri.decode(artistId),
                    onBackClick = { navController.popBackStack() }
            )
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

        // Phone Login
        composable(Routes.PHONE_LOGIN) {
            val vm: com.faster.festival.ui.auth.phone.PhoneLoginViewModel =
                    viewModel(
                            factory =
                                    com.faster.festival.ui.auth.phone.PhoneLoginViewModel.Factory(
                                            authRepository
                                    )
                    )
            com.faster.festival.ui.auth.phone.PhoneLoginScreen(
                    viewModel = vm,
                    onBack = { navController.popBackStack() },
                    onOtpSent = { phone -> navController.navigate("phone_otp/$phone") }
            )
        }

        // Phone OTP
        composable(
                route = Routes.PHONE_OTP,
                arguments =
                        listOf(
                                androidx.navigation.navArgument("phone") {
                                    type = androidx.navigation.NavType.StringType
                                }
                        )
        ) { backStackEntry ->
            val phone = backStackEntry.arguments?.getString("phone") ?: ""
            val vm: com.faster.festival.ui.auth.phone.PhoneOtpViewModel =
                    viewModel(
                            factory =
                                    com.faster.festival.ui.auth.phone.PhoneOtpViewModel.Factory(
                                            authRepository
                                    )
                    )
            com.faster.festival.ui.auth.phone.PhoneOtpScreen(
                    phone = phone,
                    viewModel = vm,
                    onBack = { navController.popBackStack() },
                    onVerified = {
                        // navigate to onboarding
                        navController.navigate("onboarding")
                    }
            )
        }
    }
}
