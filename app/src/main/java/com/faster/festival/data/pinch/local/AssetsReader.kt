package com.faster.festival.data.pinch.local

import android.content.Context
import kotlinx.serialization.json.Json

class AssetsReader(private val context: Context) {

    @PublishedApi
    internal val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    fun readJsonString(fileName: String): String {
        return context.assets.open(fileName).bufferedReader().use { it.readText() }
    }

    inline fun <reified T> readJson(fileName: String): T {
        val raw = readJsonString(fileName)
        return json.decodeFromString<T>(raw)
    }
}
