//package com.niroshpg.android.earthquakemonitor.push;
//
//import android.app.Activity;
//import android.content.ComponentName;
//import android.content.Context;
//import android.content.Intent;
//import android.support.v4.content.WakefulBroadcastReceiver;
//
///**
// * Created by Windows User on 23/07/2015.
// */
//public class GcmBroadcastReceiver extends WakefulBroadcastReceiver {
//
//    @Override
//    public void onReceive(Context context, Intent intent) {
//        ComponentName comp = new ComponentName(context.getPackageName(),
//                GCMNotificationIntentService.class.getName());
//        startWakefulService(context, (intent.setComponent(comp)));
//        setResultCode(Activity.RESULT_OK);
//    }
//}
