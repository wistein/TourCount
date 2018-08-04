package com.wmstein.tourcount;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.wmstein.tourcount.database.Count;
import com.wmstein.tourcount.database.CountDataSource;
import com.wmstein.tourcount.database.Section;
import com.wmstein.tourcount.database.SectionDataSource;
import com.wmstein.tourcount.widgets.CountEditWidget;

import java.util.ArrayList;
import java.util.List;

/***************************************************************
 * Edit the species list (change, delete) and insert new species
 * EditSectionActivity is called from CountingActivity
 * Uses CountEditWidget.java, activity_edit_section.xml.
 * Based on EditProjectActivity.java by milo on 05/05/2014.
 * Adopted by wmstein on 2016-02-18,
 * last edited on 2018-08-03
 */
public class EditSectionActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener
{
    private static final String TAG = "TourCountEditSecAct";
    private ArrayList<CountEditWidget> savedCounts;
    private TourCountApplication tourCount;

    // the actual data
    private Section section;
    private LinearLayout counts_area;
    private SectionDataSource sectionDataSource;
    private CountDataSource countDataSource;
    private View markedForDelete;
    private int idToDelete;
    private Bitmap bMap;
    private BitmapDrawable bg;

    private boolean dupPref;
    private boolean screenOrientL; // option for screen orientation
    private boolean brightPref;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        tourCount = (TourCountApplication) getApplication();
        SharedPreferences prefs = TourCountApplication.getPrefs();
        prefs.registerOnSharedPreferenceChangeListener(this);
        dupPref = prefs.getBoolean("pref_duplicate", true);
        brightPref = prefs.getBoolean("pref_bright", true);
        screenOrientL = prefs.getBoolean("screen_Orientation", false);

        setContentView(R.layout.activity_edit_section);

        ScrollView counting_screen = findViewById(R.id.editingScreen);

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

        bMap = tourCount.decodeBitmap(R.drawable.kbackground, tourCount.width, tourCount.height);
        bg = new BitmapDrawable(counting_screen.getResources(), bMap);
        counting_screen.setBackground(bg);

        savedCounts = new ArrayList<>();
        counts_area = findViewById(R.id.editingCountsLayout);

        // Restore any edit widgets the user has added previously
        if (savedInstanceState != null)
        {
            if (savedInstanceState.getSerializable("savedCounts") != null)
            {
                //noinspection unchecked
                savedCounts = (ArrayList<CountEditWidget>) savedInstanceState.getSerializable("savedCounts");
            }
        }
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

    @SuppressLint("LongLogTag")
    @Override
    protected void onResume()
    {
        super.onResume();

        SharedPreferences prefs = TourCountApplication.getPrefs();
        prefs.registerOnSharedPreferenceChangeListener(this);
        dupPref = prefs.getBoolean("pref_duplicate", true);
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
            //noinspection ConstantConditions
            getSupportActionBar().setTitle(section.name);
        } catch (NullPointerException e)
        {
            if (MyDebug.LOG)
                Log.e(TAG, "NullPointerException: No section name!");
        }

        // load the counts data
        List<Count> counts = countDataSource.getAllSpecies();

