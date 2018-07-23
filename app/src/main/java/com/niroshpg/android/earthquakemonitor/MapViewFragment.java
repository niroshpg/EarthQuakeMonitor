package com.niroshpg.android.earthquakemonitor;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.RelativeLayout;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.niroshpg.android.earthquakemonitor.data.EarthQuakeDataContract;
import com.niroshpg.android.earthquakemonitor.data.EarthQuakeDataContract.QuakesEntry;
import com.niroshpg.android.earthquakemonitor.push.RegistrationIntentService;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * Fragment to show the map within the main activity view
 *
 * @author niroshpg
 * @since 06/10/2014
 */
public class MapViewFragment extends SupportMapFragment implements MainActivity.MarkerCallback,
        LoaderManager.LoaderCallbacks<Cursor> {

    public static final String LOG_TAG = MapViewFragment.class.getSimpleName();

    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;

    // For the earthquake events view we're showing only a small subset of the stored data.
    // Specify the columns we need.
    public static final String[] QUAKES_COLUMNS = {
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

    private static final int DEFAULT_MAP_ZOOM = 2;
    private static final int QUAKES_LOADER = 1;

    private static MapViewFragment mInstance;
    private SupportMapFragment mFragment;
    private String mTitle;
    private GoogleMap mMap;
    private boolean isMarkersLoaded = false;
    private int mMapZoom = DEFAULT_MAP_ZOOM;
    private static boolean enableCursorLoader = false;
    private PolygonOptions rectOptions;
    private boolean allowMapToScroll = false;
    public BlockingQueue<LatLng> latLngBlockingQueue = new ArrayBlockingQueue<LatLng>(1024);
    private Polygon polygon;
    ImageButton clearRegion;
    private MapView mMapView;
    private List<MarkerOptions> markerOptionsList = new ArrayList<MarkerOptions>();

    public static MapViewFragment getNewInstance() {
        enableCursorLoader = true;
        if (mInstance == null) {
            mInstance = new MapViewFragment();
        }
        mInstance.restartLoader();
        mInstance.setArguments(new Bundle());
        return mInstance;
    }

    public static void clearInstance() {
        mInstance = null;
    }


    /**
     * invoke when map is ready load markers etc
     */
    public void loadData() {
       if(markerOptionsList.size()>0 && mMap !=null){
           for(MarkerOptions markerOptions : markerOptionsList){
               mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(markerOptions.getPosition(), mMapZoom));
               mMap.addMarker(markerOptions);
           }
           markerOptionsList.clear();
       }
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        View view = inflater.inflate(R.layout.fragment_map, container, false);

//        FrameLayout suportMapOverlayLayout = (FrameLayout)view.findViewById(R.id.suport_map_overlay);
//        suportMapOverlayLayout.setOnTouchListener(new View.OnTouchListener(){
//                @Override
//                public boolean onTouch(View v, MotionEvent event) {
//                    return false;
//                }
//            }
//        );
/*
        ImageButton markRegion = (ImageButton)view.findViewById(R.id.markRegionButton);
        final ImageButton clearRegion = (ImageButton)view.findViewById(R.id.clearMarkedRegionButton);

        markRegion.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (allowMapToScroll != true) {
                    allowMapToScroll = true;
                    clearRegion.setVisibility(View.VISIBLE);
                } else {
                    allowMapToScroll = false;

                    clearRegion.setVisibility(View.GONE);
                }
            }
        });



        clearRegion.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (polygon != null) {
                    polygon.remove();
                    latLngBlockingQueue.clear();
                }
            }
        });
        FrameLayout parentLayout1 = (FrameLayout)view.findViewById(R.id.suport_map_overlay);
        parentLayout1.setOnTouchListener(new View.OnTouchListener() {


            public double longitude;
            public double latitude;
            public Projection projection;
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    float x = event.getX();
                    float y = event.getY();

                    int x_co = Math.round(x);
                    int y_co = Math.round(y);

                    projection = mMap.getProjection();
                    Point x_y_points = new Point(x_co, y_co);

                    LatLng latLng = mMap.getProjection().fromScreenLocation(x_y_points);
                    latitude = latLng.latitude;

                    longitude = latLng.longitude;

                    int eventaction = event.getAction();
                    switch (eventaction) {
                        case MotionEvent.ACTION_DOWN:
                            // finger touches the screen
                            latLngBlockingQueue.add(new LatLng(latitude, longitude));
                            break;

                        case MotionEvent.ACTION_MOVE:
                            // finger moves on the screen
                            latLngBlockingQueue.add(new LatLng(latitude, longitude));
                            break;

                        case MotionEvent.ACTION_UP:
                            // finger leaves the screen
                            Draw_Map();

                            break;
                    }
                    if (allowMapToScroll == true) {
                        return true;

                    } else {
                        return false;
                    }
                }
        });

        Button register = (Button)view.findViewById(R.id.register);
        register.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checkPlayServices()) {

                    if(polygon!=null) {
                        savePolygon(getActivity().getApplicationContext(), polygon.getPoints());
                    }

                    // Start IntentService to register this application with GCM.
                    Intent intent = new Intent(getActivity(), RegistrationIntentService.class);
                    getActivity().startService(intent);
                }
            }
        });

        RelativeLayout parentLayout = (RelativeLayout)view.findViewById(R.id.fragment_map_parent);
        parentLayout.setOnTouchListener(new View.OnTouchListener() {


            @Override
            public boolean onTouch(View v, MotionEvent event) {


                if (allowMapToScroll == true) {
                    return true;

                } else {
                    return false;
                }
            }
        });
        */

        setUpMapIfNeeded();
        return view;
    }

    public void Draw_Map() {
        rectOptions = new PolygonOptions();
        List<LatLng> points = new ArrayList<>();


        points.addAll(latLngBlockingQueue);
        double minLat = points.get(0).latitude;
        double maxLat = points.get(0).latitude;
        double minLng = points.get(0).longitude;
        double maxLng = points.get(0).longitude;

        for (LatLng point : points) {
            if (point.latitude < minLat) minLat = point.latitude;
            if (point.latitude > maxLat) maxLat = point.latitude;
            if (point.longitude < minLng) minLng = point.longitude;
            if (point.longitude > maxLng) maxLng = point.longitude;
        }
        LatLng p1 = new LatLng(minLat, minLng);
        LatLng p2 = new LatLng(minLat, maxLng);
        LatLng p3 = new LatLng(maxLat, maxLng);
        LatLng p4 = new LatLng(maxLat, minLng);
        rectOptions.add(p1);
        rectOptions.add(p2);
        rectOptions.add(p3);
        rectOptions.add(p4);
        rectOptions.add(p1);
        //rectOptions.addAll(latLngBlockingQueue);
        //rectOptions.add(rectOptions.getPoints().get(0));
        rectOptions.strokeColor(Color.BLUE);
        rectOptions.strokeWidth(7);
        //rectOptions.fillColor(R.color.transparent);

        if (polygon != null) {
            polygon.remove();
        }
        polygon = mMap.addPolygon(rectOptions);


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
        if (enableCursorLoader)
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
            mFragment.getMapAsync(new OnMapReadyCallback() {

                @Override
                public void onMapReady(GoogleMap googleMap) {
                    if (googleMap != null) {
                        mMap = googleMap;
                        setUpMap();
                        loadData();
                    }
                }
            });

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
        if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
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
                else{
                    markerOptionsList.clear();
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


                       if(mMap !=null){
                          mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, mMapZoom));
                          mMap.addMarker(markerOptions);
                      }
                      else{
                           markerOptionsList.add(markerOptions);
                       }

                   }
               }
                break;
        }
    }
