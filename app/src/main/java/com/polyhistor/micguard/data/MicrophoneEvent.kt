package com.polyhistor.micguard.data

data class MicrophoneEvent(
    val packageName: String,
    val appName: String,
    val timestamp: Long,
    val eventType: EventType,
    val duration: Long = 0L
) {
    enum class EventType {
        STARTED,
        STOPPED,
        ONGOING
    }
} 