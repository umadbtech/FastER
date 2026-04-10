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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.faster.festival.ui.screens.FestivalDetailsScreen
import com.faster.festival.ui.screens.FestivalDetailsLoadingState
import com.faster.festival.ui.screens.FestivalDetailsErrorState
import com.faster.festival.ui.screens.UpcomingEventDetailScreen
import com.faster.festival.ui.screens.UpcomingEventDetailLoadingState
import com.faster.festival.ui.screens.UpcomingEventDetailErrorState
import com.faster.festival.ui.screens.FasterScreen
import com.faster.festival.ui.screens.HeroDetailScreen
import com.faster.festival.ui.screens.FriendsScreen
import com.faster.festival.ui.screens.HealthSettingsScreen
import com.faster.festival.ui.screens.HomeScreen
import com.faster.festival.ui.screens.LineupScreen
import com.faster.festival.ui.screens.ScheduleScreen
import com.faster.festival.ui.screens.LocationSettingsScreen
import com.faster.festival.ui.screens.LoginScreen
import com.faster.festival.ui.screens.MapScreen
import com.faster.festival.ui.screens.NotificationSettingsScreen
import com.faster.festival.ui.screens.PaymentSettingsScreen
import com.faster.festival.ui.screens.PersonalInfoEditScreen
import com.faster.festival.ui.screens.PrivacyPolicyScreen
import com.faster.festival.ui.screens.AnnouncementDetailScreen
import com.faster.festival.ui.screens.PromotionDetailScreen
import com.faster.festival.ui.screens.SponsorDetailScreen
import com.faster.festival.ui.screens.SupportTicketScreen
import com.faster.festival.ui.screens.TermsScreen
import com.faster.festival.ui.screens.TicketsScreen
import com.faster.festival.ui.screens.InAppWebViewScreen
import com.faster.festival.ui.screens.PinchHelpScreen
import com.faster.festival.ui.screens.ProvisionFlowScreen
import com.faster.festival.ui.viewmodel.PinchHelpViewModel
import com.faster.festival.ui.viewmodel.ProvisionViewModel
import com.faster.festival.di.NotificationModule
import com.faster.festival.di.PinchModule
import com.faster.festival.ui.viewmodel.NotificationSettingsViewModel
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
    const val ANNOUNCEMENT_DETAIL = "announcement/{announcementId}"
    const val PROMOTION_DETAIL = "promotion/{promotionId}"
    const val SPONSOR_DETAIL = "sponsor/{sponsorId}"
    const val HERO_DETAIL = "hero/{heroItemId}"
    const val FESTIVAL_DETAILS = "festival_details/{festivalSlug}"
    const val UPCOMING_EVENT_DETAIL = "upcoming_event/{eventId}"
    const val STAGE_SCHEDULE = "stage_schedule"
    const val IN_APP_WEB = "in_app_web/{url}/{title}"
    const val PINCH_HELP = "pinch_help"
    const val PROVISION_FLOW = "provision_flow"
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
                onHeroItemClick = { heroItemId ->
                    val encoded = android.net.Uri.encode(heroItemId)
                    navController.navigate("hero/$encoded")
                },
                onPromotionClick = { promotionId ->
                    val encoded = android.net.Uri.encode(promotionId)
                    navController.navigate("promotion/$encoded")
                },
                onSponsorClick = { sponsorId ->
                    val encoded = android.net.Uri.encode(sponsorId)
                    navController.navigate("sponsor/$encoded")
                },
                onAnnouncementClick = { announcementId ->
                    val encoded = android.net.Uri.encode(announcementId)
                    navController.navigate("announcement/$encoded")
                },
                onNavigateToSchedule = { navController.navigate(Routes.STAGE_SCHEDULE) },
                onNavigateToMap = { navController.navigate(Routes.MAP) },
                onNavigateToFAQ = { navController.navigate(Routes.FAQ) },
                onFestivalBannerClick = { festivalSlug ->
                    val encoded = android.net.Uri.encode(festivalSlug)
                    navController.navigate("festival_details/$encoded")
                },
                onUpcomingEventClick = { eventId ->
                    val encoded = android.net.Uri.encode(eventId)
                    navController.navigate("upcoming_event/$encoded")
                },
                sessionManager = sessionManager
            )
        }

        composable(Routes.LINEUP) {
            LineupScreen(
                onArtistClick = { artistSlug ->
                    val encoded = android.net.Uri.encode(artistSlug)
                    navController.navigate("artist/$encoded")
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
            FasterScreen(
                onPinchHelp = { navController.navigate(Routes.PINCH_HELP) },
                onPairWristband = { navController.navigate(Routes.PROVISION_FLOW) }
            )
        }

        composable(Routes.PINCH_HELP) {
            val pinchViewModel: PinchHelpViewModel = viewModel(
                factory = PinchHelpViewModel.Factory(
                    emergencyRepository = PinchModule.emergencyRepository,
                    feedbackRepository = PinchModule.feedbackRepository
                )
            )
            PinchHelpScreen(
                viewModel = pinchViewModel,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(Routes.PROVISION_FLOW) {
            val provisionViewModel: ProvisionViewModel = viewModel()
            ProvisionFlowScreen(
                viewModel = provisionViewModel,
                onBackClick = { navController.popBackStack() },
                onComplete = {
                    navController.popBackStack()
                },
                showBackOnSplash = true,
                completeButtonText = "Go Home"
            )
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
                onNavigateToEmergencyContacts = { navController.navigate(Routes.EMERGENCY_CONTACTS) },
                onNavigateToUploadAvatar = { navController.navigate(Routes.AVATAR_UPLOAD) },
                onNavigateToHealth = { navController.navigate(Routes.HEALTH_SETTINGS) },
                onNavigateToNotifications = { navController.navigate(Routes.NOTIFICATION_SETTINGS) },
                onNavigateToLocation = { navController.navigate(Routes.LOCATION_SETTINGS) },
                onNavigateToPayments = { navController.navigate(Routes.PAYMENT_SETTINGS) },
                onNavigateToFriends = { navController.navigate(Routes.FRIENDS) },
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
            val notifViewModel: NotificationSettingsViewModel = viewModel(
                factory = NotificationSettingsViewModel.Factory(
                    repository = NotificationModule.repository,
                    fcmTokenRegistrar = NotificationModule.fcmTokenRegistrar
                )
            )
            NotificationSettingsScreen(
                viewModel = notifViewModel,
                onBackClick = { navController.popBackStack() }
            )
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

        composable(Routes.STAGE_SCHEDULE) {
            ScheduleScreen(
                onBackClick = { navController.popBackStack() },
                onArtistClick = { artistId ->
                    val encoded = android.net.Uri.encode(artistId)
                    navController.navigate("artist/$encoded")
                }
            )
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

        composable(
            route = Routes.ANNOUNCEMENT_DETAIL,
            arguments = listOf(
                navArgument("announcementId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val announcementId = backStackEntry.arguments?.getString("announcementId") ?: return@composable
            val decodedId = android.net.Uri.decode(announcementId)

            val announcementViewModel: com.faster.festival.ui.viewmodel.AnnouncementDetailViewModel = viewModel(
                factory = com.faster.festival.ui.viewmodel.AnnouncementDetailViewModel.Factory(
                    appHomeApi = NetworkModule.appHomeApi,
                    festivalSlug = AppConfig.DEFAULT_FESTIVAL_SLUG,
                    announcementId = decodedId
                )
            )
            val announcementState = announcementViewModel.state.collectAsState()

            when (val s = announcementState.value) {
                is com.faster.festival.ui.viewmodel.AnnouncementDetailState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                is com.faster.festival.ui.viewmodel.AnnouncementDetailState.Success -> {
                    AnnouncementDetailScreen(
                        announcement = s.announcement,
                        onBackClick = { navController.popBackStack() }
                    )
                }
                is com.faster.festival.ui.viewmodel.AnnouncementDetailState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(s.message)
                    }
                }
            }
        }

        composable(
            route = Routes.PROMOTION_DETAIL,
            arguments = listOf(
                navArgument("promotionId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val promotionId = backStackEntry.arguments?.getString("promotionId") ?: return@composable
            val decodedId = android.net.Uri.decode(promotionId)

            val promoViewModel: com.faster.festival.ui.viewmodel.PromotionDetailViewModel = viewModel(
                factory = com.faster.festival.ui.viewmodel.PromotionDetailViewModel.Factory(
                    appHomeApi = NetworkModule.appHomeApi,
                    festivalSlug = AppConfig.DEFAULT_FESTIVAL_SLUG,
                    promotionId = decodedId
                )
            )
            val state = promoViewModel.promotionState.collectAsState()

            when (val s = state.value) {
                is com.faster.festival.ui.viewmodel.PromotionDetailState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                is com.faster.festival.ui.viewmodel.PromotionDetailState.Success -> {
                    PromotionDetailScreen(
                        promotion = s.promotion,
                        onBackClick = { navController.popBackStack() }
                    )
                }
                is com.faster.festival.ui.viewmodel.PromotionDetailState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(s.message)
                    }
                }
            }
        }

        composable(
            route = Routes.SPONSOR_DETAIL,
            arguments = listOf(
                navArgument("sponsorId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val sponsorId = backStackEntry.arguments?.getString("sponsorId") ?: return@composable
            val decodedId = android.net.Uri.decode(sponsorId)

            val sponsorViewModel: com.faster.festival.ui.viewmodel.SponsorDetailViewModel = viewModel(
                factory = com.faster.festival.ui.viewmodel.SponsorDetailViewModel.Factory(
                    appHomeApi = NetworkModule.appHomeApi,
                    festivalSlug = AppConfig.DEFAULT_FESTIVAL_SLUG,
                    sponsorId = decodedId
                )
            )
            val state = sponsorViewModel.sponsorState.collectAsState()

            when (val s = state.value) {
                is com.faster.festival.ui.viewmodel.SponsorDetailState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                is com.faster.festival.ui.viewmodel.SponsorDetailState.Success -> {
                    SponsorDetailScreen(
                        sponsor = s.sponsor,
                        onBackClick = { navController.popBackStack() }
                    )
                }
                is com.faster.festival.ui.viewmodel.SponsorDetailState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(s.message)
                    }
                }
            }
        }

        composable(
            route = Routes.HERO_DETAIL,
            arguments = listOf(
                navArgument("heroItemId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val heroItemId = backStackEntry.arguments?.getString("heroItemId") ?: return@composable
            val decodedId = android.net.Uri.decode(heroItemId)

            val heroViewModel: com.faster.festival.ui.viewmodel.HeroDetailViewModel = viewModel(
                factory = com.faster.festival.ui.viewmodel.HeroDetailViewModel.Factory(
                    appHomeApi = NetworkModule.appHomeApi,
                    festivalSlug = AppConfig.DEFAULT_FESTIVAL_SLUG,
                    heroItemId = decodedId
                )
            )
            val state = heroViewModel.heroState.collectAsState()

            when (val s = state.value) {
                is com.faster.festival.ui.viewmodel.HeroDetailState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                is com.faster.festival.ui.viewmodel.HeroDetailState.Success -> {
                    HeroDetailScreen(
                        heroItem = s.heroItem,
                        onBackClick = { navController.popBackStack() },
                        onArtistClick = { artistSlug ->
                            val encoded = android.net.Uri.encode(artistSlug)
                            navController.navigate("artist/$encoded")
                        }
                    )
                }
                is com.faster.festival.ui.viewmodel.HeroDetailState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(s.message)
                    }
                }
            }
        }

        composable(
            route = Routes.FESTIVAL_DETAILS,
            arguments = listOf(
                navArgument("festivalSlug") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val slug = backStackEntry.arguments?.getString("festivalSlug") ?: return@composable
            val decodedSlug = android.net.Uri.decode(slug)

            val festivalViewModel: com.faster.festival.ui.viewmodel.FestivalDetailsViewModel = viewModel(
                factory = com.faster.festival.ui.viewmodel.FestivalDetailsViewModel.Factory(
                    appHomeApi = NetworkModule.appHomeApi,
                    festivalSlug = decodedSlug
                )
            )
            val state = festivalViewModel.state.collectAsState()

            when (val s = state.value) {
                is com.faster.festival.ui.viewmodel.FestivalDetailsState.Loading -> {
                    FestivalDetailsLoadingState()
                }
                is com.faster.festival.ui.viewmodel.FestivalDetailsState.Success -> {
                    FestivalDetailsScreen(
                        festival = s.festival,
                        bannerUrls = s.bannerUrls,
                        location = s.location,
                        onBackClick = { navController.popBackStack() }
                    )
                }
                is com.faster.festival.ui.viewmodel.FestivalDetailsState.Error -> {
                    FestivalDetailsErrorState(
                        message = s.message,
                        onBackClick = { navController.popBackStack() }
                    )
                }
            }
        }

        composable(
            route = Routes.UPCOMING_EVENT_DETAIL,
            arguments = listOf(
                navArgument("eventId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val eventId = backStackEntry.arguments?.getString("eventId") ?: return@composable
            val decodedId = android.net.Uri.decode(eventId)

            val eventViewModel: com.faster.festival.ui.viewmodel.UpcomingEventDetailViewModel = viewModel(
                factory = com.faster.festival.ui.viewmodel.UpcomingEventDetailViewModel.Factory(
                    appHomeApi = NetworkModule.appHomeApi,
                    festivalSlug = AppConfig.DEFAULT_FESTIVAL_SLUG,
                    eventId = decodedId
                )
            )
            val state = eventViewModel.state.collectAsState()

            when (val s = state.value) {
                is com.faster.festival.ui.viewmodel.UpcomingEventDetailState.Loading -> {
                    UpcomingEventDetailLoadingState()
                }
                is com.faster.festival.ui.viewmodel.UpcomingEventDetailState.Success -> {
                    UpcomingEventDetailScreen(
                        event = s.event,
                        onBackClick = { navController.popBackStack() }
                    )
                }
                is com.faster.festival.ui.viewmodel.UpcomingEventDetailState.Error -> {
                    UpcomingEventDetailErrorState(
                        message = s.message,
                        onBackClick = { navController.popBackStack() }
                    )
                }
            }
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

        composable(
            route = Routes.IN_APP_WEB,
            arguments = listOf(
                navArgument("url") { type = NavType.StringType },
                navArgument("title") { type = NavType.StringType; defaultValue = "Website" }
            )
        ) { backStackEntry ->
            val url = android.net.Uri.decode(backStackEntry.arguments?.getString("url") ?: "")
            val title = android.net.Uri.decode(backStackEntry.arguments?.getString("title") ?: "Website")
            InAppWebViewScreen(
                url = url,
                title = title,
                onClose = { navController.popBackStack() }
            )
        }
    }
}
