/**
 * Copyright 2015 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.niroshpg.android.earthquakemonitor.push;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.gcm.GcmPubSub;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;
import com.niroshpg.android.earthquakemonitor.MainActivity;
import com.niroshpg.android.earthquakemonitor.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

public class RegistrationIntentService extends IntentService {

    private static final String TAG = MainActivity.class.getSimpleName();

    private static final String[] TOPICS = {"global"};

    public RegistrationIntentService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        try {
            // In the (unlikely) event that multiple refresh operations occur simultaneously,
            // ensure that they are processed sequentially.
            synchronized (TAG) {
                // [START register_for_gcm]
                // Initially this call goes out to the network to retrieve the token, subsequent calls
                // are local.
                // [START get_token]
                InstanceID instanceID = InstanceID.getInstance(this);
                String token = instanceID.getToken(getString(R.string.gcm_sender_id),
                        GoogleCloudMessaging.INSTANCE_ID_SCOPE, null);
                // [END get_token]
                Log.i(TAG, "GCM Registration Token: " + token);

                // TODO: Implement this method to send any registration to your app's servers.
                sendRegistrationToServer(token);

                // Subscribe to topic channels
                subscribeTopics(token);

                // You should store a boolean that indicates whether the generated token has been
                // sent to your server. If the boolean is false, send the token to your server,
                // otherwise your server should have already received the token.
                sharedPreferences.edit().putBoolean(QuickstartPreferences.SENT_TOKEN_TO_SERVER, true).apply();
                // [END register_for_gcm]
            }
        } catch (Exception e) {
            Log.d(TAG, "Failed to complete token refresh", e);
            // If an exception happens while fetching the new token or updating our registration data
            // on a third-party server, this ensures that we'll attempt the update at a later time.
            sharedPreferences.edit().putBoolean(QuickstartPreferences.SENT_TOKEN_TO_SERVER, false).apply();
        }
        // Notify UI that registration has completed, so the progress indicator can be hidden.
        Intent registrationComplete = new Intent(QuickstartPreferences.REGISTRATION_COMPLETE);
        LocalBroadcastManager.getInstance(this).sendBroadcast(registrationComplete);
    }

    /**
     * Persist registration to third-party servers.
     *
     * Modify this method to associate the user's GCM registration token with any server-side account
     * maintained by your application.
     *
     * @param token The new token.
     */
    private void sendRegistrationToServer(String token) {
        String stringUrl = ApplicationConstants.APP_SERVER_URL+"/register";
        ConnectivityManager connMgr = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            try {
                JSONObject polygon = loadPolygon(this);
                postToUrl(stringUrl,token,polygon,5.0,ServerCommand.REGISTER);
            }catch (IOException ex)
            {
                Log.d(TAG, ex.getLocalizedMessage());
            }
        } else {
            // textView.setText("No network connection available.");
        }
    }
    private String postToUrl(String myUrl,String regId,JSONObject polygon,double minMagnitude,
                             ServerCommand command) throws IOException {
        InputStream is = null;
        DataOutputStream os = null;
        // Only display the first 500 characters of the retrieved
        // web page content.
        int len = 500;

        String returnValue = "";

        try {
            URL url = new URL(myUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setInstanceFollowRedirects(false);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            conn.setRequestProperty("charset", "utf-8");
            //conn.setRequestProperty("Content-Length", "" + Integer.toString(urlParameters.getBytes().length));
            conn.setUseCaches(false);

            conn.setReadTimeout(10000 /* milliseconds */);
            conn.setConnectTimeout(15000 /* milliseconds */);
            conn.setRequestMethod("POST");
            conn.setDoInput(true);
            // Starts the query
            conn.connect();

            //Create JSONObject here
            JSONObject jsonParam = new JSONObject();
            jsonParam.put("command", command.toString());
            jsonParam.put("regId", regId);
            if (command == ServerCommand.REGISTER)
            {
                jsonParam.put("polygon", polygon);
                jsonParam.put("minMagnitude", minMagnitude);
            }
            // Send POST output.
            os = new DataOutputStream(conn.getOutputStream ());
            os.write(URLEncoder.encode(jsonParam.toString()).getBytes("UTF-8"));
            os.flush ();
            os.close();

            int statusCode = conn.getResponseCode();
            Log.d(TAG, "The response is: " + statusCode);
            returnValue = String.valueOf(statusCode);

        } catch (JSONException e) {
            e.printStackTrace();
        } finally {
            if (is != null) {
                is.close();
            }
        }

        return returnValue;
    }

    // Reads an InputStream and converts it to a String.
    public String readIt(InputStream stream, int len) throws IOException, UnsupportedEncodingException {
        Reader reader = null;
        reader = new InputStreamReader(stream, "UTF-8");
        char[] buffer = new char[len];
        reader.read(buffer);
        return new String(buffer);
    }
    /**
     * Subscribe to any GCM topics of interest, as defined by the TOPICS constant.
     *
     * @param token GCM token
     * @throws IOException if unable to reach the GCM PubSub service
     */
    // [START subscribe_topics]
    private void subscribeTopics(String token) throws IOException {
        for (String topic : TOPICS) {
            GcmPubSub pubSub = GcmPubSub.getInstance(this);
            pubSub.subscribe(token, "/topics/" + topic, null);
        }
    }

    private JSONObject loadPolygon(Context context)
    {
        JSONObject jsonObject = new JSONObject();
        List<String> list = new ArrayList<>();
        String preferenceName = context.getResources().getString(R.string.REGION_SHARED_PREF_NAME);
        SharedPreferences sharedPref = context.getSharedPreferences(preferenceName, Context.MODE_PRIVATE);
        String polygonListDefault = context.getResources().getString(R.string.preference_polygon_list_default);
        String polygonList = sharedPref.getString(context.getString(R.string.preference_polygon_list), polygonListDefault);

        try {
             jsonObject = new JSONObject(polygonList);

        } catch (JSONException e) {
            e.printStackTrace();
        }

        return jsonObject;
    };

}
