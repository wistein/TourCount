package com.wmstein.tourcount;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.StrictMode;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

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
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import sheetrock.panda.changelog.ChangeLog;
import sheetrock.panda.changelog.ViewHelp;

import static com.wmstein.tourcount.TourCountApplication.getPrefs;
import static java.lang.Math.sqrt;

/**********************************************************************
 * WelcomeActivity provides the starting page with menu and buttons for
 * import/export/help/info methods and starts
 * EditMetaActivity, Counting(L)Activity and ListSpeciesActivity.
 * It uses further LocationService and PermissionDialogFragment.
 *
 * Based on BeeCount's WelcomeActivity.java by milo on 05/05/2014.
 * Changes and additions for TourCount by wmstein since 2016-04-18,
 * last modification on 2019-07-31
 */
public class WelcomeActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener, PermissionsDialogFragment.PermissionsGrantedCallback
{
    private static final String TAG = "TourCountWelcomeAct";
    private TourCountApplication tourCount;

    private static final int FILE_CHOOSER = 11;
    LocationService locationService;
    
    // Permission dispatcher mode modePerm: 
    //  1 = use location service
    //  2 = end location service
    //  3 = export DB
    //  4 = export DB -> CSV
    //  5 = export basic DB
    //  6 = import DB
    //  7 = import basic DB
    private int modePerm;
    
    // permLocGiven contains initial location permission state that
    // controls if location listener has to be stopped after permission changed: 
    // Stop listener if permission was denied after listener start.
    // Don't stop listener if permission was allowed later and listener has not been started
    private boolean permLocGiven;
     
    private ChangeLog cl;
    private ViewHelp vh;
    public boolean doubleBackToExitPressedOnce;

    // Location info handling
    private double latitude, longitude, height, uncertainty;

    // import/export stuff
    private File infile;
    private File outfile;
    private boolean mExternalStorageAvailable = false;
    private boolean mExternalStorageWriteable = false;
    private String state = Environment.getExternalStorageState();
    private AlertDialog alert;
    private final Handler mHandler = new Handler();

    // preferences
    private SharedPreferences prefs;
    private String sortPref;
    private boolean screenOrientL; // option for screen orientation
    private boolean metaPref;      // option for reverse geocoding
    private String emailString = ""; // mail address for OSM query

    // db handling
    private SQLiteDatabase database;
    private DbHelper dbHandler;
    private SectionDataSource sectionDataSource;
    private CountDataSource countDataSource;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        tourCount = (TourCountApplication) getApplication();
        prefs = getPrefs();
        prefs.registerOnSharedPreferenceChangeListener(this);
        sortPref = prefs.getString("pref_sort_sp", "none"); // sort mode species list
        screenOrientL = prefs.getBoolean("screen_Orientation", false);
        metaPref = prefs.getBoolean("pref_metadata", false);   // use Reverse Geocoding
        emailString = prefs.getString("email_String", "");     // for reliable query of Nominatim service

        setContentView(R.layout.activity_welcome);

        if (screenOrientL)
        {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }
        else
        {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }

        ScrollView baseLayout = findViewById(R.id.baseLayout);
        baseLayout.setBackground(tourCount.getBackground());

        // check initial location permission state
        permLocGiven = isPermLocGranted();
        if (MyDebug.LOG)
            Toast.makeText(this, "onCreate permLocGiven = " + permLocGiven, Toast.LENGTH_SHORT).show();
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("permLoc_Given", permLocGiven);
        editor.apply();

        // Get location with permissions check
        modePerm = 1; // get location
        permissionCaptureFragment();

        // List tour name as title
        Section section;
        String sname;
        try
        {
            sectionDataSource = new SectionDataSource(this);
            sectionDataSource.open();
            section = sectionDataSource.getSection();
            sname = section.name;
            sectionDataSource.close();
        } catch (SQLiteException e)
        {
            sname = getString(R.string.errorDb);
//            Toast.makeText(this, R.string.corruptDb, Toast.LENGTH_LONG).show();
            showSnackbarRed(getString(R.string.corruptDb));
        }

        try
        {
            getSupportActionBar().setTitle(sname);
        } catch (NullPointerException e)
        {
            // nothing
        }

