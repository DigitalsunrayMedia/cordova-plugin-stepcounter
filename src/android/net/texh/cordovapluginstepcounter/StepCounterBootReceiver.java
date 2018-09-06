package net.texh.cordovapluginstepcounter;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class StepCounterBootReceiver extends BroadcastReceiver{

    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            //start service
            Intent stepCounterIntent = new Intent(context, StepCounterService.class);
            context.startService(stepCounterIntent);
        } catch (Exception e){
            e.printStackTrace();
            Log.e("StepCounter", "StepCounterRestartReceiver - Cannot start step counter service.");
        }
    }
}
