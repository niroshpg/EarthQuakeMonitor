package com.niroshpg.android.earthquakemonitor;


import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.niroshpg.android.earthquakemonitor.data.EarthQuakeDataContract;
import com.niroshpg.android.earthquakemonitor.data.EarthQuakeDataContract.QuakesEntry;
import com.niroshpg.android.earthquakemonitor.sync.EarthQuakeSyncAdapter;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.List;

/**
 * Encapsulates fetching the forecast and displaying it as a {@link android.widget.ListView} layout.
 */
public class ListViewFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
    public static final String LOG_TAG = ListViewFragment.class.getSimpleName();
    private static ListViewFragment mInstance;

    private QuakesAdapter mQuakesAdapter;
    private ListView mListView;
    private int mPosition = ListView.INVALID_POSITION;
    private boolean mUseTodayLayout;
    private boolean mTwoPane = false;

    private static final String SELECTED_KEY = "selected_position";

    private static final int QUAKES_LOADER = 0;

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
            QuakesEntry.COLUMN_SIG,
            QuakesEntry.COLUMN_URL,
            QuakesEntry.COLUMN_UPDATED
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
    public static final int COL_QUAKE_URL = 11;
    public static final int COL_QUAKE_UPDATED = 12;

    /**
     * A callback interface that all activities containing this fragment must
     * implement. This mechanism allows activities to be notified of item
     * selections.
     */
    public interface Callback {
        /**
         * DetailFragmentCallback for when an item has been selected.
         */
        public void onItemSelected(String date, Long id, LatLng latLng);
    }

    public static ListViewFragment getNewInstance() {
        if (mInstance == null) {
            mInstance = new ListViewFragment();
        }
        mInstance.restartLoader();
        return mInstance;
    }

    public static void clearInstance()
    {
        mInstance = null;
    }

    public ListViewFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Add this line in order for this fragment to handle menu events.
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.listviewfragment, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        mQuakesAdapter = new QuakesAdapter(getActivity(), null, 0);
        mQuakesAdapter.setFragmentManager(getActivity().getSupportFragmentManager());

        View rootView = inflater.inflate(R.layout.fragment_main, container, false);
        Log.i(LOG_TAG,"onCreateView id=" +getNewInstance().getId());
        if (rootView.findViewById(R.id.map_fragment_container) != null) {
            // The detail container view will be present only in the large-screen layouts
            // (res/layout-sw600dp). If this view is present, then the activity should be
            // in two-pane mode.
            mTwoPane = true;
            // In two-pane mode, show the detail view in this activity by
            // adding or replacing the detail fragment using a
            // fragment transaction.
            if (savedInstanceState == null) {

                Cursor cursor = mQuakesAdapter.getCursor();
                if (cursor != null && cursor.moveToFirst()) {
                    Bundle arguments = new Bundle();
                    arguments.putString(DetailActivity.DATE_KEY, cursor.getString(COL_QUAKE_DATE));
                    arguments.putString(DetailActivity.ID_KEY, String.valueOf( cursor.getLong(COL_QUAKE_ID)));

                    DetailMapFragment fragmentMap = new DetailMapFragment();
                    fragmentMap.setArguments(arguments);

                    getActivity().getSupportFragmentManager().beginTransaction()
                            .replace(R.id.detail_map_container, fragmentMap)
                            .commit();

                    DetailTextFragment fragmentText = new DetailTextFragment();
                    fragmentText.setArguments(arguments);

                    getActivity().getSupportFragmentManager().beginTransaction()
                            .replace(R.id.detail_text_container,fragmentText)
                            .commit();
                }
            }
        } else {
            mTwoPane = false;
        }

        // Get a reference to the ListView, and attach this adapter to it.
        mListView = (ListView) rootView.findViewById(R.id.list_view_quakes);
        mListView.setAdapter(mQuakesAdapter);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                Cursor cursor = mQuakesAdapter.getCursor();
                if (cursor != null && cursor.moveToPosition(position)) {
                    if(mTwoPane) {
                        Bundle args = new Bundle();
                        args.putString(DetailActivity.DATE_KEY, cursor.getString(COL_QUAKE_DATE));
                        args.putString(DetailActivity.ID_KEY, String.valueOf(cursor.getLong(COL_QUAKE_ID)));

                        DetailMapFragment fragmentMap = new DetailMapFragment();
                        fragmentMap.setArguments(args);

                        getActivity().getSupportFragmentManager().beginTransaction()
                                .replace(R.id.detail_map_container, fragmentMap)
                                .commit();

                        DetailTextFragment fragmentText = new DetailTextFragment();
                        fragmentText.setArguments(args);

                        getActivity().getSupportFragmentManager().beginTransaction()
                                .replace(R.id.detail_text_container,fragmentText)
                                .commit();
                    }
                    else
                    {
                        Intent intent = new Intent(getActivity(), DetailActivity.class)
                                .putExtra(DetailActivity.DATE_KEY, cursor.getString(COL_QUAKE_DATE))
                                .putExtra(DetailActivity.ID_KEY,String.valueOf(cursor.getLong(COL_QUAKE_ID)));
                        startActivity(intent);
                    }
                }
                mPosition = position;
            }
        });

        // If there's instance state, mine it for useful information.
        // The end-goal here is that the user never knows that turning their device sideways
        // does crazy lifecycle related things.  It should feel like some stuff stretched out,
        // or magically appeared to take advantage of room, but data or place in the app was never
        // actually *lost*.
        if (savedInstanceState != null && savedInstanceState.containsKey(SELECTED_KEY)) {
            // The listview probably hasn't even been populated yet.  Actually perform the
            // swapout in onLoadFinished.
            mPosition = savedInstanceState.getInt(SELECTED_KEY);
        }

        mQuakesAdapter.setUseTodayLayout(mUseTodayLayout);

        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        getLoaderManager().initLoader(QUAKES_LOADER, null, this);
        super.onActivityCreated(savedInstanceState);
    }

    private void updateWeather() {
        EarthQuakeSyncAdapter.syncImmediately(getActivity());
    }

    private void openPreferredLocationInMap() {
        // Using the URI scheme for showing a location found on a map.  This super-handy
        // intent can is detailed in the "Common Intents" page of Android's developer site:
        // http://developer.android.com/guide/components/intents-common.html#Maps

        if (null != mQuakesAdapter) {
            Cursor c = mQuakesAdapter.getCursor();
            if (null != c) {
                c.moveToPosition(0);
                String posLat = c.getString(COL_QUAKE_LAT);
                String posLong = c.getString(COL_QUAKE_LONG);
                Uri geoLocation = Uri.parse("geo:" + posLat + "," + posLong);

                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(geoLocation);

                if (intent.resolveActivity(getActivity().getPackageManager()) != null) {
                    startActivity(intent);
                } else {
                    Log.d(LOG_TAG, "Couldn't call " + geoLocation.toString() + ", no receiving apps installed!");
                }
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        getLoaderManager().restartLoader(QUAKES_LOADER, null, this);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        // When tablets rotate, the currently selected list item needs to be saved.
        // When no item is selected, mPosition will be set to Listview.INVALID_POSITION,
        // so check for that before storing.
        if (mPosition != ListView.INVALID_POSITION) {
            outState.putInt(SELECTED_KEY, mPosition);
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // This is called when a new Loader needs to be created.  This
        // fragment only uses one loader, so we don't care about checking the id.

        // To only show current and future dates, get the String representation for today,
        // and filter the query to return weather only for dates after or including today.
        // Only return data after today.
        String startDate = EarthQuakeDataContract.getDbDateString(new DateTime(DateTime.now(), DateTimeZone.UTC));
        String sortOrder = "";

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String sigKey = getActivity().getString(R.string.pref_sig_key);

        String sigString = prefs.getString(sigKey,
                getActivity().getString(R.string.pref_sig_key));
        int sig = Utility.getSignificance(sigString);

        switch (id) {


            case QUAKES_LOADER:
                mQuakesAdapter.getMarkerOptionsList().clear();
                // Sort order:  Ascending, by date.
                sortOrder = QuakesEntry.COLUMN_DATETEXT + " DESC";

                Uri quakeWithStartDateUri = EarthQuakeDataContract.QuakesEntry.buildQuakeWithStartDate(
                        startDate);

                // Now create and return a CursorLoader that will take care of
                // creating a Cursor for the data being displayed.
                String select = "sig > ?";
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
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        switch (loader.getId()) {

            case QUAKES_LOADER:
                clearMarkers();
                mQuakesAdapter.swapCursor(data);
                if (mPosition != ListView.INVALID_POSITION) {
                    // If we don't need to restart the loader, and there's a desired position to restore
                    // to, do so now.
                    mListView.smoothScrollToPosition(mPosition);
                }
                break;
        }
    }

    private void clearMarkers(){
        FragmentManager fragmentManager = getFragmentManager();
        if(fragmentManager != null ) {
            List<Fragment> fragments =  fragmentManager.getFragments();
            if(fragments != null && fragments.size() >0) {
                for (Fragment fr : fragments) {
                    if (fr != null && fr instanceof MapViewFragment) {
                        GoogleMap googleMap = ((MapViewFragment) fr).getMap();
                        if (googleMap != null) {
                            googleMap.clear();
                        }
                    }
                }
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

        switch (loader.getId()) {
            case QUAKES_LOADER:
                mQuakesAdapter.getMarkerOptionsList().clear();
                clearMarkers();
                mQuakesAdapter.swapCursor(null);
                break;
        }
    }

    public void setUseTodayLayout(boolean useTodayLayout) {
        mUseTodayLayout = useTodayLayout;

        if (mQuakesAdapter != null) {
            mQuakesAdapter.setUseTodayLayout(mUseTodayLayout);
        }
    }

    public List<MarkerOptions> getMarkerOptionsList()
    {
        if(mQuakesAdapter!= null)
        {
            mQuakesAdapter.getMarkerOptionsList();
        }
       return null;
    }

    public void restartLoader()
    {
        if(isAdded())
        {
            getLoaderManager().restartLoader(QUAKES_LOADER,null,this);
        }
    }
}