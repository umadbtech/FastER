package com.faster.festival.ui.auth.login

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class FakeAuthRepository(
    private val savedEmail: String? = null,
    private val loginResult: Result<Any>? = null
) : com.faster.festival.data.repository.AuthRepository(
    authApiService = TODO("unused in fake"),
    sessionManager = com.faster.festival.data.local.EncryptedSessionManagerPlaceholder()
) {
    override fun getSavedEmail(): String? = savedEmail

    override suspend fun login(email: String, password: String): Result<com.faster.festival.data.model.LoginResponse> {
        @Suppress("UNCHECKED_CAST")
        return (loginResult as? Result<com.faster.festival.data.model.LoginResponse>)
            ?: Result.failure(Exception("Not configured"))
    }
}

// Simple in-memory placeholder to satisfy constructor; real EncryptedSessionManager requires Context.
class EncryptedSessionManagerPlaceholder : com.faster.festival.data.local.EncryptedSessionManagerPlaceholderBase()

class LoginViewModelTest {

    @Test
    fun prefillEmail_populatesForm() = runBlocking {
        val fakeRepo = object : com.faster.festival.data.repository.AuthRepository(
            authApiService = TODO("unused"),
            sessionManager = com.faster.festival.data.local.EncryptedSessionManagerPlaceholderBase()
        ) {
            override fun getSavedEmail(): String? = "saved@example.com"
        }

        val vm = LoginViewModel(fakeRepo)
        // give coroutine init time
        kotlinx.coroutines.delay(50)
        val form = vm.formState.value
        assertEquals("saved@example.com", form.email)
    }

    @Test
    fun validation_emailAndPassword() {
        val fakeRepo = object : com.faster.festival.data.repository.AuthRepository(
            authApiService = TODO("unused"),
            sessionManager = com.faster.festival.data.local.EncryptedSessionManagerPlaceholderBase()
        ) {
            override fun getSavedEmail(): String? = null
        }

        val vm = LoginViewModel(fakeRepo)
        vm.onEmailChange("not-an-email")
        assertNotNull(vm.formState.value.emailError)
        vm.onEmailChange("test@example.com")
        assertNull(vm.formState.value.emailError)

        vm.onPasswordChange("123")
        assertNotNull(vm.formState.value.passwordError)
        vm.onPasswordChange("secure123")
        assertNull(vm.formState.value.passwordError)
    }
}
