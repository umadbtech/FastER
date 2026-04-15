package com.faster.festival

import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.faster.festival.data.local.EncryptedSessionManager
import com.faster.festival.data.repository.AuthRepository
import com.faster.festival.di.NetworkModule
import com.faster.festival.ui.navigation.NavGraph
import com.faster.festival.ui.navigation.Routes
import com.faster.festival.ui.theme.FASTERTheme
import kotlinx.coroutines.delay

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        enableEdgeToEdge()

        // Enable edge-to-edge with visible status bar
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val insetsController = WindowInsetsControllerCompat(window, window.decorView)
        insetsController.isAppearanceLightStatusBars = true

        // DI modules are initialized eagerly in FASTERApplication.onCreate().
        // We still construct a SessionManager instance here for AuthRepository.
        val sessionManager = EncryptedSessionManager(applicationContext)
        val authRepository = AuthRepository(NetworkModule.authApiService, sessionManager)

        @Suppress("NewApi")
        setContent {
            FASTERTheme {
                FASTERApp(
                    authRepository = authRepository,
                    sessionManager = sessionManager
                )
            }
        }
    }
}

private sealed class BottomNavTab(
    val route: String,
    val label: String
) {
    class IconTab(
        route: String,
        label: String,
        val icon: ImageVector
    ) : BottomNavTab(route, label)

    class ImageTab(
        route: String,
        label: String,
        val drawableRes: Int,
        val showBadge: Boolean
    ) : BottomNavTab(route, label)
}

private val bottomNavTabs = listOf(
    BottomNavTab.IconTab(
        route = Routes.HOME,
        label = "Home",
        icon = Icons.Default.Home
    ),
    BottomNavTab.IconTab(
        route = Routes.LINEUP,
        label = "Lineup",
        icon = Icons.Default.QueueMusic
    ),
    BottomNavTab.IconTab(
        route = Routes.MAP,
        label = "Map",
        icon = Icons.Default.Map
    ),
    BottomNavTab.IconTab(
        route = Routes.PROFILE,
        label = "Profile",
        icon = Icons.Default.Person
    ),
    BottomNavTab.ImageTab(
        route = Routes.FASTER_SCREEN,
        label = "FASTER",
        drawableRes = R.drawable.faster_red,
        showBadge = true
    )
)

private val bottomNavRoutes = bottomNavTabs.map { it.route }.toSet()

@Composable
@RequiresApi(Build.VERSION_CODES.O)
fun FASTERApp(
    authRepository: AuthRepository,
    sessionManager: EncryptedSessionManager
) {
    val navController = rememberNavController()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route

    val mainViewModel: MainViewModel = viewModel(
        factory = MainViewModel.Factory(sessionManager, NetworkModule.authApiService)
    )
    val startDestination by mainViewModel.startDestination.collectAsState()
    val landingDismissed by mainViewModel.landingDismissed.collectAsState()

    // 3-phase splash: 0 = logo, 1 = tagline, 2 = done (show app or events landing)
    var splashPhase by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        delay(3000)
        splashPhase = 1
        delay(2000)
        splashPhase = 2
    }

    val showSplash = splashPhase < 2 || startDestination == null

    if (showSplash) {
        SplashScreen(phase = splashPhase)
    } else if (startDestination == Routes.LOGIN && !landingDismissed) {
        EventsLandingScreen(
            onCreateAccount = { mainViewModel.dismissLanding() },
            onLearnMore = {}
        )
    } else {
        val showBottomNav = currentRoute in bottomNavRoutes

        val navigateToTab: (String) -> Unit = navigateToTab@{ route ->
            // No-op when the user taps the tab they are already on.
            // Without this guard, the popUpTo + restoreState combo can leave
            // the current destination's composable in a stale state.
            if (route == currentRoute) return@navigateToTab
            navController.navigate(route) {
                popUpTo(Routes.HOME) {
                    inclusive = false
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }

        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                if (showBottomNav) {
                    FASTERBottomNavBar(
                        currentRoute = currentRoute,
                        onNavigate = navigateToTab
                    )
                }
            }
        ) { paddingValues ->
            Box(modifier = Modifier.padding(paddingValues)) {
                NavGraph(
                    navController = navController,
                    startDestination = startDestination!!,
                    authRepository = authRepository,
                    sessionManager = sessionManager,
                    onNavigateToTab = navigateToTab
                )
            }
        }
    }
}

