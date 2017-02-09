package net.texh.cordovapluginstepcounter;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class StepCounterBootReceiver extends BroadcastReceiver{

    @Override
    public void onReceive(Context context, Intent intent) {
        Intent stepCounterServiceIntent = new Intent(context,StepCounterService.class);
        context.startService(stepCounterServiceIntent);
    }
}
