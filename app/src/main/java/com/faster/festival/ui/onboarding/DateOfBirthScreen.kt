package com.faster.festival.ui.onboarding

import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.MaterialDatePicker
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private const val TAG = "DateOfBirthScreen"
private const val DATE_PICKER_TAG = "material_date_picker_dob"

/**
 * Helper function to safely unwrap Context and find FragmentActivity.
 * Handles ContextWrapper by recursively unwrapping until we reach the actual Activity.
 */
private fun findFragmentActivity(context: Context): FragmentActivity? {
    var current = context
    while (current is ContextWrapper) {
        if (current is FragmentActivity) {
            Log.d(TAG, "FragmentActivity found: ${current.javaClass.simpleName}")
            return current
        }
        current = current.baseContext
    }
    Log.d(TAG, "No FragmentActivity found. Final context type: ${current.javaClass.simpleName}")
    return null
}

/**
 * Screen 1: Modern Material Design 3 Date of Birth picker.
 *
 * Features:
 * - Uses Material Components MaterialDatePicker (stable, production-ready)
 * - Date range: 1900 to today (no future dates allowed)
 * - Beautiful formatted date display (DD/MM/YYYY)
 * - Clear/Reset button with haptic feedback
 * - Shake animation on validation error
 * - Proper lifecycle & FragmentManager handling
 * - Material 3 theming
 */
@Composable
fun DateOfBirthScreen(
    formState: OnboardingFormState,
    onDateChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var isShaking by remember { mutableStateOf(false) }
    var hasError by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current

    // Safely find FragmentActivity by unwrapping ContextWrapper
    val fragmentActivity = remember {
        findFragmentActivity(context)
    }
    val fragmentManager = remember {
        fragmentActivity?.supportFragmentManager
    }

    // Calculate date limits
    val today = Calendar.getInstance()
    val defaultSelectionCalendar = Calendar.getInstance().apply {
        add(Calendar.YEAR, -25)
    }
    val defaultSelection = defaultSelectionCalendar.timeInMillis

    // Parse existing DOB from formState to use as picker selection if available
    val selectedDateMillis = remember(formState.dateOfBirth) {
        if (formState.dateOfBirth.isNotEmpty()) {
            try {
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                sdf.parse(formState.dateOfBirth)?.time
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing existing date: ${formState.dateOfBirth}", e)
                null
            }
        } else {
            null
        }
    }

    // Function to open MaterialDatePicker reliably
    val openDatePicker: () -> Unit = lambda@{
        if (fragmentManager == null) {
            val activityInfo = if (fragmentActivity != null) {
                "${fragmentActivity.javaClass.simpleName} (no supportFragmentManager)"
            } else {
                "No FragmentActivity found. Context type: ${context.javaClass.simpleName}"
            }
            Log.e(TAG, "FragmentManager not available - $activityInfo")
            hasError = true
            isShaking = true
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            return@lambda
        }

        try {
            // Prevent duplicate picker dialogs
            if (fragmentManager.findFragmentByTag(DATE_PICKER_TAG) != null) {
                Log.w(TAG, "Date picker already shown, ignoring duplicate request")
                return@lambda
            }

            val constraints = CalendarConstraints.Builder()
                .setStart(Calendar.getInstance().apply { set(Calendar.YEAR, 1900) }.timeInMillis)
                .setEnd(today.timeInMillis)
                .build()

            val picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select Date of Birth")
                .setSelection(selectedDateMillis ?: defaultSelection)
                .setCalendarConstraints(constraints)
                .build()

            // Handle positive button (date selected)
            picker.addOnPositiveButtonClickListener { pickedMillis ->
                try {
                    val calendar = Calendar.getInstance().apply {
                        timeInMillis = pickedMillis
                    }

                    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val formattedDate = sdf.format(calendar.time)
                    onDateChange(formattedDate)

                    hasError = false
                    isShaking = false
                    Log.d(TAG, "Date selected: $formattedDate")

                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing date selection", e)
                    hasError = true
                    isShaking = true
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }
            }

            // Handle cancellation
            picker.addOnDismissListener {
                Log.d(TAG, "Date picker dismissed")
            }

            picker.addOnCancelListener {
                Log.d(TAG, "Date picker cancelled")
            }

            // Show picker with unique tag to prevent duplicates
            picker.show(fragmentManager, DATE_PICKER_TAG)
            Log.d(TAG, "MaterialDatePicker shown successfully")

        } catch (e: IllegalStateException) {
            Log.e(TAG, "IllegalStateException: Activity may not be in proper state", e)
            hasError = true
            isShaking = true
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error opening date picker", e)
            hasError = true
            isShaking = true
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top spacing
        Box(modifier = Modifier.height(16.dp))

        // Heading
        Text(
            text = "Date of Birth",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Subheading
        Text(
            text = "We use your date of birth to locate you in a crowd in case of an emergency.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        // Modern clickable date picker field
        BirthdayPickerField(
            selectedDate = formState.dateOfBirth,
            isError = hasError,
            isShaking = isShaking,
            onDatePickerClick = {
                Log.d(TAG, "Date picker field clicked")
                openDatePicker()
            },
            onClearClick = {
                onDateChange("")
                hasError = false
                isShaking = false
                Log.d(TAG, "Date cleared")
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
        )

        // Error message with animation
        if (hasError) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .background(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(12.dp)
            ) {
                Text(
                    text = formState.dobError ?: "Please select a valid birth date",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Helper text box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
                .background(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(12.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "✓ Must be at least 13 years old",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = "✓ Date range: 1900 - ${today.get(Calendar.YEAR)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = "✓ Tap the date field to open the calendar picker",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
        }

        Box(modifier = Modifier.weight(1f))
    }
}

/**
 * Modern clickable birthday picker field with Material 3 design.
 * Shows formatted date (DD/MM/YYYY) or "Select Birthday" placeholder.
 */
@Composable
private fun BirthdayPickerField(
    selectedDate: String,
    isError: Boolean,
    isShaking: Boolean,
    onDatePickerClick: () -> Unit,
    onClearClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }

    // Format date for display
    val displayDate = remember(selectedDate) {
        if (selectedDate.isNotEmpty()) {
            try {
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val date = sdf.parse(selectedDate)
                if (date != null) {
                    val displayFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    displayFormat.format(date)
                } else {
                    "Invalid date"
                }
            } catch (e: Exception) {
                "Invalid date"
            }
        } else {
            null
        }
    }

    // Shake offset for error state
    val shakeOffset = if (isShaking) 8.dp else 0.dp

    // Color animation for error state
    val containerColor by animateColorAsState(
        targetValue = if (isError)
            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.12f)
        else
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        label = "containerColor"
    )


    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = shakeOffset)
            .height(56.dp)
            .background(
                color = containerColor,
                shape = RoundedCornerShape(12.dp)
            )
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = rememberRipple(bounded = true),
                onClick = onDatePickerClick
            ),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Icon and text
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = "Calendar",
                    tint = if (isError) MaterialTheme.colorScheme.error
                           else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )

                Column {
                    Text(
                        text = "Date of Birth",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                    Text(
                        text = displayDate ?: "Select Birthday",
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (displayDate != null) MaterialTheme.colorScheme.onBackground
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (displayDate != null) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }

            // Clear button (only show when date is selected)
            if (displayDate != null) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(50))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = rememberRipple(bounded = true, radius = 20.dp),
                            onClick = onClearClick
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Clear",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
