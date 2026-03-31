package com.wmstein.tourcount;

import static com.wmstein.tourcount.TourCountApplication.isFirstStart;
import static com.wmstein.tourcount.Utils.fromHtml;
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
import android.os.Vibrator;
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
import androidx.core.view.MenuCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

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
 * last edited on 2026-03-27
 */
public class WelcomeActivity
        extends AppCompatActivity
        implements SharedPreferences.OnSharedPreferenceChangeListener {
    private final String TAG = "WelcomeAct";

    private TourCountApplication tourCount;
    private View baseLayout;

    LocationService locationService;
    private boolean locServiceOn = false;

    SoundService soundService;
    Intent sndIntent;
    private boolean sndServiceOn = false;

    private ChangeLog cl;
    public boolean doubleBackToExitPressedTwice = false;

    // Import/export stuff
    private File inFile;
    private File outFile;
    private boolean mExternalStorageWriteable = false;
    private final String sState = Environment.getExternalStorageState();
    private final Handler mHandler = new Handler(Looper.getMainLooper());

    // Preferences
    private SharedPreferences prefs;
    private SharedPreferences.Editor editor;
    private String outPref;
    private boolean buttonSoundPref;
    private boolean storagePermGranted = false;  // Storage permission state
    private boolean fineLocationPermGranted = false; // Foreground location permission state
    private String dataLanguage = "";

    // DB handling
    private SQLiteDatabase database;
    private DbHelper dbHelper;
    private SectionDataSource sectionDataSource;
    private Section section;
    private HeadDataSource headDataSource;
    private CountDataSource countDataSource;

    // Inserting tourName into filename with plausi check
    private final String regexFilename = "[^a-zA-Z_0-9äöüÄÖÜ-]";
    private String tourName = ""; // The tour name as shown
    private String tourNameDir = ""; // The tour name as part of a filename

    private String mesg;
    private AlertDialog alert;

    @SuppressLint({"SourceLockedOrientationActivity", "ApplySharedPref"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "143, onCreate");

        tourCount = (TourCountApplication) getApplication();

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        // Get preferences
        prefs = TourCountApplication.getPrefs();
        editor = prefs.edit();

        // Initialize sound service
        buttonSoundPref = prefs.getBoolean("pref_button_sound", false); // Prepare SoundService

        if (buttonSoundPref) {
            soundService = new SoundService(getApplicationContext());
            sndIntent = new Intent(getApplicationContext(), SoundService.class);
            startService(sndIntent);
            sndServiceOn = true;
            editor.putBoolean("snd_srv_on", true);
            editor.commit();
        }

        // Proximity sensor handling in preferences menu
        SensorManager sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        Sensor proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);

        // Check for Proximity and Ambient Light sensor
        boolean prefProx = proximitySensor != null; // true if proximity sensor is available

        // Gray out preferences menu item pref_button_vib when device has no vibrator
        Vibrator vibrator = getApplicationContext().getSystemService(Vibrator.class);
        boolean prefVib = vibrator.hasVibrator(); // true if vibrator is available

        // Set pref_prox and pref_button_vib enabler, used in SettingsFragment
        editor.putBoolean("enable_prox", prefProx);
        editor.putBoolean("enable_vib", prefVib);
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

        cl = new ChangeLog(this, prefs);

        // Show changelog for new version
        if (cl.firstRun())
            cl.getLogDialog().show();

        // Check initial storage permission state and provide dialog
        storagePermGranted = isStoragePermGranted();
        if (!storagePermGranted) // in self permission
        {
            PermissionsStorageDialogFragment.newInstance().show(getSupportFragmentManager(),
                    PermissionsStorageDialogFragment.class.getName());

            mesg = getString(R.string.storage_perm_denied);
            Toast.makeText(this,
                    fromHtml("<font color='red'><b>" + mesg + "</b></font>"),
                    Toast.LENGTH_LONG).show();

            // Prepare to ask foreground location permission only once
            editor.putBoolean("has_asked_foreground", false);
            editor.commit();
        }
        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.d(TAG, "231, onCreate, storagePermGranted: " + storagePermGranted);

        // Check DB version and upgrade if necessary
        dbHelper = new DbHelper(this);
        database = dbHelper.getWritableDatabase(); // Make DB upgrade if necessary
        dbHelper.close();

        // Set up the data sources
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
                    fromHtml("<font color='red'><b>" + mesg + "</b></font>"),
                    Toast.LENGTH_LONG).show();

            mHandler.postDelayed(this::finishAndRemoveTask, 2000);
        }

        // Prepare tourName to be part of a filename
        tourNameDir = tourName;
        if (Objects.equals(tourNameDir, ""))
            return;
        else {
            tourNameDir = tourNameDir.replaceAll(regexFilename, "");
        }

        // Test for existence of file /storage/emulated/0/Documents/TourCount/tourcount0.db
        storagePermGranted = isStoragePermGranted();
        if (storagePermGranted) {
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

        if (!inFile.exists())
            exportBasisDb(0); // create directory and create initial tourcount0.db file, 0: short name, no message
        }

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
                    // Stop sound server
                    if (sndServiceOn) {
                        locationService.releaseSoundA();
                        soundService.releaseSoundM();
                        soundService.releaseSoundP();

                        stopService(sndIntent);
                        sndServiceOn = false;
                        editor = prefs.edit();
                        editor.putBoolean("snd_srv_on", false);
                        editor.commit();
                    }
                    finish();
                    remove();
                } else {
                    doubleBackToExitPressedTwice = true;

                    mesg = getString(R.string.back_twice);
                    Toast.makeText(getApplicationContext(),
                            fromHtml("<font color='blue'>" + mesg + "</font>"),
                            Toast.LENGTH_SHORT).show();

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
            Log.i(TAG, "361, onResume");

        prefs = TourCountApplication.getPrefs();
        prefs.registerOnSharedPreferenceChangeListener(this);
        editor = prefs.edit();

        outPref = prefs.getString("pref_sort_output", "names"); // sort mode csv-export
        locServiceOn = prefs.getBoolean("loc_srv_on", false);
        sndServiceOn = prefs.getBoolean("snd_srv_on", false);
        // mail address for OSM query
        String emailString = prefs.getString("email_String", ""); // for reliable query of Nominatim service
        // option for OSM reverse geocoding
        boolean metaPref = prefs.getBoolean("pref_metadata", false); // use Reverse Geocoding
        dataLanguage = prefs.getString("pref_sel_data_lang", "de");

        storagePermGranted = isStoragePermGranted(); // set storagePermGranted from self permission

        if (isFirstStart) {
            // This is to remind a missing email address for Nominatim Reverse Geocoder.
            //   Info about the first GPS lock is handled in LocationService onLocationChanged().
            if (metaPref && Objects.equals(emailString, "")) {
                mesg = getString(R.string.missingEmail);
                Toast.makeText(this, // orange
                        fromHtml("<font color='#ff6000'>" + mesg + "</font>"),
                        Toast.LENGTH_SHORT).show();
            }
            isFirstStart = false;
        }

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

        // Prepare modified tourName to be part of a filename
        tourNameDir = tourName;
        if (!Objects.equals(tourNameDir, "")) {
            assert tourNameDir != null;
            tourNameDir = tourNameDir.replaceAll(regexFilename, "");
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
            Log.i(TAG, "436, onResume, fineLocationPermGranted: " + fineLocationPermGranted);

        locServiceOn = false;
        locationDispatcher(0);
    }
    // End of onResume()

    // Check initial external storage permission and set 'storagePermGranted'
    private Boolean isStoragePermGranted() {
        boolean storageGranted;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // check permission MANAGE_EXTERNAL_STORAGE for Android >= 11
            storageGranted = Environment.isExternalStorageManager();

            if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                Log.i(TAG, "451, ManageStoragePermission: " + storagePermGranted);
        } else {
            storageGranted = ContextCompat.checkSelfPermission(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;

            if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                Log.i(TAG, "457, ExtStoragePermission: " + storagePermGranted);
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
    //  0 = start location service just to get a fix as early as possible
    //  2 = end location service for WelcomeActivity
    public void locationDispatcher(int locationDispatcherMode) {
        if (fineLocationPermGranted) // current location permission state granted
        {
            editor = prefs.edit();
            // Handle action here
            switch (locationDispatcherMode) {
                case 0 -> {
                    // Get location data
                    locationService = new LocationService(getApplicationContext());
                    if (!locServiceOn) {
                        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                            Log.i(TAG, "483, locationDispatcher 0");

                        Intent sIntent = new Intent(getApplicationContext(), LocationService.class);
                        startService(sIntent);
                        locServiceOn = true;

                        editor.putBoolean("loc_srv_on", true);
                        editor.commit();
                    }
                }
                case 2 -> {
                    // Stop location service
                    if (locServiceOn) {
                        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                            Log.i(TAG, "497, locationDispatcher 2");

                        locationService.releaseSoundA();
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
                            fromHtml("<font color='red'><b>" + mesg + "</b></font>"),
                            Toast.LENGTH_LONG).show();
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
                            fromHtml("<font color='red'><b>" + mesg + "</b></font>"),
                            Toast.LENGTH_LONG).show();
                }
            }
            return true;
        } else if (id == R.id.exportBasisMenu) {
            // Call exportBasisDb()
            if (storagePermGranted) {
                exportBasisDb(2);
            } else {
                PermissionsStorageDialogFragment.newInstance().show(getSupportFragmentManager(),
                        PermissionsStorageDialogFragment.class.getName());
                if (storagePermGranted) {
                    exportBasisDb(2);
                } else {
                    mesg = getString(R.string.storage_perm_denied);
                    Toast.makeText(this,
                            fromHtml("<font color='red'><b>" + mesg + "</b></font>"),
                            Toast.LENGTH_LONG).show();
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
                            fromHtml("<font color='red'><b>" + mesg + "</b></font>"),
                            Toast.LENGTH_LONG).show();
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
                    fromHtml("<font color='blue'>" + mesg + "</font>"),
                    Toast.LENGTH_SHORT).show();

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
        locServiceOn = prefs.getBoolean("loc_srv_on", false);
        sndServiceOn = prefs.getBoolean("snd_srv_on", false);
        buttonSoundPref = prefs.getBoolean("pref_button_sound", false);

        // Stop sound service when denied in settings
        if (!buttonSoundPref && sndServiceOn) {
            stopService(sndIntent);
            sndServiceOn = false;
            editor = prefs.edit();
            editor.putBoolean("snd_srv_on", false);
            editor.commit();
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "684, onPause");

        countDataSource.close();
        sectionDataSource.close();

        locationDispatcher(2); // Stop location service

        prefs.unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onStop() {
        super.onStop();

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "699, onStop");

        baseLayout.invalidate();

        if (sndServiceOn) {
            soundService.releaseSoundM();
            soundService.releaseSoundP();
        }

        if (locServiceOn) {
            locationService.releaseSoundA();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (sndServiceOn) {
            stopService(sndIntent);
            sndServiceOn = false;
            editor = prefs.edit();
            editor.putBoolean("snd_srv_on", false);
            editor.commit();
        }

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "726, onDestroy, sndServiceOn: " + sndServiceOn);
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
                fromHtml("<font color='blue'>" + mesg + "</font>"),
                Toast.LENGTH_SHORT).show();

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

    // ActivityResultLauncher is part2 of importBasisDb() and importDBFile()
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
                            if (selectedFile != null)
                                inFile = new File(selectedFile);
                            else
                                inFile = null;
                        }
                    } else if ((result.getResultCode() == Activity.RESULT_FIRST_USER)) {
                        mesg = getString(R.string.noFile);
                        Toast.makeText(getApplicationContext(), // orange
                                fromHtml("<font color='#ff6000'><b>" + mesg + "</b></font>"),
                                Toast.LENGTH_LONG).show();
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

                                // Prepare new tourName to be part of a filename
                                tourNameDir = tourName;
                                if (Objects.equals(tourNameDir, ""))
                                    return;
                                else {
                                    tourNameDir = tourNameDir.replaceAll(regexFilename, "");
                                }

                                mesg = getString(R.string.importDB);
                                Toast.makeText(getApplicationContext(), // bright green
                                        fromHtml("<font color='#008000'>" + mesg + "</font>"),
                                        Toast.LENGTH_SHORT).show();
                            } catch (IOException e) {
                                mesg = getString(R.string.importFail);
                                Toast.makeText(getApplicationContext(),
                                        fromHtml("<font color='red'><b>" + mesg + "</b></font>"),
                                        Toast.LENGTH_LONG).show();
                            }
                        });
                        builder.setNegativeButton(R.string.cancelButton, (dialog, id) -> dialog.cancel());
                        alert = builder.create();
                        alert.show();
                    }
                }
            });
    // End of part2 of import of DB files

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
    // Import species list (also from TransektCount file species_YYYY-MM-DD_hhmmss.csv)
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
                            if (selectedFile != null)
                                inFile = new File(selectedFile);
                            else
                                inFile = null;
                        }
                    }
                    // RESULT_FIRST_USER is set in AdvFileChooser for no file
                    else if ((result.getResultCode() == Activity.RESULT_FIRST_USER)) {
                        mesg = getString(R.string.noFile);
                        Toast.makeText(getApplicationContext(), // orange
                                fromHtml("<font color='#ff6000'><b>" + mesg + "</b></font>"),
                                Toast.LENGTH_LONG).show();
                    }
                    if (inFile != null) {
                        String csvLine;
                        boolean brError = false;

                        // Check for old version of species list
                        try {
                            BufferedReader br = new BufferedReader(new FileReader(inFile));
                            csvLine = br.readLine(); // Read 1. line only
                            String[] specLine = csvLine.split(",");
                            if (Objects.equals(specLine[0], "nocode"))
                                mesg = getString(R.string.confirmListImport);
                            else
                                mesg = getString(R.string.specsCommonLang) + "\n\n" + getString(R.string.confirmListImport);
                            br.close();
                        } catch (Exception e) {
                            mesg = getString(R.string.br_Error);
                            brError = true;
                        }

                        AlertDialog.Builder builder = new AlertDialog.Builder(WelcomeActivity.this);
                        builder.setIcon(android.R.drawable.ic_dialog_alert);
                        builder.setMessage(mesg);
                        if (brError) {
                            builder.setCancelable(true);
                            builder.setNegativeButton(R.string.cancelButton, (dialog, id) -> dialog.cancel());
                        } else {
                            builder.setCancelable(false);
                            builder.setPositiveButton(R.string.importButton, (dialog, id) ->
                            {
                                clearDBforImport();
                                readSpeciesCSV(inFile);
                            });
                            builder.setNegativeButton(R.string.cancelButton, (dialog, id) -> dialog.cancel());
                        }
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
                + DbHelper.T_TEMP_CNT + " = 0;";
        database.execSQL(sql);

        dbHelper.close();

        editor = prefs.edit();
        editor.putInt("item_Position", 0);
        editor.putInt("count_id", 1);
        editor.apply();
    }

    // Read an exported species list and write items to table counts
    private void readSpeciesCSV(File inFile) {
        try {
            mesg = getString(R.string.waitImport);
            Toast.makeText(this,
                    fromHtml("<font color='blue'>" + mesg + "</font>"),
                    Toast.LENGTH_SHORT).show();

            String csvLine;
            List<String> codeArray = new ArrayList<>();
            List<String> nameArray = new ArrayList<>();
            List<String> nameGArray = new ArrayList<>();

            BufferedReader br = new BufferedReader(new FileReader(inFile));
            boolean newList372 = true;

            csvLine = br.readLine(); // Read 1. line only
            String[] specLine = csvLine.split(",");
            if (Objects.equals(specLine[0], "nocode")) {
                dataLanguage = specLine[2];
                editor = prefs.edit();
                editor.putString("pref_sel_data_lang", dataLanguage);
                editor.apply();
            } else
                newList372 = false;

            editor = prefs.edit();
            editor.putBoolean("new_list_372", newList372); // controls data language setting
            editor.apply();

            br.close();

            br = new BufferedReader(new FileReader(inFile));

            int i = 0;       // index of imported list
            int iCounts = 1; // index of id in table counts
            while ((csvLine = br.readLine()) != null) // for each csvLine
            {
                specLine = csvLine.split(",");
                // 1. line fields contain String[0]: "nocode", [1]: "language", [2]: "de"|"en"|"fr"|"it"|"es"
                if (Objects.equals(specLine[0], "nocode")) {
                    if (newList372) {
                        iCounts--;
                        i--;
                    }
                }
                else {
                    // comma-separated 0:code, 1:name, 2:nameL
                    codeArray.add(i, specLine[0]);
                    nameArray.add(i, specLine[1]);
                    nameGArray.add(i, specLine[2]);
                    countDataSource.writeCountItem(String.valueOf(iCounts), codeArray.get(i),
                            nameArray.get(i), nameGArray.get(i));
                }
                i++;
                iCounts++;
            }
            br.close();

            mesg = getString(R.string.importList);
            Toast.makeText(this, // bright green
                    fromHtml("<font color='#008000'>" + mesg + "</font>"),
                    Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            mesg = getString(R.string.importListFail);
            Toast.makeText(this,
                    fromHtml("<font color='red'><b>" + mesg + "</b></font>"),
                    Toast.LENGTH_LONG).show();
        }
    }
    // End of importSpeciesList()

    /***********************************************************************************************
     * The next four functions below are for exporting data files.
     * They've been put here because no database should be open at this point.
     **********************************************************************************************/
    // Exports Basic DB to Documents/TourCount/tourcount0_name.db
    // hasNoName indicated initial creation of tourcount0.db if it does not exist
    private void exportBasisDb(int i) {
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
        if (i == 0)
            outFile = new File(path, "/tourcount0.db");
        else if (i >= 0) {
            dataLanguage = prefs.getString("pref_sel_data_lang", "de");

            if (Objects.equals(tourNameDir, ""))
                outFile = new File(path, "/tourcount0_" + dataLanguage + ".db");
            else
                outFile = new File(path, "/tourcount0_" + dataLanguage + "_" + tourNameDir + ".db");
        }

        // Check if we can write the media
        mExternalStorageWriteable = Environment.MEDIA_MOUNTED.equals(sState);

        if (!mExternalStorageWriteable) {
            mesg = getString(R.string.noCard);
            Toast.makeText(this,
                    fromHtml("<font color='red'><b>" + mesg + "</b></font>"),
                    Toast.LENGTH_LONG).show();
        } else {
            // Export the basic db
            try {
                // Save current db as backup db tmpFile
                copy(inFile, tmpFile);

                // Clear DB values for basic DB
                clearDBValues();

                // Write Basic DB
                copy(inFile, outFile);

                // Restore actual db from tmpFile
                copy(tmpFile, inFile);

                // Delete backup db
                boolean d0 = tmpFile.delete();

                // Show message success
                if (d0 && i == 2) {
                    mesg = getString(R.string.saveBasisDB);
                    Toast.makeText(this, // bright green
                            fromHtml("<font color='#008000'>" + mesg + "</font>"),
                            Toast.LENGTH_SHORT).show();
                }
            } catch (IOException e) {
                mesg = getString(R.string.saveFail);
                Toast.makeText(this,
                        fromHtml("<font color='red'><b>" + mesg + "</b></font>"),
                        Toast.LENGTH_LONG).show();
            }
        }
    }
    // End of exportBasisDb()

    @SuppressLint({"SdCardPath", "LongLogTag"})
    private void exportDb() {
        // Public data directory for outFile: Documents/TourCount/
        File path;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) // Android 10+
        {
            path = Environment.getExternalStorageDirectory();
            path = new File(path + "/Documents/TourCount");
        } else {
            path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
            path = new File(path + "/TourCount");
        }

        String date, start_tm;
        section = sectionDataSource.getSection();
        date = section.date;
        start_tm = section.start_tm;
        String dbDate, dbTime;

        dataLanguage = prefs.getString("pref_sel_data_lang", "de");

        if (date != null && !date.isEmpty()) {
            String dbDateEU, dbDateEN;
            dbDateEU = date.substring(6, 10) + date.substring(3, 5) + date.substring(0, 2);
            dbDateEN = date.substring(0, 4) + date.substring(5, 7) + date.substring(8, 10);
            if (dataLanguage.equals("de") || dataLanguage.equals("fr")
                    || dataLanguage.equals("it") || dataLanguage.equals("es")) {
                try {
                    dbDate = dbDateEU;
                } catch (Exception e) {
                    dbDate = dbDateEN;
                }
            } else {
                try {
                    dbDate = dbDateEN;
                } catch (Exception e) {
                    dbDate = dbDateEU;
                }
            }
        } else
            dbDate = "";

        if (start_tm != null && !start_tm.isEmpty()) {
            dbTime = start_tm.substring(0, 2) + start_tm.substring(3, 5);

            if (!dbDate.isEmpty())
                dbDate = dbDate + "_" + dbTime; // yyyymmdd_hhmm
        } else
            dbDate = "";

        //noinspection ResultOfMethodCallIgnored
        path.mkdirs(); // Just verify path, result ignored

        // outFile -> /storage/emulated/0/Documents/TourCount/tourcount_yyyyMMdd_HHmm.db
        if (Objects.equals(tourNameDir, "") && Objects.equals(dbDate, ""))
            outFile = new File(path, "/tourcount_" + dataLanguage + "_" + getcurDate() + ".db");
        else if (Objects.equals(tourNameDir, ""))
            outFile = new File(path, "/tourcount_" + dataLanguage + "_" + dbDate + ".db");
        else if (Objects.equals(dbDate, ""))
            outFile = new File(path, "/tourcount_" + dataLanguage + "_" + tourNameDir + "_" + getcurDate() + ".db");
        else
            outFile = new File(path, "/tourcount_" + dataLanguage + "_" + tourNameDir + "_" + dbDate + ".db");

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
                    fromHtml("<font color='red'><b>" + mesg + "</b></font>"),
                    Toast.LENGTH_LONG).show();
        } else {
            // Export the db
            try {
                copy(inFile, outFile);

                mesg = getString(R.string.saveDB);
                Toast.makeText(this,
                        fromHtml("<font color='blue'>" + mesg + "</font>"),
                        Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                mesg = getString(R.string.saveFail);
                Toast.makeText(this,
                        fromHtml("<font color='red'><b>" + mesg + "</b></font>"),
                        Toast.LENGTH_LONG).show();
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
        // and distinguish versions (as getExternalStoragePublicDirectory is deprecated in Q, Android 10)
        File path;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) // Android 10+
        {
            path = Environment.getExternalStorageDirectory();
            path = new File(path + "/Documents/TourCount");
        } else {
            path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
            path = new File(path + "/TourCount");
        }

        // Set environment data
        String date, start_tm, end_tm;
        int temps, winds, clouds, tempe, winde, cloude;

        section = sectionDataSource.getSection();
        temps = section.tmp;
        tempe = section.tmp_end;
        winds = section.wind;
        winde = section.wind_end;
        clouds = section.clouds;
        cloude = section.clouds_end;
        date = section.date;
        start_tm = section.start_tm;
        end_tm = section.end_tm;

        dataLanguage = prefs.getString("pref_sel_data_lang", "de");

        String csvDate, csvTime;

        if (date != null && !date.isEmpty()) {
            String csvDateEU, csvDateEN;
            csvDateEU = date.substring(6, 10) + date.substring(3, 5) + date.substring(0, 2);
            csvDateEN = date.substring(0, 4) + date.substring(5, 7) + date.substring(8, 10);
            if (dataLanguage.equals("de") || dataLanguage.equals("fr")
                    || dataLanguage.equals("it") || dataLanguage.equals("es")) {
                try {
                    csvDate = csvDateEU;
                } catch (Exception e) {
                    csvDate = csvDateEN;
                }
            } else {
                try {
                    csvDate = csvDateEN;
                } catch (Exception e) {
                    csvDate = csvDateEU;
                }
            }
        } else
            csvDate = "";

        if (start_tm != null && !start_tm.isEmpty()) {
            csvTime = start_tm.substring(0, 2) + start_tm.substring(3, 5);
            if (!csvDate.isEmpty())
                csvDate = csvDate + "_" + csvTime; // yyyymmdd_hhmm
        } else
            csvDate = ""; // has only a value when both date and start time are given

        //noinspection ResultOfMethodCallIgnored
        path.mkdirs(); // Just verify path, result ignored

        if (Objects.equals(tourNameDir, "") && Objects.equals(csvDate, ""))
            outFile = new File(path, "/Tour_" + dataLanguage + "_" + getcurDate() + ".csv");
        else if (Objects.equals(tourNameDir, ""))
            outFile = new File(path, "/Tour_" + dataLanguage + "_" + csvDate + ".csv");
        else if (Objects.equals(csvDate, ""))
            outFile = new File(path, "/Tour_" + dataLanguage + "_" + tourNameDir + "_" + getcurDate() + ".csv");
        else
            outFile = new File(path, "/Tour_" + dataLanguage + "_" + tourNameDir + "_" + csvDate + ".csv");

        String sectName;
        String sectNotes;

        Head head;
        String country, b_state, inspecName;
        String plz, city, place, locality;
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
                    fromHtml("<font color='red'><b>" + mesg + "</b></font>"),
                    Toast.LENGTH_LONG).show();
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

                // Consult Section on Head tables for head and meta info
                section = sectionDataSource.getSection();

                sectName = "\"" + section.name + "\"";
                sectNotes = "\"" + section.notes + "\"";
                country = "\"" + section.country + "\"";
                b_state = "\"" + section.b_state + "\"";
                plz = "\"" + section.plz + "\"";
                city = "\"" + section.city + "\"";
                place = "\"" + section.place + "\"";
                locality = "\"" + section.st_locality + "\"";

                headDataSource.open();
                head = headDataSource.getHead();
                inspecName = "\"" + head.observer + "\"";
                headDataSource.close();

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


                // Write environment data
                String[] arrEnvironment =
                        {
                                "\"" + date + "\"",
                                getString(R.string.starttm),
                                "\"" + start_tm + "\"",
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
                                "\"" + end_tm + "\"",
                                String.valueOf(tempe),
                                String.valueOf(winde),
                                String.valueOf(cloude)
                        };
                csvWrite.writeNext(arrEnvironment2);

                // Empty row
                csvWrite.writeNext(arrEmpt);

                String nameSpecG = Utils.nameSpecG(dataLanguage);

                // Write counts headline
                //    Species Name, Local Name, Code, Counts, Spec.-Notes
                String[] arrCntHead =
                        {
                                getString(R.string.name_spec),
                                nameSpecG,
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
                            + DbHelper.C_NOTES + " = '0' or "
                            + DbHelper.C_COUNT_F1I + " > 0 or " + DbHelper.C_COUNT_F2I + " > 0 or "
                            + DbHelper.C_COUNT_F3I + " > 0 or " + DbHelper.C_COUNT_PI + " > 0 or "
                            + DbHelper.C_COUNT_LI + " > 0 or " + DbHelper.C_COUNT_EI + " > 0)"
                            + " order by " + DbHelper.C_NAME, null, null);
                } else {
                    curCSVCnt = database.rawQuery("select * from " + DbHelper.COUNT_TABLE
                            + " WHERE " + " ("
                            + DbHelper.C_NOTES + " = '0' or "
                            + DbHelper.C_COUNT_F1I + " > 0 or " + DbHelper.C_COUNT_F2I + " > 0 or "
                            + DbHelper.C_COUNT_F3I + " > 0 or " + DbHelper.C_COUNT_PI + " > 0 or "
                            + DbHelper.C_COUNT_LI + " > 0 or " + DbHelper.C_COUNT_EI + " > 0)"
                            + " order by " + DbHelper.C_CODE, null, null);
                }

                // Get the number of individuals with attributes
                int cnts;       // individuals icount
                String strcnts;
                int cntsmf;     // Imago male or female
                String strcntsmf;
                int cntsm = 0;  // Imago male
                String strcntsm;
                int cntsf = 0;  // Imago female
                String strcntsf;
                int cntsp = 0;  // Pupa
                String strcntsp;
                int cntsl = 0;  // Caterpillar
                String strcntsl;
                int cntse = 0;  // Egg
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
                    spcode = "\"" + curCSVCnt.getString(8) + "\""; // species code from count table
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

                    String sp_notes;   // species notes
                    sp_notes = "\"" + curCSVCnt.getString(9) + "\"";

                    // Species table
                    String[] arrStr =
                            {
                                    spname,                     // species name
                                    curCSVCnt.getString(10), // local name
                                    spcode,                     // species code
                                    strcntsmf,                  // count ♂ o. ♀
                                    strcntsm,                   // count ♂
                                    strcntsf,                   // count ♀
                                    strcntsp,                   // count pupa
                                    strcntsl,                   // count caterpillar
                                    strcntse,                   // count egg
                                    sp_notes                    // species notes
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
                                getString(R.string.bems)
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
                                    curCSVInd.getString(2),                // species name
                                    strcnts,                                 // indiv. counts
                                    "\"" + curCSVInd.getString(9) + "\"",  // locality
                                    lngi,                                    // longitude
                                    latit,                                   // latitude
                                    String.valueOf(Math.round(uncer + 20)),  // uncertainty + 20 m extra
                                    String.valueOf(Math.round(heigh)),       // height
                                    "\"" + curCSVInd.getString(7) + "\"", // date
                                    curCSVInd.getString(8),               // time
                                    curCSVInd.getString(10),              // sexus
                                    curCSVInd.getString(11),              // phase
                                    "\"" + spstate0 + "\"",                  // status
                                    "\"" + curCSVInd.getString(13) + "\"" // indiv. notes
                            };
                    csvWrite.writeNext(arrIndividual);

                    if (longi != 0) // Has coordinates
                    {
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
                        fromHtml("<font color='blue'>" + mesg + "</font>"),
                        Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                mesg = getString(R.string.saveFail);
                Toast.makeText(this,
                        fromHtml("<font color='red'><b>" + mesg + "</b></font>"),
                        Toast.LENGTH_LONG).show();
            }
        }
    }
    // End of exportDb2CSV()

    /**
     * @noinspection ResultOfMethodCallIgnored
     ********************************************************************************************/
    // Export current species list to both data directories
    //  /Documents/TransektCount/species_ll_tour_YYYYMMDD_hhmmss.csv and
    //  /Documents/TourCount/species_ll_tour_YYYYMMDD_hhmmss.csv
    private void exportSpeciesList() {
        // outFileTour -> /storage/emulated/0/Documents/TourCount/species_ll_Tour_tourname_yyyyMMdd_HHmmss.csv
        // outFileTransect -> /storage/emulated/0/Documents/TransektCount/species_ll_Tour_tourname_yyyyMMdd_HHmmss.csv
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
                    fromHtml("<font color='red'><b>" + mesg + "</b></font>"),
                    Toast.LENGTH_LONG).show();
        } else {
            // Export species list into species_ll_tour_Tourname_yyyyMMdd_HHmmss.csv
            dataLanguage = prefs.getString("pref_sel_data_lang", "de");

            String[] codeArray;
            String[] nameArray;
            String[] nameArrayL;

            codeArray = countDataSource.getAllStringsSrtCode("code");
            nameArray = countDataSource.getAllStringsSrtCode("name");
            nameArrayL = countDataSource.getAllStringsSrtCode("name_g");

            int specNum = codeArray.length;

            pathTransect.mkdirs(); // Just verify pathTransect, result ignored
            pathTour.mkdirs(); // Just verify pathTour, result ignored

                switch (dataLanguage) {
                    case "de" -> {
                        if (Objects.equals(tourNameDir, "")) {
                            outFileTransect = new File(pathTransect, "/species_de_Tour_"
                                    + getcurDate() + ".csv");
                            outFileTour = new File(pathTour, "/species_de_Tour_"
                                    + getcurDate() + ".csv");
                        } else {
                            outFileTransect = new File(pathTransect, "/species_de_Tour_"
                                    + tourNameDir + "_" + getcurDate() + ".csv");
                            outFileTour = new File(pathTour, "/species_de_Tour_"
                                    + tourNameDir + "_" + getcurDate() + ".csv");
                        }
                    }
                    case "en" -> {
                        if (Objects.equals(tourNameDir, "")) {
                            outFileTransect = new File(pathTransect, "/species_en_Tour_"
                                    + getcurDate() + ".csv");
                            outFileTour = new File(pathTour, "/species_en_Tour_"
                                    + getcurDate() + ".csv");
                        } else {
                            outFileTransect = new File(pathTransect, "/species_en_Tour_"
                                    + tourNameDir + "_" + getcurDate() + ".csv");
                            outFileTour = new File(pathTour, "/species_en_Tour_"
                                    + tourNameDir + "_" + getcurDate() + ".csv");
                        }
                    }
                    case "fr" -> {
                        if (Objects.equals(tourNameDir, "")) {
                            outFileTransect = new File(pathTransect, "/species_fr_Tour_"
                                    + getcurDate() + ".csv");
                            outFileTour = new File(pathTour, "/species_fr_Tour_"
                                    + getcurDate() + ".csv");
                        } else {
                            outFileTransect = new File(pathTransect, "/species_fr_Tour_"
                                    + tourNameDir + "_" + getcurDate() + ".csv");
                            outFileTour = new File(pathTour, "/species_fr_Tour_"
                                    + tourNameDir + "_" + getcurDate() + ".csv");
                        }
                    }
                    case "it" -> {
                        if (Objects.equals(tourNameDir, "")) {
                            outFileTransect = new File(pathTransect, "/species_it_Tour_"
                                    + getcurDate() + ".csv");
                            outFileTour = new File(pathTour, "/species_it_Tour_"
                                    + getcurDate() + ".csv");
                        } else {
                            outFileTransect = new File(pathTransect, "/species_it_Tour_"
                                    + tourNameDir + "_" + getcurDate() + ".csv");
                            outFileTour = new File(pathTour, "/species_it_Tour_"
                                    + tourNameDir + "_" + getcurDate() + ".csv");
                        }
                    }
                    case "es" -> {
                        if (Objects.equals(tourNameDir, "")) {
                            outFileTransect = new File(pathTransect, "/species_es_Tour_"
                                    + getcurDate() + ".csv");
                            outFileTour = new File(pathTour, "/species_es_Tour_"
                                    + getcurDate() + ".csv");
                        } else {
                            outFileTransect = new File(pathTransect, "/species_es_Tour_"
                                    + tourNameDir + "_" + getcurDate() + ".csv");
                            outFileTour = new File(pathTour, "/species_es_Tour_"
                                    + tourNameDir + "_" + getcurDate() + ".csv");
                        }
                    }
                    default -> {
                        // No data language given
                        if (Objects.equals(tourNameDir, "")) {
                            outFileTransect = new File(pathTransect, "/species_Tour_"
                                    + getcurDate() + ".csv");
                            outFileTour = new File(pathTour, "/species_Tour_"
                                    + getcurDate() + ".csv");
                        } else {
                            outFileTransect = new File(pathTransect, "/species_Tour_"
                                    + tourNameDir + "_" + getcurDate() + ".csv");
                            outFileTour = new File(pathTour, "/species_Tour_"
                                    + tourNameDir + "_" + getcurDate() + ".csv");
                        }
                    }
                }

            // If TransektCount is installed export to /Documents/TransektCount
            if (pathTransect.exists() && pathTransect.isDirectory()) {
                try {
                    CSVWriter csvWrite = new CSVWriter(new FileWriter(outFileTransect));

                    // 1. line contains 0: String "nocode", 1: String "language", 2: String "de"|"en"|"fr"|"it"|"es"
                    String[] specLine1 = {"nocode,language," + dataLanguage};
                    csvWrite.writeNext(specLine1);

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
                            fromHtml("<font color='red'><b>" + mesg + "</b></font>"),
                            Toast.LENGTH_LONG).show();
                }
            }

            // Export to /Documents/TourCount
            if (pathTour.exists() && pathTour.isDirectory()) {
                try {
                    CSVWriter csvWrite = new CSVWriter(new FileWriter(outFileTour));

                    // 1. line with nocode, language, de|en|fr|it|es
                    String[] specLine1 = {"nocode,language," + dataLanguage};
                    csvWrite.writeNext(specLine1);

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
                            fromHtml("<font color='blue'>" + mesg + "</font>"),
                            Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    mesg = getString(R.string.saveFailList);
                    Toast.makeText(this,
                            fromHtml("<font color='red'><b>" + mesg + "</b></font>"),
                            Toast.LENGTH_LONG).show();
                }
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
                Toast.makeText(this, // bright green
                        fromHtml("<font color='#008000'>" + mesg + "</font>"),
                        Toast.LENGTH_SHORT).show();
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

        } catch (Exception e) {
            mesg = getString(R.string.resetFail);
            Toast.makeText(this,
                    fromHtml("<font color='red'><b>" + mesg + "</b></font>"),
                    Toast.LENGTH_LONG).show();

            r_ok = false;
        }
        dbHelper.close();
        return r_ok;
    }
    // End of resetToBasisDb()

}
