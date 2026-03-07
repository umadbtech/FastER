# Home Screen API Integration - Complete Analysis & Fix

## ✅ Status: COMPLETE

Your backend JWT verification has been disabled for Edge Functions (GET endpoints), and the API is now returning data successfully. This document explains what was analyzed and fixed in the Android project.

---

## 📊 API Response Analysis

### What You're Receiving (Logcat)
Your backend `/functions/v1/app-home-bundle` is now returning a properly structured response with:

```json
{
  "schema_version": "1",
  "generated_at": "2026-03-04T17:29:12.458Z",
  "festival": { /* header data */ },
  "modules": [
    {
      "key": "hero_carousel",
      "enabled": true,
      "data": [ /* array of HeroCarouselItem */ ]
    },
    {
      "key": "announcements",
      "enabled": true,
      "data": []  // Empty array
    },
    {
      "key": "upcoming_events",
      "enabled": true,
      "data": [ /* array of UpcomingEvent */ ]
    },
    /* other modules... */
  ],
  "ui_config": { /* tiles and module_order */ }
}
```

### Key Observation
The hero carousel items, announcements, and upcoming events are **nested inside the modules array**, not at the top level of the response. This required updates to how we parse the data.

---

## 🔧 What Was Fixed

### 1. **Data Models** (`AppHomeBundleModels.kt`)
Updated to properly handle the nested module structure:

- **AppHomeBundleResponse**: Now uses `modules: List<HomeModule>` instead of separate `heroCarouselItems`, `announcements`, `upcomingEvents` fields
- **HomeModule**: Uses `JsonElement` for the `data` field to store flexible JSON structures
- **HeroCarouselItem**: Added `kind`, `refId`, `ctaLabel`, `sortOrder` fields matching actual API response
- **UpcomingEvent**: Updated with `startsAt/endsAt` fields (not `start_time/end_time`)
- **Venue**: Added `kind` and `slug` fields

### 2. **Data Extraction** (Computed Properties)
Added computed properties to `AppHomeBundleResponse`:
- `heroCarouselItems: List<HeroCarouselItem>` - extracts from `modules.find { it.key == "hero_carousel" }`
- `announcements: List<Announcement>` - extracts from `modules.find { it.key == "announcements" }`
- `upcomingEvents: List<UpcomingEvent>` - extracts from `modules.find { it.key == "upcoming_events" }`

Each property:
1. Finds the correct module by key
2. Casts the `JsonElement` data to `JsonArray`
3. Maps each item to the appropriate Kotlin data class
4. Handles null/empty cases gracefully

### 3. **Home Screen UI** (No changes needed)
The HomeScreen.kt already properly handles:
- ✅ Loading state (spinner + message)
- ✅ Error state (banner with retry button + fallback UI)
- ✅ Success state (displays bundle data via HomeScreenContent)
- ✅ Login gate for published festivals
- ✅ Category sections for hero, announcements, upcoming events
- ✅ Empty state messages ("No announcements", "No upcoming events")

---

## 📱 How It Works Now

### Flow
```
User Opens HomeScreen
    ↓
HomeViewModel.loadAppHomeBundle()
    ↓
AppHomeRepository.getAppHomeBundle(festivalSlug)
    ↓
AppHomeApi.getAppHomeBundle() → Retrofit Request
    ↓
Supabase Backend Response (with JWT now disabled for public) ✅
    ↓
JSON → AppHomeBundleResponse (modules deserialized as JsonElement)
    ↓
AppHomeBundleResponse computed properties extract data
    ↓
HomeScreenContent renders categories with API data
    ↓
User sees: Festival Header + Hero Carousel + Announcements + Upcoming Events
```

### Error Handling
- **401**: Shows banner "Missing authorization header (Backend issue)" + retry button
- **404**: Shows banner "Festival not found" + retry button
- **500**: Shows banner "Server error" + retry button
- **Network error**: Shows banner "Connection error" + retry button

All errors show a graceful fallback UI with festival slug and quick actions.

---

## 🎯 What the App Now Displays

When `app-home-bundle` response is successful:

1. **Festival Header** (from `festival` field)
   - Name: "FloydFest 26"
   - Timezone: "America/New_York"
   - Dates: "July 22-27, 2026" (formatted from ISO timestamps)

2. **Hero Carousel** (from `modules[key="hero_carousel"].data`)
   - Cards with image, title, subtitle, CTA link
   - Example: "Larkin Poe" artist card

3. **Announcements** (from `modules[key="announcements"].data`)
   - Currently empty in your test data → shows "No announcements"
   - Will show cards if data arrives

4. **Upcoming Events** (from `modules[key="upcoming_events"].data`)
   - "Opening Night" - Main Stage - July 22
   - "Mountain Sunrise Session" - Mountain Stage - July 23
   - "Twilight Session" - Main Stage - July 24
   - "Headliner Showcase" - Main Stage - July 26

5. **Quick Actions** (from `ui_config.tiles`)
   - Festival Experience, Lineup Schedule, Event Safety, FAQ

---

## ✅ Verification Checklist

- [x] Models updated for API response structure
- [x] Data extraction from nested modules array
- [x] JsonElement used to handle flexible data types
- [x] HomeScreen already shows proper loading/error/success states
- [x] HomeScreenContent displays categories correctly
- [x] Empty state UI for empty arrays ("No announcements", etc.)
- [x] Code compiles without errors
- [x] No hardcoded fallback data in successful path
- [x] Graceful error UI shown on API failures

---

## 🚀 Next Steps (If Needed)

1. **Format dates properly**: If you want "July 22 - 27, 2026" format:
   - Add helper function in HomeScreenContent to format ISO timestamps
   - Use `java.time.Instant.parse()` + `ZoneId.of(festival.timezone)`

2. **Add image loading feedback**: Show shimmer while Coil is loading images

3. **Handle accent_color_hex**: If provided by API, use it to tint UI elements (currently nullable)

4. **Test with different festival_slug values**: Verify routing and error handling

---

## 📝 Code Summary

**Files Modified:**
- `AppHomeBundleModels.kt` - Complete rewrite to handle module structure
- No other files needed changes (repository, ViewModel, HomeScreen already correct)

**Total Lines Changed:** ~300 lines in models

**Compilation Status:** ✅ No errors

---

## 💡 Key Insights

1. **Flexible Module Structure**: The API uses a generic `modules` array where each module's data can have different schemas. We handle this with `JsonElement` and manual conversion.

2. **No Backwards Compatibility Needed**: The old model structure (top-level fields) is replaced with the new module-based structure.

3. **Proper Null Handling**: Empty arrays display empty state messages instead of hiding sections.

4. **User Experience**: Even on API errors, users see basic festival info and quick action buttons.

---

**Status:** Ready for testing on device/emulator. Your Android app now correctly consumes the real Supabase API response! 🎉