//    public void onSaveInstanceState(Bundle outState){
//        //This MUST be done before saving any of your own or your base class's variables
//        final Bundle mapViewSaveState = new Bundle(outState);
//        mFragment.onSaveInstanceState(mapViewSaveState);
//        outState.putBundle("mapViewSaveState", mapViewSaveState);
//        //Add any other variables here.
//        super.onSaveInstanceState(outState);
//    }
//
//    public void onCreate(Bundle savedInstanceState){
//        super.onCreate(savedInstanceState);
//        final Bundle mapViewSavedInstanceState = savedInstanceState != null ? savedInstanceState.getBundle("mapViewSaveState") : null;
//        mFragment.onCreate(mapViewSavedInstanceState);
//        //....
//    }
//
//    @Override
//    public void onSaveInstanceState(Bundle outState) {
//        super.onSaveInstanceState(outState);
//        mMapView.onSaveInstanceState(outState);
//
//        /* I'm using Serializable, but you can use whatever you want */
//        //getArguments().putSerializable("yourfield", yourValue);
//    }

    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
    }

//    @Override
//    public void onCreate(Bundle savedState) {
//        super.onCreate(savedState);
//
//        mMapView.onCreate(savedState);
//
////        if(savedState != null) {
////            yourValue = getArguments.getSerializable("yourField");
////        }
//    }

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

    /**
     * save picture location list in shared preferences
     * @param context
     * @param list
     */
    private  void savePolygon(Context context, List<LatLng> list)
    {
        if(list != null && list.size()> 0) {
            String preferenceName = context.getResources().getString(R.string.REGION_SHARED_PREF_NAME);
            SharedPreferences sharedPref = context.getSharedPreferences(preferenceName, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPref.edit();

            String imageListAsJsonString = convertToJson(list).toString();
            editor.putString(context.getString(R.string.preference_polygon_list),imageListAsJsonString );
            editor.commit();

        }
        else
        {
            Log.w(LOG_TAG, "failed to save picture location list");
        }
    };

    private JSONObject convertToJson(List<LatLng> list)
    {
        JSONObject polygonJsonObj = new JSONObject();
        try {

        JSONArray polygonJsonArray = new JSONArray();


        for (LatLng aLatLng : list) {
            JSONObject latLngPoint = new JSONObject();

            latLngPoint.put("lat",aLatLng.latitude);
            latLngPoint.put("lng",aLatLng.longitude);

            JSONArray latLngArray = new JSONArray();
            latLngArray.put(latLngPoint);

            polygonJsonArray.put(latLngArray);
        }

            polygonJsonObj.put("regionPolygon", polygonJsonArray);
        } catch (JSONException e) {
            e.printStackTrace();

        }
        Log.e(LOG_TAG,polygonJsonObj.toString());
        return polygonJsonObj;
    }

    /**
     * Check the device to make sure it has the Google Play Services APK. If
     * it doesn't, display a dialog that allows users to download the APK from
     * the Google Play Store or enable it in the device's system settings.
     */
    private boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(getActivity());
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                GooglePlayServicesUtil.getErrorDialog(resultCode, getActivity(),
                        PLAY_SERVICES_RESOLUTION_REQUEST).show();
            } else {
                Log.i(LOG_TAG, "This device is not supported.");
                getActivity().finish();
            }
            return false;
        }
        return true;
    }

}