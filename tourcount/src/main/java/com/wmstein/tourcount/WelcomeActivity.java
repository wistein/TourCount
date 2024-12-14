package com.wmstein.tourcount;

import static java.lang.Math.sqrt;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;

import com.google.android.material.snackbar.Snackbar;
import com.wmstein.egm.EarthGravitationalModel;
import com.wmstein.filechooser.AdvFileChooser;
import com.wmstein.tourcount.database.CountDataSource;
import com.wmstein.tourcount.database.DbHelper;
import com.wmstein.tourcount.database.Head;
import com.wmstein.tourcount.database.HeadDataSource;
import com.wmstein.tourcount.database.IndividualsDataSource;
import com.wmstein.tourcount.database.Section;
import com.wmstein.tourcount.database.SectionDataSource;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;

import sheetrock.panda.changelog.ChangeLog;
import sheetrock.panda.changelog.ViewHelp;
import sheetrock.panda.changelog.ViewLicense;

/**********************************************************************
 * WelcomeActivity provides the starting page with menu and buttons for
 * import/export/help/info methods and lets you call 
 * EditMetaActivity, CountingActivity and ListSpeciesActivity.
 * It uses further LocationService and PermissionDialogFragment.
 * <p>
 * Database handling is mainly done in WelcomeActivity as upgrade to current
 * DB version when importing an older DB file by importDBFile().
 * <p>
 * Based on BeeCount's WelcomeActivity.java by milo on 05/05/2014.
 * Changes and additions for TourCount by wmstein since 2016-04-18,
 * last edited on 2024-11-25
 */
