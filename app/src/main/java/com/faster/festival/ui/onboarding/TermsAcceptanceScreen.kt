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
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
    viewModel: OnboardingViewModel,
    onTermsAcceptanceChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    // Track both checkboxes independently using local state
    var termsAndConditionsChecked by remember { mutableStateOf(false) }
    var privacyPolicyChecked by remember { mutableStateOf(false) }

    // Track if user has scrolled to bottom
    var isScrolledToBottom by remember { mutableStateOf(false) }

    // Get T&C text from ViewModel
    val termsText by viewModel.termsAndConditionsText.collectAsState()
    val scrollState = rememberScrollState()

    // Load T&C text on first composition
    LaunchedEffect(Unit) {
        viewModel.loadTermsAndConditions()
    }

    // Check if scrolled to bottom
    LaunchedEffect(scrollState.value) {
        if (scrollState.maxValue > 0) {
            val scrollProgress = scrollState.value.toFloat() / scrollState.maxValue
            isScrolledToBottom = scrollProgress > 0.95f  // 95% scrolled
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
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
                .padding(bottom = 8.dp)
        )

        // Subtitle with requirement
        Text(
            text = if (isScrolledToBottom) "Please accept both terms to continue" else "Please scroll down to read the full terms",
            style = MaterialTheme.typography.bodyMedium,
            color = if (isScrolledToBottom) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        )

        // Terms and Conditions Text Box
        if (termsText != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(12.dp)
            ) {
                Text(
                    text = termsText!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(scrollState)
                        .padding(end = 8.dp)  // Add padding for scrollbar
                )
            }
        }

        // Spacing
        Box(modifier = Modifier.height(16.dp))

        // Terms and Conditions Checkbox (only enabled if scrolled to bottom)
        TermsCheckboxItem(
            label = "I accept the Terms and Conditions",
            isChecked = termsAndConditionsChecked,
            onCheckedChange = { isChecked ->
                if (isScrolledToBottom) {
                    termsAndConditionsChecked = isChecked
                    val bothChecked = isChecked && privacyPolicyChecked
                    onTermsAcceptanceChange(bothChecked)
                }
            },
            enabled = isScrolledToBottom,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        )

        // Privacy Policy Checkbox (only enabled if scrolled to bottom)
        TermsCheckboxItem(
            label = "I accept the Privacy Policy",
            isChecked = privacyPolicyChecked,
            onCheckedChange = { isChecked ->
                if (isScrolledToBottom) {
                    privacyPolicyChecked = isChecked
                    val bothChecked = termsAndConditionsChecked && isChecked
                    onTermsAcceptanceChange(bothChecked)
                }
            },
            enabled = isScrolledToBottom,
            modifier = Modifier.fillMaxWidth()
        )

        // Info text - explains what happens
        Text(
            text = if (isScrolledToBottom) "Both boxes must be checked to continue." else "You must scroll down to read the full terms before accepting.",
            style = MaterialTheme.typography.bodySmall,
            color = if (isScrolledToBottom) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
        )
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
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberRipple(bounded = true),
                onClick = { if (enabled) onCheckedChange(!isChecked) }  // Toggle on click: true ↔ false
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
                    color = when {
                        isChecked -> MaterialTheme.colorScheme.primary  // Blue when checked
                        enabled -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)  // Gray outline when unchecked and enabled
                        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)  // Lighter gray when disabled
                    },
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
            color = if (enabled) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
            fontWeight = FontWeight.Normal,
            modifier = Modifier.weight(1f)
        )
    }
}
