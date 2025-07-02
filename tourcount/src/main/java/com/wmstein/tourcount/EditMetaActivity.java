package com.wmstein.tourcount;

import android.Manifest;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.text.HtmlCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;

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

/**********************************************************
 * EditMetaActivity collects meta info for the current tour
 * Created by wmstein on 2016-04-19,
 * last edit in Java on 2025-06-28
 */
public class EditMetaActivity extends AppCompatActivity
{
    private static final String TAG = "EditMetaAct";

    // Preferences
    private final SharedPreferences prefs = TourCountApplication.getPrefs();
    private boolean metaPref;        // option for OSM reverse geocoding
    private String emailString = ""; // mail address for OSM query

    // Database
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

    // locationDispatcherMode:
    //  1 = use location service
    //  2 = end location service
    int locationDispatcherMode;

    private boolean locServiceOn = false;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        if (MyDebug.DLOG) Log.i(TAG, "95, onCreate");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) // SDK 35+
        {
            EdgeToEdge.enable(this);
        }
        setContentView(R.layout.activity_edit_meta);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.editHeadScreen),
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

        // Option for full bright screen
        boolean brightPref = prefs.getBoolean("pref_bright", true);
        metaPref = prefs.getBoolean("pref_metadata", false); // use Reverse Geocoding
        emailString = prefs.getString("email_String", ""); // for reliable query of Nominatim service

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
                if (MyDebug.DLOG) Log.i(TAG, "142, handleOnBackPressed");
                finish();
                remove();
            }
        };
        getOnBackPressedDispatcher().addCallback(this, callback);
    }
    // End of onCreate()

    @Override
    protected void onResume()
    {
        super.onResume();

        if (MyDebug.DLOG) Log.i(TAG, "156, onResume");

        // Get location with permissions check
        locationDispatcherMode = 1;
        locationDispatcher();

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
        elw.setWidgetState1(getString(R.string.bstate));
        elw.setWidgetState2(section.b_state);
        elw.setWidgetPlz1(getString(R.string.plz));
        elw.setWidgetPlz2(section.plz);
        elw.setWidgetCity1(getString(R.string.city));
        elw.setWidgetCity2(section.city);
        elw.setWidgetPlace1(getString(R.string.place));
        elw.setWidgetPlace2(section.place);
        elw.setWidgetLocality1(getString(R.string.slocality));
        elw.setWidgetLocality2(section.st_locality);

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

        // Check for focus
        String sName = section.name;
        if (isNotEmpty(sName))
        {
            emw.requestFocus();
        }
        else
        {
            ett.requestFocus();
        }

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
            if (MyDebug.DLOG) Log.d(TAG, "336, MenuItem home");
            finish();
            return true;
        }
        else if (id == R.id.menuSaveExit)
        {
            if (MyDebug.DLOG) Log.d(TAG, "342, MenuItem saveExit");
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

        if (MyDebug.DLOG) Log.i(TAG, "355, onPause");

        headDataSource.close();
        sectionDataSource.close();

        sDate.setOnClickListener(null);
        sDate.setOnLongClickListener(null);
        sTime.setOnClickListener(null);
        sTime.setOnLongClickListener(null);
        eTime.setOnClickListener(null);
        eTime.setOnLongClickListener(null);

        // Stop RetrieveAddrWorker
        WorkManager.getInstance(this).cancelAllWork();

        // Stop location service with permissions check
        locationDispatcherMode = 2;
        locationDispatcher();
    }

    @Override
    protected void onStop()
    {
        super.onStop();

        if (MyDebug.DLOG) Log.i(TAG, "380, onStop");
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();

        if (MyDebug.DLOG) Log.i(TAG, "388, onDestroy");

        head_area.clearFocus();
        head_area.removeAllViews();
    }

    private boolean saveData()
    {
        if (MyDebug.DLOG) Log.i(TAG, "396, saveData");

        // Save head data
        head.observer = ett.getWidgetOName2();
        headDataSource.saveHead(head);

        String mesg;

        // Save section data
        section.name = ett.getWidgetName();
        section.notes = ett.getWidgetONotes2();

        section.country = elw.getWidgetCo2();
        section.b_state = elw.getWidgetState2();
        section.city = elw.getWidgetCity2();
        section.place = elw.getWidgetPlace2();
        section.st_locality = elw.getWidgetLocality2();
        section.plz = elw.getWidgetPlz2();

        section.tmp = emw.getWidgetTemp2();
        section.tmp_end = emw.getWidgetTemp3();
        if (section.tmp > 50 || section.tmp_end > 50 || section.tmp < 0 || section.tmp_end < 0)
        {
            mesg = getString(R.string.valTemp);
            Toast.makeText(this,
                    HtmlCompat.fromHtml("<font color='red'><b>" + mesg + "</b></font>",
                            HtmlCompat.FROM_HTML_MODE_LEGACY), Toast.LENGTH_LONG).show();
            return false;
        }

        section.wind = emw.getWidgetWind2();
        section.wind_end = emw.getWidgetWind3();
        if (section.wind > 4 || section.wind_end > 4 || section.wind < 0 || section.wind_end < 0)
        {
            mesg = getString(R.string.valWind);
            Toast.makeText(this,
                    HtmlCompat.fromHtml("<font color='red'><b>" + mesg + "</b></font>",
                            HtmlCompat.FROM_HTML_MODE_LEGACY), Toast.LENGTH_LONG).show();
            return false;
        }

        section.clouds = emw.getWidgetClouds2();
        section.clouds_end = emw.getWidgetClouds3();
        if (section.clouds > 100 || section.clouds_end > 100 || section.clouds < 0 || section.clouds_end < 0)
        {
            mesg = getString(R.string.valClouds);
            Toast.makeText(this,
                    HtmlCompat.fromHtml("<font color='red'><b>" + mesg + "</b></font>",
                            HtmlCompat.FROM_HTML_MODE_LEGACY), Toast.LENGTH_LONG).show();
            return false;
        }

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

    public void locationDispatcher()
    {
        if (isFineLocationPermGranted())
        {
            switch (locationDispatcherMode)
            {
                case 1 -> // Get location
                    getLoc();
                case 2 -> // Stop location service
                {
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

    private boolean isFineLocationPermGranted()
    {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED;
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
        }

        // Get reverse geocoding
        if (locationService.canGetLocation() && metaPref && (latitude != 0 || longitude != 0))
        {
            String urlString = "https://nominatim.openstreetmap.org/reverse?email=" + emailString
                + "&format=xml&lat=" + latitude + "&lon=" + longitude + "&zoom=18&addressdetails=1";

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

    /**
     * Following functions are taken from the Apache commons-lang3-3.4 library
     * licensed under Apache License Version 2.0, January 2004
     <p>
     * Checks if a CharSequence is not empty ("") and not null.
     <p>
     * isNotEmpty(null)      = false
     * isNotEmpty("")        = false
     * isNotEmpty(" ")       = true
     * isNotEmpty("bob")     = true
     * isNotEmpty("  bob  ") = true
     *
     * @param cs the CharSequence to check, may be null
     * @return {@code true} if the CharSequence is not empty and not null
     */
    public static boolean isNotEmpty(final CharSequence cs)
    {
        return !isEmpty(cs);
    }

    /**
     * Checks if a CharSequence is empty ("") or null.
     <p>
     * isEmpty(null)      = true
     * isEmpty("")        = true
     * isEmpty(" ")       = false
     * isEmpty("bob")     = false
     * isEmpty("  bob  ") = false
     *
     * @param cs the CharSequence to check, may be null
     * @return {@code true} if the CharSequence is empty or null
     */
    public static boolean isEmpty(final CharSequence cs)
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM)
            return cs == null || cs.isEmpty();
        else
            return cs == null || cs.length() == 0; // needed for older Android versions
    }

}

