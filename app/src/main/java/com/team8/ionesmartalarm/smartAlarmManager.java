package com.team8.ionesmartalarm;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import java.util.Calendar;

/**
 * @author Clayton
 *
 * Manages the set-alarm listener.
 */
public class smartAlarmManager {

    // The time of day (in hours) that the alarm should trigger an alarm set procedure
    private static final int SET_ALARM_TIME = 20;
    private static final String IS_ACTIVE_DATA = "isActive";
    private static final int ALARM_SET_ID = 7009;

    private Intent intent;
    private Context context;
    private AlarmManager alarmManager;
    private PendingIntent alarmIntent;
    private SharedPreferences appState;

    public smartAlarmManager(Context contxt){
        this.context = contxt;
        // Set the intent
        this.intent = new Intent(context, AlarmSetReceiver.class);
        appState = context.getSharedPreferences(context.getString(R.string.app_preferences), Context.MODE_PRIVATE);
    }

    public void setAlarm(){
        alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmIntent = PendingIntent.getBroadcast(context, ALARM_SET_ID, intent, 0);

        Calendar calendar = Calendar.getInstance();
        // Set the time of the alarm to 8pm
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(Calendar.HOUR_OF_DAY, SET_ALARM_TIME);

        // Tell the alarm to set and repeat
        alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), AlarmManager.INTERVAL_DAY, alarmIntent);
        Log.i("SmartAlarmManager", "The alarm has been set.");

        // Write to memory
        appState.edit().putInt(IS_ACTIVE_DATA, 1).commit();
    }

    public void cancelAlarm(){
        alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(alarmIntent);
        Log.i("SmartAlarmManager", "The alarm has been canceled");

        // Write to memory
        appState.edit().putInt(IS_ACTIVE_DATA, 0).commit();
    }

    public Context getContext(){
        return this.context;
    }

    public boolean isActive(){
        return appState.getInt(IS_ACTIVE_DATA, 0) == 1;
    }
}
