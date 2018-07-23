package net.texh.cordovapluginstepcounter;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Created by Digitalsunray Media GmbH. On 19.07.2018.
 */
public class StepCounterHelper {

    //region Static Methods

    public static void saveSteps(float sensorValue, @Nullable Context context, boolean resetOffset) {
        try {
            Integer steps = Math.round(sensorValue);
            Integer daySteps;
            Integer oldDaySteps = 0;
            Integer dayOffset;
            Integer dayBuffer = 0;

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

            } else {
                Calendar calendar = Calendar.getInstance();
                calendar.add(Calendar.DATE, -1);
                String yesterdayDateString = dateFormatter.format(calendar.getTime());

                if(pData.has(yesterdayDateString)) {
                    //Try to fetch the offset from Yesterday data, if any....
                    JSONObject yesterdayData = pData.getJSONObject(yesterdayDateString);
                    dayOffset = yesterdayData.getInt("offset") + yesterdayData.getInt("steps");
                }
                else
                    //Change offset for current count...
                    dayOffset = steps - oldDaySteps;
            }

            //Calculate the today's step ....
            daySteps = steps - dayOffset + dayBuffer;

            //Calculate the total steps...
            Integer stepsCounted = getTotalCount(context);
            stepsCounted += (daySteps - oldDaySteps);
            setTotalCount(context, stepsCounted);

            if(resetOffset) {
                //This will be used, in case of phone restart...
                dayOffset = 0;
                dayBuffer = daySteps;
            }

            //Save calculated values to SharedPreferences
            dayData.put("steps",daySteps);
            dayData.put("offset",dayOffset);
            dayData.put("buffer",dayBuffer);
            pData.put(currentDateString,dayData);

            editor.putString("pedometerData" , pData.toString());
            editor.apply();
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static void saveSteps(float sensorValue, @Nullable Context context) {
        saveSteps(sensorValue, context, false);
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
