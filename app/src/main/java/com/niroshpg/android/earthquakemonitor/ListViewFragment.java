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
import com.niroshpg.android.earthquakemonitor.data.EarthQuakeDataContract;
import com.niroshpg.android.earthquakemonitor.data.EarthQuakeDataContract.QuakesEntry;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.List;

/**
 * retrieve earthquake events as list and display as ListView layout
 *
 * @author niroshpg
 * @since  06/10/2014
 */
public class ListViewFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
    public static final String LOG_TAG = ListViewFragment.class.getSimpleName();
    /**
     * self static single instance
     */
    private static ListViewFragment mInstance;

    /**
     * list adapter for to bind quakes to the view
     */
    private QuakesAdapter mQuakesAdapter;

    /**
     * reference to hold the list view layout widget
     */
    private ListView mListView;

    /**
     * save list position between life cycles
     */
    private int mPosition = ListView.INVALID_POSITION;

    /**
     * selection for specific layout for certain list items.
     * Note: this is currently implemented as single layout for
     * all list items.
     */
    private boolean mUseSpecificLayout;

    /**
     * indicate two panes mode in use (for tablet ui displaying multiple fragments)
     */
    private boolean mTwoPane = false;

    /**
     * key for saving list position
     */
    private static final String SELECTED_KEY = "selected_position";

    /**
     * identifier for the cursor loader
     */
    private static final int QUAKES_LOADER = 0;

    // For the earthquake events list view, we're showing only a small subset of the stored data.
    // Specify the columns we need.
    public static final String[] QUAKES_COLUMNS = {
            // In this case the id needs to be fully qualified with a table name, since
            // the content provider joins the location & quakes tables in the background
            // (both have an _id column)
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

    /**
     * return reference to self single instance
     * @return
     */
    public static ListViewFragment getNewInstance() {
        if (mInstance == null) {
            mInstance = new ListViewFragment();
        }
        mInstance.restartLoader();
        return mInstance;
    }

    /**
     * clear self instance
     */
    public static void clearInstance()
    {
        mInstance = null;
    }

    /**
     * constructor
     */
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
                /**
                 * if data is available dynamically load map and text fragments
                 */
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
                /**
                 * handle user click events for the list
                 */
                Cursor cursor = mQuakesAdapter.getCursor();
                if (cursor != null && cursor.moveToPosition(position)) {
                    if(mTwoPane) {
                        /**
                         * if data is available dynamically load map and text fragments in two pane mode
                         */
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
                        /**
                         * if data is available invoke detail activity for the selected item
                         */
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
            // The list view probably hasn't even been populated yet.  Actually perform the
            // swap out in onLoadFinished.
            mPosition = savedInstanceState.getInt(SELECTED_KEY);
        }
        mQuakesAdapter.setUseSpecificLayout(mUseSpecificLayout);

        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        getLoaderManager().initLoader(QUAKES_LOADER, null, this);
        super.onActivityCreated(savedInstanceState);
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

        //use query to return only events more or equally significant than the user preferred significance level

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

    /**
     * after loading new events clear the old markers within the map
     */
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

    public void setUseSpecificLayout(boolean useTodayLayout) {
        mUseSpecificLayout = useTodayLayout;

        if (mQuakesAdapter != null) {
            mQuakesAdapter.setUseSpecificLayout(mUseSpecificLayout);
        }
    }

    /**
     * restart the loader
     */
    public void restartLoader()
    {
        if(isAdded())
        {
            getLoaderManager().restartLoader(QUAKES_LOADER,null,this);
        }
    }
}