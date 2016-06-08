package com.wmstein.tourcount;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import com.wmstein.tourcount.database.Count;
import com.wmstein.tourcount.database.CountDataSource;
import com.wmstein.tourcount.database.Head;
import com.wmstein.tourcount.database.HeadDataSource;
import com.wmstein.tourcount.database.Section;
import com.wmstein.tourcount.database.SectionDataSource;
import com.wmstein.tourcount.widgets.ListHeadWidget;
import com.wmstein.tourcount.widgets.ListMetaWidget;
import com.wmstein.tourcount.widgets.ListSpeciesWidget;
import com.wmstein.tourcount.widgets.ListTitleWidget;

import java.util.List;

/**
 * ListSpeciesActivity shows list of counting results
 * Created by wmstein on 15.03.2016
 */

public class ListSpeciesActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener
{
    private static String TAG = "tourcountListSpeciesActivity";
    TourCountApplication tourCount;
    SharedPreferences prefs;
    LinearLayout spec_area;

    Head head;
    Section section;

    public int spec_count;
    
    // preferences
    private boolean awakePref;
    
    // the actual data
    private CountDataSource countDataSource;
    private SectionDataSource sectionDataSource;
    private HeadDataSource headDataSource;

    ListTitleWidget elw;
    ListTitleWidget erw;
    ListHeadWidget ehw;
    ListMetaWidget etw;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_listspecies);

        countDataSource = new CountDataSource(this);
        sectionDataSource = new SectionDataSource(this);
        headDataSource = new HeadDataSource(this);

        tourCount = (TourCountApplication) getApplication();
        prefs = TourCountApplication.getPrefs();
        prefs.registerOnSharedPreferenceChangeListener(this);
        getPrefs();

        ScrollView listSpec_screen = (ScrollView) findViewById(R.id.listSpecScreen);
        listSpec_screen.setBackground(tourCount.getBackground());
        getSupportActionBar().setTitle(getString(R.string.viewSpecTitle));

        spec_area = (LinearLayout) findViewById(R.id.listSpecLayout);

        if (awakePref)
        {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    /*
     * So preferences can be loaded at the start, and also when a change is detected.
     */
    private void getPrefs()
    {
        awakePref = prefs.getBoolean("pref_awake", true);
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        if (awakePref)
        {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

        spec_area.removeAllViews();
        loadData();
    }

    // fill ListSpeciesWidget with relevant counts and sections data
    public void loadData()
    {
        headDataSource.open();
        sectionDataSource.open();

        //load head and meta data
        head = headDataSource.getHead();
        section = sectionDataSource.getSection(); 

        // display the list name
        elw = new ListTitleWidget(this, null);
        elw.setListTitle(getString(R.string.titleEdit));
        elw.setListName(section.name);
        spec_area.addView(elw);

        // display the list remark
        erw = new ListTitleWidget(this, null);
        erw.setListTitle(getString(R.string.notesHere));
        erw.setListName(section.notes);
        spec_area.addView(erw);

        // display the head data
        ehw = new ListHeadWidget(this, null);
        ehw.setWidgetLCo(getString(R.string.country));
        ehw.setWidgetLCo1(section.country);
        ehw.setWidgetLName(getString(R.string.inspector));
        ehw.setWidgetLName1(head.observer);
        spec_area.addView(ehw);

        // display the meta data
        etw = new ListMetaWidget(this, null);
        etw.setWidgetLMeta1(getString(R.string.temperature));
        etw.setWidgetLItem1(section.temp);
        etw.setWidgetLMeta2(getString(R.string.wind));
        etw.setWidgetLItem2(section.wind);
        etw.setWidgetLMeta3(getString(R.string.clouds));
        etw.setWidgetLItem3(section.clouds);
        etw.setWidgetLPlz1(getString(R.string.plz));
        etw.setWidgetLPlz2(section.plz);
        etw.setWidgetLCity(getString(R.string.city));
        etw.setWidgetLItem4(section.city);
        etw.setWidgetLPlace(getString(R.string.place));
        etw.setWidgetLItem5(section.place);
        etw.setWidgetLDate1(getString(R.string.date));
        etw.setWidgetLDate2(section.date);
        etw.setWidgetLStartTm1(getString(R.string.starttm));
        etw.setWidgetLStartTm2(section.start_tm);
        etw.setWidgetLEndTm1(getString(R.string.endtm));
        etw.setWidgetLEndTm2(section.end_tm);
        spec_area.addView(etw);

        //List of species
        List<Count> specs; 
        
        // setup the data sources
        countDataSource.open();

        // load the data
        specs = countDataSource.getAllSpecies();

        // display all the counts by adding them to listSpecies layout
        for (Count spec : specs)
        {
            // set section ID from count table and prepare to get section name from section table
            section = sectionDataSource.getSection();

            ListSpeciesWidget widget = new ListSpeciesWidget(this, null);
            widget.setCount(spec, section);
            spec_count = widget.getSpec_count(spec);

            // fill widget only for counted species
            if (spec_count > 0)
            {
                spec_area.addView(widget);
            }
        }
    }
    
    @Override
    protected void onPause()
    {
        super.onPause();

        // close the data sources
        headDataSource.close();
        countDataSource.close();
        sectionDataSource.close();
        
        if (awakePref)
        {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }


    /***************/
    public void saveAndExit(View view)
    {
        super.finish();
    }

    public void onSharedPreferenceChanged(SharedPreferences prefs, String key)
    {
        ScrollView listSpec_screen = (ScrollView) findViewById(R.id.listSpecScreen);
        listSpec_screen.setBackground(null);
        listSpec_screen.setBackground(tourCount.setBackground());
        getPrefs();
    }

}
