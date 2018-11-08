package net.texh.cordovapluginstepcounter;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.SensorManager;

public class StepCounterShutdownReceiver extends BroadcastReceiver {

    @SuppressLint("UnsafeProtectedBroadcastReceiver")
    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            final StepSensorManager manager = new StepSensorManager();
            manager.start(steps -> {
                //We don't need to listen anymore...
                manager.stop();

                //The device is going to be shutdown, let's synchronize the steps...
                StepCounterHelper.saveSteps(steps, context, true);

            }, context, SensorManager.SENSOR_DELAY_FASTEST);
        }
        catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }
}