package com.faster.festival.wristband.data.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.Context
import com.faster.festival.wristband.domain.model.ConnectionStatus
import com.faster.festival.wristband.domain.model.ProvisioningProgress
import com.faster.festival.wristband.domain.model.ProvisioningStep
import com.faster.festival.wristband.domain.model.WristbandError
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import no.nordicsemi.android.mesh.ApplicationKey
import no.nordicsemi.android.mesh.MeshManagerApi
import no.nordicsemi.android.mesh.MeshManagerCallbacks
import no.nordicsemi.android.mesh.MeshNetwork
import no.nordicsemi.android.mesh.MeshProvisioningStatusCallbacks
import no.nordicsemi.android.mesh.MeshStatusCallbacks
import no.nordicsemi.android.mesh.provisionerstates.ProvisioningState
import no.nordicsemi.android.mesh.provisionerstates.UnprovisionedMeshNode
import no.nordicsemi.android.mesh.transport.ConfigAppKeyAdd
import no.nordicsemi.android.mesh.transport.ConfigModelAppBind
import no.nordicsemi.android.mesh.transport.ConfigModelPublicationSet
import no.nordicsemi.android.mesh.transport.ConfigModelSubscriptionAdd
import no.nordicsemi.android.mesh.transport.ControlMessage
import no.nordicsemi.android.mesh.transport.MeshMessage
import no.nordicsemi.android.mesh.transport.ProvisionedMeshNode
import no.nordicsemi.android.mesh.transport.VendorModelMessageStatus
import no.nordicsemi.android.mesh.transport.VendorModelMessageUnacked
import no.nordicsemi.android.support.v18.scanner.BluetoothLeScannerCompat
import no.nordicsemi.android.support.v18.scanner.ScanCallback
import no.nordicsemi.android.support.v18.scanner.ScanFilter
import no.nordicsemi.android.support.v18.scanner.ScanResult
import no.nordicsemi.android.support.v18.scanner.ScanSettings
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Production [WristbandMeshManager] backed by Nordic's nRF Mesh + BLE libs.
 *
 * Drives the 9 provisioning steps end-to-end:
 *  1. Scan for unprovisioned beacon (Mesh Provisioning service 0x1827)
 *  2. Connect proxy GATT via [BleMeshGatt]
 *  3. Identify + provision via [MeshManagerApi]
 *  4. Unicast assigned by Nordic during provisioning
 *  5. AppKey distribution (ConfigAppKeyAdd)
 *  6. Bind AppKey to FastER vendor model (ConfigModelAppBind)
 *  7. Subscribe to group 0xC000 (ConfigModelSubscriptionAdd)
 *  8. Set publish target → group (ConfigModelPublicationSet)
 *  9. Verify telemetry within 3 s
 *
 * After provisioning, [VendorModelMessageStatus] PDUs are decoded by
 * [VendorPayloadCodec] and emitted on [inboundVendor] for the repository.
 */
