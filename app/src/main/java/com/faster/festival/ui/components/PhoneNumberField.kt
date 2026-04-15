package com.faster.festival.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.faster.festival.utils.PhoneNumberUtils

/**
 * Reusable phone number text field with consistent keyboard, validation,
 * and input sanitization across the app.
 *
 * - Uses [KeyboardType.Phone] — the only keyboard that allows `+`, `*`, `#`, digits
 *   while still being numeric-friendly
 * - Filters input via [PhoneNumberUtils.sanitizeInput] so only `+` and digits survive
 * - Shows an inline error when [errorMessage] is non-null
 * - Leading phone icon matches the existing design
 *
 * Use [onValueChange] to capture the sanitized value. Call
 * [PhoneNumberUtils.validate] from the ViewModel / on submit to set the error state.
 */
@Composable
fun PhoneNumberField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Phone Number",
    placeholder: String = "+1 234 567 8900",
    errorMessage: String? = null,
    imeAction: ImeAction = ImeAction.Done,
    enabled: Boolean = true,
    showLeadingIcon: Boolean = true
) {
    OutlinedTextField(
        value = value,
        onValueChange = { new ->
            // Sanitize on every keystroke so only `+` (once, at start) and digits remain
            onValueChange(PhoneNumberUtils.sanitizeInput(new))
        },
        modifier = modifier.fillMaxWidth(),
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        singleLine = true,
        enabled = enabled,
        isError = errorMessage != null,
        supportingText = if (errorMessage != null) {
            { Text(errorMessage) }
        } else null,
        leadingIcon = if (showLeadingIcon) {
            {
                Icon(
                    imageVector = Icons.Default.Phone,
                    contentDescription = null
                )
            }
        } else null,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Phone,
            imeAction = imeAction
        ),
        shape = RoundedCornerShape(12.dp)
    )
}
