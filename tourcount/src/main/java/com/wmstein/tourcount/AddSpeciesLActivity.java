/*
 * Copyright (c) 2019. Wilhelm Stein, Bonn, Germany.
 */

package com.wmstein.tourcount;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import com.wmstein.tourcount.database.Count;
import com.wmstein.tourcount.database.CountDataSource;
import com.wmstein.tourcount.widgets.SpeciesAddWidget;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NavUtils;

/************************************************************************
 * AddSpeciesLActivity lets you insert a new species into the species list
 * AddSpeciesLActivity is called from EditSpecListLActivity
 * Uses SpeciesAddWidget.java, widget_add_spec.xml.
 *
 * The sorting order of the species to add cannot be changed, as it is determined 
 * by 3 interdependent and correlated arrays in arrays.xml
 *
 * Created for TourCount by wmstein on 2022-05-21,
 * last edited on 2022-05-21
 */
public class AddSpeciesLActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener
{
    @SuppressLint("StaticFieldLeak")
    private static TourCountApplication tourCount;

    private LinearLayout add_area;

    // the actual data
    private CountDataSource countDataSource;

    private String[] idArray; // Id list of missing species
    ArrayList<String> namesCompleteArrayList, namesGCompleteArrayList, codesCompleteArrayList; // complete ArrayLists of species
    String specName, specCode, specNameG; // selected species

    private Bitmap bMap;
    private BitmapDrawable bg;

    private boolean brightPref;

    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        tourCount = (TourCountApplication) getApplication();
        SharedPreferences prefs = TourCountApplication.getPrefs();
        prefs.registerOnSharedPreferenceChangeListener(this);
        brightPref = prefs.getBoolean("pref_bright", true);

        setContentView(R.layout.activity_add_species);
        ScrollView add_screen = findViewById(R.id.addScreen);

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
        
        // Load complete species ArrayList from arrays.xml (lists are sorted by code)
        namesCompleteArrayList = new ArrayList<>(Arrays.asList(getResources().getStringArray(R.array.selSpecs)));
        namesGCompleteArrayList = new ArrayList<>(Arrays.asList(getResources().getStringArray(R.array.selSpecs_g)));
        codesCompleteArrayList = new ArrayList<>(Arrays.asList(getResources().getStringArray(R.array.selCodes)));
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        SharedPreferences prefs = TourCountApplication.getPrefs();
        prefs.registerOnSharedPreferenceChangeListener(this);
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

        // get the list of only new species not already contained in the species counting list
        List<Count> counts;
        ArrayList<String> specCodesContainedList = new ArrayList<>(); // code list of contained species

        counts = countDataSource.getAllSpeciesSrtCode(); // get species of the counting list

        // build code ArrayList of already contained species
        for (Count count : counts)
        {
            specCodesContainedList.add(count.code);
        }
        
        // build lists of missing species
        int specCodesContainedListSize = specCodesContainedList.size();
        int posSpec;
        
        // for already contained species reduce complete arraylists
        for (int i = 0; i < specCodesContainedListSize; i++)
        {
            if (codesCompleteArrayList.contains(specCodesContainedList.get(i)))
            {
                // Remove species with code x from missing species lists.
                // Prerequisites: exactly correlated arrays of selCodes, selSpecs and selSpecs_g
                //   for all localisations
                specCode = specCodesContainedList.get(i);
                posSpec = codesCompleteArrayList.indexOf(specCode);

                namesCompleteArrayList.remove(posSpec);
                namesGCompleteArrayList.remove(posSpec);
                codesCompleteArrayList.remove(specCode);
            }
        }

        idArray = setIdsSelSpecs(codesCompleteArrayList); // create idArray from codeArray

        // load the species data into the widgets
        int i;
        for (i = 0; i < codesCompleteArrayList.size(); i++)
        {
            SpeciesAddWidget saw = new SpeciesAddWidget(this, null);

            saw.setSpecName(namesCompleteArrayList.get(i));
            saw.setSpecNameG(namesGCompleteArrayList.get(i));
            saw.setSpecCode(codesCompleteArrayList.get(i));
            saw.setPSpec(codesCompleteArrayList.get(i));
            saw.setSpecId(idArray[i]);
            add_area.addView(saw);
        }

    } // end of Resume


    // create idArray from codeArray
    private String[] setIdsSelSpecs(ArrayList<String> speccodesm)
    {
        int i;
        idArray = new String[speccodesm.size()];
        for (i = 0; i < speccodesm.size(); i++)
        {
            idArray[i] = String.valueOf(i + 1);
        }
        return idArray;
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

    @SuppressLint("SourceLockedOrientationActivity")
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key)
    {
        ScrollView add_screen = findViewById(R.id.addScreen);

        bMap = tourCount.decodeBitmap(R.drawable.abackground, tourCount.width, tourCount.height);
        add_screen.setBackground(null);
        bg = new BitmapDrawable(add_screen.getResources(), bMap);
        add_screen.setBackground(bg);
    }

}
