package com.faster.festival.data.repository

import com.faster.festival.data.model.LoginResponse

interface AuthRepositoryContract {
    fun getSavedEmail(): String?
    suspend fun login(email: String, password: String): Result<LoginResponse>
}
