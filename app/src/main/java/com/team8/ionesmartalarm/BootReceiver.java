package com.team8.ionesmartalarm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * @author Clayton
 *
 * Listens for phone reboot and re-enables the set alarm listener (AlarmSetReceiver).
 */
public class BootReceiver extends BroadcastReceiver {
    public BootReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if(intent.getAction().equals("android.intent.action.BOOT_COMPLETED")){
            if(MainActivity.smartAlarm == null){
                MainActivity.smartAlarm = new SmartAlarmManager(context);
            }
            // Set a new alarm
            MainActivity.smartAlarm.setAlarm();
        }
    }
}
