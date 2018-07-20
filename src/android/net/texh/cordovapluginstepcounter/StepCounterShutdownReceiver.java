package net.texh.cordovapluginstepcounter;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class StepCounterShutdownReceiver extends BroadcastReceiver {

    @SuppressLint("UnsafeProtectedBroadcastReceiver")
    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            StepSensorManager manager = new StepSensorManager();
            manager.start(steps -> {
                //The device is going to be shutdown, let's synchronize the steps...
                StepCounterHelper.saveSteps(steps, context, true);

                //We don't need to listen anymore...
                manager.stop();
            }, context);
        }
        catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }
}
