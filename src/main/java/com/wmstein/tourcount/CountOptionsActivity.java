package com.wmstein.tourcount;

import android.content.Intent;
import android.content.SharedPreferences;
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
import android.widget.Toast;

import com.wmstein.tourcount.database.Count;
import com.wmstein.tourcount.database.CountDataSource;
import com.wmstein.tourcount.widgets.EditTitleWidget;
import com.wmstein.tourcount.widgets.OptionsWidget;

/**
 * CountOptionsActivity 
 * Created by milo on 05/05/2014.
 * Changed by wmstein on 18.02.2016
 */

public class CountOptionsActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener
{
    private static String TAG = "tourcountCountOptionsActivity";
    TourCountApplication tourCount;
    SharedPreferences prefs;

    private Count count;
    private int count_id;
    private CountDataSource countDataSource;
    private View markedForDelete;
    private int deleteAnAlert;

    private Bitmap bMap;
    private BitmapDrawable bg;

    // preferences
    private boolean brightPref;

    LinearLayout static_widget_area;
    LinearLayout dynamic_widget_area;
    OptionsWidget curr_val_widget;
    EditTitleWidget enw;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_count_options);

        tourCount = (TourCountApplication) getApplication();
        prefs = TourCountApplication.getPrefs();
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
        
        ScrollView counting_screen = (ScrollView) findViewById(R.id.count_options);
        bMap = tourCount.decodeBitmap(R.drawable.kbackground, tourCount.width, tourCount.height);
        bg = new BitmapDrawable(counting_screen.getResources(), bMap);
        counting_screen.setBackground(bg);

        static_widget_area = (LinearLayout) findViewById(R.id.static_widget_area);
        dynamic_widget_area = (LinearLayout) findViewById(R.id.dynamic_widget_area);

        Bundle extras = getIntent().getExtras();
        if (extras != null)
        {
            count_id = extras.getInt("count_id");
        }

    }

    @Override
    protected void onResume()
    {
        super.onResume();

        // clear any existing views
        static_widget_area.removeAllViews();
        dynamic_widget_area.removeAllViews();

        // get the data sources
        countDataSource = new CountDataSource(this);
        countDataSource.open();

        count = countDataSource.getCountById(count_id);
        getSupportActionBar().setTitle(count.name);

        // setup the static widgets in the following order
        // 1. Current count value (internal counter)
        // 2. Alert add/remove
        curr_val_widget = new OptionsWidget(this, null);
        curr_val_widget.setInstructions(String.format(getString(R.string.editCountValue), count.name, count.count));
        curr_val_widget.setParameterValue(count.count);
        static_widget_area.addView(curr_val_widget);

        enw = new EditTitleWidget(this, null);
        enw.setSectionName(count.notes);
        enw.setWidgetTitle(getString(R.string.notesSpecies));
        enw.setHint(getString(R.string.notesHint));
        enw.requestFocus();

        static_widget_area.addView(enw);

    }

    @Override
    protected void onSaveInstanceState(Bundle outState)
    {
    /*
     * Before these widgets can be serialised they must be removed from their parent, or else
     * trying to add them to a new parent causes a crash because they've already got one.
     */
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onPause()
    {
        super.onPause();

        // finally, close the database
        countDataSource.close();

    }

    public void saveAndExit(View view)
    {
        saveData();
        super.finish();
    }

    public void saveData()
    {
        // don't crash if the user hasn't filled things in...
        Toast.makeText(CountOptionsActivity.this, getString(R.string.sectSaving) + " " + count.name + "!", Toast.LENGTH_SHORT).show();
        count.count = curr_val_widget.getParameterValue();
        count.notes = enw.getSectionName();

        countDataSource.saveCount(count);

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
            scrlV.scrollTo(0, scroll_amount);              //scrollen
            scroll_amount = scroll_amount + scroll_amount; //scroll_amount erhöhen
            scrollY = scrollY + scrlV.getScrollY();        //scroll-Position der 1. Zeile
            if (scroll_amount > scrollY)
            {
                pageend = true;
            }
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.count_options, menu);
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
            saveData();
            super.finish();
        }
        return super.onOptionsItemSelected(item);
    }

    public void onSharedPreferenceChanged(SharedPreferences prefs, String key)
    {
        ScrollView counting_screen = (ScrollView) findViewById(R.id.count_options);
        counting_screen.setBackground(null);
        bMap = tourCount.decodeBitmap(R.drawable.kbackground, tourCount.width, tourCount.height);
        bg = new BitmapDrawable(counting_screen.getResources(), bMap);
        counting_screen.setBackground(bg);
    }

}
