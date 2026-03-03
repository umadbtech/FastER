package com.faster.festival

import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
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

        // Force light mode globally - prevents app from following system dark mode
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            enableEdgeToEdge()
        }

        val sessionManager = EncryptedSessionManager(applicationContext)
        val authRepository = AuthRepository(NetworkModule.authApiService, sessionManager)

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

@Composable
fun FastERApp(
    authRepository: AuthRepository,
    sessionManager: EncryptedSessionManager
) {
    var showSplash by remember { mutableStateOf(true) }
    val navController = rememberNavController()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route

    val mainViewModel: MainViewModel = viewModel(factory = MainViewModel.Factory(sessionManager))
    val startDestination by mainViewModel.startDestination.collectAsState()

    if (showSplash) {
        SplashScreen(onSplashFinished = { showSplash = false })
    } else if (startDestination == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        // Determine if we should show bottom nav
        val showBottomNav =
                currentRoute?.let {
                    it == Routes.HOME ||
                            it == Routes.MAP ||
                            it == Routes.SCHEDULE ||
                            it == Routes.PROFILE
                }
                        ?: false

        Scaffold(
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
        // Home
        NavigationBarItem(
                icon = { Icon(imageVector = Icons.Default.Home, contentDescription = stringResource(id = R.string.home)) },
                label = { Text(stringResource(id = R.string.home)) },
                selected = currentRoute == Routes.HOME,
                onClick = { onNavigate(Routes.HOME) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.inverseSurface,
                    selectedTextColor = MaterialTheme.colorScheme.inverseSurface,
                    indicatorColor = Color.White
                )
        )

        // Map
        NavigationBarItem(
                icon = { Icon(imageVector = Icons.Default.Map, contentDescription = stringResource(id = R.string.map)) },
                label = { Text(stringResource(id = R.string.map)) },
                selected = currentRoute == Routes.MAP,
                onClick = { onNavigate(Routes.MAP) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor =MaterialTheme.colorScheme.inverseSurface,
                    selectedTextColor =MaterialTheme.colorScheme.inverseSurface,
                    indicatorColor = Color.White
                )
        )

        // Schedule
        NavigationBarItem(
                icon = {
                    Icon(imageVector = Icons.Default.DateRange, contentDescription = "Schedule")
                },
                label = { Text(stringResource(id = R.string.schedule)) },
                selected = currentRoute == Routes.SCHEDULE,
                onClick = { onNavigate(Routes.SCHEDULE) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.inverseSurface,
                    selectedTextColor = MaterialTheme.colorScheme.inverseSurface,
                    indicatorColor = Color.White
                )
        )

        // Profile
        NavigationBarItem(
                icon = { Icon(imageVector = Icons.Default.Person, contentDescription = "Profile") },
                label = { Text(stringResource(id = R.string.profile)) },
                selected = currentRoute == Routes.PROFILE,
                onClick = { onNavigate(Routes.PROFILE) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor =MaterialTheme.colorScheme.inverseSurface,
                    selectedTextColor = MaterialTheme.colorScheme.inverseSurface,
                    indicatorColor = Color.White
                )
        )

        // Wristband
        NavigationBarItem(
            icon = {
                Image(
                    painter = painterResource(id = R.drawable.faster_red),
                    contentDescription = "FASTER Logo",
                    modifier = Modifier.size(24.dp),
                    contentScale = ContentScale.Fit
                )
            },
            label = { Text(stringResource(id = R.string.band)) },
            selected = currentRoute == Routes.PROFILE,
            onClick = { onNavigate(Routes.PROFILE) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.inverseSurface,
                selectedTextColor =MaterialTheme.colorScheme.inverseSurface,
                indicatorColor = Color.White
            )
        )

    }
}

@Composable
fun SplashScreen(onSplashFinished: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(1500) // Show splash for 1.5 seconds
        onSplashFinished()
    }
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
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.padding(top = 32.dp)
            )
        }
    }
}
