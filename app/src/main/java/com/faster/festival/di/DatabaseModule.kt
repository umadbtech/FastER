package com.faster.festival.di

import android.content.Context
import com.faster.festival.data.local.db.AppDatabase
import com.faster.festival.data.repository.local.SosHistoryRepository
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

    val wristbandRepository: WristbandRepository by lazy {
        WristbandRepository(database.wristbandDao())
    }

    val sosHistoryRepository: SosHistoryRepository by lazy {
        SosHistoryRepository(database.sosHistoryDao())
    }
}
