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

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;

import static java.lang.System.currentTimeMillis;

public class StepCounterService extends Service implements SensorEventListener {

    private final  String TAG        = "StepCounterService";
    private IBinder mBinder = null;
    private static boolean isRunning = false;

    private SensorManager mSensorManager;
    private Sensor        mStepSensor;
    private Integer       stepsCounted    = 0;
    private Boolean       haveSetOffset   = false;

    public Integer getStepsCounted() {
        return stepsCounted;
    }

    public void stopTracking() {
        Log.i(TAG, "Setting isRunning flag to false");
        isRunning = false;
        mSensorManager.unregisterListener(this);
    }

    @Override
    public IBinder onBind(Intent intent) {
        mBinder   = new StepCounterServiceBinder();
        Log.i(TAG, "onBind" + intent);
        return mBinder;
    }

    public class StepCounterServiceBinder extends Binder {
        StepCounterService getService() {
            // Return this instance of StepCounterService so clients can call public methods
            return StepCounterService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "onCreate");
        // Do some setup stuff
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "onStartCommand");

        //@TODO should test if startCommand is from autolaunch on boot -> then if yes, check if CordovaStepCounter.ACTION_START has really been called or die
        Log.i(TAG, "- Relaunch service in 1 hour (4.4.2 start_sticky bug ) : ");
        Intent newServiceIntent = new Intent(this,StepCounterService.class);
        AlarmManager aManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        PendingIntent stepIntent = PendingIntent.getService(getApplicationContext(), 10, newServiceIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        //PendingIntent.GetService (ApplicationContext, 10, intent2, PendingIntentFlags.UpdateCurrent);

        aManager.set(AlarmManager.RTC, java.lang.System.currentTimeMillis() + 1000 * 60 * 60, stepIntent);


        if (isRunning /* || has no step sensors */) {
            Log.i(TAG, "Not initialising sensors");
            return Service.START_STICKY;
        }

        Log.i(TAG, "Initialising sensors");
        doInit();

        isRunning = true;
        return Service.START_STICKY;
    }


    public void doInit() {
        Log.i(TAG, "Registering STEP_DETECTOR sensor");
        stepsCounted  = 0;
        haveSetOffset = false;

        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mStepSensor    = mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        mSensorManager.registerListener(this, mStepSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    public boolean stopService(Intent intent) {
        Log.i(TAG, "- Received stop: " + intent);
        //Stop listening to events when stop() is called
        if(isRunning){
            mSensorManager.unregisterListener(this);
        }

        isRunning = false;

        Log.i(TAG, "- Relaunch service in 500ms" );
        //Autorelaunch the service
        //@TODO should test if stopService is called from killing app or from calling stop() method in CordovaStepCounter
        Intent newServiceIntent = new Intent(this,StepCounterService.class);
        AlarmManager aManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        aManager.set(AlarmManager.RTC, java.lang.System.currentTimeMillis() + 500, PendingIntent.getService(this,11,newServiceIntent,0));

        return super.stopService(intent);
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
                haveSetOffset = true;

                //If steps is less thant dayOffset, means that dayOffset is not correct (due to reboot in the middle of the day)
                if(steps < dayOffset){
                    haveSetOffset = false;
                }
            }catch(JSONException err){
                Log.e(TAG,"Exception while getting Object from JSON for "+currentDateString);
            }
        }else{
            // If there is no data, we will have to save offset
            haveSetOffset = false;
        }



        //Counter += 1
        stepsCounted += 1;

        //If offset has not been set or if saved offset is greater than today offset
        if (!haveSetOffset) {
            //Change offset for current count
            dayOffset = steps - daySteps;
            //Add one to steps (=1 if offset not set, or +1 if steps count has been resetted by a phone restart)
            haveSetOffset = true;
            Log.i(TAG, "  * Updated offset: " + dayOffset);
        }

        //First 'steps' is 0 an not 1
        daySteps = (steps+1) - dayOffset;
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
        editor.commit();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
        Log.i(TAG, "onAccuracyChanged: " + sensor);
        Log.i(TAG, "  Accuracy: " + i);
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
}
