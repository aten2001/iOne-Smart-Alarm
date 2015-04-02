package com.team8.ionesmartalarm;

import android.content.Context;
import android.location.Location;

/**
 * @author Team 8
 *
 * Interface for the alarm classes (WakeupAlarm, GetupAlarm)
 */
public interface AlarmPrototype {

    /**
     * Enum for available weather codes from the weather API
     */
    public enum Weather {
        THUNDERSTORM(200, 900), DRIZZLE(300, 300), RAIN(500, 600), SNOW(600, 900), ATMOSPHERE(700, 300), CLEAR(800, 0), CLOUDS(801, 0), EXTREME(900, 1800), OTHER(950, 0);

        private final int code;
        public final int weight;

        /**
         * Constructor for the Weather
         *
         * @param code weather code
         * @param weight amount of time to subtract from the final alarm time
         */
        private Weather(int code, int weight) {
            this.code = code;
            this.weight = weight;
        }

        /**
         * Returns a Weather enum object corresponding to the weather code
         *
         * @param code weather code
         * @return the corresponding Weather enum object
         */
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

    /**
     * Wakeup procedure to run when called
     *
     * @param context current context
     */
    public void wakeupProcedure(Context context);

    /**
     * Start an asynchronous process to calculate the alarm time
     *
     * @param context current context
     */
    public void calculateAlarmTime(Context context);

    /**
     * Called when the location has been updated
     *
     * @param context current context
     * @param location the updated location
     */
    public void onLocationTaskCompleted(Context context, Location location);

    /**
     * Called when the traffic information has been updated
     *
     * @param duration the updated time it takes to the location
     */
    public void onMapTaskCompleted(int duration);

    /**
     * Called when the weather information has been updated
     *
     * @param temperature the updated temperature
     * @param code the updated weather code
     * @param description the updated human-readable weather description
     */
    public void onWeatherTaskCompleted(int temperature, int code, String description);

    /**
     * Called when every data has been updated and is ready
     */
    public void onAllTasksCompleted();
}
