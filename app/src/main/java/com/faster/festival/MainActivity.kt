package com.faster.festival

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.faster.festival.data.local.EncryptedSessionManager
import com.faster.festival.data.repository.AuthRepository
import com.faster.festival.di.NetworkModule
import com.faster.festival.ui.navigation.NavGraph
import com.faster.festival.ui.navigation.Routes
import com.faster.festival.ui.theme.FastERTheme
import androidx.compose.ui.res.stringResource
import com.faster.festival.R

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
    val navController = rememberNavController()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route

    val mainViewModel: MainViewModel = viewModel(factory = MainViewModel.Factory(sessionManager))
    val startDestination by mainViewModel.startDestination.collectAsState()

    if (startDestination == null) {
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
                onClick = { onNavigate(Routes.HOME) }
        )

        // Map
        NavigationBarItem(
                icon = { Icon(imageVector = Icons.Default.Map, contentDescription = stringResource(id = R.string.map)) },
                label = { Text(stringResource(id = R.string.map)) },
                selected = currentRoute == Routes.MAP,
                onClick = { onNavigate(Routes.MAP) }
        )

        // Schedule
        NavigationBarItem(
                icon = {
                    Icon(imageVector = Icons.Default.DateRange, contentDescription = "Schedule")
                },
                label = { Text(stringResource(id = R.string.schedule)) },
                selected = currentRoute == Routes.SCHEDULE,
                onClick = { onNavigate(Routes.SCHEDULE) }
        )

        // Profile
        NavigationBarItem(
                icon = { Icon(imageVector = Icons.Default.Person, contentDescription = "Profile") },
                label = { Text(stringResource(id = R.string.profile)) },
                selected = currentRoute == Routes.PROFILE,
                onClick = { onNavigate(Routes.PROFILE) }
        )
    }
}
