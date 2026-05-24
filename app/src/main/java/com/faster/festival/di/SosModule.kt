package com.faster.festival.di

import android.content.Context
import com.faster.festival.core.crypto.CanonicalSigner
import com.faster.festival.core.crypto.DeviceSignatureManager
import com.faster.festival.core.security.Ed25519KeyManager
import com.faster.festival.core.sos.ActiveSessionStore
import com.faster.festival.core.sos.ActiveSosServiceCoordinator
import com.faster.festival.core.sos.EmergencySOSManager
import com.faster.festival.core.sos.SOSDeduplicator
import com.faster.festival.core.sos.SosNotificationCoordinator
import com.faster.festival.core.sos.SosNotifier
import com.faster.festival.core.sos.WristbandAckManager
import com.faster.festival.core.sos.WristbandSosWatcher
import com.faster.festival.data.local.EncryptedSessionManager
import com.faster.festival.data.remote.Project2ApiService
import com.faster.festival.data.repository.PinchIngestRepository
import com.faster.festival.data.sos.AttestationProvider
import com.faster.festival.data.sos.DeviceRegistrationManager
import com.faster.festival.data.sos.SosLocationProvider
import com.faster.festival.data.sos.SosRemoteDataSource
import com.faster.festival.data.sos.SosRepositoryImpl
import com.faster.festival.data.sos.TestAttestationProvider
import com.faster.festival.data.sos.local.DeviceIdentityStore
import com.faster.festival.data.sos.remote.SosApiService
import com.faster.festival.data.sos.remote.SosNetworkClient
import com.faster.festival.domain.sos.CancelPinchSOSUseCase
import com.faster.festival.domain.sos.FetchSosHistoryUseCase
import com.faster.festival.domain.sos.PollSOSStatusUseCase
import com.faster.festival.domain.sos.RegisterDeviceUseCase
import com.faster.festival.domain.sos.SendLocationUpdateUseCase
import com.faster.festival.domain.sos.SosRepository
import com.faster.festival.domain.sos.SubmitPinchDetailsUseCase
import com.faster.festival.domain.sos.TriggerSOSUseCase
import com.faster.festival.domain.sos.VerifyAttestationUseCase
import com.faster.festival.presentation.sos.SOSSetupManager
import com.faster.festival.wristband.di.WristbandModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Manual DI module for the SOS trusted-device flow. Mirrors the pattern of
 * [DatabaseModule] / [NetworkModule] / `WristbandModule` etc.
 *
 * Initialized once from `FASTERApplication.onCreate` via [initialize]. Holds
 * its own session-manager reference because [SOSSetupManager] needs to read
 * the bearer token before each `account-profile` round-trip.
 */
object SosModule {

    private var appContext: Context? = null
    private var sessionManager: EncryptedSessionManager? = null

    fun initialize(context: Context, session: EncryptedSessionManager) {
        appContext = context.applicationContext
        sessionManager = session
    }

    private val ctx: Context
        get() = requireNotNull(appContext) {
            "SosModule.initialize(context, session) must be called first"
        }
    private val session: EncryptedSessionManager
        get() = requireNotNull(sessionManager) {
            "SosModule.initialize(context, session) must be called first"
        }

    // ─── primitives ────────────────────────────────────────────────────────

    val ed25519KeyManager: Ed25519KeyManager by lazy { Ed25519KeyManager(ctx) }
    val canonicalSigner: CanonicalSigner by lazy { CanonicalSigner(ed25519KeyManager) }
    val deviceSignatureManager: DeviceSignatureManager by lazy {
        DeviceSignatureManager(canonicalSigner)
    }
    val identityStore: DeviceIdentityStore by lazy { DeviceIdentityStore(ctx) }
    val locationProvider: SosLocationProvider by lazy { SosLocationProvider(ctx) }

    // Swap to `FuturePlayIntegrityProvider()` (and add the Play Integrity dep)
    // when migrating off staging.
    val attestationProvider: AttestationProvider by lazy { TestAttestationProvider() }

    // ─── network ───────────────────────────────────────────────────────────
    // Two Retrofit clients — one per Supabase project. Project 1 hosts the
    // device-registry / attestation endpoints; Project 2 hosts the signed SOS
    // ingest + status. Same Retrofit interface, different base URL + apikey.

    // Lazy authApiService accessor passed to TokenRefreshInterceptor inside
    // SosNetworkClient — without this, expired Project-1 JWTs cause every
    // subsequent pinch-ingest / pinch-alert-status to 401-loop.
    val project1SosApi: SosApiService by lazy {
        SosNetworkClient.createProject1(session) { NetworkModule.authApiService }
    }
    val project2SosApi: SosApiService by lazy {
        SosNetworkClient.createProject2(session) { NetworkModule.authApiService }
    }

    val sosRemote: SosRemoteDataSource by lazy {
        SosRemoteDataSource(
            project1Api = project1SosApi,
            project2Api = project2SosApi
        )
    }

