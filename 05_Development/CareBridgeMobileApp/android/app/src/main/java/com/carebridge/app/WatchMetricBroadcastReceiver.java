package com.carebridge.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class WatchMetricBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(final Context context, final Intent intent) {
        WatchMetricBridge.handleBroadcast(context, intent);
    }
}
