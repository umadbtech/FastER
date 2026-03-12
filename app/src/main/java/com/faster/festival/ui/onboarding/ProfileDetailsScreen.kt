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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DisplayMode
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.faster.festival.data.model.GenderIdentity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/**
 * Profile Details screen (Step 1 of onboarding).
 *
 * Collects:
 * - Date of Birth (via Compose M3 date picker)
 * - Gender Identity (dropdown)
 */
@Composable
fun ProfileDetailsScreen(
    dateOfBirth: String,
    genderIdentity: String,
    dateOfBirthError: String?,
    onDateOfBirthChange: (String) -> Unit,
    onGenderIdentityChange: (String) -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val genderOptions = GenderIdentity.displayLabels

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 24.dp)
    ) {
        // Section header with info icon
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Profile Details",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            IconButton(onClick = { /* TODO: show info tooltip */ }) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Info",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // ---- Date of Birth ----
        Text(
            text = "Date of Birth",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        DateOfBirthField(
            selectedDate = dateOfBirth,
            isError = dateOfBirthError != null,
            errorMessage = dateOfBirthError,
            onDateSelected = onDateOfBirthChange
        )

        Spacer(modifier = Modifier.height(24.dp))

        // ---- Gender Identity ----
        Text(
            text = "Gender Identity",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        GenderIdentityDropdown(
            selectedGender = genderIdentity,
            genderOptions = genderOptions,
            onGenderSelected = onGenderIdentityChange
        )

        Spacer(modifier = Modifier.weight(1f))
        Spacer(modifier = Modifier.height(32.dp))

        // ---- Continue Button ----
        Button(
            onClick = onContinue,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Text(
                text = "Continue",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

// ============================================================
// Date of Birth picker field (Compose Material 3)
// ============================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateOfBirthField(
    selectedDate: String,
    isError: Boolean,
    errorMessage: String?,
    onDateSelected: (String) -> Unit
) {
    var showDatePicker by remember { mutableStateOf(false) }

    val displayDate = remember(selectedDate) {
        if (selectedDate.isNotEmpty()) {
            try {
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val date = sdf.parse(selectedDate)
                if (date != null) {
                    SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(date)
                } else null
            } catch (e: Exception) {
                null
            }
        } else null
    }

    val initialMillis = remember(selectedDate) {
        if (selectedDate.isNotEmpty()) {
            try {
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
                sdf.parse(selectedDate)?.time
            } catch (e: Exception) {
                null
            }
        } else null
    }

    Column {
        OutlinedTextField(
            value = displayDate ?: "",
            onValueChange = {},
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { showDatePicker = true },
            placeholder = { Text("Select your date of birth") },
            readOnly = true,
            enabled = false,
            singleLine = true,
            isError = isError,
            shape = RoundedCornerShape(12.dp),
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = "Select date",
                    tint = if (isError) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.primary
                )
            },
            colors = OutlinedTextFieldDefaults.colors(
                disabledTextColor = MaterialTheme.colorScheme.onBackground,
                disabledBorderColor = if (isError) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.outline,
                disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledTrailingIconColor = if (isError) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.primary
            )
        )

        if (isError && errorMessage != null) {
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
        }
    }

    if (showDatePicker) {
        val todayMillis = System.currentTimeMillis()
        val defaultMillis = Calendar.getInstance().apply { add(Calendar.YEAR, -25) }
            .timeInMillis
        val minMillis = Calendar.getInstance().apply {
            set(Calendar.YEAR, 1900)
            set(Calendar.MONTH, Calendar.JANUARY)
            set(Calendar.DAY_OF_MONTH, 1)
        }.timeInMillis

        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = initialMillis ?: defaultMillis,
            initialDisplayMode = DisplayMode.Picker,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    return utcTimeMillis in minMillis..todayMillis
                }

                override fun isSelectableYear(year: Int): Boolean {
                    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
                    return year in 1900..currentYear
                }
            }
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
                                timeZone = TimeZone.getTimeZone("UTC")
                            }
                            onDateSelected(sdf.format(millis))
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(
                state = datePickerState,
                title = {
                    Text(
                        text = "Select Date of Birth",
                        modifier = Modifier.padding(start = 24.dp, end = 12.dp, top = 16.dp),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            )
        }
    }
}

// ============================================================
// Gender Identity dropdown
// ============================================================

@Composable
private fun GenderIdentityDropdown(
    selectedGender: String,
    genderOptions: List<String>,
    onGenderSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = selectedGender,
            onValueChange = {},
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { expanded = true },
            placeholder = { Text("Select gender identity") },
            readOnly = true,
            enabled = false,
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            colors = OutlinedTextFieldDefaults.colors(
                disabledTextColor = MaterialTheme.colorScheme.onBackground,
                disabledBorderColor = MaterialTheme.colorScheme.outline,
                disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth(0.9f)
        ) {
            genderOptions.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = option,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            if (option == selectedGender) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    },
                    onClick = {
                        onGenderSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}
