package com.wmstein.tourcount;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NavUtils;
import androidx.core.content.ContextCompat;

import com.wmstein.tourcount.database.Count;
import com.wmstein.tourcount.database.CountDataSource;
import com.wmstein.tourcount.widgets.EditNotesWidget;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;

/**********************************
 * CountOptionsActivity
 * Created by milo on 05/05/2014.
 * Adopted by wmstein on 18.02.2016,
 * last edited on 2023-05-13
 */
public class CountOptionsActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener, PermissionsDialogFragment.PermissionsGrantedCallback
{
    private TourCountApplication tourCount;
    private LinearLayout static_widget_area;
    private EditNotesWidget enw;
    private Count count;
    private int count_id;
    private CountDataSource countDataSource;
    private Bitmap bMap;
    private BitmapDrawable bg;

    // Preferences
    private boolean brightPref;
    private boolean metaPref;      // option for reverse geocoding
    private String emailString = ""; // mail address for OSM query

    // Location info handling
    private double latitude;
    private double longitude;
    LocationService locationService;

    // Permission dispatcher mode modePerm: 
    //  1 = use location service
    //  2 = end location service
    int modePerm;


    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        tourCount = (TourCountApplication) getApplication();
        SharedPreferences prefs = TourCountApplication.getPrefs();
        prefs.registerOnSharedPreferenceChangeListener(this);
        brightPref = prefs.getBoolean("pref_bright", true);
        metaPref = prefs.getBoolean("pref_metadata", false);   // use Reverse Geocoding
        emailString = prefs.getString("email_String", "");     // for reliable query of Nominatim service

        setContentView(R.layout.activity_count_options);

        LinearLayout counting_screen = findViewById(R.id.count_options);

        // Set full brightness of screen
        if (brightPref)
        {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            WindowManager.LayoutParams params = getWindow().getAttributes();
            params.screenBrightness = 1.0f;
            getWindow().setAttributes(params);
        }

        bMap = tourCount.decodeBitmap(R.drawable.kbackground, tourCount.width, tourCount.height);
        bg = new BitmapDrawable(counting_screen.getResources(), bMap);
        counting_screen.setBackground(bg);

        static_widget_area = findViewById(R.id.static_widget_area);

        Bundle extras = getIntent().getExtras();
        if (extras != null)
        {
            count_id = extras.getInt("count_id");
        }
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        // clear any existing views
        static_widget_area.removeAllViews();

        // Get location with permissions check
        modePerm = 1;
        permissionCaptureFragment();

        // get the data sources
        countDataSource = new CountDataSource(this);
        countDataSource.open();

        count = countDataSource.getCountById(count_id);
        Objects.requireNonNull(getSupportActionBar()).setTitle(count.name);

        enw = new EditNotesWidget(this, null);
        enw.setNotesName(count.notes);
        enw.setWidgetTitle(getString(R.string.notesSpecies));
        enw.setHint(getString(R.string.notesHint));

        static_widget_area.addView(enw);

    }

    @Override
    protected void onPause()
    {
        super.onPause();

        // finally, close the database
        countDataSource.close();

        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null)
        {
            imm.hideSoftInputFromWindow(enw.getWindowToken(), 0);
        }

        // Stop location service with permissions check
        modePerm = 2;
        permissionCaptureFragment();
    }

    public void saveAndExit(View view)
    {
        saveData();
        super.finish();
    }

    private void saveData()
    {
        // don't crash if the user hasn't filled things in...
        // Snackbar doesn't appear so Toast 
        Toast.makeText(CountOptionsActivity.this, getString(R.string.sectSaving) + " " + count.name + "!", Toast.LENGTH_SHORT).show();
        count.notes = enw.getNotesName();
        // hide keyboard
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null)
        {
            imm.hideSoftInputFromWindow(enw.getWindowToken(), 0);
        }
        countDataSource.saveCount(count);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.count_options, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == android.R.id.home)
        {
            Intent intent = NavUtils.getParentActivityIntent(this);
            assert intent != null;
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            NavUtils.navigateUpTo(this, intent);
        }
        else if (id == R.id.menuSaveExit)
        {
            saveData();
            super.finish();
        }
        return super.onOptionsItemSelected(item);
    }

    @SuppressLint("SourceLockedOrientationActivity")
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key)
    {
        LinearLayout counting_screen = findViewById(R.id.count_options);
        counting_screen.setBackground(null);
        brightPref = prefs.getBoolean("pref_bright", true);

        bMap = tourCount.decodeBitmap(R.drawable.kbackground, tourCount.width, tourCount.height);
        bg = new BitmapDrawable(counting_screen.getResources(), bMap);
        counting_screen.setBackground(bg);
    }

    @Override
    public void permissionCaptureFragment()
    {
        {
            if (isPermissionGranted())
            {
                switch (modePerm)
                {
                    case 1: // get location
                        getLoc();
                        break;
                    case 2: // stop location service
                        locationService.stopListener();
                        break;
                }
            }
            else
            {
                if (modePerm == 1)
                    PermissionsDialogFragment.newInstance().show(getSupportFragmentManager(), PermissionsDialogFragment.class.getName());
            }
        }
    }

    // if API level > 23 test for permissions granted
    private boolean isPermissionGranted()
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
        {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        }
        else
        {
            // handle permissions for Build.VERSION_CODES < M here
            return true;
        }
    }

    // get the location data
    public void getLoc()
    {
        locationService = new LocationService(this);

        if (locationService.canGetLocation())
        {
            longitude = locationService.getLongitude();
            latitude = locationService.getLatitude();
        }

        // get reverse geocoding
        if (locationService.canGetLocation() && metaPref && (latitude != 0 || longitude != 0))
        {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);

            runOnUiThread(() ->
            {
                URL url;
                String urlString = "https://nominatim.openstreetmap.org/reverse?email=" + emailString
                    + "&format=xml&lat=" + latitude + "&lon=" + longitude + "&zoom=18&addressdetails=1";
                try
                {
                    url = new URL(urlString);
                    RetrieveAddr.run(url);
                } catch (IOException e)
                {
                    // do nothing
                }
            });
        }
    }

}
