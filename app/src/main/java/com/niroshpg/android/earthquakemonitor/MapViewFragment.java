package com.niroshpg.android.earthquakemonitor;

import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
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

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.niroshpg.android.earthquakemonitor.data.EarthQuakeDataContract;
import com.niroshpg.android.earthquakemonitor.data.EarthQuakeDataContract.QuakesEntry;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

/**
 * Fragment to show the map within the main activity view
 *
 * @author niroshpg
 * @since  06/10/2014
 */
public class MapViewFragment extends SupportMapFragment implements MainActivity.MarkerCallback,
       LoaderManager.LoaderCallbacks<Cursor> {

    public static final String LOG_TAG = MapViewFragment.class.getSimpleName();

    // For the forecast view we're showing only a small subset of the stored data.
    // Specify the columns we need.
    public static final String[] QUAKES_COLUMNS = {
            // In this case the id needs to be fully qualified with a table name, since
            // the content provider joins the location & weather tables in the background
            // (both have an _id column)
            // On the one hand, that's annoying.  On the other, you can search the weather table
            // using the location set by the user, which is only in the Location table.
            // So the convenience is worth it.
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
            QuakesEntry.COLUMN_SIG
    };


    // These indices are tied to FORECAST_COLUMNS.  If FORECAST_COLUMNS changes, these
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

    private static final int DEFAULT_MAP_ZOOM = 2;
    private static final int QUAKES_LOADER = 1;

    private static MapViewFragment mInstance ;
    private SupportMapFragment mFragment;
    private String mTitle;
    private GoogleMap mMap;
    private boolean isMarkersLoaded = false;
    private int mMapZoom = DEFAULT_MAP_ZOOM;
    private static boolean enableCursorLoader  = false;

    public static MapViewFragment getNewInstance()
    {
        enableCursorLoader = true;
        if(mInstance == null)
        {
            mInstance = new MapViewFragment();
        }
        mInstance.restartLoader();
        return mInstance;
    }

    public static void clearInstance()
    {
        mInstance = null;
    }


    public void loadData()
    {
        if(!isMarkersLoaded) {
        }
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState){
        super.onCreateView(inflater,container, savedInstanceState );

        View view = inflater.inflate(R.layout.fragment_map, container, false);
        setUpMapIfNeeded();
        return view;
    }
    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        FragmentManager fm = getChildFragmentManager();
        mFragment = (SupportMapFragment) fm.findFragmentById(R.id.fragment_map_container);
        if (mFragment == null) {
            mFragment = SupportMapFragment.newInstance();

            fm.beginTransaction().replace(R.id.fragment_map_container, mFragment).commit();
        }
        setUpMapIfNeeded();
        if(enableCursorLoader)
            getLoaderManager().initLoader(QUAKES_LOADER, null, this);
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
        if(mMap != null)
        {
            loadData();
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

    public String getTitle() {
        return mTitle;
    }

    public void setTitle(String mTitle) {
        this.mTitle = mTitle;
    }

    @Override
    public void onAddMarker(LatLng latLng, String title, String snippet) {
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle bundle) {
        String startDate = EarthQuakeDataContract.getDbDateString(new DateTime(DateTime.now(), DateTimeZone.UTC));
        String sortOrder = "";

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String sigKey = getActivity().getString(R.string.pref_sig_key);

        String sigString = prefs.getString(sigKey,
                getActivity().getString(R.string.pref_sig_key));
        int sig = Utility.getSignificance(sigString);

        switch (id) {
            case QUAKES_LOADER:
                // Sort order:  Ascending, by date.
                sortOrder = EarthQuakeDataContract.QuakesEntry.COLUMN_DATETEXT + " DESC";
                Uri quakeWithStartDateUri = EarthQuakeDataContract.QuakesEntry.buildQuakeWithStartDate(
                        startDate);
                // Now create and return a CursorLoader that will take care of
                // creating a Cursor for the data being displayed.
                String select = "sig >= ?";
                String [] selectArgs = new String[]{String.valueOf(sig)};
                return new CursorLoader(
                        getActivity(),
                        quakeWithStartDateUri,
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
                if(mMap != null) {
                    mMap.clear();
                }
                cursor.moveToFirst();
               for(int i=0; i < cursor.getCount();i++,cursor.moveToNext())
               {
                   int significance = cursor.getInt(COL_QUAKE_SIG);
                   if(significance >= preferredSignificance ) {
                       double high = cursor.getDouble(COL_QUAKE_MAG);
                       double low = cursor.getDouble(COL_QUAKE_DEPTH);
                       double lng = cursor.getDouble(COL_QUAKE_LONG);
                       double lat = cursor.getDouble(COL_QUAKE_LAT);
                       MarkerOptions markerOptions = new MarkerOptions();
                       LatLng latLng = new LatLng(lat,lng);
                       markerOptions.position(latLng);
                       markerOptions.title("Magnitude: " + String.valueOf(high) + " M") ;
                       markerOptions.snippet("Depth: " + String.valueOf(low) + "km") ;
                       int resourceId = Utility.getMarkerResourceForSignificance(significance);
                       String alert = cursor.getString(COL_QUAKE_ALERT);
                       Bitmap bitmap = Utility.addAlertData(getActivity(),resourceId,alert);
                       markerOptions.icon(BitmapDescriptorFactory.fromBitmap(bitmap));
                       mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, mMapZoom));
                       mMap.addMarker(markerOptions);
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

    public void restartLoader()
    {
        if(this.isAdded() ) {
            getLoaderManager().restartLoader(QUAKES_LOADER, null, this);
        }
    }


}