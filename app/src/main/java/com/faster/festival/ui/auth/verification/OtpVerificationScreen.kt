package com.faster.festival.ui.auth.verification

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged

import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import com.faster.festival.R
import android.graphics.drawable.GradientDrawable
import androidx.compose.ui.graphics.toArgb

@Suppress("DEPRECATION")
@Composable
fun OtpVerificationScreen(
    email: String,
    viewModel: OtpViewModel,
    onVerified: () -> Unit,
    modifier: Modifier = Modifier,
    onCancel: () -> Unit = {}
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val state by viewModel.uiState.collectAsState()

    // Capture themed colors as ints at composition time (safe to use from coroutines)
    val toastBgColorInt = MaterialTheme.colorScheme.surfaceVariant.toArgb()
    val toastTextColorInt = MaterialTheme.colorScheme.onSurface.toArgb()

    // Ensure an OTP is sent when landing on this screen (direct navigation from Signup)
    LaunchedEffect(email) {
        try {
            // Store email in ViewModel so resend can reuse it
            viewModel.setEmail(email)
            // Initial automatic send should be silent (no toast/error); user-visible feedback appears on explicit actions
            viewModel.sendOtp(email, showFeedback = false)
        } catch (_: Exception) {
            // ignore
        }
    }

    // Focus requesters for each box
    val focusRequesters = remember { List(6) { FocusRequester() } }

    // Keep digits in state
    val digits = remember { mutableStateListOf("", "", "", "", "", "") }

    // Sync state.otp to digits
    LaunchedEffect(state.otp) {
        if (state.otp.length <= 6) {
            for (i in 0 until 6) digits[i] = state.otp.getOrNull(i)?.toString() ?: ""
        }
    }

    // collect events
    // Use a local flag to show success dialog once; avoid immediate navigation from event stream
    var showSuccessDialog by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is VerificationEvent.ShowSuccessDialog -> {
                    // set local flag; dialog will trigger navigation only when user confirms
                    showSuccessDialog = true
                }
                is VerificationEvent.ShowToast -> {
                    // Show a custom toast with an icon (drawable/faster_red) and the message
                    try {
                        val density = context.resources.displayMetrics.density
                        val padding = (12 * density).toInt()
                        val iconSize = (24 * density).toInt()

                        val layout = android.widget.LinearLayout(context).apply {
                            orientation = android.widget.LinearLayout.HORIZONTAL
                            setPadding(padding, padding, padding, padding)
                            // Use themed grey background
                            val bg = GradientDrawable().apply {
                                shape = GradientDrawable.RECTANGLE
                                cornerRadius = 8 * density
                                setColor(toastBgColorInt)
                            }
                            background = bg
                        }

                        val imageView = android.widget.ImageView(context).apply {
                            try { setImageResource(R.drawable.faster_red) } catch (_: Exception) { setImageDrawable(null) }
                            layoutParams = android.view.ViewGroup.LayoutParams(iconSize, iconSize)
                        }

                        val textView = android.widget.TextView(context).apply {
                            text = event.message
                            setTextColor(toastTextColorInt)
                            setPadding((8 * density).toInt(), 0, 0, 0)
                            textSize = 14f
                        }

                        layout.addView(imageView)
                        layout.addView(textView)

                        val toast = android.widget.Toast(context)
                        toast.duration = android.widget.Toast.LENGTH_SHORT
                        toast.view = layout
                        toast.show()
                    } catch (e: Exception) {
                        // Fallback to simple toast if anything goes wrong
                        android.widget.Toast.makeText(context, event.message, android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
                else -> {}
            }
        }
    }

    // Start 30s timer when entering if not started
    LaunchedEffect(Unit) { if (state.resendCooldown <= 0) viewModel.startTimer(30) }

    Surface(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top // changed to Top
        ) {
            // Top row: Back arrow at start
            Row(modifier = Modifier.fillMaxWidth()) {
                IconButton(onClick = onCancel) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            }

            // Title
            Text(
                text = "Verify your email",
                style =
                MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold
                )
            )

            Spacer(modifier = Modifier.height(16.dp)) // Title -> 16dp

            // Subtitle
            Text(
                text = "Enter the 6-digit code sent to $email",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp)) // Subtitle -> 24dp

            // OTP area
            Spacer(modifier = Modifier.height(32.dp)) // OTP -> 32dp (space before OTP fields)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (i in 0 until 6) {
                    OtpDigitBox(
                        value = digits[i],
                        onValueChange = { input ->
                            // paste handling
                            if (input.length > 1) {
                                val paste = input.filter { it.isDigit() }.take(6)
                                for (j in 0 until 6) digits[j] = paste.getOrNull(j)?.toString() ?: ""
                                viewModel.setCode(paste)
                                if (paste.length == 6) keyboardController?.hide()
                            } else {
                                val ch = input.filter { it.isDigit() }
                                if (ch.isEmpty()) {
                                    digits[i] = ""
                                    viewModel.setCode(digits.joinToString(""))
                                    if (i > 0) focusRequesters[i - 1].requestFocus()
                                } else {
                                    digits[i] = ch
                                    viewModel.setCode(digits.joinToString(""))
                                    if (i < 5) focusRequesters[i + 1].requestFocus() else keyboardController?.hide()
                                }
                            }
                        },
                        focusRequester = focusRequesters[i],
                        isError = state.error != null,
                        modifier = Modifier
                    )
                }
            }

            // Error message below OTP
            state.error?.let { err ->
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = err,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp)) // Button spacing -> 24dp

            Button(
                onClick = {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                    viewModel.verifyOtp(email)
                },
                enabled = state.otp.length == 6 && !state.isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (state.isLoading)
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                else Text("Verify")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Resend control
            OutlinedButton(
                onClick = { if (state.resendCooldown == 0 && !state.isResending) viewModel.resendUsingSignup() },
                enabled = state.resendCooldown == 0 && !state.isResending,
                modifier = Modifier.fillMaxWidth()
            ) {
                when {
                    state.resendCooldown > 0 -> Text("Resend in ${state.resendCooldown}s")
                    state.isResending -> Text("Resending...")
                    else -> Text("Resend Code")
                }
            }
        }
    }

    // Show success dialog when verification completed and email_verified == true
    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { /* don't dismiss by tapping outside; require explicit OK */ },
            confirmButton = {
                TextButton(onClick = {
                    showSuccessDialog = false
                    // Navigate to home only after user acknowledges
                    onVerified()
                }) {
                    Text(text = "OK")
                }
            },
            title = { Text(text = "Email Verified") },
            text = { Text(text = "Your email has been successfully verified.") }
        )
    }
}

@Composable
private fun OtpDigitBox(
    value: String,
    onValueChange: (String) -> Unit,
    focusRequester: FocusRequester,
    isError: Boolean,
    modifier: Modifier = Modifier
) {
    var focused by remember { mutableStateOf(false) }
    val borderColor by animateColorAsState(
        targetValue = when {
            isError -> MaterialTheme.colorScheme.error
            focused -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        }, label = ""
    )

    Box(
        modifier = modifier
            .size(56.dp)
            .border(width = 1.dp, color = borderColor, shape = RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 20.sp, textAlign = TextAlign.Center),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .onFocusChanged { focused = it.isFocused }
                .padding(horizontal = 4.dp)
        )
    }
}
