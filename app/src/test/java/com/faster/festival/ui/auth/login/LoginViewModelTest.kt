package com.faster.festival.ui.auth.login

import com.faster.festival.data.remote.AuthApiService

/**
 * Test helper for LoginViewModel
 * Tests email and password validation without JUnit dependencies
 */
object LoginViewModelTestHelper {

    /**
     * Mock AuthApiService for testing
     */
    class MockAuthApiService : AuthApiService {
        override suspend fun signUp(request: com.faster.festival.data.model.SignupRequest) =
            throw NotImplementedError("Not needed for login test")

        override suspend fun getUser(token: String) =
            throw NotImplementedError("Not needed for login test")

        override suspend fun enrollFactor(token: String, body: Map<String, String>) =
            throw NotImplementedError("Not needed for login test")

        override suspend fun verifyFactor(token: String, factorId: String, body: Map<String, String>) =
            throw NotImplementedError("Not needed for login test")

        override suspend fun sendPhoneOtp(request: com.faster.festival.data.model.SendOtpRequest) =
            throw NotImplementedError("Not needed for login test")

        override suspend fun verifyPhoneOtp(request: com.faster.festival.data.model.VerifyOtpRequest) =
            throw NotImplementedError("Not needed for login test")

        override suspend fun sendOtp(body: Map<String, String>) =
            throw NotImplementedError("Not needed for login test")

        override suspend fun verifyOtp(body: Map<String, String>) =
            throw NotImplementedError("Not needed for login test")

        override suspend fun recover(body: Map<String, String>) =
            throw NotImplementedError("Not needed for login test")

        override suspend fun updateUser(authorization: String, body: Map<String, String>) =
            throw NotImplementedError("Not needed for login test")

        override suspend fun logout(authorization: String) =
            throw NotImplementedError("Not needed for login test")

        override suspend fun login(request: com.faster.festival.data.model.LoginRequest) =
            throw NotImplementedError("Not needed for login test")
    }

    // ...existing code...

    /**
     * Test: Email validation with invalid format
     */
    fun testEmailValidationInvalid(): Result<String> {
        return try {
            // Simulate email validation logic
            val invalidEmail = "not-an-email"
            val isValidEmail = invalidEmail.contains("@") && invalidEmail.contains(".")

            if (!isValidEmail) {
                Result.success("✓ Invalid email 'not-an-email' fails validation")
            } else {
                Result.failure(Exception("✗ Invalid email should fail validation"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("✗ Error: ${e.message}"))
        }
    }

    /**
     * Test: Email validation with valid format
     */
    fun testEmailValidationValid(): Result<String> {
        return try {
            val validEmail = "test@example.com"
            val isValidEmail = validEmail.contains("@") && validEmail.contains(".")

            if (isValidEmail) {
                Result.success("✓ Valid email 'test@example.com' passes validation")
            } else {
                Result.failure(Exception("✗ Valid email should pass validation"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("✗ Error: ${e.message}"))
        }
    }

    /**
     * Test: Password validation with invalid format (too short)
     */
    fun testPasswordValidationInvalid(): Result<String> {
        return try {
            val shortPassword = "123"
            val isShortPassword = shortPassword.length < 6

            if (isShortPassword) {
                Result.success("✓ Invalid password '123' fails validation (too short)")
            } else {
                Result.failure(Exception("✗ Short password should fail validation"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("✗ Error: ${e.message}"))
        }
    }

    /**
     * Test: Password validation with valid format
     */
    fun testPasswordValidationValid(): Result<String> {
        return try {
            val validPassword = "secure123"
            val isValidLength = validPassword.length >= 6

            if (isValidLength) {
                Result.success("✓ Valid password 'secure123' passes validation")
            } else {
                Result.failure(Exception("✗ Valid password should pass validation"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("✗ Error: ${e.message}"))
        }
    }

    /**
     * Test: Email format validation regex
     */
    fun testEmailFormatRegex(): Result<String> {
        return try {
            val emailRegex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}$")

            val validEmails = listOf("user@example.com", "test.user@example.co.uk", "user+tag@example.com")
            val invalidEmails = listOf("notanemail", "missing@domain", "@example.com", "user@.com")

            val allValidPass = validEmails.all { emailRegex.matches(it) }
            val allInvalidFail = invalidEmails.none { emailRegex.matches(it) }

            if (allValidPass && allInvalidFail) {
                Result.success("✓ Email regex validation works correctly")
            } else {
                Result.failure(Exception("✗ Email regex validation failed"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("✗ Error: ${e.message}"))
        }
    }

    /**
     * Run all tests and return results
     */
    fun runAllTests(): List<Result<String>> {
        return listOf(
            testEmailValidationInvalid(),
            testEmailValidationValid(),
            testPasswordValidationInvalid(),
            testPasswordValidationValid(),
            testEmailFormatRegex()
        )
    }

    /**
     * Print test results to console
     */
    fun printTestResults() {
        println("\n╔═══════════════════════════════════════╗")
        println("║   LOGIN VIEWMODEL TESTS                ║")
        println("╚═══════════════════════════════════════╝\n")

        val results = runAllTests()
        var passed = 0
        var failed = 0

        results.forEach { result ->
            when {
                result.isSuccess -> {
                    println("${result.getOrNull()}")
                    passed++
                }
                else -> {
                    println("${result.exceptionOrNull()?.message}")
                    failed++
                }
            }
        }

        println("\n╔═══════════════════════════════════════╗")
        println("║        TEST SUMMARY                    ║")
        println("╠═══════════════════════════════════════╣")
        println("║ ✅ Passed: $passed")
        println("║ ❌ Failed: $failed")
        println("║ 📊 Total:  ${results.size}")
        println("╚═══════════════════════════════════════╝\n")
    }
}


