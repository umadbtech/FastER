package com.faster.festival.data.repository

import com.faster.festival.data.models.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

// Repository Interface
interface FestivalRepository {
    fun getFestival(): Flow<Festival>
    fun getArtists(): Flow<List<Artist>>
    fun getArtistById(id: String): Flow<Artist?>
    fun getPois(): Flow<List<Poi>>
    fun getSchedule(): Flow<List<ScheduleItem>>
    fun getProfile(): Flow<AccountProfile>
    fun updateProfile(profile: AccountProfile): Flow<AccountProfile>
}

// Fake In-Memory Repository
class FakeFestivalRepository : FestivalRepository {

    private val festival = Festival(
        id = "1",
        name = "FASTER",
        location = "Desert Valley, California",
        date = "May 15-17, 2026",
        description = "The fastest-growing music festival experience"
    )

    private val artists = listOf(
        Artist(
            id = "1",
            name = "Luna Echo",
            bio = "Luna Echo is a mesmerizing electronic artist known for immersive live performances. Their signature sound blends ambient textures with driving beats, creating an unforgettable sonic journey.",
            sets = listOf(
                FestivalSet("1", "Opening Night", "Main Stage", "8:00 PM", "9:15 PM"),
                FestivalSet("2", "Sunset Session", "Campground Stage", "4:30 PM", "5:45 PM")
            )
        ),
        Artist(
            id = "2",
            name = "The Midnight Collective",
            bio = "A high-energy rock band that brings raw power and emotional depth to every performance. The Midnight Collective has captivated audiences worldwide with their anthemic sound.",
            sets = listOf(
                FestivalSet("3", "Evening Peak", "Main Stage", "10:00 PM", "11:30 PM")
            )
        ),
        Artist(
            id = "3",
            name = "Harmony Waves",
            bio = "Harmony Waves delivers soul-stirring indie pop that resonates with audiences of all ages. Their poetic lyrics and infectious melodies have earned them a dedicated following.",
            sets = listOf(
                FestivalSet("4", "Afternoon Vibes", "Mountain Stage", "2:00 PM", "3:15 PM")
            )
        ),
        Artist(
            id = "4",
            name = "Desert Bloom",
            bio = "Desert Bloom brings world music fusion with folk storytelling traditions. Their eclectic instrumentation and cross-cultural collaboration create a truly unique festival experience.",
            sets = listOf(
                FestivalSet("5", "Cultural Hour", "Workshop Tent", "3:00 PM", "4:00 PM")
            )
        ),
        Artist(
            id = "5",
            name = "Neon Dreams",
            bio = "Neon Dreams is a synth-pop sensation with futuristic production and dance-floor energy. Their visually stunning performances are a highlight of any festival lineup.",
            sets = listOf(
                FestivalSet("6", "Late Night", "Main Stage", "12:00 AM", "1:15 AM")
            )
        ),
        Artist(
            id = "6",
            name = "The Jazz Collective",
            bio = "The Jazz Collective showcases virtuosity and improvisation. From smooth standards to modern jazz fusion, they bring sophisticated musical excellence to the festival.",
            sets = listOf(
                FestivalSet("7", "Sophisticated Evening", "Campground Stage", "7:00 PM", "8:15 PM")
            )
        )
    )

    private val pois = listOf(
        Poi("1", "Main Stage", "stage", 37.7749, -122.4194, "Primary performance venue"),
        Poi("2", "Campground Stage", "stage", 37.7750, -122.4195, "Secondary stage"),
        Poi("3", "Mountain Stage", "stage", 37.7748, -122.4193, "Outdoor mountain venue"),
        Poi("4", "Info/Box Office", "info", 37.7751, -122.4196, "Tickets and information"),
        Poi("5", "Vendor Village", "food", 37.7747, -122.4192, "Food and merchandise"),
        Poi("6", "Workshop Tent", "workshop", 37.7746, -122.4191, "Educational sessions")
    )

    private val scheduleItems = listOf(
        ScheduleItem("1", "Main Stage", "Luna Echo", "8:00 PM", "9:15 PM", "May 15"),
        ScheduleItem("2", "Main Stage", "The Midnight Collective", "10:00 PM", "11:30 PM", "May 15"),
        ScheduleItem("3", "Mountain Stage", "Harmony Waves", "2:00 PM", "3:15 PM", "May 16"),
        ScheduleItem("4", "Campground Stage", "The Jazz Collective", "7:00 PM", "8:15 PM", "May 16"),
        ScheduleItem("5", "Workshop Tent", "Desert Bloom", "3:00 PM", "4:00 PM", "May 16"),
        ScheduleItem("6", "Main Stage", "Neon Dreams", "12:00 AM", "1:15 AM", "May 17"),
        ScheduleItem("7", "Campground Stage", "Luna Echo", "4:30 PM", "5:45 PM", "May 17")
    )

    private var profile = AccountProfile(
        id = "1",
        name = "Alex Johnson",
        email = "alex@example.com",
        phone = "",
        emergencyContact = "",
        allergies = "",
        medications = ""
    )

    override fun getFestival(): Flow<Festival> = flowOf(festival)

    override fun getArtists(): Flow<List<Artist>> = flowOf(artists)

    override fun getArtistById(id: String): Flow<Artist?> = flowOf(artists.find { it.id == id })

    override fun getPois(): Flow<List<Poi>> = flowOf(pois)

    override fun getSchedule(): Flow<List<ScheduleItem>> = flowOf(scheduleItems)

    override fun getProfile(): Flow<AccountProfile> = flowOf(profile)

    override fun updateProfile(profile: AccountProfile): Flow<AccountProfile> {
        this.profile = profile
        return flowOf(profile)
    }
}
