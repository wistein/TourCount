package com.wmstein.tourcount;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
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
import com.wmstein.tourcount.widgets.ListIndivRemWidget;
import com.wmstein.tourcount.widgets.ListIndividualWidget;
import com.wmstein.tourcount.widgets.ListLineWidget;
import com.wmstein.tourcount.widgets.ListMetaWidget;
import com.wmstein.tourcount.widgets.ListSpeciesWidget;
import com.wmstein.tourcount.widgets.ListSumWidget;
import com.wmstein.tourcount.widgets.ListTitleWidget;

import java.util.List;
import java.util.Objects;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NavUtils;

import static java.lang.Math.sqrt;

/****************************************************
 * ListSpeciesActivity shows list of counting results
 * Created by wmstein on 2012-05-21,
 * last edited on 2022-04-21
 */
public class ListSpeciesLActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener
{
    private static final String TAG = "tourcountListSpeciesAct";
    @SuppressLint("StaticFieldLeak")
    private static TourCountApplication tourCount;
    private SharedPreferences prefs;

    private LinearLayout spec_area;

    // preferences
    private boolean awakePref;
    private String sortPref;

    // the actual data
    private CountDataSource countDataSource;
    private SectionDataSource sectionDataSource;
    private HeadDataSource headDataSource;
    private IndividualsDataSource individualsDataSource;

    ListSumWidget lsw;

    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        tourCount = (TourCountApplication) getApplication();
        prefs = TourCountApplication.getPrefs();
        prefs.registerOnSharedPreferenceChangeListener(this);
        getPrefs();

        setContentView(R.layout.activity_list_species);

        countDataSource = new CountDataSource(this);
        sectionDataSource = new SectionDataSource(this);
        headDataSource = new HeadDataSource(this);
        individualsDataSource = new IndividualsDataSource(this);

        ScrollView listSpec_screen = findViewById(R.id.listSpecScreen);
        listSpec_screen.setBackground(tourCount.getBackground());

        Objects.requireNonNull(getSupportActionBar()).setTitle(getString(R.string.viewSpecTitle));

        spec_area = findViewById(R.id.listSpecLayout);

        if (awakePref)
        {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    // Load preferences
    private void getPrefs()
    {
        awakePref = prefs.getBoolean("pref_awake", true);
        sortPref = prefs.getString("pref_sort_sp", "none"); // sorted species list
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        if (awakePref)
        {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
        
        // clear existing views
        spec_area.removeAllViews();

        loadData();
    }

    // fill ListSpeciesWidget with relevant counts and sections data
    private void loadData()
    {
        int summf = 0, summ = 0, sumf = 0, sump = 0, suml = 0, sumo = 0;
        int sumsp = 0, sumind = 0; // sum of counted species, sum of counted individuals
        double longi, lati, uncer;
        int frst = 0;
        double lo, la, loMin = 0, loMax = 0, laMin = 0, laMax = 0, uc, uncer1 = 0;

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

        // Calculate mean average values for coords and uncertainty
        DbHelper dbHandler = new DbHelper(this);
        SQLiteDatabase database = dbHandler.getWritableDatabase();
        Cursor curAInd;
        curAInd = database.rawQuery("select * from " + DbHelper.INDIVIDUALS_TABLE, null);
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
        int spec_countf1i;
        int spec_countf2i;
        int spec_countf3i;
        int spec_countpi;
        int spec_countli;
        int spec_countei;
        
        for (Count spec : specs)
        {
            ListSpeciesWidget widget = new ListSpeciesWidget(this, null);
            widget.setCount(spec);
            spec_countf1i = widget.getSpec_countf1i(spec);
            spec_countf2i = widget.getSpec_countf2i(spec);
            spec_countf3i = widget.getSpec_countf3i(spec);
            spec_countpi = widget.getSpec_countpi(spec);
            spec_countli = widget.getSpec_countli(spec);
            spec_countei = widget.getSpec_countei(spec);

            summf = summf + spec_countf1i;
            summ = summ + spec_countf2i;
            sumf = sumf + spec_countf3i;
            sump = sump + spec_countpi;
            suml = suml + spec_countli;
            sumo = sumo + spec_countei;

            sumind = sumind + spec_countf1i + spec_countf2i + spec_countf3i + spec_countpi
                + spec_countli + spec_countei; // sum of counted individuals
            sumsp = sumsp + 1;                 // sum of counted species
        }

        // display the totals
        lsw = new ListSumWidget(this, null);
        lsw.setSum(sumsp, sumind);
        spec_area.addView(lsw);
        
        int spec_count;
        List<Individuals> indivs; // List of individuals
        // display all the counts by adding them to listSpecies layout
        for (Count spec : specs)
        {
            ListSpeciesWidget widget = new ListSpeciesWidget(this, null);
            widget.setCount(spec);
            
            spec_countf1i = widget.getSpec_countf1i(spec);
            spec_countf2i = widget.getSpec_countf2i(spec);
            spec_countf3i = widget.getSpec_countf3i(spec);
            spec_countpi = widget.getSpec_countpi(spec);
            spec_countli = widget.getSpec_countli(spec);
            spec_countei = widget.getSpec_countei(spec);
            
            spec_count = spec_countf1i + spec_countf2i + spec_countf3i 
                + spec_countpi + spec_countli + spec_countei;
            
            
            // fill widget only for counted species
            if (spec_count > 0)
            {
                spec_area.addView(widget);

                String iName = widget.getSpec_name(spec);
                indivs = individualsDataSource.getIndividualsByName(iName);
                for (Individuals indiv : indivs)
                {
                    ListIndividualWidget iwidget = new ListIndividualWidget(this, null);
                    //load the individuals data
                    iwidget.setIndividual(indiv);
                    spec_area.addView(iwidget);

                    // show individual notes only when provided
                    String tRem;
                    ListIndivRemWidget rwidget = new ListIndivRemWidget(this, null);
                    if (iwidget.getIndNotes(indiv) == null)
                        tRem = "";
                    else
                        tRem = iwidget.getIndNotes(indiv);

                    if (tRem.length() > 0)
                    {
                        rwidget.setRem(indiv);
                        spec_area.addView(rwidget);
                    }
                    
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
        ScrollView listSpec_screen = findViewById(R.id.listSpecScreen);
        listSpec_screen.setBackground(null);
        listSpec_screen.setBackground(tourCount.setBackground());
        getPrefs();
    }

}
