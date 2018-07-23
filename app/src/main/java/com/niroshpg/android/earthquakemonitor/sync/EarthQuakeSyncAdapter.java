package com.niroshpg.android.earthquakemonitor.sync;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SyncRequest;
import android.content.SyncResult;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;

import com.niroshpg.android.earthquakemonitor.MainActivity;
import com.niroshpg.android.earthquakemonitor.R;
import com.niroshpg.android.earthquakemonitor.Utility;
import com.niroshpg.android.earthquakemonitor.data.EarthQuakeDataContract;
import com.niroshpg.android.earthquakemonitor.data.EarthQuakeDataContract.QuakesEntry;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Vector;


/**
 * Sync Adapter for earthquake data
 *
 * @author niroshpg
 * @since  06/10/2014
 *
 * reference: The sync adapter implementation from https://github.com/udacity/Sunshine
 */
public class EarthQuakeSyncAdapter extends AbstractThreadedSyncAdapter {
    public final String LOG_TAG = EarthQuakeSyncAdapter.class.getSimpleName();
    final String EQ_BASE_URL =
            "https://earthquake.usgs.gov/earthquakes/feed/v1.0/summary/";

    // Interval at which to sync with the earthquake data from server, in seconds.
    // 60 seconds (1 minute) * 180 = 3 hours
    public static final int SYNC_INTERVAL = 60 * 180;
    public static final int SYNC_FLEXTIME = SYNC_INTERVAL/3;
    private static final long DAY_IN_MILLIS = 1000 * 60 * 60 * 24;
    private static final int EARTH_QUAKE_NOTIFICATION_ID = 3005;

    /**
     * projection for finding most significant event to be notified
     */
    private static final String[] NOTIFY_QUAKES_PROJECTION = new String[] {
            QuakesEntry.COLUMN_PLACE,
            QuakesEntry.COLUMN_MAG,
            QuakesEntry.COLUMN_DEPTH,
            QuakesEntry.COLUMN_ALERT,
            "MAX("+QuakesEntry.COLUMN_SIG +")"
    };

    private static final int INDEX_EQ_PLACE = 0;
    private static final int INDEX_EQ_MAG = 1;
    private static final int INDEX_EQ_DEPTH = 2;
    private static final int INDEX_EQ_ALERT = 3;
    private static final int INDEX_EQ_SIG = 4;

