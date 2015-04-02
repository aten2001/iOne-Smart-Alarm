package com.team8.ionesmartalarm;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;

import java.text.SimpleDateFormat;
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
                Log.d("PebbleController", "received: " + pebbleTuples.toJsonString());
            }
        };
        PebbleKit.registerReceivedDataHandler(context, alarmDataReceiver);

        //This is a fake comment
    }

    public void stopReceivingDataFromWatch(Context context) {
        if (alarmDataReceiver != null) {
            context.unregisterReceiver(alarmDataReceiver);
            alarmDataReceiver = null;
        }
    }

    public void sendDataToWatch(Context context, int key, byte value) {
        PebbleDictionary data = new PebbleDictionary();
        data.addUint8(key, value);
        PebbleKit.sendDataToPebble(context, ALARM_UUID, data);
    }

    public void turnOnAlarm(Context context, boolean gradual) {
        sendDataToWatch(context, gradual ? 0 : 1, (byte) 1);
    }

    public void turnOffAlarm(Context context, boolean gradual) {
        sendDataToWatch(context, gradual ? 0 : 1, (byte) 0);
    }

    public void sendCalendarInfoToWatch(Context context, String info) {
        PebbleDictionary data = new PebbleDictionary();
        data.addString(2, info);
        PebbleKit.sendDataToPebble(context, ALARM_UUID, data);
    }

    public void sendWeatherInfoToWatch(Context context, int temperature, String weather) {
        PebbleDictionary data = new PebbleDictionary();
        data.addString(3, temperature + "º, " + weather);
        PebbleKit.sendDataToPebble(context, ALARM_UUID, data);
    }

    public void sendAlarmInfoToWatch(Context context, long wakeupTime, long getupTime) {
        PebbleDictionary data = new PebbleDictionary();
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a");
        data.addString(4, sdf.format(wakeupTime) + " - " + sdf.format(getupTime));
        PebbleKit.sendDataToPebble(context, ALARM_UUID, data);
    }

    public void startAlarmApp(Context context) {
        PebbleKit.startAppOnPebble(context, ALARM_UUID);
    }

    public void stopAlarmApp(Context context) {
        PebbleKit.closeAppOnPebble(context, ALARM_UUID);
    }
}
