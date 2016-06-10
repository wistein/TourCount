/*
 * Copyright (c) 2016. Wilhelm Stein, Bonn, Germany.
 */

package com.wmstein.tourcount;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Toast;

import com.wmstein.tourcount.database.Individuals;
import com.wmstein.tourcount.database.IndividualsDataSource;
import com.wmstein.tourcount.database.Temp;
import com.wmstein.tourcount.database.TempDataSource;
import com.wmstein.tourcount.widgets.EditIndividualWidget;

/**
 * Created by wmstein on 15.05.2016
 */

/***********************************************************************************************************************/
// EditIndividualActivity is called from CountingActivity 
public class EditIndividualActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener
{
    TourCountApplication tourCount;
    SharedPreferences prefs;
    public static String TAG = "TourCountEditIndividualActivity";

    Individuals individuals;
    Temp temp;

    // the actual data
    private IndividualsDataSource individualsDataSource;
    private TempDataSource tempDataSource;

    LinearLayout individ_area;

    EditIndividualWidget eiw;

    private Bitmap bMap;
    private BitmapDrawable bg;

    private int i_id;
    private String specName, latitude, longitude;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_individual);

        individ_area = (LinearLayout) findViewById(R.id.edit_individual);

        // get parameters from CountingActivity
        Bundle extras = getIntent().getExtras();
        if (extras != null)
        {
            i_id = extras.getInt("indiv_id");
            specName = extras.getString("SName");
            latitude = extras.getString("Latitude");
            longitude = extras.getString("Longitude");
        }

        tourCount = (TourCountApplication) getApplication();

        prefs = TourCountApplication.getPrefs();
        prefs.registerOnSharedPreferenceChangeListener(this);

        ScrollView individ_screen = (ScrollView) findViewById(R.id.editIndividualScreen);
        bMap = tourCount.decodeBitmap(R.drawable.kbackground, tourCount.width, tourCount.height);
        bg = new BitmapDrawable(individ_screen.getResources(), bMap);
        individ_screen.setBackground(bg);

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

        // clear any existing views
        individ_area.removeAllViews();

        // setup the data sources
        individualsDataSource = new IndividualsDataSource(this);
        individualsDataSource.open();
        tempDataSource = new TempDataSource(this);
        tempDataSource.open();

        String[] stateArray = {
            getString(R.string.stadium_1),
            getString(R.string.stadium_2),
            getString(R.string.stadium_3),
            getString(R.string.stadium_4)
        };

        ArrayAdapter<String> adapter = new ArrayAdapter<String>
            (this, android.R.layout.simple_dropdown_item_1line, stateArray);

        // set title
        try
        {
            getSupportActionBar().setTitle(specName);
        } catch (NullPointerException e)
        {
            Log.i(TAG, "NullPointerException: No species name!");
        }

        individuals = individualsDataSource.getIndividual(i_id);
        temp = tempDataSource.getTemp();

        // display the editable data
        eiw = new EditIndividualWidget(this, null);
        eiw.setWidgetLocality1(getString(R.string.locality));
        if (temp.temp_loc.equals(""))
        {
            eiw.setWidgetLocality2(individuals.locality);
        }
        else
        {
            eiw.setWidgetLocality2(temp.temp_loc);
        }

        eiw.setWidgetSex1(getString(R.string.sex1));
        eiw.setWidgetSex2(individuals.sex);

        eiw.setWidgetStadium1(getString(R.string.stadium1));
        AutoCompleteTextView acTextView = (AutoCompleteTextView) eiw.findViewById(R.id.widgetStadium2);
        acTextView.setThreshold(1);
        acTextView.setAdapter(adapter);

        eiw.setWidgetStadium2(getString(R.string.stadium_1));

        eiw.setWidgetState1(getString(R.string.state));
        eiw.setWidgetState2(individuals.state_1_6);

        eiw.setWidgetIndivNote1(getString(R.string.note));
        eiw.setWidgetIndivNote2(individuals.notes);

        eiw.setWidgetXCoord1(getString(R.string.xcoord));
        eiw.setWidgetXCoord2(latitude);

        eiw.setWidgetYCoord1(getString(R.string.ycoord));
        eiw.setWidgetYCoord2(longitude);

        individ_area.addView(eiw);
    }

    @Override
    protected void onPause()
    {
        super.onPause();

        // close the data sources
        individualsDataSource.close();
        tempDataSource.close();
    }

    public boolean saveData()
    {
        // save individual data
        // Locality
        String newlocality = eiw.getWidgetLocality2();
        if (!newlocality.equals(""))
        {
            individuals.locality = newlocality;
            temp.temp_loc = newlocality;
        }

        // Sex
        String newsex = eiw.getWidgetSex2();
        if (newsex.equals("") || newsex.matches(" |m|M|f|F"))
        {
            individuals.sex = newsex;
        }
        else
        {
            Toast.makeText(this, getString(R.string.valSex), Toast.LENGTH_SHORT).show();
            return false;
        }

        // Stadium
        individuals.stadium = eiw.getWidgetStadium2();

        // State_1-6
        int newstate = eiw.getWidgetState2();
        if (newstate >= 0 && newstate < 7)
        {
            individuals.state_1_6 = newstate;
        }
        else
        {
            Toast.makeText(this, getString(R.string.valState), Toast.LENGTH_SHORT).show();
            return false;
        }

        // Notes
        String newnotes = eiw.getWidgetIndivNote2();
        if (!newnotes.equals(""))
        {
            individuals.notes = newnotes;
        }

        individualsDataSource.saveIndividual(individuals);
        tempDataSource.saveTemp(temp);

        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.edit_individual, menu);
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
            {
                super.finish();
                // close the data sources
                individualsDataSource.close();
                tempDataSource.close();
            }
        }
        return super.onOptionsItemSelected(item);
    }

    public void onSharedPreferenceChanged(SharedPreferences prefs, String key)
    {
        ScrollView individ_screen = (ScrollView) findViewById(R.id.editIndividualScreen);
        individ_screen.setBackground(null);
        bMap = tourCount.decodeBitmap(R.drawable.kbackground, tourCount.width, tourCount.height);
        bg = new BitmapDrawable(individ_screen.getResources(), bMap);
        individ_screen.setBackground(bg);
    }

}
