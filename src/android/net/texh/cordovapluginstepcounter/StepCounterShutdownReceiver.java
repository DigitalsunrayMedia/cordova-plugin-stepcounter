package net.texh.cordovapluginstepcounter;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class StepCounterShutdownReceiver extends BroadcastReceiver {

    @SuppressLint("UnsafeProtectedBroadcastReceiver")
    @Override
    public void onReceive(Context context, Intent intent) {
        //This is broadcast when the device is being shut down (completely turned off, not sleeping).
        //Let's save today's step buffer...
        StepCounterHelper.saveDailyBuffer(context);
    }
}