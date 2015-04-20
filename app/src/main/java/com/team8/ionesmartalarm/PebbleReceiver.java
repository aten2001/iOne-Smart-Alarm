package com.team8.ionesmartalarm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.getpebble.android.kit.Constants;
import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;

import org.json.JSONException;

public class PebbleReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(Constants.INTENT_APP_RECEIVE)
                && (intent.getSerializableExtra(Constants.APP_UUID)).equals(PebbleController.ALARM_UUID)) {
            final int transactionId = intent.getIntExtra(Constants.TRANSACTION_ID, -1);
            PebbleKit.sendAckToPebble(context, transactionId);
            final String jsonData = intent.getStringExtra(Constants.MSG_DATA);
            if (jsonData == null || jsonData.isEmpty()) {
                return;
            }

            try {
                final PebbleDictionary data = PebbleDictionary.fromJson(jsonData);
                Log.d("PebbleReceiver", "received: " + jsonData);
                // do what you need with the data
                Long value = data.getUnsignedIntegerAsLong(5);

//                if (data.contains(99)) {
//                    context.sendBroadcast(new Intent("PEBBLE_RECEIVE_DATA"));
//                }

                if (data.contains(10)) {
                    // DEMO wakeup
                    Intent newActivity = new Intent(context, SnoozeScreen.class);
                    newActivity.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(newActivity);
                } else if (data.contains(11)) {
                    // DEMO getup
                    Intent newActivity = new Intent(context, FinalAlarmScreen.class);
                    newActivity.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(newActivity);
                } else if (value != null && value == 1L && !SmartAlarmManager.isAnyAlarmActive()) {
                    Intent newActivity = new Intent(context, FinalAlarmScreen.class);
                    newActivity.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(newActivity);
                } else if (value != null && value == 0L) {
                    context.sendBroadcast(new Intent("PEBBLE_SILENCE"));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }
}
