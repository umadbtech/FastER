package com.faster.festival.data.sos.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.sosDeviceDataStore by preferencesDataStore(name = "faster_sos_device")

/**
 * Persists the SOS device identifier and its verified-attestation state.
 *
 * The Ed25519 private key itself lives in
 * [com.faster.festival.core.security.Ed25519KeyManager]'s
 * EncryptedSharedPreferences — this store holds only metadata (no key
 * material).
 */
class DeviceIdentityStore(private val context: Context) {

    private object Keys {
        val DEVICE_ID = stringPreferencesKey("device_id")
        val ATTESTATION_VERIFIED = booleanPreferencesKey("attestation_verified")
    }

    val deviceIdFlow: Flow<String?> =
        context.sosDeviceDataStore.data.map { it[Keys.DEVICE_ID] }

    val attestationVerifiedFlow: Flow<Boolean> =
        context.sosDeviceDataStore.data.map { it[Keys.ATTESTATION_VERIFIED] ?: false }

    suspend fun deviceId(): String? =
        context.sosDeviceDataStore.data.first()[Keys.DEVICE_ID]

    suspend fun attestationVerified(): Boolean =
        context.sosDeviceDataStore.data.first()[Keys.ATTESTATION_VERIFIED] ?: false

    suspend fun saveDeviceId(deviceId: String) =
        context.sosDeviceDataStore.edit { it[Keys.DEVICE_ID] = deviceId }

    suspend fun markAttestationVerified(verified: Boolean) =
        context.sosDeviceDataStore.edit { it[Keys.ATTESTATION_VERIFIED] = verified }

    suspend fun clear() =
        context.sosDeviceDataStore.edit { it.clear() }
}
