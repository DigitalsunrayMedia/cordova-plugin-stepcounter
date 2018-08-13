package net.texh.cordovapluginstepcounter;

import java.util.concurrent.TimeUnit;

import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

/**
 * Created by Digitalsunray Media GmbH. On 20.07.2018.
 */
public class SynchronizationManager {

    //region Methods

    public static void start() {
        try {
            //Start synchronization worker every 15 minutes...
            //NOTE: Minimum time for periodic requests is 15 minutes (mentioned it in the IO 18)
            PeriodicWorkRequest.Builder synchWorkerRequest = new PeriodicWorkRequest.Builder(SynchronizationWorker.class,
                                                                                            15,
                                                                                            TimeUnit.MINUTES)
                                                                                            .addTag(SynchronizationWorker.class.getSimpleName());

            // Create the actual work object:
            PeriodicWorkRequest synchWorker = synchWorkerRequest.build();

            //enqueue the recurring task:
            WorkManager.getInstance().enqueueUniquePeriodicWork(SynchronizationWorker.class.getSimpleName(),
                                                                ExistingPeriodicWorkPolicy.KEEP,
                                                                synchWorker);
        }
        catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }

    public static void cancel() {
        try {
            //Cancel all available synchronization work(s)...
            WorkManager.getInstance().cancelAllWorkByTag(SynchronizationWorker.class.getSimpleName());
        }
        catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }

    //endregion
}
