package com.niroshpg.android.earthquakemonitor.sync;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.niroshpg.android.earthquakemonitor.sync.EarthQuakeSyncAdapter;

public class EarthQuakeSyncService extends Service {
    private static final Object sSyncAdapterLock = new Object();
    private static EarthQuakeSyncAdapter earthQuakeSyncAdapter = null;

    @Override
    public void onCreate() {
        Log.d("EarthQuakeSyncService", "onCreate - EarthQuakeSyncService");
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