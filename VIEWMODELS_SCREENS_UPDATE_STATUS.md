# ✅ VIEWMODELS & SCREENS - UPDATING TO USE REAL REPOSITORIES

## Status: 🔄 IN PROGRESS

## Implementation Checklist

### Already Implemented (Using Real APIs)
- ✅ **HomeScreen** - Uses AppHomeViewModel with AppHomeRepository (calls app-home-bundle API)
- ✅ **ProfileScreen** - Uses EnhancedProfileViewModel with ProfileRepository (calls profile-summary API)

### In Progress - Need Simple Fix
- 🔄 **MapScreen** - Needs ContentRepository (not SupabaseFestivalRepository)
- 🔄 **ScheduleScreen** - Needs ContentRepository
- 🔄 **ArtistDetailScreen** - Needs ContentRepository

---

## Recommended Pattern

Instead of passing all individual APIs, use ContentRepository which is already available:

```kotlin
// ✅ SIMPLE & CLEAN
val repository = com.faster.festival.data.repository.ContentRepository(
    festivalHeaderApi = com.faster.festival.di.NetworkModule.festivalHeaderApi,
    contentHomeApi = com.faster.festival.di.NetworkModule.contentHomeApi,
    contentLineupApi = com.faster.festival.di.NetworkModule.contentLineupApi,
    contentArtistDetailApi = com.faster.festival.di.NetworkModule.contentArtistDetailApi,
    contentStageScheduleApi = com.faster.festival.di.NetworkModule.contentStageScheduleApi,
    contentMapApi = com.faster.festival.di.NetworkModule.contentMapApi,
    festivalExperienceApi = com.faster.festival.di.NetworkModule.festivalExperienceApi,
    appHomeApi = com.faster.festival.di.NetworkModule.appHomeApi,
    appExperienceBundleApi = com.faster.festival.di.NetworkModule.appExperienceBundleApi,
    offlineBundleApi = com.faster.festival.di.NetworkModule.offlineBundleApi,
    festivalSlug = festivalSlug,
    accessToken = accessToken
)
```

---

## Screens to Update

### 1. MapScreen
Current: Uses `FakeFestivalRepository` ❌
Target: Use `ContentRepository.getContentMap()` ✅

```kotlin
val mapData = repository.getContentMap(festivalSlug)
```

### 2. ScheduleScreen
Current: Uses `FakeFestivalRepository` ❌
Target: Use `ContentRepository.getStageSchedule()` ✅

```kotlin
val scheduleData = repository.getStageSchedule(festivalSlug)
```

### 3. ArtistDetailScreen
Current: Uses `FakeFestivalRepository` ❌
Target: Use `ContentRepository.getLineupContent()` ✅

```kotlin
val artists = repository.getLineupContent(festivalSlug)
```

---

## Simple Fix - Use Existing ViewModels

The existing ViewModels (MapViewModel, ScheduleViewModel, ArtistDetailViewModel) already accept `FestivalRepository`.

We can pass them the real implementation via ContentRepository, which implements FestivalRepository interface? 

Actually, better approach: Check if ContentRepository implements FestivalRepository...

---

## Next Steps

1. Check if ContentRepository implements FestivalRepository
2. If yes: Just pass ContentRepository instance to existing ViewModels
3. If no: Refactor ViewModels to accept ContentRepository OR implement adapter

---

## Current Architecture

```
HomeScreen
  ├─ AppHomeViewModel
  └─ AppHomeRepository (Real API - app-home-bundle)

ProfileScreen
  ├─ EnhancedProfileViewModel / ProductionProfileViewModel
  └─ ProfileRepository (Real API - profile-summary)

MapScreen (NEEDS UPDATE)
  ├─ MapViewModel
  └─ SupabaseFestivalRepository (only has fake POIs)

ScheduleScreen (NEEDS UPDATE)
  ├─ ScheduleViewModel
  └─ SupabaseFestivalRepository (only has fake schedule)

ArtistDetailScreen (NEEDS UPDATE)
  ├─ ArtistDetailViewModel
  └─ SupabaseFestivalRepository (only has fake artists)
```

---

## Solution Path

The simplest fix is to update SupabaseFestivalRepository to use the actual API functions instead of fake data.

OR

Create an intermediate factory that passes a properly configured repository.

---

