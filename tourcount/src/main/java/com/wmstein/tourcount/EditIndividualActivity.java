package com.wmstein.tourcount;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.wmstein.tourcount.database.Count;
import com.wmstein.tourcount.database.CountDataSource;
import com.wmstein.tourcount.database.Individuals;
import com.wmstein.tourcount.database.IndividualsDataSource;
import com.wmstein.tourcount.database.Temp;
import com.wmstein.tourcount.database.TempDataSource;
import com.wmstein.tourcount.widgets.EditIndividualWidget;

/*******************************************************************************************
 * EditIndividualActivity is called from CountingActivity and collects additional info to an 
 * individual's data record
 * Copyright 2016-2018 wmstein, created on 2016-05-15, 
 * last modification an 2020-01-26
 */
public class EditIndividualActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener
{
    private static final String TAG = "TourCountEditIndivAct";
    private static TourCountApplication tourCount;
    private SharedPreferences prefs;
    private Individuals individuals;
    private Temp temp;
    private Count counts;
    private LinearLayout individ_area;
    private EditIndividualWidget eiw;

    // the actual data
    private IndividualsDataSource individualsDataSource;
    private TempDataSource tempDataSource;
    private CountDataSource countDataSource;
    private Bitmap bMap;
    private BitmapDrawable bg;
    private boolean buttonSoundPref;
    private String buttonAlertSound;
    private boolean brightPref;
    private boolean screenOrientL; // option for screen orientation

    // Location info handling
    private double latitude, longitude, height, uncertainty;

    private int count_id;
    private int i_id, iAtt;
    private String specName;
    private Boolean sdata; // true: data saved already

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        tourCount = (TourCountApplication) getApplication();
        prefs = TourCountApplication.getPrefs();
        prefs.registerOnSharedPreferenceChangeListener(this);
        getPrefs();

        setContentView(R.layout.activity_edit_individual);

        ScrollView individ_screen = findViewById(R.id.editIndividualScreen);

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
        bg = new BitmapDrawable(individ_screen.getResources(), bMap);
        individ_screen.setBackground(bg);

        individ_area = findViewById(R.id.edit_individual);

        // get parameters from CountingActivity
        Bundle extras = getIntent().getExtras();
        if (extras != null)
        {
            count_id = extras.getInt("count_id");
            i_id = extras.getInt("indiv_id");
            specName = extras.getString("SName");
            latitude = extras.getDouble("Latitude");
            longitude = extras.getDouble("Longitude");
            height = extras.getDouble("Height");
            uncertainty = extras.getDouble("Uncert");
            iAtt = extras.getInt("indivAtt");
        }

