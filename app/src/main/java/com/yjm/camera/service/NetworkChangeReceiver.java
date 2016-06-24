package com.yjm.camera.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

/**
 * Created by Liang on 2016/5/25.
 */
public class NetworkChangeReceiver extends BroadcastReceiver {
    private static final String TAG = "NetworkChangeReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        ConnectivityManager connectionManager = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectionManager.getActiveNetworkInfo();
        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(context);
        if (networkInfo != null && networkInfo.isAvailable()) {
//            localBroadcastManager.sendBroadcast(new Intent("com.yjm.camera.RESTART_LISTEN"));
            Log.i(TAG, "网络可用");
        } else {
            Log.i(TAG, "网络不可用");
//            localBroadcastManager.sendBroadcast(new Intent("com.yjm.camera.STOP_LISTEN"));
        }
    }

}
