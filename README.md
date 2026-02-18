# FastER Festival - Android Jetpack Compose App

A modern Android festival discovery app built with **Jetpack Compose** and **Material Design 3**.

## Features

- **Home Screen**: Hero header, quick actions, artist lineup, festival info
- **Map**: Interactive festival map with POI markers, draggable shortcuts sheet
- **Schedule**: Full festival schedule with filtering
- **Profile**: User account management with personal details
- **Artist Details**: Full-screen artist bios, festival sets, booking info
- **Authentication**: Multi-step email verification flow
- **Tickets**: Ticket browsing and purchase interface
- **Material 3 Design**: Dynamic colors, dark mode support, premium UI/UX
- **Responsive Layout**: Adapts to various screen sizes
- **Accessibility**: Content descriptions, proper touch targets, color contrast

## Tech Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose with Material 3
- **Navigation**: Navigation Compose
- **State Management**: ViewModels + Flow
- **Architecture**: MVVM + Repository Pattern
- **Min SDK**: 24
- **Target SDK**: 34

## Project Structure

```
app/
├── src/main/java/com/faster/festival/
│   ├── MainActivity.kt                 # App entry point
│   ├── data/
│   │   ├── models/                     # Data classes
│   │   └── repository/                 # Fake in-memory repository
│   ├── ui/
│   │   ├── components/                 # Reusable Compose components
│   │   ├── navigation/                 # NavGraph & routing
│   │   ├── screens/                    # Main app screens
│   │   ├── theme/                      # Material 3 theme, colors, typography
│   │   └── viewmodel/                  # MVVM ViewModels
│   └── res/
│       ├── values/strings.xml          # String resources
│       └── values/themes.xml           # Android theme
├── build.gradle.kts                    # App-level configuration
└── AndroidManifest.xml                 # App manifest

gradle/
└── libs.versions.toml                  # Dependency version catalog

build.gradle.kts                        # Project-level configuration
settings.gradle.kts                     # Gradle settings
```

## Screens

1. **Login** - Email/phone/Google auth entry point
2. **Verify Email** - Email verification screen
3. **Code Entry** - 6-digit code verification
4. **Home** - Hero festival header, quick actions, artist lineup, experience list
5. **Map** - Festival map with POI markers, shortcuts sheet
6. **Schedule** - Full festival event schedule
7. **Profile** - User account and personal details management
8. **Artist Detail** - Full artist bio, festival sets, actions
9. **Tickets** - Festival ticket browsing and pricing
10. **Web Placeholder** - Web content placeholders (Festival Home, FAQs)

## Components

- `FestivalHeroHeader` - Large festival branding header with gradient
- `QuickActionRow` - 4-item quick action circular buttons
- `SetupAccountCard` - Account setup call-to-action card
- `HeadlinerRowOrGrid` - Horizontal scrolling artist cards
- `ExperienceList` - Vertical list of festival info links
- `TicketsFabPill` - Floating action button for tickets (capsule shaped)
- `MapMarker` - Interactive POI marker with selection state
- `DraggableShortcutsSheet` - Bottom sheet with shortcuts chips
- `ProfileHeader` - User profile header with avatar and actions
- `ProfileCardSection` - Grouped profile information cards
- `AuthButtons` - Multi-option authentication buttons
- `ScheduleItemCard` - Schedule event card with timing
- `TicketTypeCard` - Ticket pricing and features card

## Data Models

- `Festival` - Festival info (name, location, date)
- `Artist` - Artist data with bio and sets
- `FestivalSet` - Artist performance details (stage, time)
- `Poi` - Points of interest (locations, types)
- `ScheduleItem` - Festival schedule event
- `AccountProfile` - User account information
- `QuickAction` - Home screen quick action data

## State Management

- **UiState** sealed class: `Loading`, `Success<T>`, `Error`
- **ViewModels**: HomeVM, MapVM, ScheduleVM, ProfileVM, ArtistDetailVM, AuthVM
- **State hoisting**: UI state in composables, domain logic in ViewModels
- **Flow-based**: Repository returns Flow for reactive updates

## Getting Started

### Prerequisites
- Android Studio (latest)
- Android SDK 34+
- Kotlin 1.9+

### Setup

1. Clone the repository:
```bash
git clone <repo-url>
cd FastER
```

2. Open in Android Studio and sync Gradle

3. Build and run:
```bash
./gradlew assembleDebug
```

### Gradle Sync

The project uses version catalog (`gradle/libs.versions.toml`) for dependency management:
```bash
./gradlew --refresh-dependencies
```

## Customization

### Change Theme Colors
Edit `ui/theme/Color.kt`:
```kotlin
val md_theme_light_primary = Color(0xFF7C3AED) // Purple
```

### Add New Screen
1. Create `YourScreen.kt` in `ui/screens/`
2. Add route to `NavGraph.kt`
3. Add navigation in `MainActivity.kt`

### Update Festival Data
Edit `data/repository/FakeFestivalRepository.kt` to modify sample data (artists, POIs, schedule).

## Material 3 Features

- ✅ Dynamic color support (device wallpaper-based on Android 12+)
- ✅ Dark mode support
- ✅ 8dp grid system
- ✅ Rounded shapes (16-28dp)
- ✅ Elevated cards with subtle shadows
- ✅ Modern typography (Font scales)
- ✅ Touch-friendly component sizes (48dp min)
- ✅ WCAG color contrast compliance

## Future Enhancements

- [ ] Real maps integration (Google Maps)
- [ ] Image loading library (Coil/Glide)
- [ ] Network API integration
- [ ] Database (Room) for offline support
- [ ] Push notifications
- [ ] Social sharing
- [ ] Favorites/bookmarking
- [ ] Real authentication backend
- [ ] Analytics integration
- [ ] Unit & UI tests

## Contributing

Pull requests welcome! Please follow the existing code style and architecture patterns.

## License

Apache License 2.0

## Support

For issues or questions, please open an issue in the repository.

---

**Built with ❤️ using Jetpack Compose & Material 3**
