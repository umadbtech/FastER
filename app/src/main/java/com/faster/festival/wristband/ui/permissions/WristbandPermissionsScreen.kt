package com.faster.festival.wristband.ui.permissions

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WristbandPermissionsScreen(
    onAllGranted: () -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current

    // Required to advance — BLE permissions only. Without these the wristband
    // can't pair at all.
    val requiredPerms = remember {
        if (Build.VERSION.SDK_INT >= 31)
            arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
        else arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
    }
    // Bundled in the same prompt for UX (one system dialog). Denying these
    // does NOT block pairing — the user just won't see SOS notifications.
    val bundledPerms = remember {
        buildList {
            addAll(requiredPerms)
            if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
        }.toTypedArray()
    }

    var granted by remember {
        mutableStateOf(requiredPerms.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        })
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        // Advance only if every REQUIRED permission was granted. Notification
        // permission is best-effort.
        granted = requiredPerms.all { perm -> result[perm] == true }
    }

    LaunchedEffect(granted) { if (granted) onAllGranted() }

    Scaffold(topBar = { CenterAlignedTopAppBar(title = { Text("Permissions") }) }) { padding ->
        Column(
            Modifier.padding(padding).padding(24.dp).fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("FastER needs Bluetooth", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
            Text(
                "We use Bluetooth to discover and stay connected to your wristband. " +
                        "Location is never collected."
            )
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = { launcher.launch(bundledPerms) },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Grant Permissions") }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = {
                val intent = Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:${context.packageName}")
                )
                context.startActivity(intent)
            }, modifier = Modifier.fillMaxWidth()) { Text("Open System Settings") }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
                Text("Not now")
            }
        }
    }
}
