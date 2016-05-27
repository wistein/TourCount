package com.wmstein.tourcount;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ScrollView;
import android.widget.Toast;

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

import sheetrock.panda.changelog.ChangeLog;
import sheetrock.panda.changelog.ViewHelp;

/**
 * WelcomeActivity provides the starting page with menu and buttons for 
 * import/export/help/info methods and ListSectionActivity/ListSpeciesActivity.
 * 
 * Originally based an BeeCount (GitHub) created by milo on 05/05/2014.
 * Changes and additions by wmstein on 18.04.2016
 */

public class WelcomeActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener
{
    private static String TAG = "TourCountWelcomeActivity";
    TourCountApplication tourCount;
    SharedPreferences prefs;
    ChangeLog cl;
    ViewHelp vh; // added by wmstein

    // flags for GPS, network status
    boolean isGPSEnabled = false;
    boolean isNetworkEnabled = false;
    boolean canGetLocation = false;

    Location location;

    // Declaring a Location Manager for GPS or network
    protected LocationManager locationManager;

    // import/export stuff
    File infile;
    File outfile;
    File tmpfile;
    boolean mExternalStorageAvailable = false;
    boolean mExternalStorageWriteable = false;
    String state = Environment.getExternalStorageState();
    AlertDialog alert;

    // following stuff for purging export db added by wmstein
    private SQLiteDatabase database;
    private DbHelper dbHandler;

    SectionDataSource sectionDataSource;
    HeadDataSource headDataSource;
    IndividualsDataSource individualsDataSource;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        tourCount = (TourCountApplication) getApplication();
        prefs = TourCountApplication.getPrefs();
        prefs.registerOnSharedPreferenceChangeListener(this);

        Head head;
        headDataSource = new HeadDataSource(this);
        headDataSource.open();
        head = headDataSource.getHead();

        //LinearLayout baseLayout = (LinearLayout) findViewById(R.id.baseLayout);
        ScrollView baseLayout = (ScrollView) findViewById(R.id.baseLayout);
        baseLayout.setBackground(tourCount.getBackground());

        // a title isn't necessary on this welcome screen as it appears below
        getSupportActionBar().setTitle(head.country);

        headDataSource.close();

        cl = new ChangeLog(this);
        vh = new ViewHelp(this); // by wmstein
        if (cl.firstRun())
            cl.getLogDialog().show();

        // test for GPS
        getLocation();

