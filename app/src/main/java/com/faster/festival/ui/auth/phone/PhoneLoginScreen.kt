package com.faster.festival.ui.auth.phone

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import com.faster.festival.R
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.google.i18n.phonenumbers.NumberParseException
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhoneLoginScreen(
    viewModel: PhoneLoginViewModel,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    onOtpSent: (String) -> Unit = {},
    onOtherMethod: () -> Unit = {}
) {
    val form by viewModel.formState.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var expanded by remember { mutableStateOf(false) }
    var selectedCountry by remember { mutableStateOf(commonCountries.first()) }
    var localNumber by remember { mutableStateOf("") }

    val weSentFormat = stringResource(id = R.string.we_sent_code)

    LaunchedEffect(uiState) {
        when (uiState) {
            is PhoneLoginUiState.Sent -> {
                val phone = (uiState as PhoneLoginUiState.Sent).phone
                val sentMsg = String.format(weSentFormat, phone)
                scope.launch { snackbarHostState.showSnackbar(sentMsg) }
                onOtpSent(phone)
            }
            is PhoneLoginUiState.Error -> {
                val msg = (uiState as PhoneLoginUiState.Error).message
                if (msg.isNotBlank()) scope.launch { snackbarHostState.showSnackbar(msg) }
            }
            else -> {}
        }
    }

    val title = stringResource(id = R.string.continue_with_phone)
    val backDesc = stringResource(id = R.string.back)

    Scaffold(
        modifier = modifier,
        topBar = { SmallTopAppBar(title = { Text(title) }, navigationIcon = { IconButton(onClick = onBack) { Icon(imageVector = Icons.Default.ArrowBack, contentDescription = backDesc) } }) },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Surface(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            Column(modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(modifier = Modifier.height(8.dp))

                // Country picker + local number
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                    // Use a Box anchor with clickable to ensure taps open the menu reliably
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                            .clickable { expanded = true }
                    ) {
                        OutlinedTextField(
                            value = "${selectedCountry.dialCode} ${selectedCountry.name}",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(id = R.string.country)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        commonCountries.forEach { country ->
                            DropdownMenuItem(text = { Text("${country.name} (${country.dialCode})") }, onClick = {
                                selectedCountry = country
                                expanded = false
                            })
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = localNumber,
                    onValueChange = { input ->
                        // accept digits and basic separators
                        val cleanedInput = input.filter { it.isDigit() || it == ' ' || it == '-' || it == '(' || it == ')' }
                        // update raw local value first
                        localNumber = cleanedInput

                        // attempt to parse and format using libphonenumber
                        val phoneUtil = PhoneNumberUtil.getInstance()
                        try {
                            val raw = selectedCountry.dialCode + cleanedInput
                            val parsed = phoneUtil.parse(raw, selectedCountry.code)
                            if (phoneUtil.isValidNumber(parsed)) {
                                // update the viewmodel with E.164 and format local display to NATIONAL
                                val e164 = phoneUtil.format(parsed, PhoneNumberUtil.PhoneNumberFormat.E164)
                                viewModel.onPhoneChange(e164)
                                val national = phoneUtil.format(parsed, PhoneNumberUtil.PhoneNumberFormat.NATIONAL)
                                // only update display if different to avoid cursor weirdness
                                if (national != localNumber) localNumber = national
                            }
                        } catch (e: NumberParseException) {
                            // parsing failed while typing — keep localNumber as-is and clear validity
                        }
                    },
                    label = { Text(stringResource(id = R.string.phone_number)) },
                    isError = form.phoneError != null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Done),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                form.phoneError?.let { Text(text = it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.align(Alignment.Start).padding(top = 4.dp)) }

                Spacer(modifier = Modifier.height(16.dp))

                Button(onClick = {
                    focusManager.clearFocus()
                    viewModel.sendOtp()
                }, enabled = form.isSubmitEnabled && uiState !is PhoneLoginUiState.Loading, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                    if (uiState is PhoneLoginUiState.Loading) CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MaterialTheme.colorScheme.onPrimary) else Text(stringResource(id = R.string.continue_with_phone))
                }

                Spacer(modifier = Modifier.height(12.dp))
                // Navigate back to previous/auth screen when the user selects another signup method
                TextButton(onClick = onBack) { Text(stringResource(id = R.string.sign_up_other_method)) }

                Spacer(modifier = Modifier.height(12.dp))

                val seconds = form.resendSecondsLeft
                TextButton(onClick = { viewModel.sendOtp() }, enabled = seconds <= 0) {
                    if (seconds > 0) Text(stringResource(id = R.string.resend_in, seconds)) else Text(stringResource(id = R.string.resend))
                }
            }
        }
    }
}