    /**
     * constructor
     * @param context
     * @param autoInitialize
     */
    public EarthQuakeSyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
    }

    /**
     * Read earthquake data from USGS geojson urls,
     *
     * http://earthquake.usgs.gov/earthquakes/feed/v1.0/summary/4.5_week.geojson
     *
     */
    private void readEarthquakesData()
    {
        List<String> resultsStr = new ArrayList<String>();

        // These two need to be declared outside the try/catch
        // so that they can be closed in the finally block.
        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;

        // Will contain the raw JSON response as a string.
        String earthquakeEventsJsonString = null;

        int numDays = getSelectedNumberOfDays();

        try {
            // Construct the URL for the USGS geo json query
            Uri builtUri = Uri.parse(getURLBasedOnFeedSelection()).buildUpon().build();

            URL url = new URL(builtUri.toString());

            // Create the request to USGS server, and open the connection
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();
            Log.i("Response Code" , String.valueOf(urlConnection.getResponseCode()));
            // Read the input stream into a String
            InputStream inputStream = urlConnection.getInputStream();
            StringBuffer buffer = new StringBuffer();
            if (inputStream == null) {
                // Nothing to do.
                return ;
            }

            reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while ((line = reader.readLine()) != null) {
                // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                // But it does make debugging a *lot* easier if you print out the completed
                // buffer for debugging.
                buffer.append(line + "\n");
            }

            if (buffer.length() == 0) {
                // Stream was empty.  No point in parsing.
                return ;
            }
            earthquakeEventsJsonString = buffer.toString();
        } catch (IOException e) {
            Log.e(LOG_TAG, "Error ", e);
            // If the code didn't successfully get the earthquake events data, there's no point in attempting
            // to parse it.
            return ;
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (final IOException e) {
                    Log.e(LOG_TAG, "Error closing stream", e);
                }
            }
        }

        // parse data from json string

        // These are the names of the JSON objects that need to be extracted.
        final String USGS_FEATURES = "features";
        final String USGS_FEATURE_PROPERTIES = "properties";
        final String USGS_FEATURE_PROPERTIES_MAG = "mag";
        final String USGS_FEATURE_PROPERTIES_ALERT = "alert";
        final String USGS_FEATURE_PROPERTIES_PLACE = "place";
        final String USGS_FEATURE_PROPERTIES_TIME = "time";
        final String USGS_FEATURE_PROPERTIES_SIG = "sig";
        final String USGS_FEATURE_PROPERTIES_TZ = "tz";
        final String USGS_FEATURE_PROPERTIES_URL = "url";
        final String USGS_FEATURE_PROPERTIES_UPDATED = "updated";
        final String USGS_FEATURE_GOEMETRY = "geometry";
        final String USGS_FEATURE_GOEMETRY_COORD = "coordinates";

        try {
            JSONObject earthquakesEventsJson = new JSONObject(earthquakeEventsJsonString);
            JSONArray featuresArray = earthquakesEventsJson.getJSONArray(USGS_FEATURES);

            // Insert the new earthquakes information into the database
            Vector<ContentValues> cVVector = new Vector<ContentValues>(featuresArray.length());

            for(int i = 0; i < featuresArray.length(); i++) {
                // These are the values that will be collected.
                Double mag=0.0;
                String place="";
                Long time=null;
                Long updated=null;
                int timezone=0;
                Double latitude=null;
                Double longitude=null;
                Double depth=null;
                String alert="";
                int sig=0;
                String url="";

                JSONObject eqEvent = featuresArray.getJSONObject(i);
                JSONObject eqProperties = eqEvent.getJSONObject(USGS_FEATURE_PROPERTIES);
                mag = eqProperties.getDouble(USGS_FEATURE_PROPERTIES_MAG);
                alert = eqProperties.getString(USGS_FEATURE_PROPERTIES_ALERT);
                place = eqProperties.getString(USGS_FEATURE_PROPERTIES_PLACE);
                time = eqProperties.getLong(USGS_FEATURE_PROPERTIES_TIME);

                if(eqProperties.has(USGS_FEATURE_PROPERTIES_TZ))
                {
                    if(!eqProperties.isNull(USGS_FEATURE_PROPERTIES_TZ))
                    {
                        timezone = eqProperties.getInt(USGS_FEATURE_PROPERTIES_TZ);
                    }
                    else
                    {
                        Log.d(LOG_TAG, "could not read timezone ");
                    }

                }
                else
                {
                    Log.d(LOG_TAG, "could not read timezone ");
                }
                sig = eqProperties.getInt(USGS_FEATURE_PROPERTIES_SIG);
                updated = eqProperties.getLong(USGS_FEATURE_PROPERTIES_UPDATED);
                url = eqProperties.getString(USGS_FEATURE_PROPERTIES_URL);
                JSONObject eqGeometry = eqEvent.getJSONObject(USGS_FEATURE_GOEMETRY);
                JSONArray coordinatesArray = eqGeometry.getJSONArray(USGS_FEATURE_GOEMETRY_COORD);
                longitude = coordinatesArray.getDouble(0);
                latitude = coordinatesArray.getDouble(1);
                depth = coordinatesArray.getDouble(2);

                ContentValues quakeValues = new ContentValues();

                quakeValues.put(QuakesEntry.COLUMN_MAG, mag);
                quakeValues.put(QuakesEntry.COLUMN_DATETEXT, EarthQuakeDataContract.getDbDateString(new DateTime(time, DateTimeZone.UTC)));
                quakeValues.put(QuakesEntry.COLUMN_TZ, timezone);
                quakeValues.put(QuakesEntry.COLUMN_PLACE, place);
                quakeValues.put(QuakesEntry.COLUMN_LAT, latitude);
                quakeValues.put(QuakesEntry.COLUMN_LONG, longitude);
                quakeValues.put(QuakesEntry.COLUMN_DEPTH, depth);
                quakeValues.put(QuakesEntry.COLUMN_ALERT, alert);
                quakeValues.put(QuakesEntry.COLUMN_SIG, sig);
                quakeValues.put(QuakesEntry.COLUMN_UPDATED, EarthQuakeDataContract.getDbDateString(new DateTime(updated, DateTimeZone.UTC )));
                quakeValues.put(QuakesEntry.COLUMN_URL, url);
                quakeValues.put(QuakesEntry.COLUMN_SHORT_DESC, place+","+mag+","+depth);

                cVVector.add(quakeValues);
                String timeText =  EarthQuakeDataContract.getDbDateString(new DateTime(time ));
                String resultsStrValue = "mag="+mag+", place="+place+", latitude="+latitude+", "+
                        "longitude="+longitude+", depth="+depth+", time="+ timeText;
                resultsStr.add(resultsStrValue);
            }
            if ( cVVector.size() > 0 ) {
                ContentValues[] cvArray = new ContentValues[cVVector.size()];
                cVVector.toArray(cvArray);
                getContext().getContentResolver().bulkInsert(QuakesEntry.CONTENT_URI, cvArray);

                Calendar cal = Calendar.getInstance(); //Get's a calendar object with the current time.
                if(numDays >0) {
                    cal.add(Calendar.DATE, -numDays); //Signifies values for given range
                }
                else
                {
                    cal.add(Calendar.HOUR, -1); //Signifies values for given range
                }
                String lastWeekDate = EarthQuakeDataContract.getDbDateString(new DateTime(cal.getTime()));
                getContext().getContentResolver().delete(QuakesEntry.CONTENT_URI,
                        QuakesEntry.COLUMN_DATETEXT + " <= ?",
                        new String[] {lastWeekDate});

                notifyEarthQuake();
            }
            Log.d(LOG_TAG, "readEarthquakesData Complete. " + cVVector.size() + " Inserted");

        } catch (JSONException e) {
            Log.e(LOG_TAG, e.getMessage(), e);
            e.printStackTrace();
        }
    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority, ContentProviderClient provider, SyncResult syncResult) {
        readEarthquakesData();
    }

    /**
     * notify most significant earthquake occurred for the past day
     */
    private void notifyEarthQuake() {
        Context context = getContext();
        //checking the last update and notify if it' the first of the day
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String displayNotificationsKey = context.getString(R.string.pref_enable_notifications_key);
        boolean displayNotifications = prefs.getBoolean(displayNotificationsKey,
                Boolean.parseBoolean(context.getString(R.string.pref_enable_notifications_default)));

        if ( displayNotifications ) {

            String lastNotificationKey = context.getString(R.string.pref_last_notification);
            long lastSync = prefs.getLong(lastNotificationKey, 0);

            if (System.currentTimeMillis() - lastSync >= DAY_IN_MILLIS) {
                // we'll query our contentProvider, as always
                Cursor cursor = context.getContentResolver().query(QuakesEntry.CONTENT_URI, NOTIFY_QUAKES_PROJECTION, null, null, null);

                if (cursor.moveToFirst()) {
                    String place = cursor.getString(INDEX_EQ_PLACE);
                    double mag = cursor.getDouble(INDEX_EQ_MAG);
                    double depth = cursor.getDouble(INDEX_EQ_DEPTH);
                    String alert = cursor.getString(INDEX_EQ_ALERT);
                    int sig = cursor.getInt(INDEX_EQ_SIG);

                    int iconId = Utility.getIconResourceForAlertCondition(alert,sig);
                    String title = context.getString(R.string.app_name);

                    // Define the text of the event.
                    String contentText = String.format(context.getString(R.string.format_notification),
                            place,
                            String.valueOf(mag),
                            String.valueOf(depth));

                    NotificationCompat.Builder mBuilder =
                            new NotificationCompat.Builder(getContext())
                                    .setSmallIcon(iconId)
                                    .setContentTitle(title)
                                    .setContentText(contentText);

                    // open the application when user click the notification
                    Intent resultIntent = new Intent(context, MainActivity.class);

                    // The stack builder object will contain an artificial back stack for the
                    // started Activity.
                    // This ensures that navigating backward from the Activity leads out of
                    // your application to the Home screen.
                    TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
                    stackBuilder.addNextIntent(resultIntent);
                    PendingIntent resultPendingIntent =
                            stackBuilder.getPendingIntent(
                                    0,
                                    PendingIntent.FLAG_UPDATE_CURRENT
                            );
                    mBuilder.setContentIntent(resultPendingIntent);

                    NotificationManager mNotificationManager =
                            (NotificationManager) getContext().getSystemService(Context.NOTIFICATION_SERVICE);
                    // EARTH_QUAKE_NOTIFICATION_ID allows you to update the notification later on.
                    mNotificationManager.notify(EARTH_QUAKE_NOTIFICATION_ID, mBuilder.build());


                    //refreshing last sync
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putLong(lastNotificationKey, System.currentTimeMillis());
                    editor.commit();
                }
            }
        }
    }

    /**
     * Helper retrieve the number of days to be saved selected by the user
     * @return
     */
    private int getSelectedNumberOfDays()
    {
        int numDays = 7;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());

        String historyRange = prefs.getString(getContext().getString(R.string.pref_range_key),
                getContext().getString(R.string.pref_range_week));
        if (historyRange.contains(getContext().getString(R.string.pref_range_hour))) {
            numDays = 0;
        }
        else if (historyRange.contains(getContext().getString(R.string.pref_range_day))) {
            numDays = 1;
        }
        else if (historyRange.contains(getContext().getString(R.string.pref_range_week))) {
            numDays = 7;
        }
        else if (historyRange.contains(getContext().getString(R.string.pref_range_month))) {
            numDays = 30;
        }

        return numDays;
    }

    /**
     * Helper method to generate the url based on the feed user preference
     * @return
     */
    private String getURLBasedOnFeedSelection()
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        String feedLevel = prefs.getString(getContext().getString(R.string.pref_feed_key),
                getContext().getString(R.string.pref_feed_sig));

        String historyRange = prefs.getString(getContext().getString(R.string.pref_range_key),
                getContext().getString(R.string.pref_range_week));
        String RANGE_SELECT ="week";
        if(historyRange != null && historyRange.length() >0)
        {
            if (historyRange.contains(getContext().getString(R.string.pref_range_hour))) {
                RANGE_SELECT ="hour";
            }
            else if (historyRange.contains(getContext().getString(R.string.pref_range_day))) {
                RANGE_SELECT =  "day";
            }
            else if (historyRange.contains(getContext().getString(R.string.pref_range_week))) {
                RANGE_SELECT =  "week";
            }
            else if (historyRange.contains(getContext().getString(R.string.pref_range_month))) {
                RANGE_SELECT =  "month";
            }
            else
                RANGE_SELECT =  "week";
        }

        String SIGNIFICANT_SELECT ="significant";

        if(feedLevel != null && feedLevel.length() >0) {
            if (feedLevel.contains(getContext().getString(R.string.pref_feed_sig))) {
                SIGNIFICANT_SELECT = "significant";
            }
            else if (feedLevel.contains(getContext().getString(R.string.pref_feed_4_5))) {
                SIGNIFICANT_SELECT = "4.5";
            }
            else if (feedLevel.contains(getContext().getString(R.string.pref_feed_all))) {
                SIGNIFICANT_SELECT = "all";
            }
            else
                SIGNIFICANT_SELECT = "significant";
        }

        String url = EQ_BASE_URL + SIGNIFICANT_SELECT+"_"+RANGE_SELECT+".geojson";
       return url;
    }

    /**
     * Helper method to schedule the sync adapter periodic execution
     */
    public static void configurePeriodicSync(Context context, int syncInterval, int flexTime) {
        Account account = getSyncAccount(context);
        String authority = context.getString(R.string.content_authority);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            // we can enable inexact timers in our periodic sync
            SyncRequest request = new SyncRequest.Builder().
                    syncPeriodic(syncInterval, flexTime).
                    setSyncAdapter(account, authority).setExtras(new Bundle()).build();
            ContentResolver.requestSync(request);
        } else {
            ContentResolver.addPeriodicSync(account,
                    authority, new Bundle(), syncInterval);
        }
    }


    /**
     * Helper method to have the sync adapter sync immediately
     * @param context The context used to access the account service
     */
    public static void syncImmediately(Context context) {
        Bundle bundle = new Bundle();
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        ContentResolver.requestSync(getSyncAccount(context),
                context.getString(R.string.content_authority), bundle);
    }

    /**
     * Helper method to get the fake account to be used with SyncAdapter, or make a new one
     * if the fake account doesn't exist yet.  If we make a new account, we call the
     * onAccountCreated method so we can initialize things.
     *
     * @param context The context used to access the account service
     * @return a fake account.
     */
    public static Account getSyncAccount(Context context) {
        // Get an instance of the Android account manager
        AccountManager accountManager =
                (AccountManager) context.getSystemService(Context.ACCOUNT_SERVICE);

        // Create the account type and default account
        Account newAccount = new Account(
                context.getString(R.string.app_name), context.getString(R.string.sync_account_type));

        // If the password doesn't exist, the account doesn't exist
        if ( null == accountManager.getPassword(newAccount) ) {

        /*
         * Add the account and account type, no password or user data
         * If successful, return the Account object, otherwise report an error.
         */
            if (!accountManager.addAccountExplicitly(newAccount, "", null)) {
                return null;
            }
            /*
             * If you don't set android:syncable="true" in
             * in your <provider> element in the manifest,
             * then call ContentResolver.setIsSyncable(account, AUTHORITY, 1)
             * here.
             */

            onAccountCreated(newAccount, context);
        }
        return newAccount;
    }


    /**
     * helper method to handle account created
     * @param newAccount
     * @param context
     */
    private static void onAccountCreated(Account newAccount, Context context) {
        /*
         * Since we've created an account
         */
        EarthQuakeSyncAdapter.configurePeriodicSync(context, SYNC_INTERVAL, SYNC_FLEXTIME);

        /*
         * Without calling setSyncAutomatically, our periodic sync will not be enabled.
         */
        ContentResolver.setSyncAutomatically(newAccount, context.getString(R.string.content_authority), true);

        /*
         * Finally, let's do a sync to get things started
         */
        syncImmediately(context);
    }

    /**
     * initialize the the sync adapter
     * @param context
     */
    public static void initializeSyncAdapter(Context context) {
        getSyncAccount(context);
    }


}
