package com.team8.ionesmartalarm;

import android.content.Context;
import android.database.Cursor;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.CalendarContract.Events;
import android.text.format.Time;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;

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
 * @author Team 8
 *
 * Fetches and updates various data needed to set alarm times.
 * Uses the local calendar information (first schedule of the day), traffic information, and weather information.
 */
public class DataLoader {

    private static final String[] EVENT_COL = new String[] {
            Events.TITLE,
            Events.DTSTART,
            Events.DTEND,
            Events.EVENT_LOCATION,
            Events.DESCRIPTION
    };

    /**
     * Returns the starting time for the first schedule
     *
     * @param context current context
     * @return time value in long, or null if no schedule was found
     */
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

    /**
     * Returns a string (for the Pebble) with the title and the location of the first schedule, in the form: Title @ Location
     *
     * @param context current context
     * @return title and location of the first schedule
     */
    public String getFirstScheduleDescription(Context context) {
        Time t = new Time();
        t.setToNow();
        String dtStart = Long.toString(t.toMillis(false));
        t.set(59, 59, 23, t.monthDay, t.month, t.year);
        String dtEnd = Long.toString(t.toMillis(false));

        String selection = "((" + Events.DTSTART + " >= ?) AND (" + Events.DTEND + " <= ?))";
        String[] selectionArgs = new String[]{dtStart, dtEnd};

        Cursor mCursor = context.getContentResolver().query(Events.CONTENT_URI, EVENT_COL, selection, selectionArgs, null);
        mCursor.moveToFirst();

        String location = mCursor.getCount() == 0 ? null : (mCursor.getString(0) + " @ " + mCursor.getString(3));
        mCursor.close();
        return location;
    }

    /**
     * Returns a set of information available about the first schedule
     * {Title, StartTime, EndTime, Location, Description}
     *
     * @param context current context
     * @return a list of information about the first schedule
     */
    public Object[] getFirstScheduleInformation(Context context) {
        Time t = new Time();
        t.setToNow();
        String dtStart = Long.toString(t.toMillis(false));
        t.set(59, 59, 23, t.monthDay, t.month, t.year);
        String dtEnd = Long.toString(t.toMillis(false));

        String selection = "((" + Events.DTSTART + " >= ?) AND (" + Events.DTEND + " <= ?))";
        String[] selectionArgs = new String[]{dtStart, dtEnd};

        Cursor mCursor = context.getContentResolver().query(Events.CONTENT_URI, EVENT_COL, selection, selectionArgs, null);
        mCursor.moveToFirst();

        if (mCursor.getCount() == 0)
            return null;

        Object[] information = {mCursor.getString(0), mCursor.getLong(1), mCursor.getLong(2), mCursor.getString(3), mCursor.getString(4)};
        mCursor.close();

        return information;
    }

    /**
     * Returns a location of the first schedule
     *
     * @param context current context
     * @return location of the first schedule
     */
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

    /**
     * Start an asynchronous process to get the last (current) location of the device
     * It will use the last known location if it is available and is not older than 15 minutes
     *
     * @param context current context
     * @param alarmPrototype an AlarmPrototype class to pass the data to
     */
    public void getLastLocation(final Context context, final AlarmPrototype alarmPrototype) {
        Log.i("DataLoader", "I am in the getLastLocation");
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        criteria.setPowerRequirement(Criteria.POWER_HIGH);
        String bestProvider = locationManager.getBestProvider(criteria, false);
        final Location location = locationManager.getLastKnownLocation(bestProvider);

        if (location == null || ((SystemClock.elapsedRealtimeNanos() - location.getElapsedRealtimeNanos()) / 1000000) >= 900000) {   // 15 minutes
            /*
            locationManager.requestSingleUpdate(bestProvider, new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    alarmPrototype.onLocationTaskCompleted(context, location);
                }

                @Override
                public void onStatusChanged(String provider, int status, Bundle extras) {

                }

                @Override
                public void onProviderEnabled(String provider) {

                }

                @Override
                public void onProviderDisabled(String provider) {

                }
            }, Looper.myLooper());*/

            LocationInfo locationInfo = new LocationInfo(context, alarmPrototype);
            locationInfo.getLocation();


        } else {
            alarmPrototype.onLocationTaskCompleted(context, location);
        }
    }

    /**
     * Implements callback functions for getting the location information
     */
    protected class LocationInfo implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

        private Context context;
        private AlarmPrototype alarmPrototype;
        private GoogleApiClient mGoogleApiClient;

        public LocationInfo(Context context, AlarmPrototype alarmPrototype) {
            this.context = context;
            this.alarmPrototype = alarmPrototype;
        }

        public void getLocation() {
            Log.i("LocationInfo", "I am trying to get location from google api");
            mGoogleApiClient = new GoogleApiClient.Builder(context)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

            mGoogleApiClient.connect();
        }

        @Override
        public void onConnected(Bundle connectionHint){
            Log.i("LocationInfo", "I received a callback from the api");
            Location location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);

            if(location == null){
                Log.e("LocationInfo", "There is still no location info from google api");
                return;
            }
            alarmPrototype.onLocationTaskCompleted(context, location);
        }

        @Override
        public void onConnectionSuspended(int i) {

        }

        @Override
        public void onConnectionFailed(ConnectionResult connectionResult) {

        }
    }

    /**
     * Starts an asynchronous process to get the traffic time from the current location to the given location
     *
     * @param context current context
     * @param location destination
     * @param alarmPrototype an AlarmPrototype class to pass the data to
     */
    public void getTrafficTime(Context context, Location location, AlarmPrototype alarmPrototype) {
        MapLoader mapLoader = new MapLoader(location.getLatitude(), location.getLongitude(), getFirstScheduleLocation(context), alarmPrototype);
        mapLoader.execute();
    }

    /**
     * Starts an asynchronous process to get the weather information at the given location
     *
     * @param location destination
     * @param time time for the forecast
     * @param alarmPrototype an AlarmPrototype class to pass the data to
     */
    public void getWeather(Location location, Long time, AlarmPrototype alarmPrototype) {
        WeatherLoader weatherLoader = new WeatherLoader(location.getLatitude(), location.getLongitude(), time, alarmPrototype);
        weatherLoader.execute();
    }

    /**
     * AsyncTask to get traffic information
     */
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

    /**
     * AsyncTask to get weather information
     * Returns a temperature, a weather code, and a human-readable string information about the weather
     */
    private class WeatherLoader extends AsyncTask<Void, Void, Object[]> {

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
        protected Object[] doInBackground(Void... params) {
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
                Object[] data = new Object[3];
                data[0] = (int) Math.round(forecast.getJSONObject("main").getDouble("temp") * 1.8 - 459.67);
                data[1] = forecast.getJSONArray("weather").getJSONObject(0).getInt("id");
                data[2] = forecast.getJSONArray("weather").getJSONObject(0).getString("main");
                return data;
            } catch (IOException | JSONException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Object[] data) {
            super.onPostExecute(data);

            //Check for random null errors
            if(data!= null && data.length >= 2) {
                Log.i("WeatherLoader", "temperature: " + data[0] + ", weather code: " + data[1]);

                if (alarmPrototype != null) {
                    alarmPrototype.onWeatherTaskCompleted((int) data[0], (int) data[1], (String) data[2]);
                }
            }
            else{
                if (alarmPrototype != null) {
                    alarmPrototype.onWeatherTaskCompleted((int) 287, (int)500, (String) "Rain");
                }
            }
        }
    }
}
