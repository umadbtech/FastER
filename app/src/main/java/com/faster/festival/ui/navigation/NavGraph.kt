package com.faster.festival.ui.navigation

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.faster.festival.AppConfig
import com.faster.festival.data.local.EncryptedSessionManager
import com.faster.festival.data.repository.AuthRepository
import com.faster.festival.di.NetworkModule
import com.faster.festival.ui.auth.forgot.ForgotPasswordScreen
import com.faster.festival.ui.auth.forgot.ForgotPasswordViewModel
import com.faster.festival.ui.auth.login.EmailLoginScreen
import com.faster.festival.ui.auth.login.LoginViewModel
import com.faster.festival.ui.auth.phone.PhoneLoginScreen
import com.faster.festival.ui.auth.phone.PhoneLoginViewModel
import com.faster.festival.ui.auth.phone.PhoneOtpScreen
import com.faster.festival.ui.auth.phone.PhoneOtpViewModel
import com.faster.festival.ui.auth.reset.ResetPasswordScreen
import com.faster.festival.ui.auth.reset.ResetPasswordViewModel
import com.faster.festival.ui.auth.signup.SignupScreen
import com.faster.festival.ui.auth.signup.SignupViewModel
import com.faster.festival.ui.auth.verification.CheckEmailScreen
import com.faster.festival.ui.auth.verification.OtpVerificationScreen
import com.faster.festival.ui.auth.verification.OtpViewModel
import com.faster.festival.ui.auth.verification.VerificationSuccessScreen
import com.faster.festival.ui.onboarding.OnboardingScreen
import com.faster.festival.ui.screens.AboutFasterScreen
import com.faster.festival.ui.screens.AccountManagementScreen
import com.faster.festival.ui.screens.ArtistDetailScreen
import com.faster.festival.ui.screens.AvatarUploadScreen
import com.faster.festival.ui.screens.DemographicsEditScreen
import com.faster.festival.ui.screens.EmergencyContactsScreen
import com.faster.festival.ui.screens.EnhancedProfileScreenWithNavigation
import com.faster.festival.ui.screens.FAQScreen
import com.faster.festival.ui.screens.FasterScreen
import com.faster.festival.ui.screens.FriendsScreen
import com.faster.festival.ui.screens.HealthSettingsScreen
import com.faster.festival.ui.screens.HomeScreen
import com.faster.festival.ui.screens.LineupScreen
import com.faster.festival.ui.screens.LocationSettingsScreen
import com.faster.festival.ui.screens.LoginScreen
import com.faster.festival.ui.screens.MapScreen
import com.faster.festival.ui.screens.NotificationSettingsScreen
import com.faster.festival.ui.screens.PaymentSettingsScreen
import com.faster.festival.ui.screens.PersonalInfoEditScreen
import com.faster.festival.ui.screens.PrivacyPolicyScreen
import com.faster.festival.ui.screens.SupportTicketScreen
import com.faster.festival.ui.screens.TermsScreen
import com.faster.festival.ui.screens.TicketsScreen
import com.faster.festival.ui.screens.WebPlaceholderScreen
import com.faster.festival.ui.viewmodel.EnhancedProfileViewModel
import com.faster.festival.ui.viewmodel.ProfileState
import kotlinx.coroutines.launch

object Routes {
    const val LOGIN = "login"
    const val LOGIN_EMAIL = "login/email"
    const val SIGNUP = "signup"
    const val CHECK_EMAIL = "otp/{email}"
    const val ENTER_CODE = "enter_code/{email}/{password}/{fullName}"
    const val SIGNUP_SUCCESS = "signup_success"
    const val FORGOT_PASSWORD = "forgot_password"
    const val RESET_PASSWORD = "reset_password/{email}/{token}"
    const val PHONE_LOGIN = "phone_login"
    const val PHONE_OTP = "phone_otp/{phone}"
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val LINEUP = "lineup"
    const val MAP = "map"
    const val FRIENDS = "friends"
    const val FASTER_SCREEN = "faster"
    const val PROFILE = "profile"
    const val ARTIST_DETAIL = "artist/{artistId}"
    const val TICKETS = "tickets"
    const val WEB = "web/{type}"
    const val PERSONAL_INFO = "personal_info"
    const val AVATAR_UPLOAD = "avatar_upload"
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

        // =====================================================================
        // AUTH ROUTES
        // =====================================================================

        composable(Routes.LOGIN) {
            LoginScreen(
                onPhoneClick = { navController.navigate(Routes.PHONE_LOGIN) },
                onEmailClick = { navController.navigate(Routes.LOGIN_EMAIL) },
                onSignUpClick = { navController.navigate(Routes.SIGNUP) }
            )
        }

