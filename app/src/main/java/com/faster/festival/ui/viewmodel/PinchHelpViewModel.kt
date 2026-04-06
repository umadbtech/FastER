package com.faster.festival.ui.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class PinchHelpState {
    Landing,
    SwipeForHelp,
    AlertSent,
    AnswerCall,
    OnTheWay,
    HelpArrived,
    InProgress,
    Resolved,
    FeedbackPrompt,
    FeedbackSurvey,
    FeedbackThankYou,
    CancelConfirm,
    Cancelled
}

data class PinchHelpUiState(
    val currentState: PinchHelpState = PinchHelpState.Landing,
    val etaMinutes: Int = 4,
    val dispatcherName: String = "FASTER Dispatcher",
    val dispatcherPhone: String = "+1 (555) 911-0100",
    val responderName: String = "Medical Team Alpha",
    val feedbackRating: Int = 0,
    val feedbackComment: String = ""
)

class PinchHelpViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(PinchHelpUiState())
    val uiState: StateFlow<PinchHelpUiState> = _uiState.asStateFlow()

    fun swipeToAlert() {
        _uiState.value = _uiState.value.copy(currentState = PinchHelpState.AlertSent)
    }

    fun proceedToAnswerCall() {
        _uiState.value = _uiState.value.copy(currentState = PinchHelpState.AnswerCall)
    }

    fun helpOnTheWay() {
        _uiState.value = _uiState.value.copy(currentState = PinchHelpState.OnTheWay)
    }

    fun helpArrived() {
        _uiState.value = _uiState.value.copy(currentState = PinchHelpState.HelpArrived)
    }

    fun helpInProgress() {
        _uiState.value = _uiState.value.copy(currentState = PinchHelpState.InProgress)
    }

    fun emergencyResolved() {
        _uiState.value = _uiState.value.copy(currentState = PinchHelpState.Resolved)
    }

    fun showFeedbackPrompt() {
        _uiState.value = _uiState.value.copy(currentState = PinchHelpState.FeedbackPrompt)
    }

    fun startFeedbackSurvey() {
        _uiState.value = _uiState.value.copy(currentState = PinchHelpState.FeedbackSurvey)
    }

    fun setFeedbackRating(rating: Int) {
        _uiState.value = _uiState.value.copy(feedbackRating = rating)
    }

    fun setFeedbackComment(comment: String) {
        _uiState.value = _uiState.value.copy(feedbackComment = comment)
    }

    fun submitFeedback() {
        _uiState.value = _uiState.value.copy(currentState = PinchHelpState.FeedbackThankYou)
    }

    fun requestCancel() {
        _uiState.value = _uiState.value.copy(currentState = PinchHelpState.CancelConfirm)
    }

    fun confirmCancel() {
        _uiState.value = _uiState.value.copy(currentState = PinchHelpState.Cancelled)
    }

    fun dismissCancel() {
        // Go back to the previous active state (AlertSent is the typical cancel-from state)
        _uiState.value = _uiState.value.copy(currentState = PinchHelpState.AlertSent)
    }

    fun navigateToSwipe() {
        _uiState.value = _uiState.value.copy(currentState = PinchHelpState.SwipeForHelp)
    }

    fun reset() {
        _uiState.value = PinchHelpUiState()
    }
}
