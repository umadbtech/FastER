package com.faster.festival.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.faster.festival.data.model.FriendSearchResult
import com.faster.festival.data.model.FriendshipResponse
import com.faster.festival.data.repository.FriendshipRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class FriendsUiState(
    val isLoading: Boolean = true,
    val friends: List<FriendshipResponse> = emptyList(),
    val pending: List<FriendshipResponse> = emptyList(),
    val requests: List<FriendshipResponse> = emptyList(),
    val searchResults: List<FriendSearchResult> = emptyList(),
    val isSearching: Boolean = false,
    val searchQuery: String = "",
    val errorMessage: String? = null,
    val actionMessage: String? = null,
    val selectedTab: Int = 0
)

class FriendsViewModel(
    private val friendshipRepository: FriendshipRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FriendsUiState())
    val uiState: StateFlow<FriendsUiState> = _uiState.asStateFlow()

    init {
        loadFriendships()
    }

    fun loadFriendships() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val result = friendshipRepository.getFriendships()
            result.fold(
                onSuccess = { response ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        friends = response.friends,
                        pending = response.pending,
                        requests = response.requests,
                        errorMessage = null
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.localizedMessage ?: "Failed to load friends"
                    )
                }
            )
        }
    }

    fun searchUsers(query: String) {
        if (query.length < 2) {
            _uiState.value = _uiState.value.copy(
                searchResults = emptyList(),
                searchQuery = query,
                isSearching = false
            )
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSearching = true, searchQuery = query)
            val result = friendshipRepository.searchUsers(query)
            result.fold(
                onSuccess = { response ->
                    _uiState.value = _uiState.value.copy(
                        isSearching = false,
                        searchResults = response.results
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isSearching = false,
                        errorMessage = error.localizedMessage ?: "Search failed"
                    )
                }
            )
        }
    }

    fun sendFriendRequest(userId: String) {
        viewModelScope.launch {
            val result = friendshipRepository.sendFriendRequest(userId)
            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        actionMessage = "Friend request sent!"
                    )
                    loadFriendships()
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        errorMessage = error.localizedMessage ?: "Failed to send request"
                    )
                }
            )
        }
    }

    fun acceptFriendRequest(friendshipId: String) {
        viewModelScope.launch {
            val result = friendshipRepository.respondToRequest(friendshipId, accept = true)
            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        actionMessage = "Friend request accepted!"
                    )
                    loadFriendships()
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        errorMessage = error.localizedMessage ?: "Failed to accept request"
                    )
                }
            )
        }
    }

    fun rejectFriendRequest(friendshipId: String) {
        viewModelScope.launch {
            val result = friendshipRepository.respondToRequest(friendshipId, accept = false)
            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        actionMessage = "Friend request declined"
                    )
                    loadFriendships()
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        errorMessage = error.localizedMessage ?: "Failed to decline request"
                    )
                }
            )
        }
    }

    fun onTabSelected(tabIndex: Int) {
        _uiState.value = _uiState.value.copy(selectedTab = tabIndex)
    }

    fun clearSearchResults() {
        _uiState.value = _uiState.value.copy(
            searchResults = emptyList(),
            searchQuery = "",
            isSearching = false
        )
    }

    fun clearActionMessage() {
        _uiState.value = _uiState.value.copy(actionMessage = null)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    class Factory(
        private val friendshipRepository: FriendshipRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return FriendsViewModel(friendshipRepository) as T
        }
    }
}
