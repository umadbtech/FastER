package com.faster.festival.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * FAQ (Frequently Asked Questions) Screen
 * Displays common questions and answers
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FAQScreen(
    onBackClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val faqs = listOf(
        "How do I pair my FASTER Wristband?" to "Go to Settings > Devices and follow the pairing instructions.",
        "How do I reset my password?" to "Visit the login screen and click 'Forgot Password' to reset your password.",
        "How do I contact support?" to "Visit the Support section in the app or email support@faster.festival.",
        "Is my location data secure?" to "Yes, all location data is encrypted and stored securely.",
        "Can I delete my account?" to "Yes, you can request account deletion in the Account Management section."
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top App Bar
        TopAppBar(
            title = { Text("FAQ") },
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
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(faqs.size) { index ->
                FAQItem(
                    question = faqs[index].first,
                    answer = faqs[index].second
                )
            }
        }
    }
}

@Composable
fun FAQItem(
    question: String,
    answer: String,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = question,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = "Expand"
                )
            }

            if (expanded) {
                HorizontalDivider()
                Text(
                    text = answer,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}
