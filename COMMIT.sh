#!/bin/bash
# Git commit script for Home Screen API Integration fix

# Stage all changes
git add -A

# Commit with proper message
git commit -m "fix: Update AppHomeBundleResponse models to handle nested module structure from API

## Summary
Fixed Home screen API integration to properly parse the server response where
hero_carousel_items, announcements, and upcoming_events are nested inside the
modules array, not at the top level.

## Changes Made

### Data Models (AppHomeBundleModels.kt)
- **AppHomeBundleResponse**: Removed top-level hero_carousel_items, announcements,
  upcoming_events fields. Added computed properties that extract data from
  modules array by key.

- **HomeModule**: Changed data field from \`Map<String, String>?\` to \`JsonElement?\`
  to handle flexible JSON structures (arrays, objects, primitives).

- **HeroCarouselItem**: Added missing fields from API response:
  - kind, refId, ctaLabel, sortOrder, startsAt, endsAt

- **UpcomingEvent**: Updated to match API response:
  - Changed startTime/endTime to startsAt/endsAt
  - Added name, status fields

- **Venue**: Added kind, slug fields

### Data Extraction Logic
- Added computed property methods in AppHomeBundleResponse:
  - \`heroCarouselItems: List<HeroCarouselItem>\` - extracts from modules[key=\"hero_carousel\"]
  - \`announcements: List<Announcement>\` - extracts from modules[key=\"announcements\"]
  - \`upcomingEvents: List<UpcomingEvent>\` - extracts from modules[key=\"upcoming_events\"]

- Each property safely converts JsonElement (JsonArray) to strongly-typed Kotlin objects
- Handles null/empty cases and parsing errors gracefully

## API Response Structure
Backend now returns proper module structure:
```
{
  \"modules\": [
    {
      \"key\": \"hero_carousel\",
      \"data\": [ /* HeroCarouselItem array */ ]
    },
    {
      \"key\": \"announcements\",
      \"data\": [ /* Announcement array or empty */ ]
    },
    {
      \"key\": \"upcoming_events\",
      \"data\": [ /* UpcomingEvent array */ ]
    }
  ]
}
```

## Testing
- [x] Models compile without errors
- [x] JsonElement properly serialized/deserialized by kotlinx.serialization
- [x] HomeScreen UI unchanged (uses same property names)
- [x] Error handling unchanged
- [x] Empty arrays display proper empty state messages

## Related
- Fixes 401 errors by properly handling optional Authorization header
- Backend JWT verification now disabled for GET endpoints"

# Show what was committed
echo ""
echo "✅ Commit completed!"
echo ""
git log -1 --oneline
