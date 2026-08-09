package com.carebridge.health_test

import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.EventChannel

/**
 * MainActivity — sets up the EventChannel bridge between Android native
 * (HealthBroadcastReceiver) and Flutter Dart code.
 *
 * Architecture:
 *   Gadgetbridge Broadcast → HealthBroadcastReceiver → EventChannel → Flutter
 */
class MainActivity : FlutterActivity() {

    companion object {
        private const val TAG = "[HealthTest]"
        private const val HEALTH_EVENT_CHANNEL = "com.carebridge.healthtest/health_stream"
    }

    private val healthReceiver = HealthBroadcastReceiver()

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)

        // Set up EventChannel for streaming health data to Flutter
        EventChannel(
            flutterEngine.dartExecutor.binaryMessenger,
            HEALTH_EVENT_CHANNEL
        ).setStreamHandler(object : EventChannel.StreamHandler {

            override fun onListen(arguments: Any?, events: EventChannel.EventSink) {
                Log.i(TAG, "EventChannel: Flutter is listening for health data")
                healthReceiver.eventSink = events
            }

            override fun onCancel(arguments: Any?) {
                Log.i(TAG, "EventChannel: Flutter stopped listening")
                healthReceiver.eventSink = null
            }
        })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        registerHealthReceiver()
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterHealthReceiver()
    }

    private fun registerHealthReceiver() {
        val filter = IntentFilter().apply {
            addAction(HealthBroadcastReceiver.ACTION_HEART_RATE)
            addAction(HealthBroadcastReceiver.ACTION_SPO2)
            addAction(HealthBroadcastReceiver.ACTION_STRESS)
        }
        // Android 13+ (API 33) requires explicit exported flag for dynamic receivers
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(healthReceiver, filter, RECEIVER_EXPORTED)
        } else {
            registerReceiver(healthReceiver, filter)
        }
        Log.i(TAG, "HealthBroadcastReceiver registered for HEART_RATE, SPO2, STRESS")
    }

    private fun unregisterHealthReceiver() {
        try {
            unregisterReceiver(healthReceiver)
            Log.i(TAG, "HealthBroadcastReceiver unregistered")
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "Receiver was not registered: ${e.message}")
        }
    }
}
