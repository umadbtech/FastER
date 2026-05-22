# FastER — Festival App + Wristband Safety Platform (Android)

FastER is an Android app that combines a festival discovery experience with a
**BLE Mesh wristband safety platform**. Attendees pair a FastER wristband over
Bluetooth Mesh; the wristband streams telemetry and can trigger a real
emergency (SOS) that is dispatched to a responder console through a signed,
trusted-device backend.

Built with Jetpack Compose, Kotlin Coroutines/Flow, and a manual-DI clean
architecture.

---

## What the app does

**Festival experience**
- Home / lineup / map / schedule / artist details, sponsors & promotions
- Email + OTP authentication with JWT session handling and automatic refresh
- Push notifications (FCM) with topic routing
- Server-driven content via Supabase Edge Functions

**Wristband safety platform**
- BLE Mesh provisioning (9-step pairing of a FastER wristband)
- 1 Hz telemetry streaming (accelerometer, motion, battery, device state)
- Wristband-triggered **SOS emergency** (BLE `0x11`) → signed dispatch
- In-app "Get Medical Help" (Pinch) emergency flow
- Foreground-service-backed live location updates during an active SOS
- Offline-safe telemetry upload + durable SOS retry that survive process death

---

## Architecture at a glance

```
UI (Compose screens + ViewModels)
        │  StateFlow / collectAsStateWithLifecycle
Domain (use cases, repositories ports)
        │
Data
 ├── remote   Retrofit services (2 Supabase projects), interceptors, typed errors
 ├── local    Room (v4), DataStore, EncryptedSharedPreferences
 ├── sos       trusted-device signing + dispatch
 └── wristband BLE Mesh manager, vendor-model codec
Core
 ├── crypto    Ed25519 signing, canonical JSON, SHA-256
 ├── security  key manager (EncryptedSharedPreferences)
 ├── sos        EmergencySOSManager, watchers, notifier, foreground service
 └── telemetry  queue collector + WorkManager upload worker
```

- **Pattern:** MVVM + repository, manual DI via `object` modules (no Hilt/Dagger).
- **Concurrency:** Kotlin Coroutines + Flow throughout; app-scoped supervisors
  for long-lived emergency / telemetry orchestration.
- **Persistence:** Room for durable queues + audit, DataStore for hot-path
  state, EncryptedSharedPreferences for tokens and signing keys.

---

## Backend — two Supabase projects

The app talks to **two** Supabase Edge Function projects with a single shared
user JWT and separate anon keys.

| Project | Purpose | Base URL (`.env`) | Anon key |
|---|---|---|---|
| **Project 1** | wristband pairing, active lookup, unpair, telemetry ingestion, SOS history/audit, device registry, attestation, festival/auth/content | `VITE_SUPABASE_URL` | `VITE_SUPABASE_ANON_KEY` |
| **Project 2** | **real emergency dispatch** — `pinch-ingest`, alert status, location updates, AWS incident flow | `PROJECT2_SOS_URL` | `PROJECT2_SOS_ANON_KEY` |

> Important: a Project 1 wristband SOS history record is **audit only**. The
> Project 2 `pinch-ingest` call is the real emergency dispatch.

- One Retrofit/OkHttp client per project (no duplicates). Shared interceptor
  chain: logging (redacted) → anon `apikey` → bearer JWT → 401 refresh.
- Typed errors (`ApiError`) cover 400 / 401 / 403 / 404 / 409 / 413 / 422 /
  5xx / network; every repository call returns `Result<T>`.
- Mutating POSTs carry an `Idempotency-Key`.

---

## BLE Mesh vendor model

FastER wristband — Company ID `0x0030`, Model ID `0x0001`.

| Direction | Sub-cmd | Meaning |
|---|---|---|
| Inbound  | `0x10` | telemetry (1 Hz) |
| Inbound  | `0x11` | SOS emergency |
| Inbound  | `0x12` | SOS cancel |
| Inbound  | `0x13` | device status |
| Inbound  | `0x14` | device event |
| Outbound | `0x20` | SOS ACK |
| Outbound | `0x21` | responder dispatched |
| Outbound | `0x22` | SOS resolved |
| Outbound | `0x23` | NFC result |

