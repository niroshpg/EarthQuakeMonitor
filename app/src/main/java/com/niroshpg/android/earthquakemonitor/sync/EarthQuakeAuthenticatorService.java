package com.niroshpg.android.earthquakemonitor.sync;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import com.niroshpg.android.earthquakemonitor.sync.EarthQuakeAuthenticator;

/**
 * The service which allows the sync adapter framework to access the authenticator.
 */
public class EarthQuakeAuthenticatorService extends Service {
    // Instance field that stores the authenticator object
    private EarthQuakeAuthenticator mAuthenticator;

    @Override
    public void onCreate() {
        // Create a new authenticator object
        mAuthenticator = new EarthQuakeAuthenticator(this);
    }

    /*
     * When the system binds to this Service to make the RPC call
     * return the authenticator's IBinder.
     */
    @Override
    public IBinder onBind(Intent intent) {
        return mAuthenticator.getIBinder();
    }
}
