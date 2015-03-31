package com.team8.ionesmartalarm;

import android.content.Context;
import android.os.Handler;

import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;

import java.util.UUID;

public class PebbleController {

    private static final UUID ALARM_UUID = UUID.fromString("2ba1d5d1-0868-4a15-b972-eefe5e01d3a0");
    private PebbleKit.PebbleDataReceiver alarmDataReceiver = null;

    public void beginReceivingDataFromWatch(Context context) {
        final Handler handler = new Handler();
        alarmDataReceiver = new PebbleKit.PebbleDataReceiver(ALARM_UUID) {
            @Override
            public void receiveData(Context context, int transactionId, PebbleDictionary pebbleTuples) {
                PebbleKit.sendAckToPebble(context, transactionId);
                // TODO: handle received data
            }
        };
        PebbleKit.registerReceivedDataHandler(context, alarmDataReceiver);
    }

    public void stopReceivingDataFromWatch(Context context) {
        if (alarmDataReceiver != null) {
            context.unregisterReceiver(alarmDataReceiver);
            alarmDataReceiver = null;
        }
    }

    public void sendDataToWatch(Context context) {
        PebbleDictionary data = new PebbleDictionary();

        // TODO: add some stuff to the data

        PebbleKit.sendDataToPebble(context, ALARM_UUID, data);
    }

    public void startAlarmApp(Context context) {
        PebbleKit.startAppOnPebble(context, ALARM_UUID);
    }

    public void stopAlarmApp(Context context) {
        PebbleKit.closeAppOnPebble(context, ALARM_UUID);
    }
}