- Parsing: `VendorMessageParser` (bounds-checked little-endian, malformed
  packets degrade safely to a typed `MalformedMessage`).
- Encoding: `VendorMessageEncoder`.
- Stack: Nordic nRF Mesh (`no.nordicsemi.android:mesh:3.4.0`) +
  `no.nordicsemi.android:ble:2.7.5`.
- Debug builds default to a `FakeMeshManager` (no hardware needed); release
  builds use the Nordic-backed `NordicMeshManager`.

---

## SOS dispatch flow (wristband-triggered)

1. Deduplicate by `event_id`
2. Generate `client_trigger_id` (reused only across retries)
3. Gather GPS + device context
4. Sign request (Ed25519, canonical JSON)
5. POST Project 2 `pinch-ingest`
6. Optionally record Project 1 SOS audit
7. Send BLE `0x20` ACK to the wristband
8. Update UI state + post notifications
9. Persist session (survives process death)

- Inline retry (3 attempts on transport/5xx) → durable WorkManager retry
  (`SosDispatchRetryWorker`) if the inline budget is exhausted.
- Process-death recovery branches on whether dispatch was acknowledged
  (`alertId`): re-dispatch if pending, resume polling if confirmed.
- Auto re-attestation: a `403 attestation expired` from Project 2 triggers a
  transparent `sos-verify-attestation` refresh + one retry.

---

## Telemetry pipeline (BLE `0x10` → Project 1 only)

```
BLE 0x10 → TelemetryCollector → Room queue (dedup by wristband_id+seq)
        → TelemetryUploadWorker (WorkManager) → Project 1 batch endpoint
```

- Batches of ≤ 200 readings/request.
- Offline-safe (Room-backed), deduplicated, FIFO-drained, 10k-row soft cap.
- Exponential backoff, network-constrained, survives reboot.
- Telemetry **never** goes to Project 2 (enforced by construction).

---

## Security / request signing

- **Key:** Ed25519, generated on-device, stored in
  EncryptedSharedPreferences (Keystore-wrapped AES-256-GCM). BouncyCastle is
  used because Android Keystore Ed25519 needs API 33 and `minSdk = 24`.
- **Headers on every Project 2 request:** `x-device-signature`,
  `x-device-signature-alg`, `x-device-body-sha256`.
- **Invariant:** signing happens AFTER final JSON serialization. The
  `DeviceSignatureManager` facade enforces `serialize → SHA-256 → sign` so it
  cannot be done out of order.
- Unit-tested end-to-end against a real Ed25519 keypair (signature verified
  against the canonical string).

---

## Tech stack

- **Language:** Kotlin 2.0.21
- **UI:** Jetpack Compose + Material 3
- **Async:** Coroutines + Flow
- **Networking:** Retrofit 2 + OkHttp + kotlinx.serialization
- **Background:** WorkManager, foreground services
- **Persistence:** Room 2.6, DataStore Preferences, EncryptedSharedPreferences
- **BLE:** Nordic nRF Mesh 3.4.0 + Nordic BLE 2.7.5
- **Crypto:** BouncyCastle (`bcprov-jdk18on:1.78.1`)
- **Maps/Location:** Google Maps Compose + Play Services Location
- **Push:** Firebase Cloud Messaging
- **Logging:** Timber
- **Min SDK:** 24 · **Target/Compile SDK:** 36

---

## Project structure

