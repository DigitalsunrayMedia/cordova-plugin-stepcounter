package net.texh.cordovapluginstepcounter;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

public class StepCounterBootReceiver extends BroadcastReceiver {

    //This method is called after device booting is complete!
    @SuppressLint("UnsafeProtectedBroadcastReceiver")
    public void onReceive(final Context context, Intent arg1) {
        try {
            //Start the step counter service...
            Intent stepCounterIntent = new Intent(context, StepCounterService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                context.startForegroundService(stepCounterIntent);
            else
                context.startService(stepCounterIntent);

        } catch (Exception e){
            Log.e("StepCounterService", "StepCounterBootReceiver: Cannot start step counter service.");
        }
    }
}
