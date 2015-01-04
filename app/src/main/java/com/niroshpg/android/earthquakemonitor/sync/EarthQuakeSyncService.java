package com.niroshpg.android.earthquakemonitor.sync;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

/**
 * sync adapter service
 */
public class EarthQuakeSyncService extends Service {
    private static final Object sSyncAdapterLock = new Object();
    private static EarthQuakeSyncAdapter earthQuakeSyncAdapter = null;

    @Override
    public void onCreate() {
        synchronized (sSyncAdapterLock) {
            if (earthQuakeSyncAdapter == null) {
                earthQuakeSyncAdapter = new EarthQuakeSyncAdapter(getApplicationContext(), true);
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return earthQuakeSyncAdapter.getSyncAdapterBinder();
    }
}