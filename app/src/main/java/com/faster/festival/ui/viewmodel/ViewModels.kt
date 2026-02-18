package com.faster.festival.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.faster.festival.data.models.*
import com.faster.festival.data.repository.FestivalRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// UI State Classes
sealed class UiState<out T> {
    object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val message: String) : UiState<Nothing>()
}

// Home ViewModel
class HomeViewModel(private val repository: FestivalRepository) : ViewModel() {

    private val _festivalState = MutableStateFlow<UiState<Festival>>(UiState.Loading)
    val festivalState: StateFlow<UiState<Festival>> = _festivalState.asStateFlow()

    private val _artistsState = MutableStateFlow<UiState<List<Artist>>>(UiState.Loading)
    val artistsState: StateFlow<UiState<List<Artist>>> = _artistsState.asStateFlow()

    init {
        loadFestival()
        loadArtists()
    }

    private fun loadFestival() {
        viewModelScope.launch {
            try {
                repository.getFestival().collect { festival ->
                    _festivalState.value = UiState.Success(festival)
                }
            } catch (e: Exception) {
                _festivalState.value = UiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    private fun loadArtists() {
        viewModelScope.launch {
            try {
                repository.getArtists().collect { artists ->
                    _artistsState.value = UiState.Success(artists)
                }
            } catch (e: Exception) {
                _artistsState.value = UiState.Error(e.message ?: "Unknown error")
            }
        }
    }
}

// Map ViewModel
class MapViewModel(private val repository: FestivalRepository) : ViewModel() {

    private val _poisState = MutableStateFlow<UiState<List<Poi>>>(UiState.Loading)
    val poisState: StateFlow<UiState<List<Poi>>> = _poisState.asStateFlow()

    init {
        loadPois()
    }

    private fun loadPois() {
        viewModelScope.launch {
            try {
                repository.getPois().collect { pois ->
                    _poisState.value = UiState.Success(pois)
                }
            } catch (e: Exception) {
                _poisState.value = UiState.Error(e.message ?: "Unknown error")
            }
        }
    }
}

// Schedule ViewModel
class ScheduleViewModel(private val repository: FestivalRepository) : ViewModel() {

    private val _scheduleState = MutableStateFlow<UiState<List<ScheduleItem>>>(UiState.Loading)
    val scheduleState: StateFlow<UiState<List<ScheduleItem>>> = _scheduleState.asStateFlow()

    init {
        loadSchedule()
    }

    private fun loadSchedule() {
        viewModelScope.launch {
            try {
                repository.getSchedule().collect { items ->
                    _scheduleState.value = UiState.Success(items)
                }
            } catch (e: Exception) {
                _scheduleState.value = UiState.Error(e.message ?: "Unknown error")
            }
        }
    }
}

// Profile ViewModel
class ProfileViewModel(private val repository: FestivalRepository) : ViewModel() {

    private val _profileState = MutableStateFlow<UiState<AccountProfile>>(UiState.Loading)
    val profileState: StateFlow<UiState<AccountProfile>> = _profileState.asStateFlow()

    init {
        loadProfile()
    }

    private fun loadProfile() {
        viewModelScope.launch {
            try {
                repository.getProfile().collect { profile ->
                    _profileState.value = UiState.Success(profile)
                }
            } catch (e: Exception) {
                _profileState.value = UiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun updateProfile(profile: AccountProfile) {
        viewModelScope.launch {
            try {
                repository.updateProfile(profile).collect { updated ->
                    _profileState.value = UiState.Success(updated)
                }
            } catch (e: Exception) {
                _profileState.value = UiState.Error(e.message ?: "Unknown error")
            }
        }
    }
}

// Artist Detail ViewModel
class ArtistDetailViewModel(private val repository: FestivalRepository) : ViewModel() {

    private val _artistState = MutableStateFlow<UiState<Artist?>>(UiState.Loading)
    val artistState: StateFlow<UiState<Artist?>> = _artistState.asStateFlow()

    fun loadArtist(artistId: String) {
        viewModelScope.launch {
            try {
                repository.getArtistById(artistId).collect { artist ->
                    _artistState.value = UiState.Success(artist)
                }
            } catch (e: Exception) {
                _artistState.value = UiState.Error(e.message ?: "Unknown error")
            }
        }
    }
}

// Auth ViewModel
class AuthViewModel : ViewModel() {

    private val _authState = MutableStateFlow<UiState<Boolean>>(UiState.Success(false))
    val authState: StateFlow<UiState<Boolean>> = _authState.asStateFlow()

    private val _emailState = MutableStateFlow("")
    val emailState: StateFlow<String> = _emailState.asStateFlow()

    private val _codeState = MutableStateFlow("")
    val codeState: StateFlow<String> = _codeState.asStateFlow()

    fun setEmail(email: String) {
        _emailState.value = email
    }

    fun setCode(code: String) {
        if (code.length <= 6) {
            _codeState.value = code
        }
    }

    fun verifyCode() {
        // Simulate verification
        if (_codeState.value.length == 6) {
            _authState.value = UiState.Success(true)
        }
    }

    fun sendCode() {
        // Simulate sending code
        _authState.value = UiState.Success(true)
    }
}
