package com.faster.festival.ui.viewmodel

import com.faster.festival.data.models.AppHomeBundleResponse
import com.faster.festival.data.models.AppFestivalHeader
import com.faster.festival.data.models.UiConfig
import com.faster.festival.data.repository.AppHomeRepository
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * Unit tests for AppHomeViewModel
 * Tests loading, error, and retry flows
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AppHomeViewModelTest {

    @MockK
    private lateinit var appHomeRepository: AppHomeRepository

    private lateinit var viewModel: AppHomeViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxed = true)
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test initial state is Loading`() {
        // Arrange - mock empty response
        coEvery { appHomeRepository.getAppHomeBundle(any()) } returns flowOf(
            createMockBundle()
        )

        // Act
        viewModel = AppHomeViewModel(appHomeRepository, "test-slug")

        // Assert - should start loading, then transition to success
        assertEquals(UiState.Loading::class, viewModel.bundleState.value::class)
    }

    @Test
    fun `test successful bundle load updates state`() = runTest {
        // Arrange
        val mockBundle = createMockBundle()
        coEvery { appHomeRepository.getAppHomeBundle("test-slug") } returns flowOf(mockBundle)

        // Act
        viewModel = AppHomeViewModel(appHomeRepository, "test-slug")
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        val state = viewModel.bundleState.value
        assertIs<UiState.Success<*>>(state)
    }

    @Test
    fun `test bundle load error updates error state`() = runTest {
        // Arrange
        val exception = Exception("Network error")
        coEvery { appHomeRepository.getAppHomeBundle(any()) } throws exception

        // Act
        viewModel = AppHomeViewModel(appHomeRepository, "test-slug")
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        val state = viewModel.bundleState.value
        assertIs<UiState.Error>(state)
        assertEquals("Network error", (state as UiState.Error).message)
    }

    @Test
    fun `test refreshBundle clears error state`() = runTest {
        // Arrange
        val exception = Exception("Network error")
        coEvery { appHomeRepository.getAppHomeBundle(any()) } throws exception

        viewModel = AppHomeViewModel(appHomeRepository, "test-slug")
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify initial error state
        assertIs<UiState.Error>(viewModel.bundleState.value)

        // Act - refresh
        coEvery { appHomeRepository.getAppHomeBundle(any()) } returns flowOf(createMockBundle())
        viewModel.refreshBundle()
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert - should now be success (error cleared)
        assertIs<UiState.Success<*>>(viewModel.bundleState.value)
    }

    // Helper to create mock bundle
    private fun createMockBundle() = AppHomeBundleResponse(
        schemaVersion = "1",
        generatedAt = "2026-03-07T00:00:00Z",
        festival = AppFestivalHeader(
            id = "test-id",
            slug = "test-slug",
            name = "Test Festival",
            timezone = "America/New_York",
            startsAt = "2026-07-22T16:00:00+00:00",
            endsAt = "2026-07-27T03:00:00+00:00",
            logoUrl = null,
            bannerUrl = null,
            bannerUrls = emptyList(),
            accentColorHex = null,
            contextState = "PRE"
        ),
        modules = emptyList(),
        uiConfig = UiConfig()
    )
}
