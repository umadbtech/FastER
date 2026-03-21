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
 * Terms & Conditions Screen
 * Displays the terms and conditions for using the FASTER app
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TermsScreen(
    onBackClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var accepted by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Terms & Conditions") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            item {
                Text(
                    text = "FASTER Festival App - Terms & Conditions",
                    style = MaterialTheme.typography.titleLarge
                )
            }

            item {
                Text(
                    text = """
                    Last Updated: March 2, 2026
                    
                    1. ACCEPTANCE OF TERMS
                    By using this app, you accept and agree to be bound by the terms and provision of this agreement.
                    
                    2. USE LICENSE
                    Permission is granted to temporarily access and use this app for personal, non-commercial transitory viewing only.
                    
                    3. DISCLAIMER OF WARRANTIES
                    This app is provided on an "AS IS" basis without warranties of any kind.
                    
                    4. LIMITATIONS OF LIABILITY
                    In no event shall FASTER or its suppliers be liable for any damages arising out of the use of this app.
                    
                    5. GOVERNING LAW
                    These terms and conditions are governed by and construed in accordance with the laws of California.
                    
                    6. CONTACT INFORMATION
                    If you have any questions about these terms, please contact support@faster.festival.
                    """.trimIndent(),
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Checkbox(
                            checked = accepted,
                            onCheckedChange = { accepted = it }
                        )
                        Text(
                            text = "I accept the Terms & Conditions",
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }

            item {
                Button(
                    onClick = { onBackClick() },
                    enabled = accepted,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text("Accept")
                }
            }
        }
    }
}
