package com.team8.ionesmartalarm;

import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;


public class SnoozeScreen extends ActionBarActivity {

    private PowerManager.WakeLock wakeLock;
    private PebbleController pebble;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_snooze_screen);

        //Go ahead and set the getup alarm
        Intent getupService = new Intent(this, GetupAlarm.class);
        getupService.putExtra("isFirstGetupAlarmSet", true);
        this.startService(getupService);

        PowerManager powerManager = (PowerManager) this.getApplicationContext().getSystemService(Context.POWER_SERVICE);
        wakeLock = powerManager.newWakeLock((PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP), "wakeupAlarm");
        wakeLock.acquire();

        //get past lock screen
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

        // Get data from the data loader
        DataLoader info = new DataLoader();
        Object[] calInfo = info.getFirstScheduleInformation(this);
        TextView eventDescription = (TextView) this.findViewById(R.id.eventDescription);
        TextView eventTitle = (TextView) this.findViewById(R.id.eventTitle);
        eventTitle.setText((String)calInfo[0]);
        eventDescription.setText((String)calInfo[4]);

        Bundle passedData = this.getIntent().getExtras();
        String weather = passedData.getString("weatherInfo", "Thunderstorm");
        Double temp = passedData.getDouble("tempInfo", 20);
        String eventDescr = passedData.getString("eventDescr", "No Events");
        //Start the pebble procedure
        pebble = new PebbleController();
        startPebbleWakeup(weather, temp, eventDescr);
    }

    private void startPebbleWakeup(String weather, Double temp, String eventDescription){
        pebble.beginReceivingDataFromWatch(this);
        pebble.startAlarmApp(this);
        pebble.turnOnAlarm(this, true, eventDescription, Integer.toString(temp.intValue())+"º, "+weather);
        Log.d("SnoozeAlarm", "Should have turned on the pebble alarm.");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_snooze_screen, menu);
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

    public void snooze(View view){
        this.finish();
    }

    @Override
    public void onDestroy(){
        super.onDestroy();

        pebble.turnOffAlarm(this, true);
        pebble.stopReceivingDataFromWatch(this);
        wakeLock.release();
    }
}