public class WelcomeActivity
    extends AppCompatActivity
    implements SharedPreferences.OnSharedPreferenceChangeListener,
               PermissionsDialogFragment.PermissionsGrantedCallback
{
    private static final String TAG = "WelcomeAct";

    private TourCountApplication tourCount;

    LocationService locationService;

    // Permission dispatcher mode locationPermissionDispatcherMode: 
    //  1 = use location service
    //  2 = end location service
    private int locationPermissionDispatcherMode;

    // permLocGiven contains initial location permission state that
    // controls if location listener has to be stopped after permission changed: 
    // Stop listener if permission was denied after listener start.
    // Don't stop listener if permission was allowed later and listener has not been started
    private boolean permLocGiven;

    private ChangeLog cl;
    private ViewHelp vh;
    private ViewLicense vl;
    public boolean doubleBackToExitPressedTwice = false;

    // Location info handling
    private double latitude, longitude, height, uncertainty;

    // Import/export stuff
    private File infile;
    private File outfile;
    private boolean mExternalStorageAvailable = false;
    private boolean mExternalStorageWriteable = false;
    private final String state = Environment.getExternalStorageState();
    private final Handler mHandler = new Handler(Looper.getMainLooper());

    private AlertDialog alert;

    // Preferences
    private SharedPreferences prefs;
    private boolean metaPref;        // option for reverse geocoding
    private String emailString = ""; // mail address for OSM query
    private String outPref;

    // DB handling
    private SQLiteDatabase database;
    private DbHelper dbHandler;
    private SectionDataSource sectionDataSource;
    private Section section;
    private HeadDataSource headDataSource;
    private CountDataSource countDataSource;
    private IndividualsDataSource individualsDataSource;

    private String tourName = "";
    private View baseLayout;

    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        if (MyDebug.dLOG) Log.i(TAG, "146, onCreate");

        tourCount = (TourCountApplication) getApplication();

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        prefs = TourCountApplication.getPrefs();

        setContentView(R.layout.activity_welcome);
        baseLayout = findViewById(R.id.baseLayout);
        baseLayout.setBackground(tourCount.setBackgr());

        if (!isStorageGranted())
        {
            PermissionsDialogFragment.newInstance().show(getSupportFragmentManager(), PermissionsDialogFragment.class.getName());
            if (!isStorageGranted())
                showSnackbarRed(getString(R.string.perm_cancel));
        }

        // Check initial location permission state
        permLocGiven = isPermLocGranted();
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("permLoc_Given", permLocGiven);
        editor.apply();

        // setup the data sources
        headDataSource = new HeadDataSource(this);
        sectionDataSource = new SectionDataSource(this);
        countDataSource = new CountDataSource(this);
        individualsDataSource = new IndividualsDataSource(this);

        // Check DB and try to get tour name
        try
        {
            sectionDataSource.open();
            section = sectionDataSource.getSection();
            tourName = section.name;
            sectionDataSource.close();
        } catch (SQLiteException e)
        {
            sectionDataSource.close();
            showSnackbarRed(getString(R.string.corruptDb));

            mHandler.postDelayed(this::finishAndRemoveTask, 2000);
        }

        cl = new ChangeLog(this);
        vh = new ViewHelp(this);
        vl = new ViewLicense(this);

        // Show changelog for new version
        if (cl.firstRun())
            cl.getLogDialog().show();

        // Test for existence of file /storage/emulated/0/Documents/TourCount/tourcount0.db
        File path;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) // Android 10+
        {
            path = Environment.getExternalStorageDirectory();
            path = new File(path + "/Documents/TourCount");
        }
        else
        {
            path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
            path = new File(path + "/TourCount");
        }

        infile = new File(path, "/tourcount0.db");
        if (!infile.exists())
            exportBasisDb(); // create directory and copy internal DB-data to initial Basis DB-file

        // New onBackPressed logic
        OnBackPressedCallback callback = new OnBackPressedCallback(true)
        {
            @Override
            public void handleOnBackPressed()
            {
                if (doubleBackToExitPressedTwice)
                {
                    if (MyDebug.dLOG) Log.i(TAG, "225, handleOnBackPressed");

                    clear_loc(); // Clear last locality in tmp

                    finishAndRemoveTask();
                }

                doubleBackToExitPressedTwice = true;

                Toast t = new Toast(getApplicationContext());
                LayoutInflater inflater = getLayoutInflater();

                @SuppressLint("InflateParams")
                View toastView = inflater.inflate(R.layout.toast_view, null);
                TextView textView = toastView.findViewById(R.id.toast);
                textView.setText(R.string.back_twice);

                t.setView(toastView);
                t.setDuration(Toast.LENGTH_SHORT);
                t.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM, 0, 0);
                t.show();

                mHandler.postDelayed(() ->
                    doubleBackToExitPressedTwice = false, 1500);
            }
        };
        getOnBackPressedDispatcher().addCallback(this, callback);
    }
    // End of onCreate()

    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    protected void onResume()
    {
        super.onResume();

        if (MyDebug.dLOG) Log.i(TAG, "261, onResume");

        prefs = TourCountApplication.getPrefs();
        prefs.registerOnSharedPreferenceChangeListener(this);
        metaPref = prefs.getBoolean("pref_metadata", false);   // use Reverse Geocoding
        emailString = prefs.getString("email_String", "");    // for reliable query of Nominatim service
        outPref = prefs.getString("pref_sort_output", "names"); // sort mode csv-export

        permLocGiven = prefs.getBoolean("permLoc_Given", false);
        if (MyDebug.dLOG)
            Toast.makeText(this, "onResume permLocGiven = " + permLocGiven, Toast.LENGTH_SHORT).show();

        headDataSource.open();
        sectionDataSource.open();
        countDataSource.open();
        individualsDataSource.open();

        // Set tour name as title
        section = sectionDataSource.getSection();
        tourName = section.name;
        try
        {
            Objects.requireNonNull(getSupportActionBar()).setTitle(tourName);
        } catch (NullPointerException e)
        {
            // nothing
        }

        // Get location with permissions check
        locationPermissionDispatcherMode = 1; // get location
        locationCaptureFragment();
    }
    // End of onResume()

    // Check initial location permission
    private boolean isPermLocGranted()
    {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    // Check initial external storage permission
    private boolean isStorageGranted()
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) // Android 11+
        {
            return Environment.isExternalStorageManager(); // check permission MANAGE_EXTERNAL_STORAGE for Android 11+
        }
        else
            return ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    // Part of permission handling
    @Override
    public void locationCaptureFragment()
    {
        if (isLocPermissionGranted()) // current location permission state granted
        {
            // Handle action here
            if (MyDebug.dLOG)
                Toast.makeText(this, "Fragment permLocGiven = " + permLocGiven, Toast.LENGTH_SHORT).show();

            switch (locationPermissionDispatcherMode)
            {
                case 1 ->
                { // Get location
                    if (permLocGiven) // location permission state after start
                    {
                        getLoc();
                    }
                }
                case 2 ->
                { // Stop location service
                    if (permLocGiven)
                        locationService.stopListener();
                }
            }
        }
        else
        {
            if (locationPermissionDispatcherMode == 1)
                PermissionsDialogFragment.newInstance().show(getSupportFragmentManager(), PermissionsDialogFragment.class.getName());
        }

    }

    // If API level > 23 test for permissions granted
    private boolean isLocPermissionGranted()
    {
        return (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED);
    }

    // Get the location data
    public void getLoc()
    {
        locationService = new LocationService(this);

        if (locationService.canGetLocation())
        {
            longitude = locationService.getLongitude();
            latitude = locationService.getLatitude();
            height = locationService.getAltitude();
            if (height != 0)
                height = correctHeight(latitude, longitude, height);
            uncertainty = locationService.getAccuracy();
        }

        // Get reverse geocoding
        if (locationService.canGetLocation() && metaPref && (latitude != 0 || longitude != 0))
        {
            String urlString = "https://nominatim.openstreetmap.org/reverse?email=" + emailString
                + "&format=xml&lat=" + latitude + "&lon=" + longitude + "&zoom=18&addressdetails=1";

            // Using WorkManager
            WorkRequest retrieveAddrWorkRequest =
                new OneTimeWorkRequest.Builder(RetrieveAddrWorker.class)
                    .setInputData(new Data.Builder()
                            .putString("URL_STRING", urlString)
                            .build()
                                 )
                    .build();

            WorkManager
                .getInstance(this)
                .enqueue(retrieveAddrWorkRequest);
        }
    }

    // Correct height with geoid offset from simplified EarthGravitationalModel
    private double correctHeight(double latitude, double longitude, double gpsHeight)
    {
        double corrHeight;
        double nnHeight;

        EarthGravitationalModel gh = new EarthGravitationalModel();
        try
        {
            gh.load(this); // load the WGS84 correction coefficient table egm180.txt
        } catch (IOException e)
        {
            return 0;
        }

        // Calculate the offset between the ellipsoid and geoid
        try
        {
            corrHeight = gh.heightOffset(latitude, longitude, gpsHeight);
        } catch (Exception e)
        {
            return 0;
        }

        nnHeight = gpsHeight + corrHeight;
        return nnHeight;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.welcome, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        int id = item.getItemId();
        if (id == R.id.action_settings)
        {
            startActivity(new Intent(this, SettingsActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
            return true;
        }
        else if (id == R.id.exportMenu)
        {
            if (isStorageGranted())
            {
                exportDb();
            }
            else
            {
                PermissionsDialogFragment.newInstance().show(getSupportFragmentManager(),
                    PermissionsDialogFragment.class.getName());
                if (isStorageGranted())
                {
                    exportDb();
                }
                else
                {
                    showSnackbarRed(getString(R.string.perm_cancel));
                }
            }
            return true;
        }
        else if (id == R.id.exportCSVMenu)
        {
            if (isStorageGranted())
            {
                exportDb2CSV();
            }
            else
            {
                PermissionsDialogFragment.newInstance().show(getSupportFragmentManager(), 
                    PermissionsDialogFragment.class.getName());
                if (isStorageGranted())
                {
                    exportDb2CSV();
                }
                else
                {
                    showSnackbarRed(getString(R.string.perm_cancel));
                }
            }
            return true;
        }
        else if (id == R.id.exportBasisMenu)
        {
            if (isStorageGranted())
            {
                exportBasisDb();
            }
            else
            {
                PermissionsDialogFragment.newInstance().show(getSupportFragmentManager(),
                    PermissionsDialogFragment.class.getName());
                if (isStorageGranted())
                {
                    exportBasisDb();
                }
                else
                {
                    showSnackbarRed(getString(R.string.perm_cancel));
                }
            }
            return true;
        }
        else if (id == R.id.importBasisMenu)
        {
            importBasisDb();
            return true;
        }
        else if (id == R.id.importFileMenu)
        {
            importDBFile();
            return true;
        }
        else if (id == R.id.resetDBMenu)
        {
            resetToBasisDb();
            return true;
        }
/*
        else if (id == R.id.importListMenu)
        {
            importTransektCountList();
            return true;
        }
 */
        else if (id == R.id.viewHelp)
        {
            vh.getFullLogDialog().show();
            return true;
        }
        else if (id == R.id.changeLog)
        {
            cl.getFullLogDialog().show();
            return true;
        }
        else if (id == R.id.viewLicense)
        {
            vl.getFullLogDialog().show();
            return true;
        }
        else if (id == R.id.startCounting)
        {
            // Call CountingActivity
            Intent intent;
            intent = new Intent(WelcomeActivity.this,
                CountingActivity.class);
            intent.putExtra("Latitude", latitude);
            intent.putExtra("Longitude", longitude);
            intent.putExtra("Height", height);
            intent.putExtra("Uncert", uncertainty);
            startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
            return true;
        }
        else if (id == R.id.editMeta)
        {
            // Call EditMetaActivity
            startActivity(new Intent(this, EditMetaActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
            return true;
        }
        else if (id == R.id.viewSpecies)
        {
            Toast.makeText(getApplicationContext(), getString(R.string.wait),
                Toast.LENGTH_SHORT).show(); // a Snackbar here comes incomplete

            // Trick: Pause for 100 msec to show toast
            mHandler.postDelayed(() ->
                startActivity(new Intent(getApplicationContext(), ListSpeciesActivity
                    .class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)), 100);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    // End of onOptionsItemSelected

    public void onSharedPreferenceChanged(SharedPreferences prefs, String key)
    {
        baseLayout = findViewById(R.id.baseLayout);
        baseLayout.setBackground(tourCount.setBackgr());
        outPref = prefs.getString("pref_sort_output", "names");
        metaPref = prefs.getBoolean("pref_metadata", false);   // use Reverse Geocoding
        emailString = prefs.getString("email_String", "");     // for reliable query of Nominatim service
        permLocGiven = prefs.getBoolean("permLoc_Given", false);
    }

    public void onPause()
    {
        super.onPause();

        if (MyDebug.dLOG) Log.i(TAG, "586, onPause");

        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("permLoc_Given", permLocGiven);
        editor.apply();

        headDataSource.close();
        individualsDataSource.close();
        countDataSource.close();
        sectionDataSource.close();

        // Stop location service with permissions check
        locationPermissionDispatcherMode = 2;
        locationCaptureFragment();

        // Stop RetrieveAddrWorker
        WorkManager.getInstance(this).cancelAllWork();

        prefs.unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onStop()
    {
        super.onStop();

        if (MyDebug.dLOG) Log.i(TAG, "612, onStop");
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();

        if (MyDebug.dLOG) Log.i(TAG, "620, onDestroy");
    }

    // Handle button click "Counting" here
    public void startCounting(View view)
    {
        Intent intent;
        intent = new Intent(WelcomeActivity.this, CountingActivity.class);
        intent.putExtra("Latitude", latitude);
        intent.putExtra("Longitude", longitude);
        intent.putExtra("Height", height);
        intent.putExtra("Uncert", uncertainty);
        startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
    }

    // Handle button click "Prepare Inspection" here
    public void editMeta(View view)
    {
        startActivity(new Intent(this, EditMetaActivity.class)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
    }

    // Handle button click "Show Results" here
    public void viewSpecies(View view)
    {
        Toast.makeText(getApplicationContext(), getString(R.string.wait), Toast.LENGTH_SHORT).show(); // a Snackbar here comes incomplete

        // Trick: Pause for 100 msec to show toast
        mHandler.postDelayed(() ->
            startActivity(new Intent(getApplicationContext(), ListSpeciesActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)), 100);
    }

    // Date for filename of Export-DB
    private static String getcurDate()
    {
        Date date = new Date();
        @SuppressLint("SimpleDateFormat")
        DateFormat dform = new SimpleDateFormat("yyyy-MM-dd_HHmmss");
        return dform.format(date);
    }

    /****************************************************************************
     * The six functions below are for exporting and importing of database files.
     * They've been put here because no database should be open at this point.
     */
    @SuppressLint({"SdCardPath", "LongLogTag"})
    private void exportDb()
    {
        // New data directory
        // outfile -> Public Directory Documents/TourCount/
        File path;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) // Android 10+
        {
            path = Environment.getExternalStorageDirectory();
            path = new File(path + "/Documents/TourCount");
        }
        else
        {
            path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
            path = new File(path + "/TourCount");
        }

        //noinspection ResultOfMethodCallIgnored
        path.mkdirs(); // just verify path, result ignored

        // outfile -> /storage/emulated/0/Documents/TourCount/tourcount_yyyy-MM-dd_HHmmss.db
        if (Objects.equals(tourName, ""))
            outfile = new File(path, "/tourcount_" + getcurDate() + ".db");
        else
            outfile = new File(path, "/tourcount_" + tourName + "_" + getcurDate() + ".db");

        // infile <- /data/data/com.wmstein.tourcount/databases/tourcount.db
        String inPath = getApplicationContext().getFilesDir().getPath();
        inPath = inPath.substring(0, inPath.lastIndexOf("/"))
            + "/databases/tourcount.db";
        infile = new File(inPath);

        if (Environment.MEDIA_MOUNTED.equals(state))
        {
            // We can read and write the media
            mExternalStorageAvailable = mExternalStorageWriteable = true;
        }
        else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state))
        {
            // We can only read the media
            mExternalStorageAvailable = true;
            mExternalStorageWriteable = false;
        }
        else
        {
            // Something else is wrong. It may be one of many other states, but all we need
            //  to know is we can neither read nor write
            mExternalStorageAvailable = mExternalStorageWriteable = false;
        }

        if ((!mExternalStorageAvailable) || (!mExternalStorageWriteable))
        {
            if (MyDebug.dLOG) Log.e(TAG, "718, No sdcard access");
            showSnackbarRed(getString(R.string.noCard));
        }
        else
        {
            // Export the db
            try
            {
                copy(infile, outfile);
                showSnackbar(getString(R.string.saveDB));
            } catch (IOException e)
            {
                showSnackbarRed(getString(R.string.saveFail));
            }
        }
    }
    // End of exportDb()

    /************************************************************
     // Exports DB contents as tourcount_yyyy-MM-dd_HHmmss.csv to
     // Documents/TourCount/ with purged data set.
     // Spreadsheet programs can import this csv file with
     //   - Unicode UTF-8 filter,
     //   - comma delimiter and
     //   - "" for text recognition.
     */
    private void exportDb2CSV()
    {
        // outfile -> /storage/emulated/0/Documents/TourCount/tourcount_yyyy-MM-dd_HHmmss.csv
        //   and distinguish versions (as getExternalStoragePublicDirectory is deprecated in Q, Android 10)
        File path;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) // Android 11+
        {
            path = Environment.getExternalStorageDirectory();
            path = new File(path + "/Documents/TourCount");
        }
        else
        {
            path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
            path = new File(path + "/TourCount");
        }

        //noinspection ResultOfMethodCallIgnored
        path.mkdirs(); // Just verify path, result ignored

        if (Objects.equals(tourName, ""))
            outfile = new File(path, "/tourcount_" + getcurDate() + ".csv");
        else
            outfile = new File(path, "/tourcount_" + tourName + "_" + getcurDate() + ".csv");

        String sectName;
        String sectNotes;

        Head head;
        String country, inspecName;
        int temps, winds, clouds, tempe, winde, cloude;
        String plz, city, place;
        String date, start_tm, end_tm;
        int spstate;
        String spstate0;
        double longi, lati, heigh, uncer;
        int frst, sum = 0;
        int summf = 0, summ = 0, sumf = 0, sump = 0, suml = 0, sume = 0;
        String sumMF = "", sumM = "", sumF = "", sumP = "", sumL = "", sumE = "";
        double lo, la, loMin = 0, loMax = 0, laMin = 0, laMax = 0, uc, uncer1 = 0;

        if (Environment.MEDIA_MOUNTED.equals(state))
        {
            // We can read and write the media
            mExternalStorageAvailable = mExternalStorageWriteable = true;
        }
        else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state))
        {
            // We can only read the media
            mExternalStorageAvailable = true;
            mExternalStorageWriteable = false;
        }
        else
        {
            // Something else is wrong. It may be one of many other states, but all we need
            //  to know is we can neither read nor write
            mExternalStorageAvailable = mExternalStorageWriteable = false;
        }

        if ((!mExternalStorageAvailable) || (!mExternalStorageWriteable))
        {
            if (MyDebug.dLOG) Log.e(TAG, "804, No sdcard access");
            showSnackbarRed(getString(R.string.noCard));
        }
        else
        {
            // Sort mode species list
            String sortMode;
            if (outPref.equals("names"))
            {
                sortMode = getString(R.string.sort_names);
            }
            else
            {
                sortMode = getString(R.string.sort_codes);
            }
            // Export the purged count table to csv
            try
            {
                // Export purged db as csv
                CSVWriter csvWrite = new CSVWriter(new FileWriter(outfile));

                // Consult Section an Head tables for head and meta info
                section = sectionDataSource.getSection();

                sectName = section.name;
                sectNotes = section.notes;
                country = section.country;
                plz = section.plz;
                city = section.city;
                place = section.place;

                head = headDataSource.getHead();

                inspecName = head.observer;

                String[] arrHead =
                    {
                        getString(R.string.zlist) + ":", // Count List:
                        sectName,          // Section name
                        "", "",
                        getString(R.string.inspector),     // Inspector:
                        inspecName,        // Inspector name
                        "", "", "",
                        sortMode
                    };
                csvWrite.writeNext(arrHead);

                // 2nd row
                String[] arrRow2 =
                    {
                        "", "", "", "", "", "", "", "", "",
                        getString(R.string.sort_time)
                    };
                csvWrite.writeNext(arrRow2);

                // Set location headline
                String[] arrLocHead =
                    {
                        getString(R.string.country),
                        getString(R.string.plz),
                        getString(R.string.city),
                        getString(R.string.place),
                        getString(R.string.zlnotes)
                    };
                csvWrite.writeNext(arrLocHead);

                // Set location dataline
                String[] arrLocation =
                    {
                        country,
                        plz,
                        city,
                        place,
                        sectNotes
                    };
                csvWrite.writeNext(arrLocation);

                // Empty row
                String[] arrEmpt = {};
                csvWrite.writeNext(arrEmpt);

                // Set environment headline
                String[] arrEnvHead =
                    {
                        getString(R.string.date),
                        "",
                        getString(R.string.tm),
                        getString(R.string.temperature),
                        getString(R.string.wind),
                        getString(R.string.clouds)
                    };
                csvWrite.writeNext(arrEnvHead);

                // Set environment data
                temps = section.tmp;
                tempe = section.tmp_end;
                winds = section.wind;
                winde = section.wind_end;
                clouds = section.clouds;
                cloude = section.clouds_end;
                date = section.date;
                start_tm = section.start_tm;
                end_tm = section.end_tm;

                // Write environment data
                String[] arrEnvironment =
                    {
                        date,
                        getString(R.string.starttm),
                        start_tm,
                        String.valueOf(temps),
                        String.valueOf(winds),
                        String.valueOf(clouds)
                    };
                csvWrite.writeNext(arrEnvironment);

                // Write environment data
                String[] arrEnvironment2 =
                    {
                        "",
                        getString(R.string.endtm),
                        end_tm,
                        String.valueOf(tempe),
                        String.valueOf(winde),
                        String.valueOf(cloude)
                    };
                csvWrite.writeNext(arrEnvironment2);

                // Empty row
                csvWrite.writeNext(arrEmpt);

                // Write counts headline
                //    Species Name, Local Name, Code, Counts, Spec.-Notes
                String[] arrCntHead =
                    {
                        getString(R.string.name_spec),
                        getString(R.string.name_spec_g),
                        getString(R.string.speccode),
                        getString(R.string.cntsmf),
                        getString(R.string.cntsm),
                        getString(R.string.cntsf),
                        getString(R.string.cntsp),
                        getString(R.string.cntsl),
                        getString(R.string.cntse),
                        getString(R.string.bema)
                    };
                csvWrite.writeNext(arrCntHead);

                // Write counts data
                dbHandler = new DbHelper(this);
                database = dbHandler.getWritableDatabase();

                Cursor curCSVCnt; // Cursor for Counts table

                // Sort mode species list
                if (outPref.equals("names"))
                {
                    curCSVCnt = database.rawQuery("select * from " + DbHelper.COUNT_TABLE
                        + " WHERE " + " ("
                        + DbHelper.C_COUNT_F1I + " > 0 or " + DbHelper.C_COUNT_F2I + " > 0 or "
                        + DbHelper.C_COUNT_F3I + " > 0 or " + DbHelper.C_COUNT_PI + " > 0 or "
                        + DbHelper.C_COUNT_LI + " > 0 or " + DbHelper.C_COUNT_EI + " > 0)"
                        + " order by " + DbHelper.C_NAME, null, null);
                }
                else
                {
                    curCSVCnt = database.rawQuery("select * from " + DbHelper.COUNT_TABLE
                        + " WHERE " + " ("
                        + DbHelper.C_COUNT_F1I + " > 0 or " + DbHelper.C_COUNT_F2I + " > 0 or "
                        + DbHelper.C_COUNT_F3I + " > 0 or " + DbHelper.C_COUNT_PI + " > 0 or "
                        + DbHelper.C_COUNT_LI + " > 0 or " + DbHelper.C_COUNT_EI + " > 0)"
                        + " order by " + DbHelper.C_CODE, null, null);
                }

                // Get the number of individuals with attributes
                int cnts; // individuals icount
                String strcnts;
                int cntsmf; // Imago male or female
                String strcntsmf;
                int cntsm = 0; // Imago male
                String strcntsm;
                int cntsf = 0; // Imago female
                String strcntsf;
                int cntsp = 0; // Pupa
                String strcntsp;
                int cntsl = 0; // Caterpillar
                String strcntsl;
                int cntse = 0; // Egg
                String strcntse;
                String male = "m";
                String fmale = "f";
                String stadium1 = getString(R.string.stadium_1);
                String stadium2 = getString(R.string.stadium_2);
                String stadium3 = getString(R.string.stadium_3);
                String stadium4 = getString(R.string.stadium_4);

                String spname;
                String spcode;
                String slct; // Recording time to sort individuals

                Cursor curCSVInd; // Cursor for Individuals table

                while (curCSVCnt.moveToNext())
                {
                    spname = curCSVCnt.getString(7); // species name from count table
                    spcode = curCSVCnt.getString(8); // species code from count table
                    slct = "SELECT * FROM " + DbHelper.INDIVIDUALS_TABLE + " WHERE "
                        + DbHelper.I_NAME + " = ? AND "
                        + DbHelper.I_SEX + " = ? AND "
                        + DbHelper.I_STADIUM + " = ?";

                    // Select male
                    curCSVInd = database.rawQuery(slct, new String[]{spname, male, stadium1});
                    while (curCSVInd.moveToNext())
                    {
                        cnts = curCSVInd.getInt(14); // individuals icount
                        cntsm = cntsm + cnts;
                    }
                    curCSVInd.close();

                    // Select female
                    curCSVInd = database.rawQuery(slct, new String[]{spname, fmale, stadium1});
                    while (curCSVInd.moveToNext())
                    {
                        cnts = curCSVInd.getInt(14); // individuals icount
                        cntsf = cntsf + cnts;
                    }
                    curCSVInd.close();

                    String slct1 = "SELECT * FROM " + DbHelper.INDIVIDUALS_TABLE
                        + " WHERE " + DbHelper.I_NAME + " = ? AND " + DbHelper.I_STADIUM + " = ?";

                    // Select pupa
                    curCSVInd = database.rawQuery(slct1, new String[]{spname, stadium2});
                    while (curCSVInd.moveToNext())
                    {
                        cnts = curCSVInd.getInt(14); // individuals icount
                        cntsp = cntsp + cnts;
                    }
                    curCSVInd.close();

                    // Select caterpillar
                    curCSVInd = database.rawQuery(slct1, new String[]{spname, stadium3}); // select caterpillar
                    while (curCSVInd.moveToNext())
                    {
                        cnts = curCSVInd.getInt(14); // individuals icount
                        cntsl = cntsl + cnts;
                    }
                    curCSVInd.close();

                    // Select egg
                    curCSVInd = database.rawQuery(slct1, new String[]{spname, stadium4}); // select egg
                    while (curCSVInd.moveToNext())
                    {
                        cnts = curCSVInd.getInt(14); // individuals icount
                        cntse = cntse + cnts;
                    }
                    curCSVInd.close();

                    cntsmf = curCSVCnt.getInt(1);
                    cntsm = curCSVCnt.getInt(2);
                    cntsf = curCSVCnt.getInt(3);
                    cntsp = curCSVCnt.getInt(4);
                    cntsl = curCSVCnt.getInt(5);
                    cntse = curCSVCnt.getInt(6);

                    if (cntsmf > 0) // suppress '0' in output
                        strcntsmf = Integer.toString(cntsmf);
                    else
                        strcntsmf = "";
                    if (cntsm > 0)
                        strcntsm = Integer.toString(cntsm);
                    else
                        strcntsm = "";
                    if (cntsf > 0)
                        strcntsf = Integer.toString(cntsf);
                    else
                        strcntsf = "";
                    if (cntsp > 0)
                        strcntsp = Integer.toString(cntsp);
                    else
                        strcntsp = "";
                    if (cntsl > 0)
                        strcntsl = Integer.toString(cntsl);
                    else
                        strcntsl = "";
                    if (cntse > 0)
                        strcntse = Integer.toString(cntse);
                    else
                        strcntse = "";

                    // Species table
                    String[] arrStr =
                        {
                            spname,                 // species name
                            curCSVCnt.getString(10), // local name
                            spcode,                 // species code
                            strcntsmf,              // count ♂ o. ♀
                            strcntsm,               // count ♂
                            strcntsf,               // count ♀
                            strcntsp,               // count pupa
                            strcntsl,               // count caterpillar
                            strcntse,               // count egg
                            curCSVCnt.getString(9)  // species notes
                        };
                    csvWrite.writeNext(arrStr);

                    sum = sum + cntsmf + cntsm + cntsf + cntsp + cntsl + cntse;
                    summf = summf + cntsmf;
                    summ = summ + cntsm;
                    sumf = sumf + cntsf;
                    sump = sump + cntsp;
                    suml = suml + cntsl;
                    sume = sume + cntse;

                    // Suppress 0 by blank
                    if (summf == 0)
                        sumMF = "";
                    else
                        sumMF = Integer.toString(summf);

                    if (summ == 0)
                        sumM = "";
                    else
                        sumM = Integer.toString(summ);

                    if (sumf == 0)
                        sumF = "";
                    else
                        sumF = Integer.toString(sumf);

                    if (sump == 0)
                        sumP = "";
                    else
                        sumP = Integer.toString(sump);

                    if (suml == 0)
                        sumL = "";
                    else
                        sumL = Integer.toString(suml);

                    if (sume == 0)
                        sumE = "";
                    else
                        sumE = Integer.toString(sume);

                    cntsm = 0;
                    cntsf = 0;
                    cntsp = 0;
                    cntsl = 0;
                    cntse = 0;
                }
                curCSVCnt.close();

                int sumSpec = countDataSource.getDiffSpec(); // get number of different species

                // Write total sum
                String[] arrSum =
                    {
                        getString(R.string.sumSpec),
                        Integer.toString(sumSpec),
                        getString(R.string.sum),
                        sumMF,
                        sumM,
                        sumF,
                        sumP,
                        sumL,
                        sumE,
                        getString(R.string.sum_total),
                        Integer.toString(sum)
                    };
                csvWrite.writeNext(arrSum);
                // End of Species table

                // Empty row
                csvWrite.writeNext(arrEmpt);

                // Individuals table
                // Write individual headline
                //    Individuals, Counts, Locality, Longitude, Latitude, Uncertainty, Height,
                //    Date, Time, Sexus, Phase, State, Indiv.-Notes 
                String[] arrIndHead =
                    {
                        getString(R.string.individuals) + ":",
                        getString(R.string.cnts) + ":",
                        getString(R.string.locality) + ":",
                        getString(R.string.ycoord),
                        getString(R.string.xcoord),
                        getString(R.string.uncerti),
                        getString(R.string.zcoord),
                        getString(R.string.date),
                        getString(R.string.time) + ":",
                        getString(R.string.sex) + ":",
                        getString(R.string.stadium) + ":",
                        getString(R.string.state1) + ":",
                        getString(R.string.bemi) + ":"
                    };
                csvWrite.writeNext(arrIndHead);

                // Build the sorted individuals array
                curCSVInd = database.rawQuery("select * from " + DbHelper.INDIVIDUALS_TABLE
                        + " order by " + DbHelper.I_DATE_STAMP + ", " + DbHelper.I_TIME_STAMP,
                    null, null);

                String lngi, latit;
                frst = 0;
                while (curCSVInd.moveToNext())
                {
                    longi = curCSVInd.getDouble(4);
                    lati = curCSVInd.getDouble(3);
                    uncer = Math.rint(curCSVInd.getDouble(6));
                    heigh = Math.rint(curCSVInd.getDouble(5));
                    spstate = curCSVInd.getInt(12);
                    if (spstate == 0)
                        spstate0 = "-";
                    else
                        spstate0 = Integer.toString(spstate);
                    cnts = curCSVInd.getInt(14);
                    if (cnts > 0)
                        strcnts = String.valueOf(cnts);
                    else
                        strcnts = "";

                    try
                    {
                        lngi = String.valueOf(longi).substring(0, 8); // longitude
                    } catch (StringIndexOutOfBoundsException e)
                    {
                        lngi = String.valueOf(longi);
                    }

                    try
                    {
                        latit = String.valueOf(lati).substring(0, 8); // latitude
                    } catch (StringIndexOutOfBoundsException e)
                    {
                        latit = String.valueOf(lati);
                    }

                    String[] arrIndividual =
                        {
                            curCSVInd.getString(2), // species name
                            strcnts,                   // indiv. counts
                            curCSVInd.getString(9), // locality
                            lngi,                      // longitude
                            latit,                     // latitude
                            String.valueOf(Math.round(uncer + 20)), // uncertainty + 20 m extra
                            String.valueOf(Math.round(heigh)),      // height
                            curCSVInd.getString(7),  // date
                            curCSVInd.getString(8),  // time
                            curCSVInd.getString(10), // sexus
                            curCSVInd.getString(11), // stadium
                            spstate0,                   // state
                            curCSVInd.getString(13)  // indiv. notes
                        };
                    csvWrite.writeNext(arrIndividual);

                    if (longi != 0) // Has coordinates
                    {
                        if (MyDebug.dLOG) Log.d(TAG, "1263, longi " + longi);
                        if (frst == 0)
                        {
                            loMin = longi;
                            loMax = longi;
                            laMin = lati;
                            laMax = lati;
                            uncer1 = uncer;
                            frst = 1; // Just 1 with coordinates
                        }
                        else
                        {
                            loMin = Math.min(loMin, longi);
                            loMax = Math.max(loMax, longi);
                            laMin = Math.min(laMin, lati);
                            laMax = Math.max(laMax, lati);
                            uncer1 = Math.max(uncer1, uncer);
                        }
                    }
                }
                curCSVInd.close();

                // Empty row
                csvWrite.writeNext(arrEmpt);

                // Write Average Coords
                String[] arrACoordHead =
                    {
                        "",
                        "",
                        "",
                        getString(R.string.ycoord),
                        getString(R.string.xcoord),
                        getString(R.string.uncerti)
                    };
                csvWrite.writeNext(arrACoordHead);

                lo = (loMax + loMin) / 2;   // average longitude
                la = (laMax + laMin) / 2;   // average latitude

                // Simple distance calculation between 2 coordinates within the temperate zone in meters (Pythagoras):
                //   uc = (((loMax-loMin)*71500)² + ((laMax-laMin)*111300)²)½ 
                uc = sqrt(((Math.pow((loMax - loMin) * 71500, 2)) + (Math.pow((laMax - laMin) * 111300, 2))));
                uc = Math.rint(uc / 2) + 20; // average uncertainty radius + default gps uncertainty
                if (uc <= uncer1)
                    uc = uncer1;

                try
                {
                    lngi = String.valueOf(lo).substring(0, 8); //longitude
                } catch (StringIndexOutOfBoundsException e)
                {
                    lngi = String.valueOf(lo);
                }

                try
                {
                    latit = String.valueOf(la).substring(0, 8); // latitude
                } catch (StringIndexOutOfBoundsException e)
                {
                    latit = String.valueOf(la);
                }


                String[] arrAvCoords =
                    {
                        "",
                        "",
                        getString(R.string.avCoords),
                        lngi, // average longitude
                        latit, // average latitude
                        String.valueOf(Math.round(uc)) // average uncertainty radius
                    };
                csvWrite.writeNext(arrAvCoords);

                csvWrite.close();
                dbHandler.close();

                showSnackbar(getString(R.string.savecsv));
            } catch (IOException e)
            {
                if (MyDebug.dLOG) Log.e(TAG, "1344, Failed to export csv file");
                showSnackbarRed(getString(R.string.saveFail));
            }
        }
    }
    // End of exportDb2CSV()

    /***************************************************************************************/
    // Exports Basis DB to Documents/TourCount/tourcount0.db
    private void exportBasisDb()
    {
        // infile <- /data/data/com.wmstein.tourcount/databases/tourcount.db
        String inPath = getApplicationContext().getFilesDir().getPath();
        inPath = inPath.substring(0, inPath.lastIndexOf("/")) + "/databases/tourcount.db";
        infile = new File(inPath);

        // tmpfile -> /data/data/com.wmstein.tourcount/files/tourcount_tmp.db
        String tmpPath = getApplicationContext().getFilesDir().getPath();
        tmpPath = tmpPath.substring(0, tmpPath.lastIndexOf("/")) + "/files/tourcount_tmp.db";
        File tmpfile = new File(tmpPath);

        // New data directory
        // outfile in Public Directory Documents/TourCount/
        // distinguish versions (as getExternalStoragePublicDirectory is deprecated in Q, Android 10)
        File path;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) // Android 10+
        {
            path = Environment.getExternalStorageDirectory();
            path = new File(path + "/Documents/TourCount");
        }
        else
        {
            path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
            path = new File(path + "/TourCount");
        }

        //noinspection ResultOfMethodCallIgnored
        path.mkdirs(); // just verify path, result ignored
        outfile = new File(path, "/tourcount0.db");

        if (Environment.MEDIA_MOUNTED.equals(state))
        {
            // We can read and write the media
            mExternalStorageAvailable = mExternalStorageWriteable = true;
        }
        else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state))
        {
            // We can only read the media
            mExternalStorageAvailable = true;
            mExternalStorageWriteable = false;
        }
        else
        {
            // Something else is wrong. It may be one of many other states, but all we need
            //  to know is we can neither read nor write
            mExternalStorageAvailable = mExternalStorageWriteable = false;
        }

        if ((!mExternalStorageAvailable) || (!mExternalStorageWriteable))
        {
            if (MyDebug.dLOG) Log.e(TAG, "1404, No sdcard access");
            showSnackbarRed(getString(R.string.noCard));
        }
        else
        {
            // Export the basic db
            try
            {
                // Save current db as backup db tmpfile
                copy(infile, tmpfile);

                // Clear DB values for basic DB
                clearDBValues();

                // Write Basis DB
                copy(infile, outfile);

                // Restore actual db from tmpfile
                copy(tmpfile, infile);

                // Delete backup db
                boolean d0 = tmpfile.delete();
                if (d0)
                {
                    showSnackbar(getString(R.string.saveDB));
                }
            } catch (IOException e)
            {
                if (MyDebug.dLOG) Log.e(TAG, "1432, Failed to export Basic DB");
                showSnackbarRed(getString(R.string.saveFail));
            }
        }
    }
    // End of exportBasisDb()

    /*************************************************/
    // Clear all relevant DB values, reset to basic DB 
    private void resetToBasisDb()
    {
        // Confirm dialogue before anything else takes place
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setIcon(android.R.drawable.ic_dialog_alert);
        builder.setMessage(R.string.confirmResetDB);
        builder.setCancelable(false);
        builder.setPositiveButton(R.string.deleteButton, (dialog, id) ->
        {
            boolean r_ok = clearDBValues();
            if (r_ok)
            {
                showSnackbar(getString(R.string.reset2basic));
            }
            Objects.requireNonNull(getSupportActionBar()).setTitle("");
        });
        builder.setNegativeButton(R.string.cancelButton, (dialog, id) -> dialog.cancel());
        alert = builder.create();
        alert.show();
    }

    // Clear temp_loc in tmp
    private void clear_loc()
    {
        dbHandler = new DbHelper(this);
        database = dbHandler.getWritableDatabase();

        // String sql = "UPDATE " + DbHelper.TEMP_TABLE + " SET " + DbHelper.T_TEMP_LOC + " = '';";
        String sql = "UPDATE tmp SET temp_loc = '';";  // tmp is a table name!
        database.execSQL(sql);
        dbHandler.close();
    }

    // Clear DB values for basic DB
    private boolean clearDBValues()
    {
        dbHandler = new DbHelper(this);
        database = dbHandler.getWritableDatabase();
        boolean r_ok = true;

        try
        {
            String sql = "UPDATE " + DbHelper.COUNT_TABLE + " SET "
                + DbHelper.C_COUNT_F1I + " = 0, "
                + DbHelper.C_COUNT_F2I + " = 0, "
                + DbHelper.C_COUNT_F3I + " = 0, "
                + DbHelper.C_COUNT_PI + " = 0, "
                + DbHelper.C_COUNT_LI + " = 0, "
                + DbHelper.C_COUNT_EI + " = 0, "
                + DbHelper.C_NOTES + " = '';";
            database.execSQL(sql);

            sql = "UPDATE " + DbHelper.SECTION_TABLE + " SET "
                + DbHelper.S_NAME + " = '', "
                + DbHelper.S_PLZ + " = '', "
                + DbHelper.S_CITY + " = '', "
                + DbHelper.S_PLACE + " = '', "
                + DbHelper.S_TEMPE + " = 0, "
                + DbHelper.S_WIND + " = 0, "
                + DbHelper.S_CLOUDS + " = 0, "
                + DbHelper.S_TEMPE_END + " = 0, "
                + DbHelper.S_WIND_END + " = 0, "
                + DbHelper.S_CLOUDS_END + " = 0, "
                + DbHelper.S_DATE + " = '', "
                + DbHelper.S_START_TM + " = '', "
                + DbHelper.S_END_TM + " = '', "
                + DbHelper.S_NOTES + " = '';";
            database.execSQL(sql);

            sql = "DELETE FROM " + DbHelper.INDIVIDUALS_TABLE;
            database.execSQL(sql);

            sql = "UPDATE " + DbHelper.TEMP_TABLE + " SET "
                + DbHelper.T_TEMP_LOC + " = '', "
                + DbHelper.T_TEMP_CNT + " = 0;";
            database.execSQL(sql);

            dbHandler.close();
        } catch (Exception e)
        {
            if (MyDebug.dLOG) Log.e(TAG, "1521, Failed to reset DB");
            showSnackbarRed(getString(R.string.resetFail));
            dbHandler.close();
            r_ok = false;
        }
        return r_ok;
    }

    /************************************************************/
    // Choose a tourcount db-file to load and set it to tourcount.db
    private void importDBFile()
    {
        String extension = ".db";
        String filterFileName = "tourcount";

        Intent intent;
        intent = new Intent(this, AdvFileChooser.class);
        intent.putExtra("filterFileExtension", extension);
        intent.putExtra("filterFileName", filterFileName);
        myActivityResultLauncher.launch(intent);
    }

    // ActivityResultLauncher is part of importDBFile()
    // and processes the result of AdvFileChooser
    final ActivityResultLauncher<Intent> myActivityResultLauncher = registerForActivityResult(
        new ActivityResultContracts.StartActivityForResult(),
        new ActivityResultCallback<>()
        {
            @Override
            public void onActivityResult(ActivityResult result)
            {
                String selectedFile;
                infile = null;
                if (result.getResultCode() == Activity.RESULT_OK)
                {
                    Intent data = result.getData();
                    if (data != null)
                    {
                        selectedFile = data.getStringExtra("fileSelected");
                        if (MyDebug.dLOG)
                            Log.d(TAG, "1561, File selected: " + selectedFile);

                        if (selectedFile != null)
                            infile = new File(selectedFile);
                        else
                            infile = null;
                    }
                }

                // outfile = "/data/data/com.wmstein.tourcount/databases/tourcount.db"
                String destPath = getApplicationContext().getFilesDir().getPath();
                destPath = destPath.substring(0, destPath.lastIndexOf("/")) + "/databases/tourcount.db";
                outfile = new File(destPath);

                if (infile != null)
                {
                    AlertDialog.Builder builder = new AlertDialog.Builder(WelcomeActivity.this);
                    builder.setIcon(android.R.drawable.ic_dialog_alert);
                    builder.setMessage(R.string.confirmDBImport);
                    builder.setCancelable(false);
                    builder.setPositiveButton(R.string.importButton, (dialog, id) ->
                    {
                        try
                        {
                            copy(infile, outfile);

                            // save values for initial countId and itemposition
                            SharedPreferences.Editor editor = prefs.edit();
                            editor.putInt("count_id", 1);
                            editor.putInt("item_Position", 0);
                            editor.apply();

                            section = sectionDataSource.getSection();
                            showSnackbar(getString(R.string.importWin));

                            // List tour name as title
                            try
                            {
                                Objects.requireNonNull(getSupportActionBar()).setTitle(section.name);
                            } catch (NullPointerException e)
                            {
                                // nothing
                            }
                        } catch (IOException e)
                        {
                            showSnackbarRed(getString(R.string.importFail));
                        }
                    });
                    builder.setNegativeButton(R.string.cancelButton, (dialog, id) -> dialog.cancel());
                    alert = builder.create();
                    alert.show();
                }
            }
        });

    /**************************************************************************************************/
    // Import the basic DB
    private void importBasisDb()
    {
        // infile <- /storage/emulated/0/Documents/TourCount/tourcount0.db
        File path;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) // Android 10+
        {
            path = Environment.getExternalStorageDirectory();
            path = new File(path + "/Documents/TourCount");
        }
        else
        {
            path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
            path = new File(path + "/TourCount");
        }
        infile = new File(path, "/tourcount0.db");

        // outfile -> /data/data/com.wmstein.tourcount/databases/tourcount.db
        String destPath = getApplicationContext().getFilesDir().getPath();
        destPath = destPath.substring(0, destPath.lastIndexOf("/")) + "/databases/tourcount.db";
        outfile = new File(destPath);
        if (!(infile.exists()))
        {
            showSnackbar(getString(R.string.noDb));
            return;
        }

        // Confirm dialogue before importing
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setIcon(android.R.drawable.ic_dialog_alert);
        builder.setMessage(R.string.confirmBasisImport);
        builder.setCancelable(false).setPositiveButton(R.string.importButton, (dialog, id) ->
        {
            try
            {
                copy(infile, outfile);
                showSnackbar(getString(R.string.importWin));

                // Save values for initial count-id and itemposition
                SharedPreferences.Editor editor = prefs.edit();
                editor.putInt("count_id", 1);
                editor.putInt("item_Position", 0);
                editor.apply();

                Objects.requireNonNull(getSupportActionBar()).setTitle("");
            } catch (IOException e)
            {
                if (MyDebug.dLOG) Log.e(TAG, "1664, Failed to import database");
                showSnackbarRed(getString(R.string.importFail));
            }
        }).setNegativeButton(R.string.cancelButton, (dialog, id) -> dialog.cancel());
        alert = builder.create();
        alert.show();
    }

    /**********************************************************************************************/
    // http://stackoverflow.com/questions/9292954/how-to-make-a-copy-of-a-file-in-android
    private static void copy(File src, File dst) throws IOException
    {
        FileInputStream in = new FileInputStream(src);
        FileOutputStream out = new FileOutputStream(dst);

        // Transfer bytes from in to out
        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0)
        {
            out.write(buf, 0, len);
        }
        in.close();
        out.close();
    }

    private void showSnackbar(String str) // green text
    {
        baseLayout = findViewById(R.id.baseLayout);
        Snackbar sB = Snackbar.make(baseLayout, str, Snackbar.LENGTH_LONG);
        TextView tv = sB.getView().findViewById(R.id.snackbar_text);
        tv.setTextAlignment(TextView.TEXT_ALIGNMENT_CENTER);
        tv.setTextColor(Color.GREEN);
        sB.show();
    }

    private void showSnackbarRed(String str) // bold red text
    {
        baseLayout = findViewById(R.id.baseLayout);
        Snackbar sB = Snackbar.make(baseLayout, str, Snackbar.LENGTH_LONG);
        TextView tv = sB.getView().findViewById(R.id.snackbar_text);
        tv.setTextAlignment(TextView.TEXT_ALIGNMENT_CENTER);
        tv.setTypeface(tv.getTypeface(), Typeface.BOLD);
        tv.setTextColor(Color.RED);
        sB.show();
    }

}
