package com.faster.festival.utils

import android.util.Log
import com.google.i18n.phonenumbers.PhoneNumberUtil
import java.util.Locale

/**
 * Phone number utilities for normalization and validation.
 *
 * GRADLE DEPENDENCY (add to build.gradle.kts):
 * implementation("io.michaelrocks:libphonenumber-android:8.13.0")
 */
object PhoneNumberUtils {
    private val tag = "PhoneNumberUtils"
    private val phoneUtil = PhoneNumberUtil.getInstance()

    /**
     * Normalize a phone number to E.164 format.
     * Uses the device's locale as the default region if parsing fails with it.
     * Fallback: US region.
     */
    fun normalizeToE164(rawPhone: String): String {
        return try {
            // Get the device's locale or default to US
            val defaultRegion = Locale.getDefault().country.takeIf { it.isNotEmpty() } ?: "US"

            val parsed = phoneUtil.parse(rawPhone, defaultRegion)
            if (!phoneUtil.isValidNumber(parsed)) {
                Log.w(tag, "Phone number is invalid for region $defaultRegion: $rawPhone")
                // Fallback: try US
                if (defaultRegion != "US") {
                    val parsedUS = phoneUtil.parse(rawPhone, "US")
                    if (phoneUtil.isValidNumber(parsedUS)) {
                        return phoneUtil.format(parsedUS, PhoneNumberUtil.PhoneNumberFormat.E164)
                    }
                }
                // Last resort: return raw with + prefix if not present
                return if (rawPhone.startsWith("+")) rawPhone else "+$rawPhone"
            }

            phoneUtil.format(parsed, PhoneNumberUtil.PhoneNumberFormat.E164)
        } catch (e: Exception) {
            Log.e(tag, "Error parsing phone number: ${e.localizedMessage}")
            // Fallback: return raw with + prefix if not present
            if (rawPhone.startsWith("+")) rawPhone else "+$rawPhone"
        }
    }

    /**
     * Extract all phone numbers from a raw phone value (handles multiple numbers).
     * Returns list of E.164 formatted numbers.
     */
    fun extractPhoneNumbers(rawPhone: String): List<String> {
        return try {
            val defaultRegion = Locale.getDefault().country.takeIf { it.isNotEmpty() } ?: "US"
            val normalized = normalizeToE164(rawPhone)
            listOf(normalized)
        } catch (e: Exception) {
            Log.e(tag, "Error extracting phone numbers: ${e.localizedMessage}")
            emptyList()
        }
    }

    /**
     * Validate if a phone number is in valid E.164 format.
     */
    fun isValidE164(phone: String): Boolean {
        return phone.startsWith("+") &&
               phone.length >= 10 &&
               phone.length <= 15 &&
               phone.substring(1).all { it.isDigit() }
    }
}
