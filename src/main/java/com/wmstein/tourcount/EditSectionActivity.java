package com.wmstein.tourcount;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Toast;

import com.wmstein.tourcount.database.Count;
import com.wmstein.tourcount.database.CountDataSource;
import com.wmstein.tourcount.database.Section;
import com.wmstein.tourcount.database.SectionDataSource;
import com.wmstein.tourcount.widgets.CountEditWidget;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by milo on 05/05/2014.
 * Changed by wmstein on 18.02.2016
 */

/***********************************************************************************************************************/
// EditSectionActivity is called from CountingActivity 
public class EditSectionActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener
{
    TourCountApplication tourCount;
    SharedPreferences prefs;
    public static String TAG = "TourCountEditSectionActivity";

    // the actual data
    Section section;
    List<Count> counts;

    private SectionDataSource sectionDataSource;
    private CountDataSource countDataSource;

    LinearLayout counts_area;
    private View markedForDelete;
    private int idToDelete;
    AlertDialog.Builder areYouSure;
    
    public ArrayList<String> countNames;
    public ArrayList<String> cmpCountNames;
    public ArrayList<Integer> countIds;
    public ArrayList<CountEditWidget> savedCounts;

    private Bitmap bMap;
    private BitmapDrawable bg;

    //added for dupPref
    private boolean dupPref;
    String new_count_name = "";

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_section);

        countNames = new ArrayList<>();
        countIds = new ArrayList<>();
        savedCounts = new ArrayList<>();

        counts_area = (LinearLayout) findViewById(R.id.editingCountsLayout);

        /*
         * Restore any edit widgets the user has added previously
         */
        if (savedInstanceState != null)
        {
            if (savedInstanceState.getSerializable("savedCounts") != null)
            {
                savedCounts = (ArrayList<CountEditWidget>) savedInstanceState.getSerializable("savedCounts");
            }
        }

        tourCount = (TourCountApplication) getApplication();
        prefs = TourCountApplication.getPrefs();
        prefs.registerOnSharedPreferenceChangeListener(this);

        //added for dupPref
        dupPref = prefs.getBoolean("pref_duplicate", true);

        ScrollView counting_screen = (ScrollView) findViewById(R.id.editingScreen);
        bMap = tourCount.decodeBitmap(R.drawable.kbackground, tourCount.width, tourCount.height);
        bg = new BitmapDrawable(counting_screen.getResources(), bMap);
        counting_screen.setBackground(bg);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState)
    {
    /*
     * Before these widgets can be serialised they must be removed from their parent, or else
     * trying to add them to a new parent causes a crash because they've already got one.
     */
        super.onSaveInstanceState(outState);
        for (CountEditWidget cew : savedCounts)
        {
            ((ViewGroup) cew.getParent()).removeView(cew);
        }
        outState.putSerializable("savedCounts", savedCounts);
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        // clear any existing views
        counts_area.removeAllViews();

        // setup the data sources
        sectionDataSource = new SectionDataSource(this);
        sectionDataSource.open();
        countDataSource = new CountDataSource(this);
        countDataSource.open();

        // load the sections data
        section = sectionDataSource.getSection();
        try
        {
            getSupportActionBar().setTitle(section.name);
        } catch (NullPointerException e)
        {
            Log.i(TAG, "NullPointerException: No section name!");
        }

/*
        // display editable section notes; the same class
        enw = new EditTitleWidget(this, null);
        enw.setSectionName(section.notes);
        enw.setWidgetTitle(getString(R.string.notesHere));
        enw.setHint(getString(R.string.notesHint));
        enw.requestFocus();
        notes_area.addView(enw);
*/
        // load the counts data
        counts = countDataSource.getAllSpecies();

        // display all the counts by adding them to countCountLayout
        for (Count count : counts)
        {
            // widget
            CountEditWidget cew = new CountEditWidget(this, null);
            cew.setCountName(count.name);
            cew.setCountId(count.id);
            counts_area.addView(cew);
        }
        for (CountEditWidget cew : savedCounts)
        {
            counts_area.addView(cew);
        }
        getCountNames();
    }

    @Override
    protected void onPause()
    {
        super.onPause();

        // close the data sources
        sectionDataSource.close();
        countDataSource.close();

    }

    public void getCountNames()
    {
    /*
     * My plan here is that both the names and ids arrays contain the entries in the same
     * order, so I can link a count name to its id by knowing the index.
     */
        countNames.clear();
        countIds.clear();
        int childcount = counts_area.getChildCount();
        for (int i = 0; i < childcount; i++)
        {
            CountEditWidget cew = (CountEditWidget) counts_area.getChildAt(i);
            String name = cew.getCountName();
            // ignore count widgets where the user has filled nothing in. 
            // Id will be 0 if this is a new count.
            if (StringUtils.isNotEmpty(name))
            {
                countNames.add(name);
                countIds.add(cew.countId);
            }
        }
    }

    // Compare count names for duplicates and returns name of 1. duplicate found
    public String compCountNames()
    {
        String name = "";
        String isDbl = "";
        cmpCountNames = new ArrayList<String>();
        
        int childcount = counts_area.getChildCount();
        // for all CountEditWidgets
        for (int i = 0; i < childcount; i++)
        {
            CountEditWidget cew = (CountEditWidget) counts_area.getChildAt(i);
            name = cew.getCountName();
            
            if (cmpCountNames.contains(name))
            {
                isDbl = name;
                //Log.i(TAG, "Double name = " + isDbl);
                break;
            }
            cmpCountNames.add(name);
        }
        return isDbl;
    }

    public void saveAndExit(View view)
    {
        if (saveData())
        {
            savedCounts.clear();
            super.finish();
        }
    }

    public boolean saveData()
    {
        // save counts (species list)
        boolean retValue = true;
        String isDbl = "";
        int childcount; //No. of species in list
        childcount = counts_area.getChildCount();
        //Log.i(TAG, "childcount: " + String.valueOf(childcount));

        // check for unique species names
        if (dupPref)
        {
            isDbl = compCountNames();
            if (isDbl.equals(""))
            {
                // do for all species 
                for (int i = 0; i < childcount; i++)
                {
                    CountEditWidget cew = (CountEditWidget) counts_area.getChildAt(i);
                    if (StringUtils.isNotEmpty(cew.getCountName()))
                    {
                        //Log.i(TAG, "cew: " + String.valueOf(cew.countId) + ", " + cew.getCountName());
                        // create or save
                        if (cew.countId == 0)
                        {
                            //Log.i(TAG, "Creating!");
                            //returns newCount
                            countDataSource.createCount(cew.getCountName());
                        }
                        else
                        {
                            //Log.i(TAG, "Updating!");
                            countDataSource.updateCountName(cew.countId, cew.getCountName());
                        }
                        retValue = true;
                    }
                }
            }
            else
            {
                Toast.makeText(this, isDbl + " " + getString(R.string.isdouble), Toast.LENGTH_SHORT).show();
                retValue = false;
            }
        }
        
        if (retValue)
        {
            Toast.makeText(EditSectionActivity.this, getString(R.string.sectSaving) + " " + section.name + "!", Toast.LENGTH_SHORT).show();
        }
        else
        {
            Toast.makeText(this, getString(R.string.duplicate), Toast.LENGTH_SHORT).show();
        }

        return retValue;
    }

    /*
     * Scroll to end of view
     * by wmstein
     */
    public void ScrollToEndOfView(View scrlV)
    {
        int scroll_amount = scrlV.getBottom();
        int scrollY = scroll_amount;
        boolean pageend = false;
        while (!pageend)
        {
            scrlV.scrollTo(0, scroll_amount);            //scroll
            scroll_amount = scroll_amount + scroll_amount; //increase scroll_amount 
            scrollY = scrollY + scrlV.getScrollY();      //scroll position 1. row
            if (scroll_amount > scrollY)
            {
                pageend = true;
            }
        }
    }

    public void newCount(View view)
    {
        CountEditWidget cew = new CountEditWidget(this, null);
        counts_area.addView(cew); // adds a child view cew
        // Scroll to end of view, added by wmstein
        View scrollV = findViewById(R.id.editingScreen);
        ScrollToEndOfView(scrollV);
        cew.requestFocus();       // set focus to cew added by wmstein
        savedCounts.add(cew);
    }

    /*
     * These are required for purging counts
     */
    public void deleteCount(View view)
    {
        markedForDelete = view;
        idToDelete = (Integer) view.getTag();
        if (idToDelete == 0)
        {
            // the actual CountEditWidget is two levels up from the button in which it is embedded
            counts_area.removeView((CountEditWidget) view.getParent().getParent());
        }
        else
        {
            areYouSure = new AlertDialog.Builder(this);
            areYouSure.setTitle(getString(R.string.deleteCount));
            areYouSure.setMessage(getString(R.string.reallyDeleteCount));
            areYouSure.setPositiveButton(R.string.yesDeleteIt, new DialogInterface.OnClickListener()
            {
                public void onClick(DialogInterface dialog, int whichButton)
                {
                    // go ahead for the delete
                    countDataSource.deleteCountById(idToDelete);
                    counts_area.removeView((CountEditWidget) markedForDelete.getParent().getParent());
                    new_count_name ="";
                }
            });
            areYouSure.setNegativeButton(R.string.noCancel, new DialogInterface.OnClickListener()
            {
                public void onClick(DialogInterface dialog, int whichButton)
                {
                    // Cancelled.
                }
            });
            areYouSure.show();
        }
        getCountNames();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.edit_section, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings)
        {
            startActivity(new Intent(this, SettingsActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
        }
        else if (id == R.id.home)
        {
            Intent intent = NavUtils.getParentActivityIntent(this);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            NavUtils.navigateUpTo(this, intent);
        }
        else if (id == R.id.menuSaveExit)
        {
            if (saveData())
            {
                savedCounts.clear();
                super.finish();
            }
        }
        else if (id == R.id.newCount)
        {
            newCount(findViewById(R.id.editingCountsLayout));
        }
        return super.onOptionsItemSelected(item);
    }

    public void onSharedPreferenceChanged(SharedPreferences prefs, String key)
    {
        ScrollView counting_screen = (ScrollView) findViewById(R.id.editingScreen);
        counting_screen.setBackground(null);
        bMap = tourCount.decodeBitmap(R.drawable.kbackground, tourCount.width, tourCount.height);
        bg = new BitmapDrawable(counting_screen.getResources(), bMap);
        counting_screen.setBackground(bg);

        //added for dupPref
        dupPref = prefs.getBoolean("duplicate_counts", true);
    }
}
