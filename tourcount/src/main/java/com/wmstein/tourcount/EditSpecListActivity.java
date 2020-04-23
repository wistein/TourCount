package com.wmstein.tourcount;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import android.text.Html;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;
import com.wmstein.tourcount.database.Count;
import com.wmstein.tourcount.database.CountDataSource;
import com.wmstein.tourcount.database.Section;
import com.wmstein.tourcount.database.SectionDataSource;
import com.wmstein.tourcount.widgets.CountEditWidget;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NavUtils;
import androidx.core.content.ContextCompat;

/***************************************************************
 * EditSpecListActivity lets you edit the species list (change, delete, select 
 * and insert new species)
 * EditSpecListActivity is called from CountingActivity
 * Uses CountEditWidget.java, activity_edit_section.xml, widget_edit_count.xml.
 * Calls AddSpeciesActivity.java for adding a new species to the species list.
 *
 * Based on EditProjectActivity.java by milo on 05/05/2014.
 * Adopted, modified and enhanced for TourCount by wmstein on 2016-02-18,
 * last edited on 2020-04-23
 */
public class EditSpecListActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener, PermissionsDialogFragment.PermissionsGrantedCallback
{
    private static final String TAG = "TourCountEditSecAct";
    private static TourCountApplication tourCount;

    private ArrayList<CountEditWidget> savedCounts;
    private LinearLayout counts_area;
    private final Handler mHandler = new Handler();

    // the actual data
    private Section section;
    private SectionDataSource sectionDataSource;
    private CountDataSource countDataSource;

    private View viewMarkedForDelete;
    private int idToDelete;
    private Bitmap bMap;
    private BitmapDrawable bg;

    // Location info handling
    private double latitude, longitude;
    LocationService locationService;

    // Permission dispatcher mode modePerm: 
    //  1 = use location service
    //  2 = end location service
    int modePerm;

    // Preferences
    private boolean dupPref;
    private String sortPref;
    private boolean screenOrientL; // option for landscape screen orientation
    private boolean brightPref;
    private boolean metaPref;      // option for reverse geocoding
    private String emailString = ""; // mail address for OSM query

    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        tourCount = (TourCountApplication) getApplication();
        SharedPreferences prefs = TourCountApplication.getPrefs();
        prefs.registerOnSharedPreferenceChangeListener(this);
        dupPref = prefs.getBoolean("pref_duplicate", true);
        sortPref = prefs.getString("pref_sort_sp", "none");
        brightPref = prefs.getBoolean("pref_bright", true);
        screenOrientL = prefs.getBoolean("screen_Orientation", false);

        setContentView(R.layout.activity_edit_section);

        ScrollView counting_screen = findViewById(R.id.editingScreen);

        if (screenOrientL)
        {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }
        else
        {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }

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

        savedCounts = new ArrayList<>();
        counts_area = findViewById(R.id.editingCountsLayout);

        // Restore any edit widgets the user has added previously
        if (savedInstanceState != null)
        {
            if (savedInstanceState.getSerializable("savedCounts") != null)
            {
                //noinspection unchecked
                savedCounts = (ArrayList<CountEditWidget>) savedInstanceState.getSerializable("savedCounts");
            }
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState)
    {
        /*
         * Before these widgets can be serialised they must be removed from their parent, or else
         * trying to add them to a new parent causes a crash because they've already got one.
         */
        super.onSaveInstanceState(outState);
        for (CountEditWidget cew : savedCounts)
        {
            ((ViewGroup) cew.getParent()).removeView(cew);
        }
        outState.putSerializable("savedCounts", savedCounts);
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        SharedPreferences prefs = TourCountApplication.getPrefs();
        prefs.registerOnSharedPreferenceChangeListener(this);
        dupPref = prefs.getBoolean("pref_duplicate", true);
        screenOrientL = prefs.getBoolean("screen_Orientation", false);
        brightPref = prefs.getBoolean("pref_bright", true);
        metaPref = prefs.getBoolean("pref_metadata", false);   // use Reverse Geocoding
        emailString = prefs.getString("email_String", "");     // for reliable query of Nominatim service

        // Set full brightness of screen
        if (brightPref)
        {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            WindowManager.LayoutParams params = getWindow().getAttributes();
            params.screenBrightness = 1.0f;
            getWindow().setAttributes(params);
        }

        // Get location with permissions check
        modePerm = 1;
        permissionCaptureFragment();

        // clear any existing views
        counts_area.removeAllViews();

        // setup the data sources
        sectionDataSource = new SectionDataSource(this);
        sectionDataSource.open();
        countDataSource = new CountDataSource(this);
        countDataSource.open();

        // load the sections data
        section = sectionDataSource.getSection();
        try
        {
            //noinspection ConstantConditions
            getSupportActionBar().setTitle(section.name);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        } catch (NullPointerException e)
        {
            if (MyDebug.LOG)
                Log.e(TAG, "NullPointerException: No section name!");
        }

        // load the sorted species data
        List<Count> counts;
        switch (sortPref)
        {
        case "names_alpha":
            counts = countDataSource.getAllSpeciesSrtName();
            break;
        case "codes":
            counts = countDataSource.getAllSpeciesSrtCode();
            break;
        default:
            counts = countDataSource.getAllSpecies();
            break;
        }

        // display all the counts by adding them to CountEditWidget
        for (Count count : counts)
        {
            // widget
            CountEditWidget cew = new CountEditWidget(this, null);
            cew.setCountName(count.name);
            cew.setCountNameG(count.name_g);
            cew.setCountCode(count.code);
            cew.setCountId(count.id);
            cew.setPSpec(count);
            counts_area.addView(cew);
        }
        for (CountEditWidget cew : savedCounts)
        {
            counts_area.addView(cew);
        }

    } // end of Resume

