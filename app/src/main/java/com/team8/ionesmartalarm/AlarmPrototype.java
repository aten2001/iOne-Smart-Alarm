package com.team8.ionesmartalarm;

import android.content.Context;

public interface AlarmPrototype {
    public void wakeupProcedure(Context context);
    public int calculateAlarmTime(Context context);
    public void onMapTaskCompleted(int duration);
    public void onWeatherTaskCompleted(int temperature, int code);
}
