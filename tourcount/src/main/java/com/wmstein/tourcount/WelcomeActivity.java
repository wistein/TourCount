package com.wmstein.tourcount;

import static com.wmstein.tourcount.TourCountApplication.isFirstStart;
import static java.lang.Math.sqrt;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.text.HtmlCompat;
import androidx.core.view.MenuCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;

import com.wmstein.changelog.ChangeLog;
import com.wmstein.filechooser.AdvFileChooser;
import com.wmstein.tourcount.database.CountDataSource;
import com.wmstein.tourcount.database.DbHelper;
import com.wmstein.tourcount.database.Head;
import com.wmstein.tourcount.database.HeadDataSource;
import com.wmstein.tourcount.database.Section;
import com.wmstein.tourcount.database.SectionDataSource;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**********************************************************************
 * WelcomeActivity provides the starting page with menu and buttons for
 * import/export/help/info methods and lets you call 
 * EditMetaActivity, CountingActivity and ShowResultsActivity.
 * It uses further LocationService and PermissionDialogFragment.
 * <p>
 * Database handling is mainly done in WelcomeActivity as upgrade to current
 * DB version when importing an older DB file by importDBFile().
 * <p>
 * Based on BeeCount's WelcomeActivity.java by milo on 05/05/2014.
 * Changes and additions for TourCount by wmstein since 2016-04-18,
 * last edited on 2025-12-28
 */
