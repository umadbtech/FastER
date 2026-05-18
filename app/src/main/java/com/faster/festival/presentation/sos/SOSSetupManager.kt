package com.faster.festival.presentation.sos

import com.faster.festival.data.local.EncryptedSessionManager
import com.faster.festival.data.remote.ProfileApiService
import com.faster.festival.data.sos.DeviceRegistrationManager
import com.faster.festival.domain.sos.RegisterDeviceUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber

/**
 * Silent SOS setup orchestrator. Run from app start (and after login) — it
 * checks membership readiness via the EXISTING [ProfileApiService.getAccountProfile]
 * endpoint, then triggers the trusted-device bootstrap if eligible.
 *
 * No buttons, no UI prompts during setup — per spec the user must NEVER see
 * "register device" or "verify attestation" actions.
 */
class SOSSetupManager(
    private val sessionManager: EncryptedSessionManager,
    private val profileApi: ProfileApiService,
    private val registerDevice: RegisterDeviceUseCase,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) {

    enum class Readiness {
        Unknown,                  // not yet evaluated
        SetupInProgress,          // currently running register / attest
        Ready,                    // device verified, SOS enabled
        OnboardingRequired,       // membership not active or wrong role
        Failed                    // bootstrap exception
    }

    private val _readiness = MutableStateFlow(Readiness.Unknown)
    val readiness: StateFlow<Readiness> = _readiness.asStateFlow()

    private val mutex = Mutex()

    /**
     * Idempotent. Safe to call multiple times — concurrent calls coalesce so
     * we never run two `register-device` round-trips simultaneously.
     *
     * Returns immediately with the current readiness; the caller observes
     * [readiness] for ongoing transitions.
     */
    fun ensureSetup() = ensureSetupInternal(waitForMembership = false)

    /**
     * Variant for the post-onboarding hook. Same flow as [ensureSetup] but
     * when the first `account-profile` read returns `membership.status =
     * pending` we retry up to [MEMBERSHIP_PROPAGATION_ATTEMPTS] times with
     * [MEMBERSHIP_PROPAGATION_BACKOFF_MS] spacing before giving up. This
     * covers the brief window where the backend's `saveWristband` response
     * has returned to the client but the membership-activation row hasn't
     * propagated yet to the `account-profile` query.
     *
     * Call from `onOnboardingComplete` and from the post-login navigation
     * sites — anywhere we have high confidence the user JUST became active.
     */
    fun ensureSetupAfterOnboarding() = ensureSetupInternal(waitForMembership = true)

    private fun ensureSetupInternal(waitForMembership: Boolean) {
        scope.launch {
            mutex.withLock {
                if (_readiness.value == Readiness.Ready ||
                    _readiness.value == Readiness.SetupInProgress
                ) {
                    return@withLock
                }
                _readiness.value = Readiness.SetupInProgress
                try {
                    val accessToken = sessionManager.getAccessToken()
                    if (accessToken.isNullOrBlank()) {
                        Timber.tag(TAG).i("No session — SOS setup deferred until login")
                        _readiness.value = Readiness.Unknown
                        return@withLock
                    }

                    val membershipActive = if (waitForMembership) {
                        waitForActiveMembership(accessToken)
                    } else {
                        isMembershipActiveAttendee(accessToken)
                    }
                    if (!membershipActive) {
                        Timber.tag(TAG).i("Membership not active — SOS setup deferred")
                        _readiness.value = Readiness.OnboardingRequired
                        return@withLock
                    }

                    val result = registerDevice()
                    _readiness.value = when (result) {
                        is DeviceRegistrationManager.BootstrapResult.Ready -> Readiness.Ready
                        is DeviceRegistrationManager.BootstrapResult.Failed -> {
                            Timber.tag(TAG).w(result.cause, "SOS bootstrap failed")
                            Readiness.Failed
                        }
                    }
                } catch (t: Throwable) {
                    Timber.tag(TAG).w(t, "SOS setup threw unexpectedly")
                    _readiness.value = Readiness.Failed
                }
            }
        }
    }

    /**
     * Polls `account-profile` up to [MEMBERSHIP_PROPAGATION_ATTEMPTS] times
     * for the membership row to flip active. Returns `true` the moment it
     * does; `false` if it never does inside the budget.
     */
    private suspend fun waitForActiveMembership(accessToken: String): Boolean {
        repeat(MEMBERSHIP_PROPAGATION_ATTEMPTS) { attempt ->
            if (isMembershipActiveAttendee(accessToken)) return true
            if (attempt < MEMBERSHIP_PROPAGATION_ATTEMPTS - 1) {
                Timber.tag(TAG).d(
                    "Membership still pending after onboarding — attempt %d/%d, retrying in %dms",
                    attempt + 1, MEMBERSHIP_PROPAGATION_ATTEMPTS,
                    MEMBERSHIP_PROPAGATION_BACKOFF_MS
                )
                delay(MEMBERSHIP_PROPAGATION_BACKOFF_MS)
            }
        }
        return false
    }

    /**
     * Reads the existing `account-profile` endpoint and checks membership.
     * Per spec: `membership.status == active && role == attendee`.
     */
    private suspend fun isMembershipActiveAttendee(accessToken: String): Boolean = try {
        val profile = profileApi.getAccountProfile("Bearer $accessToken")
        val status = profile.membership?.status?.lowercase()
        val role = profile.membership?.role?.lowercase()
        val active = status == "active" && role == "attendee"
        Timber.tag(TAG).d("Membership readiness — status=%s role=%s active=%s",
            status, role, active)
        active
    } catch (t: Throwable) {
        Timber.tag(TAG).w(t, "account-profile lookup failed; assuming not ready")
        false
    }

    private companion object {
        const val TAG = "SOSSetupManager"
        /** Max attempts when waiting for membership activation to propagate. */
        const val MEMBERSHIP_PROPAGATION_ATTEMPTS = 4
        /** Backoff between membership-readiness re-checks (ms). */
        const val MEMBERSHIP_PROPAGATION_BACKOFF_MS = 1_500L
    }
}
