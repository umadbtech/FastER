package com.faster.festival.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val CoralRed = Color(0xFFE53935)

/** Health Settings Screen — Landing + Main health management */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthSettingsScreen(onBackClick: () -> Unit = {}, modifier: Modifier = Modifier) {
    var showMainHealth by remember { mutableStateOf(false) }

    if (showMainHealth) {
        HealthMainScreen(
                onBackClick = { showMainHealth = false },
                modifier = modifier
        )
    } else {
        HealthLandingScreen(
                onBackClick = onBackClick,
                onContinue = { showMainHealth = true },
                modifier = modifier
        )
    }
}

// ─── Health Landing ───────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HealthLandingScreen(
        onBackClick: () -> Unit,
        onContinue: () -> Unit,
        modifier: Modifier = Modifier
) {
    Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                        title = {
                            Text(
                                    "Health",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = onBackClick) {
                                Icon(
                                        Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Back"
                                )
                            }
                        },
                        colors =
                                TopAppBarDefaults.centerAlignedTopAppBarColors(
                                        containerColor = MaterialTheme.colorScheme.background
                                )
                )
            },
            floatingActionButton = {
                ExtendedFloatingActionButton(
                        onClick = onContinue,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).height(52.dp),
                        shape = RoundedCornerShape(26.dp),
                        containerColor = CoralRed,
                        contentColor = Color.White
                ) {
                    Text("Continue to Health", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            floatingActionButtonPosition = FabPosition.Center
    ) { paddingValues ->
        Column(
                modifier =
                        modifier.fillMaxSize()
                                .background(MaterialTheme.colorScheme.background)
                                .padding(paddingValues)
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 24.dp)
        ) {
            // Title
            Text(
                    text = "Welcome to FASTER Health",
                    fontWeight = FontWeight.Bold,
                    fontSize = 26.sp,
                    lineHeight = 32.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Subtitle — left aligned
            Text(
                    text =
                            "Personal health information you want medical personnel to know in the case of an emergency",
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 22.sp,
                    textAlign = TextAlign.Start
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Section header
            Text(
                    text = "How your health information is used",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Numbered points
            HealthInfoPoint(
                    number = 1,
                    text =
                            "Your health data is saved in our system without your name or any details that identify you."
            )
            Spacer(modifier = Modifier.height(14.dp))
            HealthInfoPoint(
                    number = 2,
                    text =
                            "If you call for help in an emergency, your personal and health information will be shared with the dispatcher who takes your call."
            )
            Spacer(modifier = Modifier.height(14.dp))
            HealthInfoPoint(
                    number = 3,
                    text =
                            "Emergency responders will use your information to help them respond quickly and effectively."
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Checkmark + privacy note
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF607D8B),
                        modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                        text = "Your health data is never sold.",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                )
            }

            // Bottom spacing for FAB clearance
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@Composable
private fun HealthInfoPoint(number: Int, text: String) {
    Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
    ) {
        Text(
                text = "$number.",
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                modifier = Modifier.width(20.dp)
        )
        Text(
                text = text,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ─── Health Main Screen ───────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HealthMainScreen(onBackClick: () -> Unit, modifier: Modifier = Modifier) {
    // Local state for health data
    var allergies by remember { mutableStateOf(listOf("Celiac (Severe Gluten Allergy)", "Penicillin")) }
    var medications by remember { mutableStateOf(listOf("Adderall 5mg")) }
    var accessibility by remember { mutableStateOf(listOf("Adderall 5mg")) }
    var additionalInfo by remember { mutableStateOf("") }

    // Add dialogs
    var showAddAllergyDialog by remember { mutableStateOf(false) }
    var showAddMedicationDialog by remember { mutableStateOf(false) }
    var showAddAccessibilityDialog by remember { mutableStateOf(false) }
    var newItemText by remember { mutableStateOf("") }

    Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                        title = {
                            Text(
                                    "Health",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = onBackClick) {
                                Icon(
                                        Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Back"
                                )
                            }
                        },
                        colors =
                                TopAppBarDefaults.centerAlignedTopAppBarColors(
                                        containerColor = MaterialTheme.colorScheme.surface
                                )
                )
            }
    ) { paddingValues ->
    Column(
            modifier =
                    modifier.fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                            .padding(paddingValues)
    ) {
        // Scrollable content
        Column(
                modifier =
                        Modifier.fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            // Allergies
            HealthSection(
                    title = "Allergies",
                    items = allergies,
                    onAddClick = {
                        newItemText = ""
                        showAddAllergyDialog = true
                    },
                    onItemClick = { /* detail view */ }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Medications
            HealthSection(
                    title = "Medications",
                    items = medications,
                    onAddClick = {
                        newItemText = ""
                        showAddMedicationDialog = true
                    },
                    onItemClick = { /* detail view */ }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Accessibility
            HealthSection(
                    title = "Accessibility",
                    items = accessibility,
                    onAddClick = {
                        newItemText = ""
                        showAddAccessibilityDialog = true
                    },
                    onItemClick = { /* detail view */ }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Additional Information
            Text(
                    text = "Additional Information",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                    value = additionalInfo,
                    onValueChange = { additionalInfo = it },
                    placeholder = { Text("Textbox", color = Color(0xFFB0B0B0)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    minLines = 2,
                    colors =
                            OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = CoralRed,
                                    unfocusedBorderColor = Color(0xFFD6D6D6),
                                    cursorColor = CoralRed
                            )
            )

            Spacer(modifier = Modifier.height(20.dp))

            // PINCH History
            OutlinedCard(
                    modifier = Modifier.fillMaxWidth().clickable { /* open PINCH history */ },
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFFD6D6D6))
            ) {
                Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                            Icons.Default.History,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                            text = "PINCH History",
                            fontWeight = FontWeight.Medium,
                            fontSize = 15.sp,
                            modifier = Modifier.weight(1f)
                    )
                    Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = "Open",
                            tint = Color(0xFFBDBDBD),
                            modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
    }

    // Add Allergy Dialog
    if (showAddAllergyDialog) {
        AddItemDialog(
                title = "Add Allergy",
                value = newItemText,
                onValueChange = { newItemText = it },
                onConfirm = {
                    if (newItemText.isNotBlank()) {
                        allergies = allergies + newItemText.trim()
                    }
                    showAddAllergyDialog = false
                },
                onDismiss = { showAddAllergyDialog = false }
        )
    }

    // Add Medication Dialog
    if (showAddMedicationDialog) {
        AddItemDialog(
                title = "Add Medication",
                value = newItemText,
                onValueChange = { newItemText = it },
                onConfirm = {
                    if (newItemText.isNotBlank()) {
                        medications = medications + newItemText.trim()
                    }
                    showAddMedicationDialog = false
                },
                onDismiss = { showAddMedicationDialog = false }
        )
    }

    // Add Accessibility Dialog
    if (showAddAccessibilityDialog) {
        AddItemDialog(
                title = "Add Accessibility Need",
                value = newItemText,
                onValueChange = { newItemText = it },
                onConfirm = {
                    if (newItemText.isNotBlank()) {
                        accessibility = accessibility + newItemText.trim()
                    }
                    showAddAccessibilityDialog = false
                },
                onDismiss = { showAddAccessibilityDialog = false }
        )
    }
}

// ─── Shared Components ────────────────────────────────────────────────────────

@Composable
private fun HealthSection(
        title: String,
        items: List<String>,
        onAddClick: () -> Unit,
        onItemClick: (Int) -> Unit
) {
    // Header row
    Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
        IconButton(onClick = onAddClick, modifier = Modifier.size(28.dp)) {
            Icon(
                    Icons.Default.Add,
                    contentDescription = "Add $title",
                    modifier = Modifier.size(20.dp)
            )
        }
    }

    Spacer(modifier = Modifier.height(8.dp))

    // Item list
    if (items.isNotEmpty()) {
        OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, Color(0xFFD6D6D6))
        ) {
            Column {
                items.forEachIndexed { index, item ->
                    Row(
                            modifier =
                                    Modifier.fillMaxWidth()
                                            .clickable { onItemClick(index) }
                                            .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                                text = item,
                                fontSize = 14.sp,
                                modifier = Modifier.weight(1f)
                        )
                        Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = "Details",
                                tint = Color(0xFFBDBDBD),
                                modifier = Modifier.size(20.dp)
                        )
                    }
                    if (index < items.lastIndex) {
                        HorizontalDivider(
                                color = Color(0xFFEEEEEE),
                                modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AddItemDialog(
        title: String,
        value: String,
        onValueChange: (String) -> Unit,
        onConfirm: () -> Unit,
        onDismiss: () -> Unit
) {
    AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(title) },
            text = {
                OutlinedTextField(
                        value = value,
                        onValueChange = onValueChange,
                        label = { Text("Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                )
            },
            confirmButton = {
                Button(
                        onClick = onConfirm,
                        colors =
                                ButtonDefaults.buttonColors(
                                        containerColor = CoralRed,
                                        contentColor = Color.White
                                )
                ) {
                    Text("Add")
                }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
