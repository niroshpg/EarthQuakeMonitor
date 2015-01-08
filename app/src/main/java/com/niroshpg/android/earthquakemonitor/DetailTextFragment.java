package com.niroshpg.android.earthquakemonitor;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.ShareActionProvider;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.gms.maps.model.LatLng;
import com.niroshpg.android.earthquakemonitor.data.EarthQuakeDataContract;
import com.niroshpg.android.earthquakemonitor.data.EarthQuakeDataContract.QuakesEntry;

import java.util.TimeZone;

/**
 * DetailTextFragment
 * This contains view with some text details of earthquake event
 *
 * @author niroshpg
 * @since  06/10/2014
 */
public class DetailTextFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final String LOG_TAG = DetailTextFragment.class.getSimpleName();

    private static final String EARTHQUAKE_ALERT_SHARE_HASHTAG = " #EarthQuakeMonitor";

    private ShareActionProvider mShareActionProvider;

    private static final String LOCATION_KEY = "location";

    private static final String LAT_KEY = "latitude";

    private static final String LNG_KEY = "longitude";

    private String mLocation;

    private String mEqAlert;

    private String mSharedText;

    private String mUrl;

    private String mDateStr;

    private String mIdStr;

    private LatLng mLatLng = new LatLng(0.0f,0.0f);

    private static final int DETAIL_LOADER = 0;

    // For the earthquake events view we're showing only a small subset of the stored data.
    // Specify the columns we need.
    private static final String[] QUAKES_COLUMNS = {
            QuakesEntry.TABLE_NAME + "." + QuakesEntry._ID,
            QuakesEntry.COLUMN_DATETEXT,
            QuakesEntry.COLUMN_SHORT_DESC,
            QuakesEntry.COLUMN_MAG,
            QuakesEntry.COLUMN_PLACE,
            QuakesEntry.COLUMN_DEPTH,
            QuakesEntry.COLUMN_LAT,
            QuakesEntry.COLUMN_LONG,
            QuakesEntry.COLUMN_TZ,
            QuakesEntry.COLUMN_ALERT,
            QuakesEntry.COLUMN_SIG,
            QuakesEntry.COLUMN_UPDATED,
            QuakesEntry.COLUMN_URL
    };

    private TextView mMagView;
    private TextView mDepthView;
    private TextView mAlertView;
    private TextView mSigView;
    private TextView mUrlView;
    private TextView mLatLngView;
    private TextView mUpdatedView;
    private double mLatitude;
    private double mLongitude;


    public DetailTextFragment() {
        setHasOptionsMenu(true);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString(LOCATION_KEY, mLocation);
        outState.putDouble(LAT_KEY,mLatitude);
        outState.putDouble(LNG_KEY,mLongitude);
        super.onSaveInstanceState(outState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        Bundle arguments = getArguments();
        if (arguments != null) {
            mDateStr = arguments.getString(DetailActivity.DATE_KEY);
            mIdStr = arguments.getString(DetailActivity.ID_KEY);
        }

        if (savedInstanceState != null) {
            mLocation = savedInstanceState.getString(LOCATION_KEY);
            mLatitude = savedInstanceState.getDouble(LAT_KEY);
            mLongitude = savedInstanceState.getDouble(LNG_KEY);
        }

        View rootView = inflater.inflate(R.layout.fragment_detail_text, container, false);

        mMagView = (TextView) rootView.findViewById(R.id.detail_mag_textview);
        mDepthView = (TextView) rootView.findViewById(R.id.detail_depth_textview);
        mSigView = (TextView) rootView.findViewById(R.id.detail_sig_textview);
        mUrlView  = (TextView) rootView.findViewById(R.id.detail_url_textview);
        mAlertView  = (TextView) rootView.findViewById(R.id.detail_alert_textview);
        mUpdatedView  = (TextView) rootView.findViewById(R.id.detail_updated_textview);
        mLatLngView  = (TextView) rootView.findViewById(R.id.detail_latlng_textview);
        return rootView;
    }



    @Override
    public void onResume() {
        super.onResume();
        Bundle arguments = getArguments();
        if (arguments != null && arguments.containsKey(DetailActivity.DATE_KEY) &&
                mLocation != null &&
                !mLocation.equals(Utility.getPreferredLocation(getActivity()))) {
            getLoaderManager().restartLoader(DETAIL_LOADER, null, this);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // Inflate the menu; this adds items to the action bar if it is present.
        inflater.inflate(R.menu.detailfragment_share, menu);
        // Retrieve the share menu item
        MenuItem menuItem = menu.findItem(R.id.action_share);

        // Get the provider and hold onto it to set/change the share intent.
        mShareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(menuItem);

        // If onLoadFinished happens before this, we can go ahead and set the share intent now.
        if (mEqAlert != null) {
            mShareActionProvider.setShareIntent(createShareIntent());
        }
    }

    /**
     *  Create share intent from the earth quake alert information
     * @return
     */
    private Intent createShareIntent() {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, mEqAlert );
        shareIntent.putExtra(Intent.EXTRA_TEXT, Html.fromHtml(mSharedText + EARTHQUAKE_ALERT_SHARE_HASHTAG) );

        return shareIntent;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (savedInstanceState != null) {
            mLocation = savedInstanceState.getString(LOCATION_KEY);
        }

        Bundle arguments = getArguments();
        if (arguments != null && arguments.containsKey(DetailActivity.DATE_KEY)) {
            getLoaderManager().initLoader(DETAIL_LOADER, null, this);
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {

        String sortOrder= EarthQuakeDataContract.QuakesEntry.COLUMN_DATETEXT + " DESC";
        Uri quakeWithIdUri = EarthQuakeDataContract.QuakesEntry.buildQuakesUri(
                Long.parseLong(mIdStr));

        // Now create and return a CursorLoader that will take care of
        // creating a Cursor for the data being displayed.
        return new CursorLoader(
                getActivity(),
                quakeWithIdUri,
                QUAKES_COLUMNS,
                null,
                null,
                sortOrder
        );

    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (data != null && data.moveToFirst()) {

            String alert = data.getString(data.getColumnIndex(EarthQuakeDataContract.QuakesEntry.COLUMN_ALERT));
            int sig = data.getInt(data.getColumnIndex(EarthQuakeDataContract.QuakesEntry.COLUMN_SIG));

            // Read date from cursor and update views for day of week and date
            String date = data.getString(data.getColumnIndex(EarthQuakeDataContract.QuakesEntry.COLUMN_DATETEXT));
            String timeZoneId = Utility.currentTimeZone.getDisplayName(true, TimeZone.SHORT);
            String dateText = Utility.getFriendlyDayString(getActivity(), date);
            String updated = data.getString(data.getColumnIndex(EarthQuakeDataContract.QuakesEntry.COLUMN_UPDATED));
            String updatedText = Utility.getFriendlyDayString(getActivity(), updated);
            mUpdatedView.setText("Updated: " + updatedText + " "+timeZoneId);

            // Read description from cursor and update view
            String place = data.getString(data.getColumnIndex(
                    EarthQuakeDataContract.QuakesEntry.COLUMN_PLACE));

            double mag = data.getDouble(data.getColumnIndex(EarthQuakeDataContract.QuakesEntry.COLUMN_MAG));
            String magString = "Magnitude: "+String.valueOf(mag) +"M";
            mMagView.setText(magString);

            // Read depth from cursor and update view
            double depth = data.getDouble(data.getColumnIndex(EarthQuakeDataContract.QuakesEntry.COLUMN_DEPTH));
            String depthString = "Depth: "+ String.valueOf(depth) + " km";
            mDepthView.setText(depthString);

            mLongitude = data.getDouble(data.getColumnIndex(EarthQuakeDataContract.QuakesEntry.COLUMN_LONG));
            mLatitude = data.getDouble(data.getColumnIndex(EarthQuakeDataContract.QuakesEntry.COLUMN_LAT));

            mLatLngView.setText("Location: ("+mLatitude+","+mLongitude+")");
            mAlertView.setText("Alert: "+alert);
            mSigView.setText("Significance: "+String.valueOf(sig));

            String url = data.getString(data.getColumnIndex(EarthQuakeDataContract.QuakesEntry.COLUMN_URL));
            mUrlView.setText(Html.fromHtml("<a href=" + url + "> See USGS Event "));
            mUrlView.setMovementMethod(LinkMovementMethod.getInstance());

            // for the share intent
            // subject of the shared content
            mEqAlert = String.format("%s - %s - %sM/%skm", dateText, place, mag, depth);
            // body of the shared content
            mSharedText = "<p> Summary:  A earthquake of "+mag+" M magnitude and "+depth+" km deep occurred at "
                    + place + " on " +dateText +"."
                    +"<p> <a href=\"" + mUrl + "\"> See USGS Event for details "
                    +"<p> ";
            // url to of the event for sharing
            mUrl = url;
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) { }


}
