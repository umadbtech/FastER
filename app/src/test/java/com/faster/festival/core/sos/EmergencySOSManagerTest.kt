package com.faster.festival.core.sos

import com.faster.festival.data.repository.local.PairedWristband
import com.faster.festival.data.repository.local.WristbandRepository
import com.faster.festival.data.sos.DeviceRegistrationManager
import com.faster.festival.data.sos.SosLocationProvider
import com.faster.festival.data.sos.remote.SosAlert
import com.faster.festival.data.sos.remote.SosLocation
import com.faster.festival.data.sos.remote.WristbandInfo
import com.faster.festival.domain.sos.PollSOSStatusUseCase
import com.faster.festival.domain.sos.SosRepository
import com.faster.festival.domain.sos.SosUserStatus
import com.faster.festival.domain.sos.TriggerHandle
import com.faster.festival.domain.sos.TriggerSOSUseCase
import com.faster.festival.wristband.domain.repository.WristbandMeshRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals

/**
 * End-to-end test of the unified SOS orchestrator.
 *
 * Mocks the backend (via the [SosRepository] interface) and the wristband
 * BLE side (via [WristbandMeshRepository]). Real [SOSDeduplicator] +
 * [WristbandAckManager] are exercised so dedup / single-ACK logic is
 * actually verified, not stubbed.
 *
 * Polling uses canned responses queued in [FakeSosRepository.pollQueue]:
 *  • A terminal first emission resolves the session immediately.
 *  • A non-terminal first emission lands in Active state.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class EmergencySOSManagerTest {

    /** In-memory fake of [SosRepository] — only the methods the manager touches. */
    private class FakeSosRepository : SosRepository {
        var triggerCallCount = 0
        var triggerResult: Result<TriggerHandle> = Result.success(
            TriggerHandle(
                clientTriggerId = "stub",
                alertId = "alert-stub",
                initialStatus = SosUserStatus.SosReceived
            )
        )

        /** Each poll returns the next item; if exhausted, returns the last forever. */
        var pollQueue: List<SosAlert?> = emptyList()
        private var pollIndex = 0

        override val isDeviceReady: Flow<Boolean> = flowOf(true)
        override suspend fun deviceId(): String? = "device-test"
        override suspend fun bootstrap() = DeviceRegistrationManager.BootstrapResult.Ready
        override suspend fun resetTrustedDevice() = Unit

        override suspend fun triggerSos(
            clientTriggerId: String,
            festivalId: String,
            location: SosLocation?,
            wristband: WristbandInfo
        ): Result<TriggerHandle> {
            triggerCallCount++
            return triggerResult.map { it.copy(clientTriggerId = clientTriggerId) }
        }

        override suspend fun sendLocationUpdate(
            clientTriggerId: String,
            trackingSessionId: String,
            location: SosLocation
        ): Result<Unit> = Result.success(Unit)

        override suspend fun pollStatus(clientTriggerId: String): Result<SosAlert?> {
            val alert = pollQueue.getOrNull(pollIndex) ?: pollQueue.lastOrNull()
            pollIndex++
            return Result.success(alert)
        }
    }

    private fun wristbandMeshRepo(): WristbandMeshRepository = mockk(relaxed = true)

    private fun pairedRepoNoPair(): WristbandRepository = mockk {
        coEvery { activeWristband } returns flowOf(null)
    }

    private fun locationProvider(): SosLocationProvider = mockk {
        coEvery { currentFix(any()) } returns null
    }

    private fun sessionStore(): ActiveSessionStore = mockk(relaxed = true)

    private fun managerWith(
        repo: SosRepository,
        wristbandRepo: WristbandMeshRepository = wristbandMeshRepo(),
        scope: TestScope
    ) = EmergencySOSManager(
        // appContext is used by the manager only to enqueue / cancel the
        // WorkManager-backed retry worker. Tests do not exercise that path;
        // a mockk-relaxed Context is sufficient for the JVM unit harness.
        appContext = io.mockk.mockk(relaxed = true),
        triggerSos = TriggerSOSUseCase(repo),
        pollStatus = PollSOSStatusUseCase(repo),
        locationProvider = locationProvider(),
        pairedWristbandRepo = pairedRepoNoPair(),
        wristbandAck = WristbandAckManager(wristbandRepo),
        sessionStore = sessionStore(),
        deduplicator = SOSDeduplicator(),
        scope = scope
    )

    // ─── Tests ─────────────────────────────────────────────────────────────

    @Test
    fun `manual trigger reaches Active with first non-terminal poll`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val scope = TestScope(dispatcher)
        val repo = FakeSosRepository().apply {
            pollQueue = listOf(SosAlert(userStatus = "DISPATCH_RECEIVED"))
        }
        val manager = managerWith(repo, scope = scope)

        manager.startManualSOS(festivalId = "fest-1")
        advanceUntilIdle()

        assertEquals(1, repo.triggerCallCount, "exactly one backend pinch-ingest")
        val state = manager.state.value
        assertIs<EmergencySOSState.Active>(state)
        assertEquals(SosSource.Manual, state.session.source)
        assertEquals(SosUserStatus.DispatchReceived, state.userStatus)
    }

    @Test
    fun `wristband 0x11 retries dedup to ONE backend alert and ONE BLE ACK`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val scope = TestScope(dispatcher)
        val repo = FakeSosRepository().apply {
            pollQueue = listOf(SosAlert(userStatus = "DISPATCH_RECEIVED"))
        }
        val wristbandRepo = wristbandMeshRepo()
        coEvery { wristbandRepo.sendSosAck(any(), any()) } returns Result.success(Unit)

        val manager = managerWith(repo, wristbandRepo, scope = scope)

        // Firmware fires once + retries 4 times — all carry event_id = 0xCAFE.
        repeat(5) {
            manager.handleWristbandSOS(
                eventId = 0xCAFE,
                retryCount = it,
                batteryPct = 73,
                deviceUptimeMs = 1000L * it
            )
        }
        advanceUntilIdle()

        assertEquals(1, repo.triggerCallCount, "wristband retries must dedup to ONE backend alert")
        coVerify(exactly = 1) { wristbandRepo.sendSosAck(0xCAFE, helpDispatched = true) }

        val state = manager.state.value
        assertIs<EmergencySOSState.Active>(state)
        assertEquals(SosSource.Wristband, state.session.source)
        assertEquals(0xCAFE, state.session.wristbandEvent?.eventId)
    }

    @Test
    fun `manual trigger after wristband does not create second session`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val scope = TestScope(dispatcher)
        val repo = FakeSosRepository().apply {
            pollQueue = listOf(SosAlert(userStatus = "DISPATCH_RECEIVED"))
        }
        val wristbandRepo = wristbandMeshRepo()
        coEvery { wristbandRepo.sendSosAck(any(), any()) } returns Result.success(Unit)

        val manager = managerWith(repo, wristbandRepo, scope = scope)

        manager.handleWristbandSOS(
            eventId = 1234L, retryCount = 0, batteryPct = 80, deviceUptimeMs = 0
        )
        advanceUntilIdle()
        val firstClientTriggerId = (manager.state.value as EmergencySOSState.Active)
            .session.clientTriggerId

        manager.startManualSOS(festivalId = "fest-1")
        advanceUntilIdle()

        assertEquals(1, repo.triggerCallCount, "manual must not create a second backend alert")
        val state = manager.state.value
        assertIs<EmergencySOSState.Active>(state)
        assertEquals(SosSource.Wristband, state.session.source)
        assertEquals(firstClientTriggerId, state.session.clientTriggerId)
    }

    @Test
    fun `wristband 0x12 cancel ends matching session`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val scope = TestScope(dispatcher)
        val repo = FakeSosRepository().apply {
            pollQueue = listOf(SosAlert(userStatus = "DISPATCH_RECEIVED"))
        }
        val wristbandRepo = wristbandMeshRepo()
        coEvery { wristbandRepo.sendSosAck(any(), any()) } returns Result.success(Unit)

        val manager = managerWith(repo, wristbandRepo, scope = scope)

        manager.handleWristbandSOS(
            eventId = 555L, retryCount = 0, batteryPct = 50, deviceUptimeMs = 0
        )
        advanceUntilIdle()

        manager.handleWristbandCancel(eventId = 555L)
        advanceUntilIdle()

        assertIs<EmergencySOSState.Cancelled>(manager.state.value)
    }

    @Test
    fun `wristband cancel for unrelated event_id is ignored`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val scope = TestScope(dispatcher)
        val repo = FakeSosRepository().apply {
            pollQueue = listOf(SosAlert(userStatus = "DISPATCH_RECEIVED"))
        }
        val wristbandRepo = wristbandMeshRepo()
        coEvery { wristbandRepo.sendSosAck(any(), any()) } returns Result.success(Unit)

        val manager = managerWith(repo, wristbandRepo, scope = scope)

        manager.handleWristbandSOS(
            eventId = 100L, retryCount = 0, batteryPct = 50, deviceUptimeMs = 0
        )
        advanceUntilIdle()

        manager.handleWristbandCancel(eventId = 999L)  // unrelated
        advanceUntilIdle()

        assertIs<EmergencySOSState.Active>(manager.state.value)
    }

    @Test
    fun `pinch-ingest failure surfaces Failed state with error message`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val scope = TestScope(dispatcher)
        val repo = FakeSosRepository().apply {
            triggerResult = Result.failure(IllegalStateException("boom"))
        }
        val manager = managerWith(repo, scope = scope)

        manager.startManualSOS(festivalId = "fest-1")
        advanceUntilIdle()

        val state = manager.state.value
        assertIs<EmergencySOSState.Failed>(state)
        assert(state.message.contains("boom")) { "Error message must propagate: ${state.message}" }
    }

    @Test
    fun `terminal RESOLVED first poll emits Resolved state`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val scope = TestScope(dispatcher)
        val repo = FakeSosRepository().apply {
            pollQueue = listOf(SosAlert(userStatus = "RESOLVED"))
        }
        val manager = managerWith(repo, scope = scope)

        manager.startManualSOS(festivalId = "fest-1")
        advanceUntilIdle()

        val state = manager.state.value
        assertIs<EmergencySOSState.Resolved>(state)
        assertEquals(SosUserStatus.Resolved, state.terminalStatus)
    }

    @Test
    fun `each emergency gets a fresh client_trigger_id`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val scope = TestScope(dispatcher)
        val repo = FakeSosRepository().apply {
            pollQueue = listOf(SosAlert(userStatus = "RESOLVED"))
        }
        val wristbandRepo = wristbandMeshRepo()
        coEvery { wristbandRepo.sendSosAck(any(), any()) } returns Result.success(Unit)

        val manager = managerWith(repo, wristbandRepo, scope = scope)

        manager.startManualSOS(festivalId = "fest-1")
        advanceUntilIdle()
        val firstId = (manager.state.value as EmergencySOSState.Resolved)
            .session.clientTriggerId

        manager.startManualSOS(festivalId = "fest-1")
        advanceUntilIdle()
        val secondId = (manager.state.value as EmergencySOSState.Resolved)
            .session.clientTriggerId

        assertNotEquals(firstId, secondId, "each emergency must have its own client_trigger_id")
        assertEquals(2, repo.triggerCallCount)
    }
}
