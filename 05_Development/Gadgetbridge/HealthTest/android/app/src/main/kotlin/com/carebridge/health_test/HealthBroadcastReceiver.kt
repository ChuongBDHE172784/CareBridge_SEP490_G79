package com.carebridge.health_test

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import io.flutter.plugin.common.EventChannel

/**
 * CareBridge Health Broadcast Receiver
 *
 * Receives health data broadcasts from Gadgetbridge and forwards
 * them to Flutter via EventChannel.
 *
 * Actions received:
 *   com.carebridge.health.HEART_RATE
 *   com.carebridge.health.SPO2
 *   com.carebridge.health.STRESS
 *
 * Extras per broadcast:
 *   value       (Int)    - the health metric value
 *   timestamp   (Long)   - epoch milliseconds
 *   deviceName  (String) - source device name
 */
class HealthBroadcastReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "[HealthTest]"

        // Must match CareBridgeBroadcastSender constants in Gadgetbridge
        const val ACTION_HEART_RATE = "com.carebridge.health.HEART_RATE"
        const val ACTION_SPO2       = "com.carebridge.health.SPO2"
        const val ACTION_STRESS     = "com.carebridge.health.STRESS"

        const val EXTRA_VALUE       = "value"
        const val EXTRA_TIMESTAMP   = "timestamp"
        const val EXTRA_DEVICE_NAME = "deviceName"
    }

    // EventSink to send data to Flutter — set from MainActivity
    var eventSink: EventChannel.EventSink? = null

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val value      = intent.getIntExtra(EXTRA_VALUE, -1)
        val timestamp  = intent.getLongExtra(EXTRA_TIMESTAMP, System.currentTimeMillis())
        val deviceName = intent.getStringExtra(EXTRA_DEVICE_NAME) ?: "Unknown Device"

        // Map action → type string (matches Flutter HealthDataType enum)
        val type = when (action) {
            ACTION_HEART_RATE -> "HEART_RATE"
            ACTION_SPO2       -> "SPO2"
            ACTION_STRESS     -> "STRESS"
            else -> {
                Log.w(TAG, "Unknown action: $action")
                return
            }
        }

        Log.i(TAG, "Received $type: $value from $deviceName")

        // Forward to Flutter via EventChannel
        val payload = mapOf(
            "type"       to type,
            "value"      to value,
            "timestamp"  to timestamp,
            "deviceName" to deviceName
        )

        // Must run on main thread for Flutter EventSink
        eventSink?.success(payload)
    }
}
