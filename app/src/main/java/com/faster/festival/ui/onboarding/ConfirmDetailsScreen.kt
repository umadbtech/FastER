package com.faster.festival.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Locale


/**
 * Confirm Account Details screen (Step 3 of onboarding).
 *
 * Collects:
 * - Full Legal Name (editable)
 * - Phone Number (editable)
 *
 * Displays (read-only):
 * - Email (from session)
 * - Date of Birth (from step 1)
 * - Gender Identity (from step 1)
 *
 * Saves profile name, then advances to the Username step.
 */
@Composable
fun ConfirmDetailsScreen(
    legalName: String,
    phoneNumber: String,
    email: String,
    dateOfBirth: String,
    genderIdentity: String,
    legalNameError: String?,
    phoneNumberError: String?,
    onLegalNameChange: (String) -> Unit,
    onPhoneNumberChange: (String) -> Unit,
    onCreateAccount: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()

    // Format DOB for display
    val displayDob = if (dateOfBirth.isNotEmpty()) {
        try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val date = sdf.parse(dateOfBirth)
            if (date != null) {
                SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(date)
            } else dateOfBirth
        } catch (e: Exception) {
            dateOfBirth
        }
    } else ""

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 24.dp)
    ) {
        // Section header
        Text(
            text = "Confirm Account Details",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(32.dp))

        // ---- Full Legal Name (editable) ----
        OutlinedTextField(
            value = legalName,
            onValueChange = onLegalNameChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Full Legal Name") },
            placeholder = { Text("John Smith") },
            singleLine = true,
            isError = legalNameError != null,
            supportingText = if (legalNameError != null) {
                { Text(text = legalNameError, color = MaterialTheme.colorScheme.error) }
            } else null,
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Next,
                capitalization = KeyboardCapitalization.Words
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // ---- Phone Number (editable) ----
        OutlinedTextField(
            value = phoneNumber,
            onValueChange = onPhoneNumberChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Phone Number") },
            placeholder = { Text("111-111-1111") },
            singleLine = true,
            isError = phoneNumberError != null,
            supportingText = if (phoneNumberError != null) {
                { Text(text = phoneNumberError, color = MaterialTheme.colorScheme.error) }
            } else null,
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Done,
                keyboardType = KeyboardType.Phone
            ),
            keyboardActions = KeyboardActions(
                onDone = { focusManager.clearFocus() }
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // ---- Email (read-only) ----
        OutlinedTextField(
            value = email,
            onValueChange = {},
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Email") },
            readOnly = true,
            enabled = false,
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                disabledTextColor = MaterialTheme.colorScheme.onBackground,
                disabledBorderColor = MaterialTheme.colorScheme.outline,
                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // ---- Date of Birth (read-only) ----
        OutlinedTextField(
            value = displayDob,
            onValueChange = {},
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Date of Birth") },
            readOnly = true,
            enabled = false,
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                disabledTextColor = MaterialTheme.colorScheme.onBackground,
                disabledBorderColor = MaterialTheme.colorScheme.outline,
                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // ---- Gender Identity (read-only) ----
        OutlinedTextField(
            value = genderIdentity,
            onValueChange = {},
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Gender Identity") },
            readOnly = true,
            enabled = false,
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                disabledTextColor = MaterialTheme.colorScheme.onBackground,
                disabledBorderColor = MaterialTheme.colorScheme.outline,
                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )

        Spacer(modifier = Modifier.weight(1f))
        Spacer(modifier = Modifier.height(32.dp))

        // ---- Continue Button ----
        Button(
            onClick = onCreateAccount,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Text(
                text = "Continue",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
