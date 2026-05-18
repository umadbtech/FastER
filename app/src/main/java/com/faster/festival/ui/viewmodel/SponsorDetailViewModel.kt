package com.faster.festival.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.faster.festival.data.models.SponsorOffer
import com.faster.festival.data.network.NetworkMonitor
import com.faster.festival.data.remote.AppHomeApi
import com.faster.festival.ui.util.armAutoRetry
import com.faster.festival.ui.util.isOfflineNow
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class SponsorDetailState {
    object Loading : SponsorDetailState()
    data class Success(val sponsor: SponsorOffer) : SponsorDetailState()
    object Offline : SponsorDetailState()
    data class Error(val message: String) : SponsorDetailState()
}

class SponsorDetailViewModel(
    private val appHomeApi: AppHomeApi,
    private val festivalSlug: String,
    private val sponsorId: String,
    private val networkMonitor: NetworkMonitor? = null
) : ViewModel() {

    private val _sponsorState = MutableStateFlow<SponsorDetailState>(SponsorDetailState.Loading)
    val sponsorState: StateFlow<SponsorDetailState> = _sponsorState.asStateFlow()
    private var autoRetryJob: Job? = null

    init { loadSponsor() }

    fun loadSponsor() {
        viewModelScope.launch {
            if (networkMonitor.isOfflineNow()) {
                _sponsorState.value = SponsorDetailState.Offline
                autoRetryJob = networkMonitor?.armAutoRetry(viewModelScope, autoRetryJob) { loadSponsor() }
                return@launch
            }
            _sponsorState.value = SponsorDetailState.Loading
            try {
                val response = appHomeApi.getAppHomeBundle(
                    festivalSlug = festivalSlug,
                    ifNoneMatch = null
                )
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        val sponsor = body.sponsorOffers.find { it.id == sponsorId }
                        if (sponsor != null) {
                            _sponsorState.value = SponsorDetailState.Success(sponsor)
                        } else {
                            _sponsorState.value = SponsorDetailState.Error("Sponsor not found")
                        }
                    } else {
                        _sponsorState.value = SponsorDetailState.Error("Empty response")
                    }
                } else {
                    _sponsorState.value = SponsorDetailState.Error("API error: ${response.code()}")
                }
            } catch (e: Exception) {
                if (networkMonitor.isOfflineNow(e)) {
                    _sponsorState.value = SponsorDetailState.Offline
                    autoRetryJob = networkMonitor?.armAutoRetry(viewModelScope, autoRetryJob) { loadSponsor() }
                } else {
                    _sponsorState.value = SponsorDetailState.Error(
                        e.localizedMessage ?: "Network error"
                    )
                }
            }
        }
    }

    fun retry() = loadSponsor()

    class Factory(
        private val appHomeApi: AppHomeApi,
        private val festivalSlug: String,
        private val sponsorId: String,
        private val networkMonitor: NetworkMonitor? = null
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SponsorDetailViewModel(
                appHomeApi, festivalSlug, sponsorId, networkMonitor
            ) as T
        }
    }
}
