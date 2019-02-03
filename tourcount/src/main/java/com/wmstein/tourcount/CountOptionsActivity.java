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
import android.view.inputmethod.InputMethodManager;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.wmstein.tourcount.database.Count;
import com.wmstein.tourcount.database.CountDataSource;
import com.wmstein.tourcount.widgets.EditNotesWidget;

/**********************************
 * CountOptionsActivity
 * Created by milo on 05/05/2014.
 * Adopted by wmstein on 18.02.2016,
 * last edited on 2019-01-27
 */
public class CountOptionsActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener
{
    private static String TAG = "tourcountCountOptionAct";
    private TourCountApplication tourCount;
    private LinearLayout static_widget_area;
    private EditNotesWidget enw;
    private Count count;
    private int count_id;
    private CountDataSource countDataSource;
    private Bitmap bMap;
    private BitmapDrawable bg;
    private boolean brightPref;
    private boolean screenOrientL; // option for screen orientation

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        tourCount = (TourCountApplication) getApplication();
        SharedPreferences prefs = TourCountApplication.getPrefs();
        prefs.registerOnSharedPreferenceChangeListener(this);
        brightPref = prefs.getBoolean("pref_bright", true);
        screenOrientL = prefs.getBoolean("screen_Orientation", false);

        setContentView(R.layout.activity_count_options);

        LinearLayout counting_screen = findViewById(R.id.count_options);

        if (screenOrientL)
        {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        } else
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

        static_widget_area = findViewById(R.id.static_widget_area);

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

        // get the data sources
        countDataSource = new CountDataSource(this);
        countDataSource.open();

        count = countDataSource.getCountById(count_id);
        //noinspection ConstantConditions
        getSupportActionBar().setTitle(count.name);

        enw = new EditNotesWidget(this, null);
        enw.setNotesName(count.notes);
        enw.setWidgetTitle(getString(R.string.notesSpecies));
        enw.setHint(getString(R.string.notesHint));

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
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null)
        {
            imm.hideSoftInputFromWindow(enw.getWindowToken(), 0);
        }
    }

    public void saveAndExit(View view)
    {
        saveData();
        super.finish();
    }

    private void saveData()
    {
        // don't crash if the user hasn't filled things in...
        // Snackbar doesn't appear so Toast 
        Toast.makeText(CountOptionsActivity.this, getString(R.string.sectSaving) + " " + count.name + "!", Toast.LENGTH_SHORT).show();
        count.notes = enw.getNotesName();
        // hide keyboard
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null)
        {
            imm.hideSoftInputFromWindow(enw.getWindowToken(), 0);
        }
        countDataSource.saveCount(count);
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
        if (id == R.id.home)
        {
            Intent intent = NavUtils.getParentActivityIntent(this);
            assert intent != null;
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
        LinearLayout counting_screen = findViewById(R.id.count_options);
        counting_screen.setBackground(null);
        brightPref = prefs.getBoolean("pref_bright", true);
        screenOrientL = prefs.getBoolean("screen_Orientation", false);
        if (screenOrientL)
        {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        } else
        {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
        bMap = tourCount.decodeBitmap(R.drawable.kbackground, tourCount.width, tourCount.height);
        bg = new BitmapDrawable(counting_screen.getResources(), bMap);
        counting_screen.setBackground(bg);
    }

}
