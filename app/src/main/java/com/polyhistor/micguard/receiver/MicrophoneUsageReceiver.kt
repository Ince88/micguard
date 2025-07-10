package com.polyhistor.micguard.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.util.Log
import com.polyhistor.micguard.data.MicrophoneEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import androidx.core.content.ContextCompat

class MicrophoneUsageReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "MicrophoneUsageReceiver"
        private const val ACTION_MICROPHONE_USAGE = "com.polyhistor.micguard.MICROPHONE_USAGE"
        
        private val _microphoneEvents = MutableStateFlow<List<MicrophoneEvent>>(emptyList())
        val microphoneEvents: StateFlow<List<MicrophoneEvent>> = _microphoneEvents
        
        fun register(context: Context): MicrophoneUsageReceiver {
            val receiver = MicrophoneUsageReceiver()
            val filter = IntentFilter(ACTION_MICROPHONE_USAGE)
            ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
            return receiver
        }
    }
    
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == ACTION_MICROPHONE_USAGE) {
            val timestamp = intent.getLongExtra("timestamp", System.currentTimeMillis())
            
            val event = MicrophoneEvent(
                packageName = "unknown",
                appName = "Unknown App",
                timestamp = timestamp,
                eventType = MicrophoneEvent.EventType.STARTED
            )
            
            addMicrophoneEvent(event)
            
            Log.d(TAG, "Microphone usage detected")
        }
    }
    
    private fun addMicrophoneEvent(event: MicrophoneEvent) {
        val currentEvents = _microphoneEvents.value.toMutableList()
        currentEvents.add(event)
        
        // Keep only last 50 events
        if (currentEvents.size > 50) {
            currentEvents.removeAt(0)
        }
        
        _microphoneEvents.value = currentEvents
    }
} 