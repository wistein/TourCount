package com.wmstein.tourcount;

import static com.wmstein.tourcount.Utils.fromHtml;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.SharedPreferences;
import android.content.res.Resources;
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
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

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
 * Copyright 2016-2026 wmstein
 * Created by wmstein on 2016-04-19,
 * last edit in Java on 2026-01-24
 */
public class EditMetaActivity extends AppCompatActivity
{
    private static final String TAG = "EditMetaAct";

    // Preferences
    private final SharedPreferences prefs = TourCountApplication.getPrefs();

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

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "77, onCreate");

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

        // Set full brightness of screen
        if (brightPref)
        {
            WindowManager.LayoutParams params = getWindow().getAttributes();
            params.screenBrightness = 1.0f;
            getWindow().setAttributes(params);
        }

        head_area = findViewById(R.id.edit_head);

        Objects.requireNonNull(getSupportActionBar()).setTitle(getString(R.string.editHeadTitle));

        headDataSource = new HeadDataSource(this);
        sectionDataSource = new SectionDataSource(this);

        // New onBackPressed logic
        if (getNavBarMode() == 0 || getNavBarMode() == 1) {
            OnBackPressedCallback callback = new OnBackPressedCallback(true) {
                @Override
                public void handleOnBackPressed() {
                    if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                        Log.i(TAG, "121, handleOnBackPressed");
                    finish();
                    remove();
                }
            };
            getOnBackPressedDispatcher().addCallback(this, callback);
        }
    }
    // End of onCreate()

    // Check for Navigation bar 1-, 2- or 3-button mode
    public int getNavBarMode() {
        Resources resources = this.getResources();
        @SuppressLint("DiscouragedApi")
        int resourceId = resources.getIdentifier("config_navBarInteractionMode",
                "integer", "android");
        return resourceId > 0 ? resources.getInteger(resourceId) : 0;
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "144, onResume");

        // Setup data sources
        headDataSource.open();
        sectionDataSource.open();

        // Clear existing view
        head_area.removeAllViews();

        // Load head and meta data
        head = headDataSource.getHead();
        section = sectionDataSource.getSection();

        // Display editable list title, observer name and notes by EditTitleWidget
        ett = new EditTitleWidget(getApplicationContext(), null);
        ett.setWidgetTitle(getString(R.string.titleEdit));
        ett.setWidgetName(section.name);
        ett.setWidgetOName1(getString(R.string.inspector));
        ett.setWidgetOName2(head.observer);
        ett.setWidgetONotes1(getString(R.string.titleTourNotes));
        ett.setWidgetONotes2(section.notes);
        head_area.addView(ett);

        // Display the editable location data by EditLocationWidget
        elw = new EditLocationWidget(getApplicationContext(), null);
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

        // Display the editable meta data by EditMetaWidget
        emw = new EditMetaWidget(getApplicationContext(), null);
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

        sDate = findViewById(R.id.widgetDate2);
        sTime = findViewById(R.id.widgetStartTm2);
        eTime = findViewById(R.id.widgetEndTm2);

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
            if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                Log.d(TAG, "321, MenuItem home");
            finish();
            return true;
        }
        else if (id == R.id.menuSaveExit)
        {
            if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                Log.d(TAG, "328, MenuItem saveExit");
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

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "342, onPause");

        headDataSource.close();
        sectionDataSource.close();

        sDate.setOnClickListener(null);
        sDate.setOnLongClickListener(null);
        sTime.setOnClickListener(null);
        sTime.setOnLongClickListener(null);
        eTime.setOnClickListener(null);
        eTime.setOnLongClickListener(null);

        head_area.clearFocus();
        head_area.removeAllViews();
    }

    @Override
    protected void onStop()
    {
        super.onStop();

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "364, onStop");

        head_area = null;
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "375, onDestroy");
    }

    private boolean saveData()
    {
        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "381, saveData");

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
            Toast.makeText(this, //orange
                    fromHtml("<font color='#ff6000'><b>" + mesg + "</b></font>"),
                    Toast.LENGTH_LONG).show();
            return false;
        }

        section.wind = emw.getWidgetWind2();
        section.wind_end = emw.getWidgetWind3();
        if (section.wind > 4 || section.wind_end > 4 || section.wind < 0 || section.wind_end < 0)
        {
            mesg = getString(R.string.valWind);
            Toast.makeText(this, // orange
                    fromHtml("<font color='#ff6000'><b>" + mesg + "</b></font>"),
                    Toast.LENGTH_LONG).show();
            return false;
        }

        section.clouds = emw.getWidgetClouds2();
        section.clouds_end = emw.getWidgetClouds3();
        if (section.clouds > 100 || section.clouds_end > 100 || section.clouds < 0 || section.clouds_end < 0)
        {
            mesg = getString(R.string.valClouds);
            Toast.makeText(this, // orange
                    fromHtml("<font color='#ff6000'><b>" + mesg + "</b></font>"),
                    Toast.LENGTH_LONG).show();
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) // sdk 35
            return cs == null || cs.isEmpty();
        else
            return cs == null || cs.length() == 0; // needed for older Android versions
    }

}