        if (!canGetLocation)
        {
            // can't get location, GPS or Network is not enabled
            Toast.makeText(getApplicationContext(), R.string.activate_GPS, Toast.LENGTH_LONG).show();
        }

    }

    // Try to find locationservice
    public Boolean getLocation()
    {
        try
        {
            locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

            // getting GPS status
            isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

            // getting network status
            isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

            if (isGPSEnabled || isNetworkEnabled)
            {
                this.canGetLocation = true;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return canGetLocation;
    }

    // Date for filename of Export-DB
    // by wmstein
    public String getcurDate()
    {
        Date date = new Date();
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
            startActivity(new Intent(this, CountingActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
            return true;
        }
        else if (id == R.id.viewSpecies)
        {
            Toast.makeText(getApplicationContext(), getString(R.string.wait), Toast.LENGTH_SHORT).show();

            startActivity(new Intent(this, ListSpeciesActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void viewCounts(View view)
    {
        startActivity(new Intent(this, CountingActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
    }

    public void editMeta(View view)
    {
        startActivity(new Intent(this, EditMetaActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
    }

    public void viewSpecies(View view)
    {
        Toast.makeText(getApplicationContext(), getString(R.string.wait), Toast.LENGTH_SHORT).show();
        startActivity(new Intent(this, ListSpeciesActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
    }

    public void onSharedPreferenceChanged(SharedPreferences prefs, String key)
    {
        //LinearLayout baseLayout = (LinearLayout) findViewById(R.id.baseLayout);
        ScrollView baseLayout = (ScrollView) findViewById(R.id.baseLayout);
        baseLayout.setBackground(null);
        baseLayout.setBackground(tourCount.setBackground());
    }

    public void onPause()
    {
        super.onPause();
//        gps.stopUsingGPS();
    }

    public void onStop()
    {
        super.onStop();
//        gps.stopUsingGPS();
    }

    public void onDestroy()
    {
        super.onDestroy();
//        gps.stopUsingGPS();
    }

    /**************************************************************************
     * The six activities below are for exporting and importing the database. 
     * They've been put here because no database should be open at this point.
     ***********************************************************************/
    // Exports DB to SdCard/tourcount_yyyy-MM-dd_HHmmss.db
    // supplemented with date and time in filename by wmstein
    @SuppressLint("SdCardPath")
    public void exportDb()
    {
        boolean mExternalStorageAvailable;
        boolean mExternalStorageWriteable;
        String state = Environment.getExternalStorageState();
        outfile = new File(Environment.getExternalStorageDirectory() + "/tourcount_" + getcurDate() + ".db");
        String destPath = "/data/data/com.wmstein.tourcount/files";

        try
        {
            destPath = getFilesDir().getPath();
        } catch (Exception e)
        {
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

    @SuppressLint("SdCardPath")
    public void exportDb2CSV()
    {
        boolean mExternalStorageAvailable;
        boolean mExternalStorageWriteable;
        String state = Environment.getExternalStorageState();
        tmpfile = new File("/data/data/com.wmstein.tourcount/files/tourcount_tmp.db");
        outfile = new File(Environment.getExternalStorageDirectory() + "/tourcount_" + getcurDate() + ".csv");
        String destPath = "/data/data/com.wmstein.tourcount/files";

        Section section;
        String sectName;
        String sectNotes;
        
        Head head;
        String country, inspecName;
        int temp, wind, clouds;
        String plz, city, place;
        String date, start_tm, end_tm;
        int spcode, spstate;

        try
        {
            destPath = getFilesDir().getPath();
        } catch (Exception e)
        {
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
            Log.e(TAG, "No sdcard access");
            Toast.makeText(this, getString(R.string.noCard), Toast.LENGTH_LONG).show();
        }
        else
        {
            // export the count table to csv
            try
            {
                // save current db as backup db tmpfile
                copy(infile, tmpfile);

                // purge tourcount.db count table from empty rows 
                dbHandler = new DbHelper(this);
                database = dbHandler.getWritableDatabase();

                String sql = "DELETE FROM " + DbHelper.COUNT_TABLE + " WHERE (" + DbHelper.C_COUNT + " = 0);";
                database.execSQL(sql);

                // open Head and Section table for head and meta info
                headDataSource = new HeadDataSource(this);
                headDataSource.open();
                sectionDataSource = new SectionDataSource(this);
                sectionDataSource.open();
                individualsDataSource = new IndividualsDataSource(this);
                individualsDataSource.open();

                // export purged db as csv
                CSVWriter csvWrite = new CSVWriter(new FileWriter(outfile));
                Cursor curCSVCnt = database.rawQuery("select * from " + DbHelper.COUNT_TABLE, null);

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
                        getString(R.string.zlist) + ":",     //Count List:
                        sectName,                            //section name
                        "",
                        getString(R.string.inspector) + ":", //Inspector:
                        inspecName                           //inspector name
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
                //    Species, Spec.-Code, Counts, Spec.-Notes
                String arrCntHead[] =
                    {
                        getString(R.string.spec),
                        getString(R.string.speccode),
                        getString(R.string.cnts),
                        getString(R.string.bema)
                    };
                csvWrite.writeNext(arrCntHead);
                
                // write counts data
                while (curCSVCnt.moveToNext())
                {
                    spcode = curCSVCnt.getInt(0);
                    String arrStr[] =
                        {
                            curCSVCnt.getString(3), // species name
                            String.valueOf(spcode), // species code 
                            curCSVCnt.getString(2), // count
                            curCSVCnt.getString(4)  // species notes
                        };
                    csvWrite.writeNext(arrStr);
                }

                // Empty row
                csvWrite.writeNext(arrEmpt);

                // write individual headline
                //    Species, Spec.-Code, Locality, Latitude, Longitude, Uncertainty, 
                //    Date, Time, Sex, Stadium, State, Indiv.-Notes 
                String arrIndHead[] =
                    {
                        getString(R.string.spec),
                        getString(R.string.speccode),
                        getString(R.string.locality),
                        getString(R.string.ycoord),
                        getString(R.string.xcoord),
                        getString(R.string.uncerti),
                        getString(R.string.date),
                        getString(R.string.time),
                        getString(R.string.sex),
                        getString(R.string.stadium),
                        getString(R.string.state1),
                        getString(R.string.bemi)
                    };
                csvWrite.writeNext(arrIndHead);

                // build the sorted individuals array
                Cursor curCSVInd = database.rawQuery("select * from " + DbHelper.INDIVIDUALS_TABLE 
                    + " order by " + DbHelper.I_COUNT_ID, null);
                  
                while (curCSVInd.moveToNext())
                {
                    spcode = curCSVInd.getInt(1);
                    spstate = curCSVInd.getInt(11);
                    String arrIndividual[] =
                        {
                            curCSVInd.getString(2),  //species name
                            String.valueOf(spcode),  //species code 
                            curCSVInd.getString(8),  //locality
                            curCSVInd.getString(4),  //longitude
                            curCSVInd.getString(3),  //latitude
                            curCSVInd.getString(5),  //uncertainty
                            curCSVInd.getString(6),  //date
                            curCSVInd.getString(7),  //time
                            curCSVInd.getString(9),  //sex
                            curCSVInd.getString(10), //stadium
                            String.valueOf(spstate), //state
                            curCSVInd.getString(12)  //ind.-notes
                        };
                    csvWrite.writeNext(arrIndividual);
                }
                
                csvWrite.close();
                curCSVCnt.close();
                curCSVInd.close();
                dbHandler.close();

                // restore current db from tmpfile
                copy(tmpfile, infile);

                // delete backup db
                boolean d0 = tmpfile.delete();
                if (d0)
                {
                    Toast.makeText(this, getString(R.string.saveWin), Toast.LENGTH_SHORT).show();
                }
            } catch (IOException e)
            {
                Log.e(TAG, "Failed to export csv file");
                Toast.makeText(this, getString(R.string.saveFail), Toast.LENGTH_LONG).show();
            }
        }
    }

    /**************************************************************************************************/
    @SuppressLint("SdCardPath")
    // modified by wmstein
    public void exportBasisDb()
    {
        boolean mExternalStorageAvailable;
        boolean mExternalStorageWriteable;
        String state = Environment.getExternalStorageState();
        tmpfile = new File("/data/data/com.wmstein.tourcount/files/tourcount_tmp.db");
        outfile = new File(Environment.getExternalStorageDirectory() + "/tourcount0.db");
        String destPath = "/data/data/com.wmstein.tourcount/files";

        try
        {
            destPath = getFilesDir().getPath();
        } catch (Exception e)
        {
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
            Log.e(TAG, "No sdcard access");
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
                Log.e(TAG, "Failed to export Basic DB");
                Toast.makeText(this, getString(R.string.saveFail), Toast.LENGTH_LONG).show();
            }
        }
    }

    /**************************************************************************************************/
    // Clear all relevant DB values, reset to basic DB 
    // created by wmstein
    public void resetToBasisDb()
    {
        // a confirm dialogue before anything else takes place
        // http://developer.android.com/guide/topics/ui/dialogs.html#AlertDialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setIcon(android.R.drawable.ic_dialog_alert);
        builder.setMessage(R.string.confirmResetDB).setCancelable(false).setPositiveButton(R.string.deleteButton, new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int id)
            {
                clearDBValues();
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
        Toast.makeText(this, getString(R.string.reset2basic), Toast.LENGTH_SHORT).show();
    }

    // clear DB values for basic DB
    public void clearDBValues()
    {
        // clear values in DB
        dbHandler = new DbHelper(this);
        database = dbHandler.getWritableDatabase();

        String sql = "UPDATE " + DbHelper.COUNT_TABLE + " SET "
            + DbHelper.C_COUNT + " = 0, "
            + DbHelper.C_NOTES + " = '';";
        database.execSQL(sql);

        sql = "UPDATE " + DbHelper.SECTION_TABLE + " SET "
            + DbHelper.S_CREATED_AT + " = '', "
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

        sql = "DELETE FROM " + DbHelper.ALERT_TABLE;
        database.execSQL(sql);

        sql = "DELETE FROM " + DbHelper.INDIVIDUALS_TABLE;
        database.execSQL(sql);

        dbHandler.close();
    }

    /**************************************************************************************************/
    @SuppressLint("SdCardPath")
    // modified by wmstein
    public void importBasisDb()
    {
        //infile = new File("/data/data/com.wmstein.tourcount/databases/tourcount0.db");
        infile = new File(Environment.getExternalStorageDirectory() + "/tourcount0.db");
        String destPath = "/data/data/com.wmstein.tourcount/files";
        try
        {
            destPath = getFilesDir().getPath();
        } catch (Exception e)
        {
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
                    Log.e(TAG, "No sdcard access");
                    Toast.makeText(getApplicationContext(), getString(R.string.noCard), Toast.LENGTH_LONG).show();
                }
                else
                {
                    try
                    {
                        copy(infile, outfile);
                        Toast.makeText(getApplicationContext(), getString(R.string.importWin), Toast.LENGTH_SHORT).show();
                    } catch (IOException e)
                    {
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
    public void copy(File src, File dst) throws IOException
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
