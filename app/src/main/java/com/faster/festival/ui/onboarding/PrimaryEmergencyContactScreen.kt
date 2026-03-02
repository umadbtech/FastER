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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Screen 4: Primary Emergency Contact form.
 * User enters name, phone (with country code), and optional relationship.
 */
@Composable
fun PrimaryEmergencyContactScreen(
    formState: OnboardingFormState,
    onNameChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onRelationshipChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
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
            text = "Primary Emergency Contact",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Subheading
        Text(
            text = "We need at least one emergency contact in case we need to reach your family or friends during the festival.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Error message if any
        formState.emergencyContactError?.let { error ->
            Text(
                text = error,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )
        }

        // Emergency Contact Name field
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Text(
                text = "Contact Name",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            OutlinedTextField(
                value = formState.emergencyContactName,
                onValueChange = onNameChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("e.g., John Doe") },
                singleLine = true,
                isError = formState.emergencyContactError != null
            )
        }

        // Phone Number field with country code indicator
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Text(
                text = "Phone Number (with Country Code)",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            OutlinedTextField(
                value = formState.emergencyContactPhone,
                onValueChange = onPhoneChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("e.g., +1 (555) 123-4567") },
                singleLine = true,
                isError = formState.emergencyContactError != null
            )
        }

        // Relationship field (optional)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = "Relationship (Optional)",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            OutlinedTextField(
                value = formState.emergencyContactRelationship,
                onValueChange = onRelationshipChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("e.g., Sister, Brother, Friend") },
                singleLine = true
            )
        }
    }
}
