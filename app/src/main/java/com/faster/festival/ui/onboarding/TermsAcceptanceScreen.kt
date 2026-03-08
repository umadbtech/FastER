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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Terms of Use acceptance screen with checkboxes for Terms and Conditions and Privacy Policy.
 * BOTH checkboxes must be checked to enable the Submit button.
 * Modern Material 3 design matching the screenshot.
 *
 * ✅ Integration with POST /functions/v1/accept-terms endpoint
 * - Tracks independent checkbox states
 * - Validates both checkboxes are checked
 * - Calls ViewModel to submit terms
 */
@Composable
fun TermsAcceptanceScreen(
    viewModel: OnboardingViewModel,
    onTermsAcceptanceChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    onSubmitTerms: (() -> Unit)? = null  // ✅ Optional callback for submit
) {
    // Track both checkboxes independently using local state
    val termsAndConditionsChecked = remember { mutableStateOf(false) }
    val privacyPolicyChecked = remember { mutableStateOf(false) }

    // Update parent when both checkboxes are checked
    LaunchedEffect(termsAndConditionsChecked.value, privacyPolicyChecked.value) {
        val bothChecked = termsAndConditionsChecked.value && privacyPolicyChecked.value
        onTermsAcceptanceChange(bothChecked)
        viewModel.updateTermsAcceptance(bothChecked)
    }

    val scrollState = rememberScrollState()

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
            text = "Please accept both terms to continue",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        )

        // Terms Text Box (scrollable)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(12.dp)
        ) {
            Text(
                text = termsContent,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(end = 8.dp)
            )
        }

        // Spacing
        Box(modifier = Modifier.height(24.dp))

        // Terms and Conditions Checkbox
        TermsCheckboxItem(
            label = "I accept the Terms and Conditions",
            isChecked = termsAndConditionsChecked.value,
            onCheckedChange = { isChecked ->
                termsAndConditionsChecked.value = isChecked
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        )

        // Privacy Policy Checkbox
        TermsCheckboxItem(
            label = "I accept the Privacy Policy",
            isChecked = privacyPolicyChecked.value,
            onCheckedChange = { isChecked ->
                privacyPolicyChecked.value = isChecked
            },
            modifier = Modifier.fillMaxWidth()
        )

        // Requirement text
        Text(
            text = "Both boxes must be checked to continue.",
            style = MaterialTheme.typography.bodySmall,
            color = if (termsAndConditionsChecked.value && privacyPolicyChecked.value)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
        )

        // Flexible spacing to push content to top
        Box(modifier = Modifier.weight(1f))
    }
}

/**
 * Individual checkbox item for terms acceptance.
 * Shows a custom checkbox with label text and ripple effect.
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
                onClick = { onCheckedChange(!isChecked) }
            )
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Custom Checkbox Box
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(
                    color = if (isChecked)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
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

/**
 * Terms and Conditions content
 */
private val termsContent = """
10. CONTACT INFORMATION
If you have any questions about these Terms and Conditions, please contact us at support@faster-festival.com

By clicking "Accept," you acknowledge that you have read these Terms and Conditions and agree to be bound by them.
""".trimIndent()

