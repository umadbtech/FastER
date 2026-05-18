package com.faster.festival.wristband.data.ble

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.meshDataStore by preferencesDataStore(name = "wristband_mesh")

/**
 * DataStore-backed source of truth for the BLE Mesh provisioner state:
 * the network JSON exported by Nordic, the assigned unicast address of
 * our paired wristband, and the last-seen timestamp.
 *
 * Room is the user-facing mirror; this store is the technical truth. They
 * are reconciled inside the wristband mesh repository on a real provisioning
 * success.
 */
class MeshNetworkStore(private val context: Context) {

    private object Keys {
        val NETWORK = stringPreferencesKey("mesh_network_json")
        val UNICAST = intPreferencesKey("mesh_unicast")
        val LAST_SEEN = longPreferencesKey("mesh_last_seen")
    }

    val unicast: Flow<Int?> = context.meshDataStore.data.map { it[Keys.UNICAST] }
    val networkJson: Flow<String?> = context.meshDataStore.data.map { it[Keys.NETWORK] }
    val lastSeen: Flow<Long?> = context.meshDataStore.data.map { it[Keys.LAST_SEEN] }

    suspend fun saveUnicast(address: Int) =
        context.meshDataStore.edit { it[Keys.UNICAST] = address }
    suspend fun saveNetwork(json: String) =
        context.meshDataStore.edit { it[Keys.NETWORK] = json }
    suspend fun touchLastSeen() =
        context.meshDataStore.edit { it[Keys.LAST_SEEN] = System.currentTimeMillis() }
    suspend fun unicastOnce(): Int? = context.meshDataStore.data.first()[Keys.UNICAST]
    suspend fun clear() = context.meshDataStore.edit { it.clear() }
}
