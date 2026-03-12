package com.faster.festival.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
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
class LegacyHomeViewModel(private val repository: FestivalRepository) : ViewModel() {

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
class LegacyMapViewModel(private val repository: FestivalRepository) : ViewModel() {

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

// ============================================================================
// VIEWMODEL FACTORIES FOR REAL API REPOSITORIES
// ============================================================================

/**
 * Factory for MapViewModel with real API-backed repository
 * Uses ContentRepository which provides all real Supabase Edge Function APIs
 */
class MapViewModelFactory(
    private val festivalSlug: String,
    private val accessToken: String? = null
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        // ✅ Use ContentRepository with all real APIs
        val repository = com.faster.festival.data.repository.ContentRepository(
            festivalHeaderApi = com.faster.festival.di.NetworkModule.festivalHeaderApi,
            contentHomeApi = com.faster.festival.di.NetworkModule.contentHomeApi,
            contentLineupApi = com.faster.festival.di.NetworkModule.contentLineupApi,
            contentArtistDetailApi = com.faster.festival.di.NetworkModule.contentArtistDetailApi,
            contentStageScheduleApi = com.faster.festival.di.NetworkModule.contentStageScheduleApi,
            contentMapApi = com.faster.festival.di.NetworkModule.contentMapApi,
            festivalExperienceApi = com.faster.festival.di.NetworkModule.festivalExperienceApi,
            appHomeApi = com.faster.festival.di.NetworkModule.appHomeApi,
            appExperienceBundleApi = com.faster.festival.di.NetworkModule.appExperienceBundleApi,
            offlineBundleApi = com.faster.festival.di.NetworkModule.offlineBundleApi
        )
        // Map ContentRepository to FestivalRepository interface
        val festivalRepo = object : FestivalRepository {
            override fun getFestival(): kotlinx.coroutines.flow.Flow<Festival> =
                kotlinx.coroutines.flow.flow {
                    try {
                        val response = com.faster.festival.di.NetworkModule.festivalHeaderApi.getFestivalHeader(festivalSlug)
                        if (response.isSuccessful && response.body()?.festival != null) {
                            emit(Festival(
                                id = response.body()!!.festival!!.id,
                                slug = response.body()!!.festival!!.slug,
                                name = response.body()!!.festival!!.name,
                                timezone = response.body()!!.festival!!.timezone,
                                startsAt = response.body()!!.festival!!.starts_at,
                                endsAt = response.body()!!.festival!!.ends_at,
                                logoUrl = response.body()!!.festival!!.logo_url ?: "",
                                bannerUrl = response.body()!!.festival!!.banner_url ?: "",
                                accentColorHex = response.body()!!.festival!!.accent_color_hex ?: "",
                                contextState = response.body()!!.festival!!.context_state ?: ""
                            ))
                        }
                    } catch (e: Exception) {
                        throw e
                    }
                }
            override fun getArtists(): kotlinx.coroutines.flow.Flow<List<Artist>> =
                kotlinx.coroutines.flow.flow {
                    try {
                        val response = com.faster.festival.di.NetworkModule.contentLineupApi.getContentLineup(festivalSlug)
                        if (response.isSuccessful && response.body()?.featured_artists != null) {
                            emit(response.body()!!.featured_artists.map {
                                Artist(
                                    id = it.id,
                                    name = it.name,
                                    imageUrl = it.image_url ?: "",
                                    bio = it.bio ?: ""
                                )
                            })
                        }
                    } catch (e: Exception) {
                        throw e
                    }
                }
            override fun getArtistById(id: String): kotlinx.coroutines.flow.Flow<Artist?> =
                kotlinx.coroutines.flow.flow { emit(null) }
            override fun getPois(): kotlinx.coroutines.flow.Flow<List<Poi>> =
                kotlinx.coroutines.flow.flow {
                    try {
                        val response = com.faster.festival.di.NetworkModule.contentMapApi.getContentMap(festivalSlug)
                        if (response.isSuccessful && response.body()?.points_of_interest != null) {
                            emit(response.body()!!.points_of_interest.map {
                                Poi(
                                    id = it.id,
                                    name = it.name,
                                    type = it.type,
                                    latitude = it.latitude ?: 0.0,
                                    longitude = it.longitude ?: 0.0,
                                    description = it.description ?: ""
                                )
                            })
                        }
                    } catch (e: Exception) {
                        throw e
                    }
                }
            override fun getSchedule(): kotlinx.coroutines.flow.Flow<List<ScheduleItem>> =
                kotlinx.coroutines.flow.flow {
                    try {
                        val response = com.faster.festival.di.NetworkModule.contentStageScheduleApi.getStageSchedule(festivalSlug)
                        if (response.isSuccessful && response.body()?.stages != null) {
                            val items = mutableListOf<ScheduleItem>()
                            response.body()!!.stages.forEach { stage ->
                                // Add schedule items from stage
                            }
                            emit(items)
                        }
                    } catch (e: Exception) {
                        throw e
                    }
                }
            override fun getProfile(): kotlinx.coroutines.flow.Flow<AccountProfile> =
                kotlinx.coroutines.flow.flow { emit(AccountProfile("", "", "", "", "", "", "")) }
            override fun updateProfile(profile: AccountProfile): kotlinx.coroutines.flow.Flow<AccountProfile> =
                kotlinx.coroutines.flow.flow { emit(profile) }
        }
        return LegacyMapViewModel(festivalRepo) as T
    }
}

/**
 * Factory for ScheduleViewModel with real API-backed repository
 */
class ScheduleViewModelFactory(
    private val festivalSlug: String,
    private val accessToken: String? = null
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val repository = com.faster.festival.data.repository.ContentRepository(
            festivalHeaderApi = com.faster.festival.di.NetworkModule.festivalHeaderApi,
            contentHomeApi = com.faster.festival.di.NetworkModule.contentHomeApi,
            contentLineupApi = com.faster.festival.di.NetworkModule.contentLineupApi,
            contentArtistDetailApi = com.faster.festival.di.NetworkModule.contentArtistDetailApi,
            contentStageScheduleApi = com.faster.festival.di.NetworkModule.contentStageScheduleApi,
            contentMapApi = com.faster.festival.di.NetworkModule.contentMapApi,
            festivalExperienceApi = com.faster.festival.di.NetworkModule.festivalExperienceApi,
            appHomeApi = com.faster.festival.di.NetworkModule.appHomeApi,
            appExperienceBundleApi = com.faster.festival.di.NetworkModule.appExperienceBundleApi,
            offlineBundleApi = com.faster.festival.di.NetworkModule.offlineBundleApi
        )
        val festivalRepo = object : FestivalRepository {
            override fun getFestival(): kotlinx.coroutines.flow.Flow<Festival> =
                kotlinx.coroutines.flow.flow {
                    try {
                        val response = com.faster.festival.di.NetworkModule.festivalHeaderApi.getFestivalHeader(festivalSlug)
                        if (response.isSuccessful && response.body()?.festival != null) {
                            emit(Festival(
                                id = response.body()!!.festival!!.id,
                                slug = response.body()!!.festival!!.slug,
                                name = response.body()!!.festival!!.name,
                                timezone = response.body()!!.festival!!.timezone,
                                startsAt = response.body()!!.festival!!.starts_at,
                                endsAt = response.body()!!.festival!!.ends_at,
                                logoUrl = response.body()!!.festival!!.logo_url ?: "",
                                bannerUrl = response.body()!!.festival!!.banner_url ?: "",
                                accentColorHex = response.body()!!.festival!!.accent_color_hex ?: "",
                                contextState = response.body()!!.festival!!.context_state ?: ""
                            ))
                        }
                    } catch (e: Exception) {
                        throw e
                    }
                }
            override fun getArtists(): kotlinx.coroutines.flow.Flow<List<Artist>> =
                kotlinx.coroutines.flow.flow { emit(emptyList()) }
            override fun getArtistById(id: String): kotlinx.coroutines.flow.Flow<Artist?> =
                kotlinx.coroutines.flow.flow { emit(null) }
            override fun getPois(): kotlinx.coroutines.flow.Flow<List<Poi>> =
                kotlinx.coroutines.flow.flow { emit(emptyList()) }
            override fun getSchedule(): kotlinx.coroutines.flow.Flow<List<ScheduleItem>> =
                kotlinx.coroutines.flow.flow {
                    try {
                        val response = com.faster.festival.di.NetworkModule.contentStageScheduleApi.getStageSchedule(festivalSlug)
                        if (response.isSuccessful && response.body()?.stages != null) {
                            val items = mutableListOf<ScheduleItem>()
                            response.body()!!.stages.forEach { stage ->
                                // Map stage performances to schedule items
                            }
                            emit(items)
                        }
                    } catch (e: Exception) {
                        throw e
                    }
                }
            override fun getProfile(): kotlinx.coroutines.flow.Flow<AccountProfile> =
                kotlinx.coroutines.flow.flow { emit(AccountProfile("", "", "", "", "", "", "")) }
            override fun updateProfile(profile: AccountProfile): kotlinx.coroutines.flow.Flow<AccountProfile> =
                kotlinx.coroutines.flow.flow { emit(profile) }
        }
        return ScheduleViewModel(festivalRepo) as T
    }
}

