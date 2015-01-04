package com.niroshpg.android.earthquakemonitor;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

/**
 * QuakesAdapter
 * - exposes a list of earth quake alerts from a Cursor to a ListView.
 *
 * @author niroshpg
 * @since  06/10/2014
 */
public class QuakesAdapter extends CursorAdapter {

    private static final int VIEW_TYPE_COUNT = 2;
    private static final int VIEW_TYPE_TODAY = 0;
    private static final int VIEW_TYPE_FUTURE_DAY = 1;

    // Flag to determine if we want to use a separate view for "today".
    private boolean mUseTodayLayout = true;

    private FragmentManager fragmentManager;

    private MapViewFragment mapViewFragment;

    private List<MarkerOptions> markerOptionsList = new ArrayList<MarkerOptions>();

    /**
     * Cache of the children views for a forecast list item.
     */
    public static class ViewHolder {
        public final EQIconView iconView;
        public final TextView dateView;
        public final TextView descriptionView;
        public final TextView magView;
        public final TextView depthView;

        public ViewHolder(View view) {
            iconView = (EQIconView) view.findViewById(R.id.quakes_list_item_icon);
            dateView = (TextView) view.findViewById(R.id.list_item_date_textview);
            descriptionView = (TextView) view.findViewById(R.id.list_item_description_textview);
            magView = (TextView) view.findViewById(R.id.list_item_mag_textview);
            depthView = (TextView) view.findViewById(R.id.list_item_depth_textview);
        }
    }

    public QuakesAdapter(Context context, Cursor c, int flags) {

        super(context, c, flags);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View view = LayoutInflater.from(context).inflate(R.layout.list_item_quakes, parent, false);

        ViewHolder viewHolder = new ViewHolder(view);
        view.setTag(viewHolder);
       // mapViewFragment = findMapViewFragment();
        return view;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String sigKey = context.getString(R.string.pref_sig_key);
        int sig = cursor.getInt(ListViewFragment.COL_QUAKE_SIG);
        String sigKeyString = prefs.getString(sigKey,
                context.getString(R.string.pref_sig_key));
        if(sig >= Utility.getSignificance(sigKeyString) ) {
            ViewHolder viewHolder = (ViewHolder) view.getTag();
            String alert = cursor.getString(ListViewFragment.COL_QUAKE_ALERT);

            int viewType = getItemViewType(cursor.getPosition());
            viewHolder.iconView.setSig(sig);
            switch (viewType) {
                case VIEW_TYPE_TODAY: {
                    // Get weather icon
                    viewHolder.iconView.setImageResource(Utility.getArtResourceForAlertCondition(
                            alert, sig));

                    break;
                }
                case VIEW_TYPE_FUTURE_DAY: {
                    // Get weather icon
                    viewHolder.iconView.setImageResource(Utility.getIconResourceForAlertCondition(
                            alert, sig));
                    break;
                }
            }

            // Read date from cursor
            String dateString = cursor.getString(ListViewFragment.COL_QUAKE_DATE);
            String timezone = cursor.getString(ListViewFragment.COL_QUAKE_TZ);
            // Find TextView and set formatted date on it
            String timeZoneId = Utility.currentTimeZone.getDisplayName(true, TimeZone.SHORT);

            viewHolder.dateView.setText(Utility.getFriendlyDayString(context, dateString) + " " + timeZoneId);

            // Read place from cursor
            String description = cursor.getString(ListViewFragment.COL_QUAKE_PLACE);
            // Find TextView and set earth quake alert description
            viewHolder.descriptionView.setText(description);

            // For accessibility, add a content description to the icon field
            viewHolder.iconView.setContentDescription(description);

            // Read magnitude from cursor
            double magnitude = cursor.getDouble(ListViewFragment.COL_QUAKE_MAG);
            viewHolder.magView.setText(String.valueOf(magnitude) + " M");

            // Read depth from cursor
            double depth = cursor.getDouble(ListViewFragment.COL_QUAKE_DEPTH);
            viewHolder.depthView.setText(String.valueOf(depth) + "km");

            double lat =  cursor.getDouble(ListViewFragment.COL_QUAKE_LAT);
            double lng =  cursor.getDouble(ListViewFragment.COL_QUAKE_LONG);

            MarkerOptions markerOptions = new MarkerOptions();
            markerOptions.position(new LatLng(
                    lat,
                    lng));
            markerOptions.title("Magnitude: " + String.valueOf(magnitude) + " M") ;
            markerOptions.snippet("Depth: " + String.valueOf(depth) + "km") ;
            markerOptionsList.add(markerOptions);
        }
    }

//    private MapViewFragment findMapViewFragment()
//    {
//        MapViewFragment mapViewFragment = null;
//        for(Fragment fr :fragmentManager.getFragments())
//        {
//            if(fr instanceof  MapViewFragment)
//            {
//                mapViewFragment = ((MapViewFragment)fr);
//            }
//        }
//        return mapViewFragment;
//    }

    public void setUseSpecificLayout(boolean useTodayLayout) {
        mUseTodayLayout = useTodayLayout;
    }

    @Override
    public int getItemViewType(int position) {
        return (position == 0 && mUseTodayLayout) ? VIEW_TYPE_TODAY : VIEW_TYPE_FUTURE_DAY;
    }

    @Override
    public int getViewTypeCount() {
        return VIEW_TYPE_COUNT;
    }

    public List<MarkerOptions> getMarkerOptionsList() {
        return markerOptionsList;
    }

    public void setFragmentManager(FragmentManager fragmentManager) {
        this.fragmentManager = fragmentManager;
    }
}