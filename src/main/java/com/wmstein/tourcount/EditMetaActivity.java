package com.wmstein.tourcount;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.wmstein.tourcount.database.Head;
import com.wmstein.tourcount.database.HeadDataSource;
import com.wmstein.tourcount.database.Section;
import com.wmstein.tourcount.database.SectionDataSource;
import com.wmstein.tourcount.widgets.EditHeadWidget;
import com.wmstein.tourcount.widgets.EditMetaWidget;
import com.wmstein.tourcount.widgets.EditTitleWidget;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**********************************************************
 * EditMetaActivity collects meta info for the current tour
 * Created by wmstein on 19.04.2016.
 */
public class EditMetaActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener
{
    private static String TAG = "tourcountEditMetaActivity";
    private TourCountApplication tourCount;
    SharedPreferences prefs;
    private Head head;
    private Section section;
    private LinearLayout head_area;
    private EditTitleWidget ett;
    private EditTitleWidget enw;
    private EditHeadWidget ehw;
    private EditMetaWidget etw;
    private Bitmap bMap;
    private BitmapDrawable bg;
    private HeadDataSource headDataSource;
    private SectionDataSource sectionDataSource;
    private boolean screenOrientL; // option for screen orientation

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        tourCount = (TourCountApplication) getApplication();
        prefs = TourCountApplication.getPrefs();
        prefs.registerOnSharedPreferenceChangeListener(this);
        boolean brightPref = prefs.getBoolean("pref_bright", true);
        screenOrientL = prefs.getBoolean("screen_Orientation", false);

        setContentView(R.layout.activity_edit_head);

        ScrollView editHead_screen = (ScrollView) findViewById(R.id.editHeadScreen);

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
        assert editHead_screen != null;
        bg = new BitmapDrawable(editHead_screen.getResources(), bMap);
        editHead_screen.setBackground(bg);

        head_area = (LinearLayout) findViewById(R.id.edit_head);

        //noinspection ConstantConditions
        getSupportActionBar().setTitle(getString(R.string.editHeadTitle));
    }

    @Override
    protected void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        prefs.registerOnSharedPreferenceChangeListener(this);
        screenOrientL = prefs.getBoolean("screen_Orientation", false);

        if (screenOrientL)
        {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        } else
        {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }

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

/*
        // check for focus
        String newCountry = section.country;
        if (StringUtils.isNotEmpty(newCountry))
        {
            etw.requestFocus();
        }
        else
        {
            ehw.requestFocus();
        }
*/
    }

    // getSDate()
    public void getSDate(View view)
    {
        ((InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE))
            .hideSoftInputFromWindow(view.getWindowToken(), 0);
        TextView sDate = (TextView) this.findViewById(R.id.widgetDate2);
        assert sDate != null;
        sDate.setText(getcurDate());
    }

    // getSTime()
    public void getSTime(View view)
    {
        ((InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE))
            .hideSoftInputFromWindow(view.getWindowToken(), 0);
        TextView sTime = (TextView) this.findViewById(R.id.widgetStartTm2);
        assert sTime != null;
        sTime.setText(getcurTime());
    }

    // getETime()
    public void getETime(View view)
    {
        ((InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE))
            .hideSoftInputFromWindow(view.getWindowToken(), 0);
        TextView eTime = (TextView) this.findViewById(R.id.widgetEndTm2);
        assert eTime != null;
        eTime.setText(getcurTime());
    }

    // Date for date
    // by wmstein
    @SuppressLint("SimpleDateFormat")
    private String getcurDate()
    {
        Date date = new Date();
        DateFormat dform;
        String lng = Locale.getDefault().toString().substring(0, 2);

        if (lng.equals("de"))
        {
            dform = new SimpleDateFormat("dd.MM.yyyy");
        }
        else
        {
            dform = new SimpleDateFormat("yyyy-MM-dd");
        }
        return dform.format(date);
    }

    // Date for start_tm and end_tm
    // by wmstein
    private String getcurTime()
    {
        Date date = new Date();
        @SuppressLint("SimpleDateFormat") 
        DateFormat dform = new SimpleDateFormat("HH:mm");
        return dform.format(date);
    }

    @Override
    protected void onPause()
    {
        super.onPause();

        // close the data sources
        headDataSource.close();
        sectionDataSource.close();
    }

    /***************/
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
            Toast.makeText(this, getString(R.string.valTemp), Toast.LENGTH_SHORT).show();
            return false;
        }
        section.wind = etw.getWidgetWind2();
        if (section.wind > 4 || section.wind < 0)
        {
            Toast.makeText(this, getString(R.string.valWind), Toast.LENGTH_SHORT).show();
            return false;
        }
        section.clouds = etw.getWidgetClouds2();
        if (section.clouds > 100 || section.clouds < 0)
        {
            Toast.makeText(this, getString(R.string.valClouds), Toast.LENGTH_SHORT).show();
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

    public void onSharedPreferenceChanged(SharedPreferences prefs, String key)
    {
        ScrollView editHead_screen = (ScrollView) findViewById(R.id.editHeadScreen);
        screenOrientL = prefs.getBoolean("screen_Orientation", false);
        if (screenOrientL)
        {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        } else
        {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
        bMap = tourCount.decodeBitmap(R.drawable.kbackground, tourCount.width, tourCount.height);
        assert editHead_screen != null;
        editHead_screen.setBackground(null);
        bg = new BitmapDrawable(editHead_screen.getResources(), bMap);
        editHead_screen.setBackground(bg);
    }

}