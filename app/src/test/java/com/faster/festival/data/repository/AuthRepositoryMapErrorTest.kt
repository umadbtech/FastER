package com.faster.festival.data.repository

/**
 * Test helper for AuthRepository error mapping
 *
 * Tests the mapLoginError() static method which converts HTTP error responses
 * to user-friendly error messages
 *
 * Note: To enable actual unit tests, add these dependencies to build.gradle.kts:
 *   testImplementation(libs.junit)
 *   androidTestImplementation(libs.androidx.test.ext.junit)
 *   androidTestImplementation(libs.androidx.test.runner)
 *
 * Then convert the helper functions to @Test methods
 */
object AuthRepositoryMapErrorTestHelper {

    /**
     * Test: Invalid credentials (401) maps to friendly message
     *
     * @return Result indicating pass/fail
     */
    fun testMapInvalidCredentials(): Result<String> {
        return try {
            val body = "{\"msg\": \"Invalid credentials\"}"
            val mapped = AuthRepository.mapLoginError(401, body)

            if (mapped.contains("Invalid email or password") || mapped.contains("Invalid credentials")) {
                Result.success("✓ 401 invalid credentials maps to friendly message: $mapped")
            } else {
                Result.failure(Exception("✗ Expected friendly invalid credentials message, got: $mapped"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("✗ Error testing invalid credentials: ${e.message}"))
        }
    }

    /**
     * Test: Rate limit (429) maps to friendly message
     *
     * @return Result indicating pass/fail
     */
    fun testMapRateLimit(): Result<String> {
        return try {
            val mapped = AuthRepository.mapLoginError(429, "{\"msg\": \"Rate limit\"}")
            val expected = "Too many requests. Please try again later."

            if (mapped == expected) {
                Result.success("✓ 429 rate limit maps to friendly message: $mapped")
            } else {
                Result.failure(Exception("✗ Expected '$expected', got: '$mapped'"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("✗ Error testing rate limit: ${e.message}"))
        }
    }

    /**
     * Test: Unknown error (500) includes status code
     *
     * @return Result indicating pass/fail
     */
    fun testMapUnknownError(): Result<String> {
        return try {
            val mapped = AuthRepository.mapLoginError(500, null)

            if (mapped.contains("Login failed: 500")) {
                Result.success("✓ 500 error includes status code: $mapped")
            } else {
                Result.failure(Exception("✗ Expected error message with 'Login failed: 500', got: $mapped"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("✗ Error testing unknown error: ${e.message}"))
        }
    }

    /**
     * Run all tests and return results
     */
    fun runAllTests(): List<Result<String>> {
        return listOf(
            testMapInvalidCredentials(),
            testMapRateLimit(),
            testMapUnknownError()
        )
    }
}
