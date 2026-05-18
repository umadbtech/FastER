package com.faster.festival.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import timber.log.Timber
import java.io.IOException

/**
 * Typed surface for HTTP failures across the FastER backend. Built so the
 * existing networking layer ([com.faster.festival.di.NetworkModule] for
 * Project 1, [com.faster.festival.data.sos.remote.SosNetworkClient] for
 * Project 2) does NOT need a separate interceptor — callers wrap their
 * Retrofit call in [safeApiCall] and get back a [Result] whose failure side
 * is always an [ApiError].
 *
 * Codes intentionally enumerated:
 *
 * | HTTP | [ApiError] |
 * |---|---|
 * | 400 | [ApiError.BadRequest] |
 * | 401 | [ApiError.Unauthorized] (TokenRefreshInterceptor has already tried) |
 * | 403 | [ApiError.Forbidden] |
 * | 404 | [ApiError.NotFound] |
 * | 409 | [ApiError.Conflict] |
 * | 413 | [ApiError.PayloadTooLarge] |
 * | 422 | [ApiError.Unprocessable] |
 * | 5xx | [ApiError.Server] (retry-safe — caller may back off) |
 * | IOException | [ApiError.Network] (retry-safe) |
 * | other | [ApiError.Unknown] |
 *
 * The `retrySafe` flag is the single signal a retry layer needs — true for
 * transient transport / server-side failures, false for semantic 4xx that a
 * re-send wouldn't fix.
 */
sealed class ApiError(
    val httpCode: Int?,
    val backendCode: String?,
    val userMessage: String,
    val retrySafe: Boolean,
    cause: Throwable? = null
) : Throwable(userMessage, cause) {

    class BadRequest(backendCode: String?, message: String, cause: Throwable?) :
        ApiError(400, backendCode, message, retrySafe = false, cause)

    class Unauthorized(backendCode: String?, message: String, cause: Throwable?) :
        ApiError(401, backendCode, message, retrySafe = false, cause)

    class Forbidden(backendCode: String?, message: String, cause: Throwable?) :
        ApiError(403, backendCode, message, retrySafe = false, cause)

    class NotFound(backendCode: String?, message: String, cause: Throwable?) :
        ApiError(404, backendCode, message, retrySafe = false, cause)

    class Conflict(
        backendCode: String?,
        message: String,
        val details: Map<String, String>,
        cause: Throwable?
    ) : ApiError(409, backendCode, message, retrySafe = false, cause)

    class PayloadTooLarge(backendCode: String?, message: String, cause: Throwable?) :
        ApiError(413, backendCode, message, retrySafe = false, cause)

    class Unprocessable(backendCode: String?, message: String, cause: Throwable?) :
        ApiError(422, backendCode, message, retrySafe = false, cause)

    class Server(httpCode: Int, backendCode: String?, message: String, cause: Throwable?) :
        ApiError(httpCode, backendCode, message, retrySafe = true, cause)

    class Network(cause: Throwable) :
        ApiError(null, null, "Network unavailable.", retrySafe = true, cause)

    class Unknown(cause: Throwable) :
        ApiError(null, null, "Unexpected error: ${cause.message}", retrySafe = false, cause)
}

/**
 * Backend uniform error envelope — see `Wristband-Backend-API.md` §7. All four
 * fields are optional because Supabase Edge Functions sometimes emit shorter
 * shapes (`{ "error": "…" }`) and sometimes raw text.
 */
@Serializable
private data class BackendErrorEnvelope(
    val error: String? = null,
    val message: String? = null,
    @SerialName("request_id") val requestId: String? = null,
    val details: Map<String, String> = emptyMap()
)

private val errorJson = Json { ignoreUnknownKeys = true; isLenient = true }

/**
 * Wraps a Retrofit `suspend` call and returns `Result<T>` where the failure
 * side is always an [ApiError]. Logs every failure with the endpoint path and
 * the parsed backend error code for structured logcat triage.
 */
suspend inline fun <T> safeApiCall(
    endpointTag: String,
    crossinline block: suspend () -> T
): Result<T> = try {
    Result.success(block())
} catch (http: HttpException) {
    val mapped = mapHttpException(endpointTag, http)
    Result.failure(mapped)
} catch (io: IOException) {
    Timber.tag("ApiCall").w(io, "%s — network failure", endpointTag)
    Result.failure(ApiError.Network(io))
} catch (t: Throwable) {
    Timber.tag("ApiCall").w(t, "%s — unknown failure", endpointTag)
    Result.failure(ApiError.Unknown(t))
}

@PublishedApi
internal fun mapHttpException(endpointTag: String, http: HttpException): ApiError {
    val errorBody = runCatching {
        http.response()?.errorBody()?.string()
    }.getOrNull()
    val parsed = errorBody?.let { body ->
        runCatching { errorJson.decodeFromString(BackendErrorEnvelope.serializer(), body) }
            .getOrNull()
    }
    val code = http.code()
    val backendCode = parsed?.error
    val message = parsed?.message
        ?: parsed?.error
        ?: errorBody?.take(200)
        ?: http.message()
        ?: "HTTP $code"

    Timber.tag("ApiCall").w(
        "%s — HTTP %d backend_code=%s request_id=%s",
        endpointTag, code, backendCode ?: "-", parsed?.requestId ?: "-"
    )

    return when (code) {
        400 -> ApiError.BadRequest(backendCode, message, http)
        401 -> ApiError.Unauthorized(backendCode, message, http)
        403 -> ApiError.Forbidden(backendCode, message, http)
        404 -> ApiError.NotFound(backendCode, message, http)
        409 -> ApiError.Conflict(backendCode, message, parsed?.details ?: emptyMap(), http)
        413 -> ApiError.PayloadTooLarge(backendCode, message, http)
        422 -> ApiError.Unprocessable(backendCode, message, http)
        in 500..599 -> ApiError.Server(code, backendCode, message, http)
        else -> ApiError.Unknown(http)
    }
}
