package com.faster.festival.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import android.content.Context
import com.faster.festival.data.pinch.model.EmergencyCategory
import com.faster.festival.data.pinch.model.EmergencyRequest
import com.faster.festival.data.pinch.model.FeedbackConfig
import com.faster.festival.data.pinch.model.FeedbackQuestion
import com.faster.festival.data.pinch.model.FeedbackSubmission
import com.faster.festival.data.pinch.model.TimelineConfig
import com.faster.festival.data.pinch.model.UserContext
import com.faster.festival.data.pinch.repository.PinchEmergencyRepository
import com.faster.festival.data.pinch.repository.PinchFeedbackRepository
import com.faster.festival.ui.pinch.map.getCurrentLocation
import com.faster.festival.ui.pinch.map.medicalStationPositions
import com.faster.festival.ui.pinch.map.responderPosition
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class PinchHelpState {
    Landing,
    SwipeForHelp,
    AlertSent,
    AnswerCall,
    EmergencyLocation,
    ContactPhone,
    CategorySelection,
    AdditionalInfoChoice,
    AdditionalInfoForm,
    FormSubmitted,
    OnTheWay,
    HelpArrived,
    InProgress,
    Resolved,
    FeedbackIntro,
    FeedbackSurvey,
    FeedbackComplete,
    CancelSwipe,
    CancelConfirm,
    Cancelled
}

data class PinchHelpUiState(
    val currentState: PinchHelpState = PinchHelpState.Landing,
    val isLoading: Boolean = false,
    val error: String? = null,

    // User context
    val userPhone: String = "",
    val userLocationLabel: String = "",
    val userCoordinates: String = "",
    val userGpsText: String = "",
    val useCurrentLocation: Boolean = true,
    val customLocationText: String = "",
    val dispatcherName: String = "",
    val dispatcherPhone: String = "",
    val responderName: String = "",
    val nearestStation: String = "",

    // ETA
    val etaMinutes: Int = 5,
    val etaStartTime: String = "",
    val etaEndTime: String = "",

    // Contact phone form
    val contactPhone: String = "",
    val useMyPhone: Boolean = true,
    val customPhone: String = "",

    // Emergency categories
    val categories: List<EmergencyCategory> = emptyList(),
    val selectedCategoryIds: Set<String> = emptySet(),

    // Additional info
    val wantsToProvideInfo: Boolean? = null,
    val additionalInfo: String = "",

    // Request
    val requestId: String = "",

    // Timeline
    val timelineConfig: TimelineConfig? = null,
    val activeTimelineStep: Int = 0,

    // Feedback
    val feedbackConfig: FeedbackConfig? = null,
    val currentQuestionIndex: Int = 0,
    val questionRatings: Map<String, Int> = emptyMap(),
    val overallRating: Int = 0,
    val feedbackComment: String = "",

    // Previous state before cancel
    val previousState: PinchHelpState = PinchHelpState.Landing,

    // Real map location
    val userLatLng: LatLng? = null,
    val responderLatLng: LatLng? = null,
    val medicalStations: List<LatLng> = emptyList(),
    val responderDistanceMeters: Double = 250.0,
    val mapLocationLoaded: Boolean = false
)

