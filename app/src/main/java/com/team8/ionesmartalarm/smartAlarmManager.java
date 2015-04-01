package com.team8.ionesmartalarm;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;

/**
 * @author Clayton
 *
 * Manages the set-alarm listener.
 */
public class SmartAlarmManager {

    // The time of day (in hours) that the alarm should trigger an alarm set procedure
    private static final int SET_ALARM_TIME = 01;
    private static final String IS_ACTIVE_DATA = "isSetAlarmActive";
    private static final String IS_ACTIVE_GETUP = "isGetupAlarmActive";
    private static final String HAS_WAKEUP_TRIGGERED = "wakeupTriggered";
    private static final int ALARM_SET_ID = 7009;
    private static final int GETUP_ALARM_ID = 5009;

    private Intent intent;
    private Intent getupIntent;
    private Context context;
    private PendingIntent alarmIntent;
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

    public void cancelAlarm(){
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(alarmIntent);
        if(getupPending != null) {
            alarmManager.cancel(getupPending);

            appState.edit().putInt(IS_ACTIVE_GETUP, 0).commit();
        }
        Log.i("SmartAlarmManager", "The alarm has been canceled");

        // Write to memory
        appState.edit().putInt(IS_ACTIVE_DATA, 0).commit();
    }

    public void setGetupAlarm(Context context, Long alarmMilli){
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        // Create the getupAlarmIntent
        getupIntent = new Intent(context, GetupAlarmReceiver.class);
        getupPending = PendingIntent.getBroadcast(context, GETUP_ALARM_ID, getupIntent, 0);

        /*AlarmPrototype alarm;
        // Get the calculated wakeup time
        if(appState.getBoolean(HAS_WAKEUP_TRIGGERED, false)){
            alarm = new GetupAlarm();
        }
        else{
            alarm = new WakeupAlarm();
        }
        alarm.calculateAlarmTime(context);
        int alarmMilli = alarm.getFirstScheduledTime();
        Long alarmHour = TimeUnit.MILLISECONDS.toHours(alarmMilli);
        Long alarmMinute = TimeUnit.MILLISECONDS.toDays(alarmMilli);*/

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(alarmMilli);

        alarmManager.setExact(AlarmManager.RTC, calendar.getTimeInMillis(), getupPending);

        appState.edit().putInt(IS_ACTIVE_GETUP, 1).commit();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Log.i("SmartAlarmManger", "An alarm has been set for: " + dateFormat.format(calendar.getTime()));
    }

    public Context getContext(){
        return this.context;
    }

    public boolean isActive(){
        return appState.getInt(IS_ACTIVE_DATA, 0) == 1;
    }

    public boolean isGetupActive(){
        return appState.getInt(IS_ACTIVE_GETUP, 0) == 1;
    }

    public boolean hasWakeupAlarmTriggered(){
        return appState.getBoolean(HAS_WAKEUP_TRIGGERED, false);
    }
}
