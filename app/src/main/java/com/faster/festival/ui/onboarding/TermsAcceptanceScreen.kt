package com.faster.festival.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Terms of Use acceptance screen with checkboxes for Terms and Conditions and Privacy Policy.
 * BOTH checkboxes must be checked to enable the Submit button.
 *
 * When both are checked, formState.termsAccepted is set to true.
 * Submit button is only enabled when formState.termsAccepted is true.
 *
 * Flow:
 * 1. User unchecks (false) or checks (true) Terms checkbox
 * 2. onTermsAcceptanceChange() is called with: termsChecked AND privacyChecked
 * 3. formState.termsAccepted is updated
 * 4. Submit button enabled only when termsAccepted == true (both boxes checked)
 */
@Composable
fun TermsAcceptanceScreen(
    formState: OnboardingFormState,
    onTermsAcceptanceChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    // Track both checkboxes independently using local state
    // These need to be separate to show the UI properly
    var termsAndConditionsChecked by remember { mutableStateOf(false) }
    var privacyPolicyChecked by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.Start
    ) {
        // Main Heading
        Text(
            text = "Terms of Use",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        )

        // Subtitle with requirement
        Text(
            text = "Please accept both terms to continue",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        )

        // Terms and Conditions Checkbox
        TermsCheckboxItem(
            label = "I accept the Terms and Conditions",
            isChecked = termsAndConditionsChecked,
            onCheckedChange = { isChecked ->
                termsAndConditionsChecked = isChecked
                // Both must be true to enable submit
                val bothChecked = isChecked && privacyPolicyChecked
                onTermsAcceptanceChange(bothChecked)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        )

        // Privacy Policy Checkbox
        TermsCheckboxItem(
            label = "I accept the Privacy Policy",
            isChecked = privacyPolicyChecked,
            onCheckedChange = { isChecked ->
                privacyPolicyChecked = isChecked
                // Both must be true to enable submit
                val bothChecked = termsAndConditionsChecked && isChecked
                onTermsAcceptanceChange(bothChecked)
            },
            modifier = Modifier.fillMaxWidth()
        )

        // Info text - explains what happens
        Text(
            text = "You must check both boxes to continue with onboarding. This ensures you have accepted all our terms.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 32.dp)
        )

        // Flexible spacing to push content to top
        Box(modifier = Modifier.weight(1f))
    }
}

/**
 * Individual checkbox item for terms acceptance.
 * Shows a custom checkbox with label text and ripple effect.
 * Click to toggle: checked (true) ↔ unchecked (false)
 *
 * Visual feedback:
 * - Checked: Primary color background with checkmark icon
 * - Unchecked: Light outline with no icon
 * - Clickable: Ripple effect on tap
 */
@Composable
private fun TermsCheckboxItem(
    label: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberRipple(bounded = true),
                onClick = { onCheckedChange(!isChecked) }  // Toggle on click: true ↔ false
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Custom Checkbox Box
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(
                    color = if (isChecked)
                        MaterialTheme.colorScheme.primary  // Blue when checked
                    else
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),  // Gray outline when unchecked
                    shape = RoundedCornerShape(4.dp)
                )
                .clip(RoundedCornerShape(4.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (isChecked) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = "Checked",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // Label Text
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Normal,
            modifier = Modifier.weight(1f)
        )
    }
}
