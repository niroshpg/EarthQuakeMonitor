package com.niroshpg.android.earthquakemonitor;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;

public class DetailActivity extends ActionBarActivity {

    public static final String DATE_KEY = "list_view_date";
    public static final String ID_KEY = "list_view_id";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail_new);

        if (savedInstanceState == null) {
            // Create the detail fragment and add it to the activity
            // using a fragment transaction.
            String date = getIntent().getStringExtra(DATE_KEY);
            String id = getIntent().getStringExtra(ID_KEY);

            Bundle arguments = new Bundle();
            arguments.putString(DetailActivity.DATE_KEY, date);
            arguments.putString(DetailActivity.ID_KEY, id);

            // add map detail fragment
            DetailMapFragment fragmentMap = new DetailMapFragment();
            fragmentMap.setArguments(arguments);

            getSupportFragmentManager().beginTransaction()
                    .add(R.id.detail_map_container, fragmentMap)
                    .commit();
            // add text detail fragment
            DetailTextFragment fragmentText = new DetailTextFragment();
            fragmentText.setArguments(arguments);

            getSupportFragmentManager().beginTransaction()
                    .add(R.id.detail_text_container,fragmentText)
                    .commit();

        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.detail, menu);
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
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


}
