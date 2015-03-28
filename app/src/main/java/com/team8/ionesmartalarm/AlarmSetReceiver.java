package com.team8.ionesmartalarm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * @author Clayton
 *
 * Listens for an android alarm to start the process of gathering information neccissary to set the
 * alarm time. The intention is that every day at some time (8pm for example) the application
 * will check the user's calander and other contextual information and set a tentaive wakeup alarm.
 */
public class AlarmSetReceiver extends BroadcastReceiver {
    public AlarmSetReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i("AlarmSetReceiver", "The alarm receiver has been called.");
        // Set the tentative time that the alarm is going to be
        if(MainActivity.smartAlarm == null){
            MainActivity.smartAlarm = new smartAlarmManager(context);
        }
        // Set a new alarm
        MainActivity.smartAlarm.setGetupAlarm(context, 01, 19);
    }
}
