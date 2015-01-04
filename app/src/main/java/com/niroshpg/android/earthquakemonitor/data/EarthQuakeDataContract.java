package com.niroshpg.android.earthquakemonitor.data;

import android.content.ContentUris;
import android.net.Uri;
import android.provider.BaseColumns;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Defines table and column names for the earthquakes database.
 *
 * @author niroshpg
 * @since  06/10/2014
 *
 * reference: The content provider implementation from https://github.com/udacity/Sunshine
 */
public class EarthQuakeDataContract {

    // The "Content authority" is a name for the entire content provider, similar to the
    // relationship between a domain name and its website.  A convenient string to use for the
    // content authority is the package name for the app, which is guaranteed to be unique on the
    // device.
    public static final String CONTENT_AUTHORITY = "com.niroshpg.android.earthquakemonitor";

    // Use CONTENT_AUTHORITY to create the base of all URI's which apps will use to contact
    // the content provider.
    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

    // Possible paths (appended to base content URI for possible URI's)
    public static final String PATH_QUAKE = "quake";

    // Format used for storing dates in the database.  ALso used for converting those strings
    // back into date objects for comparison/processing.
    public static final String DATE_FORMAT = "yyyyMMddHHmmss";

    /**
     * Converts Date class to a string representation, used for easy comparison and database lookup.
     * @param date The input date
     * @return a DB-friendly representation of the date, using the format defined in DATE_FORMAT.
     */
    public static String getDbDateString(DateTime date){
        // Because the API returns a unix timestamp (measured in seconds),
        // it must be converted to milliseconds in order to be converted to valid date.
        //SimpleDateFormat sdf = new SimpleDateTiFormat(DATE_FORMAT);
        DateTimeFormatter dbDateFormat = DateTimeFormat.forPattern(DATE_FORMAT);
        return dbDateFormat.print(date);
    }

    /**
     * Converts a dateText to a long Unix time representation
     * @param dateText the input date string
     * @return the Date object
     */
    public static DateTime getDateTimeFromDb(String dateText) {
        DateTimeFormatter dbDateFormat = DateTimeFormat.forPattern(DATE_FORMAT);
        return dbDateFormat.parseDateTime(dateText);
    }

    /**
     * Converts a dateText to a long Unix time representation
     * @param dateText the input date string
     * @return the Date object
     */
    public static Date getDateFromDb(String dateText) {
        SimpleDateFormat dbDateFormat = new SimpleDateFormat(DATE_FORMAT);
        try {
            return dbDateFormat.parse(dateText);
        } catch ( ParseException e ) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     *
     * @param date
     * @return
     */
    public static String getDbDateString(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
        return sdf.format(date);
    }

    /* Inner class that defines the table contents of the quakes table */
    public static final class QuakesEntry implements BaseColumns {

        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_QUAKE).build();

        public static final String CONTENT_TYPE =
                "vnd.android.cursor.dir/" + CONTENT_AUTHORITY + "/" + PATH_QUAKE;
        public static final String CONTENT_ITEM_TYPE =
                "vnd.android.cursor.item/" + CONTENT_AUTHORITY + "/" + PATH_QUAKE;

        public static final String TABLE_NAME = "quake";

        // Date, stored as Text with format yyyy-MM-dd
        public static final String COLUMN_DATETEXT = "date";
        // Quakes id as returned by API, to identify the icon to be used
        public static final String COLUMN_QUAKES_ID = "quakes_id";

        // Short description and long description of the earthquake event, as provided by USGS server.
        public static final String COLUMN_SHORT_DESC = "short_desc";

        // magnitude of the earthquake
        public static final String COLUMN_MAG = "mag";

        // alert level (red, orange, yellow, green and black) defined by USGS
        public static final String COLUMN_ALERT = "alert";

        // significance level (0 ~ 1000) defined by USGS
        public static final String COLUMN_SIG = "sig";

        // url for accessing the event in the USGS website
        public static final String COLUMN_URL = "url";

        // timestamp for the last update
        public static final String COLUMN_UPDATED = "updated";

        // place of the earthquake occured
        public static final String COLUMN_PLACE = "place";

        // depth of the earthquake
        public static final String COLUMN_DEPTH = "depth";

        // latitude of the event location
        public static final String COLUMN_LAT = "lat";

        // longitude of the event location
        public static final String COLUMN_LONG = "long";

        // timezone of the event location
        public static final String COLUMN_TZ= "tz";

        /**
         * build uri with id
         * @param id
         * @return
         */
        public static Uri buildQuakesUri(long id) {
            return ContentUris.withAppendedId(CONTENT_URI, id);
        }

        /**
         * build uri with start date
         * @param startDate
         * @return
         */
        public static Uri buildQuakeWithStartDate(String startDate) {
            return CONTENT_URI.buildUpon()
                    .appendQueryParameter(COLUMN_DATETEXT, startDate).build();
        }

        /**
         * get id from the uri
         * @param uri
         * @return
         */
        public static String getIdFromUri(Uri uri) {
            return uri.getPathSegments().get(1);
        }

    }
}
