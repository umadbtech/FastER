package com.faster.festival.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Utility functions for date/time formatting
 * Uses SimpleDateFormat for API 24 compatibility
 */
object DateFormatter {

    /**
     * Format festival date range from ISO timestamps
     * Example output: "July 21 - 27, 2026"
     *
     * @param startIso ISO 8601 timestamp (e.g., "2026-07-22T16:00:00+00:00")
     * @param endIso ISO 8601 timestamp (e.g., "2026-07-27T03:00:00+00:00")
     * @param timezone IANA timezone (ignored for compatibility, uses system timezone)
     * @return Formatted date range string
     */
    fun formatDateRange(
        startIso: String,
        endIso: String,
        timezone: String = "UTC"
    ): String {
        return try {
            // Parse ISO 8601 format
            val isoFormatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.ENGLISH)
            isoFormatter.isLenient = true

            val startDate = parseIsoDate(startIso) ?: return "Festival Dates"
            val endDate = parseIsoDate(endIso) ?: return "Festival Dates"

            // Format: "July 22"
            val monthDayFormatter = SimpleDateFormat("MMMM dd", Locale.ENGLISH)
            val startFormatted = monthDayFormatter.format(startDate)

            // Format: "27"
            val dayFormatter = SimpleDateFormat("dd", Locale.ENGLISH)
            val endDay = dayFormatter.format(endDate)

            // Format: "2026"
            val yearFormatter = SimpleDateFormat("yyyy", Locale.ENGLISH)
            val year = yearFormatter.format(endDate)

            "$startFormatted - $endDay, $year"
        } catch (e: Exception) {
            // Fallback if parsing fails
            "Festival Dates"
        }
    }

    /**
     * Parse ISO 8601 date string to Date object
     * Handles formats like: 2026-07-22T16:00:00+00:00, 2026-07-22T16:00:00Z
     */
    private fun parseIsoDate(isoString: String): Date? {
        return try {
            // Remove timezone info for SimpleDateFormat compatibility
            val dateString = isoString
                .replace("Z", "")
                .replace(Regex("\\+\\d{2}:\\d{2}$"), "")
                .replace(Regex("\\-\\d{2}:\\d{2}$"), "")

            val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.ENGLISH)
            formatter.isLenient = true
            formatter.parse(dateString)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Format a single ISO date as "dd-MMM-yyyy" (e.g., "03-MAR-2026")
     *
     * @param isoDate ISO 8601 timestamp (e.g., "2026-03-03T16:00:00+00:00")
     * @return Formatted date string like "03-MAR-2026"
     */
    fun formatDateCompact(isoDate: String): String {
        return try {
            val date = parseIsoDate(isoDate) ?: return ""
            val formatter = SimpleDateFormat("dd-MMM-yyyy", Locale.ENGLISH)
            formatter.format(date).uppercase(Locale.ENGLISH)
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * Parse hex color string to Long
     * Handles formats: #00A86B, 00A86B, #00A86BFF, 00A86BFF
     * If null or empty, returns default Navy Blue color (#0B1A4A)
     *
     * @param hexColor Hex color string, or null for default
     * @return Color as Long value
     */
    fun parseHexColor(hexColor: String?): Long {
        // Default to Navy Blue if null or empty
        if (hexColor == null || hexColor.isEmpty() || hexColor == "null") {
            return 0xFF0B1A4AL // Navy Blue default
        }

        return try {
            val hex = hexColor.removePrefix("#")
            when {
                hex.length == 6 -> {
                    // RGB format - add FF alpha
                    0xFF000000L or hex.toLong(16)
                }
                hex.length == 8 -> {
                    // ARGB format
                    hex.toLong(16)
                }
                else -> {
                    0xFF0B1A4AL // Navy Blue fallback for invalid format
                }
            }
        } catch (e: Exception) {
            0xFF0B1A4AL // Navy Blue fallback on error
        }
    }
}