public class WelcomeActivity
        extends AppCompatActivity
        implements SharedPreferences.OnSharedPreferenceChangeListener {
    private static final String TAG = "WelcomeAct";

    private TourCountApplication tourCount;

    LocationService locationService;
    private boolean locServiceOn = false;

    private ChangeLog cl;
    public boolean doubleBackToExitPressedTwice = false;

    // Import/export stuff
    private File inFile;
    private File outFile;
    private boolean mExternalStorageWriteable = false;
    private final String sState = Environment.getExternalStorageState();
    private final Handler mHandler = new Handler(Looper.getMainLooper());

    private AlertDialog alert;

    // Preferences
    private SharedPreferences prefs;
    private SharedPreferences.Editor editor;
    private String outPref;
    private boolean metaPref;        // option for OSM reverse geocoding
    private boolean storagePermGranted = false;  // Storage permission state
    private boolean fineLocationPermGranted = false; // Foreground location permission state
    private String emailString = ""; // mail address for OSM query

    // DB handling
    private SQLiteDatabase database;
    private DbHelper dbHelper;
    private SectionDataSource sectionDataSource;
    private Section section;
    private HeadDataSource headDataSource;
    private CountDataSource countDataSource;

    private String tourName = "";
    private View baseLayout;
    private String mesg;

    @SuppressLint({"SourceLockedOrientationActivity", "ApplySharedPref"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "140, onCreate");

        tourCount = (TourCountApplication) getApplication();

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        // Get preferences
        prefs = TourCountApplication.getPrefs();

        // Proximity sensor handling in preferences menu
        SensorManager mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        Sensor mProximity = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);

        // Grey out preferences menu item pref_prox when max. proximity sensitivity = null
        boolean prefProx = mProximity != null;

        // Set pref_prox enabler, used in SettingsFragment
        editor = prefs.edit();
        editor.putBoolean("enable_prox", prefProx);
        editor.apply();

        // Set DarkMode when system is in BrightMode
        int nightModeFlags = Configuration.UI_MODE_NIGHT_MASK;
        int confUi = getResources().getConfiguration().uiMode;
        if ((nightModeFlags & confUi) == Configuration.UI_MODE_NIGHT_NO) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        }

        // Use EdgeToEdge mode for Android 15+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) // SDK 35+
        {
            EdgeToEdge.enable(this);
        }

        setContentView(R.layout.activity_welcome);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.baseLayout),
                (v, windowInsets) -> {
                    Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
                    ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
                    mlp.topMargin = insets.top;
                    mlp.bottomMargin = insets.bottom;
                    mlp.leftMargin = insets.left;
                    mlp.rightMargin = insets.right;
                    v.setLayoutParams(mlp);
                    return WindowInsetsCompat.CONSUMED;
                });

        // Check initial storage permission state and provide dialog
        storagePermGranted = isStoragePermGranted();
        if (!storagePermGranted) // in self permission
        {
            PermissionsStorageDialogFragment.newInstance().show(getSupportFragmentManager(),
                    PermissionsStorageDialogFragment.class.getName());

            mesg = getString(R.string.storage_perm_denied);
            Toast.makeText(this,
                    HtmlCompat.fromHtml("<font color='red'><b>" + mesg + "</b></font>",
                            HtmlCompat.FROM_HTML_MODE_LEGACY), Toast.LENGTH_LONG).show();

            // Prepare to ask foreground location permission only once
            editor.putBoolean("has_asked_foreground", false);
            editor.commit();
        }
        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.d(TAG, "205, onCreate, storagePermGranted: " + storagePermGranted);

        // Check DB version and upgrade if necessary
        dbHelper = new DbHelper(this);
        database = dbHelper.getWritableDatabase();
        dbHelper.close();

        // setup the data sources
        headDataSource = new HeadDataSource(this);
        sectionDataSource = new SectionDataSource(this);
        countDataSource = new CountDataSource(this);

        // Get tour name and check for DB integrity
        try {
            sectionDataSource.open();
            section = sectionDataSource.getSection();
            tourName = section.name;
            sectionDataSource.close();
        } catch (SQLiteException e) {
            sectionDataSource.close();
            mesg = getString(R.string.corruptDb);
            Toast.makeText(this,
                    HtmlCompat.fromHtml("<font color='red'><b>" + mesg + "</b></font>",
                            HtmlCompat.FROM_HTML_MODE_LEGACY), Toast.LENGTH_LONG).show();

            mHandler.postDelayed(this::finishAndRemoveTask, 2000);
        }

        cl = new ChangeLog(this, prefs);

        // Show changelog for new version
        if (cl.firstRun())
            cl.getLogDialog().show();

        // Test for existence of file /storage/emulated/0/Documents/TourCount/tourcount0.db
        File path;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) // Android 10+
        {
            path = Environment.getExternalStorageDirectory();
            path = new File(path + "/Documents/TourCount");
        } else {
            path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
            path = new File(path + "/TourCount");
        }

        // Create preliminary tourcount0.db if it does not exist
        inFile = new File(path, "/tourcount0.db"); // Initial basic DB
        File inFile1 = new File(path, "/tourcount0_" + tourName + ".db");

        if (!inFile.exists() && !inFile1.exists())
            exportBasisDb(); // create directory and copy internal DB-data to initial Basis DB-file

        // New onBackPressed logic
        // Different Navigation Bar modes and layouts:
        // - Classic three-button navigation: NavBarMode = 0
        // - Two-button navigation (Android P): NavBarMode = 1
        // - Full screen gesture mode (Android Q): NavBarMode = 2
        // Use onBackPressed logic only if NavBarMode = 0 or 1.
        if (getNavBarMode() == 0 || getNavBarMode() == 1) {
            OnBackPressedCallback callback = getOnBackPressedCallback();
            getOnBackPressedDispatcher().addCallback(this, callback);
        }

        locationService = new LocationService(getApplicationContext());
    }
    // End of onCreate()

    // Check for Navigation bar (1-, 2- or 3-button mode)
    public int getNavBarMode() {
        Resources resources = this.getResources();

        @SuppressLint("DiscouragedApi")
        int resourceId = resources.getIdentifier("config_navBarInteractionMode",
                "integer", "android");

        // navBarMode = 0: 3-button, = 1: 2-button, = 2: gesture
        return resourceId > 0 ? resources.getInteger(resourceId) : 0;
    }

    // Use onBackPressed logic for button navigation
    @NonNull
    private OnBackPressedCallback getOnBackPressedCallback() {
        final Handler m1Handler = new Handler(Looper.getMainLooper());
        final Runnable r1 = () -> doubleBackToExitPressedTwice = false;

        return new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (doubleBackToExitPressedTwice) {
                    m1Handler.removeCallbacks(r1);
                    // Clear last locality in temp_loc of table TEMP, otherwise the old
                    //   locality is shown in the 1. count of a new started tour
                    clear_loc();
                    finish();
                    remove();
                } else {
                    doubleBackToExitPressedTwice = true;
                    mesg = getString(R.string.back_twice);
                    Toast.makeText(getApplicationContext(),
                            HtmlCompat.fromHtml("<font color='blue'>" + mesg + "</font>",
                                    HtmlCompat.FROM_HTML_MODE_LEGACY), Toast.LENGTH_SHORT).show();
                    m1Handler.postDelayed(r1, 1500);
                }
            }
        };
    }

    @SuppressLint({"SourceLockedOrientationActivity", "ApplySharedPref"})
    @Override
    protected void onResume() {
        super.onResume();

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "318, onResume");

        prefs = TourCountApplication.getPrefs();
        prefs.registerOnSharedPreferenceChangeListener(this);
        outPref = prefs.getString("pref_sort_output", "names"); // sort mode csv-export
        locServiceOn = prefs.getBoolean("loc_srv_on", false);
        emailString = prefs.getString("email_String", ""); // for reliable query of Nominatim service
        metaPref = prefs.getBoolean("pref_metadata", false); // use Reverse Geocoding

        storagePermGranted = isStoragePermGranted(); // set storagePermGranted from self permission

        if (isFirstStart) {
            // This is to remind a missing email address for Nominatim Reverse Geocoder.
            //   Info about the first GPS lock is handled in LocationService onLocationChanged().
            if (metaPref && Objects.equals(emailString, "")) {
                mesg = getString(R.string.missingEmail);
                Toast.makeText(this,
                        HtmlCompat.fromHtml("<font color='blue'>" + mesg + "</font>",
                                HtmlCompat.FROM_HTML_MODE_LEGACY), Toast.LENGTH_SHORT).show();
            }
            isFirstStart = false;
        }

        headDataSource.open();
        sectionDataSource.open();
        countDataSource.open();

        baseLayout = findViewById(R.id.baseLayout);
        baseLayout.setBackground(tourCount.setBackgr());

        // Set tour name as title
        section = sectionDataSource.getSection();
        tourName = section.name;
        try {
            Objects.requireNonNull(getSupportActionBar()).setTitle(tourName);
        } catch (NullPointerException e) {
            // nothing
        }

        // Location permissions handling:
        //   Get flag fineLocationPermGranted from self permissions
        isFineLocationPermGranted();

        // If location permission is not yet granted prepare and query for it
        if (storagePermGranted && !fineLocationPermGranted) {
            // Get flag 'has_asked_foreground'
            boolean hasAskedForegroundLocation = prefs.getBoolean("has_asked_foreground", false);

            if (!hasAskedForegroundLocation) {
                // Query foreground location permission first
                // Ask necessary fine location permission with info in AlertDialog
                PermissionsForegroundDialogFragment.newInstance().show(getSupportFragmentManager(),
                        PermissionsForegroundDialogFragment.class.getName());

                editor.putBoolean("has_asked_foreground", true);
                editor.commit();
            }
        }

        // Get new location self permission state
        isFineLocationPermGranted(); // set fineLocationPermGranted from self permission
        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "380, onResume, fineLocationPermGranted: " + fineLocationPermGranted);

        locationDispatcher(1);
    }
    // End of onResume()

    // Check initial external storage permission and set 'storagePermGranted'
    private Boolean isStoragePermGranted() {
        boolean storageGranted;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // check permission MANAGE_EXTERNAL_STORAGE for Android >= 11
            storageGranted = Environment.isExternalStorageManager();
            if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                Log.i(TAG, "393, ManageStoragePermission: " + storagePermGranted);
        } else {
            storageGranted = ContextCompat.checkSelfPermission(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
            if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                Log.i(TAG, "398, ExtStoragePermission: " + storagePermGranted);
        }
        return storageGranted;
    }

    // Check initial fine location permission
    private void isFineLocationPermGranted() {
        fineLocationPermGranted = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    // Control location service
    // locationDispatcherMode:
    //  1 = start and use location service
    //  2 = end location service
    public void locationDispatcher(int locationDispatcherMode) {
        if (fineLocationPermGranted) // current location permission state granted
        {
            editor = prefs.edit();
            // Handle action here
            switch (locationDispatcherMode) {
                case 1 -> {
                    // Get location data
                    if (!locServiceOn) {
                        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                            Log.i(TAG, "423, locationDispatcher 1");
                        Intent sIntent = new Intent(getApplicationContext(), LocationService.class);
                        startService(sIntent);
                        locServiceOn = true;

                        editor.putBoolean("loc_srv_on", true);
                        editor.commit();
                    }
                    getLoc();
                }
                case 2 -> {
                    // Stop location service
                    if (locServiceOn) {
                        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                            Log.i(TAG, "437, locationDispatcher 2");
                        locationService.stopListener();
                        Intent sIntent = new Intent(getApplicationContext(), LocationService.class);
                        stopService(sIntent);
                        locServiceOn = false;

                        editor.putBoolean("loc_srv_on", false);
                        editor.commit();
                    }
                }
            }
        }
    }

    // Get the location data and use RetrieveAddrWorker to store location details
    //   to Section and Temp tables
    public void getLoc() {
        if (locationService.canGetLocation()) {
            double longitude = locationService.getLongitude();
            double latitude = locationService.getLatitude();

            // Get reverse geocoding and store data to Section and Temp table
            if (metaPref && latitude != 0.0) {
                String urlString;
                if (Objects.equals(emailString, "")) {
                    urlString = "https://nominatim.openstreetmap.org/reverse?" +
                            "email=test@temp.test" + "format=xml&lat="
                            + latitude + "&lon=" + longitude + "&zoom=18&addressdetails=1";
                }
                else {
                    urlString = "https://nominatim.openstreetmap.org/reverse?" +
                            "email=" + emailString + "&format=xml&lat="
                            + latitude + "&lon=" + longitude + "&zoom=18&addressdetails=1";
                }
                WorkRequest retrieveAddrWorkRequest =
                        new OneTimeWorkRequest.Builder(RetrieveAddrWorker.class)
                                .setInputData(new Data.Builder()
                                        .putString("URL_STRING", urlString)
                                        .build()
                                )
                                .build();
                WorkManager.getInstance(this).enqueue(retrieveAddrWorkRequest);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.welcome, menu);
        MenuCompat.setGroupDividerEnabled(menu, true); // Show dividers in menu
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            // Call SettingsActivity
            startActivity(new Intent(this, SettingsActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
            return true;
        } else if (id == R.id.exportMenu) {
            // Call exportDb()
            if (storagePermGranted) {
                exportDb();
            } else {
                PermissionsStorageDialogFragment.newInstance().show(getSupportFragmentManager(),
                        PermissionsStorageDialogFragment.class.getName());
                if (storagePermGranted) {
                    exportDb();
                } else {
                    mesg = getString(R.string.storage_perm_denied);
                    Toast.makeText(this,
                            HtmlCompat.fromHtml("<font color='red'><b>" + mesg + "</b></font>",
                                    HtmlCompat.FROM_HTML_MODE_LEGACY), Toast.LENGTH_LONG).show();
                }
            }
            return true;
        } else if (id == R.id.exportCSVMenu) {
            // Call exportDb2CSV()
            if (storagePermGranted) {
                exportDb2CSV();
            } else {
                PermissionsStorageDialogFragment.newInstance().show(getSupportFragmentManager(),
                        PermissionsStorageDialogFragment.class.getName());
                if (storagePermGranted) {
                    exportDb2CSV();
                } else {
                    mesg = getString(R.string.storage_perm_denied);
                    Toast.makeText(this,
                            HtmlCompat.fromHtml("<font color='red'><b>" + mesg + "</b></font>",
                                    HtmlCompat.FROM_HTML_MODE_LEGACY), Toast.LENGTH_LONG).show();
                }
            }
            return true;
        } else if (id == R.id.exportBasisMenu) {
            // Call exportBasisDb()
            if (storagePermGranted) {
                exportBasisDb();
            } else {
                PermissionsStorageDialogFragment.newInstance().show(getSupportFragmentManager(),
                        PermissionsStorageDialogFragment.class.getName());
                if (storagePermGranted) {
                    exportBasisDb();
                } else {
                    mesg = getString(R.string.storage_perm_denied);
                    Toast.makeText(this,
                            HtmlCompat.fromHtml("<font color='red'><b>" + mesg + "</b></font>",
                                    HtmlCompat.FROM_HTML_MODE_LEGACY), Toast.LENGTH_LONG).show();
                }
            }
            return true;
        } else if (id == R.id.exportSpeciesListMenu) {
            // Call exportSpeciesList()
            if (storagePermGranted) {
                exportSpeciesList();
            } else {
                PermissionsStorageDialogFragment.newInstance().show(getSupportFragmentManager(),
                        PermissionsStorageDialogFragment.class.getName());
                if (storagePermGranted) {
                    exportSpeciesList();
                } else {
                    mesg = getString(R.string.storage_perm_denied);
                    Toast.makeText(this,
                            HtmlCompat.fromHtml("<font color='red'><b>" + mesg + "</b></font>",
                                    HtmlCompat.FROM_HTML_MODE_LEGACY), Toast.LENGTH_LONG).show();
                }
            }
            return true;
        } else if (id == R.id.importBasisMenu) {
            // Call importBasisDb()
            importBasisDb();
            return true;
        } else if (id == R.id.importFileMenu) {
            // Call  importDBFile()
            importDBFile();
            return true;
        } else if (id == R.id.resetDBMenu) {
            // Call resetToBasisDb()
            resetToBasisDb();
            return true;
        } else if (id == R.id.importSpeciesListMenu) {
            // Call importSpeciesList()
            importSpeciesList();
            return true;
        } else if (id == R.id.viewHelp) {
            // Call ShowTextDialog with help text
            intent = new Intent(WelcomeActivity.this, ShowTextDialog.class);
            intent.putExtra("dialog", "help");
            startActivity(intent);
            return true;
        } else if (id == R.id.changeLog) {
            // Call ChangeLog
            cl.getFullLogDialog().show();
            return true;
        } else if (id == R.id.viewLicense) {
            // Call ShowTextDialog with license text
            intent = new Intent(WelcomeActivity.this, ShowTextDialog.class);
            intent.putExtra("dialog", "license");
            startActivity(intent);
            return true;
        } else if (id == R.id.editMeta) {
            // Call EditMetaActivity
            intent = new Intent(WelcomeActivity.this, EditMetaActivity.class);
            // Trick: Pause for 500 msec to store results from RetrieveAddrWorker into DB
            mHandler.postDelayed(() ->
                    startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)), 500);
            return true;
        } else if (id == R.id.startCounting) {
            // Call CountingActivity
            locationDispatcher(2); // Stop location service
            intent = new Intent(WelcomeActivity.this, CountingActivity.class);
            startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
            return true;
        } else if (id == R.id.showResults) {
            // Call ShowResultsActivity
            mesg = getString(R.string.wait);
            Toast.makeText(this,
                    HtmlCompat.fromHtml("<font color='#008000'>" + mesg + "</font>",
                            HtmlCompat.FROM_HTML_MODE_LEGACY), Toast.LENGTH_SHORT).show();

            // Trick: Pause for 100 msec to show toast
            mHandler.postDelayed(() ->
                    startActivity(new Intent(getApplicationContext(), ShowResultsActivity
                            .class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)), 100);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    // End of onOptionsItemSelected

    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        baseLayout = findViewById(R.id.baseLayout);
        baseLayout.setBackground(tourCount.setBackgr());
        outPref = prefs.getString("pref_sort_output", "names");
    }

    @Override
    public void onPause() {
        super.onPause();

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "642, onPause");

        headDataSource.close();
        countDataSource.close();
        sectionDataSource.close();

        locationDispatcher(2); // Stop location service

        prefs.unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onStop() {
        super.onStop();

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "658 onStop");

        baseLayout.invalidate();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "668, onDestroy");
    }

    // Handle button click "Counting" here
    public void startCounting(View view) {
        locationDispatcher(2); // Stop location service
        Intent intent;
        intent = new Intent(WelcomeActivity.this, CountingActivity.class);
        startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
    }

    // Handle button click "Prepare Inspection" here
    public void editMeta(View view) {
        Intent intent = new Intent(WelcomeActivity.this, EditMetaActivity.class);
        // Trick: Pause for 500 msec to store results from RetrieveAddrWorker into DB
        mHandler.postDelayed(() ->
                startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)), 500);
    }

    // Start ShowResultsActivity (by button)
    public void showResults(View view) {
        // a Snackbar here comes incomplete
        mesg = getString(R.string.wait);
        Toast.makeText(this,
                HtmlCompat.fromHtml("<font color='#008000'>" + mesg + "</font>",
                        HtmlCompat.FROM_HTML_MODE_LEGACY), Toast.LENGTH_SHORT).show();

        // Trick: Pause for 100 msec to show toast
        mHandler.postDelayed(() ->
                startActivity(new Intent(getApplicationContext(), ShowResultsActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)), 100);
    }

    // Date for filename of exported data
    private static String getcurDate() {
        Date date = new Date();
        @SuppressLint("SimpleDateFormat")
        DateFormat dform = new SimpleDateFormat("yyyyMMdd_HHmmss");
        return dform.format(date);
    }

    /***********************************************************************************************
     * The next three functions below are for importing data files.
     * They've been put here because no database should be open at this point.
     **********************************************************************************************/
    // Import the basic DB
    private void importBasisDb() {
        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.d(TAG, "716, importBasicDBFile");

        String fileExtension = ".db";
        String fileNameStart = "tourcount0";
        String fileHd = getString(R.string.fileHeadlineBasicDB);

        Intent intent;
        intent = new Intent(this, AdvFileChooser.class);
        intent.putExtra("filterFileExtension", fileExtension);
        intent.putExtra("filterFileNameStart", fileNameStart);
        intent.putExtra("fileHd", fileHd);
        myActivityResultLauncher.launch(intent);
    }
    // End of importBasisDb()

    /**********************************************************************************************/
    // Choose a tourcount db-file to load and set it to tourcount.db
    private void importDBFile() {
        String fileExtension = ".db";
        String fileNameStart = "tourcount_";
        String fileHd = getString(R.string.fileHeadlineDB);

        Intent intent;
        intent = new Intent(this, AdvFileChooser.class);
        intent.putExtra("filterFileExtension", fileExtension);
        intent.putExtra("filterFileNameStart", fileNameStart);
        intent.putExtra("fileHd", fileHd);
        myActivityResultLauncher.launch(intent);
    }

    // ActivityResultLauncher is part of importDBFile()
    // and processes the result of AdvFileChooser
    final ActivityResultLauncher<Intent> myActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    String selectedFile;
                    inFile = null;
                    if (result.getResultCode() == Activity.RESULT_OK) // has a file
                    {
                        Intent data = result.getData();
                        if (data != null) {
                            selectedFile = data.getStringExtra("fileSelected");
                            if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                                Log.i(TAG, "761, Selected file: " + selectedFile);

                            if (selectedFile != null)
                                inFile = new File(selectedFile);
                            else
                                inFile = null;
                        }
                    } else if ((result.getResultCode() == Activity.RESULT_FIRST_USER)) {
                        mesg = getString(R.string.noFile);
                        Toast.makeText(getApplicationContext(),
                                HtmlCompat.fromHtml("<font color='red'><b>" + mesg + "</b></font>",
                                        HtmlCompat.FROM_HTML_MODE_LEGACY), Toast.LENGTH_LONG).show();
                    }
                    if (inFile != null) {
                        // outFile -> /data/data/com.wmstein.tourcount/databases/tourcount.db
                        String destPath = getApplicationContext().getFilesDir().getPath();
                        destPath = destPath.substring(0, destPath.lastIndexOf("/")) + "/databases/tourcount.db";
                        outFile = new File(destPath);

                        AlertDialog.Builder builder = new AlertDialog.Builder(WelcomeActivity.this);
                        builder.setIcon(android.R.drawable.ic_dialog_alert);
                        builder.setMessage(R.string.confirmDBImport);
                        builder.setCancelable(false);
                        builder.setPositiveButton(R.string.importButton, (dialog, id) ->
                        {
                            try {
                                copy(inFile, outFile);

                                // Save values for initial count-id and itemposition
                                editor = prefs.edit();
                                editor.putInt("count_id", 1);
                                editor.putInt("item_Position", 0);
                                editor.apply();

                                // List tour name as title
                                section = sectionDataSource.getSection();
                                tourName = section.name;
                                Objects.requireNonNull(getSupportActionBar()).setTitle(tourName);

                                mesg = getString(R.string.importWin);
                                Toast.makeText(getApplicationContext(),
                                        HtmlCompat.fromHtml("<font color='#008000'>" + mesg + "</font>",
                                                HtmlCompat.FROM_HTML_MODE_LEGACY), Toast.LENGTH_SHORT).show();
                            } catch (IOException e) {
                                mesg = getString(R.string.importFail);
                                Toast.makeText(getApplicationContext(),
                                        HtmlCompat.fromHtml("<font color='red'><b>" + mesg + "</b></font>",
                                                HtmlCompat.FROM_HTML_MODE_LEGACY), Toast.LENGTH_LONG).show();
                            }
                        });
                        builder.setNegativeButton(R.string.cancelButton, (dialog, id) -> dialog.cancel());
                        alert = builder.create();
                        alert.show();
                    }
                }
            });
    // End of importDBFile()

    /**********************************************************************************************/
    // Copy file block-wise
    private static void copy(File src, File dst) throws IOException {
        FileInputStream in = new FileInputStream(src);
        FileOutputStream out = new FileOutputStream(dst);

        // Transfer bytes from in to out
        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
        in.close();
        out.close();
    }

    /**********************************************************************************************/
    // Import species list from TransektCount file species_YYYY-MM-DD_hhmmss.csv
    private void importSpeciesList() {
        // Select exported TransektCount species list file
        String fileExtension = ".csv";
        String fileNameStart = "species_";
        String fileHd = getString(R.string.fileHeadlineCSV);

        Intent intent;
        intent = new Intent(this, AdvFileChooser.class);
        intent.putExtra("filterFileExtension", fileExtension);
        intent.putExtra("filterFileNameStart", fileNameStart);
        intent.putExtra("fileHd", fileHd);
        listActivityResultLauncher.launch(intent);
    }

    // ActivityResultLauncher processes the result of AdvFileChooser
    final ActivityResultLauncher<Intent> listActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<>() {
                @SuppressLint("ApplySharedPref")
                @Override
                public void onActivityResult(ActivityResult result) {
                    String selectedFile;
                    inFile = null;
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent data = result.getData();
                        if (data != null) {
                            selectedFile = data.getStringExtra("fileSelected");
                            if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                                Log.d(TAG, "865, File selected: " + selectedFile);

                            if (selectedFile != null)
                                inFile = new File(selectedFile);
                            else
                                inFile = null;
                        }
                    }
                    // RESULT_FIRST_USER is set in AdvFileChooser for no file
                    else if ((result.getResultCode() == Activity.RESULT_FIRST_USER)) {
                        mesg = getString(R.string.noFile);
                        Toast.makeText(getApplicationContext(),
                                HtmlCompat.fromHtml("<font color='red'><b>" + mesg + "</b></font>",
                                        HtmlCompat.FROM_HTML_MODE_LEGACY), Toast.LENGTH_LONG).show();
                    }
                    if (inFile != null) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(WelcomeActivity.this);
                        builder.setIcon(android.R.drawable.ic_dialog_alert);
                        builder.setMessage(R.string.confirmListImport);
                        builder.setCancelable(false);
                        builder.setPositiveButton(R.string.importButton, (dialog, id) ->
                        {
                            clearDBforImport();
                            readCSV(inFile);
                        });
                        builder.setNegativeButton(R.string.cancelButton, (dialog, id) -> dialog.cancel());
                        alert = builder.create();
                        alert.show();
                    }
                }
            });

    // Clear DB for import of external species list
    private void clearDBforImport() {
        dbHelper = new DbHelper(this);
        database = dbHelper.getWritableDatabase();

        String sql = "DELETE FROM " + DbHelper.COUNT_TABLE;
        database.execSQL(sql);

        sql = "DELETE FROM " + DbHelper.INDIVIDUALS_TABLE;
        database.execSQL(sql);

        sql = "UPDATE " + DbHelper.TEMP_TABLE + " SET "
                + DbHelper.T_TEMP_LOC + " = '', "
                + DbHelper.T_TEMP_CNT + " = 0;";
        database.execSQL(sql);

        dbHelper.close();

        editor = prefs.edit();
        editor.putInt("item_Position", 0);
        editor.putInt("count_id", 1);
        editor.apply();
    }

    private void readCSV(File inFile) {
        try {
            // Read exported species list and write items to table counts
            mesg = getString(R.string.waitImport);
            Toast.makeText(this,
                    HtmlCompat.fromHtml("<font color='#008000'>" + mesg + "</font>",
                            HtmlCompat.FROM_HTML_MODE_LEGACY), Toast.LENGTH_SHORT).show();
            BufferedReader br = new BufferedReader(new FileReader(inFile));
            String csvLine;
            List<String> codeArray = new ArrayList<>();
            List<String> nameArray = new ArrayList<>();
            List<String> nameGArray = new ArrayList<>();
            int i = 0;
            while ((csvLine = br.readLine()) != null) // for each csvLine
            {
                // comma-separated 0:code, 1:name, 2:nameL
                String[] specLine = csvLine.split(",");
                codeArray.add(i, specLine[0]);
                nameArray.add(i, specLine[1]);
                nameGArray.add(i, specLine[2]);
                countDataSource.writeCountItem(String.valueOf(i + 1), codeArray.get(i),
                        nameArray.get(i), nameGArray.get(i));
                i++;
            }
            br.close();
            mesg = getString(R.string.importList);
            Toast.makeText(this,
                    HtmlCompat.fromHtml("<font color='#008000'>" + mesg + "</font>",
                            HtmlCompat.FROM_HTML_MODE_LEGACY), Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            mesg = getString(R.string.importListFail);
            Toast.makeText(this,
                    HtmlCompat.fromHtml("<font color='red'><b>" + mesg + "</b></font>",
                            HtmlCompat.FROM_HTML_MODE_LEGACY), Toast.LENGTH_LONG).show();
        }
    }
    // End of importSpeciesList()

    /***********************************************************************************************
     * The next four functions below are for exporting data files.
     * They've been put here because no database should be open at this point.
     **********************************************************************************************/
    // Exports Basis DB to Documents/TourCount/tourcount0_name.db
    // hasNoName indicated initial creation of tourcount0.db if it does not exist
    private void exportBasisDb() {
        // inFile <- /data/data/com.wmstein.tourcount/databases/tourcount.db
        String inPath = getApplicationContext().getFilesDir().getPath();
        inPath = inPath.substring(0, inPath.lastIndexOf("/")) + "/databases/tourcount.db";
        inFile = new File(inPath);

        // tmpFile -> /data/data/com.wmstein.tourcount/files/tourcount_tmp.db
        String tmpPath = getApplicationContext().getFilesDir().getPath();
        tmpPath = tmpPath.substring(0, tmpPath.lastIndexOf("/")) + "/files/tourcount_tmp.db";
        File tmpFile = new File(tmpPath);

        // outFile in Public Directory Documents/TourCount/
        // distinguish versions (as getExternalStoragePublicDirectory is deprecated in Q, Android 10)
        File path;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) // Android 10+
        {
            path = Environment.getExternalStorageDirectory();
            path = new File(path + "/Documents/TourCount");
        } else {
            path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
            path = new File(path + "/TourCount");
        }

        //noinspection ResultOfMethodCallIgnored
        path.mkdirs(); // just verify path, result ignored
        if (Objects.equals(tourName, ""))
            outFile = new File(path, "/tourcount0.db");
        else
            outFile = new File(path, "/tourcount0_" + tourName + ".db");

        // Check if we can write the media
        mExternalStorageWriteable = Environment.MEDIA_MOUNTED.equals(sState);

        if (!mExternalStorageWriteable) {
            mesg = getString(R.string.noCard);
            Toast.makeText(this,
                    HtmlCompat.fromHtml("<font color='red'><b>" + mesg + "</b></font>",
                            HtmlCompat.FROM_HTML_MODE_LEGACY), Toast.LENGTH_LONG).show();
        } else {
            // Export the basic db
            try {
                // Save current db as backup db tmpFile
                copy(inFile, tmpFile);

                // Clear DB values for basic DB
                clearDBValues();

                // Write Basis DB
                copy(inFile, outFile);

                // Restore actual db from tmpFile
                copy(tmpFile, inFile);

                // Delete backup db
                boolean d0 = tmpFile.delete();
                if (d0) {
                    mesg = getString(R.string.saveBasisDB);
                    Toast.makeText(this,
                            HtmlCompat.fromHtml("<font color='#008000'>" + mesg + "</font>",
                                    HtmlCompat.FROM_HTML_MODE_LEGACY), Toast.LENGTH_SHORT).show();
                }
            } catch (IOException e) {
                if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                    Log.e(TAG, "1028, Failed to export Basic DB");
                mesg = getString(R.string.saveFail);
                Toast.makeText(this,
                        HtmlCompat.fromHtml("<font color='red'><b>" + mesg + "</b></font>",
                                HtmlCompat.FROM_HTML_MODE_LEGACY), Toast.LENGTH_LONG).show();
            }
        }
    }
    // End of exportBasisDb()

    @SuppressLint({"SdCardPath", "LongLogTag"})
    private void exportDb() {
        // New data directory:
        //   outFile -> Public Directory Documents/TourCount/
        File path;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) // Android 10+
        {
            path = Environment.getExternalStorageDirectory();
            path = new File(path + "/Documents/TourCount");
        } else {
            path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
            path = new File(path + "/TourCount");
        }

        //noinspection ResultOfMethodCallIgnored
        path.mkdirs(); // Just verify path, result ignored

        // outFile -> /storage/emulated/0/Documents/TourCount/tourcount_yyyyMMdd_HHmmss.db
        if (Objects.equals(tourName, ""))
            outFile = new File(path, "/tourcount_" + getcurDate() + ".db");
        else
            outFile = new File(path, "/tourcount_" + getcurDate() + "_" + tourName + ".db");

        // inFile <- /data/data/com.wmstein.tourcount/databases/tourcount.db
        String inPath = getApplicationContext().getFilesDir().getPath();
        inPath = inPath.substring(0, inPath.lastIndexOf("/"))
                + "/databases/tourcount.db";
        inFile = new File(inPath);

        // Check if we can write the media
        mExternalStorageWriteable = Environment.MEDIA_MOUNTED.equals(sState);

        if (!mExternalStorageWriteable) {
            mesg = getString(R.string.noCard);
            Toast.makeText(this,
                    HtmlCompat.fromHtml("<font color='red'><b>" + mesg + "</b></font>",
                            HtmlCompat.FROM_HTML_MODE_LEGACY), Toast.LENGTH_LONG).show();
        } else {
            // Export the db
            try {
                copy(inFile, outFile);
                mesg = getString(R.string.saveDB);
                Toast.makeText(this,
                        HtmlCompat.fromHtml("<font color='#008000'>" + mesg + "</font>",
                                HtmlCompat.FROM_HTML_MODE_LEGACY), Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                mesg = getString(R.string.saveFail);
                Toast.makeText(this,
                        HtmlCompat.fromHtml("<font color='red'><b>" + mesg + "</b></font>",
                                HtmlCompat.FROM_HTML_MODE_LEGACY), Toast.LENGTH_LONG).show();
            }
        }
    }
    // End of exportDb()

    /***********************************************************************************************
     // Exports DB contents as tourcount_yyyy-MM-dd_HHmmss.csv to
     // Documents/TourCount/ with purged data set.
     // Spreadsheet programs can import this csv file with
     //   - Unicode UTF-8 filter,
     //   - comma delimiter and
     //   - "" for text recognition.
     */
    private void exportDb2CSV() {
        // outFile -> /storage/emulated/0/Documents/TourCount/Tour_yyyyMMdd_HHmmss_tourname.csv
        //   and distinguish versions (as getExternalStoragePublicDirectory is deprecated in Q, Android 10)
        File path;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) // Android 10+
        {
            path = Environment.getExternalStorageDirectory();
            path = new File(path + "/Documents/TourCount");
        } else {
            path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
            path = new File(path + "/TourCount");
        }

        //noinspection ResultOfMethodCallIgnored
        path.mkdirs(); // Just verify path, result ignored

        if (Objects.equals(tourName, ""))
            outFile = new File(path, "/Tour_" + getcurDate() + ".csv");
        else
            outFile = new File(path, "/Tour_" + getcurDate() + "_" + tourName + ".csv");

        String sectName;
        String sectNotes;

        Head head;
        String country, b_state, inspecName;
        int temps, winds, clouds, tempe, winde, cloude;
        String plz, city, place, locality;
        String date, start_tm, end_tm;
        int spstate;
        String spstate0;
        double longi, lati, heigh, uncer;
        int frst, sum = 0;
        int summf = 0, summ = 0, sumf = 0, sump = 0, suml = 0, sume = 0;
        String sumMF = "", sumM = "", sumF = "", sumP = "", sumL = "", sumE = "";
        double lo, la, loMin = 0, loMax = 0, laMin = 0, laMax = 0, uc, uncer1 = 0;

        // Check if we can write the media
        mExternalStorageWriteable = Environment.MEDIA_MOUNTED.equals(sState);

        if (!mExternalStorageWriteable) {
            mesg = getString(R.string.noCard);
            Toast.makeText(this,
                    HtmlCompat.fromHtml("<font color='red'><b>" + mesg + "</b></font>",
                            HtmlCompat.FROM_HTML_MODE_LEGACY), Toast.LENGTH_LONG).show();
        } else {
            // Get sorting mode of species list
            String sortMode;
            if (outPref.equals("names")) {
                sortMode = getString(R.string.sort_names);
            } else {
                sortMode = getString(R.string.sort_codes);
            }

            // Export the purged count table to csv
            try {
                // Export purged db as csv
                CSVWriter csvWrite = new CSVWriter(new FileWriter(outFile));

                // Consult Section an Head tables for head and meta info
                section = sectionDataSource.getSection();

                sectName = section.name;
                sectNotes = section.notes;
                country = section.country;
                b_state = section.b_state;
                plz = section.plz;
                city = section.city;
                place = section.place;
                locality = section.st_locality;

                head = headDataSource.getHead();

                inspecName = head.observer;

                String[] arrHead =
                        {
                                getString(R.string.zList) + ":", // Count List:
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
                                getString(R.string.bstate),
                                getString(R.string.plz),
                                getString(R.string.city),
                                getString(R.string.place),
                                getString(R.string.slocality),
                                getString(R.string.zlNotes)
                        };
                csvWrite.writeNext(arrLocHead);

                // Set location dataline
                String[] arrLocation =
                        {
                                country,
                                b_state,
                                plz,
                                city,
                                place,
                                locality,
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
                dbHelper = new DbHelper(this);
                database = dbHelper.getWritableDatabase();

                Cursor curCSVCnt; // Cursor for Counts table

                // Sort mode species list
                if (outPref.equals("names")) {
                    curCSVCnt = database.rawQuery("select * from " + DbHelper.COUNT_TABLE
                            + " WHERE " + " ("
                            + DbHelper.C_COUNT_F1I + " > 0 or " + DbHelper.C_COUNT_F2I + " > 0 or "
                            + DbHelper.C_COUNT_F3I + " > 0 or " + DbHelper.C_COUNT_PI + " > 0 or "
                            + DbHelper.C_COUNT_LI + " > 0 or " + DbHelper.C_COUNT_EI + " > 0)"
                            + " order by " + DbHelper.C_NAME, null, null);
                } else {
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
                while (curCSVCnt.moveToNext()) {
                    spname = curCSVCnt.getString(7); // species name from count table
                    spcode = curCSVCnt.getString(8); // species code from count table
                    slct = "SELECT * FROM " + DbHelper.INDIVIDUALS_TABLE + " WHERE "
                            + DbHelper.I_NAME + " = ? AND "
                            + DbHelper.I_SEX + " = ? AND "
                            + DbHelper.I_STADIUM + " = ?";

                    // Select male
                    curCSVInd = database.rawQuery(slct, new String[]{spname, male, stadium1});
                    while (curCSVInd.moveToNext()) {
                        cnts = curCSVInd.getInt(14); // individuals icount
                        cntsm = cntsm + cnts;
                    }
                    curCSVInd.close();

                    // Select female
                    curCSVInd = database.rawQuery(slct, new String[]{spname, fmale, stadium1});
                    while (curCSVInd.moveToNext()) {
                        cnts = curCSVInd.getInt(14); // individuals icount
                        cntsf = cntsf + cnts;
                    }
                    curCSVInd.close();

                    String slct1 = "SELECT * FROM " + DbHelper.INDIVIDUALS_TABLE
                            + " WHERE " + DbHelper.I_NAME + " = ? AND " + DbHelper.I_STADIUM + " = ?";

                    // Select pupa
                    curCSVInd = database.rawQuery(slct1, new String[]{spname, stadium2});
                    while (curCSVInd.moveToNext()) {
                        cnts = curCSVInd.getInt(14); // individuals icount
                        cntsp = cntsp + cnts;
                    }
                    curCSVInd.close();

                    // Select caterpillar
                    curCSVInd = database.rawQuery(slct1, new String[]{spname, stadium3}); // select caterpillar
                    while (curCSVInd.moveToNext()) {
                        cnts = curCSVInd.getInt(14); // individuals icount
                        cntsl = cntsl + cnts;
                    }
                    curCSVInd.close();

                    // Select egg
                    curCSVInd = database.rawQuery(slct1, new String[]{spname, stadium4}); // select egg
                    while (curCSVInd.moveToNext()) {
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
                                getString(R.string.status123) + ":",
                                getString(R.string.bema)
                        };
                csvWrite.writeNext(arrIndHead);

                // Build the sorted individuals array
                curCSVInd = database.rawQuery("select * from " + DbHelper.INDIVIDUALS_TABLE
                                + " order by " + DbHelper.I_DATE_STAMP + ", " + DbHelper.I_TIME_STAMP,
                        null, null);

                String lngi, latit;
                frst = 0;
                while (curCSVInd.moveToNext()) {
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

                    try {
                        lngi = String.valueOf(longi).substring(0, 8); // longitude
                    } catch (StringIndexOutOfBoundsException e) {
                        lngi = String.valueOf(longi);
                    }

                    try {
                        latit = String.valueOf(lati).substring(0, 8); // latitude
                    } catch (StringIndexOutOfBoundsException e) {
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
                                    spstate0,                   // status
                                    curCSVInd.getString(13)  // indiv. notes
                            };
                    csvWrite.writeNext(arrIndividual);

                    if (longi != 0) // Has coordinates
                    {
                        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                            Log.d(TAG, "1590 longi " + longi);
                        if (frst == 0) {
                            loMin = longi;
                            loMax = longi;
                            laMin = lati;
                            laMax = lati;
                            uncer1 = uncer;
                            frst = 1; // Just 1 with coordinates
                        } else {
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

                try {
                    lngi = String.valueOf(lo).substring(0, 8); //longitude
                } catch (StringIndexOutOfBoundsException e) {
                    lngi = String.valueOf(lo);
                }

                try {
                    latit = String.valueOf(la).substring(0, 8); // latitude
                } catch (StringIndexOutOfBoundsException e) {
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
                dbHelper.close();

                mesg = getString(R.string.saveCSV);
                Toast.makeText(this,
                        HtmlCompat.fromHtml("<font color='#008000'>" + mesg + "</font>",
                                HtmlCompat.FROM_HTML_MODE_LEGACY), Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                mesg = getString(R.string.saveFail);
                Toast.makeText(this,
                        HtmlCompat.fromHtml("<font color='red'><b>" + mesg + "</b></font>",
                                HtmlCompat.FROM_HTML_MODE_LEGACY), Toast.LENGTH_LONG).show();
                if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                    Log.e(TAG, "1670, Failed to export csv file");
            }
        }
    }
    // End of exportDb2CSV()

    /**
     * @noinspection ResultOfMethodCallIgnored
     ********************************************************************************************/
    // Export current species list to both data directories
    //  /Documents/TransektCount/species_YYYYMMDD_hhmmss.csv
    //  /Documents/TourCount/species_YYYYMMDD_hhmmss.csv and
    private void exportSpeciesList() {
        // outFileTour -> /storage/emulated/0/Documents/TourCount/species_yyyyMMdd_HHmmss.csv
        // outFileTransect -> /storage/emulated/0/Documents/TransektCount/species_yyyyMMdd_HHmmss.csv
        File pathTour, outFileTour, pathTransect, outFileTransect;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) // Android 10+
        {
            pathTransect = new File(Environment.getExternalStorageDirectory() + "/Documents/TransektCount");
            pathTour = new File(Environment.getExternalStorageDirectory() + "/Documents/TourCount");
        } else {
            pathTransect = new File(Environment
                    .getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS) + "/TransektCount");
            pathTour = new File(Environment.
                    getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS) + "/TourCount");
        }

        // Check if we can write the media
        mExternalStorageWriteable = Environment.MEDIA_MOUNTED.equals(sState);

        if (!mExternalStorageWriteable) {
            mesg = getString(R.string.noCard);
            Toast.makeText(this,
                    HtmlCompat.fromHtml("<font color='red'><b>" + mesg + "</b></font>",
                            HtmlCompat.FROM_HTML_MODE_LEGACY), Toast.LENGTH_LONG).show();
        } else {
            // Export species list into species_yyyyMMdd_HHmmss.csv
            dbHelper = new DbHelper(this);
            database = dbHelper.getWritableDatabase();

            String[] codeArray;
            String[] nameArray;
            String[] nameArrayL;

            codeArray = countDataSource.getAllStringsSrtCode("code");
            nameArray = countDataSource.getAllStringsSrtCode("name");
            nameArrayL = countDataSource.getAllStringsSrtCode("name_g");

            int specNum = codeArray.length;

            // If TransektCount is installed export to /Documents/TransektCount
            if (pathTransect.exists() && pathTransect.isDirectory()) {
                pathTransect.mkdirs(); // Just verify pathTour, result ignored
                String language = Locale.getDefault().toString().substring(0, 2);
                if (language.equals("de")) {
                    if (Objects.equals(tourName, ""))
                        outFileTransect = new File(pathTransect, "/species_Tour_de"
                                + getcurDate() + ".csv");
                    else
                        outFileTransect = new File(pathTransect, "/species_Tour_de"
                                + getcurDate() + "_" + tourName + ".csv");
                } else {
                    if (Objects.equals(tourName, ""))
                        outFileTransect = new File(pathTransect, "/species_Tour_en"
                                + getcurDate() + ".csv");
                    else
                        outFileTransect = new File(pathTransect, "/species_Tour_en"
                                + getcurDate() + "_" + tourName + ".csv");
                }
                try {
                    CSVWriter csvWrite = new CSVWriter(new FileWriter(outFileTransect));

                    int i = 0;
                    while (i < specNum) {
                        String[] specLine =
                                {
                                        codeArray[i],
                                        nameArray[i],
                                        nameArrayL[i]
                                };
                        i++;
                        csvWrite.writeNext(specLine);
                    }
                    csvWrite.close();
                } catch (Exception e) {
                    mesg = getString(R.string.saveFailListTransect);
                    Toast.makeText(this,
                            HtmlCompat.fromHtml("<font color='red'><b>" + mesg + "</b></font>",
                                    HtmlCompat.FROM_HTML_MODE_LEGACY), Toast.LENGTH_LONG).show();
                }
            }

            // Export to /Documents/TourCount
            try {
                pathTour.mkdirs(); // Just verify pathTour, result ignored
                if (Objects.equals(tourName, ""))
                    outFileTour = new File(pathTour, "/species_Tour_" + getcurDate() + ".csv");
                else
                    outFileTour = new File(pathTour, "/species_Tour_" + getcurDate() + "_" + tourName + ".csv");
                CSVWriter csvWrite = new CSVWriter(new FileWriter(outFileTour));

                int i = 0;
                while (i < specNum) {
                    String[] specLine =
                            {
                                    codeArray[i],
                                    nameArray[i],
                                    nameArrayL[i]
                            };
                    i++;
                    csvWrite.writeNext(specLine);
                }
                csvWrite.close();
                mesg = getString(R.string.saveList);
                Toast.makeText(this,
                        HtmlCompat.fromHtml("<font color='#008000'>" + mesg + "</font>",
                                HtmlCompat.FROM_HTML_MODE_LEGACY), Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                mesg = getString(R.string.saveFailList);
                Toast.makeText(this,
                        HtmlCompat.fromHtml("<font color='red'><b>" + mesg + "</b></font>",
                                HtmlCompat.FROM_HTML_MODE_LEGACY), Toast.LENGTH_LONG).show();
            }
        }
    }
    // End of exportSpeciesList()

    /**********************************************************************************************/
    // Clear all relevant DB values, reset to basic DB 
    private void resetToBasisDb() {
        // Confirm dialogue before anything else takes place
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setIcon(android.R.drawable.ic_dialog_alert);
        builder.setMessage(R.string.confirmResetDB);
        builder.setCancelable(false);
        builder.setPositiveButton(R.string.deleteButton, (dialog, id) ->
        {
            boolean r_ok = clearDBValues();
            if (r_ok) {
                mesg = getString(R.string.reset2basic);
                Toast.makeText(this,
                        HtmlCompat.fromHtml("<font color='#008000'>" + mesg + "</font>",
                                HtmlCompat.FROM_HTML_MODE_LEGACY), Toast.LENGTH_SHORT).show();
            }
            Objects.requireNonNull(getSupportActionBar()).setTitle("");
        });
        builder.setNegativeButton(R.string.cancelButton, (dialog, id) -> dialog.cancel());
        alert = builder.create();
        alert.show();
    }

    // Clear temp_loc in table tmp
    private void clear_loc() {
        dbHelper = new DbHelper(this);
        database = dbHelper.getWritableDatabase();
        String sql = "UPDATE tmp SET temp_loc = '';";
        database.execSQL(sql);
        dbHelper.close();
    }

    // Clear DB values for basic DB
    private boolean clearDBValues() {
        dbHelper = new DbHelper(this);
        database = dbHelper.getWritableDatabase();
        boolean r_ok = true;

        try {
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
                    + DbHelper.S_COUNTRY + " = '', "
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
                    + DbHelper.S_NOTES + " = '', "
                    + DbHelper.S_STATE + " = '', "
                    + DbHelper.S_ST_LOCALITY + " = '';";
            database.execSQL(sql);

            sql = "DELETE FROM " + DbHelper.INDIVIDUALS_TABLE;
            database.execSQL(sql);

            sql = "UPDATE " + DbHelper.TEMP_TABLE + " SET "
                    + DbHelper.T_TEMP_LOC + " = '', "
                    + DbHelper.T_TEMP_CNT + " = 0;";
            database.execSQL(sql);

            dbHelper.close();
        } catch (Exception e) {
            if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                Log.e(TAG, "1878, Failed to reset DB");

            mesg = getString(R.string.resetFail);
            Toast.makeText(this,
                    HtmlCompat.fromHtml("<font color='red'><b>" + mesg + "</b></font>",
                            HtmlCompat.FROM_HTML_MODE_LEGACY), Toast.LENGTH_LONG).show();
            dbHelper.close();
            r_ok = false;
        }
        return r_ok;
    }
    // End of resetToBasisDb()

}
