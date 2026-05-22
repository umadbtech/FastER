package com.faster.festival.wristband.di

import android.content.Context
import com.faster.festival.BuildConfig
import com.faster.festival.di.DatabaseModule
import com.faster.festival.wristband.data.ble.BleMeshGatt
import com.faster.festival.wristband.data.ble.FakeMeshManager
import com.faster.festival.wristband.data.ble.MeshLogger
import com.faster.festival.wristband.data.ble.MeshNetworkStore
import com.faster.festival.wristband.data.ble.NordicMeshManager
import com.faster.festival.wristband.data.ble.WristbandMeshManager
import com.faster.festival.wristband.data.repository.WristbandMeshRepositoryImpl
import com.faster.festival.wristband.domain.repository.WristbandMeshRepository
import com.faster.festival.wristband.domain.usecase.ObserveConnectionUseCase
import com.faster.festival.wristband.domain.usecase.ObserveDeviceEventsUseCase
import com.faster.festival.wristband.domain.usecase.ObserveSosEventsUseCase
import com.faster.festival.wristband.domain.usecase.ObserveTelemetryUseCase
import com.faster.festival.wristband.domain.usecase.ProvisionWristbandUseCase
import com.faster.festival.wristband.domain.usecase.ReconnectWristbandUseCase
import com.faster.festival.wristband.domain.usecase.SendRemoteConfigUseCase
import com.faster.festival.wristband.domain.usecase.SendSosAckUseCase
import com.faster.festival.wristband.domain.usecase.SendSosResolvedUseCase
import com.faster.festival.wristband.domain.usecase.SendSosResponderUseCase
import com.faster.festival.wristband.domain.usecase.UnpairWristbandUseCase

/**
 * Manual-DI module for the wristband stack. Mirrors the existing
 * [DatabaseModule] / `NetworkModule` / `PinchModule` pattern.
 *
 * Production operates on REAL BLE Mesh only ([NordicMeshManager]).
 * [FakeMeshManager] is retained strictly as engineering / preview tooling and
 * can ONLY be selected in a debug build (see [meshManager] guard) — release
 * builds are hard-wired to the real implementation regardless of any flag.
 */
object WristbandModule {

    private var appContext: Context? = null

    /**
     * Engineering-only override to run the simulated mesh stack
     * ([FakeMeshManager]) for previews / instrumented tests.
     *
     * Default `false` — the app pairs against real hardware out of the box.
     * Setting this `true` has NO effect in a release build: the [meshManager]
     * getter ignores it unless [BuildConfig.DEBUG] is set, so production users
     * can never reach the fake pairing flow.
     *
     * To use in a debug build, set this BEFORE the first [meshManager] access
     * (the value is read once, lazily).
     */
    @Volatile var useFakeMesh: Boolean = false

    fun initialize(context: Context) {
        appContext = context.applicationContext
    }

    private val ctx: Context
        get() = requireNotNull(appContext) {
            "WristbandModule.initialize(context) must be called first"
        }

    private val logger = MeshLogger()
    private val store: MeshNetworkStore by lazy { MeshNetworkStore(ctx) }

    val meshManager: WristbandMeshManager by lazy {
        // Hard guard: the fake manager is reachable ONLY in a debug build AND
        // only when an engineer explicitly opted in. Release builds always get
        // the real Nordic-backed implementation, no matter what useFakeMesh is.
        if (BuildConfig.DEBUG && useFakeMesh) {
            logger.info("WristbandModule: DEBUG fake mesh enabled (engineering override)")
            FakeMeshManager()
        } else {
            // Real Nordic nRF Mesh-backed implementation. Requires Bluetooth
            // permissions to be granted and BLE radio to be on at the time
            // the first scan / connect runs (NordicMeshManager.provision is
            // tolerant — failures land on WristbandError).
            NordicMeshManager(
                context = ctx,
                gatt = BleMeshGatt(ctx),
                store = store,
                logger = logger
            )
        }
    }

    val repository: WristbandMeshRepository by lazy {
        WristbandMeshRepositoryImpl(meshManager, store, DatabaseModule.wristbandRepository)
    }

    // ─── Use case accessors ─────────────────────────────────────────────────
    val provision get() = ProvisionWristbandUseCase(repository)
    val reconnect get() = ReconnectWristbandUseCase(repository)
    val observeTelemetry get() = ObserveTelemetryUseCase(repository)
    val observeSos get() = ObserveSosEventsUseCase(repository)
    val observeDeviceEvents get() = ObserveDeviceEventsUseCase(repository)
    val observeConnection get() = ObserveConnectionUseCase(repository)
    val sendAck get() = SendSosAckUseCase(repository)
    val sendResponder get() = SendSosResponderUseCase(repository)
    val sendResolved get() = SendSosResolvedUseCase(repository)
    val sendConfig get() = SendRemoteConfigUseCase(repository)
    val unpair get() = UnpairWristbandUseCase(repository)
}