    @Override
    protected void onPause()
    {
        super.onPause();

        // close the data sources
        sectionDataSource.close();
        countDataSource.close();

        // Stop location service with permissions check
        modePerm = 2;
        permissionCaptureFragment();
    }

    // Compare count names for duplicates and returns name of 1. duplicate found
    private String compCountNames()
    {
        String name;
        String isDbl = "";
        ArrayList<String> cmpCountNames = new ArrayList<>();

        int childcount = counts_area.getChildCount();
        // for all CountEditWidgets
        for (int i = 0; i < childcount; i++)
        {
            CountEditWidget cew = (CountEditWidget) counts_area.getChildAt(i);
            name = cew.getCountName();

            if (cmpCountNames.contains(name))
            {
                isDbl = name;
                if (MyDebug.LOG)
                    Log.d(TAG, "Double name = " + isDbl);
                break;
            }
            cmpCountNames.add(name);
        }
        return isDbl;
    }

    public void saveAndExit(View view)
    {
        if (saveData())
        {
            savedCounts.clear();
            super.finish();
        }
    }

    private boolean saveData()
    {
        // test for double entries and save species list
        boolean retValue = true;
        String isDbl;
        int childcount; //No. of species in list
        childcount = counts_area.getChildCount();
        if (MyDebug.LOG)
            Log.d(TAG, "childcount: " + childcount);

        // check for unique species names
        if (dupPref)
        {
            isDbl = compCountNames();
            if (isDbl.equals(""))
            {
                // do for all species 
                for (int i = 0; i < childcount; i++)
                {
                    CountEditWidget cew = (CountEditWidget) counts_area.getChildAt(i);
                    if (isNotEmpty(cew.getCountName()))
                    {
                        if (MyDebug.LOG)
                            Log.d(TAG, "cew: " + cew.countId + ", " + cew.getCountName());
                        
                        //updates species name and code
                        countDataSource.updateCountName(cew.countId, cew.getCountName(), cew.getCountCode(), cew.getCountNameG());
                        retValue = true;
                    }
                }
            }
            else
            {
//                Toast.makeText(this, isDbl + " " + getString(R.string.isdouble), Toast.LENGTH_SHORT).show();
                showSnackbarRed(isDbl + " " + getString(R.string.isdouble) + " "
                    + getString(R.string.duplicate));
                retValue = false;
            }
        }

        if (retValue)
        {
            // Snackbar doesn't appear, so Toast is used
            Toast.makeText(EditSpecListActivity.this, getString(R.string.sectSaving) + " " + section.name + "!", Toast.LENGTH_SHORT).show();
        }

        return retValue;
    }

    private boolean testData()
    {
        // test species list for double entry
        boolean retValue = true;
        String isDbl;

        // check for unique species names
        if (dupPref)
        {
            isDbl = compCountNames();
            if (!isDbl.equals(""))
            {
                showSnackbarRed(isDbl + " " + getString(R.string.isdouble) + " "
                    + getString(R.string.duplicate));
                retValue = false;
            }
        }

        return retValue;
    }

    private void showSnackbarRed(String str) // bold red text
    {
        View view = findViewById(R.id.editingScreen);
        Snackbar sB = Snackbar.make(view, Html.fromHtml("<font color=\"#ff0000\"><b>" + str + "</font></b>"), Snackbar.LENGTH_LONG);
        TextView tv = sB.getView().findViewById(R.id.snackbar_text);
        tv.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        sB.show();
    }

