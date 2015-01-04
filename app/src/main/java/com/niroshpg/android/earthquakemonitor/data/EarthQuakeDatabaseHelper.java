package com.niroshpg.android.earthquakemonitor.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.niroshpg.android.earthquakemonitor.data.EarthQuakeDataContract.QuakesEntry;

/**
 * Manages a local SQLite database for storing earthquakes data.
 *
 * @author niroshpg
 * @since  06/10/2014
 *
 * reference: The content provider implementation from https://github.com/udacity/Sunshine
 */
public class EarthQuakeDatabaseHelper extends SQLiteOpenHelper {

    // If you change the database schema, you must increment the database version.
    private static final int DATABASE_VERSION = 1;

    public static final String DATABASE_NAME = "earthquake.db";

    public EarthQuakeDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        final String SQL_CREATE_QUAKE_TABLE = "CREATE TABLE " + QuakesEntry.TABLE_NAME + " (" +

                QuakesEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +

                // the ID of the location entry associated with this earthquake data
                QuakesEntry.COLUMN_MAG + " REAL NOT NULL, " +
                QuakesEntry.COLUMN_DATETEXT + " TEXT NOT NULL, " +
                QuakesEntry.COLUMN_SHORT_DESC + " TEXT NOT NULL, " +
                QuakesEntry.COLUMN_PLACE + " TEXT NOT NULL," +
                QuakesEntry.COLUMN_ALERT + " TEXT , " +
                QuakesEntry.COLUMN_URL + " TEXT , " +
                QuakesEntry.COLUMN_UPDATED + " TEXT , " +
                QuakesEntry.COLUMN_SIG + " REAL NOT NULL, " +

                QuakesEntry.COLUMN_DEPTH + " REAL NOT NULL, " +
                QuakesEntry.COLUMN_LAT + " REAL NOT NULL, " +

                QuakesEntry.COLUMN_LONG + " REAL NOT NULL, " +
                QuakesEntry.COLUMN_TZ + " REAL NOT NULL, " +

                // To assure the application have just one earthquake entry per moment
                // per location, it's created a UNIQUE constraint with REPLACE strategy
                " UNIQUE (" + QuakesEntry.COLUMN_DATETEXT + ", " +
                QuakesEntry.COLUMN_LAT  + ", "+ QuakesEntry.COLUMN_LONG +") ON CONFLICT REPLACE);";
        sqLiteDatabase.execSQL(SQL_CREATE_QUAKE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
        // This database is only a cache for online data, so its upgrade policy is
        // to simply to discard the data and start over
        // Note that this only fires if you change the version number for your database.
        // It does NOT depend on the version number for your application.
        // If you want to update the schema without wiping data, commenting out the next 2 lines
        // should be your top priority before modifying this method.
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + QuakesEntry.TABLE_NAME);
        onCreate(sqLiteDatabase);
    }
}

