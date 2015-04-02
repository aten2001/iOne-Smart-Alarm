package com.team8.ionesmartalarm;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.text.format.Time;
import android.util.Log;

import java.text.SimpleDateFormat;

public class WakeupAlarm extends IntentService implements AlarmPrototype  {

    private int firstScheduleTime, duration = -1;
    private double temperature = -1;
    private Weather weather = null;
    private String weatherDescription = null;
    private boolean isFirstSet;
    private final Long repeatingAlarmCheck = 600000l;
    private final Long earlyCheckDiff = 1800000l;

    public WakeupAlarm() {
        super("WakeupAlarm");

        this.firstScheduleTime = this.duration = -1;
        this.temperature = -1;
        this.weather = null;
        this.weatherDescription = null;
    }

    protected void onHandleIntent(Intent intent){
        Log.d("WakeupAlarm", "The intent service was called");
        isFirstSet = intent.getBooleanExtra("isFirstAlarmSet", false);
        calculateAlarmTime(WakeupAlarm.this);
    }

    public void wakeupProcedure(Context context) {
        //Launch the activity to allow a user to snooze
        Intent newActivity = new Intent(this, SnoozeScreen.class);
        newActivity.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        this.startActivity(newActivity);
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

    public void onWeatherTaskCompleted(int temperature, int code, String description) {
        Log.d("WakeupAlarm", "temperature: " + temperature + ", code: " + code + ", weather: " + Weather.getWeather(code).name() + ", description: " + description); // TEST
        this.temperature = temperature;
        this.weather = Weather.getWeather(code);
        this.weatherDescription = description;
        if (duration != -1) {
            onAllTasksCompleted();
        }
    }

    public void onAllTasksCompleted() {
        long time = firstScheduleTime - duration - weather.weight - ((temperature <= 273 || temperature >= 310) ? 600 : 0) - 1800;
        SimpleDateFormat sdf = new SimpleDateFormat("MMMM d, yyyy 'at' h:mm a"); // TEST
        Log.d("WakeupAlarm", "final wakeup alarm time: " + time + " = " + sdf.format(time*1000)); // TEST

        time = time*1000;
        if(time < System.currentTimeMillis()){
            this.wakeupProcedure(this);
        }
        else{
            Long nextWakeTime;
            if(isFirstSet) {
                nextWakeTime = time - earlyCheckDiff;
            }
            else{
                Long nextCycleWake = System.currentTimeMillis() + repeatingAlarmCheck;
                // Check again in ten minutes unless the calculated time occurs before that
                nextWakeTime = Math.min(nextCycleWake, time);
            }
            //Set the getup alarm
            if (MainActivity.smartAlarm == null) {
                MainActivity.smartAlarm = new SmartAlarmManager(this);
            }
            MainActivity.smartAlarm.setWakeupAlarm(this, nextWakeTime);
        }
    }
}
