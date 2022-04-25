package com.wmstein.tourcount;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.text.Html;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.android.material.snackbar.Snackbar;
import com.wmstein.tourcount.database.Head;
import com.wmstein.tourcount.database.HeadDataSource;
import com.wmstein.tourcount.database.Section;
import com.wmstein.tourcount.database.SectionDataSource;
import com.wmstein.tourcount.widgets.EditHeadWidget;
import com.wmstein.tourcount.widgets.EditMetaWidget;
import com.wmstein.tourcount.widgets.EditTitleWidget;

import java.io.IOException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NavUtils;
import androidx.core.content.ContextCompat;

/**********************************************************
 * EditMetaActivity collects meta info for the current tour
 * Created by wmstein on 2016-04-19,
 * last edit on 2022-04-25
 */
public class EditMetaActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener, PermissionsDialogFragment.PermissionsGrantedCallback
{
    @SuppressLint("StaticFieldLeak")
    private static TourCountApplication tourCount;

    SharedPreferences prefs;
    private boolean brightPref;    // option for full bright screen
    private boolean screenOrientL; // option for screen orientation
    private boolean metaPref;      // option for reverse geocoding
    private String emailString = ""; // mail address for OSM query
    
    private Head head;
    private Section section;
    private HeadDataSource headDataSource;
    private SectionDataSource sectionDataSource;

    private Calendar pdate, ptime;

    private LinearLayout head_area;
    private TextView sDate, sTime, eTime;

    private EditTitleWidget ett;
    private EditTitleWidget enw;
    private EditHeadWidget ehw;
    private EditMetaWidget etw;

    // Location info handling
    private double latitude;
    private double longitude;
    LocationService locationService;

    // Permission dispatcher mode modePerm: 
    //  1 = use location service
    //  2 = end location service
    int modePerm;

    private Bitmap bMap;
    private BitmapDrawable bg;

    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        tourCount = (TourCountApplication) getApplication();
        prefs = TourCountApplication.getPrefs();
        prefs.registerOnSharedPreferenceChangeListener(this);
        brightPref = prefs.getBoolean("pref_bright", true);
        screenOrientL = prefs.getBoolean("screen_Orientation", false);

        setContentView(R.layout.activity_edit_head);
        ScrollView editHead_screen = findViewById(R.id.editHeadScreen);

        if (screenOrientL)
        {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        } else
        {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }

        // Set full brightness of screen
        if (brightPref)
        {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            WindowManager.LayoutParams params = getWindow().getAttributes();
            params.screenBrightness = 1.0f;
            getWindow().setAttributes(params);
        }

        bMap = tourCount.decodeBitmap(R.drawable.kbackground, tourCount.width, tourCount.height);
        bg = new BitmapDrawable(editHead_screen.getResources(), bMap);
        editHead_screen.setBackground(bg);

        head_area = findViewById(R.id.edit_head);

