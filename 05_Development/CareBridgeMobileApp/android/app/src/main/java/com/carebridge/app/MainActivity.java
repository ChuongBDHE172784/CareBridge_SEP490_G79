package com.carebridge.app;

import android.os.Build;

import io.flutter.embedding.android.FlutterActivity;
import io.flutter.embedding.engine.FlutterEngine;
import io.flutter.plugin.common.EventChannel;
import io.flutter.plugin.common.MethodChannel;

public class MainActivity extends FlutterActivity {
    @Override
    public void configureFlutterEngine(final FlutterEngine flutterEngine) {
        super.configureFlutterEngine(flutterEngine);

        new MethodChannel(
                flutterEngine.getDartExecutor().getBinaryMessenger(),
                WatchMetricBridge.METHOD_CHANNEL
        ).setMethodCallHandler((call, result) -> {
            if ("openGadgetbridge".equals(call.method)) {
                result.success(WatchMetricBridge.openGadgetbridge(this));
                return;
            }
            if ("drainQueuedEvents".equals(call.method)) {
                result.success(WatchMetricBridge.drainQueuedEvents(this));
                return;
            }
            result.notImplemented();
        });

        new MethodChannel(
                flutterEngine.getDartExecutor().getBinaryMessenger(),
                "com.carebridge.app/device"
        ).setMethodCallHandler((call, result) -> {
            if ("isEmulator".equals(call.method)) {
                result.success(isEmulator());
                return;
            }
            result.notImplemented();
        });

        new EventChannel(
                flutterEngine.getDartExecutor().getBinaryMessenger(),
                WatchMetricBridge.EVENT_CHANNEL
        ).setStreamHandler(new EventChannel.StreamHandler() {
            @Override
            public void onListen(final Object arguments, final EventChannel.EventSink events) {
                WatchMetricBridge.setEventSink(events);
            }

            @Override
            public void onCancel(final Object arguments) {
                WatchMetricBridge.setEventSink(null);
            }
        });
    }

    private static boolean isEmulator() {
        return "ranchu".equals(Build.HARDWARE)
                || "goldfish".equals(Build.HARDWARE)
                || Build.PRODUCT.startsWith("sdk_gphone")
                || Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.contains("emulator");
    }
}
