package com.niroshpg.android.earthquakemonitor;

import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.niroshpg.android.earthquakemonitor.data.EarthQuakeDataContract;

import java.util.TimeZone;

public class MapViewDetailFragment extends SupportMapFragment implements     LoaderManager.LoaderCallbacks<Cursor> {

    public static final String LOG_TAG = MapViewFragment.class.getSimpleName();

    // Specify the columns we need.
    public static final String[] QUAKES_COLUMNS = {
            EarthQuakeDataContract.QuakesEntry.TABLE_NAME + "." + EarthQuakeDataContract.QuakesEntry._ID,
            EarthQuakeDataContract.QuakesEntry.COLUMN_DATETEXT,
            EarthQuakeDataContract.QuakesEntry.COLUMN_SHORT_DESC,
            EarthQuakeDataContract.QuakesEntry.COLUMN_MAG,
            EarthQuakeDataContract.QuakesEntry.COLUMN_PLACE,
            EarthQuakeDataContract.QuakesEntry.COLUMN_DEPTH,
            EarthQuakeDataContract.QuakesEntry.COLUMN_LAT,
            EarthQuakeDataContract.QuakesEntry.COLUMN_LONG,
            EarthQuakeDataContract.QuakesEntry.COLUMN_TZ,
            EarthQuakeDataContract.QuakesEntry.COLUMN_ALERT,
            EarthQuakeDataContract.QuakesEntry.COLUMN_SIG
    };
    // These indices are tied to QUAKES_COLUMNS.  If QUAKES_COLUMNS changes, these
    // must change.
    public static final int COL_QUAKE_ID = 0;
    public static final int COL_QUAKE_DATE = 1;
    public static final int COL_QUAKE_DESC = 2;
    public static final int COL_QUAKE_MAG = 3;
    public static final int COL_QUAKE_PLACE = 4;
    public static final int COL_QUAKE_DEPTH = 5;
    public static final int COL_QUAKE_LAT = 6;
    public static final int COL_QUAKE_LONG = 7;
    public static final int COL_QUAKE_TZ = 8;
    public static final int COL_QUAKE_ALERT = 9;
    public static final int COL_QUAKE_SIG = 10;

    private static final int DEFAULT_MAP_ZOOM = 5;
    private static final int QUAKES_LOADER = 1;
    private String mIdStr;
    private SupportMapFragment mFragment;
    private GoogleMap mMap;
    private EQIconView mIconView;
    private TextView mFriendlyDateView;
    private TextView mPlaceView;

    public void setMapZoom(int mMapZoom) {
        this.mMapZoom = mMapZoom;
    }

