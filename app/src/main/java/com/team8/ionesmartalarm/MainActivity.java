package com.team8.ionesmartalarm;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ToggleButton;


public class MainActivity extends ActionBarActivity {

    public static smartAlarmManager smartAlarm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if(smartAlarm == null){
            smartAlarm = new smartAlarmManager(MainActivity.this);
            if(smartAlarm.isActive()) {
                smartAlarm.setAlarm();
                smartAlarm.setGetupAlarm(this, 05, 27);
                //Log.i("MainActivity", "This should have set the getup alarm.");
            }
        }
        // Set the toggle button accordingly
        ToggleButton alarmActiveButton = (ToggleButton) this.findViewById(R.id.alarmState);
        alarmActiveButton.setChecked(smartAlarm.isActive());
    }

    public void onDestroy(){
        smartAlarm.cancelAlarm();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
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

    public void onToggleClicked(View view){
        Context context = this.getBaseContext();
        ComponentName alarmRebootReceiver = new ComponentName(context, BootReceiver.class);
        PackageManager packageManager = context.getPackageManager();

        boolean isOn = ((ToggleButton) view).isChecked();
        if(isOn){
            // Enable the alarm even on reboot
            packageManager.setComponentEnabledSetting(alarmRebootReceiver, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
            Log.i("MainActivity", "The alarm has reboot alarm enabled");
            // Set the repeated alarm
            smartAlarm.setAlarm();
            smartAlarm.setGetupAlarm(this, 05, 19);
        }
        else{
            // Disable the alarm from restarting on reboot
            packageManager.setComponentEnabledSetting(alarmRebootReceiver, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
            Log.i("MainActivity", "The alarm has reboot alarm disabled");
            smartAlarm.cancelAlarm();
        }
    }
}
