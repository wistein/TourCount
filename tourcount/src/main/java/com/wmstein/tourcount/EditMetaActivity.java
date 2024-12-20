package com.wmstein.tourcount;

import android.Manifest;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.material.snackbar.Snackbar;
import com.wmstein.tourcount.database.Head;
import com.wmstein.tourcount.database.HeadDataSource;
import com.wmstein.tourcount.database.Section;
import com.wmstein.tourcount.database.SectionDataSource;
import com.wmstein.tourcount.widgets.EditLocationWidget;
import com.wmstein.tourcount.widgets.EditMetaWidget;
import com.wmstein.tourcount.widgets.EditTitleWidget;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;

/**********************************************************
 * EditMetaActivity collects meta info for the current tour
 * Created by wmstein on 2016-04-19,
 * last edit in Java on 2024-11-25
 */
public class EditMetaActivity extends AppCompatActivity
    implements PermissionsDialogFragment.PermissionsGrantedCallback
{
    private static final String TAG = "EditMetaAct";

    private final SharedPreferences prefs = TourCountApplication.getPrefs();
    private boolean metaPref;        // option for OSM reverse geocoding
    private String emailString = ""; // mail address for OSM query

    private Head head;
    private Section section;
    private HeadDataSource headDataSource;
    private SectionDataSource sectionDataSource;

    private Calendar pdate, ptime;

    private LinearLayout head_area;
    private TextView sDate, sTime, eTime;

    private EditTitleWidget ett;
    private EditLocationWidget elw;
    private EditMetaWidget emw;

    // Location info handling in the first activity to support a quicker 1. GPS fix
    private double latitude;
    private double longitude;
    LocationService locationService;

    // Permission dispatcher mode locationPermissionDispatcherMode: 
    //  1 = use location service
    //  2 = end location service
    int locationPermissionDispatcherMode;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        if (MyDebug.dLOG) Log.i(TAG, "86, onCreate");

        setContentView(R.layout.activity_edit_meta);

        // Option for full bright screen
        boolean brightPref = prefs.getBoolean("pref_bright", true);

        // Set full brightness of screen
        if (brightPref)
        {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            WindowManager.LayoutParams params = getWindow().getAttributes();
            params.screenBrightness = 1.0f;
            getWindow().setAttributes(params);
        }

        head_area = findViewById(R.id.edit_head);

        Objects.requireNonNull(getSupportActionBar()).setTitle(getString(R.string.editHeadTitle));

        headDataSource = new HeadDataSource(this);
        sectionDataSource = new SectionDataSource(this);

        // New onBackPressed logic
        OnBackPressedCallback callback = new OnBackPressedCallback(true)
        {
            @Override
            public void handleOnBackPressed()
            {
                if (MyDebug.dLOG) Log.i(TAG, "115, handleOnBackPressed");
                finish();
            }
        };
        getOnBackPressedDispatcher().addCallback(this, callback);
    }
    // End of onCreate()

    @Override
    protected void onResume()
    {
        super.onResume();

        if (MyDebug.dLOG) Log.i(TAG, "128, onResume");

        metaPref = prefs.getBoolean("pref_metadata", false);   // use Reverse Geocoding
        emailString = prefs.getString("email_String", "");     // for reliable query of Nominatim service

        // Get location with permissions check
        locationPermissionDispatcherMode = 1;
        locationCaptureFragment();

        // Clear existing view
        head_area.removeAllViews();

        // Setup data sources
        headDataSource.open();
        sectionDataSource.open();

        // Load head and meta data
        head = headDataSource.getHead();
        section = sectionDataSource.getSection();

        // Display editable list title, observer name and notes
        ett = new EditTitleWidget(this, null);
        ett.setWidgetTitle(getString(R.string.titleEdit));
        ett.setWidgetName(section.name);
        ett.setWidgetOName1(getString(R.string.inspector));
        ett.setWidgetOName2(head.observer);
        ett.setWidgetONotes1(getString(R.string.notesHere));
        ett.setWidgetONotes2(section.notes);
        ett.setHintN(getString(R.string.notesHint));
        head_area.addView(ett);

        // Display the editable location data
        elw = new EditLocationWidget(this, null);
        elw.setWidgetCo1(getString(R.string.country));
        elw.setWidgetCo2(section.country);
        elw.setWidgetPlz1(getString(R.string.plz));
        elw.setWidgetPlz2(section.plz);
        elw.setWidgetCity1(getString(R.string.city));
        elw.setWidgetCity2(section.city);
        elw.setWidgetPlace1(getString(R.string.place));
        elw.setWidgetPlace2(section.place);

        head_area.addView(elw);

        // Display the editable meta data
        emw = new EditMetaWidget(this, null);
        emw.setWidgetDate1(getString(R.string.date));
        emw.setWidgetDate2(section.date);
        emw.setWidgetStartTm1(getString(R.string.starttm));
        emw.setWidgetStartTm2(section.start_tm);
        emw.setWidgetEndTm1(getString(R.string.endtm));
        emw.setWidgetEndTm2(section.end_tm);

        emw.setWidgetTemp1(getString(R.string.temperature));
        emw.setWidgetWind1(getString(R.string.wind));
        emw.setWidgetClouds1(getString(R.string.clouds));

        emw.setWidgetTemp2(section.tmp);
        emw.setWidgetTemp3(section.tmp_end);
        emw.setWidgetWind2(section.wind);
        emw.setWidgetWind3(section.wind_end);
        emw.setWidgetClouds2(section.clouds);
        emw.setWidgetClouds3(section.clouds_end);
        head_area.addView(emw);

        pdate = Calendar.getInstance();
        ptime = Calendar.getInstance();

        sDate = this.findViewById(R.id.widgetDate2);
        sTime = this.findViewById(R.id.widgetStartTm2);
        eTime = this.findViewById(R.id.widgetEndTm2);

        // Get current date by click
        sDate.setOnClickListener(v ->
        {
            Date date = new Date();
            sDate.setText(getformDate(date));
        });

        // Get date picker result
        final DatePickerDialog.OnDateSetListener dpd = (view, year, monthOfYear, dayOfMonth) ->
        {
            pdate.set(Calendar.YEAR, year);
            pdate.set(Calendar.MONTH, monthOfYear);
            pdate.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            Date date = pdate.getTime();
            sDate.setText(getformDate(date));
        };

        // Select date by long click
        sDate.setOnLongClickListener(v ->
        {
            new DatePickerDialog(EditMetaActivity.this, dpd,
                pdate.get(Calendar.YEAR),
                pdate.get(Calendar.MONTH),
                pdate.get(Calendar.DAY_OF_MONTH)).show();
            return true;
        });

        // Get current start time
        sTime.setOnClickListener(v ->
        {
            Date date = new Date();
            sTime.setText(getformTime(date));
        });

        // Get start time picker result
        final TimePickerDialog.OnTimeSetListener stpd = (view, hourOfDay, minute) ->
        {
            ptime.set(Calendar.HOUR_OF_DAY, hourOfDay);
            ptime.set(Calendar.MINUTE, minute);
            Date date = ptime.getTime();
            sTime.setText(getformTime(date));
        };

        // Select start time
        sTime.setOnLongClickListener(v ->
        {
            new TimePickerDialog(EditMetaActivity.this, stpd,
                ptime.get(Calendar.HOUR_OF_DAY),
                ptime.get(Calendar.MINUTE),
                true).show();
            return true;
        });

        // Get current end time
        eTime.setOnClickListener(v ->
        {
            Date date = new Date();
            eTime.setText(getformTime(date));
        });

        // Get start time picker result
        final TimePickerDialog.OnTimeSetListener etpd = (view, hourOfDay, minute) ->
        {
            ptime.set(Calendar.HOUR_OF_DAY, hourOfDay);
            ptime.set(Calendar.MINUTE, minute);
            Date date = ptime.getTime();
            eTime.setText(getformTime(date));
        };

        // Select end time
        eTime.setOnLongClickListener(v ->
        {
            new TimePickerDialog(EditMetaActivity.this, etpd,
                ptime.get(Calendar.HOUR_OF_DAY),
                ptime.get(Calendar.MINUTE),
                true).show();
            return true;
        });
    }
    // End of onResume()

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.edit_meta, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle action bar item clicks here.
        int id = item.getItemId();
        if (id == android.R.id.home) // back button in actionBar
        {
            if (MyDebug.dLOG) Log.d(TAG, "296, MenuItem home");
            finish();
            return true;
        }

        if (id == R.id.menuSaveExit)
        {
            if (MyDebug.dLOG) Log.d(TAG, "303, MenuItem saveExit");
            if (saveData())
                finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPause()
    {
        super.onPause();

        if (MyDebug.dLOG) Log.i(TAG, "316, onPause");

        headDataSource.close();
        sectionDataSource.close();

        // Stop location service with permissions check
        locationPermissionDispatcherMode = 2;
        locationCaptureFragment();

        sDate.setOnClickListener(null);
        sDate.setOnLongClickListener(null);
        sTime.setOnClickListener(null);
        sTime.setOnLongClickListener(null);
        eTime.setOnClickListener(null);
        eTime.setOnLongClickListener(null);
    }

    @Override
    protected void onStop()
    {
        super.onStop();

        if (MyDebug.dLOG) Log.i(TAG, "338, onStop");
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();

        if (MyDebug.dLOG) Log.i(TAG, "346, onDestroy");
    }

    private boolean saveData()
    {
        if (MyDebug.dLOG) Log.i(TAG, "351, saveData");

        // Save head data
        head.observer = ett.getWidgetOName2();
        headDataSource.saveHead(head);

        // Save section data
        section.name = ett.getWidgetName();
        section.notes = ett.getWidgetONotes2();

        section.country = elw.setWidgetCo2();
        section.tmp = emw.getWidgetTemp2();
        section.tmp_end = emw.getWidgetTemp3();
        if (section.tmp > 50 || section.tmp_end > 50 || section.tmp < 0 || section.tmp_end < 0)
        {
            Snackbar sB = Snackbar.make(emw, getString(R.string.valTemp), Snackbar.LENGTH_LONG);
            sB.setActionTextColor(Color.RED);
            TextView tv = sB.getView().findViewById(R.id.snackbar_text);
            tv.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_START);
            tv.setTypeface(tv.getTypeface(), Typeface.BOLD);
            sB.show();
            return false;
        }

        section.wind = emw.getWidgetWind2();
        section.wind_end = emw.getWidgetWind3();
        if (section.wind > 4 || section.wind_end > 4 || section.wind < 0 || section.wind_end < 0)
        {
            Snackbar sB = Snackbar.make(emw, getString(R.string.valWind), Snackbar.LENGTH_LONG);
            sB.setActionTextColor(Color.RED);
            TextView tv = sB.getView().findViewById(R.id.snackbar_text);
            tv.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            tv.setTypeface(tv.getTypeface(), Typeface.BOLD);
            sB.show();
            return false;
        }

        section.clouds = emw.getWidgetClouds2();
        section.clouds_end = emw.getWidgetClouds3();
        if (section.clouds > 100 || section.clouds_end > 100 || section.clouds < 0 || section.clouds_end < 0)
        {
            Snackbar sB = Snackbar.make(emw, getString(R.string.valClouds), Snackbar.LENGTH_LONG);
            sB.setActionTextColor(Color.RED);
            TextView tv = sB.getView().findViewById(R.id.snackbar_text);
            tv.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_END);
            tv.setTypeface(tv.getTypeface(), Typeface.BOLD);
            sB.show();
            return false;
        }

        section.plz = elw.getWidgetPlz2();
        section.city = elw.getWidgetCity2();
        section.place = elw.getWidgetPlace2();
        section.date = emw.getWidgetDate2();
        section.start_tm = emw.getWidgetStartTm2();
        section.end_tm = emw.getWidgetEndTm2();

        sectionDataSource.saveSection(section);
        return true;
    }

    // Formatted date
    public static String getformDate(Date date)
    {
        DateFormat dform;
        String lng = Locale.getDefault().toString().substring(0, 2);

        if (lng.equals("de"))
        {
            dform = new SimpleDateFormat("dd.MM.yyyy", Locale.GERMAN);
        }
        else
        {
            dform = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        }
        return dform.format(date);
    }

    // Date for start_tm and end_tm
    public static String getformTime(Date date)
    {
        DateFormat dform = new SimpleDateFormat("HH:mm", Locale.US);
        return dform.format(date);
    }

    @Override
    public void locationCaptureFragment()
    {
        {
            if (isPermissionGranted())
            {
                switch (locationPermissionDispatcherMode)
                {
                case 1 -> // Get location
                    getLoc();
                case 2 -> // Stop location service
                    locationService.stopListener();
                }
            }
            else
            {
                if (locationPermissionDispatcherMode == 1)
                    PermissionsDialogFragment.newInstance().show(getSupportFragmentManager(), PermissionsDialogFragment.class.getName());
            }
        }
    }

    // If API level > 23 test for permissions granted
    private boolean isPermissionGranted()
    {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
            && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    // Get the location data
    public void getLoc()
    {
        locationService = new LocationService(this);

        if (locationService.canGetLocation())
        {
            longitude = locationService.getLongitude();
            latitude = locationService.getLatitude();
        }

        // Get reverse geocoding
        if (locationService.canGetLocation() && metaPref && (latitude != 0 || longitude != 0))
        {
            String urlString = "https://nominatim.openstreetmap.org/reverse?email=" + emailString
                + "&format=xml&lat=" + latitude + "&lon=" + longitude + "&zoom=18&addressdetails=1";

            // Implementation with WorkManager
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

}