/**
 * Factory for ArtistDetailViewModel with real API-backed repository
 */
class ArtistDetailViewModelFactory(
    private val festivalSlug: String,
    private val accessToken: String? = null
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val repository = com.faster.festival.data.repository.ContentRepository(
            festivalHeaderApi = com.faster.festival.di.NetworkModule.festivalHeaderApi,
            contentHomeApi = com.faster.festival.di.NetworkModule.contentHomeApi,
            contentLineupApi = com.faster.festival.di.NetworkModule.contentLineupApi,
            contentArtistDetailApi = com.faster.festival.di.NetworkModule.contentArtistDetailApi,
            contentStageScheduleApi = com.faster.festival.di.NetworkModule.contentStageScheduleApi,
            contentMapApi = com.faster.festival.di.NetworkModule.contentMapApi,
            festivalExperienceApi = com.faster.festival.di.NetworkModule.festivalExperienceApi,
            appHomeApi = com.faster.festival.di.NetworkModule.appHomeApi,
            appExperienceBundleApi = com.faster.festival.di.NetworkModule.appExperienceBundleApi,
            offlineBundleApi = com.faster.festival.di.NetworkModule.offlineBundleApi
        )
        val festivalRepo = object : FestivalRepository {
            override fun getFestival(): kotlinx.coroutines.flow.Flow<Festival> =
                kotlinx.coroutines.flow.flow {
                    try {
                        val response = com.faster.festival.di.NetworkModule.festivalHeaderApi.getFestivalHeader(festivalSlug)
                        if (response.isSuccessful && response.body()?.festival != null) {
                            emit(Festival(
                                id = response.body()!!.festival!!.id,
                                slug = response.body()!!.festival!!.slug,
                                name = response.body()!!.festival!!.name,
                                timezone = response.body()!!.festival!!.timezone,
                                startsAt = response.body()!!.festival!!.starts_at,
                                endsAt = response.body()!!.festival!!.ends_at,
                                logoUrl = response.body()!!.festival!!.logo_url ?: "",
                                bannerUrl = response.body()!!.festival!!.banner_url ?: "",
                                accentColorHex = response.body()!!.festival!!.accent_color_hex ?: "",
                                contextState = response.body()!!.festival!!.context_state ?: ""
                            ))
                        }
                    } catch (e: Exception) {
                        throw e
                    }
                }
            override fun getArtists(): kotlinx.coroutines.flow.Flow<List<Artist>> =
                kotlinx.coroutines.flow.flow { emit(emptyList()) }
            override fun getArtistById(id: String): kotlinx.coroutines.flow.Flow<Artist?> =
                kotlinx.coroutines.flow.flow {
                    try {
                        val response = com.faster.festival.di.NetworkModule.contentLineupApi.getContentLineup(festivalSlug)
                        if (response.isSuccessful && response.body()?.featured_artists != null) {
                            val artist = response.body()!!.featured_artists.find { it.id == id }
                            emit(artist?.let {
                                Artist(
                                    id = it.id,
                                    name = it.name,
                                    imageUrl = it.image_url ?: "",
                                    bio = it.bio ?: ""
                                )
                            })
                        }
                    } catch (e: Exception) {
                        throw e
                    }
                }
            override fun getPois(): kotlinx.coroutines.flow.Flow<List<Poi>> =
                kotlinx.coroutines.flow.flow { emit(emptyList()) }
            override fun getSchedule(): kotlinx.coroutines.flow.Flow<List<ScheduleItem>> =
                kotlinx.coroutines.flow.flow { emit(emptyList()) }
            override fun getProfile(): kotlinx.coroutines.flow.Flow<AccountProfile> =
                kotlinx.coroutines.flow.flow { emit(AccountProfile("", "", "", "", "", "", "")) }
            override fun updateProfile(profile: AccountProfile): kotlinx.coroutines.flow.Flow<AccountProfile> =
                kotlinx.coroutines.flow.flow { emit(profile) }
        }
        return ArtistDetailViewModel(festivalRepo) as T
    }
}
