package com.faster.festival.data.pinch.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ═══════════════════════════════════════════════════════════════════════════════
// Emergency Categories
// ═══════════════════════════════════════════════════════════════════════════════

@Serializable
data class EmergencyCategoriesResponse(
    val categories: List<EmergencyCategoryDto>
)

@Serializable
data class EmergencyCategoryDto(
    val id: String,
    val title: String,
    val priority: Int,
    val color: String,
    val items: List<EmergencyItemDto>
)

@Serializable
data class EmergencyItemDto(
    val id: String,
    val label: String,
    val icon: String
)

// ═══════════════════════════════════════════════════════════════════════════════
// Feedback Questions
// ═══════════════════════════════════════════════════════════════════════════════

@Serializable
data class FeedbackQuestionsResponse(
    val questions: List<FeedbackQuestionDto>,
    @SerialName("intro_title") val introTitle: String,
    @SerialName("intro_message") val introMessage: String,
    @SerialName("intro_cta") val introCta: String,
    @SerialName("completion_title") val completionTitle: String,
    @SerialName("completion_message") val completionMessage: String
)

@Serializable
data class FeedbackQuestionDto(
    val id: String,
    val text: String,
    val order: Int,
    @SerialName("scale_min") val scaleMin: Int,
    @SerialName("scale_max") val scaleMax: Int,
    @SerialName("scale_min_label") val scaleMinLabel: String,
    @SerialName("scale_max_label") val scaleMaxLabel: String
)

// ═══════════════════════════════════════════════════════════════════════════════
// Status Timeline
// ═══════════════════════════════════════════════════════════════════════════════

@Serializable
data class StatusTimelineResponse(
    val statuses: List<StatusStepDto>,
    @SerialName("eta_format") val etaFormat: String,
    @SerialName("default_eta_minutes") val defaultEtaMinutes: Int,
    @SerialName("nearest_location") val nearestLocation: String
)

@Serializable
data class StatusStepDto(
    val id: String,
    val label: String,
    val icon: String,
    val order: Int
)

// ═══════════════════════════════════════════════════════════════════════════════
// Mock User Context
// ═══════════════════════════════════════════════════════════════════════════════

@Serializable
data class MockUserContextResponse(
    val user: MockUserDto,
    val location: MockLocationDto,
    val dispatcher: MockDispatcherDto,
    val responder: MockResponderDto
)

@Serializable
data class MockUserDto(
    val id: String,
    val name: String,
    val phone: String,
    @SerialName("wristband_id") val wristbandId: String
)

@Serializable
data class MockLocationDto(
    @SerialName("current_label") val currentLabel: String,
    val coordinates: String,
    @SerialName("gps_text") val gpsText: String
)

@Serializable
data class MockDispatcherDto(
    val name: String,
    val phone: String
)

@Serializable
data class MockResponderDto(
    val name: String,
    @SerialName("default_eta_minutes") val defaultEtaMinutes: Int,
    @SerialName("nearest_station") val nearestStation: String
)

// ═══════════════════════════════════════════════════════════════════════════════
// Domain Models (UI-facing)
// ═══════════════════════════════════════════════════════════════════════════════

data class EmergencyCategory(
    val id: String,
    val title: String,
    val priority: Int,
    val colorHex: String,
    val items: List<EmergencyItem>
)

data class EmergencyItem(
    val id: String,
    val label: String,
    val icon: String
)

data class FeedbackQuestion(
    val id: String,
    val text: String,
    val order: Int,
    val scaleMin: Int,
    val scaleMax: Int,
    val scaleMinLabel: String,
    val scaleMaxLabel: String
)

data class FeedbackConfig(
    val questions: List<FeedbackQuestion>,
    val introTitle: String,
    val introMessage: String,
    val introCta: String,
    val completionTitle: String,
    val completionMessage: String
)

data class StatusStep(
    val id: String,
    val label: String,
    val icon: String,
    val order: Int
)

data class TimelineConfig(
    val steps: List<StatusStep>,
    val etaFormat: String,
    val defaultEtaMinutes: Int,
    val nearestLocation: String
)

data class UserContext(
    val userId: String,
    val userName: String,
    val userPhone: String,
    val wristbandId: String,
    val locationLabel: String,
    val coordinates: String,
    val gpsText: String,
    val dispatcherName: String,
    val dispatcherPhone: String,
    val responderName: String,
    val defaultEtaMinutes: Int,
    val nearestStation: String
)

data class EmergencyRequest(
    val locationLabel: String,
    val coordinates: String,
    val contactPhone: String,
    val selectedCategoryIds: List<String>,
    val additionalInfo: String?,
    val useCurrentLocation: Boolean
)

data class FeedbackSubmission(
    val ratings: Map<String, Int>,
    val overallRating: Int,
    val comment: String
)

// ═══════════════════════════════════════════════════════════════════════════════
// Mappers
// ═══════════════════════════════════════════════════════════════════════════════

fun EmergencyCategoryDto.toDomain() = EmergencyCategory(
    id = id,
    title = title,
    priority = priority,
    colorHex = color,
    items = items.map { it.toDomain() }
)

fun EmergencyItemDto.toDomain() = EmergencyItem(
    id = id,
    label = label,
    icon = icon
)

fun FeedbackQuestionDto.toDomain() = FeedbackQuestion(
    id = id,
    text = text,
    order = order,
    scaleMin = scaleMin,
    scaleMax = scaleMax,
    scaleMinLabel = scaleMinLabel,
    scaleMaxLabel = scaleMaxLabel
)

fun FeedbackQuestionsResponse.toDomain() = FeedbackConfig(
    questions = questions.map { it.toDomain() }.sortedBy { it.order },
    introTitle = introTitle,
    introMessage = introMessage,
    introCta = introCta,
    completionTitle = completionTitle,
    completionMessage = completionMessage
)

fun StatusStepDto.toDomain() = StatusStep(
    id = id,
    label = label,
    icon = icon,
    order = order
)

fun StatusTimelineResponse.toDomain() = TimelineConfig(
    steps = statuses.map { it.toDomain() }.sortedBy { it.order },
    etaFormat = etaFormat,
    defaultEtaMinutes = defaultEtaMinutes,
    nearestLocation = nearestLocation
)

fun MockUserContextResponse.toDomain() = UserContext(
    userId = user.id,
    userName = user.name,
    userPhone = user.phone,
    wristbandId = user.wristbandId,
    locationLabel = location.currentLabel,
    coordinates = location.coordinates,
    gpsText = location.gpsText,
    dispatcherName = dispatcher.name,
    dispatcherPhone = dispatcher.phone,
    responderName = responder.name,
    defaultEtaMinutes = responder.defaultEtaMinutes,
    nearestStation = responder.nearestStation
)
