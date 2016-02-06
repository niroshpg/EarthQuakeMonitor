package com.niroshpg.android.earthquakemonitor;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;
import com.niroshpg.android.earthquakemonitor.push.QuickstartPreferences;
import com.niroshpg.android.earthquakemonitor.sync.EarthQuakeSyncAdapter;

/**
 * MainActivity for the application. Handles the main tab view using PagerAdapter and
 * includes the logic to load rest of the activities and fragments
 *
 * @author niroshpg
 * @since  06/10/2014
 */
public class MainActivity extends ActionBarActivity implements ActionBar.TabListener ,
         ListViewFragment.Callback {


    private static final String TAG = "MainActivity";

    private BroadcastReceiver mRegistrationBroadcastReceiver;

    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide fragments for each of the
     * three primary sections of the app. We use a {@link android.support.v4.app.FragmentPagerAdapter}
     * derivative, which will keep every loaded fragment in memory. If this becomes too memory
     * intensive, it may be best to switch to a {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    public AppSectionsPagerAdapter mAppSectionsPagerAdapter;

    /**
     * The {@link ViewPager} that will display the three primary sections of the app, one at a
     * time.
     */
    ViewPager mViewPager;

    private boolean mTwoPane = false;

    /**
     * A callback interface that all activities containing this fragment must
     * implement. This mechanism allows activities to be notified of item
     * selections.
     */
    public interface MarkerCallback {
        /**
         * DetailFragmentCallback for when an item has been selected.
         */
        public void onAddMarker( LatLng latLng, String title, String snippet);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        clearInstances();

        // Create the adapter that will return a fragment for each of the three primary sections
        // of the app.
        mAppSectionsPagerAdapter = new AppSectionsPagerAdapter(getSupportFragmentManager());

        // Set up the action bar.
        final ActionBar actionBar = this.getSupportActionBar();

        // Specify that the Home/Up button should not be enabled, since there is no hierarchical
        // parent.
        actionBar.setHomeButtonEnabled(false);

        // Specify that we will be displaying tabs in the action bar.
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        // Set up the ViewPager, attaching the adapter and setting up a listener for when the
        // user swipes between sections.
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mAppSectionsPagerAdapter);
        mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                // When swiping between different app sections, select the corresponding tab.
                // We can also use ActionBar.Tab#select() to do this if we have a reference to the
                // Tab.
                actionBar.setSelectedNavigationItem(position);
            }
        });

        //mViewPager.setBackgroundColor(Color.BLUE);
        // For each of the sections in the app, add a tab to the action bar.
        for (int i = 0; i < mAppSectionsPagerAdapter.getCount(); i++) {
            // Create a tab with text corresponding to the page title defined by the adapter.
            // Also specify this Activity object, which implements the TabListener interface, as the
            // listener for when this tab is selected.
            actionBar.addTab(
                    actionBar.newTab()
                            .setText(mAppSectionsPagerAdapter.getPageTitle(i))
                            .setIcon(mAppSectionsPagerAdapter.getPageIcon(i))
                            .setTabListener(this));
        }

        EarthQuakeSyncAdapter.initializeSyncAdapter(this);
        mRegistrationBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
               // mRegistrationProgressBar.setVisibility(ProgressBar.GONE);
                SharedPreferences sharedPreferences =
                        PreferenceManager.getDefaultSharedPreferences(context);
                boolean sentToken = sharedPreferences
                        .getBoolean(QuickstartPreferences.SENT_TOKEN_TO_SERVER, false);
                if (sentToken) {
                   // mInformationTextView.setText(getString(R.string.gcm_send_message));
                    Toast.makeText(context,"token send to server", Toast.LENGTH_LONG);
                } else {
                   // mInformationTextView.setText(getString(R.string.token_error_message));
                    Toast.makeText(context,"error sending token to server", Toast.LENGTH_LONG);
                }
            }
        };

    }

    @Override
    protected void onResume() {
        super.onResume();

        LocalBroadcastManager.getInstance(this).registerReceiver(mRegistrationBroadcastReceiver,
                new IntentFilter(QuickstartPreferences.REGISTRATION_COMPLETE));
    }

    @Override
    protected void onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mRegistrationBroadcastReceiver);
        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
        }

        return super.onOptionsItemSelected(item);
    }


    @Override
    public void onTabSelected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
        mViewPager.setCurrentItem(tab.getPosition());
    }

    @Override
    public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
    }

    @Override
    public void onTabReselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
    }

    @Override
    public void onItemSelected(String date, Long id, LatLng latLng) {
        // dynamically load fragments or invoke activity according to
        // two pane mode or not
        if (mTwoPane) {
            Bundle arguments = new Bundle();
            arguments.putString(DetailActivity.DATE_KEY, date);
            arguments.putString(DetailActivity.ID_KEY, String.valueOf(id));

            DetailMapFragment fragmentMap = new DetailMapFragment();
            fragmentMap.setArguments(arguments);

            getSupportFragmentManager().beginTransaction()
                    .add(R.id.detail_map_container, fragmentMap)
                    .commit();

            DetailTextFragment fragmentText = new DetailTextFragment();
            fragmentText.setArguments(arguments);

            getSupportFragmentManager().beginTransaction()
                    .add(R.id.detail_text_container,fragmentText)
                    .commit();

        } else {
            Intent intent = new Intent(this, DetailActivity.class)
                    .putExtra(DetailActivity.DATE_KEY, date)
                    .putExtra(DetailActivity.ID_KEY,String.valueOf(id));
            startActivity(intent);
        }
    }

    /**
     * clear the single instances used (to be used beginning of a new life cycle event)
     */
    public static void clearInstances()
    {
        MapViewFragment.clearInstance();
        ListViewFragment.clearInstance();
    }

    /**
     * A {@link android.support.v4.app.FragmentPagerAdapter} that returns a fragment corresponding to one of the primary
     * sections of the app.
     */
    public static class AppSectionsPagerAdapter extends FragmentPagerAdapter {

        public ListViewFragment mListViewFragment;

        public final FragmentManager mFragmentManager;

        public Fragment mFragment;

        public AppSectionsPagerAdapter(FragmentManager fm)
        {
            super(fm);
            mFragmentManager = fm;
        }

        @Override
        public Fragment getItem(int i) {
            switch (i) {
                case 0:
                    /**
                     * default fragment to be used. Here map fragment is use
                     * as this will be more interesting to user
                     */
                    mFragment = MapViewFragment.getNewInstance();
                 break;

                default:
                    /**
                     * select list view for other selections
                     */
                    mFragment= ListViewFragment.getNewInstance();
                    ((ListViewFragment)mFragment).setUseSpecificLayout(false);
                    mListViewFragment = ListViewFragment.getNewInstance();
                 break;
            }
            return mFragment;
        }

        @Override
        public int getCount() {
            return 2;
        }

        @Override
        public CharSequence getPageTitle(int position) {
           switch (position)
           {
               case 0:
                   return " Map ";
               case 1:
                   return "List ";
               default:
                   return " Map ";
           }
        }

        public int getPageIcon(int position)
        {
            switch (position)
            {
                case 0:
                    return  R.drawable.ic_action_map;

                case 1:
                    return  R.drawable.ic_action_view_as_list;

                default:
                    return  R.drawable.ic_action_map;
            }
        }
    }

    /**
     * hanlde exit with back button
     */
    @Override
    public void onBackPressed() {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
        System.exit(0);
    }



}
