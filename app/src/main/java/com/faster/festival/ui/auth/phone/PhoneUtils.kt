package com.faster.festival.ui.auth.phone

/**
 * Shared phone utilities and small country list used by phone UI screens.
 */
data class Country(val name: String, val code: String, val dialCode: String)

val commonCountries = listOf(
    Country("United States", "US", "+1"),
    Country("India", "IN", "+91"),
    Country("United Kingdom", "GB", "+44"),
    Country("Canada", "CA", "+1"),
    Country("Australia", "AU", "+61")
)
