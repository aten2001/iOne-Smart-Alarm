package com.team8.ionesmartalarm;

import android.app.Activity;
import android.content.Context;
import android.location.Location;

public interface AlarmPrototype {

    public enum Weather {
        THUNDERSTORM(200, 900), DRIZZLE(300, 300), RAIN(500, 600), SNOW(600, 900), ATMOSPHERE(700, 300), CLEAR(800, 0), CLOUDS(801, 0), EXTREME(900, 1800), OTHER(950, 0);

        private final int code;
        public final int weight;

        private Weather(int code, int weight) {
            this.code = code;
            this.weight = weight;
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

    public void calculateAlarmTime(Context context);

    public void onLocationTaskCompleted(Context context, Location location);

    public void onMapTaskCompleted(int duration);

    public void onWeatherTaskCompleted(int temperature, int code);
}