    /**
     * Project 2 unsigned auxiliary surface — health probe etc. Built from the
     * SAME Retrofit instance the signed [SosApiService] uses (see
     * [SosNetworkClient.project2Retrofit]) so there's exactly one OkHttp /
     * interceptor chain for the entire Project 2 surface.
     */
    val project2ApiService: Project2ApiService by lazy {
        SosNetworkClient.project2Retrofit(session) { NetworkModule.authApiService }
            .create(Project2ApiService::class.java)
    }

    /**
     * Caller-facing facade for signed pinch-ingest dispatch. Delegates signing
     * + transport to [repository] (the SOS repo). [project2ApiService] is
     * passed in so the facade can also reach the unsigned health probe.
     */
    val pinchIngestRepository: PinchIngestRepository by lazy {
        PinchIngestRepository(
            sosRepository = repository,
            project2ApiService = project2ApiService
        )
    }

    // ─── data ──────────────────────────────────────────────────────────────

    val deviceRegistration: DeviceRegistrationManager by lazy {
        DeviceRegistrationManager(
            keyManager = ed25519KeyManager,
            identityStore = identityStore,
            deviceRepo = DatabaseModule.sosDeviceRepository,
            remote = sosRemote,
            attestationProvider = attestationProvider
        )
    }

    val repository: SosRepository by lazy {
        SosRepositoryImpl(
            identityStore = identityStore,
            remote = sosRemote,
            signatureManager = deviceSignatureManager,
            deviceRegistration = deviceRegistration
        )
    }

    // ─── use cases ─────────────────────────────────────────────────────────

    val registerDevice get() = RegisterDeviceUseCase(repository)
    val verifyAttestation get() = VerifyAttestationUseCase(repository)
    val triggerSos get() = TriggerSOSUseCase(repository)
    val pollStatus get() = PollSOSStatusUseCase(repository)
    val submitPinchDetails get() = SubmitPinchDetailsUseCase(repository)
    val cancelPinchSos get() = CancelPinchSOSUseCase(repository)
    val sendLocationUpdate get() = SendLocationUpdateUseCase(repository)
    val fetchSosHistory get() = FetchSosHistoryUseCase(repository)

    // ─── presentation ──────────────────────────────────────────────────────

    val setupManager: SOSSetupManager by lazy {
        SOSSetupManager(
            sessionManager = session,
            profileApi = NetworkModule.profileApiService,
            registerDevice = registerDevice
        )
    }

    // ─── Unified emergency orchestration (wristband + manual) ──────────────
    // EmergencySOSManager is the single source of truth for the active SOS.
    // SosAlertViewModel + SOSViewModel both observe its `state` flow and
    // delegate mutations to it. Wristband 0x11/0x12 packets are pumped into
    // it by [WristbandSosWatcher] running in [emergencyScope].

    private val emergencyScope: CoroutineScope by lazy {
        CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }

    val deduplicator: SOSDeduplicator by lazy { SOSDeduplicator() }

    val activeSessionStore: ActiveSessionStore by lazy { ActiveSessionStore(ctx) }

    val wristbandAck: WristbandAckManager by lazy {
        WristbandAckManager(WristbandModule.repository)
    }

    val emergencyManager: EmergencySOSManager by lazy {
        EmergencySOSManager(
            appContext = ctx,
            triggerSos = triggerSos,
            pollStatus = pollStatus,
            submitDetailsUseCase = submitPinchDetails,
            cancelPinch = cancelPinchSos,
            sendLocation = sendLocationUpdate,
            locationProvider = locationProvider,
            pairedWristbandRepo = DatabaseModule.wristbandRepository,
            wristbandAck = wristbandAck,
            sessionStore = activeSessionStore,
            deduplicator = deduplicator,
            scope = emergencyScope
        )
    }

    val wristbandSosWatcher: WristbandSosWatcher by lazy {
        WristbandSosWatcher(
            observeSos = WristbandModule.observeSos,
            manager = emergencyManager,
            scope = emergencyScope
        )
    }

    val sosNotifier: SosNotifier by lazy { SosNotifier(ctx) }

    val sosNotificationCoordinator: SosNotificationCoordinator by lazy {
        SosNotificationCoordinator(
            manager = emergencyManager,
            notifier = sosNotifier,
            scope = emergencyScope
        )
    }

    val activeSosServiceCoordinator: ActiveSosServiceCoordinator by lazy {
        ActiveSosServiceCoordinator(
            context = ctx,
            manager = emergencyManager,
            scope = emergencyScope
        )
    }

    /**
     * Convenience: start the long-lived wristband-SOS watcher, the
     * notification coordinator, the foreground-service coordinator, and
     * resume any persisted session. Call exactly once from
     * `FASTERApplication.onCreate` after both [WristbandModule] and this
     * module are initialized.
     */
    fun startEmergencyOrchestration() {
        wristbandSosWatcher.start()
        sosNotificationCoordinator.start()
        activeSosServiceCoordinator.start()
        // resumeIfPersisted last — its state emission has to flow through
        // the just-started coordinators (notification + FG service).
        emergencyManager.resumeIfPersisted()
    }
}
