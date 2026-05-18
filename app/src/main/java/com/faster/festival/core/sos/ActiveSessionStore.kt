package com.faster.festival.core.sos

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.activeSosDataStore by preferencesDataStore(name = "faster_active_sos")

/**
 * DataStore-backed mirror of the currently active [ActiveSOSSession].
 *
 * Survives process death so the polling job can resume on the next app start
 * without losing the alert. The data here duplicates what's in
 * [com.faster.festival.core.sos.EmergencySOSManager] — that's intentional;
 * the in-memory copy is the source of truth while the manager is alive, the
 * DataStore copy is the recovery copy for cold starts.
 */
class ActiveSessionStore(private val context: Context) {

    private object Keys {
        val CLIENT_TRIGGER_ID = stringPreferencesKey("active_client_trigger_id")
        val SOURCE = stringPreferencesKey("active_source")             // "manual" | "wristband"
        val FESTIVAL_ID = stringPreferencesKey("active_festival_id")
        val STARTED_AT = longPreferencesKey("active_started_at")
        val WRISTBAND_EVENT_ID = longPreferencesKey("active_wb_event_id")
        val WRISTBAND_RETRY_COUNT = longPreferencesKey("active_wb_retry")
        val WRISTBAND_BATTERY = longPreferencesKey("active_wb_battery")
        val WRISTBAND_UPTIME_MS = longPreferencesKey("active_wb_uptime")
        val TRACKING_SESSION_ID = stringPreferencesKey("active_tracking_session_id")
        val ALERT_ID = stringPreferencesKey("active_alert_id")
    }

    suspend fun load(): ActiveSOSSession? {
        val prefs = context.activeSosDataStore.data.first()
        val tid = prefs[Keys.CLIENT_TRIGGER_ID] ?: return null
        val sourceTag = prefs[Keys.SOURCE] ?: return null
        val festivalId = prefs[Keys.FESTIVAL_ID] ?: ""
        val startedAt = prefs[Keys.STARTED_AT] ?: System.currentTimeMillis()
        val source = when (sourceTag) {
            "manual" -> SosSource.Manual
            "wristband" -> SosSource.Wristband
            else -> return null
        }
        val wbEvent = prefs[Keys.WRISTBAND_EVENT_ID]?.let { eventId ->
            WristbandEventInfo(
                eventId = eventId,
                retryCount = (prefs[Keys.WRISTBAND_RETRY_COUNT] ?: 0L).toInt(),
                batteryPct = (prefs[Keys.WRISTBAND_BATTERY] ?: 0L).toInt(),
                deviceUptimeMs = prefs[Keys.WRISTBAND_UPTIME_MS] ?: 0L
            )
        }
        return ActiveSOSSession(
            clientTriggerId = tid,
            source = source,
            festivalId = festivalId,
            startedAtEpochMs = startedAt,
            wristbandEvent = wbEvent,
            trackingSessionId = prefs[Keys.TRACKING_SESSION_ID],
            alertId = prefs[Keys.ALERT_ID]
        )
    }

    suspend fun save(session: ActiveSOSSession) {
        context.activeSosDataStore.edit { p ->
            p[Keys.CLIENT_TRIGGER_ID] = session.clientTriggerId
            p[Keys.SOURCE] = session.sourceTag
            p[Keys.FESTIVAL_ID] = session.festivalId
            p[Keys.STARTED_AT] = session.startedAtEpochMs
            session.wristbandEvent?.let { wb ->
                p[Keys.WRISTBAND_EVENT_ID] = wb.eventId
                p[Keys.WRISTBAND_RETRY_COUNT] = wb.retryCount.toLong()
                p[Keys.WRISTBAND_BATTERY] = wb.batteryPct.toLong()
                p[Keys.WRISTBAND_UPTIME_MS] = wb.deviceUptimeMs
            } ?: run {
                p.remove(Keys.WRISTBAND_EVENT_ID)
                p.remove(Keys.WRISTBAND_RETRY_COUNT)
                p.remove(Keys.WRISTBAND_BATTERY)
                p.remove(Keys.WRISTBAND_UPTIME_MS)
            }
            session.trackingSessionId?.let { p[Keys.TRACKING_SESSION_ID] = it }
                ?: p.remove(Keys.TRACKING_SESSION_ID)
            session.alertId?.let { p[Keys.ALERT_ID] = it }
                ?: p.remove(Keys.ALERT_ID)
        }
    }

    suspend fun clear() = context.activeSosDataStore.edit { it.clear() }
}
