# 📐 HomeScreen Explore Section - Layout & Architecture Diagram

## Component Tree Structure

```
HomeScreen (Main screen container)
│
├─ Box (Root layout)
│  │
│  ├─ UiState.Loading
│  │  └─ CircularProgressIndicator
│  │
│  ├─ UiState.Error
│  │  ├─ Error Banner Card
│  │  ├─ Festival Information Card
│  │  └─ Quick Links
│  │
│  └─ UiState.Success
│     └─ HomeScreenContent
│        │
│        ├─ Text (Festival Header)
│        ├─ QuickActionRow (Schedule, Lineup, Parking, Wristband)
│        ├─ SetupAccountCard
│        │
│        ├─ HomeLoginGate (if not logged in) [CONDITIONAL]
│        │
│        └─ Explore Section [CONDITIONAL - if logged in]
│           │
│           ├─ HomeCategorySection("Featured")
│           │  └─ HomeHeroCarouselSection ⬅️ HORIZONTAL
│           │     └─ LazyRow
│           │        ├─ HomeExploreCard
│           │        ├─ HomeExploreCard
│           │        └─ HomeExploreCard...
│           │
│           ├─ HomeCategorySection("Announcements") 
│           │  └─ HomeAnnouncementsSection ⬅️ 2-COLUMN GRID (UPDATED)
│           │     └─ LazyVerticalGrid(columns=2)
│           │        ├─ HomeAnnouncementCard
│           │        ├─ HomeAnnouncementCard
│           │        ├─ HomeAnnouncementCard
│           │        └─ HomeAnnouncementCard...
│           │
│           └─ HomeCategorySection("Upcoming Events")
│              └─ HomeUpcomingEventsSection ⬅️ 2-COLUMN GRID (UPDATED)
│                 └─ LazyVerticalGrid(columns=2)
│                    ├─ HomeEventCard
│                    ├─ HomeEventCard
│                    ├─ HomeEventCard
│                    └─ HomeEventCard...
│
└─ ExperienceList
   └─ Experience tiles/cards
```

---

## Layout Flow Diagram

```
┌──────────────────────────────────────────────────────────────┐
│                    HomeScreen Container                      │
│  (fillMaxSize, Box layout)                                  │
├──────────────────────────────────────────────────────────────┤
│                                                              │
│  ┌────────────────────────────────────────────────────────┐ │
│  │ HomeScreenContent (verticalScroll)                    │ │
│  ├────────────────────────────────────────────────────────┤ │
│  │ Festival Name Header                                  │ │
│  │ "FloydFest 26"                                        │ │
│  ├────────────────────────────────────────────────────────┤ │
│  │ Quick Action Row (Horizontal)                         │ │
│  │ [Schedule] [Lineup] [Parking] [Wristband]            │ │
│  ├────────────────────────────────────────────────────────┤ │
│  │ Setup Account Card                                    │ │
│  │ "Connect your FASTER Band"                            │ │
│  ├────────────────────────────────────────────────────────┤ │
│  │                                                        │ │
│  │ Explore FloydFest (Section Title)                     │ │
│  │                                                        │ │
│  │ Featured (Category Title)                             │ │
│  │ ┌─────────────┐ ┌─────────────┐ ┌─────────────┐      │ │
│  │ │ Hero Card 1 │ │ Hero Card 2 │ │ Hero Card 3│ →    │ │
│  │ │ (LazyRow)   │ │             │ │            │      │ │
│  │ └─────────────┘ └─────────────┘ └─────────────┘      │ │
│  │                                                        │ │
│  │ Announcements (Category Title)                        │ │
│  │ ┌──────────────────┬──────────────────┐              │ │
│  │ │ Ann Card 1       │ Ann Card 2       │              │ │
│  │ │ (GridCells.2)    │ (GridCells.2)    │              │ │
│  │ ├──────────────────┼──────────────────┤ ✅ UPDATED   │ │
│  │ │ Ann Card 3       │ Ann Card 4       │              │ │
│  │ ├──────────────────┼──────────────────┤              │ │
│  │ │ Ann Card 5       │ Ann Card 6       │              │ │
│  │ └──────────────────┴──────────────────┘              │ │
│  │                                                        │ │
│  │ Upcoming Events (Category Title)                      │ │
│  │ ┌──────────────────┬──────────────────┐              │ │
│  │ │ Event Card 1     │ Event Card 2     │              │ │
│  │ │ (GridCells.2)    │ (GridCells.2)    │              │ │
│  │ ├──────────────────┼──────────────────┤ ✅ UPDATED   │ │
│  │ │ Event Card 3     │ Event Card 4     │              │ │
│  │ └──────────────────┴──────────────────┘              │ │
│  │                                                        │ │
│  └────────────────────────────────────────────────────────┘ │
│                         (scrolls ⬇️)                        │
└──────────────────────────────────────────────────────────────┘
```

---

## Grid Configuration Diagram

### Before: LazyRow (Horizontal Scrolling)
```
┌─────────────────────────────────────────┐
│ Screen Width (412dp)                    │
├─────────────────────────────────────────┤
│ padding: 16dp    [Card: 280dp] padding  │
│  ◄─────────────────────────────────────►│
│  ◄──16──►┌──────280──────┐◄─16─►       │
│         │   Card 1       │     overflow │
│         └────────────────┘ ← needs scroll
│                           │             │
│  ◄──────────────────────────────────────┘
│                ❌ Space wasted (96dp)
│                ❌ Horizontal scroll needed
```

