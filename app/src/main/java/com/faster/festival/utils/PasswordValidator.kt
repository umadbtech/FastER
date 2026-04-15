package com.faster.festival.utils

/**
 * Strong password validation shared across signup, reset password, and
 * any other screen where the user creates or changes a password.
 *
 * Rules (enforced together):
 *   - 8 ≤ length ≤ 16
 *   - ≥ 1 uppercase letter (A–Z)
 *   - ≥ 1 lowercase letter (a–z)
 *   - ≥ 1 digit (0–9)
 *   - ≥ 1 special character
 *
 * Login uses only [isNotEmpty] — existing users may have weaker legacy
 * passwords that predate the new rules.
 */
object PasswordValidator {

    const val MIN_LENGTH = 8
    const val MAX_LENGTH = 16

    private const val SPECIAL_CHARS = "!@#\$%^&*()_+-=[]{};:'\",.<>?/|\\`~"

    /**
     * Run every rule against [password] and return a per-rule result the UI
     * can iterate to render a live requirements list.
     */
    fun validate(password: String): PasswordValidationResult {
        val length = password.length
        return PasswordValidationResult(
            hasValidLength = length in MIN_LENGTH..MAX_LENGTH,
            hasUppercase = password.any { it.isUpperCase() },
            hasLowercase = password.any { it.isLowerCase() },
            hasDigit = password.any { it.isDigit() },
            hasSpecialChar = password.any { it in SPECIAL_CHARS }
        )
    }

    /**
     * Returns a single user-facing error message (the first failing rule)
     * for flows that show one error at a time instead of a requirements list.
     */
    fun firstError(password: String): String? {
        if (password.isEmpty()) return "Password is required"
        if (password.length < MIN_LENGTH) return "Must be at least $MIN_LENGTH characters"
        if (password.length > MAX_LENGTH) return "Must be at most $MAX_LENGTH characters"
        if (!password.any { it.isUpperCase() }) return "Must include an uppercase letter"
        if (!password.any { it.isLowerCase() }) return "Must include a lowercase letter"
        if (!password.any { it.isDigit() }) return "Must include a number"
        if (!password.any { it in SPECIAL_CHARS }) return "Must include a special character"
        return null
    }

    /**
     * Validate that [confirmPassword] matches [password].
     * Returns null when they match, otherwise a user-facing error message.
     */
    fun confirmError(password: String, confirmPassword: String): String? {
        if (confirmPassword.isEmpty()) return "Please confirm your password"
        if (password != confirmPassword) return "Passwords do not match"
        return null
    }

    /**
     * Convenience: does [password] pass every rule?
     */
    fun isValid(password: String): Boolean = validate(password).allValid
}

/**
 * Per-rule validation result. The UI can use these flags to render a
 * requirements checklist that updates live as the user types.
 */
data class PasswordValidationResult(
    val hasValidLength: Boolean,
    val hasUppercase: Boolean,
    val hasLowercase: Boolean,
    val hasDigit: Boolean,
    val hasSpecialChar: Boolean
) {
    val allValid: Boolean
        get() = hasValidLength && hasUppercase && hasLowercase && hasDigit && hasSpecialChar
}
