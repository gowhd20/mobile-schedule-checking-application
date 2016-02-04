package com.example.haejong.prac3;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;

import android.content.IntentFilter;
import android.database.Cursor;
import android.media.AudioManager;
import android.net.Uri;
import android.os.IBinder;

import android.os.PowerManager;
import android.provider.CalendarContract;
import android.util.Log;
import android.widget.Toast;

import java.util.Calendar;


/**
 * Created by dhaejong on 19.1.2016.
 */
public class CalendarService extends Service {

    private static final String TAG = "MyActivity";

    public static final String[] INSTANCE_PROJECTION = new String[] {
            CalendarContract.Instances.EVENT_ID,      // 0
            CalendarContract.Instances.BEGIN,         // 1
            CalendarContract.Instances.TITLE,          // 2
            CalendarContract.Instances.END              // 3
    };

    // The indices for the projection array above.

    private static final int PROJECTION_ID_INDEX = 0;
    private static final int PROJECTION_BEGIN_INDEX = 1;
    private static final int PROJECTION_TITLE_INDEX = 2;
    private static final int PROJECTION_END_INDEX = 3;

    private PendingIntent pendingIntent;
    private BroadcastReceiver myBroadcastReceiver;
    private IntentFilter intentFilter;
    private final String USER_ACTION = "android.intent.action.USER_ACTION";
    private Context self = this;



    @Override
    public void onCreate(){

        Log.i(TAG, "CalendarService.java");

        intentFilter = new IntentFilter(USER_ACTION);
        myBroadcastReceiver = new MyBroadcastReceiver();

        registerReceiver(myBroadcastReceiver, intentFilter);
        start(this);
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //TODO do something useful


        return Service.START_NOT_STICKY;
    }


    @Override
    public IBinder onBind(Intent intent) {
        //TODO for communication return IBinder implementation
        return null;
    }




    public void start(Context context) {

        Intent alarmIntent = new Intent(USER_ACTION);
        pendingIntent = PendingIntent.getBroadcast(context, 0, alarmIntent, 0);
        AlarmManager manager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        int interval = 10000; // alarm checking time interval 10 sec

        manager.setInexactRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), interval, pendingIntent);
        Toast.makeText(context, "Alarm Set", Toast.LENGTH_SHORT).show();
        Log.i(TAG, "Alarm set");
    }



    public class MyBroadcastReceiver extends BroadcastReceiver{
        private boolean isEventOn = false;
        private int defaultRingerMode = 3; // null
        private AudioManager myAudioManager;
        long beginTimeOfTheEvent;
        long endTimeOfTheEvent;

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("android.intent.action.USER_ACTION")){

                PowerManager pm = (PowerManager) context.getSystemService(context.POWER_SERVICE);
                PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "");

                wl.acquire();

                Toast.makeText(context, "Checking calendar", Toast.LENGTH_LONG).show(); // For example

                queryEvents(context);

                Log.i(TAG, " Checking calendar"); // For example

                wl.release();
            }
        }

        public void queryEvents(Context context){


            Calendar beginTime = Calendar.getInstance();
            //beginTime.set(2016, 1, -29, -22, 0);  // year, month, days(0 = 30), hours(0 = 22:00), mins
            long startMillis = beginTime.getTimeInMillis();
            Calendar endTime = Calendar.getInstance();
            //endTime.set(2016, 2, -29, -22, 0);  // year, month, days(0 = 30), hours(0 = 22:00), mins

            endTime.add(Calendar.MONTH, 1);
            long endMillis = endTime.getTimeInMillis();

            Cursor cur = null;
            ContentResolver cr = context.getContentResolver();

            Uri.Builder builder = CalendarContract.Instances.CONTENT_URI.buildUpon();
            ContentUris.appendId(builder, startMillis);
            ContentUris.appendId(builder, endMillis);

            // Submit the query
            cur =  cr.query(builder.build(),
                    INSTANCE_PROJECTION,
                    null,
                    null,
                    null);

            while (cur.moveToNext()) {   // can also add condition if isEventOn == false;
                String title = null;
                long eventID = 0;
                long beginVal = 0;
                long endVal = 0;


                // Get the field values
                eventID = cur.getLong(PROJECTION_ID_INDEX);
                beginVal = cur.getLong(PROJECTION_BEGIN_INDEX);
                title = cur.getString(PROJECTION_TITLE_INDEX);
                endVal = cur.getLong(PROJECTION_END_INDEX);

                // found an event is ongoing
                if(onEvent(beginVal, endVal)){

                    this.beginTimeOfTheEvent = beginVal;
                    this.endTimeOfTheEvent = endVal;
                    // get the default ringer mode

                    // if default ringer mode is null
                    if(!this.isEventOn) {
                        Log.i(TAG, "An event found");
                        myAudioManager = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
                        this.isEventOn = true;
                        this.defaultRingerMode = myAudioManager.getRingerMode(); // 0 = mute 1 = vibration 2 = sound

                        if(this.defaultRingerMode != 1){
                            myAudioManager.setRingerMode(1); // set ringer mode as vibration
                        }else{
                            this.defaultRingerMode = 3; // keep as none mode
                        }
                    }

                    // when the event is over
                }else if(!onEvent(this.beginTimeOfTheEvent, this.endTimeOfTheEvent) && this.isEventOn == true){

                    this.isEventOn = false;
                    Log.i(TAG, " event ended");
                    myAudioManager.setRingerMode(this.defaultRingerMode);
                    defaultRingerMode = 3; // ringer mode set to none
                }

            }
        }

        public boolean onEvent(long begin, long end){

            Calendar getTime = Calendar.getInstance();
            long currentTime = getTime.getTimeInMillis();

            if(currentTime >= begin && currentTime <= end){
                return true;
            }else{
                return false;
            }
        }

    }


}
