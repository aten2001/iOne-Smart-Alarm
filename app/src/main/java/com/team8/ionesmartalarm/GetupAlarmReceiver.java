package com.team8.ionesmartalarm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class GetupAlarmReceiver extends BroadcastReceiver {
    public GetupAlarmReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i("GetupAlarmReceiver", "The getup alarm is called");

        // Set a new alarm through wakeup
        Intent newService = new Intent(context, GetupAlarm.class);
        context.startService(newService);
    }
}
