package com.faster.festival.data.repository.local

import com.faster.festival.data.local.db.PairedWristbandEntity
import com.faster.festival.data.local.db.WristbandDao
import com.faster.festival.data.remote.ApiError
import com.faster.festival.data.remote.Project1ApiService
import com.faster.festival.data.remote.SosHistoryResponse
import com.faster.festival.data.remote.TelemetrySnapshot
import com.faster.festival.data.remote.SosPhoneLocation
import com.faster.festival.data.remote.WristbandPairRequest
import com.faster.festival.data.remote.WristbandPairing
import com.faster.festival.data.remote.WristbandSosRecordRequest
import com.faster.festival.data.remote.WristbandSosRecordResponse
import com.faster.festival.data.remote.WristbandTelemetryBatchRequest
import com.faster.festival.data.remote.WristbandTelemetryBatchResponse
import com.faster.festival.data.remote.WristbandUnpairRequest
import com.faster.festival.data.remote.WristbandUnpairResponse
import com.faster.festival.data.remote.safeApiCall
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.UUID

/**
 * Domain model exposed to the UI layer.
 * Mirrors [PairedWristbandEntity] but lives in a UI-friendly shape.
 */
data class PairedWristband(
    val wristbandId: String,
    val deviceName: String?,
    val firmwareVersion: String?,
    val batteryLevel: Int?,
    val connectionStatus: String,
    val pairedAt: Long,
    val unicastAddress: Int?,
    val groupAddress: Int,
    val lastSeenAt: Long?
)

/**
 * Repository for the paired wristband — single source of truth for whatever
 * wristband is currently active. Backed by Room/SQLite so data survives
 * process death and app reinstalls.
 *
 * `activeWristband` filters out legacy v1 rows where unicastAddress is null
 * (i.e. rows written by the simulated walkthrough before the real BLE Mesh
 * stack landed). Those rows are intentionally invisible to the UI so users
 * are not auto-routed to the dashboard / reconnect for a fake pairing.
 */
