package com.team8.ionesmartalarm;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;

import java.text.SimpleDateFormat;
import java.util.UUID;

/**
 * @author Team 8
 *
 * Responsible for sending and receiving data between the Pebble watch and the Android app
 */
public class PebbleController {

    public static final UUID ALARM_UUID = UUID.fromString("2ba1d5d1-0868-4a15-b972-eefe5e01d3a0");
    private PebbleKit.PebbleDataReceiver alarmDataReceiver = null;

    /**
     * Begin receiving data from a pebble watch
     *
     * @param context current context
     */
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
    }

    /**
     * Stop receiving data from a pebble watch
     *
     * @param context current context
     */
    public void stopReceivingDataFromWatch(Context context) {
        if (alarmDataReceiver != null) {
            context.unregisterReceiver(alarmDataReceiver);
            alarmDataReceiver = null;
        }
    }

    /**
     * Send a byte value to a pebble watch
     *
     * @param context current context
     * @param key key for the data
     * @param value value of the data
     */
    public void sendDataToWatch(Context context, int key, byte value, String calendarInfo, String tempAndWeather) {
        PebbleDictionary data = new PebbleDictionary();
        data.addUint8(key, value);
        if (value == 1) {
            data.addString(2, calendarInfo);
            data.addString(3, tempAndWeather);
        }
        PebbleKit.sendDataToPebble(context, ALARM_UUID, data);
    }

    /**
     * Start the alarm on a pebble watch
     *
     * @param context current context
     * @param gradual indicates if the alarm should be gradual or not
     */
    public void turnOnAlarm(Context context, boolean gradual, String calendarInfo, String tempAndWeather) {
        sendDataToWatch(context, gradual ? 0 : 1, (byte) 1, calendarInfo, tempAndWeather);
    }

    /**
     * Stop the alarm on a pebble watch
     *
     * @param context current context
     * @param gradual indicates if the alarm was gradual or not
     */
    public void turnOffAlarm(Context context, boolean gradual) {
        sendDataToWatch(context, gradual ? 0 : 1, (byte) 0, "", "");
    }

    /**
     * Send calendar information to a pebble watch
     *
     * @param context current context
     * @param info description of the schedule
     */
    public void sendCalendarInfoToWatch(Context context, String info) {
        PebbleDictionary data = new PebbleDictionary();
        data.addString(2, info);
        PebbleKit.sendDataToPebble(context, ALARM_UUID, data);
    }

    /**
     * Send weather information to a pebble watch
     *
     * @param context current context
     * @param temperature temperature
     * @param weather a human-readable weather description
     */
    public void sendWeatherInfoToWatch(Context context, int temperature, String weather) {
        PebbleDictionary data = new PebbleDictionary();
        data.addString(3, temperature + "º, " + weather);
        PebbleKit.sendDataToPebble(context, ALARM_UUID, data);
    }

    /**
     * Send alarm time information to a pebble watch
     *
     * @param context current context
     * @param wakeupTime wakeup time
     * @param getupTime getup time
     */
    public void sendAlarmInfoToWatch(Context context, long wakeupTime, long getupTime) {
        PebbleDictionary data = new PebbleDictionary();
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a");
        data.addString(4, sdf.format(wakeupTime) + " - " + sdf.format(getupTime));
        PebbleKit.sendDataToPebble(context, ALARM_UUID, data);
    }

    public void checkAlarmApp(Context context) {
        PebbleDictionary data = new PebbleDictionary();
        data.addUint8(99, (byte) 1);
        PebbleKit.sendDataToPebble(context, ALARM_UUID, data);
    }

    /**
     * Start the alarm app on a pebble watch
     *
     * @param context current context
     */
    public void startAlarmApp(Context context) {
        PebbleKit.startAppOnPebble(context, ALARM_UUID);
    }

    /**
     * Stop the alarm app on a pebble watch
     *
     * @param context current context
     */
    public void stopAlarmApp(Context context) {
        PebbleKit.closeAppOnPebble(context, ALARM_UUID);
    }
}
