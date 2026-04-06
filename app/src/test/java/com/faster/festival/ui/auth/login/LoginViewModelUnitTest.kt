package com.faster.festival.ui.auth.login

import kotlinx.coroutines.runBlocking
import com.faster.festival.data.model.LoginResponse
import com.faster.festival.data.model.User

/**
 * Fake AuthRepository for testing LoginViewModel
 * Implements AuthRepositoryContract interface
 */
class FakeAuthRepoForVm(
    private val savedEmail: String? = null,
    private val loginResult: Result<LoginResponse>? = null
) : com.faster.festival.data.repository.AuthRepositoryContract {
    override fun getSavedEmail(): String? = savedEmail

    override suspend fun login(email: String, password: String): Result<LoginResponse> {
        return loginResult ?: Result.failure(Exception("not configured"))
    }
}

/**
 * Test helper for LoginViewModel
 * Tests prefill email, validation logic, and login state transitions
 */
object LoginViewModelUnitTestHelper {

    /**
     * Test: Prefill email is applied from saved state
     */
    fun testPrefillEmail(): Result<String> {
        return try {
            runBlocking {
                val repo = FakeAuthRepoForVm(savedEmail = "prefill@example.com")
                val vm = LoginViewModel(repo)
                kotlinx.coroutines.delay(50)

                val email = vm.formState.value.email
                if (email == "prefill@example.com") {
                    Result.success("✓ Prefill email 'prefill@example.com' is applied correctly")
                } else {
                    Result.failure(Exception("✗ Expected 'prefill@example.com', got '$email'"))
                }
            }
        } catch (e: Exception) {
            Result.failure(Exception("✗ Error: ${e.message}"))
        }
    }

    /**
     * Test: Email validation logic
     */
    fun testEmailValidation(): Result<String> {
        return try {
            val repo = FakeAuthRepoForVm()
            val vm = LoginViewModel(repo)

            // Test invalid email
            vm.onEmailChange("bad")
            val emailError1 = vm.formState.value.emailError

            // Test valid email
            vm.onEmailChange("ok@example.com")
            val emailError2 = vm.formState.value.emailError

            if (emailError1 != null && emailError2 == null) {
                Result.success("✓ Email validation logic works: 'bad' fails, 'ok@example.com' passes")
            } else {
                Result.failure(Exception("✗ Email validation failed. Invalid error: $emailError1, Valid error: $emailError2"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("✗ Error: ${e.message}"))
        }
    }

    /**
     * Test: Password validation logic
     */
    fun testPasswordValidation(): Result<String> {
        return try {
            val repo = FakeAuthRepoForVm()
            val vm = LoginViewModel(repo)

            // Test invalid password
            vm.onPasswordChange("123")
            val passwordError1 = vm.formState.value.passwordError

            // Test valid password
            vm.onPasswordChange("password123")
            val passwordError2 = vm.formState.value.passwordError

            if (passwordError1 != null && passwordError2 == null) {
                Result.success("✓ Password validation logic works: '123' fails, 'password123' passes")
            } else {
                Result.failure(Exception("✗ Password validation failed. Invalid error: $passwordError1, Valid error: $passwordError2"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("✗ Error: ${e.message}"))
        }
    }

    /**
     * Test: Login success updates UI state
     */
    fun testLoginSuccessState(): Result<String> {
        return try {
            runBlocking {
                val successResp = LoginResponse(
                    accessToken = "a",
                    refreshToken = "r",
                    user = User(id = "u", email = "u@e.com")
                )
                val repo = FakeAuthRepoForVm(loginResult = Result.success(successResp))
                val vm = LoginViewModel(repo)

                vm.onEmailChange("u@e.com")
                vm.onPasswordChange("password123")
                vm.login(onSuccess = {})
                kotlinx.coroutines.delay(50)

                val uiState = vm.uiState.value
                if (uiState is LoginUiState.Success) {
                    Result.success("✓ Login success updates UI state to Success")
                } else {
                    Result.failure(Exception("✗ Expected LoginUiState.Success, got ${uiState::class.simpleName}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(Exception("✗ Error: ${e.message}"))
        }
    }

    /**
     * Test: Login failure updates error state
     */
    fun testLoginFailureState(): Result<String> {
        return try {
            runBlocking {
                val repo = FakeAuthRepoForVm(loginResult = Result.failure(Exception("Invalid creds")))
                val vm = LoginViewModel(repo)

                vm.onEmailChange("u@e.com")
                vm.onPasswordChange("password123")
                vm.login(onSuccess = {})
                kotlinx.coroutines.delay(50)

                val uiState = vm.uiState.value
                if (uiState is LoginUiState.Error) {
                    if (uiState.message == "Invalid creds") {
                        Result.success("✓ Login failure updates UI state to Error with message 'Invalid creds'")
                    } else {
                        Result.failure(Exception("✗ Expected 'Invalid creds', got '${uiState.message}'"))
                    }
                } else {
                    Result.failure(Exception("✗ Expected LoginUiState.Error, got ${uiState::class.simpleName}"))
                }
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
            testPrefillEmail(),
            testEmailValidation(),
            testPasswordValidation(),
            testLoginSuccessState(),
            testLoginFailureState()
        )
    }

    /**
     * Print test results to console
     */
    fun printTestResults() {
        println("\n╔═══════════════════════════════════════════════╗")
        println("║   LOGIN VIEWMODEL UNIT TESTS                  ║")
        println("╚═══════════════════════════════════════════════╝\n")

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

        println("\n╔═══════════════════════════════════════════════╗")
        println("║            TEST SUMMARY                       ║")
        println("╠═══════════════════════════════════════════════╣")
        println("║ ✅ Passed: $passed")
        println("║ ❌ Failed: $failed")
        println("║ 📊 Total:  ${results.size}")
        println("║ 📈 Rate:   ${(passed * 100 / results.size)}%")
        println("╚═══════════════════════════════════════════════╝\n")
    }
}

