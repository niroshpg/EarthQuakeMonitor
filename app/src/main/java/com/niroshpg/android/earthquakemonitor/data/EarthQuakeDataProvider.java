package com.niroshpg.android.earthquakemonitor.data;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

public class EarthQuakeDataProvider extends ContentProvider {

    // The URI Matcher used by this content provider.
    private static final UriMatcher sUriMatcher = buildUriMatcher();
    private EarthQuakeDatabaseHelper mOpenHelper;


    private static final int QUAKE = 400;
    private static final int QUAKE_WITH_ID = 401;


    private static final String sQuakeIdSelection =
            EarthQuakeDataContract.QuakesEntry.TABLE_NAME+
                    "." + EarthQuakeDataContract.QuakesEntry._ID + " = ? ";

    private static UriMatcher buildUriMatcher() {
        // I know what you're thinking.  Why create a UriMatcher when you can use regular
        // expressions instead?  Because you're not crazy, that's why.

        // All paths added to the UriMatcher have a corresponding code to return when a match is
        // found.  The code passed into the constructor represents the code to return for the root
        // URI.  It's common to use NO_MATCH as the code for this case.
        final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
        final String authority = EarthQuakeDataContract.CONTENT_AUTHORITY;

        // For each type of URI you want to add, create a corresponding code.
        matcher.addURI(authority, EarthQuakeDataContract.PATH_QUAKE, QUAKE);
        matcher.addURI(authority, EarthQuakeDataContract.PATH_QUAKE + "/*", QUAKE_WITH_ID);

        return matcher;
    }

    @Override
    public boolean onCreate() {
        mOpenHelper = new EarthQuakeDatabaseHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {
        // Here's the switch statement that, given a URI, will determine what kind of request it is,
        // and query the database accordingly.
        Cursor retCursor;
        switch (sUriMatcher.match(uri)) {
            // "quake"
            case QUAKE_WITH_ID: {
                String id = EarthQuakeDataContract.QuakesEntry.getIdFromUri(uri);

                retCursor = mOpenHelper.getReadableDatabase().query(
                        EarthQuakeDataContract.QuakesEntry.TABLE_NAME,
                        projection,
                        sQuakeIdSelection,
                        new String[]{id},
                        null,
                        null,
                        sortOrder
                );
                break;
            }
            case QUAKE: {
                retCursor = mOpenHelper.getReadableDatabase().query(
                        EarthQuakeDataContract.QuakesEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder
                );
                break;
            }

            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        retCursor.setNotificationUri(getContext().getContentResolver(), uri);
        return retCursor;
    }

    @Override
    public String getType(Uri uri) {

        // Use the Uri Matcher to determine what kind of URI this is.
        final int match = sUriMatcher.match(uri);

        switch (match) {

            case QUAKE_WITH_ID:
                return EarthQuakeDataContract.QuakesEntry.CONTENT_TYPE;
            case QUAKE:
                return EarthQuakeDataContract.QuakesEntry.CONTENT_TYPE;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        Uri returnUri;

        switch (match) {
            case QUAKE_WITH_ID: {
                long _id = db.insert(EarthQuakeDataContract.QuakesEntry.TABLE_NAME, null, values);
                if ( _id > 0 )
                    returnUri = EarthQuakeDataContract.QuakesEntry.buildQuakesUri(_id);
                else
                    throw new android.database.SQLException("Failed to insert row into " + uri);
                break;
            }
            case QUAKE: {
                long _id = db.insert(EarthQuakeDataContract.QuakesEntry.TABLE_NAME, null, values);
                if ( _id > 0 )
                    returnUri = EarthQuakeDataContract.QuakesEntry.buildQuakesUri(_id);
                else
                    throw new android.database.SQLException("Failed to insert row into " + uri);
                break;
            }
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return returnUri;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        int rowsDeleted;
        switch (match) {
            case QUAKE_WITH_ID:
                rowsDeleted = db.delete(
                        EarthQuakeDataContract.QuakesEntry.TABLE_NAME, selection, selectionArgs);
                break;
            case QUAKE:
                rowsDeleted = db.delete(
                        EarthQuakeDataContract.QuakesEntry.TABLE_NAME, selection, selectionArgs);
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        // Because a null deletes all rows
        if (selection == null || rowsDeleted != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return rowsDeleted;
    }

    @Override
    public int update(
            Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        int rowsUpdated;

        switch (match) {
            case QUAKE_WITH_ID:
                rowsUpdated = db.update(EarthQuakeDataContract.QuakesEntry.TABLE_NAME, values, selection,
                        selectionArgs);
                break;
            case QUAKE:
                rowsUpdated = db.update(EarthQuakeDataContract.QuakesEntry.TABLE_NAME, values, selection,
                        selectionArgs);
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        if (rowsUpdated != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return rowsUpdated;
    }

    @Override
    public int bulkInsert(Uri uri, ContentValues[] values) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case QUAKE:

                int returnCount = 0;
                db.beginTransaction();
                returnCount = 0;
                try {
                    for (ContentValues value : values) {
                        long _id = db.insert(EarthQuakeDataContract.QuakesEntry.TABLE_NAME, null, value);
                        if (_id != -1) {
                            returnCount++;
                        }
                    }
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
                getContext().getContentResolver().notifyChange(uri, null);
                return returnCount;
            default:
                return super.bulkInsert(uri, values);
        }
    }
}