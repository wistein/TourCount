package com.wmstein.tourcount;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.CursorIndexOutOfBoundsException;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Toast;

import com.wmstein.tourcount.database.Count;
import com.wmstein.tourcount.database.CountDataSource;
import com.wmstein.tourcount.database.IndividualsDataSource;
import com.wmstein.tourcount.database.Section;
import com.wmstein.tourcount.database.SectionDataSource;
import com.wmstein.tourcount.widgets.CountingWidget;
import com.wmstein.tourcount.widgets.NotesWidget;

import org.apache.commons.lang3.StringUtils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Created by milo on 05/05/2014.
 * Changed by wmstein on 18.04.2016
 */

public class CountingActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener
{
    private static final String TAG = CountingActivity.class.getSimpleName();
    // private static String TAG = "tourcountCountingActivity";

    TourCountApplication tourCount;
    SharedPreferences prefs;
    LinearLayout count_area;
    LinearLayout notes_area;

    private LocationManager locationManager;
    private LocationListener locationListener;
    private String provider;

    // preferences
    private boolean awakePref;
    private boolean fontPref;
    private boolean buttonSoundPref;
    private String buttonAlertSound;
    private String latitude, longitude;

    // the actual data
    Section section;
    List<Count> counts;

    List<CountingWidget> countingWidgets;

    private SectionDataSource sectionDataSource;
    private CountDataSource countDataSource;
    private IndividualsDataSource individualsDataSource;
    