        // display all the counts by adding them to CountEditWidget
        for (Count count : counts)
        {
            // widget
            CountEditWidget cew = new CountEditWidget(this, null);
            cew.setCountName(count.name);
            cew.setCountCode(count.code);
            cew.setCountId(count.id);
            counts_area.addView(cew);
        }
        for (CountEditWidget cew : savedCounts)
        {
            counts_area.addView(cew);
        }
    }

    @Override
    protected void onPause()
    {
        super.onPause();

        // close the data sources
        sectionDataSource.close();
        countDataSource.close();
    }

    // Compare count names for duplicates and returns name of 1. duplicate found
    private String compCountNames()
    {
        String name;
        String isDbl = "";
        ArrayList<String> cmpCountNames = new ArrayList<>();

        int childcount = counts_area.getChildCount();
        // for all CountEditWidgets
        for (int i = 0; i < childcount; i++)
        {
            CountEditWidget cew = (CountEditWidget) counts_area.getChildAt(i);
            name = cew.getCountName();

            if (cmpCountNames.contains(name))
            {
                isDbl = name;
                if (MyDebug.LOG)
                    Log.d(TAG, "Double name = " + isDbl);
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

    private boolean saveData()
    {
        // save counts (species list)
        boolean retValue = true;
        String isDbl;
        int childcount; //No. of species in list
        childcount = counts_area.getChildCount();
        if (MyDebug.LOG)
            Log.d(TAG, "childcount: " + String.valueOf(childcount));

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
                    if (isNotEmpty(cew.getCountName()))
                    {
                        if (MyDebug.LOG)
                            Log.d(TAG, "cew: " + String.valueOf(cew.countId) + ", " + cew.getCountName());
                        // create or update
                        if (cew.countId == 0)
                        {
                            if (MyDebug.LOG)
                                Log.d(TAG, "Creating!");
                            //creates new species entry
                            countDataSource.createCount(cew.getCountName(), cew.getCountCode());
                        }
                        else
                        {
                            if (MyDebug.LOG)
                                Log.d(TAG, "Updating!");
                            //updates species name and code
                            countDataSource.updateCountName(cew.countId, cew.getCountName(), cew.getCountCode());
                        }
                        retValue = true;
                    }
                }
            }
            else
            {
//                Toast.makeText(this, isDbl + " " + getString(R.string.isdouble), Toast.LENGTH_SHORT).show();
                showSnackbarRed(isDbl + " " + getString(R.string.isdouble));
                retValue = false;
            }
        }

        if (retValue)
        {
            // Snackbar doesn't appear, so Toast is used
            Toast.makeText(EditSectionActivity.this, getString(R.string.sectSaving) + " " + section.name + "!", Toast.LENGTH_SHORT).show();
        }
        else
        {
//            Toast.makeText(this, getString(R.string.duplicate), Toast.LENGTH_SHORT).show();
            showSnackbarRed(getString(R.string.duplicate));
        }

        return retValue;
    }

    private void showSnackbarRed(String str) // bold red text
    {
        View view = findViewById(R.id.editingScreen);
        Snackbar sB = Snackbar.make(view, Html.fromHtml("<font color=\"#ff0000\"><b>" +  str + "</font></b>"), Snackbar.LENGTH_LONG);
        TextView tv = sB.getView().findViewById(R.id.snackbar_text);
        tv.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        sB.show();
    }

    /*
     * Scroll to end of view
     * by wmstein
     */
    private void ScrollToEndOfView(View scrlV)
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
            AlertDialog.Builder areYouSure = new AlertDialog.Builder(this);
            areYouSure.setTitle(getString(R.string.deleteCount));
            areYouSure.setMessage(getString(R.string.reallyDeleteCount));
            areYouSure.setPositiveButton(R.string.yesDeleteIt, new DialogInterface.OnClickListener()
            {
                public void onClick(DialogInterface dialog, int whichButton)
                {
                    // go ahead for the delete
                    countDataSource.deleteCountById(idToDelete);
                    counts_area.removeView((CountEditWidget) markedForDelete.getParent().getParent());
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
        if (id == R.id.home)
        {
            Intent intent = NavUtils.getParentActivityIntent(this);
            if (intent != null)
            {
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                NavUtils.navigateUpTo(this, intent);
            }
            else
            {
                super.finish();
            }
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
        ScrollView counting_screen = findViewById(R.id.editingScreen);
        dupPref = prefs.getBoolean("pref_duplicate", true);
        screenOrientL = prefs.getBoolean("screen_Orientation", false);
        if (screenOrientL)
        {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }
        else
        {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }

        bMap = tourCount.decodeBitmap(R.drawable.kbackground, tourCount.width, tourCount.height);
        counting_screen.setBackground(null);
        bg = new BitmapDrawable(counting_screen.getResources(), bMap);
        counting_screen.setBackground(bg);
    }

    /**
     * Checks if a CharSequence is empty ("") or null.
     * <p>
     * isEmpty(null)      = true
     * isEmpty("")        = true
     * isEmpty(" ")       = false
     * isEmpty("bob")     = false
     * isEmpty("  bob  ") = false
     *
     * @param cs the CharSequence to check, may be null
     * @return {@code true} if the CharSequence is empty or null
     */
    private static boolean isEmpty(final CharSequence cs)
    {
        return cs == null || cs.length() == 0;
    }

    /**
     * Checks if a CharSequence is not empty ("") and not null.
     * <p>
     * isNotEmpty(null)      = false
     * isNotEmpty("")        = false
     * isNotEmpty(" ")       = true
     * isNotEmpty("bob")     = true
     * isNotEmpty("  bob  ") = true
     *
     * @param cs the CharSequence to check, may be null
     * @return {@code true} if the CharSequence is not empty and not null
     */
    private static boolean isNotEmpty(final CharSequence cs)
    {
        return !isEmpty(cs);
    }

}
