package com.faster.festival.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Emergency Contacts Management Screen
 * Allows users to manage their emergency contacts
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmergencyContactsScreen(
    onBackClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val emergencyContacts = remember {
        mutableStateOf(
            listOf(
                Pair("Jane Doe", "555-0123"),
                Pair("Bob Smith", "555-0456")
            )
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top App Bar
        TopAppBar(
            title = { Text("Emergency Contacts") },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                IconButton(onClick = { /* Add new contact */ }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Contact")
                }
            }
        )

        // Content
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(emergencyContacts.value.size) { index ->
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = emergencyContacts.value[index].first,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = emergencyContacts.value[index].second,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        IconButton(
                            onClick = {
                                val updated = emergencyContacts.value.toMutableList()
                                updated.removeAt(index)
                                emergencyContacts.value = updated
                            }
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}
