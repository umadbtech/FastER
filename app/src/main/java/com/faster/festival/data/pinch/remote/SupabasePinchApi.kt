package com.faster.festival.data.pinch.remote

/**
 * ═══════════════════════════════════════════════════════════════════════════════
 * SUPABASE MIGRATION GUIDE — Pinch Emergency Flow
 * ═══════════════════════════════════════════════════════════════════════════════
 *
 * PHASE 2: Replace local assets with Supabase backend
 *
 * INTERFACES THAT STAY UNCHANGED:
 *   - PinchEmergencyRepository (data/pinch/repository/)
 *   - PinchFeedbackRepository (data/pinch/repository/)
 *   - All domain models in data/pinch/model/PinchModels.kt
 *   - All UI code in ui/screens/PinchHelpScreen.kt
 *   - PinchHelpViewModel (ui/viewmodel/)
 *
 * WHAT GETS REPLACED:
 *   - FakeEmergencyApi → SupabaseEmergencyApi (this file)
 *   - FakeFeedbackApi → SupabaseFeedbackApi (this file)
 *   - AssetsEmergencyRepository → SupabaseEmergencyRepository
 *   - AssetsFeedbackRepository → SupabaseFeedbackRepository
 *   - AssetsReader is no longer needed for pinch flow
 *
 * REPOSITORY SWAP (in PinchModule.kt or your DI framework):
 *   Change:
 *     val emergencyRepo: PinchEmergencyRepository = AssetsEmergencyRepository(fakeApi)
 *   To:
 *     val emergencyRepo: PinchEmergencyRepository = SupabaseEmergencyRepository(supabaseApi)
 *
 * ENVIRONMENT / CONFIG VALUES NEEDED:
 *   - SUPABASE_URL (already in BuildConfig as VITE_SUPABASE_URL)
 *   - SUPABASE_ANON_KEY (already in BuildConfig as VITE_SUPABASE_ANON_KEY)
 *   - User auth token from EncryptedSessionManager
 *
 * SUPABASE KOTLIN CLIENT SETUP:
 *   Add dependency:
 *     implementation("io.github.jan-tennert.supabase:postgrest-kt:VERSION")
 *     implementation("io.github.jan-tennert.supabase:gotrue-kt:VERSION")
 *     implementation("io.github.jan-tennert.supabase:realtime-kt:VERSION")  // optional
 *     implementation("io.github.jan-tennert.supabase:storage-kt:VERSION")   // optional
 *     implementation("io.ktor:ktor-client-android:VERSION")
 *
 *   Create client:
 *     val supabase = createSupabaseClient(
 *         supabaseUrl = BuildConfig.VITE_SUPABASE_URL,
 *         supabaseKey = BuildConfig.VITE_SUPABASE_ANON_KEY
 *     ) {
 *         install(Postgrest)
 *         install(GoTrue)
 *         install(Realtime)  // optional — for live status updates
 *         install(Storage)   // optional — for file uploads
 *     }
 *
 * SUGGESTED SUPABASE TABLES:
 *
 *   emergency_categories:
 *     id (uuid, PK), title (text), priority (int), color (text)
 *
 *   emergency_category_items:
 *     id (uuid, PK), category_id (uuid, FK), label (text), icon (text)
 *
 *   emergency_requests:
 *     id (uuid, PK), user_id (uuid, FK), location_label (text),
 *     coordinates (text), contact_phone (text), use_current_location (bool),
 *     additional_info (text, nullable), status (text), created_at (timestamptz)
 *
 *   emergency_request_categories:
 *     request_id (uuid, FK), category_item_id (uuid, FK)
 *     — junction table for many-to-many
 *
 *   emergency_updates:
 *     id (uuid, PK), request_id (uuid, FK), status (text),
 *     message (text, nullable), eta_minutes (int, nullable),
 *     responder_name (text, nullable), created_at (timestamptz)
 *
 *   feedback_questions:
 *     id (uuid, PK), text (text), order (int),
 *     scale_min (int), scale_max (int),
 *     scale_min_label (text), scale_max_label (text)
 *
 *   feedback_responses:
 *     id (uuid, PK), request_id (uuid, FK), question_id (uuid, FK),
 *     rating (int), created_at (timestamptz)
 *
 *   feedback_overall:
 *     id (uuid, PK), request_id (uuid, FK), overall_rating (int),
 *     comment (text), created_at (timestamptz)
 *
 *   user_profiles:
 *     id (uuid, PK, FK to auth.users), name (text), phone (text),
 *     wristband_id (text, nullable), avatar_url (text, nullable)
 *
 * JSON-TO-TABLE MAPPING:
 *   emergency_categories.json → emergency_categories + emergency_category_items
 *   feedback_questions.json   → feedback_questions + app_config (for intro/completion text)
 *   status_timeline.json      → app_config (or hardcoded in client)
 *   mock_user_context.json    → user_profiles + live dispatcher/responder assignment
 *
 * REALTIME (OPTIONAL):
 *   Subscribe to emergency_updates table filtered by request_id
 *   to receive live status pushes (help dispatched, ETA update, arrived, etc.)
 *   instead of polling or manual state transitions.
 *
 * EDGE FUNCTIONS (OPTIONAL):
 *   - submit-emergency: validates request, assigns dispatcher, creates emergency_updates
 *   - submit-feedback: validates ratings, stores in feedback_responses + feedback_overall
 */

interface SupabaseEmergencyApi {
    suspend fun fetchCategories(): List<SupabaseCategoryRow>
    suspend fun fetchTimeline(): List<SupabaseTimelineRow>
    suspend fun fetchUserProfile(userId: String): SupabaseUserRow
    suspend fun createEmergencyRequest(row: SupabaseEmergencyRequestRow): String
}

interface SupabaseFeedbackApi {
    suspend fun fetchFeedbackQuestions(): List<SupabaseFeedbackQuestionRow>
    suspend fun submitFeedbackResponses(responses: List<SupabaseFeedbackResponseRow>)
    suspend fun submitFeedbackOverall(overall: SupabaseFeedbackOverallRow)
}

data class SupabaseCategoryRow(
    val id: String,
    val title: String,
    val priority: Int,
    val color: String,
    val items: List<SupabaseCategoryItemRow>
)

data class SupabaseCategoryItemRow(
    val id: String,
    val categoryId: String,
    val label: String,
    val icon: String
)

data class SupabaseTimelineRow(
    val id: String,
    val label: String,
    val icon: String,
    val order: Int
)

data class SupabaseUserRow(
    val id: String,
    val name: String,
    val phone: String,
    val wristbandId: String?
)

data class SupabaseEmergencyRequestRow(
    val userId: String,
    val locationLabel: String,
    val coordinates: String,
    val contactPhone: String,
    val useCurrentLocation: Boolean,
    val additionalInfo: String?,
    val categoryItemIds: List<String>
)

data class SupabaseFeedbackQuestionRow(
    val id: String,
    val text: String,
    val order: Int,
    val scaleMin: Int,
    val scaleMax: Int,
    val scaleMinLabel: String,
    val scaleMaxLabel: String
)

data class SupabaseFeedbackResponseRow(
    val requestId: String,
    val questionId: String,
    val rating: Int
)

data class SupabaseFeedbackOverallRow(
    val requestId: String,
    val overallRating: Int,
    val comment: String
)
