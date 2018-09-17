package net.texh.cordovapluginstepcounter;

/*
    Copyright 2015 Jarrod Linahan <jarrod@texh.net>

    Permission is hereby granted, free of charge, to any person obtaining
    a copy of this software and associated documentation files (the
    "Software"), to deal in the Software without restriction, including
    without limitation the rights to use, copy, modify, merge, publish,
    distribute, sublicense, and/or sell copies of the Software, and to
    permit persons to whom the Software is furnished to do so, subject to
    the following conditions:

    The above copyright notice and this permission notice shall be
    included in all copies or substantial portions of the Software.

    THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
    EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
    MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
    NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
    LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
    OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
    WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

 */

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.RemoteViews;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;

public class StepCounterService extends Service implements SensorEventListener {

    //region Constants

    private static final int NOTIFICATION_ID = 777;

    //endregion

    //region Variables

    private final String TAG = "StepCounterService";
    private static boolean isRunning = false;
    private SensorManager mSensorManager;
    private NotificationCompat.Builder builder;
    private Integer stepsCounted = 0;

    //endregion

    //region Service Methods

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "onStartCommand");

        if (isRunning /* || has no step sensors */) {
            Log.i(TAG, "Not initialising sensors");
            return Service.START_STICKY;
        }

        Log.i(TAG, "Initialising sensors");
        doInit();

        isRunning = true;
        return Service.START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        IBinder mBinder = new StepCounterServiceBinder();
        Log.i(TAG, "onBind" + intent);
        return mBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "onCreate");
        // Do some setup stuff
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.i(TAG, "onUnbind");
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy(){
        Log.i(TAG, "onDestroy");
    }

    @Override
    public boolean stopService(Intent intent) {
        Log.i(TAG, "- Received stop: " + intent);
        //Stop listening to events when stop() is called
        if(isRunning){
            mSensorManager.unregisterListener(this);
        }

        isRunning = false;

        return super.stopService(intent);
    }

    //endregion

    public Integer getStepsCounted() {
        return stepsCounted;
    }

    public void stopTracking() {
        Log.i(TAG, "Setting isRunning flag to false");
        isRunning = false;
        mSensorManager.unregisterListener(this);
    }

    public void doInit() {
        if(isRunning){
            return;
        }

        Log.i(TAG, "Registering STEP_DETECTOR sensor");
        stepsCounted  = 0;

        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        Sensor mStepSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);

        mSensorManager.registerListener(this, mStepSensor, SensorManager.SENSOR_DELAY_NORMAL);

        startForegroundService();
    }

    /* Used to build and start foreground service. */
    private void startForegroundService()
    {
        Log.d(TAG, "Start foreground service.");

        String channelId = "StepCounterChannel";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createChannel();
        }
        builder = new NotificationCompat.Builder(this, channelId);

        builder.setSmallIcon(getResources().getIdentifier("notification_icon", "drawable", getPackageName()));
        builder.setAutoCancel(false);
        builder.setOngoing(true);
        builder.setPriority(Notification.PRIORITY_MAX);
        builder.setVisibility(Notification.VISIBILITY_PUBLIC);

        //handle notification click, open main activity
        Intent intent = getPackageManager().getLaunchIntentForPackage(getPackageName());
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 1110, intent,   PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(pendingIntent);

        //custom notification ui
        RemoteViews views = new RemoteViews(getPackageName(), getResources().getIdentifier("sticky_notification", "layout", getPackageName()));
        views.setTextViewText(getResources().getIdentifier("tvSteps", "id", getPackageName()), getTodaySteps());
        builder.setCustomContentView(views);

        Notification notification = builder.build();

        // Start foreground service.
        startForeground(NOTIFICATION_ID, notification);
    }

    private void updateNotification(){
        Log.d(TAG, "update notification");

        RemoteViews views = new RemoteViews(getPackageName(), getResources().getIdentifier("sticky_notification", "layout", getPackageName()));
        views.setTextViewText(getResources().getIdentifier("tvSteps", "id", getPackageName()), getTodaySteps());
        builder.setCustomContentView(views);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {

        //Log.i(TAG, "onSensorChanged event!");
        Integer steps = Math.round(sensorEvent.values[0]);

        Integer daySteps = 0;
        Integer dayOffset = 0;

        Date currentDate = new Date();
        SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd");

        String currentDateString = dateFormatter.format(currentDate);
        SharedPreferences sharedPref = getSharedPreferences("UserData", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();

        JSONObject pData = new JSONObject();
        JSONObject dayData = new JSONObject();
        if(sharedPref.contains("pedometerData")){
            String pDataString = sharedPref.getString("pedometerData","{}");
            try{
                pData = new JSONObject(pDataString);
                Log.d(TAG," got json shared prefs "+pData.toString());
            }catch (JSONException err){
                Log.d(TAG," Exception while parsing json string : "+pDataString);
            }
        }

        //Get the datas previously stored for today
        if(pData.has(currentDateString)){
            try {
                dayData = pData.getJSONObject(currentDateString);
                dayOffset = dayData.getInt("offset");
                daySteps = dayData.getInt("steps");

            }catch(JSONException err){
                Log.e(TAG,"Exception while getting Object from JSON for "+currentDateString);
            }
        }

        //Counter += 1
        stepsCounted += 1;

        //First 'steps' is 0 an not 1
        daySteps += steps;

        //Log all this
        Log.i(TAG, "** daySteps :"+ daySteps+" ** stepCounted :"+stepsCounted);


        //Save calculated values to SharedPreferences
        try{
            dayData.put("steps",daySteps);
            dayData.put("offset",dayOffset);
            pData.put(currentDateString,dayData);
        }catch (JSONException err){
            Log.e(TAG,"Exception while setting int in JSON for "+currentDateString);
        }

        editor.putString("pedometerData",pData.toString());
        editor.apply();

        updateNotification();
    }

    private String getTodaySteps(){
        Integer daySteps = 0;

        Date currentDate = new Date();
        SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd");

        String currentDateString = dateFormatter.format(currentDate);
        SharedPreferences sharedPref = getSharedPreferences("UserData", Context.MODE_PRIVATE);

        JSONObject pData = new JSONObject();
        JSONObject dayData;
        if(sharedPref.contains("pedometerData")){
            String pDataString = sharedPref.getString("pedometerData","{}");
            try{
                pData = new JSONObject(pDataString);
                Log.d(TAG," got json shared prefs "+pData.toString());
            } catch (JSONException err){
                Log.d(TAG," Exception while parsing json string : "+pDataString);
            }
        }

        //Get the datas previously stored for today
        if(pData.has(currentDateString)){
            try {
                dayData = pData.getJSONObject(currentDateString);
                daySteps = dayData.getInt("steps");
            } catch(JSONException err){
                Log.e(TAG,"Exception while getting Object from JSON for "+currentDateString);
            }
        }

        String stepsString = String.format("%,d", daySteps);

        return stepsString;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
        Log.i(TAG, "onAccuracyChanged: " + sensor);
        Log.i(TAG, "  Accuracy: " + i);
    }

    class StepCounterServiceBinder extends Binder {
        StepCounterService getService() {
            // Return this instance of StepCounterService so clients can call public methods
            return StepCounterService.this;
        }
    }

    @NonNull
    @TargetApi(26)
    private String createChannel() {
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        String name = "StepCounter";
        int importance = NotificationManager.IMPORTANCE_LOW;

        NotificationChannel mChannel = new NotificationChannel("StepCounterChannel", name, importance);

        mChannel.enableLights(true);
        mChannel.setLightColor(Color.BLUE);
        if (mNotificationManager != null) {
            mNotificationManager.createNotificationChannel(mChannel);
        } else {
            stopSelf();
        }
        return "StepCounterChannel";
    }

}
