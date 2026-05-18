package com.faster.festival.di

import android.content.Context
import com.faster.festival.data.local.db.AppDatabase
import com.faster.festival.data.repository.local.SosDeviceRepository
import com.faster.festival.data.repository.local.SosHistoryRepository
import com.faster.festival.data.repository.local.TelemetryQueueRepository
import com.faster.festival.data.repository.local.WristbandRepository

/**
 * Manual DI holder for Room + local repositories.
 * Initialized once from MainActivity.onCreate via [initialize].
 */
object DatabaseModule {

    private var appContext: Context? = null

    fun initialize(context: Context) {
        appContext = context.applicationContext
    }

    private val ctx: Context
        get() = requireNotNull(appContext) {
            "DatabaseModule not initialized. Call initialize(context) first."
        }

    val database: AppDatabase by lazy { AppDatabase.getInstance(ctx) }

    /**
     * [WristbandRepository] gets the Project 1 API service lazily so a NetworkModule
     * initialization failure (e.g. missing apikey on a misbuilt apk) does not
     * cascade into Room — local pairing state still works, backend sync is just
     * skipped (the repository tolerates a null [Project1ApiService]).
     */
    val wristbandRepository: WristbandRepository by lazy {
        val remote = runCatching { NetworkModule.project1ApiService }.getOrNull()
        WristbandRepository(database.wristbandDao(), remote)
    }

    val sosHistoryRepository: SosHistoryRepository by lazy {
        SosHistoryRepository(database.sosHistoryDao())
    }

    val sosDeviceRepository: SosDeviceRepository by lazy {
        SosDeviceRepository(database.sosDeviceDao())
    }

    /**
     * Durable queue between the BLE 0x10 collector and the
     * [com.faster.festival.core.telemetry.TelemetryUploadWorker]. Project 1 only —
     * see [TelemetryQueueRepository] kdoc.
     */
    val telemetryQueueRepository: TelemetryQueueRepository by lazy {
        TelemetryQueueRepository(database.pendingTelemetryDao())
    }
}