class WristbandRepository(
    private val dao: WristbandDao,
    /**
     * Project 1 backend surface for wristband CRUD / heartbeat / telemetry
     * batch. Optional so unit tests and the existing simulated walkthrough
     * keep working with local-only state; the orchestration layer (DI)
     * injects the real service in production.
     */
    private val remote: Project1ApiService? = null
) {

    val activeWristband: Flow<PairedWristband?> =
        dao.observeActive().map { row ->
            row?.takeIf { it.unicastAddress != null }?.toDomain()
        }

    suspend fun savePairedWristband(
        wristbandId: String,
        deviceName: String? = null,
        firmwareVersion: String? = "—",
        batteryLevel: Int? = 0,
        connectionStatus: String = "Connected",
        unicastAddress: Int? = null
    ) {
        dao.deactivateAll()
        dao.upsert(
            PairedWristbandEntity(
                wristbandId = wristbandId,
                deviceName = deviceName,
                firmwareVersion = firmwareVersion,
                batteryLevel = batteryLevel,
                connectionStatus = connectionStatus,
                pairedAt = System.currentTimeMillis(),
                isActive = true,
                unicastAddress = unicastAddress,
                groupAddress = 0xC000,
                lastSeenAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun getActiveOnce(): PairedWristband? = dao.getActive()
        ?.takeIf { it.unicastAddress != null }
        ?.toDomain()

    suspend fun touchLastSeen() = dao.updateLastSeen(System.currentTimeMillis())

    suspend fun unpair() = dao.clear()

    // ───────────────────────────────────────────────────────────────────────
    // Backend sync — only used when [remote] is wired (production).
    // Local Room write is the source of truth on the device; remote calls
    // are best-effort and never block local state.
    // ───────────────────────────────────────────────────────────────────────

    /**
     * Local upsert + best-effort POST to `wristband-pair`. The local row is
     * written FIRST so a backend failure can't desync the dashboard. The
     * server-issued `pairing_id` is returned on success; callers can
     * persist it separately if they need it.
     *
     * Backend 409 (active_pairing_exists) is surfaced as [ApiError.Conflict]
     * — the caller decides whether to prompt for unpair-then-retry.
     */
    suspend fun pairAndSync(
        wristbandId: String,
        unicastAddress: Int,
        deviceName: String? = null,
        firmwareVersion: String? = null,
        batteryLevel: Int? = null
    ): Result<WristbandPairing?> {
        savePairedWristband(
            wristbandId = wristbandId,
            deviceName = deviceName,
            firmwareVersion = firmwareVersion ?: "—",
            batteryLevel = batteryLevel ?: 0,
            connectionStatus = "Connected",
            unicastAddress = unicastAddress
        )
        val api = remote ?: return Result.success(null)
        return safeApiCall("project1/wristband-pair") {
            api.pairWristband(
                body = WristbandPairRequest(
                    wristbandId = wristbandId,
                    unicastAddress = unicastAddress,
                    deviceName = deviceName,
                    firmwareVersion = firmwareVersion,
                    pairedAt = nowIso()
                ),
                idempotencyKey = newIdempotencyKey()
            )
        }.also { it.onFailure { err ->
            Timber.tag(TAG).w(err, "wristband-pair sync failed (local row preserved)")
        } }
    }

    /**
     * Pull-from-backend recovery — used on cold-launch when the local Room
     * is empty (fresh install / cleared data) but the user already has an
     * active pairing server-side. The caller projects the [WristbandPairing]
     * into Room via [savePairedWristband] only if it makes sense; we don't
     * do it here automatically because the unicast address may be stale
     * relative to the BLE Mesh provisioner store.
     *
     * HTTP 204 from the backend (no active pairing) is mapped to
     * `Result.success(null)` rather than [ApiError.NotFound] because it is
     * a normal "no-state" signal, not an error.
     */
    suspend fun fetchActiveFromBackend(): Result<WristbandPairing?> {
        val api = remote ?: return Result.success(null)
        return safeApiCall("project1/wristband-active") {
            val resp = api.getActiveWristband()
            when (resp.code()) {
                200 -> resp.body()
                204 -> null
                else -> throw retrofit2.HttpException(resp)
            }
        }
    }

    /**
     * Local clear + best-effort DELETE to `wristband-unpair`. Local Room is
     * cleared FIRST so a backend 5xx can't trap the user in a "paired" UI.
     * 404 from the backend is treated as success (idempotent unpair per
     * `Wristband-Backend-API.md` §5.3).
     */
    suspend fun unpairAndSync(
        wristbandId: String,
        reason: String = "user_initiated"
    ): Result<WristbandUnpairResponse?> {
        unpair()
        val api = remote ?: return Result.success(null)
        return safeApiCall("project1/wristband-unpair") {
            api.unpairWristband(
                wristbandId = wristbandId,
                body = WristbandUnpairRequest(reason = reason),
                idempotencyKey = newIdempotencyKey()
            )
        }.recoverCatching { err ->
            if (err is ApiError.NotFound) {
                Timber.tag(TAG).i("Unpair 404 — treating as success (already unpaired)")
                null
            } else throw err
        }
    }

    /**
     * High-frequency telemetry batch upload. Caller buffers samples; this
     * call sends them as one POST. Returns the server-side accept/reject
     * counts so the caller can re-queue rejected slots (e.g. malformed
     * device_state).
     */
    suspend fun uploadTelemetryBatch(
        wristbandId: String,
        snapshots: List<TelemetrySnapshot>
    ): Result<WristbandTelemetryBatchResponse> {
        val api = remote ?: return Result.failure(
            IllegalStateException("Project1ApiService not wired — telemetry upload disabled")
        )
        if (snapshots.isEmpty()) {
            return Result.success(WristbandTelemetryBatchResponse(accepted = 0, rejected = 0))
        }
        return safeApiCall("project1/wristband-telemetry-batch") {
            api.uploadTelemetryBatch(
                body = WristbandTelemetryBatchRequest(
                    wristbandId = wristbandId,
                    snapshots = snapshots
                ),
                idempotencyKey = newIdempotencyKey()
            )
        }
    }

    /**
     * Best-effort POST to `wristband-sos-record` — the optional Project 1
     * audit row for a wristband 0x11 event. The primary SOS dispatch path
     * is signed Project 2 `pinch-ingest`; this call is purely an audit
     * mirror and must NOT block the SOS critical path. Caller fires it
     * fire-and-forget on an app-scope coroutine.
     *
     * Backend rule: `(wristband_id, event_id)` is unique. Replays of the
     * same event_id are idempotent server-side, so the worker / retry path
     * can call this repeatedly without producing duplicates.
     */
    suspend fun recordSosEvent(
        wristbandId: String,
        eventId: Long,
        state: String,
        retryCount: Int,
        batteryPct: Int,
        deviceUptimeMs: Long,
        receivedAtIso: String,
        clientTriggerId: String,
        phoneLocation: SosPhoneLocation? = null
    ): Result<WristbandSosRecordResponse?> {
        val api = remote ?: return Result.success(null)
        return safeApiCall("project1/wristband-sos-record") {
            api.recordSosEvent(
                wristbandId = wristbandId,
                body = WristbandSosRecordRequest(
                    eventId = eventId,
                    state = state,
                    retryCount = retryCount,
                    batteryPct = batteryPct,
                    deviceUptimeMs = deviceUptimeMs,
                    receivedAt = receivedAtIso,
                    clientTriggerId = clientTriggerId,
                    phoneLocation = phoneLocation
                ),
                idempotencyKey = newIdempotencyKey()
            )
        }.also { it.onFailure { err ->
            // Audit POST failures NEVER affect the SOS state — log only.
            Timber.tag(TAG).w(err, "wristband-sos-record failed (audit only, SOS unaffected)")
        } }
    }

    /**
     * Optional SOS history fetch (paginated). Returns an empty list if the
     * backend wasn't wired (debug builds / tests). 404 maps to an empty list
     * since "no history yet" isn't a failure.
     */
    suspend fun fetchSosHistory(
        wristbandId: String,
        limit: Int = 50,
        beforeIsoTimestamp: String? = null
    ): Result<SosHistoryResponse> {
        val api = remote ?: return Result.success(SosHistoryResponse())
        return safeApiCall("project1/wristband-sos-history") {
            api.listSosHistory(wristbandId, limit, beforeIsoTimestamp)
        }.recoverCatching { err ->
            if (err is ApiError.NotFound) SosHistoryResponse() else throw err
        }
    }

    private fun nowIso(): String =
        DateTimeFormatter.ISO_INSTANT.format(Instant.now())

    private fun newIdempotencyKey(): String =
        UUID.randomUUID().toString().replace("-", "").take(32)

    private companion object {
        const val TAG = "WristbandRepo"
    }
}

private fun PairedWristbandEntity.toDomain() = PairedWristband(
    wristbandId = wristbandId,
    deviceName = deviceName,
    firmwareVersion = firmwareVersion,
    batteryLevel = batteryLevel,
    connectionStatus = connectionStatus,
    pairedAt = pairedAt,
    unicastAddress = unicastAddress,
    groupAddress = groupAddress,
    lastSeenAt = lastSeenAt
)
