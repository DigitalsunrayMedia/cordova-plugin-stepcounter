package net.texh.cordovapluginstepcounter;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Created by Digitalsunray Media GmbH. On 19.07.2018.
 */
class StepCounterHelper {

    //region Constants

    private static final int STEPS_DELTA_THRESHOLD = 20;

    //endregion

    //region Static Methods

    static int saveSteps(float sensorValue, @NonNull Context context, boolean resetOffset) {
        try {
            int steps = Math.round(sensorValue);
            int daySteps;
            int oldDaySteps = 0;
            int dayOffset;
            int dayBuffer = 0;

            Date currentDate = new Date();
            SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

            String currentDateString = dateFormatter.format(currentDate);
            SharedPreferences sharedPref = context.getSharedPreferences("UserData",
                                                                        Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPref.edit();

            JSONObject pData = new JSONObject();
            JSONObject dayData = new JSONObject();
            if(sharedPref.contains("pedometerData")){
                String pDataString = sharedPref.getString("pedometerData","{}");
                pData = new JSONObject(pDataString);
            }

            //Get the data previously stored for today
            if (pData.has(currentDateString)) {
                dayData = pData.getJSONObject(currentDateString);
                dayOffset = dayData.getInt("offset");
                oldDaySteps = dayData.getInt("steps");

                if (dayData.has("buffer")) //Backward compatibility check!
                    dayBuffer = dayData.getInt("buffer");

                //Data validation/correction and normalization...
                int delta = (steps - dayOffset + dayBuffer) - oldDaySteps;
                if(delta > STEPS_DELTA_THRESHOLD) {
                    //We didn't set the offset correctly, let's add some buffer!
                    dayOffset += (delta - 1);
                }
                else
                    if(delta < 0) {
                    //We didn't save day's buffer properly!
                    dayBuffer += (Math.abs(delta) + 1);
                }

            } else {
                Calendar calendar = Calendar.getInstance();
                calendar.add(Calendar.DATE, -1);
                String yesterdayDateString = dateFormatter.format(calendar.getTime());

                if(pData.has(yesterdayDateString)) {
                    //Try to fetch the offset from Yesterday data, if any....
                    JSONObject yesterdayData = pData.getJSONObject(yesterdayDateString);
                    dayOffset = yesterdayData.getInt("offset") +
                                yesterdayData.getInt("steps");

                    if (yesterdayData.has("buffer"))
                        dayBuffer = yesterdayData.getInt("buffer");
                }
                else
                    //Change offset for current count...
                    dayOffset = steps - oldDaySteps;
            }

            //Calculate the today's step ....
            daySteps = steps - dayOffset + dayBuffer;

            //Calculate the total steps...
            int stepsCounted = getTotalCount(context);
            stepsCounted += (daySteps - oldDaySteps);
            setTotalCount(context, stepsCounted);

            if(resetOffset) {
                //This will be used, in case of phone shutdown/reboot...
                dayOffset = 0;
                dayBuffer = daySteps;
            }

            //Save calculated values to SharedPreferences
            dayData.put("steps", daySteps);
            dayData.put("offset", dayOffset);
            dayData.put("buffer", dayBuffer);
            pData.put(currentDateString, dayData);

            editor.putString("pedometerData", pData.toString());
            editor.apply();

            return daySteps;
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }

        return 0;
    }

    static int getTodaySteps(@NonNull Context context){
        Date currentDate = new Date();
        SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        String currentDateString = dateFormatter.format(currentDate);
        SharedPreferences sharedPref = context.getSharedPreferences("UserData", Context.MODE_PRIVATE);

        if(sharedPref.contains("pedometerData")){
            String pDataString = sharedPref.getString("pedometerData","{}");
            try{
                JSONObject pData = new JSONObject(pDataString);
                //Get the information previously stored for today...
                if(pData.has(currentDateString))
                    return pData.getJSONObject(currentDateString).getInt("steps");
            }
            catch (Exception ex){
               ex.printStackTrace();
            }
        }

        return 0;
    }

    static int saveSteps(float sensorValue, @NonNull Context context) {
        return saveSteps(sensorValue, context, false);
    }

    static int getTotalCount(@NonNull Context context){
        Integer totalCount = 0;
        SharedPreferences sharedPref = context.getSharedPreferences("UserData",
                                                                    Context.MODE_PRIVATE);
        if(sharedPref.contains("PEDOMETER_TOTAL_COUNT_PREF"))
            totalCount = sharedPref.getInt("PEDOMETER_TOTAL_COUNT_PREF", 0);

        return totalCount;
    }

    private static void setTotalCount(@NonNull Context context, Integer newValue){
        SharedPreferences sharedPref = context.getSharedPreferences("UserData",
                                                                    Context.MODE_PRIVATE);
        SharedPreferences.Editor sharedPrefEditor = sharedPref.edit();
        sharedPrefEditor.putInt("PEDOMETER_TOTAL_COUNT_PREF", newValue);
        sharedPrefEditor.apply();
    }

    //endregion
}