@Composable
fun FASTERBottomNavBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        bottomNavTabs.forEach { tab ->
            val selected = currentRoute == tab.route

            NavigationBarItem(
                icon = {
                    when (tab) {
                        is BottomNavTab.IconTab -> {
                            androidx.compose.material3.Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.label
                            )
                        }
                        is BottomNavTab.ImageTab -> {
                            BadgedBox(
                                badge = {
                                    if (tab.showBadge) {
                                        Badge(
                                            containerColor = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            ) {
                                Image(
                                    painter = painterResource(id = tab.drawableRes),
                                    contentDescription = tab.label,
                                    modifier = Modifier.size(24.dp),
                                    contentScale = ContentScale.Fit
                                )
                            }
                        }
                    }
                },
                label = { Text(tab.label) },
                selected = selected,
                onClick = { onNavigate(tab.route) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    indicatorColor = Color.Transparent
                )
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// Splash colors — matching wireframe deep navy
// ═══════════════════════════════════════════════════════════════════════════════
private val SplashNavyDeep = Color(0xFF050B26)
private val SplashNavy = Color(0xFF0A1A4D)
private val SplashNavyMid = Color(0xFF112B6E)
private val SplashGlow = Color(0xFF1E4FA8)
private val SplashRed = Color(0xFFD11818)

// ═══════════════════════════════════════════════════════════════════════════════
// 3-Phase Splash Screen
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
fun SplashScreen(phase: Int) {
    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = phase == 0,
            enter = fadeIn(tween(300)),
            exit = fadeOut(tween(500))
        ) {
            SplashPhaseOne()
        }

        AnimatedVisibility(
            visible = phase == 1,
            enter = fadeIn(tween(500)),
            exit = fadeOut(tween(500))
        ) {
            SplashPhaseTwo()
        }
    }
}

/**
 * Reusable navy splash background with radial glow.
 * Matches the FigJam wireframe — deep navy core with brighter blue edge glow.
 */
@Composable
private fun SplashBackground(content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            // Base vertical gradient: navy → deep navy
            .background(
                Brush.verticalGradient(
                    colors = listOf(SplashNavy, SplashNavyDeep)
                )
            )
    ) {
        // Radial glow overlay — soft blue light from center-bottom
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            SplashGlow.copy(alpha = 0.45f),
                            SplashNavyMid.copy(alpha = 0.25f),
                            Color.Transparent
                        ),
                        radius = 900f
                    )
                )
        )
        // Edge vignette for premium feel
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Transparent,
                            SplashNavyDeep.copy(alpha = 0.6f)
                        ),
                        radius = 1400f
                    )
                )
        )
        content()
    }
}

@Composable
private fun SplashPhaseOne() {
    SplashBackground {
        Image(
            painter = painterResource(id = R.drawable.faster_logo_white),
            contentDescription = "FASTER Logo",
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.55f),
            contentScale = ContentScale.Fit
        )
    }
}

@Composable
private fun SplashPhaseTwo() {
    // Animated fade-in for the tagline
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        visible = true
    }

    SplashBackground {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(800)) +
                    androidx.compose.animation.expandVertically(
                        animationSpec = tween(800),
                        expandFrom = Alignment.CenterVertically
                    ),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Text(
                text = "Technology for faster,\nsafer events",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// Events Landing Screen (unauthenticated users)
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
fun EventsLandingScreen(
    onCreateAccount: () -> Unit,
    onLearnMore: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            // Header row: FASTER logo + shield
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = R.drawable.faster_red),
                        contentDescription = "FASTER",
                        modifier = Modifier.size(32.dp),
                        contentScale = ContentScale.Fit
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "FASTER",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1A1A1A)
                    )
                }
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF16A34A)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Heading
            Text(
                text = "Events Powered By FASTER",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = SplashRed
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Choose the event you are attending to see the lineup, pair your wristband, and get help in an emergency.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF555555)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Event grid 2x2
            val events = listOf(
                "Event Name" to "April 23-25",
                "Event Name" to "April 23-25",
                "Event Name" to "April 23-25",
                "Event Name" to "April 23-25"
            )

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                for (i in events.indices step 2) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        EventCard(
                            name = events[i].first,
                            date = events[i].second,
                            modifier = Modifier.weight(1f)
                        )
                        if (i + 1 < events.size) {
                            EventCard(
                                name = events[i + 1].first,
                                date = events[i + 1].second,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Create an Account card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SplashNavy),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Create an Account",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Create an account to pair your wristband, enter the event, unlock all app features, and contactless payments.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = onCreateAccount,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SplashRed)
                    ) {
                        Text(
                            text = "Create an Account",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Learn More link
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Learn More About FASTER",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1A1A1A),
                    textDecoration = TextDecoration.Underline
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = Color(0xFF1A1A1A),
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Copyright
            Text(
                text = "\u00A9 FASTER EVENTS. All Rights Reserved.",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF999999),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun EventCard(
    name: String,
    date: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            // Image placeholder
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.4f)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF8B0000),
                                Color(0xFF1A1A1A)
                            )
                        )
                    )
            )
            // Event info
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp)
            ) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = date,
                    style = MaterialTheme.typography.labelSmall,
                    color = SplashRed
                )
            }
        }
    }
}
