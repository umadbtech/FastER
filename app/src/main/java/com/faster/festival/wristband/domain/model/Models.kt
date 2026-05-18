package com.faster.festival.wristband.domain.model

// ─── Device state (byte 13 of telemetry, see vendor spec §7.1) ──────────────
enum class DeviceState(val raw: Int) {
    Boot(0), Wake(1), Provisionable(2), Provisioned(3),
    Operational(4), SosActive(5), Sleep(6), Unknown(-1);

    companion object {
        fun fromRaw(b: Int): DeviceState = values().firstOrNull { it.raw == b } ?: Unknown
    }
}

// ─── SOS state (byte 5 of 0x11, see vendor spec §7.2) ───────────────────────
enum class SosState(val raw: Int) {
    Idle(0), Counting(1), Active(2), Retry(3),
    Confirmed(4), Responder(5), Resolved(6),
    CancelWindow(7), Canceled(99), Unknown(-1);

    companion object {
        fun fromRaw(b: Int): SosState = values().firstOrNull { it.raw == b } ?: Unknown
    }
}

// ─── Telemetry packet (1 Hz from device) ────────────────────────────────────
data class Telemetry(
    val seqNum: Int,
    val accelX_g: Float,
    val accelY_g: Float,
    val accelZ_g: Float,
    val peakMag_g: Float,
    val motionDetected: Boolean,
    val batteryPct: Int,
    val deviceState: DeviceState,
    val receivedAtMs: Long
)

// ─── SOS event + cancel ─────────────────────────────────────────────────────
data class SosEvent(
    val eventId: Long,
    val state: SosState,
    val deviceUptimeMs: Long,
    val retryCount: Int,
    val batteryPct: Int,
    val receivedAtMs: Long
)

data class SosCancel(val eventId: Long, val reason: Int, val receivedAtMs: Long)

// ─── Device lifecycle event (0x14) ──────────────────────────────────────────
enum class DeviceEventCode(val raw: Int) {
    PairWindowOpen(0x01), PairConfirmed(0x02), Operational(0x03),
    Wake(0x04), Sleep(0x05), SosTrigger(0x06), SosCancel(0x07),
    Unknown(-1);

    companion object {
        fun fromRaw(b: Int): DeviceEventCode = values().firstOrNull { it.raw == b } ?: Unknown
    }
}

data class DeviceEvent(val code: DeviceEventCode, val receivedAtMs: Long)

// ─── Device status snapshot (0x13 — on-demand or post-boot) ─────────────────
//
// Spec wire format is marked STUB in firmware doc §45/73; treat fields as
// provisional. Mobile parser is the authoritative source until firmware
// locks the layout — see VendorMessageParser.parseStatus().
data class DeviceStatus(
    val seqNum: Int,
    val batteryPct: Int,
    val deviceState: DeviceState,
    val firmwareVersion: String,            // "<major>.<minor>.<patch>"
    val uptimeMs: Long,
    val flags: Int,                         // bitfield — see DeviceStatusFlag
    val receivedAtMs: Long
) {
    val nfcPresent: Boolean get() = (flags and DeviceStatusFlag.NFC_PRESENT) != 0
    val charging: Boolean get() = (flags and DeviceStatusFlag.CHARGING) != 0
}

object DeviceStatusFlag {
    const val NFC_PRESENT = 0x01
    const val CHARGING = 0x02
}

// ─── Provisioning step labels for the progress UI ───────────────────────────
enum class ProvisioningStep(val label: String) {
    Scanning("Searching for wristband"),
    Connecting("Connecting"),
    Provisioning("Pairing securely"),
    AssigningUnicast("Assigning device address"),
    DistributingKeys("Sharing security keys"),
    BindingAppKey("Authorizing FastER channel"),
    SubscribingGroup("Joining event group"),
    SettingPublishTarget("Enabling live telemetry"),
    Verifying("Verifying connection")
}

sealed class StepStatus {
    object Pending : StepStatus()
    object Running : StepStatus()
    object Success : StepStatus()
    data class Failed(val reason: String) : StepStatus()
}

data class ProvisioningProgress(
    val steps: Map<ProvisioningStep, StepStatus> =
        ProvisioningStep.values().associateWith { StepStatus.Pending },
    val currentStep: ProvisioningStep? = null,
    val terminalError: WristbandError? = null,
    val finished: Boolean = false
) {
    val isFailed: Boolean get() = terminalError != null
}

// ─── User-facing connectivity to the wristband (NOT phone internet) ─────────
sealed class ConnectionStatus {
    object Idle : ConnectionStatus()
    object Connecting : ConnectionStatus()
    object Connected : ConnectionStatus()
    object Reconnecting : ConnectionStatus()
    data class Disconnected(val reason: String?) : ConnectionStatus()
    object Stale : ConnectionStatus()
}

// ─── Failure surfaces ────────────────────────────────────────────────────────
sealed class WristbandError(val userMessage: String, val ctaText: String) {
    object BluetoothOff : WristbandError(
        "Bluetooth is off. Turn it on to pair your wristband.", "Turn On Bluetooth")
    object BluetoothPermissionDenied : WristbandError(
        "FastER needs Bluetooth permission to find your wristband.", "Grant Permissions")
    object NearbyDevicesPermissionDenied : WristbandError(
        "Allow Nearby Devices so we can connect to your wristband.", "Grant Permissions")
    object LocationPermissionDenied : WristbandError(
        "Location is required to scan for nearby wristbands.", "Grant Permissions")
    object ScanTimeout : WristbandError(
        "We couldn't find your wristband. Make sure it's powered on.", "Try Again")
    data class ProvisioningFailed(val detail: String) :
        WristbandError("Pairing failed: $detail", "Retry Pairing")
    data class BindFailed(val detail: String) :
        WristbandError("Could not authorize the FastER channel: $detail", "Retry Pairing")
    data class SubscriptionFailed(val detail: String) :
        WristbandError("Could not join the event group: $detail", "Retry Pairing")
    data class PublishConfigFailed(val detail: String) :
        WristbandError("Live telemetry could not start: $detail", "Retry Pairing")
    data class ReconnectFailed(val detail: String) :
        WristbandError("We couldn't reconnect to your wristband: $detail", "Reconnect")
    data class Generic(val detail: String) :
        WristbandError("Something went wrong: $detail", "Try Again")
}