        Objects.requireNonNull(getSupportActionBar()).setTitle(getString(R.string.editHeadTitle));
    }

    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    protected void onResume()
    {
        super.onResume();

        prefs = TourCountApplication.getPrefs();
        prefs.registerOnSharedPreferenceChangeListener(this);
        screenOrientL = prefs.getBoolean("screen_Orientation", false);
        metaPref = prefs.getBoolean("pref_metadata", false);   // use Reverse Geocoding
        emailString = prefs.getString("email_String", "");     // for reliable query of Nominatim service

        // Get location with permissions check
        modePerm = 1;
        permissionCaptureFragment();

        //clear existing view
        head_area.removeAllViews();

        //setup data sources
        headDataSource = new HeadDataSource(this);
        headDataSource.open();
        sectionDataSource = new SectionDataSource(this);
        sectionDataSource.open();

        //load head and meta data
        head = headDataSource.getHead();
        section = sectionDataSource.getSection();

        // display an editable list title
        ett = new EditTitleWidget(this, null);
        ett.setSectionName(section.name);
        ett.setWidgetTitle(getString(R.string.titleEdit));
        head_area.addView(ett);

        // display editable section notes; the same class
        enw = new EditTitleWidget(this, null);
        enw.setSectionName(section.notes);
        enw.setWidgetTitle(getString(R.string.notesHere));
        enw.setHint(getString(R.string.notesHint));
        head_area.addView(enw);

        // display the editable head data
        ehw = new EditHeadWidget(this, null);
        ehw.setWidgetCo1(getString(R.string.country));
        ehw.setWidgetCo2(section.country);
        ehw.setWidgetName1(getString(R.string.inspector));
        ehw.setWidgetName2(head.observer);
        head_area.addView(ehw);

        // display the editable meta data
        etw = new EditMetaWidget(this, null);
        etw.setWidgetTemp1(getString(R.string.temperature));
        etw.setWidgetTemp2(section.temp);
        etw.setWidgetWind1(getString(R.string.wind));
        etw.setWidgetWind2(section.wind);
        etw.setWidgetClouds1(getString(R.string.clouds));
        etw.setWidgetClouds2(section.clouds);
        etw.setWidgetPlz1(getString(R.string.plz));
        etw.setWidgetPlz2(section.plz);
        etw.setWidgetCity1(getString(R.string.city));
        etw.setWidgetCity2(section.city);
        etw.setWidgetPlace1(getString(R.string.place));
        etw.setWidgetPlace2(section.place);
        etw.setWidgetDate1(getString(R.string.date));
        etw.setWidgetDate2(section.date);
        etw.setWidgetStartTm1(getString(R.string.starttm));
        etw.setWidgetStartTm2(section.start_tm);
        etw.setWidgetEndTm1(getString(R.string.endtm));
        etw.setWidgetEndTm2(section.end_tm);
        head_area.addView(etw);

        pdate = Calendar.getInstance();
        ptime = Calendar.getInstance();

        sDate = this.findViewById(R.id.widgetDate2);
        sTime = this.findViewById(R.id.widgetStartTm2);
        eTime = this.findViewById(R.id.widgetEndTm2);

        // get current date by click
        sDate.setOnClickListener(v -> {
            Date date = new Date();
            sDate.setText(getformDate(date));
        });

        // get date picker result
        final DatePickerDialog.OnDateSetListener dpd = (view, year, monthOfYear, dayOfMonth) -> {
            pdate.set(Calendar.YEAR, year);
            pdate.set(Calendar.MONTH, monthOfYear);
            pdate.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            Date date = pdate.getTime();
            sDate.setText(getformDate(date));
        };

        // select date by long click
        sDate.setOnLongClickListener(v -> {
            new DatePickerDialog(EditMetaActivity.this, dpd,
                pdate.get(Calendar.YEAR),
                pdate.get(Calendar.MONTH),
                pdate.get(Calendar.DAY_OF_MONTH)).show();
            return true;
        });

        // get current start time
        sTime.setOnClickListener(v -> {
            Date date = new Date();
            sTime.setText(getformTime(date));
        });

        // get start time picker result
        final TimePickerDialog.OnTimeSetListener stpd = (view, hourOfDay, minute) -> {
            ptime.set(Calendar.HOUR_OF_DAY, hourOfDay);
            ptime.set(Calendar.MINUTE, minute);
            Date date = ptime.getTime();
            sTime.setText(getformTime(date));
        };

        // select start time
        sTime.setOnLongClickListener(v -> {
            new TimePickerDialog(EditMetaActivity.this, stpd,
                ptime.get(Calendar.HOUR_OF_DAY),
                ptime.get(Calendar.MINUTE),
                true).show();
            return true;
        });

        // get current end time
        eTime.setOnClickListener(v -> {
            Date date = new Date();
            eTime.setText(getformTime(date));
        });

        // get start time picker result
        final TimePickerDialog.OnTimeSetListener etpd = (view, hourOfDay, minute) -> {
            ptime.set(Calendar.HOUR_OF_DAY, hourOfDay);
            ptime.set(Calendar.MINUTE, minute);
            Date date = ptime.getTime();
            eTime.setText(getformTime(date));
        };

        // select end time
        eTime.setOnLongClickListener(v -> {
            new TimePickerDialog(EditMetaActivity.this, etpd,
                ptime.get(Calendar.HOUR_OF_DAY),
                ptime.get(Calendar.MINUTE),
                true).show();
            return true;
        });
    }

    // formatted date
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

    // date for start_tm and end_tm
    public static String getformTime(Date date)
    {
        DateFormat dform = new SimpleDateFormat("HH:mm", Locale.US);
        return dform.format(date);
    }
    
    @Override
    protected void onPause()
    {
        super.onPause();

        // close the data sources
        headDataSource.close();
        sectionDataSource.close();

        // Stop location service with permissions check
        modePerm = 2;
        permissionCaptureFragment();
    }

    // triggered by save button in actionbar
    public void saveAndExit(View view)
    {
        if (saveData())
            super.finish();
    }

    private boolean saveData()
    {
        // Save head data
        head.observer = ehw.setWidgetName2();

        headDataSource.saveHead(head);

        // Save meta data
        section.name = ett.getSectionName();
        section.notes = enw.getSectionName();

        section.country = ehw.setWidgetCo2();
        section.temp = etw.getWidgetTemp2();
        if (section.temp > 50 || section.temp < 0)
        {
            Snackbar sB = Snackbar.make(etw, Html.fromHtml("<font color=\"#ff0000\"><b>" +  getString(R.string.valTemp) + "</font></b>"), Snackbar.LENGTH_LONG);
            TextView tv = sB.getView().findViewById(R.id.snackbar_text);
            tv.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_START);
            sB.show();
            return false;
        }
        section.wind = etw.getWidgetWind2();
        if (section.wind > 4 || section.wind < 0)
        {
            Snackbar sB = Snackbar.make(etw, Html.fromHtml("<font color=\"#ff0000\"><b>" +  getString(R.string.valWind) + "</font></b>"), Snackbar.LENGTH_LONG);
            TextView tv = sB.getView().findViewById(R.id.snackbar_text);
            tv.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            sB.show();
            return false;
        }
        section.clouds = etw.getWidgetClouds2();
        if (section.clouds > 100 || section.clouds < 0)
        {
            Snackbar sB = Snackbar.make(etw, Html.fromHtml("<font color=\"#ff0000\"><b>" +  getString(R.string.valClouds) + "</font></b>"), Snackbar.LENGTH_LONG);
            TextView tv = sB.getView().findViewById(R.id.snackbar_text);
            tv.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_END);
            sB.show();
            return false;
        }
        section.plz = etw.getWidgetPlz2();
        section.city = etw.getWidgetCity2();
        section.place = etw.getWidgetPlace2();
        section.date = etw.getWidgetDate2();
        section.start_tm = etw.getWidgetStartTm2();
        section.end_tm = etw.getWidgetEndTm2();

        sectionDataSource.saveSection(section);
        return true;
    }

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
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.menuSaveExit)
        {
            if (saveData())
                super.finish();
        }
        return super.onOptionsItemSelected(item);
    }

    // puts up function to back button
    @Override
    public void onBackPressed()
    {
        NavUtils.navigateUpFromSameTask(this);
        super.onBackPressed();
    }

    @SuppressLint("SourceLockedOrientationActivity")
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key)
    {
        ScrollView editHead_screen = findViewById(R.id.editHeadScreen);
        screenOrientL = prefs.getBoolean("screen_Orientation", false);
        if (screenOrientL)
        {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        } else
        {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
        bMap = tourCount.decodeBitmap(R.drawable.kbackground, tourCount.width, tourCount.height);
        editHead_screen.setBackground(null);
        bg = new BitmapDrawable(editHead_screen.getResources(), bMap);
        editHead_screen.setBackground(bg);
    }

    @Override
    public void permissionCaptureFragment()
    {
        {
            if (isPermissionGranted())
            {
                switch (modePerm)
                {
                case 1: // get location
                    getLoc();
                    break;
                case 2: // stop location service
                    locationService.stopListener();
                    break;
                }
            }
            else
            {
                if (modePerm == 1)
                    PermissionsDialogFragment.newInstance().show(getSupportFragmentManager(), PermissionsDialogFragment.class.getName());
            }
        }
    }

    // if API level > 23 test for permissions granted
    private boolean isPermissionGranted()
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
        {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        }
        else
        {
            // handle permissions for Build.VERSION_CODES < M here
            return true;
        }
    }

    // get the location data
    public void getLoc()
    {
        locationService = new LocationService(this);

        if (locationService.canGetLocation())
        {
            longitude = locationService.getLongitude();
            latitude = locationService.getLatitude();
        }

        // get reverse geocoding
        if (locationService.canGetLocation() && metaPref && (latitude != 0 || longitude != 0))
        {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);

            runOnUiThread(() -> {
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
            });
        }
    }

}

