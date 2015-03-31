package com.team8.ionesmartalarm;

import android.content.Context;
import android.location.Location;

public interface AlarmPrototype {

    public enum Weather {
        THUNDERSTORM(200), DRIZZLE(300), RAIN(500), SNOW(600), ATMOSPHERE(700), CLEAR(800), CLOUDS(801), EXTREME(900), OTHER(950);

        private final int code;

        private Weather(int code) {
            this.code = code;
        }

        public static Weather getWeather(int code) {
            if (code < DRIZZLE.code) {
                return THUNDERSTORM;
            } else if (code < RAIN.code) {
                return DRIZZLE;
            } else if (code < SNOW.code) {
                return RAIN;
            } else if (code < ATMOSPHERE.code) {
                return SNOW;
            } else if (code < CLEAR.code) {
                return ATMOSPHERE;
            } else if (code < CLOUDS.code) {
                return CLEAR;
            } else if (code < EXTREME.code) {
                return CLOUDS;
            } else if (code < OTHER.code) {
                return EXTREME;
            } else return OTHER;
        }
    }

    public void wakeupProcedure(Context context);
    public int calculateAlarmTime(Context context);
    public void onLocationTaskCompleted(Context context, Location location);
    public void onMapTaskCompleted(int duration);
    public void onWeatherTaskCompleted(int temperature, int code);
}
