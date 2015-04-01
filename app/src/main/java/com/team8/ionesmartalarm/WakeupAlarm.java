package com.team8.ionesmartalarm;

import android.content.Context;
import android.location.Location;
import android.os.PowerManager;
import android.text.format.Time;
import android.util.Log;

import java.text.SimpleDateFormat;

public class WakeupAlarm implements AlarmPrototype {

    private int firstScheduleTime, duration;
    private double temperature;
    private Weather weather;

    public WakeupAlarm() {
        this.firstScheduleTime = this.duration = -1;
        this.temperature = -1;
        this.weather = null;
    }

    public void wakeupProcedure(Context context) {
        PowerManager powerManager = (PowerManager) context.getApplicationContext().getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wakeLock = powerManager.newWakeLock((PowerManager.SCREEN_DIM_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP), "wakeupAlarm");

        wakeLock.acquire();
        Log.i("wakeupAlarm", "The screen has woken up");
        wakeLock.release();
    }

    public void calculateAlarmTime(Context context) {
        DataLoader dataLoader = new DataLoader();
        Log.d("WakeupAlarm", "first location: " + dataLoader.getFirstScheduleLocation(context)); // TEST
        Log.d("WakeupAlarm", "first time: " + dataLoader.getFirstScheduleTime(context)); // TEST
        firstScheduleTime = (int) (dataLoader.getFirstScheduleTime(context) / 1000);
        dataLoader.getLastLocation(context, this);
    }

    public void onLocationTaskCompleted(Context context, Location location) {
        Log.d("WakeupAlarm", "latitude: " + location.getLatitude() + ", longitude: " + location.getLongitude()); // TEST
        DataLoader dataLoader = new DataLoader();
        dataLoader.getTrafficTime(context, location, this);
        Time time = new Time();
        time.setToNow();
        dataLoader.getWeather(location, time.toMillis(false), this);
    }

    public void onMapTaskCompleted(int duration) {
        Log.d("WakeupAlarm", "duration: " + duration); // TEST
        this.duration = duration;
        if (temperature != -1) {
            onAllTasksCompleted();
        }
    }

    public void onWeatherTaskCompleted(int temperature, int code) {
        Log.d("WakeupAlarm", "temperature: " + temperature + ", code: " + code + ", weather: " + Weather.getWeather(code).name()); // TEST
        this.temperature = temperature;
        this.weather = Weather.getWeather(code);
        if (duration != -1) {
            onAllTasksCompleted();
        }
    }

    public void onAllTasksCompleted() {
        long time = firstScheduleTime - duration - weather.weight - ((temperature <= 273 || temperature >= 310) ? 600 : 0) - 1800;
        SimpleDateFormat sdf = new SimpleDateFormat("MMMM d, yyyy 'at' h:mm a"); // TEST
        Log.d("WakeupAlarm", "final wakeup alarm time: " + time + " = " + sdf.format(time*1000)); // TEST
    }
}
