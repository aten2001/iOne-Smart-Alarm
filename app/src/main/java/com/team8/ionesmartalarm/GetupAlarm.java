package com.team8.ionesmartalarm;

import android.content.Context;
import android.os.PowerManager;
import android.text.format.Time;
import android.util.Log;

public class GetupAlarm implements AlarmPrototype {

    public void wakeupProcedure(Context context) {
        PowerManager powerManager = (PowerManager) context.getApplicationContext().getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wakeLock = powerManager.newWakeLock((PowerManager.SCREEN_DIM_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP), "wakeupAlarm");

        wakeLock.acquire();
        Log.i("getupAlarm", "The screen has woken up");
        wakeLock.release();
    }

    public int calculateAlarmTime(Context context) {
        DataLoader dataLoader = new DataLoader();
        Log.d("WakeupAlarm", "first location: " + dataLoader.getFirstScheduleLocation(context)); // TEST
        Log.d("WakeupAlarm", "first time: " + dataLoader.getFirstScheduleTime(context)); // TEST
        Log.d("WakeupAlarm", "latitude: " + dataLoader.getLastLocation(context).getLatitude() + ", longitude: " + dataLoader.getLastLocation(context).getLongitude()); // TEST
        dataLoader.getTrafficTime(context, this);
        Time time = new Time();
        time.setToNow();
        dataLoader.getWeather(context, time.toMillis(false), this);
        Log.d("WakeupAlarm", "time: " + time.toMillis(false)); // TEST
        // return firstScheduleTime (or default time if unavailable) - duration - (weatherCode == bad ? 10 : 0) - (temperature == extreme ? 10 : 0)
        return 0;
    }

    public void onMapTaskCompleted(int duration) {
        Log.d("WakeupAlarm", "duration: " + duration); // TEST
    }

    public void onWeatherTaskCompleted(int temperature, int code) {
        Log.d("WakeupAlarm", "temperature: " + temperature + ", code: " + code + ", weather: " + Weather.getWeather(code).name()); // TEST
    }
}
