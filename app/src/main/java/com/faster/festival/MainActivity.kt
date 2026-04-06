package com.faster.festival

import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.faster.festival.data.local.EncryptedSessionManager
import com.faster.festival.data.repository.AuthRepository
import com.faster.festival.di.NetworkModule
import com.faster.festival.ui.navigation.NavGraph
import com.faster.festival.ui.navigation.Routes
import com.faster.festival.ui.theme.FastERTheme
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

        val sessionManager = EncryptedSessionManager(applicationContext)
        NetworkModule.initializeWithSessionManager(sessionManager)
        val authRepository = AuthRepository(NetworkModule.authApiService, sessionManager)

        @Suppress("NewApi")
        setContent {
            FastERTheme {
                FastERApp(
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
fun FastERApp(
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

    // Minimum splash display so logo + spinner are visible even when auth resolves instantly
    var splashMinElapsed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(1200)
        splashMinElapsed = true
    }

    val showSplash = !splashMinElapsed || startDestination == null

    if (showSplash) {
        SplashScreen()
    } else {
        val showBottomNav = currentRoute in bottomNavRoutes

        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                if (showBottomNav) {
                    FastERBottomNavBar(
                        currentRoute = currentRoute,
                        onNavigate = { route ->
                            navController.navigate(route) {
                                popUpTo(Routes.HOME) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        ) { paddingValues ->
            Box(modifier = Modifier.padding(paddingValues)) {
                NavGraph(
                    navController = navController,
                    startDestination = startDestination!!,
                    authRepository = authRepository,
                    sessionManager = sessionManager
                )
            }
        }
    }
}

@Composable
fun FastERBottomNavBar(
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

@Composable
fun SplashScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(id = R.drawable.faster_logo),
                contentDescription = "FASTER Logo",
                modifier = Modifier
                    .size(120.dp)
                    .clip(RoundedCornerShape(0.dp)),
                contentScale = ContentScale.Fit
            )

            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 32.dp)
            )
        }
    }
}
