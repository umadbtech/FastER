package com.faster.festival.wristband.data.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.content.Context
import android.util.Log
import androidx.annotation.MainThread
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import no.nordicsemi.android.ble.BleManager
import no.nordicsemi.android.ble.data.Data
import no.nordicsemi.android.ble.observer.ConnectionObserver
import java.util.UUID

/**
 * Kotlin-native wrapper around Nordic's [BleManager] (`no.nordicsemi.android:ble`).
 * Modeled on the `BleMeshManager` from Nordic's nRF Mesh sample app, but
 * exposes only the four operations the rest of the FastER stack needs and
 * emits raw GATT notifications via [meshNotifications].
 *
 * BT SIG Mesh GATT layout used (see [MeshConstants]):
 *  • Mesh Provisioning  (0x1827)  Data In 0x2ADB  /  Data Out 0x2ADC
 *  • Mesh Proxy         (0x1828)  Data In 0x2ADD  /  Data Out 0x2ADE
 *
 * Lifecycle: one [BleMeshGatt] per app process, owned by
 * [com.faster.festival.wristband.di.WristbandModule]. Safe to re-use across
 * [connect]/[disconnect] cycles.
 */
@SuppressLint("MissingPermission")
class BleMeshGatt(context: Context) {

    private val manager = InnerManager(context.applicationContext)
    private val _notifications = MutableSharedFlow<ByteArray>(extraBufferCapacity = 64)
    val meshNotifications: SharedFlow<ByteArray> = _notifications.asSharedFlow()

    private var connectAck: CompletableDeferred<Unit>? = null

    init {
        manager.setConnectionObserver(object : ConnectionObserver {
            override fun onDeviceConnecting(device: BluetoothDevice) {}
            override fun onDeviceConnected(device: BluetoothDevice) {}
            override fun onDeviceFailedToConnect(device: BluetoothDevice, reason: Int) {
                connectAck?.completeExceptionally(
                    IllegalStateException("connect failed (reason=$reason)")
                )
            }
            override fun onDeviceReady(device: BluetoothDevice) {
                connectAck?.complete(Unit)
            }
            override fun onDeviceDisconnecting(device: BluetoothDevice) {}
            override fun onDeviceDisconnected(device: BluetoothDevice, reason: Int) {
                if (connectAck?.isCompleted == false) {
                    connectAck?.completeExceptionally(
                        IllegalStateException("disconnected before ready (reason=$reason)")
                    )
                }
            }
        })
    }

    /**
     * Connects to [device] and suspends until services are discovered, MTU is
     * negotiated, and notifications are enabled. The returned [CompletableDeferred]
     * completes successfully once the manager is ready, or exceptionally on
     * failure / disconnect-before-ready.
     */
    @MainThread
    fun connect(device: BluetoothDevice): CompletableDeferred<Unit> {
        val deferred = CompletableDeferred<Unit>()
        connectAck = deferred
        manager.connect(device)
            .useAutoConnect(false)
            .timeout(15_000)
            .retry(2, 200)
            .enqueue()
        return deferred
    }

    fun disconnect() {
        manager.disconnect().enqueue()
    }

    /**
     * Writes a provisioning PDU. Used during the initial provision phase, when
     * only the Mesh Provisioning service is exposed.
     */
    fun writeProvisioningPdu(bytes: ByteArray) {
        manager.writeProvisioning(bytes)
    }

    /**
     * Writes a mesh PDU over the proxy service. Used after provisioning, when
     * the node advertises with network or node identity.
     */
    fun writeMeshPdu(bytes: ByteArray) {
        manager.writeProxy(bytes)
    }

    /** Negotiated MTU. Defaults to 23 (BLE minimum) until [InnerManager.initialize] runs. */
    fun mtu(): Int = manager.currentMtu

    // ─── Inner Nordic BleManager subclass ──────────────────────────────────

    private inner class InnerManager(ctx: Context) : BleManager(ctx) {

        private val provisioningService = UUID.fromString(MeshConstants.MESH_PROVISIONING_SERVICE)
        private val proxyService = UUID.fromString(MeshConstants.MESH_PROXY_SERVICE)
        private val provisioningDataIn = UUID.fromString("00002ADB-0000-1000-8000-00805F9B34FB")
        private val provisioningDataOut = UUID.fromString("00002ADC-0000-1000-8000-00805F9B34FB")
        private val proxyDataIn = UUID.fromString("00002ADD-0000-1000-8000-00805F9B34FB")
        private val proxyDataOut = UUID.fromString("00002ADE-0000-1000-8000-00805F9B34FB")

        private var provIn: BluetoothGattCharacteristic? = null
        private var provOut: BluetoothGattCharacteristic? = null
        private var proxyIn: BluetoothGattCharacteristic? = null
        private var proxyOut: BluetoothGattCharacteristic? = null

        var currentMtu: Int = 23
            private set

        fun writeProvisioning(bytes: ByteArray) {
            val ch = provIn ?: return
            writeCharacteristic(
                ch, bytes, BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
            ).enqueue()
        }

        fun writeProxy(bytes: ByteArray) {
            val ch = proxyIn ?: return
            writeCharacteristic(
                ch, bytes, BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
            ).enqueue()
        }

        override fun getGattCallback(): BleManagerGattCallback = object : BleManagerGattCallback() {

            override fun isRequiredServiceSupported(gatt: BluetoothGatt): Boolean {
                val provisioning: BluetoothGattService? = gatt.getService(provisioningService)
                val proxy: BluetoothGattService? = gatt.getService(proxyService)
                provIn = provisioning?.getCharacteristic(provisioningDataIn)
                provOut = provisioning?.getCharacteristic(provisioningDataOut)
                proxyIn = proxy?.getCharacteristic(proxyDataIn)
                proxyOut = proxy?.getCharacteristic(proxyDataOut)
                // Pre-pair only the provisioning service is exposed; post-pair
                // only the proxy service is exposed. Either is sufficient.
                return (provIn != null && provOut != null) ||
                        (proxyIn != null && proxyOut != null)
            }

            override fun initialize() {
                requestMtu(69).with { _, mtu -> currentMtu = mtu }.enqueue()

                provOut?.let { ch ->
                    setNotificationCallback(ch).with { _, data: Data ->
                        data.value?.let { _notifications.tryEmit(it) }
                    }
                    enableNotifications(ch).enqueue()
                }
                proxyOut?.let { ch ->
                    setNotificationCallback(ch).with { _, data: Data ->
                        data.value?.let { _notifications.tryEmit(it) }
                    }
                    enableNotifications(ch).enqueue()
                }
            }

            override fun onServicesInvalidated() {
                provIn = null; provOut = null; proxyIn = null; proxyOut = null
            }
        }

        override fun log(priority: Int, message: String) {
            Log.println(priority, "BleMeshGatt", message)
        }

        override fun getMinLogPriority(): Int = Log.VERBOSE
    }
}
