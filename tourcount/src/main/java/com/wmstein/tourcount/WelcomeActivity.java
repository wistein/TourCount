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
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ScrollView;
import android.widget.Toast;

import com.wmstein.egm.EarthGravitationalModel;
import com.wmstein.filechooser.AdvFileChooser;
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

import static java.lang.Math.sqrt;

/**********************************************************************
 * WelcomeActivity provides the starting page with menu and buttons for
 * import/export/help/info methods and
 * EditMetaActivity, CountingActivity and ListSpeciesActivity.
 * <p/>
 * Based on BeeCount's WelcomeActivity.java by milo on 05/05/2014.
 * Changes and additions for TourCount by wmstein since 2016-04-18,
 * last modification on 2018-04-03
 */
public class WelcomeActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener
{
    private static final int FILE_CHOOSER = 11;
    private static final String TAG = "TourCountWelcomeAct";
    private TourCountApplication tourCount;
    private ChangeLog cl;
    private ViewHelp vh;
    private boolean canGetLocation = false;
    public boolean doubleBackToExitPressedOnce;

    // Location info handling
    private LocationManager locationManager;
    private LocationListener locationListener;
    private String provider;
    private double latitude, longitude, height, uncertainty;

    // import/export stuff
    private File infile;
    private File outfile;
    private boolean mExternalStorageAvailable = false;
    private boolean mExternalStorageWriteable = false;
    private final String state = Environment.getExternalStorageState();
    private AlertDialog alert;
    private SectionDataSource sectionDataSource;
    private final Handler mHandler = new Handler();

    // preferences
    private String sortPref;
    private boolean screenOrientL; // option for screen orientation
    private boolean metaPref;      // option for reverse geocoding
    private String emailString = ""; // mail address for OSM query

    // following stuff for purging export db
    private SQLiteDatabase database;
    private DbHelper dbHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        tourCount = (TourCountApplication) getApplication();
        SharedPreferences prefs = TourCountApplication.getPrefs();
        prefs.registerOnSharedPreferenceChangeListener(this);
        sortPref = prefs.getString("pref_sort_sp", "none"); // sort mode species list
        screenOrientL = prefs.getBoolean("screen_Orientation", false);

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

