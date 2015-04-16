package com.team8.ionesmartalarm;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.ToggleButton;

/**
 * @author Team 8
 *
 * MainActivity of the app
 */
public class MainActivity extends ActionBarActivity {

    public static SmartAlarmManager smartAlarm;
    private boolean isReceieverSet = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if(smartAlarm == null){
            smartAlarm = new SmartAlarmManager(MainActivity.this);
            if(smartAlarm.isActive()) {
                smartAlarm.setAlarm();
                //Log.i("MainActivity", "This should have set the getup alarm.");
            }
        }
        // Set the toggle button accordingly
        ToggleButton alarmActiveButton = (ToggleButton) this.findViewById(R.id.alarmState);
        alarmActiveButton.setChecked(smartAlarm.isActive());

        //Set a listener for when the background returns data for UI
        registerReceiver(fillNextAlarmInfo, new IntentFilter("SET_NEXT_ALARM_UI"));
        isReceieverSet = true;
    }

    public void onDestroy(){
        super.onDestroy();

        if(this.isReceieverSet) {
            unregisterReceiver(fillNextAlarmInfo);
            isReceieverSet = false;
        }
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

            //Set the receiver if it has been unregistered
            if(!isReceieverSet) {
                try {
                    registerReceiver(fillNextAlarmInfo, new IntentFilter("SET_NEXT_ALARM_UI"));
                    isReceieverSet = true;
                } catch (IllegalArgumentException e) {
                    Log.e("MainActivity", "The receiever was already set.", e);
                }
            }
            // Set the repeated alarm
            smartAlarm.setAlarm();
        }
        else{
            // Disable the alarm from restarting on reboot
            packageManager.setComponentEnabledSetting(alarmRebootReceiver, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
            Log.i("MainActivity", "The alarm has reboot alarm disabled");

            //Disable the receiever so app does not receive fake alarms used to cancel in AlarmManager
            unregisterReceiver(fillNextAlarmInfo);
            removeNextAlarmInfo();
            isReceieverSet = false;
            //cancel the alarm
            smartAlarm.cancelAlarm(this);

        }
    }

    private void removeNextAlarmInfo(){
        this.findViewById(R.id.textView).setVisibility(View.INVISIBLE);
        this.findViewById(R.id.textView2).setVisibility(View.INVISIBLE);
        this.findViewById(R.id.textView3).setVisibility(View.INVISIBLE);
        this.findViewById(R.id.textView4).setVisibility(View.INVISIBLE);
        this.findViewById(R.id.nextAlarmTime).setVisibility(View.INVISIBLE);
        this.findViewById(R.id.nextAlarmEvent).setVisibility(View.INVISIBLE);
        this.findViewById(R.id.nextAlarmWeather).setVisibility(View.INVISIBLE);
    }

    public void fillNextAlarmInfo(String time, String weather, String event){
        //Set all the descriptions to visible
        this.findViewById(R.id.textView).setVisibility(View.VISIBLE);
        this.findViewById(R.id.textView2).setVisibility(View.VISIBLE);
        this.findViewById(R.id.textView3).setVisibility(View.VISIBLE);
        this.findViewById(R.id.textView4).setVisibility(View.VISIBLE);

        //Get all of the placeholder values
        TextView nextTime = (TextView) this.findViewById(R.id.nextAlarmTime);
        TextView nextEvent = (TextView) this.findViewById(R.id.nextAlarmEvent);
        TextView nextWeather = (TextView) this.findViewById(R.id.nextAlarmWeather);

        //Make them visible
        nextTime.setVisibility(View.VISIBLE);
        nextEvent.setVisibility(View.VISIBLE);
        nextWeather.setVisibility(View.VISIBLE);

        //Assign the values
        nextTime.setText(time);
        nextEvent.setText(event);
        nextWeather.setText(weather);
    }

    public void launchDemoWakeup(View view){
        Intent newActivity = new Intent(this, SnoozeScreen.class);
        newActivity.putExtra("weatherInfo", "Rain");
        newActivity.putExtra("tempInfo", 20);
        newActivity.putExtra("eventDescr", "Demo Event");
        this.startActivity(newActivity);
    }

    public void launchDemoGetup(View view){
        Intent newActivity = new Intent(this, FinalAlarmScreen.class);
        newActivity.putExtra("weatherInfo", "Rain");
        newActivity.putExtra("tempInfo", 20);
        newActivity.putExtra("eventDescr", "Demo Event");
        this.startActivity(newActivity);
    }

    BroadcastReceiver fillNextAlarmInfo = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //Give default values for next alarm infor
            String weather = "";
            String eventDescr = "";
            String time = "";

            Bundle passedData = intent.getExtras();
            if(passedData != null) {
                weather = passedData.getString("weatherInfo", "Thunderstorm");
                time = passedData.getString("time", "Demo Alarm");
                eventDescr = passedData.getString("eventDescr", "No Events");
            }

            MainActivity.this.fillNextAlarmInfo(time, weather, eventDescr);
        }
    };
}
