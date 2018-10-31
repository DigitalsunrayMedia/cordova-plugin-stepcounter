package net.texh.cordovapluginstepcounter;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

public class StepCounterBootReceiver extends BroadcastReceiver {

    // Method is called after device bootup is complete
    public void onReceive(final Context context, Intent arg1) {
        try {
            //start service
            Intent stepCounterIntent = new Intent(context, StepCounterService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(stepCounterIntent);
            } else {
                context.startService(stepCounterIntent);
            }
        } catch (Exception e){
            e.printStackTrace();
            Log.e("StepCounter", "StepCounterBootReceiver - Cannot start step counter service.");
        }
    }
    
}