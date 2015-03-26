package com.team8.ionesmartalarm;

import android.content.Context;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.provider.CalendarContract.Events;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.text.format.Time;
import android.util.Log;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.Format;

/**
 * @author Dennis
 *
 * Fetches and updates various data needed to set alarm times.
 * Uses the local calendar information (first schedule of the day), traffic information, and weather information.
 */
public class DataLoader implements LocationListener {

    private static final String[] EVENT_COL = new String[] {
            Events.TITLE,
            Events.DTSTART,
            Events.DTEND,
            Events.EVENT_LOCATION
    };

    private String firstLocation = null;

    public void startUpdatingTime(Context context) {
        Time t = new Time();
        t.setToNow();
        t.set(0, 0, 18, t.monthDay, t.month, t.year);
        String dtStart = Long.toString(t.toMillis(false));
        t.set(59, 59, 23, t.monthDay, t.month, t.year);
        String dtEnd = Long.toString(t.toMillis(false));

        String selection = "((" + Events.DTSTART + " >= ?) AND (" + Events.DTEND + " <= ?))";
        String[] selectionArgs = new String[]{dtStart, dtEnd};

        Cursor mCursor = context.getContentResolver().query(Events.CONTENT_URI, EVENT_COL, selection, selectionArgs, null);
        mCursor.moveToFirst();

        Log.i("DataLoader", "" + mCursor.getCount()); // TEST

        if (mCursor.getCount() != 0) {
            String title = mCursor.getString(0);
            long start = mCursor.getLong(1);
            long end = mCursor.getLong(2);
            firstLocation = mCursor.getString(3);

            Format df = DateFormat.getDateFormat(context);
            Format tf = DateFormat.getTimeFormat(context);
            Log.i("DataLoader", "Title: " + title +
                    ", Start: " + df.format(start) + " " + tf.format(start) +
                    ", End: " + df.format(end) + " " + tf.format(end) +
                    ", Location: " + firstLocation); // TEST

            LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 300000, 0, this);
        }

        mCursor.close();
    }

    @Override
    public void onLocationChanged(Location location) {
        MapLoader mapLoader = new MapLoader();
        mapLoader.execute(location.getLatitude(), location.getLongitude());
        WeatherLoader weatherLoader = new WeatherLoader();
        weatherLoader.execute(location.getLatitude(), location.getLongitude());
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

    private class MapLoader extends AsyncTask<Double, Void, Integer> {

        @Override
        protected Integer doInBackground(Double... params) {
            String url = null;
            try {
                url = "http://maps.googleapis.com/maps/api/directions/json?origin=" + params[0] + "," + params[1] + "&destination=" + URLEncoder.encode(firstLocation, "UTF-8");
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
        protected void onPostExecute(Integer value) {
            super.onPostExecute(value);
            Log.i("MapLoader", "value: " + value); // TEST
        }
    }

    private class WeatherLoader extends AsyncTask<Double, Void, Void> {

        @Override
        protected Void doInBackground(Double... params) {
            String url = "http://api.openweathermap.org/data/2.5/weather?lat=" + params[0] + "&lon=" + params[1];
            HttpClient client = new DefaultHttpClient();
            HttpGet request = new HttpGet(url);
            try {
                HttpResponse response = client.execute(request);
                HttpEntity entity = response.getEntity();
                String str = EntityUtils.toString(entity);
                JSONObject json = new JSONObject(str);
                Log.i("WeatherLoader", "json: " + json.toString()); // TEST
            } catch (IOException | JSONException e) {
                e.printStackTrace();
            }

            return null;
        }
    }
}
