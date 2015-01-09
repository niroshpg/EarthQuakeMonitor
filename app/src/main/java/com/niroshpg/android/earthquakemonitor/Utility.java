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
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;

import com.niroshpg.android.earthquakemonitor.data.EarthQuakeDataContract;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/**
 * Utility class for groping helper methods used across the classes
 *
 * @author niroshpg
 * @since  06/10/2014
 */
public class Utility {

    // Format used for storing dates in the database.  ALso used for converting those strings
    // back into date objects for comparison/processing.
    public static final String DATE_FORMAT = "yyyyMMdd";

    public static final String DATETIME_FORMAT = "yyyyMMddHHmmss";

    public static TimeZone currentTimeZone = Calendar.getInstance().getTimeZone();

    public static final int STROKE_WIDTH_ADJUST = 15;
    public static final int PATH_EFFECT_ADJUST = 10;


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
        // The day string for earthquake events uses the following logic:
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

    /**
     * Get icon for alert
     * @param alert
     * @param sig
     * @return
     */
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

    /**
     * get art resource for alert
     * @param alert
     * @param sig
     * @return
     */
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

    /**
     * get marker icon for alert
     * @param alert
     * @return
     */
    public static int getMarkerResourceForAlertCondition(String alert) {
        // based on alert condition defined by USGS web site
        int resourceId = 0;
        if(alert!= null && !alert.contains("null"))
        {
            if(alert.contains("green"))
            {
                resourceId = R.drawable.ic_marker_green;
            }
            else if (alert.contains("yellow"))
            {
                resourceId =  R.drawable.ic_marker_yellow;
            }
            else if (alert.contains("orange"))
            {
                resourceId =  R.drawable.ic_marker_orange;
            }
            else if (alert.contains("red"))
            {
                resourceId =  R.drawable.ic_marker_red;
            }
            else {
                resourceId = R.drawable.ic_marker;
            }
        }
        else {
            resourceId = R.drawable.ic_marker;
        }
        return resourceId;
    }

    /**
     * get marker icon for significance
     * @param sig
     * @return
     */
    public static int getMarkerResourceForSignificance(int sig)
    {
        // based on significance defined by USGS web site
        int resourceId;
        if(sig > 500 && sig <=1000)
        {
            resourceId = R.drawable.ic_marker_red;
        }
        else if (sig > 400  && sig <=500)
        {
            resourceId = R.drawable.ic_marker_orange;
        }
        else if (sig > 300  && sig <=400)
        {
            resourceId = R.drawable.ic_marker_yellow;
        }
        else if (sig > 200  && sig <=300)
        {
            resourceId = R.drawable.ic_marker_green;
        }
        else
        {
            resourceId = R.drawable.ic_marker;
        }
        return resourceId;
    }

    /**
     * add alert data to the specified resource
     * @param context
     * @param drawableId
     * @param alert
     * @return
     */
    public static Bitmap addAlertData(Context context, int drawableId, String alert) {
        Bitmap bm = BitmapFactory.decodeResource(context.getResources(), drawableId)
                .copy(Bitmap.Config.ARGB_8888, true);
        Paint paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(getColorForAlert(alert));

        paint.setAntiAlias(true);
        Canvas canvas = new Canvas(bm);
        final double scale = 0.9;
        int width = canvas.getWidth();
        int height = (int)(canvas.getHeight()*scale);
        paint.setStrokeWidth(height/STROKE_WIDTH_ADJUST);

        /**
         * to make alert ring to be totted with intervals correlated to the device size
         * factor derived from canvas width measurement is used here
         */
        paint.setPathEffect(
                new DashPathEffect( new float[] {
                                    2.0f *( width / PATH_EFFECT_ADJUST ),
                                    1.0f *( width / PATH_EFFECT_ADJUST )}, 0)
                );
        int r = (int)Math.min(width*scale/2,height*scale/2);
        int xc=width/2;
        int yc=height/2;
        canvas.drawCircle(xc,yc,r,paint);

        return  bm;
    }

    /**
     * get color for alert
     * @param alert
     * @return
     */
    private static int getColorForAlert(String alert)
    {
        int color;

        if(alert.contains("green"))
        {
            color = Color.GREEN;
        }
        else if (alert.contains("yellow"))
        {
            color = Color.YELLOW;
        }
        else if (alert.contains("orange"))
        {
            color =  Color.rgb(0xFF, 0xA5, 0x00);
        }
        else if (alert.contains("red"))
        {
            color =  Color.RED;
        }
        else {
            color = Color.BLACK;
        }
        return color;

    }

    /**
     * get significance for key
     * @param key
     * @return
     */
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
