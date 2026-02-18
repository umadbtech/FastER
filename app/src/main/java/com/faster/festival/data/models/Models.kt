package com.faster.festival.data.models

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

// Festival Data Models
data class Festival(
    val id: String,
    val name: String,
    val location: String,
    val date: String,
    val heroImageUrl: String = "",
    val description: String = ""
)

data class Artist(
    val id: String,
    val name: String,
    val imageUrl: String = "",
    val bio: String = "",
    val sets: List<FestivalSet> = emptyList()
)

data class FestivalSet(
    val id: String,
    val name: String,
    val stageName: String,
    val startTime: String,
    val endTime: String,
    val dayName: String = "Opening Night"
)

data class Poi(
    val id: String,
    val name: String,
    val type: String,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val description: String = ""
)

data class QuickAction(
    val id: String,
    val label: String,
    val icon: ImageVector,
    val route: String
)

data class ScheduleItem(
    val id: String,
    val stageName: String,
    val artistName: String,
    val startTime: String,
    val endTime: String,
    val date: String
)

data class AccountProfile(
    val id: String,
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val emergencyContact: String = "",
    val allergies: String = "",
    val medications: String = "",
    val avatar: String = "",
    val completionProgress: Int = 0,
    val paymentMethods: List<PaymentMethod> = emptyList()
)

data class PaymentMethod(
    val id: String,
    val last4: String,
    val brand: String,
    val expiryMonth: Int,
    val expiryYear: Int
)

data class Shortcut(
    val id: String,
    val label: String
)

// UI State wrappers
sealed class UiState<T> {
    object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error<T>(val exception: Exception) : UiState<T>()
}

sealed class AuthState {
    object Unauthenticated : AuthState()
    object Loading : AuthState()
    data class Authenticated(val profile: AccountProfile) : AuthState()
    data class Error(val message: String) : AuthState()
}

