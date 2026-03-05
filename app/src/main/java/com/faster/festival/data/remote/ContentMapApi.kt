package com.faster.festival.data.remote

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Retrofit API service for Content Map endpoint
 * GET /functions/v1/content-map?festival_slug=<slug>
 *
 * Returns: ContentMapResponse with map image, POIs, zones, facilities
 */
interface ContentMapApi {

    /**
     * Fetch map content (map image, points of interest, facilities)
     *
     * @param festivalSlug Festival identifier (required)
     * @return Response with map data
     */
    @GET("functions/v1/content-map")
    suspend fun getContentMap(
        @Query("festival_slug") festivalSlug: String
    ): Response<ContentMapResponse>

    data class ContentMapResponse(
        val schema_version: String,
        val map: MapData,
        val points_of_interest: List<PointOfInterest>,
        val zones: List<Zone>,
        val facilities: List<Facility>
    )

    data class MapData(
        val id: String,
        val image_url: String,
        val thumbnail_url: String?,
        val width: Int?,
        val height: Int?,
        val zoom_levels: List<ZoomLevel>?
    )

    data class ZoomLevel(
        val level: Int,
        val image_url: String
    )

    data class PointOfInterest(
        val id: String,
        val name: String,
        val type: String,
        val description: String?,
        val latitude: Double?,
        val longitude: Double?,
        val x_coordinate: Int?,
        val y_coordinate: Int?,
        val icon_url: String?,
        val image_url: String?
    )

    data class Zone(
        val id: String,
        val name: String,
        val zone_type: String,
        val description: String?,
        val coordinates: List<Coordinate>?,
        val color_hex: String?
    )

    data class Coordinate(
        val x: Int,
        val y: Int,
        val latitude: Double?,
        val longitude: Double?
    )

    data class Facility(
        val id: String,
        val name: String,
        val facility_type: String,
        val description: String?,
        val location: String?,
        val latitude: Double?,
        val longitude: Double?,
        val opening_hours: String?,
        val contact: String?
    )
}
