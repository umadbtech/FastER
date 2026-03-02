package com.faster.festival.ui.theme

import androidx.compose.ui.graphics.Color

// ============================================================================
// COLOR TOKENS - Centralized Color System
// ============================================================================

// ============================================================================
// PRIMARY PALETTE - FastER Navy
// ============================================================================
val NavyBlue = Color(0xFF0B1A4A) // Primary
val NavyBlueDark = Color(0xFF050D25) // Darker variant
val NavyBlueLight = Color(0xFF1A3A8A) // Lighter variant

// ============================================================================
// SECONDARY COLORS - Emergency Red
// ============================================================================
val EmergencyRed = Color(0xFFFF1E1E) // Secondary / Action
val EmergencyRedDark = Color(0xFFCC0000) // Darker variant
val EmergencyRedLight = Color(0xFFFF4C4C) // Lighter variant

// ============================================================================
// NEUTRAL COLORS
// ============================================================================
val Black = Color(0xFF000000)
val White = Color(0xFFFFFFFF)
val DarkSurface = Color(0xFF121212)
val Grey800 = Color(0xFF424242)
val Grey400 = Color(0xFFBDBDBD)
val Grey = Color(0xFF808080) // Requested Text Color

// ============================================================================
// SEMANTIC COLORS
// ============================================================================
val ErrorRed = Color(0xFFFF1E1E)
val SuccessGreen = Color(0xFF16A34A)
val WarningAmber = Color(0xFFFFC107)

// ============================================================================
// MATERIAL 3 COLOR SCHEME MAPPING
// ============================================================================

// Light Theme (Day)
val md_theme_light_primary = NavyBlue
val md_theme_light_onPrimary = White
val md_theme_light_primaryContainer = NavyBlueLight // Lighter for container
val md_theme_light_onPrimaryContainer = White

val md_theme_light_secondary = EmergencyRed
val md_theme_light_onSecondary = White
val md_theme_light_secondaryContainer = EmergencyRedLight
val md_theme_light_onSecondaryContainer = White

val md_theme_light_tertiary = NavyBlueLight
val md_theme_light_onTertiary = White
val md_theme_light_tertiaryContainer = NavyBlueDark
val md_theme_light_onTertiaryContainer = White

val md_theme_light_error = ErrorRed
val md_theme_light_onError = White
val md_theme_light_errorContainer = EmergencyRedLight
val md_theme_light_onErrorContainer = White

val md_theme_light_background = White // Requested: White
val md_theme_light_onBackground = Grey // Requested: Grey (#808080)
val md_theme_light_surface = White
val md_theme_light_onSurface = Black
val md_theme_light_surfaceVariant = Color(0xFFE0E0E0) // Light Grey for refinement
val md_theme_light_onSurfaceVariant = Grey800
val md_theme_light_outline = Grey800

// Dark Theme (Night) - Proper dark mode with high contrast
val md_theme_dark_primary = NavyBlue
val md_theme_dark_onPrimary = White
val md_theme_dark_primaryContainer = Color(0xFF050D25) // Very dark navy
val md_theme_dark_onPrimaryContainer = White

val md_theme_dark_secondary = EmergencyRed
val md_theme_dark_onSecondary = White
val md_theme_dark_secondaryContainer = Color(0xFF800000) // Very dark red
val md_theme_dark_onSecondaryContainer = White

val md_theme_dark_tertiary = NavyBlueLight
val md_theme_dark_onTertiary = White
val md_theme_dark_tertiaryContainer = Color(0xFF050D25)
val md_theme_dark_onTertiaryContainer = White

val md_theme_dark_error = EmergencyRedLight
val md_theme_dark_onError = Color(0xFF600000)
val md_theme_dark_errorContainer = Color(0xFF8C0000)
val md_theme_dark_onErrorContainer = White

val md_theme_dark_background = Color(0xFF0A0A0A) // Pure black for dark mode
val md_theme_dark_onBackground = Color(0xFFFFFFFF) // Pure white text
val md_theme_dark_surface = Color(0xFF1A1A1A) // Slightly lighter black for surfaces
val md_theme_dark_onSurface = Color(0xFFFFFFFF) // Pure white
val md_theme_dark_surfaceVariant = Color(0xFF2A2A2A) // Dark gray
val md_theme_dark_onSurfaceVariant = Color(0xFFE0E0E0) // Light gray text
val md_theme_dark_outline = Color(0xFF808080)
val md_theme_dark_scrim = Color(0xFF000000)
