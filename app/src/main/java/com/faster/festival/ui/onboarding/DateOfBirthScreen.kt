package com.faster.festival.ui.onboarding

import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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

    Log.d(TAG, "DateOfBirthScreen rendered - dateOfBirth: ${formState.dateOfBirth}")

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

    // Main UI - ensure it's visible
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start
        ) {
            // Heading section with proper spacing
            Text(
                text = "Date of Birth",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.fillMaxWidth()
            )

            // Spacing between heading and description
            Box(modifier = Modifier.height(8.dp))

            // Description/subtitle with better hierarchy
            Text(
                text = "We use your date of birth to locate you in a crowd in case of an emergency.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth()
            )

            // Major spacing before the input field
            Box(modifier = Modifier.height(32.dp))

            // Date picker field container - Material 3 styled
            RefactoredBirthdayPickerField(
                selectedDate = formState.dateOfBirth,
                isError = formState.dobError != null || hasError,
                isShaking = isShaking,
                onDatePickerClick = {
                    Log.d(TAG, "Date picker field clicked")
                    openDatePicker()
                },
                onClearClick = {
                    onDateChange("")
                    hasError = false
                    Log.d(TAG, "Date cleared")
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                },
                modifier = Modifier.fillMaxWidth(),
                errorMessage = formState.dobError
            )

            // Spacing before requirements box
            Box(modifier = Modifier.height(32.dp))

            // Requirements/Helper box with improved styling
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(20.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Requirements",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    RequirementItem("Must be at least 13 years old")
                    RequirementItem("Date range: 1900 – ${today.get(Calendar.YEAR)}")
                    RequirementItem("Tap the date field to open the calendar picker")
                }
            }

            // Flexible spacing to push content up
            Box(modifier = Modifier.weight(1f))
        }
    }
}

/**
 * Individual requirement item with checkmark icon
 */
@Composable
private fun RequirementItem(text: String) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "✓",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * Modern Material 3 Date of Birth picker field with improved spacing and visual design.
 * Features:
 * - Outlined text field style with animated border color
 * - Proper visual hierarchy with label, value, and helper text
 * - Trailing clear button (only visible when date is selected)
 * - Smooth animations on error state
 * - No text overlap, proper spacing throughout
 */
@Composable
private fun RefactoredBirthdayPickerField(
    selectedDate: String,
    isError: Boolean,
    isShaking: Boolean,
    onDatePickerClick: () -> Unit,
    onClearClick: () -> Unit,
    modifier: Modifier = Modifier,
    errorMessage: String? = null
) {
    val interactionSource = remember { MutableInteractionSource() }

    // Format date for display (DD/MM/YYYY)
    val displayDate = remember(selectedDate) {
        if (selectedDate.isNotEmpty()) {
            try {
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val date = sdf.parse(selectedDate)
                if (date != null) {
                    val displayFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    displayFormat.format(date)
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }

    // Subtle shake offset for error feedback
    val shakeOffset = if (isShaking) 3.dp else 0.dp

    // Animated colors for error state
    val borderColor by animateColorAsState(
        targetValue = if (isError)
            MaterialTheme.colorScheme.error
        else
            MaterialTheme.colorScheme.outline,
        label = "borderColor"
    )

    val containerColor by animateColorAsState(
        targetValue = if (isError)
            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.08f)
        else
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        label = "containerColor"
    )

    val iconColor by animateColorAsState(
        targetValue = if (isError)
            MaterialTheme.colorScheme.error
        else
            MaterialTheme.colorScheme.primary,
        label = "iconColor"
    )

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Main picker field container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .padding(horizontal = shakeOffset)
                .border(
                    width = 1.5.dp,
                    color = borderColor,
                    shape = RoundedCornerShape(14.dp)
                )
                .background(
                    color = containerColor,
                    shape = RoundedCornerShape(14.dp)
                )
                .clip(RoundedCornerShape(14.dp))
                .clickable(
                    interactionSource = interactionSource,
                    indication = rememberRipple(bounded = true),
                    onClick = onDatePickerClick
                )
                .padding(horizontal = 20.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left side: Icon and text content
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    // Calendar icon
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = "Calendar",
                        tint = iconColor,
                        modifier = Modifier.size(28.dp)
                    )

                    // Text content: label and date
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 4.dp)
                    ) {
                        // Label text
                        Text(
                            text = "Date of Birth",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1
                        )

                        // Selected date or placeholder
                        Text(
                            text = displayDate ?: "Select your birthday",
                            style = MaterialTheme.typography.titleMedium,
                            color = if (displayDate != null)
                                MaterialTheme.colorScheme.onBackground
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (displayDate != null) FontWeight.SemiBold else FontWeight.Normal,
                            maxLines = 1,
                            fontSize = 16.sp
                        )
                    }
                }

                // Right side: Clear button (conditional)
                if (displayDate != null) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = rememberRipple(bounded = true, radius = 22.dp),
                                onClick = onClearClick
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear date",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }

        // Error message (below field, separate line, no overlap)
        if (isError && errorMessage != null) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 20.dp, top = 4.dp)
            )
        }

        // Helper/hint text (below error or field)
        if (!isError && displayDate != null) {
            Text(
                text = "Tap to change",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 11.sp,
                modifier = Modifier.padding(start = 20.dp, top = 4.dp)
            )
        }
    }
}


