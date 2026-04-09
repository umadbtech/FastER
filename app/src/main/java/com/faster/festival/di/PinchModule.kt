package com.faster.festival.di

import android.content.Context
import com.faster.festival.data.pinch.local.AssetsReader
import com.faster.festival.data.pinch.local.FakeEmergencyApi
import com.faster.festival.data.pinch.local.FakeFeedbackApi
import com.faster.festival.data.pinch.repository.AssetsEmergencyRepository
import com.faster.festival.data.pinch.repository.AssetsFeedbackRepository
import com.faster.festival.data.pinch.repository.PinchEmergencyRepository
import com.faster.festival.data.pinch.repository.PinchFeedbackRepository

object PinchModule {

    private var appContext: Context? = null

    fun initialize(context: Context) {
        appContext = context.applicationContext
    }

    private val assetsReader: AssetsReader by lazy {
        AssetsReader(requireNotNull(appContext) { "PinchModule not initialized. Call initialize(context) first." })
    }

    private val fakeEmergencyApi: FakeEmergencyApi by lazy {
        FakeEmergencyApi(assetsReader)
    }

    private val fakeFeedbackApi: FakeFeedbackApi by lazy {
        FakeFeedbackApi(assetsReader)
    }

    val emergencyRepository: PinchEmergencyRepository by lazy {
        AssetsEmergencyRepository(fakeEmergencyApi)
    }

    val feedbackRepository: PinchFeedbackRepository by lazy {
        AssetsFeedbackRepository(fakeFeedbackApi)
    }
}
