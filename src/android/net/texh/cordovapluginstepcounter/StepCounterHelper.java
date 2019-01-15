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

    private static final int STEPS_DELTA_THRESHOLD = 30;

    private static final String DEFAULT_DATE_PATTERN = "yyyy-MM-dd";
    private static final String PREFERENCE_NAME = "UserData";
    private static final String PREF_KEY_PEDOMETER_DATA = "pedometerData";
    private static final String PEDOMETER_DATA_STEPS = "steps";
    private static final String PEDOMETER_DATA_OFFSET = "offset";
    private static final String PEDOMETER_DATA_DAILY_BUFFER = "buffer";

    //endregion

    //region Static Methods

    static int saveSteps(float sensorValue, @NonNull Context context) {
        try {
            int steps = Math.round(sensorValue);
            int daySteps;
            int oldDaySteps = 0;
            int dayOffset;
            int dayBuffer = 0;

            Date currentDate = new Date();
            SimpleDateFormat dateFormatter = new SimpleDateFormat(DEFAULT_DATE_PATTERN , Locale.getDefault());

            String currentDateString = dateFormatter.format(currentDate);
            SharedPreferences sharedPref = context.getSharedPreferences(PREFERENCE_NAME,
                                                                        Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPref.edit();

            JSONObject pData = new JSONObject();
            JSONObject dayData = new JSONObject();
            if(sharedPref.contains(PREF_KEY_PEDOMETER_DATA)){
                String pDataString = sharedPref.getString(PREF_KEY_PEDOMETER_DATA,"{}");
                pData = new JSONObject(pDataString);
            }

            //Get the data previously stored for today
            if (pData.has(currentDateString)) {
                dayData = pData.getJSONObject(currentDateString);
                dayOffset = dayData.getInt(PEDOMETER_DATA_OFFSET);
                oldDaySteps = dayData.getInt(PEDOMETER_DATA_STEPS);

                if (dayData.has(PEDOMETER_DATA_DAILY_BUFFER)) //Backward compatibility check!
                    dayBuffer = dayData.getInt(PEDOMETER_DATA_DAILY_BUFFER);

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
                    dayOffset = yesterdayData.getInt(PEDOMETER_DATA_OFFSET) +
                                yesterdayData.getInt(PEDOMETER_DATA_STEPS);

                    if (yesterdayData.has(PEDOMETER_DATA_DAILY_BUFFER))
                        dayBuffer = yesterdayData.getInt(PEDOMETER_DATA_DAILY_BUFFER);
                }
                else
                    //Change offset for current count...
                    dayOffset = steps - oldDaySteps;
            }

            //Calculate the today's step ....
            daySteps = steps - dayOffset + dayBuffer;

            if(daySteps < 0)
                return oldDaySteps; //Something went wrong, don't save false values!

            //Calculate the total steps...
            int stepsCounted = getTotalCount(context);
            stepsCounted += (daySteps - oldDaySteps);
            setTotalCount(context, stepsCounted);

            //Save calculated values to SharedPreferences
            dayData.put(PEDOMETER_DATA_STEPS, daySteps);
            dayData.put(PEDOMETER_DATA_OFFSET, dayOffset);
            dayData.put(PEDOMETER_DATA_DAILY_BUFFER, dayBuffer);
            pData.put(currentDateString, dayData);

            editor.putString(PREF_KEY_PEDOMETER_DATA, pData.toString());
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
        SimpleDateFormat dateFormatter = new SimpleDateFormat(DEFAULT_DATE_PATTERN, Locale.getDefault());

        String currentDateString = dateFormatter.format(currentDate);
        SharedPreferences sharedPref = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE);

        if(sharedPref.contains(PREF_KEY_PEDOMETER_DATA)){
            String pDataString = sharedPref.getString(PREF_KEY_PEDOMETER_DATA,"{}");
            try{
                JSONObject pData = new JSONObject(pDataString);
                //Get the information previously stored for today...
                if(pData.has(currentDateString))
                    return pData.getJSONObject(currentDateString).getInt(PEDOMETER_DATA_STEPS);
            }
            catch (Exception ex){
                ex.printStackTrace();
            }
        }

        return 0;
    }

    static int getTotalCount(@NonNull Context context){
        Integer totalCount = 0;
        SharedPreferences sharedPref = context.getSharedPreferences(PREFERENCE_NAME,
                                                                    Context.MODE_PRIVATE);
        if(sharedPref.contains("PEDOMETER_TOTAL_COUNT_PREF"))
            totalCount = sharedPref.getInt("PEDOMETER_TOTAL_COUNT_PREF", 0);

        return totalCount;
    }

    private static void setTotalCount(@NonNull Context context, Integer newValue){
        SharedPreferences sharedPref = context.getSharedPreferences(PREFERENCE_NAME,
                                                                    Context.MODE_PRIVATE);
        SharedPreferences.Editor sharedPrefEditor = sharedPref.edit();
        sharedPrefEditor.putInt("PEDOMETER_TOTAL_COUNT_PREF", newValue);
        sharedPrefEditor.apply();
    }

    static void saveDailyBuffer(@NonNull Context context) {
        try {
            //NOTE: this method MUST be used, in case of phone shutdown/reboot...
            Date currentDate = new Date();
            SimpleDateFormat dateFormatter = new SimpleDateFormat(DEFAULT_DATE_PATTERN, Locale.getDefault());

            String currentDateString = dateFormatter.format(currentDate);
            SharedPreferences sharedPref = context.getSharedPreferences(PREFERENCE_NAME,
                                                                        Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPref.edit();

            if(sharedPref.contains(PREF_KEY_PEDOMETER_DATA)){
                JSONObject data = new JSONObject(sharedPref.getString(PREF_KEY_PEDOMETER_DATA,"{}"));
                if (data.has(currentDateString)) {
                    JSONObject dayData = data.getJSONObject(currentDateString);
                    int steps = dayData.getInt(PEDOMETER_DATA_STEPS);

                    if(steps >= 0) {
                        //Save calculated values to the private preferences ...
                        dayData.put(PEDOMETER_DATA_STEPS, steps);
                        dayData.put(PEDOMETER_DATA_OFFSET, 0);
                        dayData.put(PEDOMETER_DATA_DAILY_BUFFER, steps);
                        data.put(currentDateString, dayData);

                        editor.putString(PREF_KEY_PEDOMETER_DATA, data.toString());
                        editor.apply();
                    }
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    //endregion
}