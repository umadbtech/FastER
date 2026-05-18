package com.faster.festival.presentation.sos

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

/**
 * Drop-in Compose effect that requests `POST_NOTIFICATIONS` exactly once per
 * process on API 33+. No-op on API < 33 and when the permission is already
 * granted.
 *
 * Why this exists: the SOS foreground notification (see
 * [com.faster.festival.core.sos.SosNotifier]) silently skips posting when
 * `POST_NOTIFICATIONS` isn't granted. Without this prompt, users on Android
 * 13+ would never see SOS heads-up alerts unless they manually flipped the
 * permission in Settings.
 *
 * Mount this on screens where SOS is reachable — at minimum the FASTER tab
 * (manual trigger) and the wristband permissions screen (pair flow).
 *
 * The user is free to deny — the rest of the SOS pipeline still functions.
 */
@Composable
fun EnsureNotificationPermission() {
    if (Build.VERSION.SDK_INT < 33) return

    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* result is observed via the next composition's permission check */ }

    var asked by rememberSaveable { mutableStateOf(false) }
    val granted = remember {
        ContextCompat.checkSelfPermission(
            context, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    LaunchedEffect(asked, granted) {
        if (!granted && !asked) {
            asked = true
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
