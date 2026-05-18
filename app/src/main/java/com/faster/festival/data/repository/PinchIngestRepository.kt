package com.faster.festival.data.repository

import com.faster.festival.AppConfig
import com.faster.festival.core.crypto.NonceGenerator
import com.faster.festival.data.remote.ApiError
import com.faster.festival.data.remote.PinchIngestDispatchResult
import com.faster.festival.data.remote.Project2ApiService
import com.faster.festival.data.remote.safeApiCall
import com.faster.festival.data.sos.remote.SosLocation
import com.faster.festival.data.sos.remote.WristbandInfo
import com.faster.festival.domain.sos.SosRepository
import com.faster.festival.domain.sos.SosUserStatus
import retrofit2.HttpException
import timber.log.Timber

/**
 * Caller-facing facade for the Project 2 signed `pinch-ingest` dispatch.
 *
 * **No new transport.** This repository delegates to the existing
 * [SosRepository.triggerSos] which already owns:
 *   • Ed25519 canonical signing
 *   • Body-hash-equals-payload invariant
 *   • Two-project Retrofit clients (Project 1 for register/verify; Project 2
 *     for pinch-ingest)
 *
 * The point of the facade is that any feature that wants to dispatch a
 * pinch event (e.g. an automation harness, a phone-side trigger that lives
 * outside the SOS module) can depend on `PinchIngestRepository` without
 * pulling in the broader SOS surface.
 *
 * Errors are mapped to [ApiError]. Callers can check
 * [ApiError.retrySafe] to decide whether to back off — the underlying
 * [SosRepository.triggerSos] re-signs on each attempt (fresh nonce +
 * timestamp), so a re-issue of the same [clientTriggerId] is server-side
 * idempotent.
 *
 * [Project2ApiService] is injected so a future direct-unsigned Project 2
 * endpoint (e.g. a pre-flight health probe) can also be reached from here.
 */
class PinchIngestRepository(
    private val sosRepository: SosRepository,
    private val project2ApiService: Project2ApiService
) {

    /**
     * Dispatch a signed pinch-ingest event.
     *
     * @param clientTriggerId caller-generated. Retries MUST reuse the same id
     *   so the backend dedups. Default: a fresh trigger id.
     * @return The dispatch result on success, or [ApiError] on failure. The
     *   typed error tells the caller whether to retry (transport / 5xx) or
     *   give up (4xx semantic).
     */
    suspend fun dispatch(
        location: SosLocation?,
        wristband: WristbandInfo,
        clientTriggerId: String = NonceGenerator.newClientTriggerId(),
        festivalId: String = AppConfig.DEFAULT_FESTIVAL_SLUG
    ): Result<PinchIngestDispatchResult> {
        val result = sosRepository.triggerSos(
            clientTriggerId = clientTriggerId,
            festivalId = festivalId,
            location = location,
            wristband = wristband
        )
        return result.fold(
            onSuccess = { handle ->
                Timber.tag(TAG).i(
                    "pinch-ingest dispatched | trigger=%s alert=%s status=%s",
                    handle.clientTriggerId.takeLast(8),
                    handle.alertId ?: "-",
                    handle.initialStatus.name
                )
                Result.success(
                    PinchIngestDispatchResult(
                        ok = true,
                        clientTriggerId = handle.clientTriggerId,
                        alertId = handle.alertId,
                        status = handle.initialStatus
                            .takeIf { it != SosUserStatus.Unknown }
                            ?.name,
                        userStatus = handle.initialStatus.name
                    )
                )
            },
            onFailure = { err ->
                Result.failure(mapDispatchError(err))
            }
        )
    }

    /**
     * Optional pre-flight health probe — uses the unsigned auxiliary surface
     * on [Project2ApiService]. Returns Unit on a 2xx response so the caller
     * can short-circuit a dispatch if Project 2 is unreachable.
     */
    suspend fun pinchHealth(): Result<Unit> =
        safeApiCall("project2/pinch-health") {
            val resp = project2ApiService.pinchHealth()
            if (!resp.isSuccessful) throw HttpException(resp)
            Unit
        }

    private fun mapDispatchError(err: Throwable): ApiError {
        if (err is ApiError) return err
        return when (err) {
            is HttpException -> com.faster.festival.data.remote.mapHttpException(
                "project2/pinch-ingest", err
            )
            is java.io.IOException -> ApiError.Network(err)
            else -> ApiError.Unknown(err)
        }
    }

    private companion object {
        const val TAG = "PinchIngestRepo"
    }
}
