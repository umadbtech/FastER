package com.faster.festival.data.local.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Audit record for the SOS trusted-device registration with the backend.
 *
 * The hot-path cache for `device_id` + `attestation_verified` continues to
 * live in [com.faster.festival.data.sos.local.DeviceIdentityStore] (DataStore)
 * so the signed `pinch-ingest` flow doesn't hit Room. This row is the
 * canonical record — extended fields for status, timestamps, and the raw
 * backend response payload (useful for support / debugging).
 *
 * The Ed25519 private key itself lives in
 * [com.faster.festival.core.security.Ed25519KeyManager]'s
 * EncryptedSharedPreferences. We persist only the public key here.
 *
 * Phase A is single-active-device per install — the `active` flag exists for
 * future multi-device scenarios (e.g. user switches phones, old row is
 * marked inactive instead of deleted).
 */
@Entity(
    tableName = "sos_registered_device",
    indices = [Index(value = ["device_id"], unique = true)]
)
data class SosRegisteredDeviceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    @ColumnInfo(name = "device_id") val deviceId: String,
    val platform: String,
    @ColumnInfo(name = "app_id") val appId: String,
    @ColumnInfo(name = "app_version") val appVersion: String,
    @ColumnInfo(name = "public_key") val publicKey: String,
    @ColumnInfo(name = "key_algorithm") val keyAlgorithm: String,
    /** "pending" | "registered" | "failed" — mirrors backend lifecycle. */
    @ColumnInfo(name = "registration_status") val registrationStatus: String,
    /** "pending" | "verified" | "failed". */
    @ColumnInfo(name = "attestation_status") val attestationStatus: String,
    val verified: Boolean,
    @ColumnInfo(name = "verified_at") val verifiedAt: Long?,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "last_sync_at") val lastSyncAt: Long?,
    @ColumnInfo(name = "raw_backend_payload") val rawBackendPayload: String?,
    val active: Boolean = true
)
