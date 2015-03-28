package com.team8.ionesmartalarm;

import android.content.Context;
import android.os.PowerManager;
import android.util.Log;

/**
 * Created by Clayton on 3/27/2015.
 */
public class WakeupAlarm implements AlarmPrototype {

    public void wakeupProcedure(Context context){
        PowerManager powerManager = (PowerManager) context.getApplicationContext().getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wakeLock = powerManager.newWakeLock((PowerManager.SCREEN_DIM_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP), "wakeupAlarm");

        wakeLock.acquire();
        Log.i("wakeupAlarm", "The screen has woken up");
        wakeLock.release();
    }

    public int calculateAlarmTime(){

        return 0;
    }
}
