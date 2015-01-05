package com.android.niroshpg.earthquakemonitor;

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.test.AndroidTestCase;

import com.niroshpg.android.earthquakemonitor.data.EarthQuakeDataContract.QuakesEntry;

/**
 * test the content provider
 */
public class TestProvider extends AndroidTestCase {

    public static final String LOG_TAG = TestProvider.class.getSimpleName();

    // brings our database to an empty state
    public void deleteAllRecords() {
        mContext.getContentResolver().delete(
                QuakesEntry.CONTENT_URI,
                null,
                null
        );

        Cursor cursor = mContext.getContentResolver().query(
                QuakesEntry.CONTENT_URI,
                null,
                null,
                null,
                null
        );
        assertEquals(0, cursor.getCount());
        cursor.close();

    }

    // Since we want each test to start with a clean slate, run deleteAllRecords
    // in setUp (called by the test runner before each test).
    public void setUp() {
        deleteAllRecords();
    }

    /**
     * basic insert and read back test
     */
    public void testInsertReadProvider() {
        ContentValues earthquakeValues = TestDb.createEarthquakeValues();

        Uri quakeInsertUri = mContext.getContentResolver()
                .insert(QuakesEntry.CONTENT_URI, earthquakeValues);
        assertTrue(quakeInsertUri != null);

        // A cursor is your primary interface to the query results.
        Cursor quakesCursor = mContext.getContentResolver().query(
                QuakesEntry.CONTENT_URI,  // Table to Query
                null, // leaving "columns" null just returns all the columns.
                null, // cols for "where" clause
                null, // values for "where" clause
                null // columns to group by
        );

        TestDb.validateCursor(quakesCursor, earthquakeValues);

    }

    /**
     * test for getType
     */
    public void testGetType() {
        // content://com.niroshpg.android.earthquakemonitor/quake/
        String type = mContext.getContentResolver().getType(QuakesEntry.CONTENT_URI);
        // vnd.android.cursor.dir/com.niroshpg.android.earthquakemonitor/quake
        assertEquals(QuakesEntry.CONTENT_TYPE, type);

        // content://com.niroshpg.android.earthquakemonitor/quake?startDate=20140612
        String testDate = "20140612";
        type = mContext.getContentResolver().getType(
                QuakesEntry.buildQuakeWithStartDate( testDate));
        // vnd.android.cursor.item/com.niroshpg.android.earthquakemonitor/quake
        assertEquals(QuakesEntry.CONTENT_TYPE, type);

        // content://com.niroshpg.android.earthquakemonitor/quake/1
        type = mContext.getContentResolver().getType(
                QuakesEntry.buildQuakesUri(1L));
        // vnd.android.cursor.item/com.niroshpg.android.earthquakemonitor/query
        assertEquals(QuakesEntry.CONTENT_ITEM_TYPE, type);
    }

    // Make sure we can still delete after adding/updating stuff
    public void testDeleteRecordsAtEnd() {
        deleteAllRecords();
    }

}
