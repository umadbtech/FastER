package com.faster.festival.utils

import android.util.Log
import com.google.i18n.phonenumbers.NumberParseException
import com.google.i18n.phonenumbers.PhoneNumberUtil
import java.util.Locale

/**
 * Phone number utilities for normalization and validation.
 * Uses Google's libphonenumber for robust E.164 parsing.
 */
object PhoneNumberUtils {
    private const val TAG = "PhoneNumberUtils"
    private val phoneUtil: PhoneNumberUtil = PhoneNumberUtil.getInstance()

    /**
     * Result of a phone number validation attempt.
     */
    sealed class ValidationResult {
        object Valid : ValidationResult()
        data class Invalid(val errorMessage: String) : ValidationResult()
    }

    /**
     * Strip all formatting characters except leading `+` and digits.
     * Used inside `onValueChange` to filter TextField input.
     */
    fun sanitizeInput(raw: String): String {
        if (raw.isEmpty()) return ""
        val hasPlus = raw.trimStart().startsWith("+")
        val digits = raw.filter { it.isDigit() }
        return if (hasPlus) "+$digits" else digits
    }

    /**
     * Validate a phone number for UI display (live validation as user types).
     * Returns a structured result with a user-friendly error message.
     *
     * Rules:
     * - Accepts optional leading `+`
     * - After sanitization, must be 7–15 digits (ITU-T E.164 range)
     * - If starts with `+`, must parse via libphonenumber as valid international number
     * - If no `+`, parses against the device's default region
     */
    fun validate(rawPhone: String): ValidationResult {
        val trimmed = rawPhone.trim()
        if (trimmed.isBlank()) {
            return ValidationResult.Invalid("Phone number is required")
        }

        val sanitized = sanitizeInput(trimmed)
        val digitCount = sanitized.count { it.isDigit() }

        if (digitCount < 7) {
            return ValidationResult.Invalid("Phone number is too short")
        }
        if (digitCount > 15) {
            return ValidationResult.Invalid("Phone number is too long")
        }

        return try {
            val defaultRegion = Locale.getDefault().country.takeIf { it.isNotEmpty() } ?: "US"
            val parsed = phoneUtil.parse(sanitized, defaultRegion)
            if (phoneUtil.isValidNumber(parsed)) {
                ValidationResult.Valid
            } else {
                // Retry as US for common case
                if (defaultRegion != "US") {
                    val parsedUs = phoneUtil.parse(sanitized, "US")
                    if (phoneUtil.isValidNumber(parsedUs)) {
                        return ValidationResult.Valid
                    }
                }
                ValidationResult.Invalid("Enter a valid phone number")
            }
        } catch (e: NumberParseException) {
            Log.w(TAG, "Parse failed for '$sanitized': ${e.errorType}")
            ValidationResult.Invalid("Enter a valid phone number. Include country code (e.g. +1)")
        } catch (e: Exception) {
            Log.w(TAG, "Unexpected error validating phone: ${e.message}")
            ValidationResult.Invalid("Enter a valid phone number")
        }
    }

    /**
     * Normalize a phone number to E.164 format.
     * Uses the device's locale as the default region if parsing fails.
     */
    fun normalizeToE164(rawPhone: String): String {
        val sanitized = sanitizeInput(rawPhone.trim())
        return try {
            val defaultRegion = Locale.getDefault().country.takeIf { it.isNotEmpty() } ?: "US"
            val parsed = phoneUtil.parse(sanitized, defaultRegion)
            if (!phoneUtil.isValidNumber(parsed)) {
                Log.w(TAG, "Phone number is invalid for region $defaultRegion: $sanitized")
                if (defaultRegion != "US") {
                    val parsedUS = phoneUtil.parse(sanitized, "US")
                    if (phoneUtil.isValidNumber(parsedUS)) {
                        return phoneUtil.format(parsedUS, PhoneNumberUtil.PhoneNumberFormat.E164)
                    }
                }
                return if (sanitized.startsWith("+")) sanitized else "+$sanitized"
            }
            phoneUtil.format(parsed, PhoneNumberUtil.PhoneNumberFormat.E164)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing phone number: ${e.localizedMessage}")
            if (sanitized.startsWith("+")) sanitized else "+$sanitized"
        }
    }

    /**
     * Extract all phone numbers from a raw phone value (handles multiple numbers).
     */
    fun extractPhoneNumbers(rawPhone: String): List<String> {
        return try {
            val normalized = normalizeToE164(rawPhone)
            listOf(normalized)
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting phone numbers: ${e.localizedMessage}")
            emptyList()
        }
    }

    /**
     * Validate strict E.164 format (used for backend payloads).
     * E.164: starts with +, followed by 7–15 digits, no other characters.
     */
    fun isValidE164(phone: String): Boolean {
        return phone.startsWith("+") &&
                phone.length in 8..16 &&
                phone.substring(1).all { it.isDigit() }
    }
}
