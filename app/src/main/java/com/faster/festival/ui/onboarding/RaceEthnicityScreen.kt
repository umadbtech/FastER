package com.faster.festival.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Screen 2: Race & Ethnicity multi-select.
 * User can select one or more options; "Other/Multiple" reveals a text input.
 */
@Composable
fun RaceEthnicityScreen(
    formState: OnboardingFormState,
    onRaceToggle: (String) -> Unit,
    onCustomTextChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // Mapping of display labels to backend enum values
    val raceOptions = listOf(
        "Asian" to "asian",
        "Black or African American" to "black_or_african_american",
        "White" to "white",
        "Hispanic or Latino" to "hispanic_or_latino",
        "Native American or Alaska Native" to "native_american_or_alaska_native",
        "Native Hawaiian or Pacific Islander" to "native_hawaiian_or_pacific_islander",
        "Middle Eastern or North African" to "middle_eastern_or_north_african",
        "Multiracial" to "multiracial",
        "Other" to "other",
        "Prefer Not to Say" to "prefer_not_to_say",
        "Self-Describe" to "self_describe"
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
            text = "Race and Ethnicity",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Subheading
        Text(
            text = "Please select the race(s) and/or ethnicities you identify with so we can find you in a crowd in case of an emergency.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Checkboxes
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            raceOptions.forEach { (displayLabel, backendValue) ->
                RaceCheckboxItem(
                    label = displayLabel,
                    isChecked = formState.selectedRaceEthnicity.contains(backendValue),
                    onToggle = { onRaceToggle(backendValue) }
                )
            }
        }

        // Custom text input if "self_describe" is selected
        // Validation rule: if self_describe is included, race_ethnicity_text must be non-empty
        if (formState.selectedRaceEthnicity.contains("self_describe")) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp)
            ) {
                Text(
                    text = "Please describe your race and ethnicity",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                OutlinedTextField(
                    value = formState.raceEthnicityText,
                    onValueChange = onCustomTextChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Enter your race/ethnicity") },
                    maxLines = 3,
                    singleLine = false
                )
            }
        } else {
            // Validation rule: if self_describe is NOT included, race_ethnicity_text must be NULL/empty
            if (formState.raceEthnicityText.isNotEmpty()) {
                onCustomTextChange("")
            }
        }
    }
}

@Composable
private fun RaceCheckboxItem(
    label: String,
    isChecked: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
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
        Checkbox(
            checked = isChecked,
            onCheckedChange = { onToggle() }
        )
    }
}