class PinchHelpViewModel(
    private val emergencyRepository: PinchEmergencyRepository,
    private val feedbackRepository: PinchFeedbackRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PinchHelpUiState())
    val uiState: StateFlow<PinchHelpUiState> = _uiState.asStateFlow()

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val userResult = emergencyRepository.getUserContext()
            val categoriesResult = emergencyRepository.getEmergencyCategories()
            val timelineResult = emergencyRepository.getTimelineConfig()

            userResult.onSuccess { ctx ->
                val now = java.util.Calendar.getInstance()
                val startHour = now.get(java.util.Calendar.HOUR_OF_DAY)
                val startMin = now.get(java.util.Calendar.MINUTE) + ctx.defaultEtaMinutes
                val endMin = startMin + 5

                _uiState.value = _uiState.value.copy(
                    userPhone = ctx.userPhone,
                    contactPhone = ctx.userPhone,
                    userLocationLabel = ctx.locationLabel,
                    userCoordinates = ctx.coordinates,
                    userGpsText = ctx.gpsText,
                    dispatcherName = ctx.dispatcherName,
                    dispatcherPhone = ctx.dispatcherPhone,
                    responderName = ctx.responderName,
                    nearestStation = ctx.nearestStation,
                    etaMinutes = ctx.defaultEtaMinutes,
                    etaStartTime = String.format("%d:%02d", startHour, startMin % 60),
                    etaEndTime = String.format("%d:%02d", startHour + (endMin / 60), endMin % 60)
                )
            }

            categoriesResult.onSuccess { cats ->
                _uiState.value = _uiState.value.copy(categories = cats)
            }

            timelineResult.onSuccess { config ->
                _uiState.value = _uiState.value.copy(timelineConfig = config)
            }

            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }

    // Navigation functions

    fun navigateToSwipe() {
        _uiState.value = _uiState.value.copy(currentState = PinchHelpState.SwipeForHelp)
    }

    fun swipeToAlert() {
        _uiState.value = _uiState.value.copy(
            currentState = PinchHelpState.AlertSent,
            activeTimelineStep = 0
        )
    }

    fun proceedToAnswerCall() {
        _uiState.value = _uiState.value.copy(
            currentState = PinchHelpState.AnswerCall,
            activeTimelineStep = 1
        )
    }

    fun proceedToEmergencyLocation() {
        _uiState.value = _uiState.value.copy(currentState = PinchHelpState.EmergencyLocation)
    }

    fun proceedToContactPhone() {
        _uiState.value = _uiState.value.copy(currentState = PinchHelpState.ContactPhone)
    }

    fun proceedToCategorySelection() {
        _uiState.value = _uiState.value.copy(
            currentState = PinchHelpState.CategorySelection,
            activeTimelineStep = 2
        )
    }

    fun proceedToAdditionalInfoChoice() {
        _uiState.value = _uiState.value.copy(currentState = PinchHelpState.AdditionalInfoChoice)
    }

    fun proceedToAdditionalInfoForm() {
        _uiState.value = _uiState.value.copy(
            currentState = PinchHelpState.AdditionalInfoForm,
            wantsToProvideInfo = true
        )
    }

    fun skipAdditionalInfo() {
        _uiState.value = _uiState.value.copy(wantsToProvideInfo = false)
        submitEmergencyRequest()
    }

    fun submitAdditionalInfo() {
        submitEmergencyRequest()
    }

    private fun submitEmergencyRequest() {
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.value = state.copy(isLoading = true)
            val phone = if (state.useMyPhone) state.userPhone else state.customPhone
            val location = if (state.useCurrentLocation) state.userLocationLabel else state.customLocationText
            val coords = if (state.useCurrentLocation) state.userCoordinates else ""

            val request = EmergencyRequest(
                locationLabel = location,
                coordinates = coords,
                contactPhone = phone,
                selectedCategoryIds = state.selectedCategoryIds.toList(),
                additionalInfo = if (state.wantsToProvideInfo == true) state.additionalInfo else null,
                useCurrentLocation = state.useCurrentLocation
            )

            emergencyRepository.submitEmergencyRequest(request).onSuccess { requestId ->
                _uiState.value = _uiState.value.copy(
                    currentState = PinchHelpState.FormSubmitted,
                    requestId = requestId,
                    isLoading = false,
                    activeTimelineStep = 2
                )
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    error = e.message,
                    isLoading = false
                )
            }
        }
    }

    fun helpOnTheWay() {
        _uiState.value = _uiState.value.copy(
            currentState = PinchHelpState.OnTheWay,
            activeTimelineStep = 3
        )
    }

    fun helpArrived() {
        _uiState.value = _uiState.value.copy(
            currentState = PinchHelpState.HelpArrived,
            activeTimelineStep = 4
        )
    }

    fun helpInProgress() {
        _uiState.value = _uiState.value.copy(
            currentState = PinchHelpState.InProgress,
            activeTimelineStep = 5
        )
    }

    fun emergencyResolved() {
        _uiState.value = _uiState.value.copy(
            currentState = PinchHelpState.Resolved,
            activeTimelineStep = 6
        )
        loadFeedbackConfig()
    }

    private fun loadFeedbackConfig() {
        viewModelScope.launch {
            feedbackRepository.getFeedbackConfig().onSuccess { config ->
                _uiState.value = _uiState.value.copy(feedbackConfig = config)
            }
        }
    }

    fun showFeedbackIntro() {
        _uiState.value = _uiState.value.copy(currentState = PinchHelpState.FeedbackIntro)
    }

    fun startFeedbackSurvey() {
        _uiState.value = _uiState.value.copy(
            currentState = PinchHelpState.FeedbackSurvey,
            currentQuestionIndex = 0,
            questionRatings = emptyMap()
        )
    }

    fun setQuestionRating(questionId: String, rating: Int) {
        val updated = _uiState.value.questionRatings.toMutableMap()
        updated[questionId] = rating
        _uiState.value = _uiState.value.copy(questionRatings = updated)
    }

    fun nextQuestion() {
        val config = _uiState.value.feedbackConfig ?: return
        val nextIndex = _uiState.value.currentQuestionIndex + 1
        if (nextIndex < config.questions.size) {
            _uiState.value = _uiState.value.copy(currentQuestionIndex = nextIndex)
        } else {
            submitFeedback()
        }
    }

    fun setOverallRating(rating: Int) {
        _uiState.value = _uiState.value.copy(overallRating = rating)
    }

    fun setFeedbackComment(comment: String) {
        _uiState.value = _uiState.value.copy(feedbackComment = comment)
    }

    fun submitFeedback() {
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.value = state.copy(isLoading = true)
            val submission = FeedbackSubmission(
                ratings = state.questionRatings,
                overallRating = state.overallRating,
                comment = state.feedbackComment
            )
            feedbackRepository.submitFeedback(state.requestId, submission).onSuccess {
                _uiState.value = _uiState.value.copy(
                    currentState = PinchHelpState.FeedbackComplete,
                    isLoading = false
                )
            }.onFailure {
                _uiState.value = _uiState.value.copy(
                    currentState = PinchHelpState.FeedbackComplete,
                    isLoading = false
                )
            }
        }
    }

    // Form field setters

    fun setUseCurrentLocation(use: Boolean) {
        _uiState.value = _uiState.value.copy(useCurrentLocation = use)
    }

    fun setCustomLocationText(text: String) {
        _uiState.value = _uiState.value.copy(customLocationText = text)
    }

    fun setUseMyPhone(use: Boolean) {
        _uiState.value = _uiState.value.copy(useMyPhone = use)
    }

    fun setCustomPhone(phone: String) {
        _uiState.value = _uiState.value.copy(customPhone = phone)
    }

    fun toggleCategory(categoryId: String) {
        val current = _uiState.value.selectedCategoryIds.toMutableSet()
        if (current.contains(categoryId)) {
            current.remove(categoryId)
        } else {
            current.add(categoryId)
        }
        _uiState.value = _uiState.value.copy(selectedCategoryIds = current)
    }

    fun setAdditionalInfo(info: String) {
        _uiState.value = _uiState.value.copy(additionalInfo = info)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    // Map location — called once from PinchHelpScreen with app context

    fun initLocationIfNeeded(context: Context) {
        if (_uiState.value.mapLocationLoaded) return
        viewModelScope.launch {
            val userLoc = getCurrentLocation(context)
            val distance = _uiState.value.responderDistanceMeters
            _uiState.value = _uiState.value.copy(
                userLatLng = userLoc,
                responderLatLng = responderPosition(userLoc, distance),
                medicalStations = medicalStationPositions(userLoc),
                mapLocationLoaded = true
            )
        }
    }

    fun setResponderDistance(meters: Double) {
        val userLoc = _uiState.value.userLatLng ?: return
        _uiState.value = _uiState.value.copy(
            responderDistanceMeters = meters,
            responderLatLng = responderPosition(userLoc, meters)
        )
    }

    // Cancel flow

    fun requestCancel() {
        _uiState.value = _uiState.value.copy(
            previousState = _uiState.value.currentState,
            currentState = PinchHelpState.CancelSwipe
        )
    }

    fun showCancelConfirmDialog() {
        _uiState.value = _uiState.value.copy(
            currentState = PinchHelpState.CancelConfirm
        )
    }

    fun confirmCancel() {
        _uiState.value = _uiState.value.copy(currentState = PinchHelpState.Cancelled)
    }

    fun dismissCancel() {
        _uiState.value = _uiState.value.copy(currentState = _uiState.value.previousState)
    }

    fun cancelSwipeBack() {
        _uiState.value = _uiState.value.copy(currentState = _uiState.value.previousState)
    }

    fun reset() {
        _uiState.value = PinchHelpUiState()
        loadInitialData()
    }

    // Validation helpers

    fun isLocationValid(): Boolean {
        val s = _uiState.value
        return s.useCurrentLocation || s.customLocationText.isNotBlank()
    }

    fun isPhoneValid(): Boolean {
        val s = _uiState.value
        return s.useMyPhone || s.customPhone.length >= 7
    }

    fun isCategorySelected(): Boolean {
        return _uiState.value.selectedCategoryIds.isNotEmpty()
    }

    fun isAdditionalInfoValid(): Boolean {
        return _uiState.value.additionalInfo.isNotBlank()
    }

    fun getCurrentQuestion(): FeedbackQuestion? {
        val config = _uiState.value.feedbackConfig ?: return null
        val index = _uiState.value.currentQuestionIndex
        return config.questions.getOrNull(index)
    }

    fun isLastQuestion(): Boolean {
        val config = _uiState.value.feedbackConfig ?: return true
        return _uiState.value.currentQuestionIndex >= config.questions.size - 1
    }

    class Factory(
        private val emergencyRepository: PinchEmergencyRepository,
        private val feedbackRepository: PinchFeedbackRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PinchHelpViewModel(emergencyRepository, feedbackRepository) as T
        }
    }
}