        sdata = false;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);
    }

    // Load preferences
    private void getPrefs()
    {
        buttonSoundPref = prefs.getBoolean("pref_button_sound", false);
        buttonAlertSound = prefs.getString("alert_button_sound", null);
        brightPref = prefs.getBoolean("pref_bright", true);
        screenOrientL = prefs.getBoolean("screen_Orientation", false);
    }

    @SuppressLint("LongLogTag")
    @Override
    protected void onResume()
    {
        super.onResume();

        // clear any existing views
        individ_area.removeAllViews();

        // setup the data sources
        individualsDataSource = new IndividualsDataSource(this);
        individualsDataSource.open();

        // get last found locality from temp 
        tempDataSource = new TempDataSource(this);
        tempDataSource.open();
        temp = tempDataSource.getTemp();

        String sLocality = temp.temp_loc;

        countDataSource = new CountDataSource(this);
        countDataSource.open();

        // set title
        try
        {
            //noinspection ConstantConditions
            getSupportActionBar().setTitle(specName);
        } catch (NullPointerException e)
        {
            if (MyDebug.LOG)
                Log.e(TAG, "NullPointerException: No species name!");
        }

        individuals = individualsDataSource.getIndividual(i_id);
        counts = countDataSource.getCountById(count_id);

        // display the editable data
        eiw = new EditIndividualWidget(this, null);
        eiw.setWidgetLocality1(getString(R.string.locality));
        eiw.setWidgetLocality2(sLocality);

        eiw.setWidgetZCoord1(getString(R.string.zcoord));
        eiw.setWidgetZCoord2(String.format("%.1f", height));

        eiw.setWidgetStadium1(getString(R.string.stadium));
        switch (iAtt)
        {
        case 1: // ♂♀
            eiw.setWidgetStadium2(getString(R.string.stadium_1));
            break;
        case 2: // ♂
            eiw.setWidgetStadium2(getString(R.string.stadium_1));
            break;
        case 3: // ♀
            eiw.setWidgetStadium2(getString(R.string.stadium_1));
            break;
        case 4: // Pupa
            eiw.setWidgetStadium2(getString(R.string.stadium_2));
            break;
        case 5: // Larva
            eiw.setWidgetStadium2(getString(R.string.stadium_3));
            break;
        case 6: // Egg
            eiw.setWidgetStadium2(getString(R.string.stadium_4));
            break;
        }

        eiw.setWidgetState1(getString(R.string.state));
        eiw.setWidgetState2(individuals.state_1_6);

        eiw.setWidgetCount1(getString(R.string.count1)); // icount
        eiw.setWidgetCount2(1);

        eiw.setWidgetIndivNote1(getString(R.string.note));
        eiw.setWidgetIndivNote2(individuals.notes);

        eiw.setWidgetXCoord1(getString(R.string.xcoord));
        eiw.setWidgetXCoord2(Double.toString(latitude));

        eiw.setWidgetYCoord1(getString(R.string.ycoord));
        eiw.setWidgetYCoord2(Double.toString(longitude));

        individ_area.addView(eiw);
    }

    @Override
    protected void onPause()
    {
        super.onPause();

        if (!sdata)
        {
            saveData();
        }

        // close the data sources
        individualsDataSource.close();
        tempDataSource.close();
        countDataSource.close();
    }

    private boolean saveData()
    {
        buttonSound();

        // save individual data
        // Locality (from reverse geocoding in CountingActivity or manual input) 
        individuals.locality = eiw.getWidgetLocality2();

        // Uncertainty
        if (latitude != 0)
        {
            individuals.uncert = String.valueOf(uncertainty);
        }
        else
        {
            individuals.uncert = "0";
        }

        temp.temp_loc = eiw.getWidgetLocality2();

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
//            Toast.makeText(this, getString(R.string.valState), Toast.LENGTH_SHORT).show();
            showSnackbarRed(getString(R.string.valState));

            return false;
        }

        // number of individuals
        int newcount = eiw.getWidgetCount2();
        if (newcount > 0) // valid newcount
        {
            switch (iAtt)
            {
            case 1:
                //  counts.count = counts.count + newcount - 1; // -1 when CountingActivity already added 1
                counts.count_f1i = counts.count_f1i + newcount;
                individuals.icount = newcount;
                individuals.sex = "-";
                individuals.icategory = 1;
                countDataSource.saveCountf1i(counts);
                break;

            case 2:
                counts.count_f2i = counts.count_f2i + newcount;
                individuals.icount = newcount;
                individuals.sex = "m";
                individuals.icategory = 2;
                countDataSource.saveCountf2i(counts);
                break;
            case 3:
                counts.count_f3i = counts.count_f3i + newcount;
                individuals.icount = newcount;
                individuals.sex = "f";
                individuals.icategory = 3;
                countDataSource.saveCountf3i(counts);
                break;
            case 4:
                counts.count_pi = counts.count_pi + newcount;
                individuals.icount = newcount;
                individuals.sex = "-";
                individuals.icategory = 4;
                countDataSource.saveCountpi(counts);
                break;
            case 5:
                counts.count_li = counts.count_li + newcount;
                individuals.icount = newcount;
                individuals.sex = "-";
                individuals.icategory = 5;
                countDataSource.saveCountli(counts);
                break;
            case 6:
                counts.count_ei = counts.count_ei + newcount;
                individuals.icount = newcount;
                individuals.sex = "-";
                individuals.icategory = 6;
                countDataSource.saveCountei(counts);
                break;
            }

            // Notes
            String newnotes = eiw.getWidgetIndivNote2();
            if (!newnotes.equals(""))
            {
                individuals.notes = newnotes;
            }

            individualsDataSource.saveIndividual(individuals);
        }
        else // newcount is <= 1
        {
            
//            Toast.makeText(this, getString(R.string.warnCount), Toast.LENGTH_SHORT).show();
            showSnackbarRed(getString(R.string.warnCount));
            return false; // forces input newcount > 0
        }
        
        tempDataSource.saveTempLoc(temp);
        sdata = true;

        return true;
    }

    private void showSnackbarRed(String str)
    {
        View view = findViewById(R.id.editIndividualScreen);
        Snackbar sB = Snackbar.make(view, Html.fromHtml("<font color=\"#ff0000\"><b>" +  str + "</font></b>"), Snackbar.LENGTH_LONG);
        TextView tv = sB.getView().findViewById(R.id.snackbar_text);
        tv.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        sB.show();
    }
    private void buttonSound()
    {
        if (buttonSoundPref)
        {
            try
            {
                Uri notification;
                if (isNotBlank(buttonAlertSound) && buttonAlertSound != null)
                {
                    notification = Uri.parse(buttonAlertSound);
                }
                else
                {
                    notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                }
                Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);
                r.play();
            } catch (Exception e)
            {
                if (MyDebug.LOG)
                    Log.e(TAG, "could not play button sound.", e);
            }
        }
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
                countDataSource.close();
            }
        }
        return super.onOptionsItemSelected(item);
    }

    public void onSharedPreferenceChanged(SharedPreferences prefs, String key)
    {
        ScrollView individ_screen = findViewById(R.id.editIndividualScreen);
        individ_screen.setBackground(null);
        getPrefs();
        if (screenOrientL)
        {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }
        else
        {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }

        bMap = tourCount.decodeBitmap(R.drawable.kbackground, tourCount.width, tourCount.height);
        bg = new BitmapDrawable(individ_screen.getResources(), bMap);
        individ_screen.setBackground(bg);
    }

    /**
     * Checks if a CharSequence is whitespace, empty ("") or null
     * <p>
     * isBlank(null)      = true
     * isBlank("")        = true
     * isBlank(" ")       = true
     * isBlank("bob")     = false
     * isBlank("  bob  ") = false
     *
     * @param cs the CharSequence to check, may be null
     * @return {@code true} if the CharSequence is null, empty or whitespace
     */
    private static boolean isBlank(final CharSequence cs)
    {
        int strLen;
        if (cs == null || (strLen = cs.length()) == 0)
        {
            return true;
        }
        for (int i = 0; i < strLen; i++)
        {
            if (!Character.isWhitespace(cs.charAt(i)))
            {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks if a CharSequence is not empty (""), not null and not whitespace only.
     * <p>
     * isNotBlank(null)      = false
     * isNotBlank("")        = false
     * isNotBlank(" ")       = false
     * isNotBlank("bob")     = true
     * isNotBlank("  bob  ") = true
     *
     * @param cs the CharSequence to check, may be null
     * @return {@code true} if the CharSequence is
     * not empty and not null and not whitespace
     */
    private static boolean isNotBlank(final CharSequence cs)
    {
        return !isBlank(cs);
    }

}
