package com.faster.festival.wristband.data.ble

import android.util.Log

/**
 * Hex-breadcrumb logger for every inbound and outbound vendor PDU. Body is
 * single-purpose so it can later forward to analytics / crash-reporting
 * without changing call sites in the manager / repository layer.
 */
class MeshLogger(private val tag: String = "FasterMesh") {
    fun inbound(src: Int, raw: ByteArray) =
        Log.d(tag, "<- 0x${"%04X".format(src)} ${raw.toHex()}")
    fun outbound(dst: Int, raw: ByteArray) =
        Log.d(tag, "-> 0x${"%04X".format(dst)} ${raw.toHex()}")
    fun info(msg: String) = Log.i(tag, msg)
    fun error(msg: String) = Log.e(tag, msg)
    private fun ByteArray.toHex(): String = joinToString(" ") { "%02X".format(it) }
}
