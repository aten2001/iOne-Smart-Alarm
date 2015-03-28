package com.team8.ionesmartalarm;

import android.content.Context;
import android.database.Cursor;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.provider.CalendarContract.Events;
import android.text.format.Time;
import android.util.Log;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * @author Dennis
 *
 * Fetches and updates various data needed to set alarm times.
 * Uses the local calendar information (first schedule of the day), traffic information, and weather information.
 */
public class DataLoader {

    private static final String[] EVENT_COL = new String[] {
            Events.TITLE,
            Events.DTSTART,
            Events.DTEND,
            Events.EVENT_LOCATION
    };

    public long getFirstScheduleTime(Context context) {
        Time t = new Time();
        t.setToNow();
        String dtStart = Long.toString(t.toMillis(false));
        t.set(59, 59, 23, t.monthDay, t.month, t.year);
        String dtEnd = Long.toString(t.toMillis(false));

        String selection = "((" + Events.DTSTART + " >= ?) AND (" + Events.DTEND + " <= ?))";
        String[] selectionArgs = new String[]{dtStart, dtEnd};

        Cursor mCursor = context.getContentResolver().query(Events.CONTENT_URI, EVENT_COL, selection, selectionArgs, null);
        mCursor.moveToFirst();

        long time = mCursor.getCount() == 0 ? -1 : mCursor.getLong(1);
        mCursor.close();
        return time;
    }

    public String getFirstScheduleLocation(Context context) {
        Time t = new Time();
        t.setToNow();
        String dtStart = Long.toString(t.toMillis(false));
        t.set(59, 59, 23, t.monthDay, t.month, t.year);
        String dtEnd = Long.toString(t.toMillis(false));

        String selection = "((" + Events.DTSTART + " >= ?) AND (" + Events.DTEND + " <= ?))";
        String[] selectionArgs = new String[]{dtStart, dtEnd};

        Cursor mCursor = context.getContentResolver().query(Events.CONTENT_URI, EVENT_COL, selection, selectionArgs, null);
        mCursor.moveToFirst();

        String location = mCursor.getCount() == 0 ? null : mCursor.getString(3);
        mCursor.close();
        return location;
    }

    public Location getLastLocation(Context context) {
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        String bestProvider = locationManager.getBestProvider(new Criteria(), false);
        return locationManager.getLastKnownLocation(bestProvider);
    }

    public void getTrafficTime(Context context, AlarmPrototype alarmPrototype) {
        Location location = getLastLocation(context);
        MapLoader mapLoader = new MapLoader(location.getLatitude(), location.getLongitude(), getFirstScheduleLocation(context), alarmPrototype);
        mapLoader.execute();
    }

    public void getWeather(Context context, Long time, AlarmPrototype alarmPrototype) {
        Location location = getLastLocation(context);
        WeatherLoader weatherLoader = new WeatherLoader(location.getLatitude(), location.getLongitude(), time, alarmPrototype);
        weatherLoader.execute();
    }

    private class MapLoader extends AsyncTask<Void, Void, Integer> {

        private double latitude, longitude;
        private String location;
        private AlarmPrototype alarmPrototype;

        public MapLoader(double latitude, double longitude, String location, AlarmPrototype alarmPrototype) {
            this.latitude = latitude;
            this.longitude = longitude;
            this.location = location;
            this.alarmPrototype = alarmPrototype;
        }

        public MapLoader(double latitude, double longitude, String location) {
            this(latitude, longitude, location, null);
        }

        @Override
        protected Integer doInBackground(Void... params) {
            String url = null;
            try {
                url = "http://maps.googleapis.com/maps/api/directions/json?origin=" + latitude + "," + longitude + "&destination=" + URLEncoder.encode(location, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            HttpClient client = new DefaultHttpClient();
            HttpGet request = new HttpGet(url);
            try {
                HttpResponse response = client.execute(request);
                HttpEntity entity = response.getEntity();
                String str = EntityUtils.toString(entity);
                JSONObject json = new JSONObject(str);
                return json.getJSONArray("routes").getJSONObject(0).getJSONArray("legs").getJSONObject(0).getJSONObject("duration").getInt("value");
            } catch (IOException | JSONException e) {
                e.printStackTrace();
            }
            return 0;
        }

        @Override
        protected void onPostExecute(Integer duration) {
            super.onPostExecute(duration);
            Log.i("MapLoader", "duration: " + duration); // TEST

            if (alarmPrototype != null) {
                alarmPrototype.onMapTaskCompleted(duration);
            }
        }
    }

    private class WeatherLoader extends AsyncTask<Void, Void, Integer[]> {

        private double latitude, longitude;
        private long time;
        private AlarmPrototype alarmPrototype;

        public WeatherLoader(double latitude, double longitude, long time, AlarmPrototype alarmPrototype) {
            this.latitude = latitude;
            this.longitude = longitude;
            this.time = time / 1000;
            this.alarmPrototype = alarmPrototype;
        }

        public WeatherLoader(double latitude, double longitude, long time) {
            this(latitude, longitude, time, null);
        }

        @Override
        protected Integer[] doInBackground(Void... params) {
            String url = "http://api.openweathermap.org/data/2.5/forecast?lat=" + latitude + "&lon=" + longitude;
            HttpClient client = new DefaultHttpClient();
            HttpGet request = new HttpGet(url);
            try {
                HttpResponse response = client.execute(request);
                HttpEntity entity = response.getEntity();
                String str = EntityUtils.toString(entity);
                JSONArray forecasts = new JSONObject(str).getJSONArray("list");
                JSONObject forecast = null;
                for (int i = 0; i < forecasts.length(); i++) {
                    forecast = forecasts.getJSONObject(i);
                    long dt = forecast.getLong("dt");
                    if (Math.abs(dt - time) < 10800) {
                        break;
                    }
                }
                if (forecast == null) return null;
                Log.i("WeatherLoader", "forecast: " + forecast.toString()); // TEST
                Integer[] data = new Integer[2];
                data[0] = forecast.getJSONObject("main").getInt("temp");
                data[1] = forecast.getJSONArray("weather").getJSONObject(0).getInt("id");
                return data;
            } catch (IOException | JSONException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Integer[] data) {
            super.onPostExecute(data);
            Log.i("WeatherLoader", "temperature: " + data[0] + ", weather code: " + data[1]);

            if (alarmPrototype != null) {
                alarmPrototype.onWeatherTaskCompleted(data[0], data[1]);
            }
        }
    }
}
