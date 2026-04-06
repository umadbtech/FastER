package com.faster.festival.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Retrofit API service for Content FAQ endpoint
 * GET /functions/v1/content-faq?festival_slug=<slug>
 *
 * Supports optional filters: phase, category, q (search)
 */
interface ContentFaqApi {

    @GET("functions/v1/content-faq")
    suspend fun getFaq(
        @Query("festival_slug") festivalSlug: String,
        @Query("phase") phase: String? = null,
        @Query("category") category: String? = null,
        @Query("q") query: String? = null
    ): Response<ContentFaqResponse>
}

@Serializable
data class ContentFaqResponse(
    @SerialName("ok")
    val ok: Boolean = true,
    @SerialName("categories")
    val categories: List<FaqCategory> = emptyList(),
    @SerialName("items")
    val items: List<FaqApiItem> = emptyList()
)

@Serializable
data class FaqCategory(
    @SerialName("id")
    val id: String,
    @SerialName("key")
    val key: String,
    @SerialName("label")
    val label: String,
    @SerialName("description")
    val description: String? = null,
    @SerialName("sort_order")
    val sortOrder: Int = 0
)

@Serializable
data class FaqApiItem(
    @SerialName("id")
    val id: String,
    @SerialName("question")
    val question: String,
    @SerialName("answer")
    val answer: String,
    @SerialName("category_keys")
    val categoryKeys: List<String> = emptyList(),
    @SerialName("sort_order")
    val sortOrder: Int = 0
)
