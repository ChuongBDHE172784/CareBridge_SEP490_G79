/*
 * CareBridge Health Broadcast Sender
 * Sends health metrics from Gadgetbridge to CareBridge app
 * via targeted Android Broadcast Intents.
 *
 * Supported metrics:
 *   - HEART_RATE (realtime & manual)
 *   - SPO2       (normal & sleep)
 *   - STRESS     (manual & auto)
 *
 * Extendable for: STEPS, SLEEP, CALORIES, BATTERY, TEMPERATURE
 */
package nodomain.freeyourgadget.gadgetbridge.carebridge;

import android.content.Context;
import android.content.Intent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CareBridgeBroadcastSender {

    private static final Logger LOG = LoggerFactory.getLogger(CareBridgeBroadcastSender.class);

    // ─── Target package ───────────────────────────────────────────────────────
    /** Package of the CareBridge Flutter app that receives these broadcasts. */
    public static final String TARGET_PACKAGE = "com.carebridge.app";

    // ─── Broadcast Actions ────────────────────────────────────────────────────
    public static final String ACTION_HEART_RATE = "com.carebridge.health.HEART_RATE";
    public static final String ACTION_SPO2       = "com.carebridge.health.SPO2";
    public static final String ACTION_STRESS     = "com.carebridge.health.STRESS";

    // Extendable future actions:
    // public static final String ACTION_STEPS       = "com.carebridge.health.STEPS";
    // public static final String ACTION_SLEEP       = "com.carebridge.health.SLEEP";
    // public static final String ACTION_CALORIES    = "com.carebridge.health.CALORIES";
    // public static final String ACTION_BATTERY     = "com.carebridge.health.BATTERY";
    // public static final String ACTION_TEMPERATURE = "com.carebridge.health.TEMPERATURE";

    // ─── Extras keys ──────────────────────────────────────────────────────────
    public static final String EXTRA_VALUE       = "value";
    public static final String EXTRA_TIMESTAMP   = "timestamp";
    public static final String EXTRA_DEVICE_NAME = "deviceName";

    // ─── Log tag ──────────────────────────────────────────────────────────────
    private static final String LOG_TAG = "[CareBridgeBroadcast]";

    // Utility class — not instantiable
    private CareBridgeBroadcastSender() {}

    // ─── Public API ───────────────────────────────────────────────────────────

    /**
     * Send a Heart Rate broadcast.
     *
     * @param context    Android context (use service context)
     * @param heartRate  Heart rate in BPM (must be > 0)
     * @param deviceName Friendly name of the source device (e.g. "Mi Smart Band 6")
     */
    public static void sendHeartRate(final Context context,
                                     final int heartRate,
                                     final String deviceName) {
        if (heartRate <= 0) {
            LOG.warn("{} Skipping invalid heart rate: {}", LOG_TAG, heartRate);
            return;
        }
        LOG.info("{} Sending HEART_RATE: {}", LOG_TAG, heartRate);
        send(context, ACTION_HEART_RATE, heartRate, deviceName);
    }

    public static void sendHeartRate(final Context context,
                                     final int heartRate,
                                     final String deviceName,
                                     final long timestampMillis) {
        if (heartRate <= 0) {
            LOG.warn("{} Skipping invalid heart rate: {}", LOG_TAG, heartRate);
            return;
        }
        LOG.info("{} Sending HEART_RATE: {}", LOG_TAG, heartRate);
        send(context, ACTION_HEART_RATE, heartRate, deviceName, timestampMillis);
    }

    /**
     * Send a SpO₂ broadcast.
     *
     * @param context    Android context
     * @param spo2       SpO₂ percentage (must be > 0)
     * @param deviceName Friendly name of the source device
     */
    public static void sendSpo2(final Context context,
                                final int spo2,
                                final String deviceName) {
        if (spo2 <= 0) {
            LOG.warn("{} Skipping invalid SpO2: {}", LOG_TAG, spo2);
            return;
        }
        LOG.info("{} Sending SPO2: {}", LOG_TAG, spo2);
        send(context, ACTION_SPO2, spo2, deviceName);
    }

    /**
     * Send a Stress broadcast.
     *
     * Scale: 0-39 = Relaxed, 40-59 = Mild, 60-79 = Moderate, 80-100 = High
     *
     * @param context    Android context
     * @param stress     Stress value 0-100
     * @param deviceName Friendly name of the source device
     */
    public static void sendStress(final Context context,
                                  final int stress,
                                  final String deviceName) {
        if (stress < 0) {
            LOG.warn("{} Skipping invalid stress: {}", LOG_TAG, stress);
            return;
        }
        LOG.info("{} Sending STRESS: {}", LOG_TAG, stress);
        send(context, ACTION_STRESS, stress, deviceName);
    }

    public static void sendStress(final Context context,
                                  final int stress,
                                  final String deviceName,
                                  final long timestampMillis) {
        if (stress < 0) {
            LOG.warn("{} Skipping invalid stress: {}", LOG_TAG, stress);
            return;
        }
        LOG.info("{} Sending STRESS: {}", LOG_TAG, stress);
        send(context, ACTION_STRESS, stress, deviceName, timestampMillis);
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    /**
     * Build and send a targeted broadcast to the Health Test app.
     * Using setPackage() restricts the broadcast to only the target app,
     * preventing other apps from intercepting health data.
     */
    private static void send(final Context context,
                             final String action,
                             final int value,
                             final String deviceName) {
        send(context, action, value, deviceName, System.currentTimeMillis());
    }

    private static void send(final Context context,
                             final String action,
                             final int value,
                             final String deviceName,
                             final long timestampMillis) {
        try {
            final Intent intent = new Intent(action);
            intent.setPackage(TARGET_PACKAGE);
            intent.putExtra(EXTRA_VALUE, value);
            intent.putExtra(EXTRA_TIMESTAMP, timestampMillis > 0 ? timestampMillis : System.currentTimeMillis());
            intent.putExtra(EXTRA_DEVICE_NAME, deviceName != null ? deviceName : "Unknown Device");
            context.sendBroadcast(intent);
        } catch (final Exception e) {
            LOG.error("{} Failed to send broadcast for action {}: {}", LOG_TAG, action, e.getMessage());
        }
    }
}
