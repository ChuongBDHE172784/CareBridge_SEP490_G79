package com.carebridge.app;

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
}
