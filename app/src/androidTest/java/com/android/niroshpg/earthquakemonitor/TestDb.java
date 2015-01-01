package com.android.niroshpg.earthquakemonitor;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.test.AndroidTestCase;

import com.niroshpg.android.earthquakemonitor.data.EarthQuakeDataContract;
import com.niroshpg.android.earthquakemonitor.data.EarthQuakeDataContract.QuakesEntry;
import com.niroshpg.android.earthquakemonitor.data.EarthQuakeDatabaseHelper;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.Map;
import java.util.Set;

/**
 * test SQLLite database directly
 */
public class TestDb extends AndroidTestCase {

    public static final String LOG_TAG = TestDb.class.getSimpleName();

    public void testCreateDb() throws Throwable {
        mContext.deleteDatabase(EarthQuakeDatabaseHelper.DATABASE_NAME);
        SQLiteDatabase db = new EarthQuakeDatabaseHelper(
                this.mContext).getWritableDatabase();
        assertEquals(true, db.isOpen());
        db.close();
    }

    public void testInsertReadDb() {
        // If there's an error in those massive SQL table creation Strings,
        // errors will be thrown here when you try to get a writable database.
        EarthQuakeDatabaseHelper dbHelper = new EarthQuakeDatabaseHelper(mContext);
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        // Add earthquake event dummy values
        ContentValues quakesValues = createEarthquakeValues();

        long quakeRowId = db.insert(QuakesEntry.TABLE_NAME, null, quakesValues);
        assertTrue(quakeRowId != -1);

        // A cursor is your primary interface to the query results.
        Cursor quakesCursor = db.query(
                QuakesEntry.TABLE_NAME,  // Table to Query
                null, // leaving "columns" null just returns all the columns.
                null, // cols for "where" clause
                null, // values for "where" clause
                null, // columns to group by
                null, // columns to filter by row groups
                null  // sort order
        );

        validateCursor(quakesCursor, quakesValues);

        dbHelper.close();
    }

    static ContentValues createEarthquakeValues() {
        ContentValues quakeValues = new ContentValues();
        quakeValues.put(QuakesEntry.COLUMN_MAG, 5.5);
        quakeValues.put(QuakesEntry.COLUMN_DATETEXT, EarthQuakeDataContract.getDbDateString(new DateTime(DateTime.now(), DateTimeZone.UTC)));
        quakeValues.put(QuakesEntry.COLUMN_TZ, "UTC");
        quakeValues.put(QuakesEntry.COLUMN_PLACE, "Sydney");
        quakeValues.put(QuakesEntry.COLUMN_LAT, -33.0);
        quakeValues.put(QuakesEntry.COLUMN_LONG, 150.0);
        quakeValues.put(QuakesEntry.COLUMN_DEPTH, 10);
        quakeValues.put(QuakesEntry.COLUMN_ALERT, "green");
        quakeValues.put(QuakesEntry.COLUMN_SIG, 300);
        quakeValues.put(QuakesEntry.COLUMN_UPDATED, EarthQuakeDataContract.getDbDateString(new DateTime(DateTime.now(), DateTimeZone.UTC )));
        quakeValues.put(QuakesEntry.COLUMN_URL, "http://www.google.com.au");
        quakeValues.put(QuakesEntry.COLUMN_SHORT_DESC, "Sydney"+","+5.5+","+10);

        return quakeValues;
    }

    static void validateCursor(Cursor valueCursor, ContentValues expectedValues) {

        assertTrue(valueCursor.moveToFirst());

        Set<Map.Entry<String, Object>> valueSet = expectedValues.valueSet();
        for (Map.Entry<String, Object> entry : valueSet) {
            String columnName = entry.getKey();
            int idx = valueCursor.getColumnIndex(columnName);
            assertFalse(idx == -1);
            String expectedValue = entry.getValue().toString();
            assertEquals(expectedValue, valueCursor.getString(idx));
        }
        valueCursor.close();
    }
}