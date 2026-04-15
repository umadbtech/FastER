package com.faster.festival.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

/**
 * Support Ticket / Report Issue Screen
 * Allows users to report issues and create support tickets
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportTicketScreen(
    onBackClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var issueTitle by remember { mutableStateOf("") }
    var issueDescription by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("General") }

    val context = LocalContext.current
    val navBarPadding = WindowInsets.navigationBars.asPaddingValues()
    val bottomSafeInset = navBarPadding.calculateBottomPadding()
    val buttonHeight = 52.dp
    val buttonBottomMargin = 24.dp
    val buttonHorizontalMargin = 16.dp
    val scrollBottomPadding = buttonHeight + buttonBottomMargin + bottomSafeInset + 16.dp

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBar(
            title = { Text("Report an Issue") },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            }
        )

        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 16.dp,
                    bottom = scrollBottomPadding
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = issueTitle,
                        onValueChange = { issueTitle = it },
                        label = { Text("Issue Title") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = issueDescription,
                        onValueChange = { issueDescription = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 5
                    )
                }

                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Category",
                                style = MaterialTheme.typography.labelLarge
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            listOf("General", "App Bug", "Wristband", "Payment", "Other").forEach { category ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = selectedCategory == category,
                                        onClick = { selectedCategory = category }
                                    )
                                    Text(category)
                                }
                            }
                        }
                    }
                }
            }

            Button(
                onClick = {
                    Toast.makeText(
                        context,
                        "This feature is coming soon",
                        Toast.LENGTH_SHORT
                    ).show()
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(
                        start = buttonHorizontalMargin,
                        end = buttonHorizontalMargin,
                        bottom = bottomSafeInset + buttonBottomMargin
                    )
                    .height(buttonHeight)
            ) {
                Text("Submit Ticket")
            }
        }
    }
}
