package com.wmstein.tourcount;

import static android.os.Build.VERSION.SDK_INT;
import static java.lang.Math.sqrt;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
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
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.MenuCompat;

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

import sheetrock.panda.changelog.ChangeLog;
import sheetrock.panda.changelog.ViewHelp;
import sheetrock.panda.changelog.ViewLicense;

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
 * last edited on 2025-03-17
 */
public class WelcomeActivity
    extends AppCompatActivity
    implements SharedPreferences.OnSharedPreferenceChangeListener
{
    private static final String TAG = "WelcomeAct";

    private TourCountApplication tourCount;

    LocationService locationService;

    // locationDispatcherMode: 
    //  1 = use location service
    //  2 = end location service
    private int locationDispatcherMode;

    private boolean locServiceOn = false;

    // Location info handling
    private double latitude, longitude, height, uncertainty;

    private ChangeLog cl;
    private ViewHelp vh;
    private ViewLicense vl;
    public boolean doubleBackToExitPressedTwice = false;

    // Import/export stuff
    private File inFile;
    private File outFile;
    private boolean mExternalStorageAvailable = false;
    private boolean mExternalStorageWriteable = false;
    private final String sState = Environment.getExternalStorageState();
    private final Handler mHandler = new Handler(Looper.getMainLooper());

    private AlertDialog alert;

    // Preferences
    private SharedPreferences prefs;
    private String outPref;

    private boolean storagePermGranted = false;  // Storage permission state
    private boolean fineLocationPermGranted = false; // Foreground location permission state

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

    @SuppressLint({"SourceLockedOrientationActivity", "ApplySharedPref"})
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        if (MyDebug.DLOG) Log.i(TAG, "145, onCreate");

        tourCount = (TourCountApplication) getApplication();

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        setContentView(R.layout.activity_welcome);
        baseLayout = findViewById(R.id.baseLayout);
        baseLayout.setBackground(tourCount.setBackgr());

        prefs = TourCountApplication.getPrefs();
        SharedPreferences.Editor editor = prefs.edit();

        // Check initial storage permission state and provide dialog
        isStoragePermGranted();
        if (!storagePermGranted)
        {
            // Ask necessary storage permission with info in Snackbar
            showSnackbarPermission(getString(R.string.dialog_storage_hint));

            // Prepare to ask foreground location permission only once
            editor.putBoolean("has_asked_foreground", false);
            editor.commit();
        }
        if (MyDebug.DLOG) Log.d(TAG, "169, onCreate, storagePermGranted: " + storagePermGranted);

        // Check DB version and upgrade if necessary
        dbHandler = new DbHelper(this);
        database = dbHandler.getWritableDatabase();
        dbHandler.close();

        // setup the data sources
        headDataSource = new HeadDataSource(this);
        sectionDataSource = new SectionDataSource(this);
        countDataSource = new CountDataSource(this);
        individualsDataSource = new IndividualsDataSource(this);

        // Get tour name and check for DB integrity
        try
        {
            if (MyDebug.DLOG) Log.i(TAG, "185, onCreate, try section");
            sectionDataSource.open();
            section = sectionDataSource.getSection();
            tourName = section.name;
            if (MyDebug.DLOG) Log.i(TAG, "189, onCreate, tourName: " + tourName);
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
        if (SDK_INT >= Build.VERSION_CODES.Q) // Android 10+
        {
            path = Environment.getExternalStorageDirectory();
            path = new File(path + "/Documents/TourCount");
        }
        else
        {
            path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
            path = new File(path + "/TourCount");
        }

        // Create preliminary tourcount0.db if it does not exist
        inFile = new File(path, "/tourcount0.db");
        if (!inFile.exists())
            exportBasisDb(); // create directory and copy internal DB-data to initial Basis DB-file

        // Different Navigation Bar modes and layouts:
        // - Classic three-button navigation: NavBarMode = 0
        // - Two-button navigation (Android P): NavBarMode = 1
        // - Full screen gesture mode (Android Q): NavBarMode = 2
        // Use onBackPressed logic only if 2 or 3 button Navigation bar is present.
        if (getNavBarMode() == 0 || getNavBarMode() == 1)
        {
            OnBackPressedCallback callback = getOnBackPressedCallback();
            getOnBackPressedDispatcher().addCallback(this, callback);
        }
    }
    // End of onCreate()

    // Check for Navigation bar
    public int getNavBarMode()
    {
        Resources resources = this.getResources();

        @SuppressLint("DiscouragedApi")
        int resourceId = resources.getIdentifier("config_navBarInteractionMode",
            "integer", "android");

        // iMode = 0: 3-button, = 1: 2-button, = 2: gesture
        int iMode = resourceId > 0 ? resources.getInteger(resourceId) : 0;
        if (MyDebug.DLOG) Log.i(TAG, "249, NavBarMode = " + iMode);
        return iMode;
    }

    // Use onBackPressed logic for button navigation
    @NonNull
    private OnBackPressedCallback getOnBackPressedCallback()
    {
        final Handler m1Handler = new Handler(Looper.getMainLooper());
        final Runnable r1 = () -> doubleBackToExitPressedTwice = false;

        return new OnBackPressedCallback(true)
        {
            @Override
            public void handleOnBackPressed()
            {
                if (doubleBackToExitPressedTwice)
                {
                    m1Handler.removeCallbacks(r1);
                    // Clear last locality in temp_loc of table TEMP, otherwise the old
                    //   locality is shown in the 1. count of a new started tour
                    clear_loc();
                    finish();
                    remove();
                }
                else
                {
                    doubleBackToExitPressedTwice = true;
                    showSnackbarBlue(getString(R.string.back_twice));
                    m1Handler.postDelayed(r1, 1500);
                }
            }
        };
    }

    // Check initial external storage permission and set 'storagePermGranted'
    private void isStoragePermGranted()
    {
        if (SDK_INT >= Build.VERSION_CODES.R) // Android >= 11
        {
            // check permission MANAGE_EXTERNAL_STORAGE for Android >= 11
            storagePermGranted = Environment.isExternalStorageManager();
            if (MyDebug.DLOG) Log.i(TAG, "291, ManageStoragePermission: " + storagePermGranted);
        }
        else
        {
            storagePermGranted = ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
            if (MyDebug.DLOG) Log.i(TAG, "297, ExtStoragePermission: " + storagePermGranted);
        }
    }

    // Check initial fine location permission
    private void isFineLocationPermGranted()
    {
        fineLocationPermGranted = ContextCompat.checkSelfPermission(this,
            Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    @SuppressLint({"SourceLockedOrientationActivity", "ApplySharedPref"})
    @Override
    protected void onResume()
    {
        super.onResume();

        if (MyDebug.DLOG) Log.i(TAG, "314, onResume");

        prefs = TourCountApplication.getPrefs();
        prefs.registerOnSharedPreferenceChangeListener(this);
        SharedPreferences.Editor editor = prefs.edit();
        outPref = prefs.getString("pref_sort_output", "names"); // sort mode csv-export

        isStoragePermGranted(); // set storagePermGranted from self permission

        headDataSource.open();
        sectionDataSource.open();
        countDataSource.open();
        individualsDataSource.open();

        // Set tour name as title
        if (MyDebug.DLOG) Log.i(TAG, "329, onResume, get section");
        section = sectionDataSource.getSection();
        tourName = section.name;
        if (MyDebug.DLOG) Log.i(TAG, "332, tourName: " + tourName);
        try
        {
            Objects.requireNonNull(getSupportActionBar()).setTitle(tourName);
        } catch (NullPointerException e)
        {
            // nothing
        }

        // 1. Part of location permissions handling:
        //   Set flag fineLocationPermGranted from self permissions
        // Store flag 'hasAskedBackground = true' in SharedPreferences
        isFineLocationPermGranted();
        if (MyDebug.DLOG) Log.i(TAG, "345, onCreate, fineLocationPermGranted: "
            + fineLocationPermGranted);

        // If not yet location permission is granted prepare and query for them
        if (storagePermGranted && !fineLocationPermGranted)
        {
            // Reset background location permission status in case it was set previously
            editor.putBoolean("has_asked_background", false);
            editor.commit();

            // Get flag 'has_asked_foreground'
            boolean hasAskedForegroundLocation = prefs.getBoolean("has_asked_foreground", false);

            if (!hasAskedForegroundLocation)
            {
                // Query foreground location permission first
                // Ask necessary fine location permission after info in Snackbar
                showSnackbarForegroundLocationPermission(getString(R.string.dialog_fine_location_hint));

                editor.putBoolean("has_asked_foreground", true);
                editor.commit();
            }
        }

        // Get location self permission state
        isFineLocationPermGranted(); // set fineLocationPermGranted from self permission
        if (MyDebug.DLOG) Log.i(TAG, "371, onResume, fineLocationPermGranted: "
            + fineLocationPermGranted);

        // Get flag 'has_asked_background'
        boolean hasAskedBackgroundLocation = prefs.getBoolean("has_asked_background", false);
        if (MyDebug.DLOG) Log.i(TAG, "376, hasAskedBackgroundLocation: "
            + hasAskedBackgroundLocation);

        // Get background location with permissions check only once and if storage and fine location
        //   permissions are granted
        if (storagePermGranted && fineLocationPermGranted && !hasAskedBackgroundLocation
            && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
        {
            // Ask optional background location permission with info in Snackbar
            showSnackbarBackgroundLocationPermission(getString(R.string.dialog_background_loc_hint));

            // Store flag 'hasAskedBackground = true' in SharedPreferences
            editor.putBoolean("has_asked_background", true);
            editor.commit();
        }
        locationDispatcherMode = 1;
        locationDispatcher();
    }
    // End of onResume()

    // Part of permission handling
    public void locationDispatcher()
    {
        if (fineLocationPermGranted) // current location permission state granted
        {
            // Handle action here
            if (MyDebug.DLOG) Log.i(TAG, "402, locationDispatcher, fineLocationPermGranted: true");
            switch (locationDispatcherMode)
            {
                case 1 ->
                {
                    // Get location data
                    getLoc();
                }
                case 2 ->
                {
                    // Stop location service
                    if (locServiceOn)
                    {
                        locationService.stopListener();
                        Intent sIntent = new Intent(this, LocationService.class);
                        stopService(sIntent);
                        locServiceOn = false;
                    }
                }
            }
        }
    }

    // Get the location data
    public void getLoc()
    {
        locationService = new LocationService(this);
        Intent sIntent = new Intent(this, LocationService.class);
        startService(sIntent);
        locServiceOn = true;

        if (locationService.canGetLocation())
        {
            longitude = locationService.getLongitude();
            latitude = locationService.getLatitude();
            height = locationService.getAltitude();
            if (height != 0)
                height = correctHeight(latitude, longitude, height);
            uncertainty = locationService.getAccuracy();
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
        MenuCompat.setGroupDividerEnabled(menu, true); // Show dividers in menu
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu)
    {
        // Grey out menu item 'Import Species List' if there is no
        //   directory /storage/emulated/0/Documents/TransektCount
        File path;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) // Android 10+
        {
            path = Environment.getExternalStorageDirectory();
            path = new File(path + "/Documents/TransektCount");
        }
        else
        {
            path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
            path = new File(path + "/TransektCount");
        }

        MenuItem item = menu.findItem(R.id.importSpeciesListMenu);
        if (path.exists() && path.isDirectory())
        {
            item.setEnabled(true);
            Objects.requireNonNull(item.getIcon()).setAlpha(255);
        }
        else
        {
            item.setEnabled(false);
            Objects.requireNonNull(item.getIcon()).setAlpha(130);
        }
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
            if (storagePermGranted)
            {
                exportDb();
            }
            else
            {
                showSnackbarRed(getString(R.string.storage_perm_cancel));
            }
            return true;
        }
        else if (id == R.id.exportCSVMenu)
        {
            if (storagePermGranted)
            {
                exportDb2CSV();
            }
            else
            {
                showSnackbarRed(getString(R.string.storage_perm_cancel));
            }
            return true;
        }
        else if (id == R.id.exportBasisMenu)
        {
            if (storagePermGranted)
            {
                exportBasisDb();
            }
            else
            {
                PermissionsStorageDialogFragment.newInstance().show(getSupportFragmentManager(),
                    PermissionsStorageDialogFragment.class.getName());
                if (storagePermGranted)
                {
                    exportBasisDb();
                }
                else
                {
                    showSnackbarRed(getString(R.string.storage_perm_cancel));
                }
            }
            return true;
        }
        else if (id == R.id.exportSpeciesListMenu)
        {
            exportSpeciesList();
            return true;
        }
        else if (id == R.id.importBasisMenu)
        {
            importBasisDb();
            return true;
        }
        else if (id == R.id.importFileMenu)
        {
            headDataSource.close();
            individualsDataSource.close();
            countDataSource.close();
            sectionDataSource.close();

            importDBFile();

            headDataSource.open();
            sectionDataSource.open();
            countDataSource.open();
            individualsDataSource.open();

            // List tour name as title
            section = sectionDataSource.getSection();
            try
            {
                Objects.requireNonNull(getSupportActionBar()).setTitle(section.name);
            } catch (NullPointerException e)
            {
                // nothing
            }
            return true;
        }
        else if (id == R.id.resetDBMenu)
        {
            resetToBasisDb();
            return true;
        }
        else if (id == R.id.importSpeciesListMenu)
        {
            importSpeciesList();
            return true;
        }
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
            intent = new Intent(WelcomeActivity.this, CountingActivity.class);
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
                startActivity(new Intent(getApplicationContext(), ShowResultsActivity
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
    }

    public void onPause()
    {
        super.onPause();

        if (MyDebug.DLOG) Log.i(TAG, "672, onPause");

        headDataSource.close();
        individualsDataSource.close();
        countDataSource.close();
        sectionDataSource.close();

        locationDispatcherMode = 2;
        locationDispatcher();

        prefs.unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onStop()
    {
        super.onStop();

        if (MyDebug.DLOG) Log.i(TAG, "690, onStop");
        // Stop location service with permissions check

        baseLayout.invalidate();
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();

        if (MyDebug.DLOG) Log.i(TAG, "701, onDestroy");
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
        // a Snackbar here comes incomplete
        Toast.makeText(getApplicationContext(), getString(R.string.wait), Toast.LENGTH_SHORT).show();

        // Trick: Pause for 100 msec to show toast
        mHandler.postDelayed(() ->
            startActivity(new Intent(getApplicationContext(), ShowResultsActivity.class)
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

    /***********************************************************************************************
     * The next three functions below are for importing data files.
     * They've been put here because no database should be open at this point.
     **********************************************************************************************/
    // Import the basic DB
    private void importBasisDb()
    {
        // inFile <- /storage/emulated/0/Documents/TourCount/tourcount0.db
        File path;
        if (SDK_INT >= Build.VERSION_CODES.Q) // Android 10+
        {
            path = Environment.getExternalStorageDirectory();
            path = new File(path + "/Documents/TourCount");
        }
        else
        {
            path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
            path = new File(path + "/TourCount");
        }
        inFile = new File(path, "/tourcount0.db");

        // outFile -> /data/data/com.wmstein.tourcount/databases/tourcount.db
        String destPath = getApplicationContext().getFilesDir().getPath();
        destPath = destPath.substring(0, destPath.lastIndexOf("/")) + "/databases/tourcount.db";
        outFile = new File(destPath);
        if (!(inFile.exists()))
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
                copy(inFile, outFile);
                showSnackbar(getString(R.string.importWin));

                // Save values for initial count-id and itemposition
                SharedPreferences.Editor editor = prefs.edit();
                editor.putInt("count_id", 1);
                editor.putInt("item_Position", 0);
                editor.apply();

                Objects.requireNonNull(getSupportActionBar()).setTitle("");
            } catch (IOException e)
            {
                if (MyDebug.DLOG) Log.e(TAG, "795, Failed to import database");
                showSnackbarRed(getString(R.string.importFail));
            }
        }).setNegativeButton(R.string.cancelButton, (dialog, id) -> dialog.cancel());
        alert = builder.create();
        alert.show();
    }

    /**********************************************************************************************/
    // Copy file block-wise
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
    // End of importBasisDb() 

    /************************************************************/
    // Choose a tourcount db-file to load and set it to tourcount.db
    private void importDBFile()
    {
        if (MyDebug.DLOG) Log.d(TAG, "826, importDBFile");

        String fileExtension = ".db";
        String fileName = "tourcount";

        Intent intent;
        intent = new Intent(this, AdvFileChooser.class);
        intent.putExtra("filterFileExtension", fileExtension);
        intent.putExtra("filterFileName", fileName);
        myActivityResultLauncher.launch(intent);
    }

    // ActivityResultLauncher processes the result of AdvFileChooser
    final ActivityResultLauncher<Intent> myActivityResultLauncher = registerForActivityResult(
        new ActivityResultContracts.StartActivityForResult(),
        new ActivityResultCallback<>()
        {
            @SuppressLint("ApplySharedPref")
            @Override
            public void onActivityResult(ActivityResult result)
            {
                String selectedFile;
                inFile = null;
                if (result.getResultCode() == Activity.RESULT_OK)
                {
                    Intent data = result.getData();
                    if (data != null)
                    {
                        selectedFile = data.getStringExtra("fileSelected");
                        if (MyDebug.DLOG) Log.d(TAG, "855, File selected: " + selectedFile);

                        if (selectedFile != null)
                            inFile = new File(selectedFile);
                        else
                            inFile = null;
                    }
                }

                // outFile = "/data/data/com.wmstein.tourcount/databases/tourcount.db"
                String destPath = getApplicationContext().getFilesDir().getPath();
                destPath = destPath.substring(0, destPath.lastIndexOf("/"))
                    + "/databases/tourcount.db";
                outFile = new File(destPath);

                if (inFile != null)
                {
                    AlertDialog.Builder builder = new AlertDialog.Builder(WelcomeActivity.this);
                    builder.setIcon(android.R.drawable.ic_dialog_alert);
                    builder.setMessage(R.string.confirmDBImport);
                    builder.setCancelable(false);
                    builder.setPositiveButton(R.string.importButton, (dialog, id) ->
                    {
                        try
                        {
                            copy(inFile, outFile);

                            // save values for initial countId and itemposition
                            SharedPreferences.Editor editor = prefs.edit();
                            editor.putInt("count_id", 1);
                            editor.putInt("item_Position", 0);
                            editor.commit();

                            // Trick: Pause for 100 msec to accept the imported DB
                            mHandler.postDelayed(() ->
                                showSnackbar(getString(R.string.importWin)), 100);
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
    // End of importDBFile()

    /**********************************************************************************************/
    // Import species list from TransektCount file species_YYYY-MM-DD_hhmmss.csv
    private void importSpeciesList()
    {
        // Select exported TransektCount species list file
        String fileExtension = ".csv";
        String fileName = "species";

        Intent intent;
        intent = new Intent(this, AdvFileChooser.class);
        intent.putExtra("filterFileExtension", fileExtension);
        intent.putExtra("filterFileName", fileName);
        listActivityResultLauncher.launch(intent);
    }

    // ActivityResultLauncher processes the result of AdvFileChooser
    final ActivityResultLauncher<Intent> listActivityResultLauncher = registerForActivityResult(
        new ActivityResultContracts.StartActivityForResult(),
        new ActivityResultCallback<>()
        {
            @SuppressLint("ApplySharedPref")
            @Override
            public void onActivityResult(ActivityResult result)
            {
                String selectedFile;
                inFile = null;
                if (result.getResultCode() == Activity.RESULT_OK)
                {
                    Intent data = result.getData();
                    if (data != null)
                    {
                        selectedFile = data.getStringExtra("fileSelected");
                        if (MyDebug.DLOG) Log.d(TAG, "936, File selected: " + selectedFile);

                        if (selectedFile != null)
                            inFile = new File(selectedFile);
                        else
                            inFile = null;
                    }
                }
                if (inFile != null)
                {
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

    private void readCSV(File inFile)
    {
        try
        {
            // Read exported TransektCount species list and write items to table counts
            Toast.makeText(getApplicationContext(), getString(R.string.waitImport), Toast.LENGTH_SHORT).show();
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
            showSnackbar(getString(R.string.importList));
        } catch (Exception e)
        {
            showSnackbarRed(getString(R.string.importListFail));
        }
    }
    // End of importSpeciesList()

    /***********************************************************************************************
     * The next four functions below are for exporting data files.
     * They've been put here because no database should be open at this point.
     **********************************************************************************************/
    // Exports Basis DB to Documents/TourCount/tourcount0.db
    private void exportBasisDb()
    {
        // inFile <- /data/data/com.wmstein.tourcount/databases/tourcount.db
        String inPath = getApplicationContext().getFilesDir().getPath();
        inPath = inPath.substring(0, inPath.lastIndexOf("/")) + "/databases/tourcount.db";
        inFile = new File(inPath);

        // tmpFile -> /data/data/com.wmstein.tourcount/files/tourcount_tmp.db
        String tmpPath = getApplicationContext().getFilesDir().getPath();
        tmpPath = tmpPath.substring(0, tmpPath.lastIndexOf("/")) + "/files/tourcount_tmp.db";
        File tmpFile = new File(tmpPath);

        // New data directory
        // outFile in Public Directory Documents/TourCount/
        // distinguish versions (as getExternalStoragePublicDirectory is deprecated in Q, Android 10)
        File path;
        if (SDK_INT >= Build.VERSION_CODES.Q) // Android 10+
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
        outFile = new File(path, "/tourcount0.db");

        if (Environment.MEDIA_MOUNTED.equals(sState))
        {
            // We can read and write the media
            mExternalStorageAvailable = mExternalStorageWriteable = true;
        }
        else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(sState))
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
            if (MyDebug.DLOG) Log.e(TAG, "1050, No sdcard access");
            showSnackbarRed(getString(R.string.noCard));
        }
        else
        {
            // Export the basic db
            try
            {
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
                if (d0)
                {
                    showSnackbar(getString(R.string.saveDB));
                }
            } catch (IOException e)
            {
                if (MyDebug.DLOG) Log.e(TAG, "1078, Failed to export Basic DB");
                showSnackbarRed(getString(R.string.saveFail));
            }
        }
    }
    // End of exportBasisDb()

    @SuppressLint({"SdCardPath", "LongLogTag"})
    private void exportDb()
    {
        // New data directory:
        //   outFile -> Public Directory Documents/TourCount/
        File path;
        if (SDK_INT >= Build.VERSION_CODES.Q) // Android 10+
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

        // outFile -> /storage/emulated/0/Documents/TourCount/tourcount_yyyy-MM-dd_HHmmss.db
        if (Objects.equals(tourName, ""))
            outFile = new File(path, "/tourcount_" + getcurDate() + ".db");
        else
            outFile = new File(path, "/tourcount_" + tourName + "_" + getcurDate() + ".db");

        // inFile <- /data/data/com.wmstein.tourcount/databases/tourcount.db
        String inPath = getApplicationContext().getFilesDir().getPath();
        inPath = inPath.substring(0, inPath.lastIndexOf("/"))
            + "/databases/tourcount.db";
        inFile = new File(inPath);

        if (Environment.MEDIA_MOUNTED.equals(sState))
        {
            // We can read and write the media
            mExternalStorageAvailable = mExternalStorageWriteable = true;
        }
        else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(sState))
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
            if (MyDebug.DLOG) Log.e(TAG, "1137, No sdcard access");
            showSnackbarRed(getString(R.string.noCard));
        }
        else
        {
            // Export the db
            try
            {
                copy(inFile, outFile);
                showSnackbar(getString(R.string.saveDB));
            } catch (IOException e)
            {
                showSnackbarRed(getString(R.string.saveFail));
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
    private void exportDb2CSV()
    {
        // outFile -> /storage/emulated/0/Documents/TourCount/tourcount_yyyy-MM-dd_HHmmss.csv
        //   and distinguish versions (as getExternalStoragePublicDirectory is deprecated in Q, Android 10)
        File path;
        if (SDK_INT >= Build.VERSION_CODES.Q) // Android 10+
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
            outFile = new File(path, "/tourcount_" + getcurDate() + ".csv");
        else
            outFile = new File(path, "/tourcount_" + tourName + "_" + getcurDate() + ".csv");

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

        if (Environment.MEDIA_MOUNTED.equals(sState))
        {
            // We can read and write the media
            mExternalStorageAvailable = mExternalStorageWriteable = true;
        }
        else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(sState))
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
            if (MyDebug.DLOG) Log.e(TAG, "1223, No sdcard access");
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
                        getString(R.string.bstate),
                        getString(R.string.plz),
                        getString(R.string.city),
                        getString(R.string.place),
                        getString(R.string.slocality),
                        getString(R.string.zlnotes)
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
                        getString(R.string.status123) + ":",
                        getString(R.string.bema) + ":"
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
                            spstate0,                   // status
                            curCSVInd.getString(13)  // indiv. notes
                        };
                    csvWrite.writeNext(arrIndividual);

                    if (longi != 0) // Has coordinates
                    {
                        if (MyDebug.DLOG) Log.d(TAG, "1688 longi " + longi);
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
                if (MyDebug.DLOG) Log.e(TAG, "1768, Failed to export csv file");
                showSnackbarRed(getString(R.string.saveFail));
            }
        }
    }
    // End of exportDb2CSV()

    /**********************************************************************************************/
    // Export current species list to species_YYYY-MM-DD_hhmmss.csv
    private void exportSpeciesList()
    {
        // outFile -> /storage/emulated/0/Documents/TourCount/species_yyyy-MM-dd_HHmmss.csv
        File path;
        if (SDK_INT >= Build.VERSION_CODES.Q) // Android 10+
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

        outFile = new File(path, "/species_" + getcurDate() + ".csv");

        // Check if we can write the media
        mExternalStorageWriteable = Environment.MEDIA_MOUNTED.equals(sState);

        if (!mExternalStorageWriteable)
        {
            showSnackbarRed(getString(R.string.noCard));
        }
        else
        {
            // Export species list into species_yyyy-MM-dd_HHmmss.csv
            dbHandler = new DbHelper(this);
            database = dbHandler.getWritableDatabase();

            String[] codeArray;
            String[] nameArray;
            String[] nameArrayL;

            codeArray = countDataSource.getAllStringsSrtCode("code");
            nameArray = countDataSource.getAllStringsSrtCode("name");
            nameArrayL = countDataSource.getAllStringsSrtCode("name_g");

            int specNum = codeArray.length;

            try
            {
                CSVWriter csvWrite = new CSVWriter(new FileWriter(outFile));

                int i = 0;
                while (i < specNum)
                {
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
                showSnackbar(getString(R.string.saveList));

            } catch (Exception e)
            {
                showSnackbarRed(getString(R.string.saveFailList));
            }
        }
    }
    // End of exportSpeciesList()

    /**********************************************************************************************/
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

            dbHandler.close();
        } catch (Exception e)
        {
            if (MyDebug.DLOG) Log.e(TAG, "1931, Failed to reset DB");
            showSnackbarRed(getString(R.string.resetFail));
            dbHandler.close();
            r_ok = false;
        }
        return r_ok;
    }
    // End of resetToBasisDb()

    // Clear DB for import of external species list
    private void clearDBforImport()
    {
        dbHandler = new DbHelper(this);
        database = dbHandler.getWritableDatabase();

        String sql = "DELETE FROM " + DbHelper.COUNT_TABLE;
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

        dbHandler.close();

        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt("item_Position", 0);
        editor.putInt("count_id", 1);
        editor.apply();
    }

    // Green message to point something out
    private void showSnackbar(String str)
    {
        baseLayout = findViewById(R.id.baseLayout);
        Snackbar sB = Snackbar.make(baseLayout, str, Snackbar.LENGTH_LONG);
        TextView tv = sB.getView().findViewById(R.id.snackbar_text);
        tv.setTextColor(Color.GREEN);
        tv.setGravity(Gravity.CENTER);
        sB.show();
    }

    // Red warning message
    private void showSnackbarRed(String str)
    {
        baseLayout = findViewById(R.id.baseLayout);
        Snackbar sB = Snackbar.make(baseLayout, str, Snackbar.LENGTH_LONG);
        TextView tv = sB.getView().findViewById(R.id.snackbar_text);
        tv.setTypeface(tv.getTypeface(), Typeface.BOLD);
        tv.setTextColor(Color.RED);
        tv.setGravity(Gravity.CENTER);
        sB.show();
    }

    // Cyan message to do something
    private void showSnackbarBlue(String str)
    {
        baseLayout = findViewById(R.id.baseLayout);
        Snackbar sB = Snackbar.make(baseLayout, str, Snackbar.LENGTH_LONG);
        TextView tv = sB.getView().findViewById(R.id.snackbar_text);
        tv.setTypeface(tv.getTypeface(), Typeface.BOLD);
        tv.setTextColor(Color.CYAN);
        tv.setGravity(Gravity.CENTER);
        sB.show();
    }

    // Blue storage permission message with button before granting dialog
    private void showSnackbarPermission(String str)
    {
        baseLayout = findViewById(R.id.baseLayout);
        Snackbar sB = Snackbar.make(baseLayout, str, Snackbar.LENGTH_INDEFINITE);
        TextView tv = sB.getView().findViewById(R.id.snackbar_text);
        tv.setTypeface(tv.getTypeface(), Typeface.BOLD);
        tv.setTextColor(Color.CYAN);
        tv.setMaxLines(3);
        tv.setGravity(Gravity.CENTER);
        sB.setAction("Ok", View ->
        {
            sB.dismiss();
            PermissionsStorageDialogFragment.newInstance().show(getSupportFragmentManager(),
                PermissionsStorageDialogFragment.class.getName());
        });
        sB.show();
    }

    // Blue foreground location permission message with button before granting dialog
    private void showSnackbarForegroundLocationPermission(String str)
    {
        baseLayout = findViewById(R.id.baseLayout);
        Snackbar sB = Snackbar.make(baseLayout, str, Snackbar.LENGTH_INDEFINITE);
        TextView tv = sB.getView().findViewById(R.id.snackbar_text);
        tv.setTypeface(tv.getTypeface(), Typeface.BOLD);
        tv.setTextColor(Color.CYAN);
        tv.setMaxLines(7);
        tv.setGravity(Gravity.CENTER);
        sB.setAction("Ok", View ->
        {
            sB.dismiss();
            PermissionsForegroundDialogFragment.newInstance().show(getSupportFragmentManager(),
                PermissionsForegroundDialogFragment.class.getName());
        });
        sB.show();
    }

    // Blue background location permission message with button before granting dialog
    private void showSnackbarBackgroundLocationPermission(String str)
    {
        baseLayout = findViewById(R.id.baseLayout);
        Snackbar sB = Snackbar.make(baseLayout, str, Snackbar.LENGTH_INDEFINITE);
        TextView tv = sB.getView().findViewById(R.id.snackbar_text);
        tv.setTypeface(tv.getTypeface(), Typeface.BOLD);
        tv.setTextColor(Color.CYAN);
        tv.setMaxLines(5);
        tv.setGravity(Gravity.CENTER);
        sB.setAction("Ok", View ->
        {
            sB.dismiss();
            PermissionsBackgroundDialogFragment.newInstance().show(getSupportFragmentManager(),
                PermissionsBackgroundDialogFragment.class.getName());
        });
        sB.show();
    }

}
