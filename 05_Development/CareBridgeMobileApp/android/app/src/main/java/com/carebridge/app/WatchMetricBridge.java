package com.carebridge.app;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.flutter.plugin.common.EventChannel;

final class WatchMetricBridge {
    static final String METHOD_CHANNEL = "com.carebridge.app/watch_metrics";
    static final String EVENT_CHANNEL = "com.carebridge.app/watch_metric_stream";

    static final String ACTION_HEART_RATE = "com.carebridge.health.HEART_RATE";
    static final String ACTION_STRESS = "com.carebridge.health.STRESS";

    private static final String GADGETBRIDGE_PACKAGE = "nodomain.freeyourgadget.gadgetbridge";
    private static final String PREFS_NAME = "carebridge_watch_metrics";
    private static final String QUEUED_EVENTS_KEY = "queued_events";

    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());
    private static EventChannel.EventSink eventSink;

    private WatchMetricBridge() {
    }

    static void setEventSink(final EventChannel.EventSink sink) {
        eventSink = sink;
    }

    static boolean openGadgetbridge(final Context context) {
        final Intent launchIntent = context.getPackageManager()
                .getLaunchIntentForPackage(GADGETBRIDGE_PACKAGE);
        if (launchIntent == null) {
            return false;
        }
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(launchIntent);
        return true;
    }

    static List<Map<String, Object>> drainQueuedEvents(final Context context) {
        final SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        final String raw = prefs.getString(QUEUED_EVENTS_KEY, "[]");
        prefs.edit().remove(QUEUED_EVENTS_KEY).apply();
        final List<Map<String, Object>> events = new ArrayList<>();
        try {
            final JSONArray array = new JSONArray(raw);
            for (int i = 0; i < array.length(); i++) {
                final JSONObject object = array.optJSONObject(i);
                if (object == null) {
                    continue;
                }
                events.add(toMap(object));
            }
        } catch (final JSONException ignored) {
            // Drop corrupt transport queue; health data is persisted only after API save.
        }
        return events;
    }

    static void handleBroadcast(final Context context, final Intent intent) {
        final Map<String, Object> payload = payloadFromIntent(intent);
        if (payload == null) {
            return;
        }
        final EventChannel.EventSink sink = eventSink;
        if (sink != null) {
            MAIN_HANDLER.post(() -> sink.success(payload));
            return;
        }
        queueEvent(context, payload);
    }

    private static Map<String, Object> payloadFromIntent(final Intent intent) {
        final String action = intent.getAction();
        final String type;
        if (ACTION_HEART_RATE.equals(action)) {
            type = "HEART_RATE";
        } else if (ACTION_STRESS.equals(action)) {
            type = "STRESS";
        } else {
            return null;
        }

        final int value = intent.getIntExtra("value", -1);
        final long timestamp = intent.getLongExtra("timestamp", System.currentTimeMillis());
        final String deviceName = intent.getStringExtra("deviceName") == null
                ? "Unknown Device"
                : intent.getStringExtra("deviceName");

        final Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", type);
        payload.put("value", value);
        payload.put("timestamp", timestamp);
        payload.put("deviceName", deviceName);
        payload.put("action", action);
        return payload;
    }

    private static void queueEvent(final Context context, final Map<String, Object> payload) {
        final SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        final String raw = prefs.getString(QUEUED_EVENTS_KEY, "[]");
        try {
            final JSONArray array = new JSONArray(raw);
            array.put(new JSONObject(payload));
            prefs.edit().putString(QUEUED_EVENTS_KEY, array.toString()).apply();
        } catch (final JSONException ignored) {
            final JSONArray array = new JSONArray();
            array.put(new JSONObject(payload));
            prefs.edit().putString(QUEUED_EVENTS_KEY, array.toString()).apply();
        }
    }

    private static Map<String, Object> toMap(final JSONObject object) {
        final Map<String, Object> map = new LinkedHashMap<>();
        map.put("type", object.optString("type", ""));
        map.put("value", object.optInt("value", -1));
        map.put("timestamp", object.optLong("timestamp", System.currentTimeMillis()));
        map.put("deviceName", object.optString("deviceName", "Unknown Device"));
        map.put("action", object.optString("action", ""));
        return map;
    }
}