@SuppressLint("MissingPermission")
class NordicMeshManager(
    context: Context,
    private val gatt: BleMeshGatt,
    private val store: MeshNetworkStore,
    private val logger: MeshLogger
) : WristbandMeshManager,
    MeshManagerCallbacks,
    MeshProvisioningStatusCallbacks,
    MeshStatusCallbacks {

    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mesh = MeshManagerApi(context.applicationContext).also {
        it.setMeshManagerCallbacks(this)
        it.setProvisioningStatusCallbacks(this)
        it.setMeshStatusCallbacks(this)
        it.loadMeshNetwork()
    }

    private val _connection = MutableStateFlow<ConnectionStatus>(ConnectionStatus.Idle)
    override val connection = _connection.asStateFlow()

    private val _inbound = MutableSharedFlow<VendorPayloadCodec.Inbound>(extraBufferCapacity = 64)
    override val inboundVendor = _inbound.asSharedFlow()

    /** Set when [onProvisioningStateChanged] reaches PROVISIONING_CAPABILITIES. */
    private var pendingIdentified: CompletableDeferred<UnprovisionedMeshNode>? = null

    /** Set when [onProvisioningCompleted] fires. */
    private var pendingProvisioned: CompletableDeferred<ProvisionedMeshNode>? = null

    /** Pump GATT notifications back into the mesh stack. One subscription per process. */
    private val notificationPump: Job = ioScope.launch {
        gatt.meshNotifications.collect { bytes ->
            mesh.handleNotifications(gatt.mtu(), bytes)
        }
    }

    // ─── public API ─────────────────────────────────────────────────────────

    override fun provision(timeoutMs: Long): Flow<ProvisioningProgress> = flow {
        val tracker = ProvisioningProgressTracker()
        emit(tracker.snapshot())

        // 1. Scan
        tracker.start(ProvisioningStep.Scanning); emit(tracker.snapshot())
        val device = withTimeoutOrNull(timeoutMs) { scanForUnprovisionedNode() }
        if (device == null) {
            tracker.fail(WristbandError.ScanTimeout)
            emit(tracker.snapshot()); return@flow
        }
        tracker.complete(ProvisioningStep.Scanning); emit(tracker.snapshot())

        // 2. Connect proxy GATT
        tracker.start(ProvisioningStep.Connecting); emit(tracker.snapshot())
        runCatching { gatt.connect(device).await() }.onFailure {
            tracker.fail(WristbandError.Generic(it.message ?: "connect"))
            emit(tracker.snapshot()); return@flow
        }
        tracker.complete(ProvisioningStep.Connecting); emit(tracker.snapshot())

        // 3. Identify + provision
        tracker.start(ProvisioningStep.Provisioning); emit(tracker.snapshot())
        val identified = identifyAndStartProvisioning()
        if (identified == null) {
            tracker.fail(WristbandError.ProvisioningFailed("identify timeout"))
            emit(tracker.snapshot()); return@flow
        }
        val provisioned = withTimeoutOrNull(20_000) { pendingProvisioned?.await() }
        if (provisioned == null) {
            tracker.fail(WristbandError.ProvisioningFailed("provision timeout"))
            emit(tracker.snapshot()); return@flow
        }
        tracker.complete(ProvisioningStep.Provisioning); emit(tracker.snapshot())

        // 4. Unicast — Nordic assigned this during provisioning.
        tracker.complete(ProvisioningStep.AssigningUnicast); emit(tracker.snapshot())

        // 5. AppKey distribution
        tracker.start(ProvisioningStep.DistributingKeys); emit(tracker.snapshot())
        val net = mesh.meshNetwork ?: run {
            tracker.fail(WristbandError.ProvisioningFailed("no mesh network"))
            emit(tracker.snapshot()); return@flow
        }
        val appKey = net.appKeys.firstOrNull()
            ?: net.createAppKey().also { net.addAppKey(it) }
        sendConfigAppKeyAdd(provisioned.unicastAddress, net, appKey)
        tracker.complete(ProvisioningStep.DistributingKeys); emit(tracker.snapshot())

        // 6. Bind AppKey to FastER vendor model
        tracker.start(ProvisioningStep.BindingAppKey); emit(tracker.snapshot())
        bindAppKey(provisioned, appKey).onFailure {
            tracker.fail(WristbandError.BindFailed(it.message ?: "bind"))
            emit(tracker.snapshot()); return@flow
        }
        tracker.complete(ProvisioningStep.BindingAppKey); emit(tracker.snapshot())

        // 7. Subscribe to group 0xC000
        tracker.start(ProvisioningStep.SubscribingGroup); emit(tracker.snapshot())
        subscribeGroup(provisioned, MeshConstants.GROUP_ADDRESS).onFailure {
            tracker.fail(WristbandError.SubscriptionFailed(it.message ?: "sub"))
            emit(tracker.snapshot()); return@flow
        }
        tracker.complete(ProvisioningStep.SubscribingGroup); emit(tracker.snapshot())

        // 8. Set publish target → group
        tracker.start(ProvisioningStep.SettingPublishTarget); emit(tracker.snapshot())
        setPublishTarget(provisioned, appKey, MeshConstants.GROUP_ADDRESS).onFailure {
            tracker.fail(WristbandError.PublishConfigFailed(it.message ?: "publish"))
            emit(tracker.snapshot()); return@flow
        }
        tracker.complete(ProvisioningStep.SettingPublishTarget); emit(tracker.snapshot())

        // 9. Verify — firmware publishes telemetry every 1 s, so 3 s budget.
        tracker.start(ProvisioningStep.Verifying); emit(tracker.snapshot())
        val telemetryHeard = withTimeoutOrNull(3_000) {
            inboundVendor.first { it is VendorPayloadCodec.Inbound.TelemetryMsg }
            true
        } ?: false
        if (!telemetryHeard) {
            tracker.fail(WristbandError.PublishConfigFailed("no telemetry within 3s"))
            emit(tracker.snapshot()); return@flow
        }
        tracker.complete(ProvisioningStep.Verifying)

        store.saveUnicast(provisioned.unicastAddress)
        store.saveNetwork(mesh.exportMeshNetwork() ?: "")
        store.touchLastSeen()
        _connection.value = ConnectionStatus.Connected
        tracker.finish()
        emit(tracker.snapshot())
    }.flowOn(Dispatchers.IO)

    override suspend fun reconnect(unicastAddress: Int): Result<Unit> = runCatching {
        _connection.value = ConnectionStatus.Reconnecting
        val device = withTimeoutOrNull(15_000) { scanForProvisionedNode() }
            ?: error("scan timeout")
        gatt.connect(device).await()
        store.touchLastSeen()
        _connection.value = ConnectionStatus.Connected
    }.onFailure { _connection.value = ConnectionStatus.Disconnected(it.message) }

    override suspend fun sendVendor(unicastAddress: Int, payload: ByteArray): Result<Unit> =
        runCatching {
            val net = mesh.meshNetwork ?: error("no mesh network")
            val appKey = net.appKeys.firstOrNull() ?: error("no appkey")
            val message = VendorModelMessageUnacked(
                appKey,
                MeshConstants.MODEL_ID,
                MeshConstants.COMPANY_ID,
                MeshConstants.VENDOR_OPCODE,
                payload
            )
            mesh.createMeshPdu(unicastAddress, message)
            logger.outbound(unicastAddress, payload)
        }

    override suspend fun unpair(unicastAddress: Int) {
        runCatching {
            val net = mesh.meshNetwork ?: return
            // ProvisionedBaseMeshNode subclass — find by unicast.
            val node = net.nodes.firstOrNull {
                (it as? ProvisionedMeshNode)?.unicastAddress == unicastAddress
            } as? ProvisionedMeshNode
            node?.let { net.deleteNode(it) }
            gatt.disconnect()
            store.clear()
            _connection.value = ConnectionStatus.Idle
        }
    }

    override suspend fun shutdown() {
        gatt.disconnect()
        notificationPump.cancel()
        ioScope.cancel()
        _connection.value = ConnectionStatus.Idle
    }

    // ─── MeshManagerCallbacks (Nordic params are @NonNull) ─────────────────

    override fun onNetworkLoaded(network: MeshNetwork) {
        logger.info("mesh network loaded: ${network.meshName}")
    }
    override fun onNetworkUpdated(network: MeshNetwork) {
        ioScope.launch { runCatching { store.saveNetwork(mesh.exportMeshNetwork() ?: "") } }
    }
    override fun onNetworkLoadFailed(error: String) { logger.error("network load: $error") }
    override fun onNetworkImported(network: MeshNetwork) {}
    override fun onNetworkImportFailed(error: String) { logger.error("import: $error") }

    override fun sendProvisioningPdu(node: UnprovisionedMeshNode, pdu: ByteArray) {
        gatt.writeProvisioningPdu(pdu)
    }
    override fun onMeshPduCreated(pdu: ByteArray) {
        gatt.writeMeshPdu(pdu)
    }
    override fun getMtu(): Int = gatt.mtu()

    // ─── MeshProvisioningStatusCallbacks (params are @NonNull) ─────────────

    override fun onProvisioningStateChanged(
        node: UnprovisionedMeshNode,
        state: ProvisioningState.States,
        data: ByteArray?
    ) {
        if (state == ProvisioningState.States.PROVISIONING_CAPABILITIES) {
            pendingIdentified?.complete(node)
        }
    }
    override fun onProvisioningFailed(
        node: UnprovisionedMeshNode,
        state: ProvisioningState.States,
        data: ByteArray?
    ) {
        val err = IllegalStateException("provisioning failed at $state")
        pendingIdentified?.completeExceptionally(err)
        pendingProvisioned?.completeExceptionally(err)
    }
    override fun onProvisioningCompleted(
        node: ProvisionedMeshNode,
        state: ProvisioningState.States,
        data: ByteArray?
    ) {
        pendingProvisioned?.complete(node)
    }

    // ─── MeshStatusCallbacks (ControlMessage / MeshMessage are @NonNull) ───

    override fun onTransactionFailed(dst: Int, hasIncompleteTimerExpired: Boolean) {}
    override fun onUnknownPduReceived(src: Int, accessPayload: ByteArray?) {}
    override fun onBlockAcknowledgementProcessed(dst: Int, message: ControlMessage) {}
    override fun onBlockAcknowledgementReceived(src: Int, message: ControlMessage) {}
    override fun onHeartbeatMessageReceived(src: Int, message: ControlMessage) {}
    override fun onMeshMessageProcessed(dst: Int, meshMessage: MeshMessage) {}
    override fun onMeshMessageReceived(src: Int, meshMessage: MeshMessage) {
        if (meshMessage is VendorModelMessageStatus) {
            val payload = meshMessage.accessPayload ?: return
            logger.inbound(src, payload)
            val parsed = VendorPayloadCodec.decode(payload)
            ioScope.launch {
                store.touchLastSeen()
                _inbound.emit(parsed)
            }
        }
    }
    override fun onMessageDecryptionFailed(meshLayer: String?, errorMessage: String?) {
        logger.error("decrypt fail: $meshLayer / $errorMessage")
    }

    // ─── internal helpers ───────────────────────────────────────────────────

    private suspend fun scanForUnprovisionedNode(): BluetoothDevice? = suspendCoroutine { cont ->
        val scanner = BluetoothLeScannerCompat.getScanner()
        val filters = listOf(
            ScanFilter.Builder().setServiceUuid(
                android.os.ParcelUuid(UUID.fromString(MeshConstants.MESH_PROVISIONING_SERVICE))
            ).build()
        )
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setReportDelay(0)
            .build()
        val cb = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                scanner.stopScan(this)
                cont.resume(result.device)
            }
        }
        scanner.startScan(filters, settings, cb)
    }

    private suspend fun scanForProvisionedNode(): BluetoothDevice? = suspendCoroutine { cont ->
        val scanner = BluetoothLeScannerCompat.getScanner()
        val filters = listOf(
            ScanFilter.Builder().setServiceUuid(
                android.os.ParcelUuid(UUID.fromString(MeshConstants.MESH_PROXY_SERVICE))
            ).build()
        )
        val cb = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val advBytes = result.scanRecord?.bytes ?: return
                if (mesh.isAdvertisingWithNetworkIdentity(advBytes) ||
                    mesh.isAdvertisedWithNodeIdentity(advBytes)
                ) {
                    scanner.stopScan(this)
                    cont.resume(result.device)
                }
            }
        }
        scanner.startScan(filters, ScanSettings.Builder().build(), cb)
    }

    /**
     * Sends `identifyNode` and waits for [onProvisioningStateChanged] to reach
     * PROVISIONING_CAPABILITIES. Then kicks off the actual provisioning.
     */
    private suspend fun identifyAndStartProvisioning(): UnprovisionedMeshNode? {
        pendingIdentified = CompletableDeferred()
        pendingProvisioned = CompletableDeferred()
        runCatching { mesh.identifyNode(UUID.randomUUID(), 5) }
            .onFailure { return null }
        val node = withTimeoutOrNull(15_000) { pendingIdentified?.await() } ?: return null
        runCatching { mesh.startProvisioning(node) }.onFailure { return null }
        return node
    }

    private fun sendConfigAppKeyAdd(unicast: Int, net: MeshNetwork, appKey: ApplicationKey) {
        val msg = ConfigAppKeyAdd(net.primaryNetworkKey, appKey)
        mesh.createMeshPdu(unicast, msg)
    }

    private fun bindAppKey(node: ProvisionedMeshNode, appKey: ApplicationKey): Result<Unit> =
        runCatching {
            val element = node.elements.values.first()
            val model = element.meshModels.values.firstOrNull {
                it.modelId == MeshConstants.MODEL_ID
            } ?: error("FastER vendor model not found on node")
            val msg = ConfigModelAppBind(element.elementAddress, model.modelId, appKey.keyIndex)
            mesh.createMeshPdu(node.unicastAddress, msg)
        }

    private fun subscribeGroup(node: ProvisionedMeshNode, groupAddress: Int): Result<Unit> =
        runCatching {
            val element = node.elements.values.first()
            val msg = ConfigModelSubscriptionAdd(
                element.elementAddress,
                groupAddress,
                MeshConstants.MODEL_ID
            )
            mesh.createMeshPdu(node.unicastAddress, msg)
        }

    /**
     * Standard 10-int + 1-bool ConfigModelPublicationSet constructor:
     *   (elementAddress, publishAddress, appKeyIndex,
     *    credentialFlag, publishTtl, publishPeriod,
     *    publishRetransmitCount, publishRetransmitIntervalSteps,
     *    companyIdentifier, modelIdentifier)
     */
    private fun setPublishTarget(
        node: ProvisionedMeshNode,
        appKey: ApplicationKey,
        group: Int
    ): Result<Unit> = runCatching {
        val element = node.elements.values.first()
        val msg = ConfigModelPublicationSet(
            element.elementAddress,        // elementAddress
            group,                          // publishAddress
            appKey.keyIndex,                // appKeyIndex
            false,                          // credentialFlag
            7,                              // publishTtl
            0,                              // publishPeriod
            1,                              // publishRetransmitCount
            50,                             // publishRetransmitIntervalSteps
            MeshConstants.COMPANY_ID,       // companyIdentifier
            MeshConstants.MODEL_ID          // modelIdentifier
        )
        mesh.createMeshPdu(node.unicastAddress, msg)
    }
}