    // Start AddSpeciesActivity to add a new species to the species list
    public void newCount(View view)
    {
        Toast.makeText(getApplicationContext(), getString(R.string.wait), Toast.LENGTH_SHORT).show(); // a Snackbar here comes incomplete

        // pause for 100 msec to show toast
        mHandler.postDelayed(() -> {
            Intent intent = new Intent(EditSpecListActivity.this, AddSpeciesActivity.class);
            startActivity(intent);
        }, 100);
    }

    // Purging species
    public void deleteCount(View view)
    {
        viewMarkedForDelete = view;
        idToDelete = (Integer) view.getTag();
        if (idToDelete == 0)
        {
            // the actual CountEditWidget is 3 levels up from the button in which it is embedded
            counts_area.removeView((CountEditWidget) view.getParent().getParent().getParent());
        }
        else
        {
            AlertDialog.Builder areYouSure = new AlertDialog.Builder(this);
            areYouSure.setTitle(getString(R.string.deleteCount));
            areYouSure.setMessage(getString(R.string.reallyDeleteCount));
            areYouSure.setPositiveButton(R.string.yesDeleteIt, (dialog, whichButton) -> {
                // go ahead for the delete
                countDataSource.deleteCountById(idToDelete);
                counts_area.removeView((CountEditWidget) viewMarkedForDelete.getParent().getParent().getParent());
            });
            areYouSure.setNegativeButton(R.string.noCancel, (dialog, whichButton) -> {
                // Cancelled.
            });
            areYouSure.show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.edit_section, menu);
        return true;
    }

    // catch back button for plausi test
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        if ((keyCode == KeyEvent.KEYCODE_BACK))
        {
            if (testData())
            {
                savedCounts.clear();
                Intent intent = NavUtils.getParentActivityIntent(this);
                assert intent != null;
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                NavUtils.navigateUpTo(this, intent);
            }
            else
                return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item)
    {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        if (!testData())
            return true;

        int id = item.getItemId();
        if (id == R.id.home)
        {
            savedCounts.clear();
            Intent intent = NavUtils.getParentActivityIntent(this);
            assert intent != null;
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            NavUtils.navigateUpTo(this, intent);
        }
        else if (id == R.id.menuSaveExit)
        {
            if (saveData())
            {
                savedCounts.clear();
                super.finish();
            }
        }
        else if (id == R.id.newCount)
        {
            Toast.makeText(getApplicationContext(), getString(R.string.wait), Toast.LENGTH_SHORT).show(); // a Snackbar here comes incomplete

            // pause for 100 msec to show toast
            mHandler.postDelayed(() -> {
                Intent intent = new Intent(EditSpecListActivity.this, AddSpeciesActivity.class);
                startActivity(intent);
            }, 100);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @SuppressLint("SourceLockedOrientationActivity")
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key)
    {
        ScrollView counting_screen = findViewById(R.id.editingScreen);
        dupPref = prefs.getBoolean("pref_duplicate", true);
        screenOrientL = prefs.getBoolean("screen_Orientation", false);
        if (screenOrientL)
        {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }
        else
        {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }

        bMap = tourCount.decodeBitmap(R.drawable.kbackground, tourCount.width, tourCount.height);
        counting_screen.setBackground(null);
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

            runOnUiThread(() -> {
                URL url;
                String urlString = "https://nominatim.openstreetmap.org/reverse?email=" + emailString + "&format=xml&lat="
                    + latitude + "&lon=" + longitude + "&zoom=18&addressdetails=1";
                try
                {
                    url = new URL(urlString);
                    RetrieveAddr getXML = new RetrieveAddr(getApplicationContext());
                    getXML.execute(url);
                } catch (IOException e)
                {
                    // do nothing
                }
            });
        }
    }

    /**
     * Checks if a CharSequence is empty ("") or null.
     * <p>
     * isEmpty(null)      = true
     * isEmpty("")        = true
     * isEmpty(" ")       = false
     * isEmpty("bob")     = false
     * isEmpty("  bob  ") = false
     *
     * @param cs the CharSequence to check, may be null
     * @return {@code true} if the CharSequence is empty or null
     */
    private static boolean isEmpty(final CharSequence cs)
    {
        return cs == null || cs.length() == 0;
    }

    /**
     * Checks if a CharSequence is not empty ("") and not null.
     * <p>
     * isNotEmpty(null)      = false
     * isNotEmpty("")        = false
     * isNotEmpty(" ")       = true
     * isNotEmpty("bob")     = true
     * isNotEmpty("  bob  ") = true
     *
     * @param cs the CharSequence to check, may be null
     * @return {@code true} if the CharSequence is not empty and not null
     */
    private static boolean isNotEmpty(final CharSequence cs)
    {
        return !isEmpty(cs);
    }

}
