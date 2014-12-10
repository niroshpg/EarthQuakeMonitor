/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.niroshpg.android.earthquakemonitor;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.niroshpg.android.earthquakemonitor.data.EarthQuakeDataContract;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class Utility {
    public static String getPreferredLocation(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString(context.getString(R.string.pref_location_key),
                context.getString(R.string.pref_location_default));
    }

    static String formatDate(String dateString) {
        Date date = EarthQuakeDataContract.getDateFromDb(dateString);
        return DateFormat.getDateInstance().format(date);
    }

    // Format used for storing dates in the database.  ALso used for converting those strings
    // back into date objects for comparison/processing.
    public static final String DATE_FORMAT = "yyyyMMdd";
    public static final String DATETIME_FORMAT = "yyyyMMddHHmmss";
    public static TimeZone currentTimeZone = Calendar.getInstance().getTimeZone();

    /**
     * Helper method to convert the database representation of the date into something to display
     * to users.  As classy and polished a user experience as "20140102" is, we can do better.
     *
     * @param context Context to use for resource localization
     * @param dateStr The db formatted date string, expected to be of the form specified
     *                in Utility.DATE_FORMAT
     * @return a user-friendly representation of the date.
     */
    public static String getFriendlyDayString(Context context, String dateStr) {
        // The day string for forecast uses the following logic:
        // For today: "Today, June 8"
        // For tomorrow:  "Tomorrow"
        // For the next 5 days: "Wednesday" (just the day name)
        // For all days after that: "Mon Jun 8"

        Date todayDate = new Date();
        String todayStr = EarthQuakeDataContract.getDbDateString(todayDate);
        DateTime inputDateUTC = DateTimeFormat.forPattern(DATETIME_FORMAT).withZone(DateTimeZone.UTC).parseDateTime(dateStr);

       // currentTimeZone.inDaylightTime()
        DateTime inputDateCurrentTZ=  inputDateUTC.toDateTime(DateTimeZone.forOffsetMillis(currentTimeZone.getRawOffset()));

        // If the date we're building the String for is today's date, the format
        // is "Today, June 24"
        if (todayStr.equals(dateStr)) {
            String today = context.getString(R.string.today);
            int formatId = R.string.format_full_friendly_date;
            return String.format(context.getString(
                    formatId,
                    today,
                    getFormattedMonthDay(context, dateStr)));
        } else {

            DateTimeFormatter shortenedDateTimeFormat = DateTimeFormat.forPattern("EEE MMM dd HH:mm:ss");
            return inputDateCurrentTZ.toString(shortenedDateTimeFormat);
//            }
        }
    }

    /**
     * Given a day, returns just the name to use for that day.
     * E.g "today", "tomorrow", "wednesday".
     *
     * @param context Context to use for resource localization
     * @param dateStr The db formatted date string, expected to be of the form specified
     *                in Utility.DATE_FORMAT
     * @return
     */
    public static String getDayName(Context context, String dateStr) {
        SimpleDateFormat dbDateFormat = new SimpleDateFormat(Utility.DATE_FORMAT);
        try {
            Date inputDate = dbDateFormat.parse(dateStr);
            Date todayDate = new Date();
            // If the date is today, return the localized version of "Today" instead of the actual
            // day name.
            if (EarthQuakeDataContract.getDbDateString(todayDate).equals(dateStr)) {
                return context.getString(R.string.today);
            } else {
                // If the date is set for tomorrow, the format is "Tomorrow".
                Calendar cal = Calendar.getInstance();
                cal.setTime(todayDate);
                cal.add(Calendar.DATE, 1);
                Date tomorrowDate = cal.getTime();
                if (EarthQuakeDataContract.getDbDateString(tomorrowDate).equals(
                        dateStr)) {
                    return context.getString(R.string.tomorrow);
                } else {
                    // Otherwise, the format is just the day of the week (e.g "Wednesday".
                    SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE");
                    return dayFormat.format(inputDate);
                }
            }
        } catch (ParseException e) {
            e.printStackTrace();
            // It couldn't process the date correctly.
            return "";
        }
    }

    /**
     * Converts db date format to the format "Month day", e.g "June 24".
     * @param context Context to use for resource localization
     * @param dateStr The db formatted date string, expected to be of the form specified
     *                in Utility.DATE_FORMAT
     * @return The day in the form of a string formatted "December 6"
     */
    public static String getFormattedMonthDay(Context context, String dateStr) {
        SimpleDateFormat dbDateFormat = new SimpleDateFormat(Utility.DATE_FORMAT);
        try {
            Date inputDate = dbDateFormat.parse(dateStr);
            SimpleDateFormat monthDayFormat = new SimpleDateFormat("MMMM dd");
            String monthDayString = monthDayFormat.format(inputDate);
            return monthDayString;
        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static int getIconResourceForAlertCondition(String alert,int sig) {
        // based on alert condition defined by USGS web site
        int resourceId = 0;
        if(alert!= null && !alert.contains("null"))
        {
            if(alert.contains("green"))
            {
                resourceId = R.drawable.ic_clear_green;
            }
            else if (alert.contains("yellow"))
            {
                resourceId =  R.drawable.ic_clear_yellow;
            }
            else if (alert.contains("orange"))
            {
                resourceId =  R.drawable.ic_clear_orange;
            }
            else if (alert.contains("red"))
            {
                resourceId =  R.drawable.ic_clear_red;
            }
            else {
                resourceId = R.drawable.ic_clear;
            }
        }
        else {
            resourceId = R.drawable.ic_clear;
        }
        return resourceId;
    }


    public static int getArtResourceForAlertCondition(String alert,int sig) {
        // reuse icon resource
        if(alert!= null && !alert.contains("null"))
        {
            if(alert.contains("green"))
            {
                return R.drawable.ic_clear_green;
            }
            else if (alert.contains("yellow"))
            {
                return R.drawable.ic_clear_yellow;
            }
            else if (alert.contains("orange"))
            {
                return R.drawable.ic_clear_orange;
            }
            else if (alert.contains("red"))
            {
                return R.drawable.ic_clear_red;
            }
        }
       return R.drawable.ic_clear;
    }

    public static int getSignificance(String key)
    {
        int sig = 0;
        if(key.contains("high"))
        {
            sig = 500;
        }
        else if(key.contains("medium"))
        {
            sig = 400;
        }
        else if(key.contains("low"))
        {
            sig = 250;
        }
        else
            sig = 0;
        return sig;
    }
}
