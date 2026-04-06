package com.faster.festival.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
 * Terms of Use acceptance screen (Step 5 of onboarding).
 *
 * Displays scrollable terms content with two checkboxes:
 * - Terms and Conditions
 * - Privacy Policy
 * Both must be checked to enable the Accept button.
 */
@Composable
fun TermsAcceptanceScreen(
    termsAccepted: Boolean,
    onTermsAcceptedChange: (Boolean) -> Unit,
    onAcceptTerms: () -> Unit,
    modifier: Modifier = Modifier
) {
    var termsChecked by remember { mutableStateOf(false) }
    var privacyChecked by remember { mutableStateOf(false) }
    val bothChecked = termsChecked && privacyChecked

    // Sync with parent when both are checked/unchecked
    if (bothChecked != termsAccepted) {
        onTermsAcceptedChange(bothChecked)
    }

    val termsScrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 24.dp)
    ) {
        // Scrollable content area
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(top = 24.dp)
        ) {
            // Heading
            Text(
                text = "Terms of Use",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Subtitle
            Text(
                text = "Please accept both terms to continue",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Scrollable terms text
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(12.dp)
            ) {
                Text(
                    text = TERMS_CONTENT,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(termsScrollState)
                        .padding(end = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Terms and Conditions checkbox
            TermsCheckboxItem(
                label = "I accept the Terms and Conditions",
                isChecked = termsChecked,
                onCheckedChange = { termsChecked = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )

            // Privacy Policy checkbox
            TermsCheckboxItem(
                label = "I accept the Privacy Policy",
                isChecked = privacyChecked,
                onCheckedChange = { privacyChecked = it },
                modifier = Modifier.fillMaxWidth()
            )

            // Status text
            Text(
                text = "Both boxes must be checked to continue.",
                style = MaterialTheme.typography.bodySmall,
                color = if (bothChecked)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 16.dp)
            )
        }

        // Pinned bottom button
        Button(
            onClick = onAcceptTerms,
            enabled = bothChecked,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Text(
                text = "Accept & Continue",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

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
                indication = null,
                onClick = { onCheckedChange(!isChecked) }
            )
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Checkbox
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(
                    color = if (isChecked)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(4.dp)
                ),
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

        // Label
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f)
        )
    }
}

private val TERMS_CONTENT = """
By using the FASTER Festival application, you agree to abide by all festival rules and regulations. This includes but is not limited to:

1. ACCEPTANCE OF TERMS
By accessing and using this application, you accept and agree to be bound by the terms and conditions outlined herein.

2. USER CONDUCT
You agree to behave responsibly and respectfully at all festival events. Any violations may result in removal from the festival.

3. PRIVACY
Your personal information will be handled in accordance with our Privacy Policy. We collect only the data necessary to provide our services.

4. WRISTBAND USAGE
Festival wristbands are non-transferable and must be worn at all times during the event.

5. LIABILITY
FASTER Festival is not responsible for lost, stolen, or damaged personal property.

6. CONTACT INFORMATION
If you have any questions about these Terms and Conditions, please contact us at support@faster-festival.com

By clicking "Accept," you acknowledge that you have read these Terms and Conditions and agree to be bound by them.
""".trimIndent()