        composable(Routes.LOGIN_EMAIL) {
            val loginViewModel: LoginViewModel =
                viewModel(factory = LoginViewModel.Factory(authRepository))
            EmailLoginScreen(
                viewModel = loginViewModel,
                onForgotPassword = { navController.navigate(Routes.FORGOT_PASSWORD) },
                onBackToSignup = { navController.navigate(Routes.SIGNUP) },
                onCancel = {
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

        composable(Routes.SIGNUP) {
            val viewModel: SignupViewModel =
                viewModel(factory = SignupViewModel.Factory(authRepository))
            SignupScreen(
                viewModel = viewModel,
                onNavigateToVerification = { email, password, fullName ->
                    navController.navigate("enter_code/$email/$password/$fullName") {
                        popUpTo(Routes.SIGNUP) { inclusive = true }
                    }
                },
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.CHECK_EMAIL,
            arguments = listOf(
                navArgument("email") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val email = backStackEntry.arguments?.getString("email") ?: ""
            CheckEmailScreen(
                email = email,
                sessionManager = sessionManager,
                onNavigateToEnterCode = { e ->
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

        composable(
            route = Routes.ENTER_CODE,
            arguments = listOf(
                navArgument("email") { type = NavType.StringType },
                navArgument("password") {
                    type = NavType.StringType
                    defaultValue = ""
                },
                navArgument("fullName") {
                    type = NavType.StringType
                    defaultValue = ""
                }
            )
        ) { backStackEntry ->
            val email = backStackEntry.arguments?.getString("email") ?: ""
            val password = backStackEntry.arguments?.getString("password") ?: ""
            val fullName = backStackEntry.arguments?.getString("fullName") ?: ""
            val otpViewModel: OtpViewModel =
                viewModel(factory = OtpViewModel.Factory(authRepository = authRepository))

            OtpVerificationScreen(
                email = email,
                password = password,
                fullName = fullName,
                viewModel = otpViewModel,
                onVerified = { },
                onCancel = { navController.popBackStack() },
                onProceedToOnboarding = {
                    navController.navigate(Routes.ONBOARDING) {
                        popUpTo(Routes.SIGNUP) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.SIGNUP_SUCCESS) {
            VerificationSuccessScreen(
                onContinue = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.SIGNUP) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.FORGOT_PASSWORD) {
            val vm: ForgotPasswordViewModel =
                viewModel(factory = ForgotPasswordViewModel.Factory(authRepository))
            ForgotPasswordScreen(
                viewModel = vm,
                onBack = { navController.popBackStack() },
                onSent = { email: String ->
                    navController.navigate("reset_password/$email/")
                }
            )
        }

        composable(
            route = Routes.RESET_PASSWORD,
            arguments = listOf(
                navArgument("email") { type = NavType.StringType },
                navArgument("token") { type = NavType.StringType }
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

        composable(Routes.PHONE_LOGIN) {
            val vm: PhoneLoginViewModel =
                viewModel(factory = PhoneLoginViewModel.Factory(authRepository))
            PhoneLoginScreen(
                viewModel = vm,
                onBack = { navController.popBackStack() },
                onOtpSent = { phone -> navController.navigate("phone_otp/$phone") }
            )
        }

        composable(
            route = Routes.PHONE_OTP,
            arguments = listOf(
                navArgument("phone") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val phone = backStackEntry.arguments?.getString("phone") ?: ""
            val vm: PhoneOtpViewModel =
                viewModel(factory = PhoneOtpViewModel.Factory(authRepository))
            PhoneOtpScreen(
                phone = phone,
                viewModel = vm,
                onBack = { navController.popBackStack() },
                onVerified = {
                    navController.navigate(Routes.ONBOARDING) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                }
            )
        }

        // =====================================================================
        // ONBOARDING
        // =====================================================================

        composable(Routes.ONBOARDING) {
            OnboardingScreen(
                sessionManager = sessionManager,
                onOnboardingComplete = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // =====================================================================
        // MAIN TABS
        // =====================================================================

        composable(Routes.HOME) {
            HomeScreen(
                onArtistClick = { artistId ->
                    navController.navigate("artist/$artistId")
                },
                onNavigateToSchedule = { navController.navigate(Routes.LINEUP) },
                onNavigateToMap = { navController.navigate(Routes.MAP) },
                sessionManager = sessionManager
            )
        }

        composable(Routes.LINEUP) {
            LineupScreen(
                onArtistClick = { artistId ->
                    navController.navigate("artist/$artistId")
                }
            )
        }

        composable(Routes.MAP) {
            MapScreen()
        }

        composable(Routes.FRIENDS) {
            FriendsScreen()
        }

        composable(Routes.FASTER_SCREEN) {
            FasterScreen()
        }

        // =====================================================================
        // PROFILE
        // =====================================================================

        composable(Routes.PROFILE) {
            val accessToken = sessionManager.getAccessToken() ?: return@composable

            val profileViewModel: EnhancedProfileViewModel =
                viewModel(
                    factory = EnhancedProfileViewModel.createFactory(
                        profileRepository = NetworkModule.profileRepository
                    )
                )

            LaunchedEffect(accessToken) { profileViewModel.loadProfile(accessToken) }

            val profileState = profileViewModel.profileState.collectAsState()
            val fullName = profileViewModel.fullName.collectAsState()
            val avatarUrl = profileViewModel.avatarUrl.collectAsState()
            val username = (profileState.value as? ProfileState.Success)?.profile?.username

            EnhancedProfileScreenWithNavigation(
                accessToken = accessToken,
                fullName = fullName.value,
                username = username,
                avatarUrl = avatarUrl.value,
                onNavigateToPersonalInfo = { navController.navigate(Routes.PERSONAL_INFO) },
                onNavigateToEditDemographics = { navController.navigate(Routes.EDIT_DEMOGRAPHICS) },
                onNavigateToEmergencyContacts = { navController.navigate(Routes.EMERGENCY_CONTACTS) },
                onNavigateToUploadAvatar = { navController.navigate(Routes.AVATAR_UPLOAD) },
                onNavigateToHealth = { navController.navigate(Routes.HEALTH_SETTINGS) },
                onNavigateToNotifications = { navController.navigate(Routes.NOTIFICATION_SETTINGS) },
                onNavigateToLocation = { navController.navigate(Routes.LOCATION_SETTINGS) },
                onNavigateToPayments = { navController.navigate(Routes.PAYMENT_SETTINGS) },
                onNavigateToReportIssue = { navController.navigate(Routes.REPORT_ISSUE) },
                onNavigateToTerms = { navController.navigate(Routes.TERMS_CONDITIONS) },
                onNavigateToPrivacy = { navController.navigate(Routes.PRIVACY_POLICY) },
                onNavigateToFAQ = { navController.navigate(Routes.FAQ) },
                onNavigateToManageAccount = { navController.navigate(Routes.ACCOUNT_MANAGEMENT) },
                onNavigateToLogin = {
                    sessionManager.clearSession()
                    kotlinx.coroutines.GlobalScope.launch {
                        try {
                            authRepository.logout(accessToken)
                        } catch (e: Exception) {
                            println("Logout API error: ${e.message}")
                        }
                    }
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.HOME) { inclusive = true }
                    }
                },
                onRefreshProfile = {
                    profileViewModel.loadProfile(accessToken)
                }
            )
        }

        // =====================================================================
        // PROFILE SUB-SCREENS
        // =====================================================================

        composable(Routes.PERSONAL_INFO) {
            PersonalInfoEditScreen(onBackClick = { navController.popBackStack() })
        }

        composable(Routes.AVATAR_UPLOAD) {
            val accessToken = sessionManager.getAccessToken() ?: return@composable
            val profileViewModel: EnhancedProfileViewModel =
                viewModel(
                    factory = EnhancedProfileViewModel.createFactory(
                        profileRepository = NetworkModule.profileRepository
                    )
                )

            val profileState = profileViewModel.profileState.collectAsState()
            val currentAvatarUrl = (profileState.value as? ProfileState.Success)?.profile?.avatarUrl

            AvatarUploadScreen(
                currentAvatarUrl = currentAvatarUrl,
                userName = (profileState.value as? ProfileState.Success)?.profile?.username,
                onBackClick = { navController.popBackStack() },
                onUploadSuccess = {
                    navController.popBackStack()
                    profileViewModel.loadProfile(accessToken)
                }
            )
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

        composable(Routes.FAQ) {
            FAQScreen(onBackClick = { navController.popBackStack() })
        }

        composable(Routes.ACCOUNT_MANAGEMENT) {
            AccountManagementScreen(onBackClick = { navController.popBackStack() })
        }

        composable(Routes.ABOUT_FASTER) {
            AboutFasterScreen(onBackClick = { navController.popBackStack() })
        }

        // =====================================================================
        // DETAIL SCREENS
        // =====================================================================

        composable(
            route = Routes.ARTIST_DETAIL,
            arguments = listOf(
                navArgument("artistId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val artistId = backStackEntry.arguments?.getString("artistId") ?: return@composable
            ArtistDetailScreen(
                artistId = android.net.Uri.decode(artistId),
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(Routes.TICKETS) {
            TicketsScreen(onBackClick = { navController.popBackStack() })
        }

        composable(
            route = Routes.WEB,
            arguments = listOf(
                navArgument("type") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val type = backStackEntry.arguments?.getString("type") ?: "Info"
            val title = when (type) {
                "festival_home" -> "Festival Home"
                "faqs" -> "FAQs"
                else -> type
            }
            WebPlaceholderScreen(title = title, onBackClick = { navController.popBackStack() })
        }
    }
}
