package com.faster.festival.ui.auth.login

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import com.faster.festival.data.model.LoginResponse
import com.faster.festival.data.model.User

class FakeAuthRepoForVm(private val savedEmail: String? = null, private val loginResult: Result<LoginResponse>? = null) : com.faster.festival.data.repository.AuthRepositoryContract {
    override fun getSavedEmail(): String? = savedEmail
    override suspend fun login(email: String, password: String): Result<LoginResponse> {
        return loginResult ?: Result.failure(Exception("not configured"))
    }
}

class LoginViewModelUnitTest {

    @Test
    fun prefillEmail_isApplied() = runBlocking {
        val repo = FakeAuthRepoForVm(savedEmail = "prefill@example.com")
        val vm = LoginViewModel(repo)
        kotlinx.coroutines.delay(50)
        assertEquals("prefill@example.com", vm.formState.value.email)
    }

    @Test
    fun validation_logic() {
        val repo = FakeAuthRepoForVm()
        val vm = LoginViewModel(repo)
        vm.onEmailChange("bad")
        assertNotNull(vm.formState.value.emailError)
        vm.onEmailChange("ok@example.com")
        assertNull(vm.formState.value.emailError)

        vm.onPasswordChange("123")
        assertNotNull(vm.formState.value.passwordError)
        vm.onPasswordChange("password123")
        assertNull(vm.formState.value.passwordError)
    }

    @Test
    fun login_success_updatesState() = runBlocking {
        val successResp = LoginResponse(accessToken = "a", refreshToken = "r", user = User(id = "u", email = "u@e.com"))
        val repo = FakeAuthRepoForVm(loginResult = Result.success(successResp))
        val vm = LoginViewModel(repo)
        vm.onEmailChange("u@e.com")
        vm.onPasswordChange("password123")
        vm.login(onSuccess = {})
        kotlinx.coroutines.delay(50)
        assertTrue(vm.uiState.value is LoginUiState.Success)
    }

    @Test
    fun login_failure_updatesError() = runBlocking {
        val repo = FakeAuthRepoForVm(loginResult = Result.failure(Exception("Invalid creds")))
        val vm = LoginViewModel(repo)
        vm.onEmailChange("u@e.com")
        vm.onPasswordChange("password123")
        vm.login(onSuccess = {})
        kotlinx.coroutines.delay(50)
        assertTrue(vm.uiState.value is LoginUiState.Error)
        val err = vm.uiState.value as LoginUiState.Error
        assertEquals("Invalid creds", err.message)
    }
}
