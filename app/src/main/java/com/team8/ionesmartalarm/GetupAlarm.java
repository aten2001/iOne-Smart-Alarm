package com.team8.ionesmartalarm;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.support.v4.app.NotificationCompat;
import android.text.format.Time;
import android.util.Log;
import android.widget.Toast;

import java.text.SimpleDateFormat;

/**
 * @author Team 8
 *
 * GetupAlarm class to calculate and handle getup alarms
 */
public class GetupAlarm extends IntentService implements AlarmPrototype {

    private int firstScheduleTime, duration, temperature;
    private Weather weather;
    private String weatherDescription = null;
    private boolean isFirstSet;
    private final Long repeatingAlarmCheck = 600000l;
    private final Long earlyCheckDiff = 1800000l;

    public GetupAlarm() {
        super("GetupAlarm");

        this.firstScheduleTime = this.duration = -1;
        this.temperature = -1;
        this.weather = null;
        this.weatherDescription = null;
    }

    protected void onHandleIntent(Intent intent){
        Log.d("WakeupAlarm", "The intent service was called");
        isFirstSet = intent.getBooleanExtra("isFirstGetupAlarmSet", false);
        calculateAlarmTime(this);
    }

    public void wakeupProcedure(Context context) {
        DataLoader info = new DataLoader();
        Log.i("GetupAlarm", "Getup alarm has triggered.");
        Intent newActivity = new Intent(this, FinalAlarmScreen.class);
        newActivity.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        newActivity.putExtra("weatherInfo", this.weatherDescription);
        newActivity.putExtra("tempInfo", this.temperature);
        newActivity.putExtra("eventDescr", info.getFirstScheduleDescription(this));
        newActivity.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        this.startActivity(newActivity);
    }

    public void calculateAlarmTime(Context context) {
        DataLoader dataLoader = new DataLoader();
        Log.d("GetupAlarm", "first location: " + dataLoader.getFirstScheduleLocation(context)); // TEST
        Log.d("GetupAlarm", "first time: " + dataLoader.getFirstScheduleTime(context)); // TEST
        long firstTime = dataLoader.getFirstScheduleTime(context);
        if (firstTime == -1) {
            Time time = new Time();
            time.setToNow();
            time.set(0, 0, 8, time.monthDay, time.month, time.year);
            firstScheduleTime = (int) (time.toMillis(false) / 1000);
        } else firstScheduleTime = (int) (firstTime / 1000);
        dataLoader.getLastLocation(context, this);
    }

    public void onLocationTaskCompleted(Context context, Location location) {
        if (location == null) {
            Toast.makeText(getApplicationContext(), "Failed to get the current location", Toast.LENGTH_LONG).show();
            return;
        }

        Log.d("GetupAlarm", "latitude: " + location.getLatitude() + ", longitude: " + location.getLongitude()); // TEST
        DataLoader dataLoader = new DataLoader();
        dataLoader.getTrafficTime(context, location, this);
        Time time = new Time();
        time.setToNow();
        dataLoader.getWeather(location, time.toMillis(false), this);
    }

    public void onMapTaskCompleted(int duration) {
        if (duration == -1) {
            Toast.makeText(getApplicationContext(), "Failed to get the traffic information", Toast.LENGTH_LONG).show();
            return;
        }

        Log.d("GetupAlarm", "duration: " + duration); // TEST
        this.duration = duration;
        if (temperature != -1) {
            onAllTasksCompleted();
        }
    }

    public void onWeatherTaskCompleted(int temperature, int code, String description) {
        if (temperature == -1) {
            Toast.makeText(getApplicationContext(), "Failed to get the temperature information", Toast.LENGTH_LONG).show();
            return;
        } else if (code == -1 || description == null) {
            Toast.makeText(getApplicationContext(), "Failed to get the weather information", Toast.LENGTH_LONG).show();
            return;
        }

        Log.d("GetupAlarm", "temperature: " + temperature + ", code: " + code + ", weather: " + Weather.getWeather(code).name() + ", description: " + description); // TEST
        this.temperature = temperature;
        this.weather = Weather.getWeather(code);
        this.weatherDescription = description;
        if (duration != -1) {
            onAllTasksCompleted();
        }
    }

    public void onAllTasksCompleted() {
        long time = firstScheduleTime - duration - weather.weight - ((temperature <= 32 || temperature >= 95) ? 600 : 0);
        SimpleDateFormat sdf = new SimpleDateFormat("MMMM d, yyyy 'at' h:mm a"); // TEST
        Log.d("GetupAlarm", "final getup alarm time: " + time + " = " + sdf.format(time*1000)); // TEST

        time = time*1000;
        if(time < System.currentTimeMillis()){
            this.wakeupProcedure(this);
        }
        else{
            Long nextWakeTime;
            if(isFirstSet) {
                nextWakeTime = time - earlyCheckDiff;

                sendPushNotification(sdf.format(time));
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
            MainActivity.smartAlarm.setGetupAlarm(this, nextWakeTime);
            Intent newBroadIntent = new Intent("SET_NEXT_ALARM_UI");
            newBroadIntent.putExtra("time", sdf.format(time));
            newBroadIntent.putExtra("weatherInfo", this.weatherDescription);
            newBroadIntent.putExtra("eventDescr", new DataLoader().getFirstScheduleDescription(this));
            this.getApplicationContext().sendBroadcast(newBroadIntent);

            //Send the wakeup time to the pebble
//            PebbleController pebble = new PebbleController();
//            pebble.startAlarmApp(this);
//            pebble.sendAlarmInfoToWatch(this, time, time);
        }
    }

    public void sendPushNotification(String alarmTime){
        //Send a notification
        PendingIntent notPending = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Builder notification = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_plusone_small_off_client)
                .setContentTitle("Next Wakeup Alarm")
                .setContentText(alarmTime)
                .setContentIntent(notPending);
        NotificationManager notificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(1941, notification.build());
    }
}
