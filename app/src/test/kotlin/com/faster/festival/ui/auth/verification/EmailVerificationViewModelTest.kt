package com.faster.festival.ui.auth.verification

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

// Note: avoid JUnit and kotlinx-coroutines-test imports so this test file compiles in simpler environments.

fun launchTestCollect(
    flow: kotlinx.coroutines.flow.SharedFlow<VerificationUiEvent>,
    collector: MutableList<VerificationUiEvent>
): Job {
    return GlobalScope.launch(Dispatchers.Default) {
        flow.collect { collector.add(it) }
    }
}

// Simple smoke-run function (not a junit test) to validate handler logic locally.
fun main() {
    runBlocking {
        val testSession = com.faster.festival.data.local.EncryptedSessionManager()
        testSession.saveUserID("user-123")

        // Use the production stub SupabaseClient (exists in main sources: io.github.jan_tennert.supabase.SupabaseClient)
        val supabaseClient = io.github.jan_tennert.supabase.SupabaseClient()

        val vm = EmailVerificationViewModel(supabaseClient, testSession)

        // Prepare a matching payload
        val payload = "{\"user_id\": \"user-123\", \"email\": \"test@example.com\", \"timestamp\": \"2026-02-19T00:00:00Z\"}"
        val message = io.github.jan_tennert.supabase.realtime.Message(payload)

        // Collect events before invoking handler
        val events = mutableListOf<VerificationUiEvent>()
        val job = launchTestCollect(vm.events, events)

        // Call internal suspend handler directly
        vm.handleVerificationEvent(message, "user-123")

        // Small delay is not necessary; handler emits synchronously in this context.

        // Basic assertions using plain checks
        if (!testSession.isEmailConfirmed()) throw AssertionError("Expected email to be confirmed in session")
        if (vm.verificationState.value != VerificationUiState.Verified) throw AssertionError("Expected VM to be in Verified state")
        if (events.size != 2) throw AssertionError("Expected 2 events emitted, got ${events.size}")
        val first = events[0]
        val second = events[1]
        if (first !is VerificationUiEvent.ShowToast || first.message != "Email verified successfully") throw AssertionError("First event should be ShowToast with correct message")
        if (second != VerificationUiEvent.NavigateHome) throw AssertionError("Second event should be NavigateHome")

        job.cancel()
        println("EmailVerificationViewModel local smoke-run: PASS")
    }
}
