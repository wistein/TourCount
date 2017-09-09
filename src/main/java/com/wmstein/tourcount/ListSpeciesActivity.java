package com.wmstein.tourcount;

import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
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
import com.wmstein.tourcount.widgets.ListSumWidget;
import com.wmstein.tourcount.widgets.ListTitleWidget;

import java.util.List;

/****************************************************
 * ListSpeciesActivity shows list of counting results
 * Created by wmstein on 15.03.2016
 */
public class ListSpeciesActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener
{
    private static String TAG = "tourcountListSpeciesActivity";
    private TourCountApplication tourCount;
    private SharedPreferences prefs;

    private LinearLayout spec_area;

    // preferences
    private boolean awakePref;
    private String sortPref;
    private boolean screenOrientL; // option for screen orientation

    // the actual data
    private CountDataSource countDataSource;
    private SectionDataSource sectionDataSource;
    private HeadDataSource headDataSource;
    
    ListSumWidget lsw;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        tourCount = (TourCountApplication) getApplication();
        prefs = TourCountApplication.getPrefs();
        prefs.registerOnSharedPreferenceChangeListener(this);
        getPrefs();

        if (screenOrientL)
        {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        } else
        {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }

        setContentView(R.layout.activity_list_species);

        countDataSource = new CountDataSource(this);
        sectionDataSource = new SectionDataSource(this);
        headDataSource = new HeadDataSource(this);

        ScrollView listSpec_screen = (ScrollView) findViewById(R.id.listSpecScreen);
        assert listSpec_screen != null;
        listSpec_screen.setBackground(tourCount.getBackground());

        //noinspection ConstantConditions
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
        sortPref = prefs.getString("pref_sort_sp", "none"); // sorted species list
        screenOrientL = prefs.getBoolean("screen_Orientation", false);
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
    private void loadData()
    {
        int sumsp = 0, sumind = 0;
        
        headDataSource.open();
        sectionDataSource.open();

        //load head and meta data
        Head head = headDataSource.getHead();
        Section section = sectionDataSource.getSection();

        // display the list name
        ListTitleWidget elw = new ListTitleWidget(this, null);
        elw.setListTitle(getString(R.string.titleEdit));
        elw.setListName(section.name);
        spec_area.addView(elw);

        // display the list remark
        ListTitleWidget erw = new ListTitleWidget(this, null);
        erw.setListTitle(getString(R.string.notesHere));
        erw.setListName(section.notes);
        spec_area.addView(erw);

        // display the head data
        ListHeadWidget ehw = new ListHeadWidget(this, null);
        ehw.setWidgetLCo(getString(R.string.country));
        ehw.setWidgetLCo1(section.country);
        ehw.setWidgetLName(getString(R.string.inspector));
        ehw.setWidgetLName1(head.observer);
        spec_area.addView(ehw);

        // display the meta data
        ListMetaWidget etw = new ListMetaWidget(this, null);
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
        switch (sortPref)
        {
        case "names_alpha":
            specs = countDataSource.getCntSpeciesSrtName();
            break;
        case "codes":
            specs = countDataSource.getCntSpeciesSrtCode();
            break;
        default:
            specs = countDataSource.getCntSpecies();
            break;
        }

        // calculate the totals
        for (Count spec : specs)
        {
            ListSpeciesWidget widget = new ListSpeciesWidget(this, null);
            widget.setCount(spec, section);
            int spec_count = widget.getSpec_count(spec);

            sumind = sumind + spec_count; // sum of counted individuals
            sumsp = sumsp + 1;			  // sum of counted species
        }

        // display the totals
        lsw = new ListSumWidget(this, null);
        lsw.setSum(sumsp, sumind);
        spec_area.addView(lsw);

        // display all the counts by adding them to listSpecies layout
        for (Count spec : specs)
        {
            ListSpeciesWidget widget = new ListSpeciesWidget(this, null);
            widget.setCount(spec, section);
            int spec_count = widget.getSpec_count(spec);

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
        assert listSpec_screen != null;
        listSpec_screen.setBackground(null);
        listSpec_screen.setBackground(tourCount.setBackground());
        getPrefs();
    }

}