    private int i_Id = 0;
    private String spec_name;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_counting);
        
        sectionDataSource = new SectionDataSource(this);
        countDataSource = new CountDataSource(this);
        individualsDataSource = new IndividualsDataSource(this);

        tourCount = (TourCountApplication) getApplication();
        prefs = TourCountApplication.getPrefs();
        prefs.registerOnSharedPreferenceChangeListener(this);
        getPrefs();

        ScrollView counting_screen = (ScrollView) findViewById(R.id.countingScreen);
        counting_screen.setBackground(tourCount.getBackground());

        count_area = (LinearLayout) findViewById(R.id.countCountLayout);
        notes_area = (LinearLayout) findViewById(R.id.countNotesLayout);

        if (awakePref)
        {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

        // LocationManager-Instanz ermitteln
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        // Liste mit Namen aller Provider erfragen
        List<String> providers = locationManager.getAllProviders();
        for (String name : providers)
        {
            LocationProvider lp = locationManager.getProvider(name);
            Log.d(TAG, lp.getName() + " --- isProviderEnabled(): "
                + locationManager.isProviderEnabled(name));
            Log.d(TAG, "requiresCell(): " + lp.requiresCell());
            Log.d(TAG, "requiresNetwork(): " + lp.requiresNetwork());
            Log.d(TAG, "requiresSatellite(): " + lp.requiresSatellite());
        }

        // Provider mit grober Auflösung und niedrigen Energieverbrauch
        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_COARSE);
        criteria.setPowerRequirement(Criteria.POWER_LOW);
        provider = locationManager.getBestProvider(criteria, true);
        Log.d(TAG, provider);

        // LocationListener-Objekt erzeugen
        locationListener = new LocationListener()
        {
            @Override
            public void onStatusChanged(String provider, int status,
                                        Bundle extras)
            {
                Log.d(TAG, "onStatusChanged()");
            }

            @Override
            public void onProviderEnabled(String provider)
            {
                Log.d(TAG, "onProviderEnabled()");
            }

            @Override
            public void onProviderDisabled(String provider)
            {
                Log.d(TAG, "onProviderDisabled()");
            }

            @Override
            public void onLocationChanged(Location location)
            {
                Log.d(TAG, "onLocationChanged()");
                if (location != null)
                {
                    latitude = "" + location.getLatitude();
                    longitude = "" + location.getLongitude();
                }
            }
        };

    }

    /*
     * So preferences can be loaded at the start, and also when a change is detected.
     */
    private void getPrefs()
    {
        awakePref = prefs.getBoolean("pref_awake", true);
        fontPref = prefs.getBoolean("pref_note_font", false);
        buttonSoundPref = prefs.getBoolean("pref_button_sound", false);
        buttonAlertSound = prefs.getString("alert_button_sound", null);
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        
        try
        {
            locationManager.requestLocationUpdates(provider, 3000, 0, locationListener);
        } catch (Exception e)
        {
            Toast.makeText(this, "Exception" + e + getString(R.string.no_GPS), Toast.LENGTH_LONG).show();
        }
            
        // clear any existing views
        count_area.removeAllViews();
        notes_area.removeAllViews();

        // setup the data sources
        sectionDataSource.open();
        countDataSource.open();
        individualsDataSource.open();

        // load the data
        // sections
        try
        {
            section = sectionDataSource.getSection();
        } catch (CursorIndexOutOfBoundsException e)
        {
            Log.e(TAG, "Problem loading section: " + e.toString());
            Toast.makeText(CountingActivity.this, getString(R.string.getHelp), Toast.LENGTH_LONG).show();
            finish();
        }

        getSupportActionBar().setTitle(section.name);

        // display list notes
        if (section.notes != null)
        {
            if (!section.notes.isEmpty())
            {
                NotesWidget section_notes = new NotesWidget(this, null);
                section_notes.setNotes(section.notes);
                section_notes.setFont(fontPref);
                notes_area.addView(section_notes);
            }
        }

        // display counts with notes and alerts
        countingWidgets = new ArrayList<>();
        counts = countDataSource.getAllSpecies();

        // display all the counts by adding them to countCountLayout
        for (Count count : counts)
        {
            CountingWidget widget = new CountingWidget(this, null);
            widget.setCount(count);
            countingWidgets.add(widget);
            count_area.addView(widget);

            // add a species note widget if there are any notes
            if (StringUtils.isNotBlank(count.notes))
            {
                NotesWidget count_notes = new NotesWidget(this, null);
                count_notes.setNotes(count.notes);
                count_notes.setFont(fontPref);
                count_area.addView(count_notes);
            }
        }

        if (awakePref)
        {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        locationManager.removeUpdates(locationListener);

        // save the data
        saveData();

        // close the data sources
        sectionDataSource.close();
        countDataSource.close();
        individualsDataSource.close();

        // N.B. a wakelock might not be held, e.g. if someone is using Cyanogenmod and
        // has denied wakelock permission to TourCount
        if (awakePref)
        {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

    }

    /**************************************************
     * Zählerstände sichern
     */
    private void saveData()
    {

        Toast.makeText(CountingActivity.this, getString(R.string.sectSaving) + " " + section.name + "!", Toast.LENGTH_SHORT).show();
        for (Count count : counts)
        {
            countDataSource.saveCount(count);
        }
    }


    /***************/
    public void saveAndExit(View view)
    {
        saveData();
        locationManager.removeUpdates(locationListener);
        super.finish();
    }

    // Triggered by count up button
    // starts EditIndividualActivity
    public void countUp(View view)
    {
        buttonSound();
        int count_id = Integer.valueOf(view.getTag().toString());
        CountingWidget widget = getCountFromId(count_id);
        if (widget != null)
        {
            widget.countUp();
        }

        // Show coords latitude, longitude for current count
        // Toast.makeText(CountingActivity.this, "Latitude: " + latitude + "\nLongitude: " + longitude, Toast.LENGTH_SHORT).show();
        
        // append individual with its Id, coords, date and time
        int uncert; // uncertainty about position (m)
        
        if (latitude != null)
            uncert = 20;
        else
            uncert = 10000;
        
        String name, datestamp, timestamp;
        name = widget.count.name;
        datestamp = getcurDate();
        timestamp = getcurTime();
        
        i_Id = individualsDataSource.saveIndividual(individualsDataSource.createIndividuals
            (count_id, name, latitude, longitude, uncert, datestamp, timestamp));

        // get edited info for individual and start EditIndividualActivity
        Intent intent = new Intent(CountingActivity.this, EditIndividualActivity.class);
        intent.putExtra("indiv_id", i_Id);
        intent.putExtra("SName", widget.count.name);
        intent.putExtra("Latitude", latitude);
        intent.putExtra("Longitude", longitude);
        startActivity(intent);
    }

    // Triggered by count down button
    // deletes last last count only when directly followed
    public void countDown(View view)
    {
        buttonSound();
        int count_id = Integer.valueOf(view.getTag().toString());
        CountingWidget widget = getCountFromId(count_id);
        spec_name = widget.count.name; // set spec_name for toast in deleteIndividual
        if (widget != null)
        {
            widget.countDown();
            i_Id = individualsDataSource.readLastIndividual(count_id);
            if (i_Id > 0)
            {
                deleteIndividual(i_Id);
                i_Id--;
            }
        }
    }

    // delete individual for count_id
    public void deleteIndividual(int id)
    {
        Toast.makeText(CountingActivity.this, getString(R.string.indivdel1) + spec_name, Toast.LENGTH_SHORT).show();
        System.out.println(getString(R.string.indivdel) + " " + id);
        individualsDataSource.deleteIndividualById(id);
    }

    // Date for date_stamp
    public String getcurDate()
    {
        Date date = new Date();
        DateFormat dform;
        String lng = Locale.getDefault().toString().substring(0, 2);

        if (lng.equals("de"))
        {
            dform = new SimpleDateFormat("dd.MM.yyyy");
        }
        else
        {
            dform = new SimpleDateFormat("yyyy-MM-dd");
        }
        return dform.format(date);
    }

    // Date for time_stamp
    public String getcurTime()
    {
        Date date = new Date();
        DateFormat dform = new SimpleDateFormat("HH:mm");
        return dform.format(date);
    }

    public void edit(View view)
    {
        int count_id = Integer.valueOf(view.getTag().toString());
        Intent intent = new Intent(CountingActivity.this, CountOptionsActivity.class);
        intent.putExtra("count_id", count_id);
        startActivity(intent);
    }

    /*
     * This is the lookup to get a counting widget (with references to the
     * associated count) from the list of widgets.
     */
    public CountingWidget getCountFromId(int id)
    {
        for (CountingWidget widget : countingWidgets)
        {
            if (widget.count.id == id)
            {
                return widget;
            }
        }
        return null;
    }

    public void buttonSound()
    {
        if (buttonSoundPref)
        {
            try
            {
                Uri notification;
                if (StringUtils.isNotBlank(buttonAlertSound) && buttonAlertSound != null)
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
                e.printStackTrace();
            }
        }
    }


    /***************************************
     * Pop up various exciting messages if the user has not bothered to turn them off in the
     * settings...
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.counting, menu);
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
            return true;
        }
        else if (id == R.id.menuEditSection)
        {
            Intent intent = new Intent(CountingActivity.this, EditSectionActivity.class);
            startActivity(intent);
            return true;
        }
        else if (id == R.id.menuSaveExit)
        {
            saveData();
            super.finish();
            return true;
        }
        else if (id == R.id.action_share)
        {
            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_TEXT, section.notes);
            sendIntent.putExtra(Intent.EXTRA_SUBJECT, section.name);
            sendIntent.setType("text/plain");
            startActivity(Intent.createChooser(sendIntent, getResources().getText(R.string.send_to)));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void onSharedPreferenceChanged(SharedPreferences prefs, String key)
    {
        ScrollView counting_screen = (ScrollView) findViewById(R.id.countingScreen);
        counting_screen.setBackground(null);
        counting_screen.setBackground(tourCount.setBackground());
        getPrefs();
    }
    
}
