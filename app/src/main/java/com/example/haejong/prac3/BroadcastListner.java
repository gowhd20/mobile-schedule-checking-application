package com.example.haejong.prac3;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;


/**
 * Created by dhaejong on 19.1.2016.
 */
public class BroadcastListner extends BroadcastReceiver {

    private static final String TAG = "MyActivity";


    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals("android.intent.action.BOOT_COMPLETED")) {
            Intent calIntent = new Intent(context, CalendarService.class);
            context.startService(calIntent);

            Log.i(TAG, "detect a reboot in BroadcastListener.java");
            // Set the alarm here.
        }
    }

}