    private  int mMapZoom = DEFAULT_MAP_ZOOM;

    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState){
        super.onCreateView(inflater,container, savedInstanceState );
        View view = inflater.inflate(R.layout.fragment_map, container, false);

        View parentView = getParentFragment().getView();

        mIconView = (EQIconView) parentView.findViewById(R.id.detail_icon);
        mFriendlyDateView = (TextView) parentView.findViewById(R.id.detail_day_textview);
        mPlaceView = (TextView) parentView.findViewById(R.id.detail_place_textview);

        setUpMapIfNeeded();
        return view;
    }
    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Bundle arguments = getArguments();
        if (arguments != null) {
            mIdStr = arguments.getString(DetailActivity.ID_KEY);
        }
        FragmentManager fm = getChildFragmentManager();
        mFragment = (SupportMapFragment) fm.findFragmentById(R.id.fragment_map_container);
        if (mFragment == null) {
            mFragment = SupportMapFragment.newInstance();

            fm.beginTransaction().replace(R.id.fragment_map_container, mFragment).commit();
        }
        setUpMapIfNeeded();
        if(mIdStr != null) {
            getLoaderManager().initLoader(QUAKES_LOADER, null, this);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        setUpMapIfNeeded();
    }

    @Override
    public void onResume() {
        super.onResume();
        setUpMapIfNeeded();
        Bundle arguments = getArguments();
        if (arguments != null && arguments.containsKey(DetailActivity.ID_KEY)) {
            getLoaderManager().restartLoader(QUAKES_LOADER, null, this);
        }
    }


    /**
     * Sets up the map if it is possible to do so (i.e., the Google Play services APK is correctly
     * installed) and the map has not already been instantiated.. This will ensure that we only ever
     * call {@link #setUpMap()} once when {@link #mMap} is not null.
     * <p>
     * If it isn't installed {@link com.google.android.gms.maps.SupportMapFragment} (and
     * {@link com.google.android.gms.maps.MapView MapView}) will show a prompt for the user to
     * install/update the Google Play services APK on their device.
     * <p>
     * A user can return to this FragmentActivity after following the prompt and correctly
     * installing/updating/enabling the Google Play services. Since the FragmentActivity may not
     * have been completely destroyed during this process (it is likely that it would only be
     * stopped or paused), {@link #onCreate(Bundle)} may not be called again so we should call this
     * method in {@link #onResume()} to guarantee that it will be called.
     */
    private void setUpMapIfNeeded() {
        if (mMap == null) {
            mFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.fragment_map_container);
            if (mFragment == null) {
                mFragment = SupportMapFragment.newInstance();

                getChildFragmentManager().beginTransaction().replace(R.id.fragment_map_container, mFragment).commit();
            }
            mMap = mFragment.getMap();
            if(mMap != null)
            {
                setUpMap();
            }
        }
    }

    /**
     * This is where we can add markers or lines, add listeners or move the camera. In this case, we
     * just add a marker near Africa.
     * <p>
     * This should only be called once and when we are sure that {@link #mMap} is not null.
     */
    private void setUpMap() {
        mMap.getUiSettings().setAllGesturesEnabled(true);
        mMap.getUiSettings().setCompassEnabled(true);
        mMap.setMyLocationEnabled(true);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle bundle) {

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String sigKey = getActivity().getString(R.string.pref_sig_key);

        String sigString = prefs.getString(sigKey,
                getActivity().getString(R.string.pref_sig_key));
        int sig = Utility.getSignificance(sigString);

        switch (id) {
            case QUAKES_LOADER:
                // Sort order:  Ascending, by date.
                String sortOrder = EarthQuakeDataContract.QuakesEntry.COLUMN_DATETEXT + " DESC";

                Uri quakeWithIdUri  = EarthQuakeDataContract.QuakesEntry.buildQuakesUri(
                        Long.parseLong(mIdStr));
                // Filter by significance preference
                String select = "sig > ?";
                String [] selectArgs = new String[]{String.valueOf(sig)};

                // Now create and return a CursorLoader that will take care of
                // creating a Cursor for the data being displayed.
                return new CursorLoader(
                        getActivity(),
                        quakeWithIdUri,
                        QUAKES_COLUMNS,
                        select,
                        selectArgs,
                        sortOrder
                );

            default:
                return null;

        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String sigKey = getActivity().getString(R.string.pref_sig_key);

        String sigKeyString = prefs.getString(sigKey,
                getActivity().getString(R.string.pref_sig_key));
        int preferredSignificance = Utility.getSignificance(sigKeyString);

        switch (cursorLoader.getId()) {

            case QUAKES_LOADER:
                mMap.clear();
                cursor.moveToFirst();

                if (cursor != null && cursor.moveToFirst()) {
                    String alert = cursor.getString(cursor.getColumnIndex(EarthQuakeDataContract.QuakesEntry.COLUMN_ALERT));
                    int sig = cursor.getInt(cursor.getColumnIndex(EarthQuakeDataContract.QuakesEntry.COLUMN_SIG));
                    mIconView.setSig(sig);
                    mIconView.setAlert(alert);
                    mIconView.setImageResource(Utility.getIconResourceForAlertCondition(alert, sig));

                    // Read date from cursor and update views for day of week and date
                    String date = cursor.getString(cursor.getColumnIndex(EarthQuakeDataContract.QuakesEntry.COLUMN_DATETEXT));
                    String timeZoneId = Utility.currentTimeZone.getDisplayName(true, TimeZone.SHORT);
                    String dateText = Utility.getFriendlyDayString(getActivity(), date);
                    mFriendlyDateView.setText(dateText + " " + timeZoneId);
                    String place = cursor.getString(cursor.getColumnIndex(EarthQuakeDataContract.QuakesEntry.COLUMN_PLACE));
                    mPlaceView.setText(place);

                    for(int i=0; i < cursor.getCount();i++,cursor.moveToNext())
                    {
                        int significance = cursor.getInt(COL_QUAKE_SIG);
                        if(significance >= preferredSignificance ) {
                            double high = cursor.getDouble(COL_QUAKE_MAG);
                            double low = cursor.getDouble(COL_QUAKE_DEPTH);
                            double lng = cursor.getDouble(COL_QUAKE_LONG);
                            double lat = cursor.getDouble(COL_QUAKE_LAT);
                            LatLng latLng = new LatLng(lat,lng);
                            MarkerOptions markerOptions = new MarkerOptions();
                            markerOptions.position(latLng);
                            markerOptions.title("Magnitude: " + String.valueOf(high) + " M") ;
                            markerOptions.snippet("Depth: " + String.valueOf(low) + "km") ;
                            markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_marker));
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, mMapZoom));
                            mMap.addMarker(markerOptions);
                        }
                    }
                }
                break;
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        switch (cursorLoader.getId()) {

            case QUAKES_LOADER:
                mMap.clear();
                break;
        }
    }

//    public void restartLoader()
//    {
//        if(this.isAdded() ) {
//            getLoaderManager().restartLoader(QUAKES_LOADER, null, this);
//        }
//    }
}