        // List tour name as title
        Section section;
        String sname = "";
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
            Toast.makeText(this, R.string.corruptDb, Toast.LENGTH_LONG).show();
        }

        try
        {
            getSupportActionBar().setTitle(sname);
        } catch (NullPointerException e)
        {
            // nothing
        }

        // if API level > 23 permission request is necessary
        int REQUEST_CODE_STORAGE = 123; // Random unique identifier for specific permission request since Android 6.0
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
        {
            int hasWriteExtStoragePermission = checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            if (hasWriteExtStoragePermission != PackageManager.PERMISSION_GRANTED)
            {
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE_STORAGE);
            }
        }

        // Request GPS location permission
        int REQUEST_CODE_GPS = 124;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
        {
            int hasAccessFineLocationPermission = checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION);
            if (hasAccessFineLocationPermission != PackageManager.PERMISSION_GRANTED)
            {
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_CODE_GPS);
            }
        }

        // Request Network location permission
        int REQUEST_CODE_NETWORK = 125;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
        {
            int hasAccessCoarseLocationPermission = checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION);
            if (hasAccessCoarseLocationPermission != PackageManager.PERMISSION_GRANTED)
            {
                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_CODE_NETWORK);
            }
        }

        cl = new ChangeLog(this);
        vh = new ViewHelp(this);
        // Show changelog for new version
        if (cl.firstRun())
            cl.getLogDialog().show();

        // test for GPS or Network location
        if (!canLocation())
        {
            // can't get location, GPS or Network is not enabled
            Toast.makeText(getApplicationContext(), R.string.activate_GPS, Toast.LENGTH_LONG).show();
        }
    }

    // Try to find locationservice
    private Boolean canLocation()
    {
        try
        {
            LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

            // getting GPS status
            boolean isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

            // getting network status (needs UnifiedNlp + location backend on LineageOS, no height info)
            boolean isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
            //Toast.makeText(getApplicationContext(), "NetworkEnabled: " + isNetworkEnabled + "\nGPSenabled: " + isGPSEnabled, Toast.LENGTH_LONG).show();

            if (isGPSEnabled || isNetworkEnabled)
            {
                this.canGetLocation = true;
            }

        } catch (Exception e)
        {
            if (MyDebug.LOG)
                Log.e(TAG, "Cannot get location.", e);
        }

        return canGetLocation;
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
    // supplemented with exportBasicMenu, importBasicMenu, viewSpecies and viewHelp by wmstein
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
            Toast.makeText(getApplicationContext(), getString(R.string.wait), Toast.LENGTH_SHORT).show();

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

    public void viewCounts(View view)
    {
        startActivity(new Intent(getApplicationContext(), CountingActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
    }

    public void editMeta(View view)
    {
        startActivity(new Intent(this, EditMetaActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
    }

    public void viewSpecies(View view)
    {
        Toast.makeText(getApplicationContext(), getString(R.string.wait), Toast.LENGTH_SHORT).show();
        // pause for 100 msec to show toast
        mHandler.postDelayed(new Runnable()
        {
            public void run()
            {
                startActivity(new Intent(getApplicationContext(), ListSpeciesActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
            }
        }, 100);
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        prefs.registerOnSharedPreferenceChangeListener(this);
        screenOrientL = prefs.getBoolean("screen_Orientation", false);
        metaPref = prefs.getBoolean("pref_metadata", false);   // use Reverse Geocoding
        emailString = prefs.getString("email_String", "");     // for reliable query of Nominatim service

        if (screenOrientL)
        {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }
        else
        {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }

        // Get LocationManager instance
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
/*
        // Request list with names of all providers
        List<String> providers = locationManager.getAllProviders();
        for (String name : providers)
        {
            LocationProvider lp = locationManager.getProvider(name);
        }
*/
        // Best possible provider
        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        // criteria.setPowerRequirement(Criteria.POWER_HIGH);
        provider = locationManager.getBestProvider(criteria, true);

        // Create LocationListener object
        locationListener = new LocationListener()
        {
            @Override
            public void onStatusChanged(String provider, int status, Bundle extras)
            {
                // nothing
            }

            @Override
            public void onProviderEnabled(String provider)
            {
                // nothing
            }

            @Override
            public void onProviderDisabled(String provider)
            {
                // nothing
            }

            @Override
            public void onLocationChanged(Location location)
            {
                if (location != null)
                {
                    latitude = location.getLatitude();
                    longitude = location.getLongitude();
                    height = location.getAltitude();
                    if (height != 0)
                        height = correctHeight(latitude, longitude, height);
                    uncertainty = location.getAccuracy();
                }
            }
        };

        // get location service
        try
        {
            locationManager.requestLocationUpdates(provider, 3000, 0, locationListener);
        } catch (Exception e)
        {
            Toast.makeText(this, getString(R.string.no_GPS), Toast.LENGTH_LONG).show();
        }

        // get reverse geocoding (todo: 1st count missing geo info)
        if (metaPref && (latitude != 0 || longitude != 0))
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
                        + Double.toString(latitude) + "&lon=" + Double.toString(longitude) + "&zoom=18&addressdetails=1";
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

        // List tour name as title
        Section section;
        String sname = "";
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

    public void onSharedPreferenceChanged(SharedPreferences prefs, String key)
    {
        ScrollView baseLayout = findViewById(R.id.baseLayout);
        baseLayout.setBackground(null);
        baseLayout.setBackground(tourCount.setBackground());
        sortPref = prefs.getString("pref_sort_sp", "none");
        screenOrientL = prefs.getBoolean("screen_Orientation", false);
        metaPref = prefs.getBoolean("pref_metadata", false);   // use Reverse Geocoding
        emailString = prefs.getString("email_String", "");     // for reliable query of Nominatim service
    }

    public void onPause()
    {
        super.onPause();

        sectionDataSource.close();
    }

    @Override
    public void onBackPressed()
    {
        if (doubleBackToExitPressedOnce)
        {
            super.onBackPressed();
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
        // Stop location service
        try
        {
            if (locationManager != null)
            {
                //Potentially missing permission is catched by exception
                locationManager.removeUpdates(locationListener);
                locationManager = null;
            }
        } catch (Exception e)
        {
            // do nothing
        }
    }

    public void onDestroy()
    {
        super.onDestroy();

        sectionDataSource.close();
        // Stop location service
        try
        {
            if (locationManager != null)
            {
                //Potentially missing permission is catched by exception
                locationManager.removeUpdates(locationListener);
                locationManager = null;
            }
        } catch (Exception e)
        {
            // do nothing
        }
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
        boolean mExternalStorageAvailable;
        boolean mExternalStorageWriteable;
        String state = Environment.getExternalStorageState();
        outfile = new File(Environment.getExternalStorageDirectory() + "/tourcount_" + getcurDate() + ".db");
        String destPath = "/data/data/com.wmstein.tourcount/databases";

        try
        {
            destPath = getFilesDir().getPath();
        } catch (Exception e)
        {
            if (MyDebug.LOG)
                Log.e(TAG, "destPath error: " + e.toString());
        }
        destPath = destPath.substring(0, destPath.lastIndexOf("/")) + "/databases";
        infile = new File(destPath + "/tourcount.db");

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
            Toast.makeText(this, getString(R.string.noCard), Toast.LENGTH_LONG).show();
        }
        else
        {
            // export the db
            try
            {
                // export db
                copy(infile, outfile);
                Toast.makeText(this, getString(R.string.saveWin), Toast.LENGTH_SHORT).show();
            } catch (IOException e)
            {
                if (MyDebug.LOG)
                    Log.e(TAG, "Failed to copy database");
                Toast.makeText(this, getString(R.string.saveFail), Toast.LENGTH_LONG).show();
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
        boolean mExternalStorageAvailable;
        boolean mExternalStorageWriteable;
        String state = Environment.getExternalStorageState();
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
            Toast.makeText(this, getString(R.string.noCard), Toast.LENGTH_LONG).show();
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

                String arrHead[] =
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
                String arrEmpt[] = {};
                csvWrite.writeNext(arrEmpt);

                // set location headline
                String arrLocHead[] =
                    {
                        getString(R.string.country),
                        getString(R.string.plz),
                        getString(R.string.city),
                        getString(R.string.place),
                        getString(R.string.zlnotes)
                    };
                csvWrite.writeNext(arrLocHead);

                // set location dataline
                String arrLocation[] =
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
                String arrEnvHead[] =
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
                String arrEnvironment[] =
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
                //    Species Name, Spec.-Code, Counts ♂♀, Counts ♂, Counts ♀, Counts Pupa, Counts Caterp., Counts Egg, Spec.-Notes
                String arrCntHead[] =
                    {
                        getString(R.string.spec),
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

                switch (sortPref) // sort mode species list
                {
                case "codes":
                    curCSVCnt = database.rawQuery("select * from " + DbHelper.COUNT_TABLE
                        + " WHERE " + " ("
                        + DbHelper.C_COUNT_F1I + " > 0 or " + DbHelper.C_COUNT_F2I + " > 0 or "
                        + DbHelper.C_COUNT_F3I + " > 0 or " + DbHelper.C_COUNT_PI + " > 0 or "
                        + DbHelper.C_COUNT_LI + " > 0 or " + DbHelper.C_COUNT_EI + " > 0)"
                        + " order by " + DbHelper.C_CODE, null, null);
                    break;
                default:
                    curCSVCnt = database.rawQuery("select * from " + DbHelper.COUNT_TABLE
                        + " WHERE " + " ("
                        + DbHelper.C_COUNT_F1I + " > 0 or " + DbHelper.C_COUNT_F2I + " > 0 or "
                        + DbHelper.C_COUNT_F3I + " > 0 or " + DbHelper.C_COUNT_PI + " > 0 or "
                        + DbHelper.C_COUNT_LI + " > 0 or " + DbHelper.C_COUNT_EI + " > 0)"
                        + " order by " + DbHelper.C_NAME, null, null);
                    break;
                }

                // open Individuals table 
                IndividualsDataSource individualsDataSource = new IndividualsDataSource(this);
                individualsDataSource.open();

                // get the number of individuals with attributes
                int cnt;  // counts count
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

                    String arrStr[] =
                        {
                            spname,                 // species name
                            curCSVCnt.getString(8), // species code 
                            strcntsmf,              // count ♂ o. ♀
                            strcntsm,               // count ♂
                            strcntsf,               // count ♀
                            strcntsp,               // count pupa
                            strcntsl,               // count caterpillar
                            strcntse,               // count egg
                            curCSVCnt.getString(4)  // species notes
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

                // write total sum
                String arrSum[] =
                    {
                        "",
                        getString(R.string.sum),
                        Integer.toString(summf),
                        Integer.toString(summ),
                        Integer.toString(sumf),
                        Integer.toString(sump),
                        Integer.toString(suml),
                        Integer.toString(sume),
                        Integer.toString(sum)
                    };
                csvWrite.writeNext(arrSum);

                // Empty row
                csvWrite.writeNext(arrEmpt);

                // write individual headline
                //    Species, Counts, Locality, Longitude, Latitude, Uncertainty, 
                //    Date, Time, Sexus, Stadium, Stadium, State, Indiv.-Notes 
                String arrIndHead[] =
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

                    String arrIndividual[] =
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
                String arrACoordHead[] =
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


                String arrAvCoords[] =
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

                Toast.makeText(this, getString(R.string.saveWin), Toast.LENGTH_SHORT).show();
            } catch (IOException e)
            {
                if (MyDebug.LOG)
                    Log.e(TAG, "Failed to export csv file");
                Toast.makeText(this, getString(R.string.saveFail), Toast.LENGTH_LONG).show();
            }
        }
    }

    /**************************************************************************************************/
    @SuppressLint({"SdCardPath", "LongLogTag"})
    // modified by wmstein
    private void exportBasisDb()
    {
        boolean mExternalStorageAvailable;
        boolean mExternalStorageWriteable;
        String state = Environment.getExternalStorageState();
        File tmpfile = new File("/data/data/com.wmstein.tourcount/files/tourcount_tmp.db");
        outfile = new File(Environment.getExternalStorageDirectory() + "/tourcount0.db");
        String destPath = "/data/data/com.wmstein.tourcount/files";

        try
        {
            destPath = getFilesDir().getPath();
        } catch (Exception e)
        {
            if (MyDebug.LOG)
                Log.e(TAG, "destPath error: " + e.toString());
        }
        destPath = destPath.substring(0, destPath.lastIndexOf("/")) + "/databases";
        infile = new File(destPath + "/tourcount.db");

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
            Toast.makeText(this, getString(R.string.noCard), Toast.LENGTH_LONG).show();
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
                Toast.makeText(this, getString(R.string.reset2basic), Toast.LENGTH_SHORT).show();

                // write Basis DB
                copy(infile, outfile);

                // restore actual db from tmpfile
                copy(tmpfile, infile);

                // delete backup db
                boolean d0 = tmpfile.delete();
                if (d0)
                {
                    Toast.makeText(this, getString(R.string.saveWin), Toast.LENGTH_SHORT).show();
                }
            } catch (IOException e)
            {
                if (MyDebug.LOG)
                    Log.e(TAG, "Failed to export Basic DB");
                Toast.makeText(this, getString(R.string.saveFail), Toast.LENGTH_LONG).show();
            }
        }
    }

    /**************************************************************************************************/
    // Clear all relevant DB values, reset to basic DB 
    // created by wmstein
    private void resetToBasisDb()
    {
        // a confirm dialogue before anything else takes place
        // http://developer.android.com/guide/topics/ui/dialogs.html#AlertDialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setIcon(android.R.drawable.ic_dialog_alert);
        builder.setMessage(R.string.confirmResetDB);
        builder.setCancelable(false);
        builder.setPositiveButton(R.string.deleteButton, new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int id)
            {
                clearDBValues();
                Toast.makeText(getApplicationContext(), getString(R.string.reset2basic), Toast.LENGTH_SHORT).show();
                //noinspection ConstantConditions
                getSupportActionBar().setTitle("");
            }
        });
        builder.setNegativeButton(R.string.importCancelButton, new DialogInterface.OnClickListener()
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
    private void clearDBValues()
    {
        // clear values in DB
        dbHandler = new DbHelper(this);
        database = dbHandler.getWritableDatabase();

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
    }

    /**************************************************************************************************/
    @SuppressLint("SdCardPath")
    // Choose a file to load and set it to tourcount.db
    // based on android-file-chooser from Google Code Archive
    // Created by wmstein
    private void loadFile()
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
    // Function is part of loadFile() and processes the result of AdvFileChooser
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
            // destPath = "/data/data/com.wmstein.tourcount/files"
            String destPath = this.getFilesDir().getPath();
            try
            {
                destPath = getFilesDir().getPath();
            } catch (Exception e)
            {
                if (MyDebug.LOG)
                    Log.e(TAG, "destPath error: " + e.toString());
            }
            destPath = destPath.substring(0, destPath.lastIndexOf("/")) + "/databases";
            //outfile = "/data/data/com.wmstein.tourcount/databases/tourcount.db"
            outfile = new File(destPath + "/tourcount.db");

            // a confirm dialogue before anything else takes place
            // http://developer.android.com/guide/topics/ui/dialogs.html#AlertDialog
            // could make the dialog central in the popup - to do later
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
                        Toast.makeText(getApplicationContext(), getString(R.string.noCard), Toast.LENGTH_LONG).show();
                    }
                    else
                    {
                        try
                        {
                            copy(infile, outfile);
                            Toast.makeText(getApplicationContext(), getString(R.string.importWin), Toast.LENGTH_SHORT).show();

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
                            Toast.makeText(getApplicationContext(), getString(R.string.importFail), Toast.LENGTH_LONG).show();
                        }
                    }
                    // END
                }
            }).setNegativeButton(R.string.importCancelButton, new DialogInterface.OnClickListener()
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
            //infile = new File("/data/data/com.wmstein.tourcount/databases/tourcount0.db");
            infile = new File(Environment.getExternalStorageDirectory() + "/tourcount0.db");
        String destPath = "/data/data/com.wmstein.tourcount/databases";
        try
        {
            destPath = getFilesDir().getPath();
        } catch (Exception e)
        {
            if (MyDebug.LOG)
                Log.e(TAG, "destPath error: " + e.toString());
        }
        destPath = destPath.substring(0, destPath.lastIndexOf("/")) + "/databases";
        //outfile = new File("/data/data/com.wmstein.tourcount/databases/tourcount.db");
        outfile = new File(destPath + "/tourcount.db");
        if (!(infile.exists()))
        {
            Toast.makeText(this, getString(R.string.noDb), Toast.LENGTH_LONG).show();
            return;
        }

        // a confirm dialogue before anything else takes place
        // http://developer.android.com/guide/topics/ui/dialogs.html#AlertDialog
        // could make the dialog central in the popup - to do later
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
                    Toast.makeText(getApplicationContext(), getString(R.string.noCard), Toast.LENGTH_LONG).show();
                }
                else
                {
                    try
                    {
                        copy(infile, outfile);
                        Toast.makeText(getApplicationContext(), getString(R.string.importWin), Toast.LENGTH_SHORT).show();
                        //noinspection ConstantConditions
                        getSupportActionBar().setTitle("");
                    } catch (IOException e)
                    {
                        if (MyDebug.LOG)
                            Log.e(TAG, "Failed to import database");
                        Toast.makeText(getApplicationContext(), getString(R.string.importFail), Toast.LENGTH_LONG).show();
                    }
                }
                // END
            }
        }).setNegativeButton(R.string.importCancelButton, new DialogInterface.OnClickListener()
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
    
}
