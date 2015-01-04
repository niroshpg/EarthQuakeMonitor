package com.niroshpg.android.earthquakemonitor;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * DetailMapFragment
 * This fragment shows map in its view with some associated text fields
 *
 * @author niroshpg
 * @since  06/10/2014
 */
public class DetailMapFragment extends Fragment {

    private static final String LOG_TAG = DetailMapFragment.class.getSimpleName();

    private String mDateStr;

    private String mIdStr;

    private MapViewDetailFragment mapViewDetailFragment;

    public DetailMapFragment() {
        setHasOptionsMenu(true);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
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

        Bundle detailMapViewArguments = new Bundle();
        detailMapViewArguments.putString(DetailActivity.DATE_KEY, mDateStr);
        detailMapViewArguments.putString(DetailActivity.ID_KEY, mIdStr);

        View rootView = inflater.inflate(R.layout.fragment_detail_map, container, false);

        /**
         * replace map container with google map fragment
         */
        if(mapViewDetailFragment == null)
        {
            mapViewDetailFragment = (MapViewDetailFragment) getChildFragmentManager().findFragmentById(R.id.map_container);
            if(mapViewDetailFragment == null)
            {
                mapViewDetailFragment = new MapViewDetailFragment();
            }
        }
        if (rootView.findViewById(R.id.map_container) != null) {
            if (savedInstanceState == null) {
                mapViewDetailFragment.setArguments(detailMapViewArguments);
                getChildFragmentManager().beginTransaction()
                        .replace(R.id.map_container, mapViewDetailFragment)
                        .commit();
            }
        }
        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // Inflate the menu; this adds items to the action bar if it is present.
        inflater.inflate(R.menu.detailfragment, menu);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

    }
}
