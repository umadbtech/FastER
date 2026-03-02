package com.faster.festival.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Screen 3: Gender Identity single-select.
 * User can select one option; "Self-describe" reveals a text input.
 */
@Composable
fun GenderIdentityScreen(
    formState: OnboardingFormState,
    onGenderSelect: (String) -> Unit,
    onCustomTextChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // Mapping of display labels to enum values (backend validation)
    val genderOptions = mapOf(
        "Male" to "male",
        "Female" to "female",
        "Non-binary" to "non_binary",
        "Prefer Not to Say" to "prefer_not_to_say",
        "Self-describe" to "self_describe"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Heading
        Text(
            text = "Gender Identity",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Subheading
        Text(
            text = "Please select the gender you identify with so we can find you in a crowd in case of an emergency.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Radio buttons
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            genderOptions.forEach { (displayLabel, enumValue) ->
                GenderRadioItem(
                    label = displayLabel,
                    isSelected = formState.selectedGenderIdentity == enumValue,
                    onSelect = { onGenderSelect(enumValue) }
                )
            }
        }

        // Custom text input if "Self-describe" is selected
        if (formState.selectedGenderIdentity == "self_describe") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp)
            ) {
                Text(
                    text = "Please describe (optional)",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                OutlinedTextField(
                    value = formState.genderIdentityText,
                    onValueChange = onCustomTextChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Describe your gender identity") },
                    maxLines = 3,
                    singleLine = false
                )
            }
        }
    }
}

@Composable
private fun GenderRadioItem(
    label: String,
    isSelected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    androidx.compose.foundation.layout.Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        RadioButton(
            selected = isSelected,
            onClick = onSelect
        )
    }
}
