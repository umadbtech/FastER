package com.faster.festival.wristband.data.ble

/**
 * Wire-protocol constants for the FastER vendor model. Values come from the
 * firmware `Mobile-Vendor-Model-Dev.md` spec — do not change without a matching
 * firmware update.
 */
object MeshConstants {
    const val COMPANY_ID = 0x0030          // STMicroelectronics
    const val MODEL_ID = 0x0001            // VENDORMODEL_STMICRO_ID1
    const val VENDOR_OPCODE = 0xC30003     // packed: 0xC0 | company[0..1] | model[0]
    const val GROUP_ADDRESS = 0xC000
    const val PROVISIONER_ADDRESS = 0x0002
    const val DEFAULT_UNICAST_START = 0x0003

    // Inbound (device → app)
    const val SUB_TELEMETRY: Byte = 0x10
    const val SUB_SOS: Byte = 0x11
    const val SUB_SOS_CANCEL: Byte = 0x12
    const val SUB_STATUS: Byte = 0x13
    const val SUB_DEVICE_EVENT: Byte = 0x14

    // Outbound (app → device)
    const val SUB_SOS_ACK: Byte = 0x20
    const val SUB_SOS_RESPONDER: Byte = 0x21
    const val SUB_SOS_RESOLVED: Byte = 0x22
    const val SUB_NFC_RESULT: Byte = 0x23
    const val SUB_CONFIG: Byte = 0x30

    // ─── Provisional payload layouts for the spec-stub sub-commands ────────
    //
    // The firmware spec marks 0x13 and 0x23 as STUB (see
    // Mobile-Vendor-Model-Dev.md §45/73). Until firmware locks the wire
    // format, the parser/encoder uses these conservative defaults so the
    // app surface compiles and unit-tests without coupling to a TBD layout.

    /** 0x13 DeviceStatus minimum payload size — provisional. */
    const val STATUS_MIN_BYTES = 12

    /** 0x23 NFC result payload size — provisional. */
    const val NFC_RESULT_BYTES = 14
    /** Tag UID length on the wire (padded to this width). */
    const val NFC_TAG_UID_BYTES = 8

    // Firmware retries SOS at 1s/2s/4s/8s/16s/30s — auto-ACK well inside this.
    const val SOS_ACK_DEADLINE_MS = 5_000L
    const val ACCEL_LSB_PER_G = 4096f

    // BT SIG Mesh GATT service UUIDs (used by the proxy/provisioning adapter).
    const val MESH_PROVISIONING_SERVICE = "00001827-0000-1000-8000-00805F9B34FB"
    const val MESH_PROXY_SERVICE = "00001828-0000-1000-8000-00805F9B34FB"

    /**
     * Canonical wristband id derived from the BLE Mesh unicast address assigned
     * during real provisioning. Single source of truth — every paired-wristband
     * row and every backend `wristband_id` must come from here, never a
     * hardcoded or random value.
     *
     * Example: unicast `3` → `"FSTR-0003"`.
     */
    fun generateWristbandId(unicastAddress: Int): String =
        "FSTR-%04X".format(unicastAddress)
}
