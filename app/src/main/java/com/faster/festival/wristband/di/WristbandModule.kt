package com.faster.festival.wristband.di

import android.content.Context
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
 * Phase A defaults to [FakeMeshManager] so the app is fully demoable
 * without the Nordic dependency. To enable real BLE Mesh later:
 *  1. Add `no.nordicsemi.android:mesh` + `:ble` to `build.gradle.kts`.
 *  2. Drop in `NordicMeshManager` + `BleMeshGatt` (per the spec doc).
 *  3. Set `useFakeMesh = false` and the lazy [meshManager] picks the real impl.
 */
object WristbandModule {

    private var appContext: Context? = null

    /** Single mock toggle. Default `true` so debug builds work out of the box. */
    @Volatile var useFakeMesh: Boolean = true

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
        if (useFakeMesh) {
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