        cl = new ChangeLog(this);
        vh = new ViewHelp(this);
        // Show changelog for new version
        if (cl.firstRun())
            cl.getLogDialog().show(); 
    } // end of onCreate

    // check initial location permission
    private boolean isPermLocGranted()
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
        {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        }
        else
        {
            return true;
        }
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        if (screenOrientL)
        {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }
        else
        {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }

        prefs = getPrefs();
        prefs.registerOnSharedPreferenceChangeListener(this);
        permLocGiven = prefs.getBoolean("permLoc_Given", false);
        if (MyDebug.LOG)
            Toast.makeText(this, "onResume permLocGiven = " + permLocGiven, Toast.LENGTH_SHORT).show();

        // List tour name as title
        Section section;
        String sname;
        try
        {
            sectionDataSource = new SectionDataSource(this);
            sectionDataSource.open();
            section = sectionDataSource.getSection();
            sname = section.name;
        } catch (SQLiteException e)
        {
            sname = getString(R.string.errorDb);
            sectionDataSource.close();
        }

        try
        {
            getSupportActionBar().setTitle(sname);
        } catch (NullPointerException e)
        {
            // nothing
        }

    } // end of onResume

    // Part of permission handling
    @Override
    public void permissionCaptureFragment()
    {
        if (isLocPermissionGranted()) // current location permission state granted
        {
            // handle action here
            if (MyDebug.LOG)
                Toast.makeText(this, "Fragment permLocGiven = " + permLocGiven, Toast.LENGTH_SHORT).show();

            switch (modePerm)
            {
            case 1: // get location
                if (permLocGiven) // location permission state after start
                    getLoc();
                break;

            case 2: // stop location service
                if (permLocGiven)
                    locationService.stopListener();
                break;
            }
        }
        else
        {
            if (modePerm == 1)
                PermissionsDialogFragment.newInstance().show(getSupportFragmentManager(), PermissionsDialogFragment.class.getName());
        }

        if (isStoPermissionGranted()) // current storage permission state granted
        {
            switch (modePerm)
            {
            case 3: // write DB
                doExportDB();
                break;

            case 4: // write DB2CSV
                doExportDb2CSV();
                break;

            case 5: // write basic DB
                doExportBasisDb();
                break;

            case 6: // read DB
                doImportDB();
                break;

            case 7: // read basic DB
                doImportBasisDB();
                break;
            }
        }
        else
        {
            if (modePerm != 2)
                PermissionsDialogFragment.newInstance().show(getSupportFragmentManager(), PermissionsDialogFragment.class.getName());
        }
    }

    // if API level > 23 test for permissions granted
    private boolean isLocPermissionGranted()
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
        {
            return (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED);
        }
        else
            return true;
    }

    // if API level > 23 test for permissions granted
    private boolean isStoPermissionGranted()
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
        {
            return (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
        }
        return true;
    }

    // get the location data
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

        // get reverse geocoding
        if (locationService.canGetLocation() && metaPref && (latitude != 0 || longitude != 0))
        {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);

            runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
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
                }
            });
        }
    }

        // Correct height with geoid offset from EarthGravitationalModel
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
    
    // Date for filename of Export-DB
    private String getcurDate()
    {
        Date date = new Date();
        @SuppressLint("SimpleDateFormat")
        DateFormat dform = new SimpleDateFormat("yyyy-MM-dd_HHmmss");
        return dform.format(date);
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
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings)
        {
            startActivity(new Intent(this, SettingsActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
            return true;
        }
        else if (id == R.id.exportMenu)
        {
            exportDb();
            return true;
        }
        else if (id == R.id.exportCSVMenu)
        {
            exportDb2CSV();
            return true;
        }
        else if (id == R.id.exportBasisMenu)
        {
            exportBasisDb();
            return true;
        }
        else if (id == R.id.importBasisMenu)
        {
            importBasisDb();
            return true;
        }
        else if (id == R.id.loadFileMenu)
        {
            loadFile();
            return true;
        }
        else if (id == R.id.resetDBMenu)
        {
            resetToBasisDb();
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
        else if (id == R.id.viewCounts)
        {
            if (screenOrientL)
            {
                Intent intent = new Intent(WelcomeActivity.this, CountingLActivity.class);
                intent.putExtra("Latitude", latitude);
                intent.putExtra("Longitude", longitude);
                intent.putExtra("Height", height);
                intent.putExtra("Uncert", uncertainty);
                startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
            }
            else
            {
                Intent intent = new Intent(WelcomeActivity.this, CountingActivity.class);
                intent.putExtra("Latitude", latitude);
                intent.putExtra("Longitude", longitude);
                intent.putExtra("Height", height);
                intent.putExtra("Uncert", uncertainty);
                startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
            }
        }
        else if (id == R.id.editMeta)
        {
            startActivity(new Intent(this, EditMetaActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
            return true;
        }
        else if (id == R.id.viewSpecies)
        {
            Toast.makeText(getApplicationContext(), getString(R.string.wait), Toast.LENGTH_SHORT).show(); // a Snackbar here comes incomplete

            // pause for 100 msec to show toast
            mHandler.postDelayed(new Runnable()
            {
                public void run()
                {
                    startActivity(new Intent(getApplicationContext(), ListSpeciesActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
                }
            }, 100);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // Handle button click "Counting" here 
    public void viewCounts(View view)
    {
        if (screenOrientL)
        {
            Intent intent = new Intent(WelcomeActivity.this, CountingLActivity.class);
            intent.putExtra("Latitude", latitude);
            intent.putExtra("Longitude", longitude);
            intent.putExtra("Height", height);
            intent.putExtra("Uncert", uncertainty);
            startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
        }
        else
        {
            Intent intent = new Intent(WelcomeActivity.this, CountingActivity.class);
            intent.putExtra("Latitude", latitude);
            intent.putExtra("Longitude", longitude);
            intent.putExtra("Height", height);
            intent.putExtra("Uncert", uncertainty);
            startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
        }
    }

    // Handle button click "Prepare Inspection" here 
    public void editMeta(View view)
    {
        startActivity(new Intent(this, EditMetaActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
    }

    // Handle button click "Show Results" here 
    public void viewSpecies(View view)
    {
        Toast.makeText(getApplicationContext(), getString(R.string.wait), Toast.LENGTH_SHORT).show(); // a Snackbar here comes incomplete

        // pause for 100 msec to show toast
        mHandler.postDelayed(new Runnable()
        {
            public void run()
            {
                startActivity(new Intent(getApplicationContext(), ListSpeciesActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
            }
        }, 100);
    }

    public void onSharedPreferenceChanged(SharedPreferences prefs, String key)
    {
        ScrollView baseLayout = findViewById(R.id.baseLayout);
        baseLayout.setBackground(null);
        baseLayout.setBackground(tourCount.setBackground());
        sortPref = prefs.getString("pref_sort_sp", "none");
        screenOrientL = prefs.getBoolean("screen_Orientation", false);
        metaPref = prefs.getBoolean("pref_metadata", false);   // use Reverse Geocoding
        emailString = prefs.getString("email_String", "");     // for reliable query of Nominatim service
        permLocGiven = prefs.getBoolean("permLoc_Given", false);
    }

    public void onPause()
    {
        super.onPause();

        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("permLoc_Given", permLocGiven);
        editor.apply();

        sectionDataSource.close();
        countDataSource.close();
    }

    @Override
    public void onBackPressed()
    {
        if (doubleBackToExitPressedOnce)
        {
            super.onBackPressed(); // stops app
            return;
        }

        this.doubleBackToExitPressedOnce = true;
        Toast.makeText(this, R.string.back_twice, Toast.LENGTH_SHORT).show();

        new Handler().postDelayed(new Runnable()
        {
            @Override
            public void run()
            {
                doubleBackToExitPressedOnce = false;
                // Clear last locality in temp
                clear_loc();
            }
        }, 1000);
    }

    public void onStop()
    {
        super.onStop();

        sectionDataSource.close();
        countDataSource.close();

        // Stop location service with permissions check
        modePerm = 2;
        permissionCaptureFragment();
    }

    public void onDestroy()
    {
        super.onDestroy();

        sectionDataSource.close();
        countDataSource.close();

        // Stop location service with permissions check
            modePerm = 2;
            permissionCaptureFragment();
    }

    /*************************************************************************
     * The six activities below are for exporting and importing the database.
     * They've been put here because no database should be open at this point.
     *************************************************************************/
    // Exports DB to SdCard/tourcount_yyyy-MM-dd_HHmmss.db
    // supplemented with date and time in filename by wmstein
    @SuppressLint({"SdCardPath", "LongLogTag"})
    private void exportDb()
    {
        // Export DB with permission check
        modePerm = 3;
        permissionCaptureFragment(); // calls doExportDB()
    }

    private void doExportDB()
    {
        // outfile -> /storage/emulated/0/tourcount_yyyy-MM-dd_HHmmss.db
        outfile = new File(Environment.getExternalStorageDirectory() + "/tourcount_" + getcurDate() + ".db");
        
        // infile <- /data/data/com.wmstein.tourcount/databases/tourcount.db
        String inPath = getApplicationContext().getFilesDir().getPath();
        inPath = inPath.substring(0, inPath.lastIndexOf("/")) + "/databases/tourcount.db";
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
            if (MyDebug.LOG)
                Log.e(TAG, "No sdcard access");
//            Toast.makeText(this, getString(R.string.noCard), Toast.LENGTH_LONG).show();
            showSnackbarRed(getString(R.string.noCard));
        }
        else
        {
            // export the db
            try
            {
                // export db
                copy(infile, outfile);
//                Toast.makeText(this, getString(R.string.saveWin), Toast.LENGTH_SHORT).show();
                showSnackbar(getString(R.string.saveWin));
            } catch (IOException e)
            {
                if (MyDebug.LOG)
                    Log.e(TAG, "Failed to copy database");
//                Toast.makeText(this, getString(R.string.saveFail), Toast.LENGTH_LONG).show();
                showSnackbarRed(getString(R.string.saveFail));
            }
        }
    }

    /***********************************************************************/
    // Exports DB to SdCard/tourcount_yyyy-MM-dd_HHmmss.csv
    // purged data set into appropriate table
    // Excel can import this csv file with Unicode UTF-8 filter
    // 15.05.2016, wm.stein
    @SuppressLint({"SdCardPath", "LongLogTag"})
    private void exportDb2CSV()
    {
        // Export DB -> CSV with permission check
        modePerm = 4;
        permissionCaptureFragment(); // calls doExportDb2CSV()
    }

    private void doExportDb2CSV()
    {
        // outfile -> /storage/emulated/0/tourcount_yyyy-MM-dd_HHmmss.csv
        outfile = new File(Environment.getExternalStorageDirectory() + "/tourcount_" + getcurDate() + ".csv");

        Section section;
        String sectName;
        String sectNotes;

        Head head;
        String country, inspecName;
        int temp, wind, clouds;
        String plz, city, place;
        String date, start_tm, end_tm;
        int spstate;
        double longi, lati, heigh, uncer;
        int frst, sum = 0;
        int summf = 0, summ = 0, sumf = 0, sump = 0, suml = 0, sume = 0;
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
            if (MyDebug.LOG)
                Log.d(TAG, "No sdcard access");
//            Toast.makeText(this, getString(R.string.noCard), Toast.LENGTH_LONG).show();
            showSnackbarRed(getString(R.string.noCard));
        }
        else
        {
            // export the purged count table to csv
            try
            {
                // open Head and Section tables for head and meta info
                HeadDataSource headDataSource = new HeadDataSource(this);
                headDataSource.open();
                sectionDataSource = new SectionDataSource(this);
                sectionDataSource.open();

                // export purged db as csv
                CSVWriter csvWrite = new CSVWriter(new FileWriter(outfile));

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
                        getString(R.string.zlist) + ":", //Count List:
                        sectName,                        //section name
                        "",
                        "",
                        getString(R.string.inspector) + ":",           //Inspector:
                        inspecName                       //inspector name
                    };
                csvWrite.writeNext(arrHead);

                // Empty row
                String[] arrEmpt = {};
                csvWrite.writeNext(arrEmpt);

                // set location headline
                String[] arrLocHead =
                    {
                        getString(R.string.country),
                        getString(R.string.plz),
                        getString(R.string.city),
                        getString(R.string.place),
                        getString(R.string.zlnotes)
                    };
                csvWrite.writeNext(arrLocHead);

                // set location dataline
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
                csvWrite.writeNext(arrEmpt);

                // set environment headline
                String[] arrEnvHead =
                    {
                        getString(R.string.temperature),
                        getString(R.string.wind),
                        getString(R.string.clouds),
                        getString(R.string.date),
                        getString(R.string.starttm),
                        getString(R.string.endtm)
                    };
                csvWrite.writeNext(arrEnvHead);

                // set environment data
                temp = section.temp;
                wind = section.wind;
                clouds = section.clouds;
                date = section.date;
                start_tm = section.start_tm;
                end_tm = section.end_tm;

                // write environment data
                String[] arrEnvironment =
                    {
                        String.valueOf(temp),
                        String.valueOf(wind),
                        String.valueOf(clouds),
                        date,
                        start_tm,
                        end_tm
                    };
                csvWrite.writeNext(arrEnvironment);

                // Empty row
                csvWrite.writeNext(arrEmpt);

                // write counts headline
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

                // write counts data
                dbHandler = new DbHelper(this);
                database = dbHandler.getWritableDatabase();

                Cursor curCSVCnt;

                // sort mode species list
                if ("codes".equals(sortPref))
                {
                    curCSVCnt = database.rawQuery("select * from " + DbHelper.COUNT_TABLE
                        + " WHERE " + " ("
                        + DbHelper.C_COUNT_F1I + " > 0 or " + DbHelper.C_COUNT_F2I + " > 0 or "
                        + DbHelper.C_COUNT_F3I + " > 0 or " + DbHelper.C_COUNT_PI + " > 0 or "
                        + DbHelper.C_COUNT_LI + " > 0 or " + DbHelper.C_COUNT_EI + " > 0)"
                        + " order by " + DbHelper.C_CODE, null, null);
                }
                else
                {
                    curCSVCnt = database.rawQuery("select * from " + DbHelper.COUNT_TABLE
                        + " WHERE " + " ("
                        + DbHelper.C_COUNT_F1I + " > 0 or " + DbHelper.C_COUNT_F2I + " > 0 or "
                        + DbHelper.C_COUNT_F3I + " > 0 or " + DbHelper.C_COUNT_PI + " > 0 or "
                        + DbHelper.C_COUNT_LI + " > 0 or " + DbHelper.C_COUNT_EI + " > 0)"
                        + " order by " + DbHelper.C_NAME, null, null);
                }

                // open Individuals table 
                IndividualsDataSource individualsDataSource = new IndividualsDataSource(this);
                individualsDataSource.open();

                // get the number of individuals with attributes
                int cnts; // individuals icount
                String strcnts;
                int cntsmf; // Imago male, female
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

                Cursor curCSVInd;
                while (curCSVCnt.moveToNext())
                {
                    String spname = curCSVCnt.getString(7); // species name

                    String slct = "SELECT * FROM " + DbHelper.INDIVIDUALS_TABLE
                        + " WHERE " + DbHelper.I_NAME + " = ? AND " + DbHelper.I_SEX + " = ? AND " + DbHelper.I_STADIUM + " = ?";

                    curCSVInd = database.rawQuery(slct, new String[]{spname, male, stadium1}); // select male

                    while (curCSVInd.moveToNext())
                    {
                        cnts = curCSVInd.getInt(14); // individuals icount
                        cntsm = cntsm + cnts;
                    }
                    curCSVInd.close();

                    curCSVInd = database.rawQuery(slct, new String[]{spname, fmale, stadium1}); // select female

                    while (curCSVInd.moveToNext())
                    {
                        cnts = curCSVInd.getInt(14); // individuals icount
                        cntsf = cntsf + cnts;
                    }
                    curCSVInd.close();

                    String slct1 = "SELECT * FROM " + DbHelper.INDIVIDUALS_TABLE
                        + " WHERE " + DbHelper.I_NAME + " = ? AND " + DbHelper.I_STADIUM + " = ?";

                    curCSVInd = database.rawQuery(slct1, new String[]{spname, stadium2}); // select pupa

                    while (curCSVInd.moveToNext())
                    {
                        cnts = curCSVInd.getInt(14); // individuals icount
                        cntsp = cntsp + cnts;
                    }
                    curCSVInd.close();

                    curCSVInd = database.rawQuery(slct1, new String[]{spname, stadium3}); // select caterpillar

                    while (curCSVInd.moveToNext())
                    {
                        cnts = curCSVInd.getInt(14); // individuals icount
                        cntsl = cntsl + cnts;
                    }
                    curCSVInd.close();

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

                    String[] arrStr =
                        {
                            spname,                 // species name
                            curCSVCnt.getString(10), // local name 
                            curCSVCnt.getString(8), // species code 
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

                    cntsm = 0;
                    cntsf = 0;
                    cntsp = 0;
                    cntsl = 0;
                    cntse = 0;
                }
                curCSVCnt.close();

                countDataSource = new CountDataSource(this);
                countDataSource.open();
                int sumSpec = countDataSource.getDiffSpec(); // get number of different species
                countDataSource.close();
                
                // write total sum
                String[] arrSum =
                    {
                        getString(R.string.sumSpec), 
                        Integer.toString(sumSpec),
                        getString(R.string.sum),
                        Integer.toString(summf),
                        Integer.toString(summ),
                        Integer.toString(sumf),
                        Integer.toString(sump),
                        Integer.toString(suml),
                        Integer.toString(sume),
                        getString(R.string.sum_total),
                        Integer.toString(sum)
                    };
                csvWrite.writeNext(arrSum);

                // Empty row
                csvWrite.writeNext(arrEmpt);

                // write individual headline
                //    Species, Counts, Locality, Longitude, Latitude, Uncertainty, 
                //    Date, Time, Sexus, Stadium, Stadium, State, Indiv.-Notes 
                String[] arrIndHead =
                    {
                        getString(R.string.individuals),
                        getString(R.string.cnts),
                        getString(R.string.locality),
                        getString(R.string.ycoord),
                        getString(R.string.xcoord),
                        getString(R.string.uncerti),
                        getString(R.string.zcoord),
                        getString(R.string.date),
                        getString(R.string.time),
                        getString(R.string.sex),
                        getString(R.string.stadium),
                        getString(R.string.state1),
                        getString(R.string.bemi)
                    };
                csvWrite.writeNext(arrIndHead);

                // build the sorted individuals array
                curCSVInd = database.rawQuery("select * from " + DbHelper.INDIVIDUALS_TABLE
                    + " order by " + DbHelper.I_COUNT_ID, null);

                String lngi, latit;
                frst = 0;
                while (curCSVInd.moveToNext())
                {
                    longi = curCSVInd.getDouble(4);
                    lati = curCSVInd.getDouble(3);
                    uncer = Math.rint(curCSVInd.getDouble(6));
                    heigh = Math.rint(curCSVInd.getDouble(5));
                    spstate = curCSVInd.getInt(12);
                    cnts = curCSVInd.getInt(14);
                    if (cnts > 0)
                        strcnts = String.valueOf(cnts);
                    else
                        strcnts = "";

                    try
                    {
                        lngi = String.valueOf(longi).substring(0, 8); //longitude
                    } catch (StringIndexOutOfBoundsException e)
                    {
                        lngi = String.valueOf(longi);
                    }

                    try
                    {
                        latit = String.valueOf(lati).substring(0, 8); //latitude
                    } catch (StringIndexOutOfBoundsException e)
                    {
                        latit = String.valueOf(lati);
                    }

                    String[] arrIndividual =
                        {
                            curCSVInd.getString(2),  //species name
                            strcnts,                             //indiv. counts
                            curCSVInd.getString(9),  //locality
                            lngi,                                //longitude
                            latit,                               //latitude
                            String.valueOf(Math.round(uncer + 20)), //uncertainty + 20 m extra
                            String.valueOf(Math.round(heigh)),   //height
                            curCSVInd.getString(7),  //date
                            curCSVInd.getString(8),  //time
                            curCSVInd.getString(10), //sexus
                            curCSVInd.getString(11), //stadium
                            String.valueOf(spstate),             //state
                            curCSVInd.getString(13)  //indiv. notes
                        };
                    csvWrite.writeNext(arrIndividual);

                    if (longi != 0) // has coordinates
                    {
                        //Toast.makeText(getApplicationContext(), longi, Toast.LENGTH_SHORT).show();
                        if (MyDebug.LOG)
                            Log.d(TAG, "longi " + longi);
                        if (frst == 0)
                        {
                            loMin = longi;
                            loMax = longi;
                            laMin = lati;
                            laMax = lati;
                            uncer1 = uncer;
                            frst = 1; // just 1 with coordinates
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

                // Empty row
                csvWrite.writeNext(arrEmpt);

                // write Average Coords
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
                    latit = String.valueOf(la).substring(0, 8); //latitude
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
                curCSVInd.close();
                dbHandler.close();

                headDataSource.close();
                sectionDataSource.close();
                individualsDataSource.close();

//                Toast.makeText(this, getString(R.string.saveWin), Toast.LENGTH_SHORT).show();
                showSnackbar(getString(R.string.saveWin));
            } catch (IOException e)
            {
                if (MyDebug.LOG)
                    Log.e(TAG, "Failed to export csv file");
//                Toast.makeText(this, getString(R.string.saveFail), Toast.LENGTH_LONG).show();
                showSnackbarRed(getString(R.string.saveFail));
            }
        }
    }

    /**************************************************************************************************/
    @SuppressLint({"SdCardPath", "LongLogTag"})
    // modified by wmstein
    private void exportBasisDb()
    {
        // Export Basic DB with permission check
        modePerm = 5;
        permissionCaptureFragment(); // calls doExportBasisDb()
    }

    private void doExportBasisDb()
    {
        // tmpfile -> /data/data/com.wmstein.tourcount/files/tourcount_tmp.db
        String tmpPath = getApplicationContext().getFilesDir().getPath();
        tmpPath = tmpPath.substring(0, tmpPath.lastIndexOf("/")) + "/files/tourcount_tmp.db";
        File tmpfile = new File(tmpPath);

        // outfile -> /storage/emulated/0/tourcount0.db
        outfile = new File(Environment.getExternalStorageDirectory() + "/tourcount0.db");
        
        // infile <- /data/data/com.wmstein.tourcount/databases/tourcount.db
        String inPath = getApplicationContext().getFilesDir().getPath();
        inPath = inPath.substring(0, inPath.lastIndexOf("/")) + "/databases/tourcount.db";
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
            if (MyDebug.LOG)
                Log.d(TAG, "No sdcard access");
//            Toast.makeText(this, getString(R.string.noCard), Toast.LENGTH_LONG).show();
            showSnackbarRed(getString(R.string.noCard));
        }
        else
        {
            // export the basic db
            try
            {
                // save current db as backup db tmpfile
                copy(infile, tmpfile);

                // clear DB values for basic DB
                clearDBValues();

                // write Basis DB
                copy(infile, outfile);

                // restore actual db from tmpfile
                copy(tmpfile, infile);

                // delete backup db
                boolean d0 = tmpfile.delete();
                if (d0)
                {
//                    Toast.makeText(this, getString(R.string.saveWin), Toast.LENGTH_SHORT).show();
                    showSnackbar(getString(R.string.saveWin));
                }
            } catch (IOException e)
            {
                if (MyDebug.LOG)
                    Log.e(TAG, "Failed to export Basic DB");
//                Toast.makeText(this, getString(R.string.saveFail), Toast.LENGTH_LONG).show();
                showSnackbarRed(getString(R.string.saveFail));
            }
        }
    }

    /**************************************************************************************************/
    // Clear all relevant DB values, reset to basic DB 
    // created by wmstein
    private void resetToBasisDb()
    {
        // confirm dialogue before anything else takes place
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setIcon(android.R.drawable.ic_dialog_alert);
        builder.setMessage(R.string.confirmResetDB);
        builder.setCancelable(false);

        builder.setPositiveButton(R.string.deleteButton, new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int id)
            {
                boolean r_ok = clearDBValues();
                if (r_ok)
                {
                    showSnackbar(getString(R.string.reset2basic));
//                    Toast.makeText(getApplicationContext(), getString(R.string.reset2basic), Toast.LENGTH_SHORT).show();
                }
                //noinspection ConstantConditions
                getSupportActionBar().setTitle("");
            }
        });

        builder.setNegativeButton(R.string.cancelButton, new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int id)
            {
                dialog.cancel();
            }
        });
        alert = builder.create();
        alert.show();
    }

    // Clear temp_loc in temp
    private void clear_loc()
    {
        dbHandler = new DbHelper(this);
        database = dbHandler.getWritableDatabase();

        String sql = "UPDATE " + DbHelper.TEMP_TABLE + " SET "
            + DbHelper.T_TEMP_LOC + " = '';";
        database.execSQL(sql);
        dbHandler.close();
    }

    // clear DB values for basic DB
    private boolean clearDBValues()
    {
        // clear values in DB
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
                + DbHelper.S_TEMP + " = 0, "
                + DbHelper.S_WIND + " = 0, "
                + DbHelper.S_CLOUDS + " = 0, "
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
            if (MyDebug.LOG)
                Log.e(TAG, "Failed to reset DB");
//            Toast.makeText(this, getString(R.string.resetFail), Toast.LENGTH_LONG).show();
            showSnackbarRed(getString(R.string.resetFail));
            r_ok = false;
        }
        return r_ok;
    }

    /**************************************************************************************************/
    @SuppressLint("SdCardPath")
    // Choose a file to load and set it to tourcount.db
    // based on android-file-chooser from Google Code Archive
    // Created by wmstein
    private void loadFile()
    {
        // Import DB with permission check
        modePerm = 6;
        permissionCaptureFragment(); // calls doImportDB()
    }
    
    private void doImportDB()
    {
        Intent intent = new Intent(this, AdvFileChooser.class);
        ArrayList<String> extensions = new ArrayList<>();
        extensions.add(".db");
        String filterFileName = "tourcount";
        intent.putStringArrayListExtra("filterFileExtension", extensions);
        intent.putExtra("filterFileName", filterFileName);
        startActivityForResult(intent, FILE_CHOOSER);
    }

    @SuppressLint("LongLogTag")
    @Override
    // onActivityResult is part of loadFile() and processes the result of AdvFileChooser
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        String fileSelected = "";
        if ((requestCode == FILE_CHOOSER) && (resultCode == -1))
        {
            fileSelected = data.getStringExtra("fileSelected");
            //Toast.makeText(this, fileSelected, Toast.LENGTH_SHORT).show();
        }

        //infile = selected File
        if (!fileSelected.equals(""))
        {
            infile = new File(fileSelected);
            
            // outfile = "/data/data/com.wmstein.tourcount/databases/tourcount.db"
            String destPath = this.getFilesDir().getPath();
            destPath = destPath.substring(0, destPath.lastIndexOf("/")) + "/databases/tourcount.db";
            outfile = new File(destPath);

            // confirm dialogue before anything else takes place
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setIcon(android.R.drawable.ic_dialog_alert);
            builder.setMessage(R.string.confirmDBImport)
                .setCancelable(false).setPositiveButton(R.string.importButton, new DialogInterface.OnClickListener()
            {
                public void onClick(DialogInterface dialog, int id)
                {
                    // START
                    // replace this with another function rather than this lazy c&p
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
                        if (MyDebug.LOG)
                            Log.d(TAG, "No sdcard access");
//                        Toast.makeText(getApplicationContext(), getString(R.string.noCard), Toast.LENGTH_LONG).show();
                        showSnackbarRed(getString(R.string.noCard));
                    }
                    else
                    {
                        try
                        {
                            copy(infile, outfile);
//                            Toast.makeText(getApplicationContext(), getString(R.string.importWin), Toast.LENGTH_SHORT).show();
                            showSnackbar(getString(R.string.importWin));
                            // save values for initial count-id and itemposition 
                            SharedPreferences.Editor editor = prefs.edit();
                            editor.putInt("count_id", 1);
                            editor.putInt("item_Position", 0);
                            editor.apply();

                            Section section;
                            sectionDataSource = new SectionDataSource(getApplicationContext());
                            sectionDataSource.open();
                            section = sectionDataSource.getSection();

                            // List tour name as title
                            try
                            {
                                getSupportActionBar().setTitle(section.name);
                            } catch (NullPointerException e)
                            {
                                // nothing
                            }
                            sectionDataSource.close();
                        } catch (IOException e)
                        {
                            if (MyDebug.LOG)
                                Log.e(TAG, "Failed to import database");
//                            Toast.makeText(getApplicationContext(), getString(R.string.importFail), Toast.LENGTH_LONG).show();
                            showSnackbarRed(getString(R.string.importFail));
                        }
                    }
                    // END
                }
            }).setNegativeButton(R.string.cancelButton, new DialogInterface.OnClickListener()
            {
                public void onClick(DialogInterface dialog, int id)
                {
                    dialog.cancel();
                }
            });
            alert = builder.create();
            alert.show();
        }
    }

    /**************************************************************************************************/
    @SuppressLint({"SdCardPath", "LongLogTag"})
    // modified by wmstein
    private void importBasisDb()
    {
        // Import basic DB with permission check
        modePerm = 7;
        permissionCaptureFragment(); // calls doImportBasisDB()
    }
    
    private void doImportBasisDB()
    {
        // infile <- /storage/emulated/0/tourcount0.db
        infile = new File(Environment.getExternalStorageDirectory() + "/tourcount0.db");
        
        // outfile -> /data/data/com.wmstein.tourcount/databases/tourcount.db
        String destPath = getApplicationContext().getFilesDir().getPath();
        destPath = destPath.substring(0, destPath.lastIndexOf("/")) + "/databases/tourcount.db";
        outfile = new File(destPath);
        if (!(infile.exists()))
        {
//            Toast.makeText(this, getString(R.string.noDb), Toast.LENGTH_LONG).show();
            showSnackbar(getString(R.string.noDb));
            return;
        }

        // confirm dialogue before anything else takes place
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setIcon(android.R.drawable.ic_dialog_alert);
        builder.setMessage(R.string.confirmBasisImport).setCancelable(false).setPositiveButton(R.string.importButton, new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int id)
            {
                // START
                // replace this with another function rather than this lazy c&p
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
                    if (MyDebug.LOG)
                        Log.d(TAG, "No sdcard access");
//                    Toast.makeText(getApplicationContext(), getString(R.string.noCard), Toast.LENGTH_LONG).show();
                    showSnackbarRed(getString(R.string.noCard));
                }
                else
                {
                    try
                    {
                        copy(infile, outfile);
//                        Toast.makeText(getApplicationContext(), getString(R.string.importWin), Toast.LENGTH_SHORT).show();
                        showSnackbar(getString(R.string.importWin));

                        // save values for initial count-id and itemposition 
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putInt("count_id", 1);
                        editor.putInt("item_Position", 0);
                        editor.apply();

                        //noinspection ConstantConditions
                        getSupportActionBar().setTitle("");
                    } catch (IOException e)
                    {
                        if (MyDebug.LOG)
                            Log.e(TAG, "Failed to import database");
//                        Toast.makeText(getApplicationContext(), getString(R.string.importFail), Toast.LENGTH_LONG).show();
                        showSnackbarRed(getString(R.string.importFail));
                    }
                }
                // END
            }
        }).setNegativeButton(R.string.cancelButton, new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int id)
            {
                dialog.cancel();
            }
        });
        alert = builder.create();
        alert.show();
    }

    /**********************************************************************************************/
    // http://stackoverflow.com/questions/9292954/how-to-make-a-copy-of-a-file-in-android
    private void copy(File src, File dst) throws IOException
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
        View view = findViewById(R.id.baseLayout);
        Snackbar sB = Snackbar.make(view, Html.fromHtml("<font color=\"#00ff00\">" + str + "</font>"), Snackbar.LENGTH_SHORT);
        TextView tv = sB.getView().findViewById(R.id.snackbar_text);
        tv.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        sB.show();
    }

    private void showSnackbarRed(String str) // bold red text
    {
        View view = findViewById(R.id.baseLayout);
        Snackbar sB = Snackbar.make(view, Html.fromHtml("<font color=\"#ff0000\"><b>" + str + "</font></b>"), Snackbar.LENGTH_LONG);
        TextView tv = sB.getView().findViewById(R.id.snackbar_text);
        tv.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        sB.show();
    }

}
