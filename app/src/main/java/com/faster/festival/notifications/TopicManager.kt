package com.faster.festival.notifications

import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging

object TopicManager {

    private const val TAG = "TopicManager"

    const val TOPIC_EMERGENCY = "emergency_alerts"
    const val TOPIC_FESTIVAL = "festival_updates"
    const val TOPIC_PROMOTIONS = "exclusive_promotions"

    fun subscribe(topic: String) {
        FirebaseMessaging.getInstance().subscribeToTopic(topic)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "Subscribed to topic: $topic")
                } else {
                    Log.e(TAG, "Failed to subscribe to topic: $topic", task.exception)
                }
            }
    }

    fun unsubscribe(topic: String) {
        FirebaseMessaging.getInstance().unsubscribeFromTopic(topic)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "Unsubscribed from topic: $topic")
                } else {
                    Log.e(TAG, "Failed to unsubscribe from topic: $topic", task.exception)
                }
            }
    }

    fun unsubscribeAll() {
        unsubscribe(TOPIC_EMERGENCY)
        unsubscribe(TOPIC_FESTIVAL)
        unsubscribe(TOPIC_PROMOTIONS)
    }

    fun syncSubscriptions(
        emergencyAlerts: Boolean,
        festivalUpdates: Boolean,
        exclusivePromotions: Boolean
    ) {
        if (emergencyAlerts) subscribe(TOPIC_EMERGENCY) else unsubscribe(TOPIC_EMERGENCY)
        if (festivalUpdates) subscribe(TOPIC_FESTIVAL) else unsubscribe(TOPIC_FESTIVAL)
        if (exclusivePromotions) subscribe(TOPIC_PROMOTIONS) else unsubscribe(TOPIC_PROMOTIONS)
    }
}
