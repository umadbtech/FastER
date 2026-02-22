package com.faster.festival.utils

import android.net.Uri

object DeepLinkHandler {
    // Deep link parsing simplified. Magic-link auth callback handling removed in favor of OTP flow.

    fun getQueryParam(uri: Uri, name: String): String? {
        return try {
            uri.getQueryParameter(name)
        } catch (e: Exception) {
            null
        }
    }
}
