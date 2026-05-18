package com.faster.festival.data.repository.local

import com.faster.festival.data.local.db.SosDeviceDao
import com.faster.festival.data.local.db.SosRegisteredDeviceEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Domain-facing model — UI-friendly mirror of [SosRegisteredDeviceEntity].
 * Boolean status enums kept as String so this matches the DTO surface the
 * backend uses ("pending", "registered", "verified", "failed").
 */
data class SosRegisteredDevice(
    val deviceId: String,
    val platform: String,
    val appId: String,
    val appVersion: String,
    val publicKey: String,
    val keyAlgorithm: String,
    val registrationStatus: String,
    val attestationStatus: String,
    val verified: Boolean,
    val verifiedAt: Long?,
    val createdAt: Long,
    val updatedAt: Long,
    val lastSyncAt: Long?,
    val rawBackendPayload: String?
)

/**
 * Single source of truth for the SOS trusted-device audit record.
 *
 *   • [observeActive] — long-lived Flow for a future "trusted device" UI surface.
 *   • [saveRegistration] — called by [com.faster.festival.data.sos.DeviceRegistrationManager]
 *     after a successful `sos-register-device` round-trip.
 *   • [markAttestationVerified] — called after `sos-verify-attestation` succeeds.
 *
 * The hot-path device_id + verified boolean still live in
 * [com.faster.festival.data.sos.local.DeviceIdentityStore] (DataStore) so
 * `pinch-ingest` doesn't pay a Room I/O cost. This repository is the
 * eventually-consistent audit log on top.
 */
class SosDeviceRepository(private val dao: SosDeviceDao) {

    /** Flow of the active trusted-device row. Null when no device has been
     *  registered yet (e.g. fresh install, post-logout). */
    val activeDevice: Flow<SosRegisteredDevice?> =
        dao.observeActive().map { it?.toDomain() }

    suspend fun getActiveOnce(): SosRegisteredDevice? = dao.getActive()?.toDomain()

    suspend fun findByDeviceId(deviceId: String): SosRegisteredDevice? =
        dao.findByDeviceId(deviceId)?.toDomain()

    /**
     * Upsert the row after a successful `sos-register-device`. Marks every
     * other row inactive — Phase A is single-active-device.
     */
    suspend fun saveRegistration(
        deviceId: String,
        platform: String,
        appId: String,
        appVersion: String,
        publicKey: String,
        keyAlgorithm: String,
        registrationStatus: String,
        rawBackendPayload: String?,
        nowMs: Long = System.currentTimeMillis()
    ) {
        val existing = dao.findByDeviceId(deviceId)
        val createdAt = existing?.createdAt ?: nowMs
        dao.upsert(
            SosRegisteredDeviceEntity(
                id = existing?.id ?: 0L,
                deviceId = deviceId,
                platform = platform,
                appId = appId,
                appVersion = appVersion,
                publicKey = publicKey,
                keyAlgorithm = keyAlgorithm,
                registrationStatus = registrationStatus,
                attestationStatus = existing?.attestationStatus ?: "pending",
                verified = existing?.verified ?: false,
                verifiedAt = existing?.verifiedAt,
                createdAt = createdAt,
                updatedAt = nowMs,
                lastSyncAt = nowMs,
                rawBackendPayload = rawBackendPayload,
                active = true
            )
        )
        dao.deactivateOthers(keepDeviceId = deviceId)
    }

    /**
     * Promote the row to "verified" after a successful `sos-verify-attestation`.
     */
    suspend fun markAttestationVerified(
        deviceId: String,
        rawBackendPayload: String? = null,
        nowMs: Long = System.currentTimeMillis()
    ) {
        dao.updateAttestationStatus(
            deviceId = deviceId,
            status = "verified",
            verified = true,
            verifiedAt = nowMs,
            payload = rawBackendPayload,
            timestamp = nowMs
        )
    }

    suspend fun markAttestationFailed(
        deviceId: String,
        rawBackendPayload: String? = null,
        nowMs: Long = System.currentTimeMillis()
    ) {
        dao.updateAttestationStatus(
            deviceId = deviceId,
            status = "failed",
            verified = false,
            verifiedAt = null,
            payload = rawBackendPayload,
            timestamp = nowMs
        )
    }

    suspend fun markRegistrationFailed(
        deviceId: String,
        rawBackendPayload: String? = null,
        nowMs: Long = System.currentTimeMillis()
    ) {
        dao.updateRegistrationStatus(
            deviceId = deviceId,
            status = "failed",
            payload = rawBackendPayload,
            timestamp = nowMs
        )
    }

    suspend fun touchSync(nowMs: Long = System.currentTimeMillis()) =
        dao.touchLastSync(nowMs)

    /**
     * Hard reset — used during a "reset trusted device" flow (matches
     * [com.faster.festival.data.sos.DeviceRegistrationManager.reset]).
     */
    suspend fun clear() = dao.clear()
}

private fun SosRegisteredDeviceEntity.toDomain() = SosRegisteredDevice(
    deviceId = deviceId,
    platform = platform,
    appId = appId,
    appVersion = appVersion,
    publicKey = publicKey,
    keyAlgorithm = keyAlgorithm,
    registrationStatus = registrationStatus,
    attestationStatus = attestationStatus,
    verified = verified,
    verifiedAt = verifiedAt,
    createdAt = createdAt,
    updatedAt = updatedAt,
    lastSyncAt = lastSyncAt,
    rawBackendPayload = rawBackendPayload
)
