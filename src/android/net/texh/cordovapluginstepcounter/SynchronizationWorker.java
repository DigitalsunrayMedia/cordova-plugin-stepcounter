package net.texh.cordovapluginstepcounter;

import android.support.annotation.NonNull;

import androidx.work.Worker;

/**
 * Created by Digitalsunray Media GmbH. On 19.07.2018.
 */
public class SynchronizationWorker extends Worker implements StepChangeListener {

    //region Variables

    private StepSensorManager stepSensorManager = null;

    //endregion

    //region Methods

    @NonNull
    @Override
    public Result doWork() {
        try {
            //Synchronizing the step counter values with local database...
            stepSensorManager = new StepSensorManager();
            stepSensorManager.start(this, getApplicationContext());

            return Result.SUCCESS;
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }

        return Result.RETRY;
    }

    //endregion

    //region Event Handlers

    @Override
    public void onChanged(float steps) {
        //Save the new steps locally...
        StepCounterHelper.saveSteps(steps, getApplicationContext());
    }

    //endregion
}
