package com.team8.ionesmartalarm;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * @author Team 8
 *
 * Manages the set-alarm listener.
 */
public class SmartAlarmManager {

    // The time of day (in hours) that the alarm should trigger an alarm set procedure
    private static final int SET_ALARM_TIME = 00;
    private static final String IS_ACTIVE_DATA = "isSetAlarmActive";
    private static final String IS_ACTIVE_WAKEUP = "isWakeupAlarmActive";
    private static final String IS_ACTIVE_GETUP = "isGetupAlarmActive";
    private static final int ALARM_SET_ID = 7009;
    private static final int WAKEUP_ALARM_ID = 5009;
    private static final int GETUP_ALARM_ID = 2111;

    private Intent intent;
    private Intent wakeupIntent;
    private Intent getupIntent;
    private Context context;
    private PendingIntent alarmIntent;
    private PendingIntent wakeupPending;
    private PendingIntent getupPending;
    private SharedPreferences appState;

    public SmartAlarmManager(Context contxt){
        this.context = contxt;
        // Set the intent
        this.intent = new Intent(context, AlarmSetReceiver.class);
        appState = context.getSharedPreferences(context.getString(R.string.app_preferences), Context.MODE_PRIVATE);
    }

    public void setAlarm(){
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmIntent = PendingIntent.getBroadcast(context, ALARM_SET_ID, intent, 0);

        Calendar calendar = Calendar.getInstance();
        // Set the time of the alarm to 8pm
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(Calendar.HOUR_OF_DAY, SET_ALARM_TIME);

        // Tell the alarm to set and repeat
        alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), AlarmManager.INTERVAL_DAY, alarmIntent);
        Log.i("SmartAlarmManager", "The prepare alarm has been set.");

        // Write to memory
        appState.edit().putInt(IS_ACTIVE_DATA, 1).commit();
    }

    public void cancelAlarm(Context context){
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        if(wakeupPending != null) {
            alarmManager.cancel(wakeupPending);

            appState.edit().putInt(IS_ACTIVE_WAKEUP, 0).commit();
        }
        if(wakeupIntent == null && appState.getInt(IS_ACTIVE_WAKEUP, 0) == 1){
            setWakeupAlarm(context, 702954000000l);
        }
        if(getupIntent == null && appState.getInt(IS_ACTIVE_GETUP, 0) == 1){
            setGetupAlarm(context, 702954000000l);
        }

        alarmManager.cancel(alarmIntent);
        alarmManager.cancel(wakeupPending);
        alarmManager.cancel(getupPending);
        //I need to try and delete a set alarm
        Log.i("SmartAlarmManager", "The alarm has been canceled");

        // Write to memory
        appState.edit().putInt(IS_ACTIVE_DATA, 0).commit();
        appState.edit().putInt(IS_ACTIVE_WAKEUP, 0).commit();
        appState.edit().putInt(IS_ACTIVE_GETUP, 0).commit();
    }

    public void setWakeupAlarm(Context context, Long alarmMilli){
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        // Create the getupAlarmIntent
        wakeupIntent = new Intent(context, WakeupAlarmReceiver.class);
        wakeupPending = PendingIntent.getBroadcast(context, WAKEUP_ALARM_ID, wakeupIntent, 0);

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(alarmMilli);

        alarmManager.setExact(AlarmManager.RTC, calendar.getTimeInMillis(), wakeupPending);

        appState.edit().putInt(IS_ACTIVE_WAKEUP, 1).commit();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Log.i("SmartAlarmManger", "A wakeup alarm has been set for: " + dateFormat.format(calendar.getTime()));
    }

    public void setGetupAlarm(Context context, Long alarmMilli){
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        // Create the getupAlarmIntent
        getupIntent = new Intent(context, GetupAlarmReceiver.class);
        getupPending = PendingIntent.getBroadcast(context, GETUP_ALARM_ID, getupIntent, 0);

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(alarmMilli);

        alarmManager.setExact(AlarmManager.RTC, calendar.getTimeInMillis(), getupPending);

        appState.edit().putInt(IS_ACTIVE_GETUP, 1).commit();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Log.i("SmartAlarmManger", "A getup alarm has been set for: " + dateFormat.format(calendar.getTime()));
    }

    public Context getContext(){
        return this.context;
    }

    public boolean isActive(){
        return appState.getInt(IS_ACTIVE_DATA, 0) == 1;
    }

    public boolean isWakeupActive(){
        return appState.getInt(IS_ACTIVE_WAKEUP, 0) == 1;
    }
}
