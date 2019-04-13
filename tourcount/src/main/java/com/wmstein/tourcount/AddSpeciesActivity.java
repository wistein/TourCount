/*
 * Copyright (c) 2019. Wilhelm Stein, Bonn, Germany.
 */

package com.wmstein.tourcount;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import com.wmstein.tourcount.database.CountDataSource;
import com.wmstein.tourcount.widgets.SpeciesAddWidget;

import java.lang.reflect.Field;
import java.util.Objects;

/***************************************************************
 * AddSpeciesActivity lets you insert a new species into the species list)
 * AddSpeciesActivity is called from EditSectionActivity
 * Uses SpeciesAddWidget.java, widget_add_spec.xml.
 *
 * Created for TourCount by wmstein on 2019-04-12,
 * last edited on 2019-04-13
 */
public class AddSpeciesActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener
{
    private static final String TAG = "TourCountAddSpecAct";
    private TourCountApplication tourCount;

    private LinearLayout add_area;

    // the actual data
    private CountDataSource countDataSource;

    private String[] idArray;
    private String[] nameArray;
    private String[] nameArrayG;
    private String[] codeArray;
    private Integer[] imageArray;

    String specName, specCode, specNameG;
    
    private Bitmap bMap;
    private BitmapDrawable bg;

    private boolean screenOrientL; // option for landscape screen orientation
    private boolean brightPref;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        tourCount = (TourCountApplication) getApplication();
        SharedPreferences prefs = TourCountApplication.getPrefs();
        prefs.registerOnSharedPreferenceChangeListener(this);
        brightPref = prefs.getBoolean("pref_bright", true);
        screenOrientL = prefs.getBoolean("screen_Orientation", false);

        setContentView(R.layout.activity_add_species);

        ScrollView add_screen = findViewById(R.id.addScreen);

        if (screenOrientL)
        {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }
        else
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

        bMap = tourCount.decodeBitmap(R.drawable.abackground, tourCount.width, tourCount.height);
        bg = new BitmapDrawable(add_screen.getResources(), bMap);
        add_screen.setBackground(bg);

        add_area = findViewById(R.id.addSpecLayout);

        nameArray = getResources().getStringArray(R.array.selSpecs);
        nameArrayG = getResources().getStringArray(R.array.selSpecs_g);
        codeArray = getResources().getStringArray(R.array.selCodes);
        idArray = setIdsSelSpecs(); // create idArray from codeArray
        imageArray = getAllImagesSelCodes(); // create imageArray
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

        SharedPreferences prefs = TourCountApplication.getPrefs();
        prefs.registerOnSharedPreferenceChangeListener(this);
        screenOrientL = prefs.getBoolean("screen_Orientation", false);
        brightPref = prefs.getBoolean("pref_bright", true);

        // Set full brightness of screen
        if (brightPref)
        {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            WindowManager.LayoutParams params = getWindow().getAttributes();
            params.screenBrightness = 1.0f;
            getWindow().setAttributes(params);
        }

        // clear any existing views
        add_area.removeAllViews();

        // setup the data sources
        countDataSource = new CountDataSource(this);
        countDataSource.open();

        Objects.requireNonNull(getSupportActionBar()).setTitle(R.string.addTitle);

        // load the species data to the widgets
        int i;
        for (i = 0; i < codeArray.length; i++)
        {
            SpeciesAddWidget saw = new SpeciesAddWidget(this, null);

            saw.setSpecName(nameArray[i]);
            saw.setSpecNameG(nameArrayG[i]);
            saw.setSpecCode(codeArray[i]);
            saw.setPSpec(codeArray[i]);
            saw.setSpecId(idArray[i]);
            add_area.addView(saw);
        }

    } // end of Resume

    private String[] setIdsSelSpecs()
    {
        int i;
        idArray = new String[codeArray.length];
        for (i = 0; i < codeArray.length; i++)
        {
            idArray[i] = String.valueOf(i + 1);
        }
        return idArray;
    }

    private Integer[] getAllImagesSelCodes()
    {
        int i;
        imageArray = new Integer[codeArray.length];
        for (i = 0; i < codeArray.length; i++)
        {
            String ucode = codeArray[i];

            String rname = "p" + ucode; // species picture resource name
            int resId = getResId(rname);
            int resId0 = getResId("p00000");

            if (resId != 0)
            {
                imageArray[i] = resId;
            }
            else
            {
                imageArray[i] = resId0;
            }
        }
        return imageArray;
    }

    // Get resource ID from resource name
    private int getResId(String rName)
    {
        try
        {
            Class res = R.drawable.class;
            Field idField = res.getField(rName);
            return idField.getInt(null);
        } catch (Exception e)
        {
            return 0;
        }
    }

    @Override
    protected void onPause()
    {
        super.onPause();

        // close the data sources
        countDataSource.close();
    }

    public void saveAndExit(View view)
    {
        if (saveData(view))
        {
            super.finish();
        }
    }

    private boolean saveData(View view)
    {
        // save added species to species list
        boolean retValue = true;

        int idToAdd = (Integer) view.getTag();
        SpeciesAddWidget saw1 = (SpeciesAddWidget) add_area.getChildAt(idToAdd);
        
        specName = saw1.getSpecName();
        specCode = saw1.getSpecCode();
        specNameG = saw1.getSpecNameG();
            
        try
        {
            countDataSource.createCount(specName, specCode, specNameG);
        } catch (Exception e)
        {
            retValue = false;
        }
        return retValue;
    }

    /*
     * Add the selected species to the species list
     */
    public void addCount(View view)
    {
        if (saveData(view))
        {
            super.finish();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.add_species, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.home)
        {
            Intent intent = NavUtils.getParentActivityIntent(this);
            assert intent != null;
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            NavUtils.navigateUpTo(this, intent);
        }
        return super.onOptionsItemSelected(item);
    }

    public void onSharedPreferenceChanged(SharedPreferences prefs, String key)
    {
        ScrollView add_screen = findViewById(R.id.addScreen);
        screenOrientL = prefs.getBoolean("screen_Orientation", false);
        if (screenOrientL)
        {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }
        else
        {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }

        bMap = tourCount.decodeBitmap(R.drawable.abackground, tourCount.width, tourCount.height);
        add_screen.setBackground(null);
        bg = new BitmapDrawable(add_screen.getResources(), bMap);
        add_screen.setBackground(bg);
    }

}
