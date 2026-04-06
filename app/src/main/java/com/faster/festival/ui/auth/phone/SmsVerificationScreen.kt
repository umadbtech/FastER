package com.faster.festival.ui.auth.phone

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import com.faster.festival.R
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * SmsVerificationScreen: segmented 6-digit OTP input with auto-advance and paste support.
 * Callbacks:
 *  - onOtpComplete(otp)
 *  - onResend()
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmsVerificationScreen(
    phone: String,
    viewModel: SmsVerificationViewModel,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    onOtpComplete: (String) -> Unit = {},
    onResend: () -> Unit = {},
    onUseEmail: () -> Unit = {}
) {
    val formOtp by viewModel.otp.collectAsState()
    val isComplete by viewModel.isOtpComplete.collectAsState()
    val error by viewModel.errorState.collectAsState()
    val seconds by viewModel.resendSeconds.collectAsState()

    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()

    val otpDigits = remember { List(6) { mutableStateOf("") } }
    val focusRequesters = remember { List(6) { FocusRequester() } }

    val snackbarHostState = remember { SnackbarHostState() }
    var showTroubleshoot by remember { mutableStateOf(false) }

    LaunchedEffect(isComplete) {
        if (isComplete) {
            onOtpComplete(formOtp)
        }
    }

    // show transient snackbar when error changes
    LaunchedEffect(error) {
        error?.let { err ->
            val duration = SnackbarDuration.Short
            snackbarHostState.showSnackbar(err.message, duration = duration)
        }
    }

    // helper to choose banner color by severity (must be @Composable because it reads MaterialTheme and resources)
    @Composable
    fun bannerColors(severity: ErrorSeverity): Pair<Color, Color> {
        return when (severity) {
            ErrorSeverity.CRITICAL -> Pair(MaterialTheme.colorScheme.error, MaterialTheme.colorScheme.onError)
            ErrorSeverity.WARNING -> Pair(colorResource(id = R.color.warning_amber), MaterialTheme.colorScheme.onBackground)
            ErrorSeverity.INFO -> Pair(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.onPrimary)
        }
    }

    val verifyTitle = stringResource(id = R.string.verify_code_title)
    val backDesc = stringResource(id = R.string.back)
    val weSentText = stringResource(id = R.string.we_sent_code, phone)
    val resendText = stringResource(id = R.string.resend)
    val resendInFormat = stringResource(id = R.string.resend_in, seconds)
    val verifyButtonLabel = stringResource(id = R.string.verify)
    val troubleshootLabel = stringResource(id = R.string.troubleshoot)
    val useEmailLabel = stringResource(id = R.string.use_email_instead)
    val retryLabel = stringResource(id = R.string.retry)

    Scaffold(topBar = {
        TopAppBar(title = { Text(verifyTitle) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = backDesc) } })
    }, snackbarHost = { SnackbarHost(hostState = snackbarHostState) }) { padding ->
        Column(modifier = Modifier
            .padding(padding)
            .padding(24.dp)
            .fillMaxSize()
            .animateContentSize(), horizontalAlignment = Alignment.CenterHorizontally) {

            // Error banner (persistent) + quick actions
            error?.let { err ->
                val (bg, fg) = bannerColors(err.severity)
                Surface(color = bg, tonalElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(text = err.message, color = fg)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Row {
                                if (err.canRetry) {
                                    TextButton(onClick = { viewModel.resendOtp(phone) }) { Text(text = err.action ?: retryLabel) }
                                }
                                if (err.showEmailFallback) {
                                    TextButton(onClick = onUseEmail) { Text(useEmailLabel) }
                                }
                            }
                            TextButton(onClick = { showTroubleshoot = true }) { Text(troubleshootLabel) }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            Text(text = weSentText)
            Spacer(modifier = Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Spacer(modifier = Modifier.weight(1f))
                for (i in 0 until 6) {
                    val state = otpDigits[i]
                    OutlinedTextField(
                        value = state.value,
                        onValueChange = { v ->
                            val digits = v.filter { it.isDigit() }
                            if (digits.length > 1) {
                                // paste
                                val all = digits
                                for (j in 0 until 6) {
                                    otpDigits[j].value = all.getOrNull(j)?.toString() ?: ""
                                }
                                val concat = otpDigits.joinToString("") { it.value }
                                viewModel.setOtp(concat)
                                if (concat.length == 6) onOtpComplete(concat)
                                focusManager.clearFocus()
                            } else {
                                state.value = digits
                                val concat = otpDigits.joinToString("") { it.value }
                                viewModel.setOtp(concat)
                                if (digits.isNotEmpty()) {
                                    if (i < 5) focusRequesters[i + 1].requestFocus() else focusManager.clearFocus()
                                } else {
                                    if (i > 0) focusRequesters[i - 1].requestFocus()
                                }
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = if (i == 5) ImeAction.Done else ImeAction.Next),
                        modifier = Modifier.width(48.dp).focusRequester(focusRequesters[i])
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(16.dp))

            val resendEnabled = seconds <= 0
            TextButton(onClick = {
                if (resendEnabled) {
                    viewModel.resendOtp(phone)
                    viewModel.startCountdown()
                    onResend()
                }
            }, enabled = resendEnabled) {
                if (!resendEnabled) Text(resendInFormat) else Text(resendText)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(onClick = {
                val token = otpDigits.joinToString("") { it.value }
                scope.launch {
                    val r = viewModel.verifyOtp(phone, token)
                    r.onSuccess { /* verified at repository level; callback/ navigation can happen in caller through onOtpComplete */ }
                }
            }, enabled = isComplete) {
                Text(verifyButtonLabel)
            }
        }
    }

    if (showTroubleshoot) {
        AlertDialog(onDismissRequest = { showTroubleshoot = false }, title = { Text(stringResource(id = R.string.troubleshoot_title)) }, text = {
            Column {
                Text(stringResource(id = R.string.troubleshoot_text))
            }
        }, confirmButton = {
            TextButton(onClick = { showTroubleshoot = false }) { Text(stringResource(id = R.string.ok)) }
        }, dismissButton = {
            TextButton(onClick = { showTroubleshoot = false }) { Text(stringResource(id = R.string.close)) }
        })
    }
}