```
app/src/main/java/com/faster/festival/
├── FASTERApplication.kt          # DI bootstrap + orchestration startup
├── MainActivity.kt
├── core/
│   ├── crypto/                   # CanonicalJson, DeviceSignatureManager, hashing, nonce
│   ├── security/                 # Ed25519KeyManager
│   ├── sos/                      # EmergencySOSManager, watchers, notifier, FG service, retry worker
│   └── telemetry/                # TelemetryCollector, TelemetryUploadWorker
├── data/
│   ├── remote/                   # Project1/Project2 services, interceptors, ApiError
│   ├── local/                    # EncryptedSessionManager, Room db (entities, DAOs, migrations)
│   ├── repository/               # auth, profile, content, wristband, pinch-ingest repos
│   ├── sos/                      # SosRepositoryImpl, signing pipeline, device registration
│   └── pinch/                    # in-app emergency (Pinch) flow
├── domain/sos/                   # use cases, repository ports
├── di/                           # NetworkModule, SosModule, DatabaseModule, TelemetryModule, ...
├── notifications/                # FCM service, channels, token registrar
├── ui/                           # Compose screens, viewmodels, navigation, theme
└── wristband/                    # BLE Mesh module (data/ble, domain, ui, di)

gradle/libs.versions.toml         # version catalog
Mobile-Vendor-Model-Dev.md        # firmware vendor-model spec
Wristband-Backend-API.md          # backend API spec
Pinch_SOS_Frontend_Implementation_Guide.md
```

---

## Getting started

### Prerequisites
- Android Studio (latest stable)
- Android SDK 36
- JDK 17
- A physical device with BLE for real wristband testing (emulator works with the fake mesh manager)

### 1. Configure `.env`

Create a `.env` file in the project root:

```env
VITE_SUPABASE_URL=https://mihgyfijfnbhypiraoxg.supabase.co
VITE_SUPABASE_ANON_KEY=<project1-anon-key>
PROJECT2_SOS_URL=https://tlwaffkoleqljanpopvn.supabase.co
PROJECT2_SOS_ANON_KEY=<project2-anon-key>
GOOGLE_MAPS_API_KEY=<maps-api-key>

# Optional dev toggles
USE_TEST_LOCATION=false          # return a fixed staging coordinate for SOS
SOS_ALLOW_TEST_ATTESTATION=false # allow the test attestation provider
```

These are surfaced as `BuildConfig` fields by `app/build.gradle.kts`.

### 2. Firebase

Place your `google-services.json` in `app/`.

### 3. Build & run

```bash
./gradlew assembleDebug
# or install on a connected device
./gradlew installDebug
```

### Build flags

| Flag | Effect |
|---|---|
| Debug build | `WristbandModule.useFakeMesh = true` — simulated BLE, no hardware needed |
| Release build | Real `NordicMeshManager` (requires BLE permissions + radio) |
| `-PuseRealSupabase=true` | Opt into the real Supabase client libs (default uses Retrofit-only) |

---

## Testing

```bash
# All unit tests
./gradlew testDebugUnitTest

# Targeted (e.g. signing pipeline)
./gradlew testDebugUnitTest --tests "com.faster.festival.core.crypto.*"
```

Covered today: canonical JSON determinism, SHA-256 hashing, Ed25519 signing
(verified against canonical string), SOS deduplication, EmergencySOSManager
state transitions, token refresh, auth error mapping, several view models.

---

## Permissions

Declared in `AndroidManifest.xml`: Internet, location (fine/coarse), Bluetooth
(scan/connect/advertise, `neverForLocation`), POST_NOTIFICATIONS,
USE_FULL_SCREEN_INTENT, VIBRATE, and foreground service types
`location | connectedDevice` for the active-SOS service.

---

## Status & known gaps

This is an active codebase. Notable items tracked before production:

- Production device attestation still defaults to a **test provider** —
  Play Integrity must be wired for release.
- Backend endpoint naming (kebab-case Edge Functions vs. path-segment style)
  needs sign-off with the backend team.
- `fallbackToDestructiveMigration()` should be removed before shipping a
  release with non-trivial Room data.
- Active-wristband refresh on launch/resume is implemented in the repository
  but not yet wired into the launch routing.

---

## License

Apache License 2.0