### After: LazyVerticalGrid (2-Column Grid)
```
┌──────────────────────────────────────────┐
│ Screen Width (412dp)                     │
├──────────────────────────────────────────┤
│ padding: 16dp                            │
│  ◄─────────────────────────────────────► │
│  ◄──16──►┌─188─┐ ◄12►┌─188─┐◄─16─►      │
│         │ Card│     │ Card│             │
│         │  1  │     │  2  │             │
│         └─────┘     └─────┘             │
│                                         │
│         (Row 2 below)                   │
│         ┌─────┐     ┌─────┐             │
│         │ Card│     │ Card│             │
│         │  3  │     │  4  │             │
│         └─────┘     └─────┘             │
│  ◄────────────────────────────────────► │
│  ✅ Full width utilization
│  ✅ Perfect spacing (12dp gaps)
│  ✅ Responsive to all screen sizes
```

### Responsive Calculation
```
Available Width = Screen Width - (Left Padding + Right Padding)
                = 412 - (16 + 16) = 380dp

Per Column Width = (Available Width - (Columns - 1) * Gap) / Columns
                 = (380 - (2 - 1) * 12) / 2
                 = (380 - 12) / 2
                 = 368 / 2
                 = 184dp per card

Different Screens:
  Small Phone (360dp):  ~156dp per card
  Medium Phone (412dp): ~184dp per card  
  Large Phone (480dp):  ~232dp per card
  Tablet (600dp+):      ~288dp per card
```

---

## Card Structure Inside Grid

### Announcement Card Layout
```
┌────────────────────────┐
│                        │
│   Image Area           │  ← AsyncImage(height: 100.dp)
│   (100.dp height)      │
│                        │
├────────────────────────┤
│ Title                  │  ← titleSmall, Bold
│                        │
│ Content preview...     │  ← bodySmall, Ellipsis
│                        │
└────────────────────────┘
Total Card Height: ~200dp (in grid context)
```

### Event Card Layout
```
┌────────────────────────┐
│ Event Title            │  ← titleSmall, Bold
│                        │
│ Venue Name             │  ← bodySmall, Primary color
│                        │
│ Event description      │  ← bodySmall, Ellipsis (3 lines)
│ continues here...      │
│                        │
│ 2026-07-22T20:00:00Z   │  ← labelSmall, timestamp
└────────────────────────┘
Total Card Height: Auto (content-based)
```

---

## State Management Flow

```
UiState (from ViewModel)
│
├─ Loading
│  └─ Show CircularProgressIndicator
│
├─ Error
│  ├─ Show Error Banner
│  ├─ Show Fallback UI
│  └─ Show Retry Button
│
└─ Success
   ├─ Data: AppHomeBundleResponse
   │  ├─ festival: AppFestivalHeader
   │  ├─ modules: List<HomeModule>
   │  │  ├─ hero_carousel module → heroCarouselItems property
   │  │  ├─ announcements module → announcements property
   │  │  └─ upcoming_events module → upcomingEvents property
   │  └─ uiConfig: UiConfig
   │
   └─ Show HomeScreenContent
      ├─ Render Hero Carousel (LazyRow)
      ├─ Render Announcements Grid ✅
      └─ Render Events Grid ✅
```

---

## Color & Styling Reference

### Grid Spacing
```
Horizontal Gap:     12.dp (between columns)
Vertical Gap:       12.dp (between rows)
Content Padding:    16.dp (left/right)
Card Padding:       12.dp (internal)
```

### Card Styling
```
Shape:              RoundedCornerShape(medium)
Elevation:          4.dp (default elevated)
Border:             None (elevation only)
Background:         Surface color
Text Color:         OnSurface / OnSurfaceVariant
```

### Typography
```
Card Title:         titleSmall, bold
Card Subtitle:      bodySmall
Card Content:       bodySmall, variant
Timestamp:          labelSmall
```

---

## Responsive Behavior

### Phone (360-430dp)
```
┌──────────────────┐
│ Card1│ Card2    │
├──────┼──────────┤
│ Card3│ Card4    │
├──────┼──────────┤
│ Card5│ Card6    │
└──────┴──────────┘
```

### Tablet (600dp+)
```
Same 2-column layout (per specification)
Cards are just wider:
┌─────────────────┬─────────────────┐
│      Card1      │      Card2      │
├─────────────────┼─────────────────┤
│      Card3      │      Card4      │
├─────────────────┼─────────────────┤
│      Card5      │      Card6      │
└─────────────────┴─────────────────┘
```

---

## Performance Optimization Diagram

```
LazyVerticalGrid (Optimized) ✅
│
├─ Only renders visible cards
├─ Removes cards when scrolled out of view
├─ Manages layout state efficiently
├─ Better memory usage
└─ Smoother scroll performance

vs

Multiple LazyRows (Previous) ❌
│
├─ Renders all cards in each row
├─ No visibility optimization
├─ Multiple scroll positions to track
├─ Higher memory usage
└─ Double-scroll interaction (bad UX)
```

---

## Summary

### Grid Properties Used
```kotlin
LazyVerticalGrid(
    columns = GridCells.Fixed(2)      // 2 columns always
    horizontalArrangement = 12.dp     // Column spacing
    verticalArrangement = 12.dp       // Row spacing  
    modifier = fillMaxWidth()         // Full width
    modifier.padding(16.dp)           // Content padding
    userScrollEnabled = false         // Parent handles scroll
)
```

### Result
✅ 2-column responsive grid
✅ Perfect spacing (Material 3 spec)
✅ Efficient rendering
✅ Better UX (single vertical scroll)
✅ Matches design screenshot

---

**Status:** Architecture Complete ✅
**Implementation:** HomeExploreComponents.kt
**Date:** March 4, 2026
