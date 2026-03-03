package com.faster.festival.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Privacy Policy Screen
 * Displays the privacy policy for the FASTER app
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen(
    onBackClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top App Bar
        TopAppBar(
            title = { Text("Privacy Policy") },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            }
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "FASTER Festival App - Privacy Policy",
                    style = MaterialTheme.typography.titleLarge
                )
            }

            item {
                Text(
                    text = """
                    Last Updated: March 2, 2026
                    
                    1. INFORMATION WE COLLECT
                    We collect information about you in various ways, including personal information you provide directly and information collected automatically when you use the app.
                    
                    2. HOW WE USE YOUR INFORMATION
                    We use the information we collect to provide, maintain, and improve the app, process transactions, and send you related information.
                    
                    3. DATA SECURITY
                    We implement appropriate technical and organizational measures to protect your personal information against unauthorized access and alteration.
                    
                    4. LOCATION INFORMATION
                    When you enable location services, we collect your location data to provide location-based features and improve your festival experience.
                    
                    5. THIRD-PARTY SHARING
                    We do not sell, trade, or rent your personal information to third parties without your explicit consent.
                    
                    6. CHILDREN'S PRIVACY
                    This app is not intended for children under 13 years of age. We do not knowingly collect information from children under 13.
                    
                    7. CHANGES TO THIS POLICY
                    We may update this policy from time to time. The most current version will always be available in the app.
                    
                    8. CONTACT US
                    If you have questions about this privacy policy, please contact privacy@faster.festival.
                    """.trimIndent(),
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            item {
                Button(
                    onClick = { onBackClick() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text("I Understand")
                }
            }
        }
    }
}
