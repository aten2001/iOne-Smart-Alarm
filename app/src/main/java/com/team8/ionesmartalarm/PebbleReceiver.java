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
            final String jsonData = intent.getStringExtra(Constants.MSG_DATA);
            if (jsonData == null || jsonData.isEmpty()) {
                return;
            }

            try {
                final PebbleDictionary data = PebbleDictionary.fromJson(jsonData);
                Log.d("PebbleReceiver", "received: " + jsonData);
                // do what you need with the data
                if (data.getBytes(5)[0] == 1) {
                    // TODO: Start the getup alarm
                }
                PebbleKit.sendAckToPebble(context, transactionId);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }
}
