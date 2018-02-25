package com.wmstein.tourcount;

import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import com.wmstein.tourcount.database.Count;
import com.wmstein.tourcount.database.CountDataSource;
import com.wmstein.tourcount.database.DbHelper;
import com.wmstein.tourcount.database.Head;
import com.wmstein.tourcount.database.HeadDataSource;
import com.wmstein.tourcount.database.Individuals;
import com.wmstein.tourcount.database.IndividualsDataSource;
import com.wmstein.tourcount.database.Section;
import com.wmstein.tourcount.database.SectionDataSource;
import com.wmstein.tourcount.widgets.ListHeadWidget;
import com.wmstein.tourcount.widgets.ListIndividualWidget;
import com.wmstein.tourcount.widgets.ListLineWidget;
import com.wmstein.tourcount.widgets.ListMetaWidget;
import com.wmstein.tourcount.widgets.ListSpRemWidget;
import com.wmstein.tourcount.widgets.ListSpeciesWidget;
import com.wmstein.tourcount.widgets.ListSumWidget;
import com.wmstein.tourcount.widgets.ListTitleWidget;

import java.util.List;

import static java.lang.Math.sqrt;

/****************************************************
 * ListSpeciesActivity shows list of counting results
 * Created by wmstein on 15.03.2016
 */
public class ListSpeciesActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener
{
    private static String TAG = "tourcountListSpeciesAct";
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
    private IndividualsDataSource individualsDataSource;

    private SQLiteDatabase database;
    private DbHelper dbHandler;
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
        }
        else
        {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }

        setContentView(R.layout.activity_list_species);

        countDataSource = new CountDataSource(this);
        sectionDataSource = new SectionDataSource(this);
        headDataSource = new HeadDataSource(this);
        individualsDataSource = new IndividualsDataSource(this);

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
        int sumsp = 0, sumind = 0, iid = 0;
        double mUncert = 0;
        double longi = 0, lati = 0, heigh = 0, uncer = 0;
        int frst = 0;
        double lo = 0, la = 0, loMin = 0, loMax = 0, laMin = 0, laMax = 0, uc = 0, uncer1 = 0;

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

        // setup the data sources
        countDataSource.open();
        individualsDataSource.open();
        Individuals individuals;

        // Calculate mean average values for coords and uncertainty
        dbHandler = new DbHelper(this);
        database = dbHandler.getWritableDatabase();
        Cursor curAInd;
        curAInd = database.rawQuery("select * from " + dbHandler.INDIVIDUALS_TABLE, null);
        frst = 0;
        while (curAInd.moveToNext())
        {
            longi = curAInd.getDouble(4);
            lati = curAInd.getDouble(3);
            uncer = Math.rint(curAInd.getDouble(6));

            if (longi != 0) // has coordinates
            {
                //Toast.makeText(getApplicationContext(), longi, Toast.LENGTH_SHORT).show();
                if (MyDebug.LOG)
                    Log.d(TAG, "longi " + longi);
                if (frst == 0)
                {
                    loMin = longi;
                    loMax = longi;
                    laMin = lati;
                    laMax = lati;
                    uncer1 = uncer;
                    frst = 1; // just 1 with coordinates
                }
                else
                {
                    loMin = Math.min(loMin, longi);
                    loMax = Math.max(loMax, longi);
                    laMin = Math.min(laMin, lati);
                    laMax = Math.max(laMax, lati);
                    uncer1 = Math.max(uncer1, uncer);
                }
            }
        }
        curAInd.close();

        lo = (loMax + loMin) / 2;   // average longitude
        la = (laMax + laMin) / 2;   // average latitude

        // Simple distance calculation between 2 coordinates within the temperate zone in meters (Pythagoras):
        //   uc = (((loMax-loMin)*71500)² + ((laMax-laMin)*111300)²)½ 
        uc = sqrt(((Math.pow((loMax - loMin) * 71500, 2)) + (Math.pow((laMax - laMin) * 111300, 2))));
        uc = Math.rint(uc / 2) + 20; // average uncertainty radius + default gps uncertainty
        if (uc <= uncer1)
            uc = uncer1;

        // display the meta data
        ListMetaWidget etw = new ListMetaWidget(this, null);
        etw.setMetaWidget(section);
        etw.setWidget_dla2(la);
        etw.setWidget_dlo2(lo);
        etw.setWidget_muncert2(uc);
        spec_area.addView(etw);

        // load the species data
        List<Count> specs; //List of species
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
            widget.setCount(spec);
            int spec_count = widget.getSpec_count(spec);

            sumind = sumind + spec_count; // sum of counted individuals
            sumsp = sumsp + 1;              // sum of counted species
        }

        // display the totals
        lsw = new ListSumWidget(this, null);
        lsw.setSum(sumsp, sumind);
        spec_area.addView(lsw);

        List<Individuals> indivs; // List of individuals

        // display all the counts by adding them to listSpecies layout
        for (Count spec : specs)
        {
            ListSpeciesWidget widget = new ListSpeciesWidget(this, null);
            widget.setCount(spec);
            int spec_count = widget.getSpec_count(spec);
            ListSpRemWidget rwidget = new ListSpRemWidget(this, null);
            rwidget.setCount(spec);
            String tRem = rwidget.getRem(spec);

            // fill widget only for counted species
            if (spec_count > 0)
            {
                spec_area.addView(widget);
                if (!tRem.equals(""))
                {
                    spec_area.addView(rwidget);
                }

                String iName = widget.getSpec_name(spec);
                indivs = individualsDataSource.getIndividualsByName(iName);
                for (Individuals indiv : indivs)
                {
                    ListIndividualWidget iwidget = new ListIndividualWidget(this, null);
                    //load the individuals data
                    iwidget.setIndividual(indiv);
                    spec_area.addView(iwidget);
                }
            }
        }
        ListLineWidget lwidget = new ListLineWidget(this, null);
        spec_area.addView(lwidget);
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
