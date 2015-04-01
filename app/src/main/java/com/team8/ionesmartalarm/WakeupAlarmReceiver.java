package com.team8.ionesmartalarm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.text.format.Time;
import android.util.Log;

public class WakeupAlarmReceiver extends BroadcastReceiver {
    public WakeupAlarmReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i("WakeupAlarmReceiver", "The wakeup alarm is called");

        // Set a new alarm through wakeup
        Intent newActivity = new Intent(context, WakeupAlarm.class);
        newActivity.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(newActivity);
    }
}
