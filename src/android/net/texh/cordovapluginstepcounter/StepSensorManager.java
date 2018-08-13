package net.texh.cordovapluginstepcounter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.annotation.NonNull;
import android.util.Log;

import static android.content.Context.SENSOR_SERVICE;

/**
 * Created by Digitalsunray Media GmbH. On 20.07.2018.
 */
public class StepSensorManager implements SensorEventListener {

    //region Variables

    private final  String TAG = "StepSensorManager";
    private StepChangeListener listener;
    private SensorManager manager;
    private boolean isStarted;

    //endregion

    //region Methods

    @SuppressLint("InlinedApi")
    public void start(@NonNull StepChangeListener listener, @NonNull Context context) {
        try {
            this.listener = listener;

            if(isStarted) {
                Log.w(TAG, "StepSensorManager is already started!");
                return;
            }

            //Let's start the step detector sensor...
            manager = (SensorManager) context.getSystemService(SENSOR_SERVICE);

            if(manager != null) {
                Sensor mStepSensor = manager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
                manager.registerListener(this, mStepSensor, SensorManager.SENSOR_DELAY_FASTEST);

                isStarted = true;

                Log.i(TAG, "STEP_DETECTOR sensor is registered!");
            }
        }
        catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }

    public void stop() {
        try {
             /*
            NOTE:
             If you want to continuously track the number of steps over a long
             period of time, do NOT unregister for this sensor, so that it keeps counting steps in the
             background even when the AP is in suspend mode and report the aggregate count when the AP
             is awake. Application needs to stay registered for this sensor because step counter does not
             count steps if it is not activated.
             */

            isStarted = false;
            listener = null;

            //Let's stop the step detector sensor :(
            if(manager != null) {
                manager.unregisterListener(this);
                Log.i(TAG, "STEP_DETECTOR sensor is unregistered!");
            }
        }
        catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }

    //endregion

    //region Event Handlers

    @Override
    public void onSensorChanged(SensorEvent event) {
        if(listener != null)
            listener.onChanged(event.values[0]);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        Log.i(TAG, "onAccuracyChanged: " + accuracy);
    }

    //endregion
}
