package com.team8.ionesmartalarm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.PowerManager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;

/**
 * @author Team 8
 *
 *
 */
public class FinalAlarmScreen extends ActionBarActivity {

    private  PowerManager.WakeLock wakeLock;
    private PebbleController pebble;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_final_alarm_screen);

        PowerManager powerManager = (PowerManager) this.getApplicationContext().getSystemService(Context.POWER_SERVICE);
        wakeLock = powerManager.newWakeLock((PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP), "getupAlarm");
        wakeLock.acquire();

        //get past lock screen
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

        Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        if(alarmSound == null){
            alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
        }
        Ringtone ringtone = RingtoneManager.getRingtone(this.getApplicationContext(), alarmSound);
        ringtone.play();

        //Get the info for the pebble
        Bundle passedData = this.getIntent().getExtras();
        String weather = passedData.getString("weatherInfo", "");
        Double temp = passedData.getDouble("tempInfo", 0);
        String eventDescr = passedData.getString("eventDescr", "");

        pebble = new PebbleController();
        startPebbleGetup(weather, temp, eventDescr);
    }


    private void startPebbleGetup(String weather, Double temp, String eventDescription){
        registerReceiver(pebbleSilenceReceiver, new IntentFilter("PEBBLE_SILENCE"));
        pebble.startAlarmApp(this);
        pebble.turnOnAlarm(this, false, eventDescription, Integer.toString(temp.intValue())+"º, "+weather);
        Log.d("SnoozeAlarm", "Should have turned on the pebble alarm.");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_final_alarm_screen, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void onDestroy(){
        super.onDestroy();

        pebble.turnOffAlarm(this, false);
        unregisterReceiver(pebbleSilenceReceiver);
        wakeLock.release();
    }

    BroadcastReceiver pebbleSilenceReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            FinalAlarmScreen.this.finish();
        }
    };
}
