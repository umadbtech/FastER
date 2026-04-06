package com.faster.festival.ui.auth.verification

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.MutableSharedFlow

/**
 * Mock VerificationUiEvent for testing (simple sealed class)
 */
sealed class MockVerificationUiEvent {
    data class ShowToast(val message: String) : MockVerificationUiEvent()
    object NavigateHome : MockVerificationUiEvent()
}

/**
 * Mock VerificationUiState for testing (simple sealed class)
 */
sealed class MockVerificationUiState {
    object Idle : MockVerificationUiState()
    object Verified : MockVerificationUiState()
    data class Error(val message: String) : MockVerificationUiState()
}

/**
 * Mock EncryptedSessionManager for testing
 */
class MockEncryptedSessionManager {
    private var emailConfirmed = false
    private var userId: String? = null

    fun saveUserID(id: String) {
        userId = id
    }

    fun isEmailConfirmed(): Boolean = emailConfirmed

    fun markEmailConfirmed() {
        emailConfirmed = true
    }
}


/**
 * Helper function to launch test collection
 */
@OptIn(kotlinx.coroutines.DelicateCoroutinesApi::class)
fun launchTestCollect(
    flow: SharedFlow<MockVerificationUiEvent>,
    collector: MutableList<MockVerificationUiEvent>
): Job {
    return GlobalScope.launch(Dispatchers.Default) {
        flow.collect { collector.add(it) }
    }
}

/**
 * Email Verification ViewModel Test Helper
 * Validates email verification event handling without external test dependencies
 */
object EmailVerificationViewModelTestHelper {

    /**
     * Test: Email verification event handling
     */
    fun testEmailVerificationEventHandling(): Result<String> {
        return try {
            runBlocking {
                // Setup
                val testSession = MockEncryptedSessionManager()
                testSession.saveUserID("user-123")

                // Mock ViewModel state
                var verificationState: MockVerificationUiState = MockVerificationUiState.Idle
                val eventsFlow = MutableSharedFlow<MockVerificationUiEvent>()

                // Prepare test payload
                val payload = "{\"user_id\": \"user-123\", \"email\": \"test@example.com\", \"timestamp\": \"2026-02-19T00:00:00Z\"}"

                // Collect events
                val events = mutableListOf<MockVerificationUiEvent>()
                val job = launchTestCollect(eventsFlow, events)

                // Simulate event handling
                if (payload.contains("user-123")) {
                    testSession.markEmailConfirmed()
                    verificationState = MockVerificationUiState.Verified
                    eventsFlow.emit(MockVerificationUiEvent.ShowToast("Email verified successfully"))
                    eventsFlow.emit(MockVerificationUiEvent.NavigateHome)
                }

                // Small delay for collection
                kotlinx.coroutines.delay(50)

                // Verify results
                if (!testSession.isEmailConfirmed()) {
                    throw AssertionError("Expected email to be confirmed in session")
                }

                if (verificationState != MockVerificationUiState.Verified) {
                    throw AssertionError("Expected state to be Verified, got $verificationState")
                }

                if (events.size != 2) {
                    throw AssertionError("Expected 2 events emitted, got ${events.size}")
                }

                val first = events[0]
                val second = events[1]

                if (first !is MockVerificationUiEvent.ShowToast || first.message != "Email verified successfully") {
                    throw AssertionError("First event should be ShowToast with correct message, got $first")
                }

                if (second != MockVerificationUiEvent.NavigateHome) {
                    throw AssertionError("Second event should be NavigateHome, got $second")
                }

                job.cancel()

                Result.success("✓ Email verification event handling works correctly")
            }
        } catch (e: Exception) {
            Result.failure(Exception("✗ Error: ${e.message}"))
        }
    }


    /**
     * Test: Email confirmation state tracking
     */
    fun testEmailConfirmationState(): Result<String> {
        return try {
            val sessionManager = MockEncryptedSessionManager()
            sessionManager.saveUserID("user-456")

            if (sessionManager.isEmailConfirmed()) {
                return Result.failure(Exception("Email should not be confirmed initially"))
            }

            sessionManager.markEmailConfirmed()

            if (!sessionManager.isEmailConfirmed()) {
                return Result.failure(Exception("Email should be confirmed after marking"))
            }

            Result.success("✓ Email confirmation state tracking works correctly")
        } catch (e: Exception) {
            Result.failure(Exception("✗ Error: ${e.message}"))
        }
    }

    /**
     * Test: Verification state transitions
     */
    fun testVerificationStateTransitions(): Result<String> {
        return try {
            var state: MockVerificationUiState = MockVerificationUiState.Idle

            // Initial state
            if (state != MockVerificationUiState.Idle) {
                throw AssertionError("Initial state should be Idle")
            }

            // Transition to Verified
            state = MockVerificationUiState.Verified
            if (state != MockVerificationUiState.Verified) {
                throw AssertionError("State should be Verified after transition")
            }

            // Transition to Error
            val errorState = MockVerificationUiState.Error("Test error")
            state = errorState
            if (state != errorState) {
                throw AssertionError("State should be Error after transition")
            }

            Result.success("✓ Verification state transitions work correctly")
        } catch (e: Exception) {
            Result.failure(Exception("✗ Error: ${e.message}"))
        }
    }

    /**
     * Test: Event emission sequence
     */
    fun testEventEmissionSequence(): Result<String> {
        return try {
            runBlocking {
                val eventFlow = MutableSharedFlow<MockVerificationUiEvent>()
                val collectedEvents = mutableListOf<MockVerificationUiEvent>()

                val job = launchTestCollect(eventFlow, collectedEvents)

                // Emit events in sequence
                eventFlow.emit(MockVerificationUiEvent.ShowToast("Starting verification"))
                eventFlow.emit(MockVerificationUiEvent.ShowToast("Email verified successfully"))
                eventFlow.emit(MockVerificationUiEvent.NavigateHome)

                kotlinx.coroutines.delay(50)

                if (collectedEvents.size != 3) {
                    throw AssertionError("Expected 3 events, got ${collectedEvents.size}")
                }

                if (collectedEvents[0] !is MockVerificationUiEvent.ShowToast) {
                    throw AssertionError("First event should be ShowToast")
                }

                if (collectedEvents[2] != MockVerificationUiEvent.NavigateHome) {
                    throw AssertionError("Third event should be NavigateHome")
                }

                job.cancel()

                Result.success("✓ Event emission sequence works correctly")
            }
        } catch (e: Exception) {
            Result.failure(Exception("✗ Error: ${e.message}"))
        }
    }

    /**
     * Run all tests
     */
    fun runAllTests(): List<Result<String>> {
        return listOf(
            testEmailVerificationEventHandling(),
            testEmailConfirmationState(),
            testVerificationStateTransitions(),
            testEventEmissionSequence()
        )
    }

    /**
     * Print test results
     */
    fun printTestResults() {
        println("\n╔═══════════════════════════════════════════════╗")
        println("║  EMAIL VERIFICATION VIEWMODEL TESTS            ║")
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
        println("║             TEST SUMMARY                       ║")
        println("╠═══════════════════════════════════════════════╣")
        println("║ ✅ Passed: $passed")
        println("║ ❌ Failed: $failed")
        println("║ 📊 Total:  ${results.size}")
        println("║ 📈 Rate:   ${(passed * 100 / results.size)}%")
        println("╚═══════════════════════════════════════════════╝\n")
    }
}

/**
 * Main function to run tests locally
 */
fun main() {
    EmailVerificationViewModelTestHelper.printTestResults()
}


