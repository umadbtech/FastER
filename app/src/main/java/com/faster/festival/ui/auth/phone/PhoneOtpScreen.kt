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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.faster.festival.R
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhoneOtpScreen(
    phone: String,
    viewModel: PhoneOtpViewModel,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    onVerified: () -> Unit = {}
) {
    val form by viewModel.formState.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // State for segmented OTP boxes
    val otpDigits = remember { List(6) { mutableStateOf("") } }
    val focusRequesters = remember { List(6) { FocusRequester() } }

    LaunchedEffect(uiState) {
        when (uiState) {
            is PhoneOtpUiState.Verified -> onVerified()
            is PhoneOtpUiState.Error -> {
                val msg = (uiState as PhoneOtpUiState.Error).message
                if (msg.isNotBlank()) scope.launch { snackbarHostState.showSnackbar(msg) }
            }
            else -> {}
        }
    }

    // helper to get concatenated OTP
    fun concatenatedOtp(): String = otpDigits.joinToString(separator = "") { it.value }

    val title = stringResource(id = R.string.verify_sms_code_title)
    val backDesc = stringResource(id = R.string.back)
    val codeSent = stringResource(id = R.string.code_sent_to, phone)

    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text(title) }, navigationIcon = { IconButton(onClick = onBack) { Icon(imageVector = Icons.Default.ArrowBack, contentDescription = backDesc) } }) },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Surface(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            Column(modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .animateContentSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = codeSent, style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(12.dp))

                // Segmented OTP input
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Spacer(modifier = Modifier.weight(1f))
                    for (i in 0 until 6) {
                        val state = otpDigits[i]
                        OutlinedTextField(
                            value = state.value,
                            onValueChange = { v ->
                                // accept only digits
                                val digits = v.filter { it.isDigit() }
                                if (digits.length > 1) {
                                    // Paste or multi-digit input: fill successive boxes
                                    val all = digits
                                    for (j in 0 until 6) {
                                        val ch = all.getOrNull(j)?.toString() ?: ""
                                        otpDigits[j].value = ch
                                    }
                                    // update viewModel
                                    viewModel.onOtpChange(concatenatedOtp())
                                    // move focus to last
                                    focusRequesters.getOrNull(minOf(5, digits.length - 1))?.requestFocus()
                                } else {
                                    state.value = digits
                                    viewModel.onOtpChange(concatenatedOtp())
                                    if (digits.isNotEmpty()) {
                                        if (i < 5) focusRequesters[i + 1].requestFocus()
                                        else focusManager.clearFocus()
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

                form.otpError?.let { Text(text = it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.align(Alignment.Start).padding(top = 8.dp)) }

                Spacer(modifier = Modifier.height(12.dp))

                val verifyOtpLabel = stringResource(id = R.string.verify_otp_button)
                Button(onClick = {
                    focusManager.clearFocus()
                    val token = concatenatedOtp()
                    if (token.length == 6) {
                        scope.launch { viewModel.verifyOtp(phone, token) }
                    }
                }, enabled = form.isFormValid && uiState !is PhoneOtpUiState.Loading, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                    if (uiState is PhoneOtpUiState.Loading) CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MaterialTheme.colorScheme.onPrimary) else Text(verifyOtpLabel)
                }

                Spacer(modifier = Modifier.height(12.dp))

                val seconds = form.resendSecondsLeft
                TextButton(onClick = { viewModel.resendOtp(phone); viewModel.startResendTimer() }, enabled = seconds <= 0) {
                    if (seconds > 0) Text(stringResource(id = R.string.resend_in, seconds)) else Text(stringResource(id = R.string.resend))
                }
            }
        }
    }
}
