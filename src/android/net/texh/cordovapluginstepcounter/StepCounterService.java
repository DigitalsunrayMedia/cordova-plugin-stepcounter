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

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

public class StepCounterService extends Service implements StepChangeListener {

    //region Variables

    private final  String TAG = "StepCounterService";
    private static boolean isRunning = false;
    private StepSensorManager stepSensorManager;

    //endregion

    //region Service Methods

    @Override
    public IBinder onBind(Intent intent) {
        IBinder mBinder = new StepCounterServiceBinder();

        SynchronizationManager.cancel();

        //Start listening to the corresponding sensor (STEP_DETECTOR) ...
        doInit();

        return mBinder;
    }

    public void doInit() {
        if(isRunning) {
            Log.w(TAG, "This service is already started!");
            return;
        }

        Log.i(TAG, "Registering STEP_DETECTOR sensor");

        stepSensorManager = new StepSensorManager();
        stepSensorManager.start(this, this);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        //Stop listening to the sensor and start synchronization worker...
        stopTracking();

        return super.onUnbind(intent);
    }

    public void stopTracking() {
        Log.i(TAG, "Setting isRunning flag to false");

        try {
            if (stepSensorManager != null)
                stepSensorManager.stop();

            //Start synchronization worker ...
            SynchronizationManager.start();
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }

        isRunning = false;
    }

    //endregion

    //region Sensor Event Handlers

    @Override
    public void onChanged(float steps) {
        //Step history changed, let's save it...
        StepCounterHelper.saveSteps(steps, this);
    }

    //endregion

    //region Binder Class

    class StepCounterServiceBinder extends Binder {
        StepCounterService getService() {
            // Return this instance of StepCounterService so clients can call public methods
            return StepCounterService.this;
        }
    }

    //endregion
}
