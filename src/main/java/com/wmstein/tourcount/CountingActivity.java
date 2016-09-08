package com.wmstein.tourcount;

import android.content.Context;
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
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
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
import com.wmstein.tourcount.widgets.CountingWidgetLH;
import com.wmstein.tourcount.widgets.NotesWidget;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * CountingActivity is the central activity of TourCount. It provides the counter, starts GPS-location polling,
 * starts EditIndividualActivity, starts editSectionActivity, switches screen off when pocketed and allows sending notes.
 * Basic counting functions created by milo for beecount on 05/05/2014.
 * Modified and enhanced by wmstein on 18.04.2016
 */

public class CountingActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener
{
    private static final String TAG = CountingActivity.class.getSimpleName();
    // private static String TAG = "tourcountCountingActivity";

    TourCountApplication tourCount;
    SharedPreferences prefs;
    LinearLayout count_area;
    LinearLayout notes_area;
    LinearLayout count_areaLH;
    LinearLayout notes_areaLH;

    // Location info handling
    private LocationManager locationManager;
    private LocationListener locationListener;
    private String provider;

    // Proximity sensor handling for screen on/off
    private PowerManager mPowerManager;
    private PowerManager.WakeLock mProximityWakeLock;

    // preferences
    private boolean awakePref;
    private boolean brightPref;
    private boolean fontPref;
    private boolean handPref;
    private boolean buttonSoundPref;
    private String buttonAlertSound;
    private double latitude, longitude, height;

    // the actual data
    Section section;
    List<Count> counts;

    List<CountingWidget> countingWidgets;
    List<CountingWidgetLH> countingWidgetsLH;

    private SectionDataSource sectionDataSource;
    private CountDataSource countDataSource;
    private IndividualsDataSource individualsDataSource;

    private int i_Id = 0;
    private String spec_name;
    private int spec_count;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        Context context = this.getApplicationContext();

        sectionDataSource = new SectionDataSource(this);
        countDataSource = new CountDataSource(this);
        individualsDataSource = new IndividualsDataSource(this);

        tourCount = (TourCountApplication) getApplication();
        prefs = TourCountApplication.getPrefs();
        prefs.registerOnSharedPreferenceChangeListener(this);
        getPrefs();

        if (handPref) // if left-handed counting page
        {
            setContentView(R.layout.activity_counting_lh);
        }
        else
        {
            setContentView(R.layout.activity_counting);
        }

        // Set full brightness of screen
        if (brightPref)
        {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            WindowManager.LayoutParams params = getWindow().getAttributes();
            params.screenBrightness = 1.0f;
            getWindow().setAttributes(params);
        }

        if (handPref) // if left-handed counting page
        {
            ScrollView counting_screen = (ScrollView) findViewById(R.id.countingScreenLH);
            counting_screen.setBackground(tourCount.getBackground());
            count_areaLH = (LinearLayout) findViewById(R.id.countCountLayoutLH);
            notes_areaLH = (LinearLayout) findViewById(R.id.countNotesLayoutLH);
        }
        else
        {
            ScrollView counting_screen = (ScrollView) findViewById(R.id.countingScreen);
            counting_screen.setBackground(tourCount.getBackground());
            count_area = (LinearLayout) findViewById(R.id.countCountLayout);
            notes_area = (LinearLayout) findViewById(R.id.countNotesLayout);
        }

        if (awakePref)
        {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

        // check for API-Level >= 21
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
        {
            mPowerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            if (mPowerManager.isWakeLockLevelSupported(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK))
            {
                mProximityWakeLock = mPowerManager.newWakeLock(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK, "WAKE LOCK");
            }
            enableProximitySensor();
        }

        // LocationManager-Instanz ermitteln
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        // Liste mit Namen aller Provider erfragen
        List<String> providers = locationManager.getAllProviders();
        for (String name : providers)
        {
            LocationProvider lp = locationManager.getProvider(name);
            Log.d(TAG, lp.getName() + " --- isProviderEnabled(): " + locationManager.isProviderEnabled(name));
            Log.d(TAG, "requiresCell(): " + lp.requiresCell());
            Log.d(TAG, "requiresNetwork(): " + lp.requiresNetwork());
            Log.d(TAG, "requiresSatellite(): " + lp.requiresSatellite());
        }

        // Best possible provider
        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        criteria.setPowerRequirement(Criteria.POWER_HIGH);
        provider = locationManager.getBestProvider(criteria, true);
        Log.d(TAG, "Provider: " + provider);

        // LocationListener-Objekt erzeugen
        locationListener = new LocationListener()
        {
            @Override
            public void onStatusChanged(String provider, int status, Bundle extras)
            {
                // Log.d(TAG, "onStatusChanged()");
            }

            @Override
            public void onProviderEnabled(String provider)
            {
                // Log.d(TAG, "onProviderEnabled()");
            }

            @Override
            public void onProviderDisabled(String provider)
            {
                // Log.d(TAG, "onProviderDisabled()");
            }

            @Override
            public void onLocationChanged(Location location)
            {
                // Log.d(TAG, "onLocationChanged()");
                if (location != null)
                {
                    latitude = location.getLatitude();
                    longitude = location.getLongitude();
                    height = location.getAltitude();
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
        brightPref = prefs.getBoolean("pref_bright", true);
        fontPref = prefs.getBoolean("pref_note_font", false);
        handPref = prefs.getBoolean("pref_left_hand", false); // left-handed counting page
        buttonSoundPref = prefs.getBoolean("pref_button_sound", false);
        buttonAlertSound = prefs.getString("alert_button_sound", null);
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        // check for API-Level >= 21
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
        {
            enableProximitySensor();
        }

        try
        {
            locationManager.requestLocationUpdates(provider, 3000, 0, locationListener);
        } catch (Exception e)
        {
            Toast.makeText(this, "Exception" + e + getString(R.string.no_GPS), Toast.LENGTH_LONG).show();
        }

/*        
         // get coarse location service
        try
        {
            locationManager.requestLocationUpdates(LocationManager.PASSIVE_PROVIDER, 3000, 0, locationListener);
        } catch (Exception e)
        {
            // do nothing
        }
        try
        {
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 3000, 0, locationListener);
        } catch (Exception e)
        {
            // do nothing
        }
*/

        // clear any existing views
        if (handPref) // if left-handed counting page
        {
            count_areaLH.removeAllViews();
            notes_areaLH.removeAllViews();
        }
        else
        {
            count_area.removeAllViews();
            notes_area.removeAllViews();
        }

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
                if (handPref) // if left-handed counting page
                {
                    NotesWidget section_notes = new NotesWidget(this, null);
                    section_notes.setNotes(section.notes);
                    section_notes.setFont(fontPref);
                    notes_areaLH.addView(section_notes);
                }
                else
                {
                    NotesWidget section_notes = new NotesWidget(this, null);
                    section_notes.setNotes(section.notes);
                    section_notes.setFont(fontPref);
                    notes_area.addView(section_notes);
                }
            }
        }

        // display counts with notes and alerts
        if (handPref) // if left-handed counting page
        {
            countingWidgetsLH = new ArrayList<>();
        }
        else
        {
            countingWidgets = new ArrayList<>();
        }

        counts = countDataSource.getAllSpecies();

        // display all the counts by adding them to countCountLayout
        if (handPref) // if left-handed counting page
        {
            for (Count count : counts)
            {
                CountingWidgetLH widget = new CountingWidgetLH(this, null);
                widget.setCount(count);
                countingWidgetsLH.add(widget);
                count_areaLH.addView(widget);

                // add a species note widget if there are any notes
                if (isNotBlank(count.notes))
                {
                    NotesWidget count_notes = new NotesWidget(this, null);
                    count_notes.setNotes(count.notes);
                    count_notes.setFont(fontPref);
                    count_areaLH.addView(count_notes);
                }
            }
        }
        else
        {
            for (Count count : counts)
            {
                CountingWidget widget = new CountingWidget(this, null);
                widget.setCount(count);
                countingWidgets.add(widget);
                count_area.addView(widget);

                // add a species note widget if there are any notes
                if (isNotBlank(count.notes))
                {
                    NotesWidget count_notes = new NotesWidget(this, null);
                    count_notes.setNotes(count.notes);
                    count_notes.setFont(fontPref);
                    count_area.addView(count_notes);
                }
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

        // check for API-Level >= 21
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
        {
            disableProximitySensor(true);
        }

        try
        {
            locationManager.removeUpdates(locationListener);
        } catch (Exception e)
        {
            // do nothing
        }

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
        try
        {
            locationManager.removeUpdates(locationListener);
        } catch (Exception e)
        {
            // do nothing
        }

        // check for API-Level >= 21
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
        {
            disableProximitySensor(true);
        }

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
        
        // check for API-Level >= 21
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
        {
            disableProximitySensor(true);
        }

        // Show coords latitude, longitude for current count
        // Toast.makeText(CountingActivity.this, "Latitude: " + latitude + "\nLongitude: " + longitude, Toast.LENGTH_SHORT).show();

        // append individual with its Id, coords, date and time
        String uncert; // uncertainty about position (m)

        if (provider.equals("gps") && latitude != 0)
        {
            uncert = "20";
        }
        else
        {
            uncert = null;
            // Toast.makeText(CountingActivity.this, "Provider: " + provider + "Uncert: " + uncert, Toast.LENGTH_SHORT).show();
        }
        
        String name, datestamp, timestamp;
        name = widget.count.name;
        datestamp = getcurDate();
        timestamp = getcurTime();

        i_Id = individualsDataSource.saveIndividual(individualsDataSource.createIndividuals
            (count_id, name, latitude, longitude, height, uncert, datestamp, timestamp));

        // get edited info for individual and start EditIndividualActivity
        Intent intent = new Intent(CountingActivity.this, EditIndividualActivity.class);
        intent.putExtra("indiv_id", i_Id);
        intent.putExtra("SName", widget.count.name);
        intent.putExtra("Latitude", latitude);
        intent.putExtra("Longitude", longitude);
        intent.putExtra("Height", height);
        startActivity(intent);
    }

    public void countUpLH(View view)
    {
        buttonSound();
        int count_id = Integer.valueOf(view.getTag().toString());
        CountingWidgetLH widget = getCountFromIdLH(count_id);
        if (widget != null)
        {
            widget.countUpLH();
        }

        // check for API-Level >= 21
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
        {
            disableProximitySensor(true);
        }

        // Show coords latitude, longitude for current count
        // Toast.makeText(CountingActivity.this, "Latitude: " + latitude + "\nLongitude: " + longitude, Toast.LENGTH_SHORT).show();

        // append individual with its Id, coords, date and time
        String uncert; // uncertainty about position (m)

        if (provider.equals("gps") && latitude != 0)
        {
            uncert = "20";
        }
        else
        {
            uncert = null;
            // Toast.makeText(CountingActivity.this, "Provider: " + provider + "Uncert: " + uncert, Toast.LENGTH_SHORT).show();
        }

        String name, datestamp, timestamp;
        name = widget.count.name;
        datestamp = getcurDate();
        timestamp = getcurTime();

        i_Id = individualsDataSource.saveIndividual(individualsDataSource.createIndividuals
            (count_id, name, latitude, longitude, height, uncert, datestamp, timestamp));

        // get edited info for individual and start EditIndividualActivity
        Intent intent = new Intent(CountingActivity.this, EditIndividualActivity.class);
        intent.putExtra("indiv_id", i_Id);
        intent.putExtra("SName", widget.count.name);
        intent.putExtra("Latitude", latitude);
        intent.putExtra("Longitude", longitude);
        intent.putExtra("Height", height);
        startActivity(intent);
    }

    // Triggered by count down button
    // deletes last count
    public void countDown(View view)
    {
        buttonSound();
        int count_id = Integer.valueOf(view.getTag().toString());
        CountingWidget widget = getCountFromId(count_id);
        spec_name = widget.count.name; // set spec_name for toast in deleteIndividual
        spec_count = widget.count.count;
        if (spec_count > 0)
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

    // Triggered by count down button from left-hand view
    // deletes last count
    public void countDownLH(View view)
    {
        buttonSound();
        int count_id = Integer.valueOf(view.getTag().toString());
        CountingWidgetLH widget = getCountFromIdLH(count_id);
        spec_name = widget.count.name; // set spec_name for toast in deleteIndividual
        spec_count = widget.count.count;
        if (spec_count > 0)
        {
            widget.countDownLH();
            i_Id = individualsDataSource.readLastIndividual(count_id);
            if (i_Id > 0)
            {
                deleteIndividual(i_Id);
                i_Id--;
            }
        }
    }

    /*
     * Get a counting widget (with reference to the associated count) from the list of widgets.
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

    /*
     * Get a left-handed counting widget (with references to the
     * associated count) from the list of widgets.
     */
    public CountingWidgetLH getCountFromIdLH(int id)
    {
        for (CountingWidgetLH widget : countingWidgetsLH)
        {
            if (widget.count.id == id)
            {
                return widget;
            }
        }
        return null;
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

    // Edit count options
    public void edit(View view)
    {
        // check for API-Level >= 21
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
        {
            disableProximitySensor(true);
        }

        int count_id = Integer.valueOf(view.getTag().toString());
        Intent intent = new Intent(CountingActivity.this, CountOptionsActivity.class);
        intent.putExtra("count_id", count_id);
        startActivity(intent);
    }

    public void buttonSound()
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
                e.printStackTrace();
            }
        }
    }


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
        if (id == R.id.menuEditSection)
        {
            // check for API-Level >= 21
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
            {
                disableProximitySensor(true);
            }

            Intent intent = new Intent(CountingActivity.this, EditSectionActivity.class);
            startActivity(intent);
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

    private void enableProximitySensor()
    {
        if (mProximityWakeLock == null)
        {
            return;
        }

        if (!mProximityWakeLock.isHeld())
        {
            mProximityWakeLock.acquire();
        }
    }


    private void disableProximitySensor(boolean waitForFarState)
    {
        if (mProximityWakeLock == null)
        {
            return;
        }
        if (mProximityWakeLock.isHeld())
        {
            int flags = waitForFarState ? PowerManager.RELEASE_FLAG_WAIT_FOR_NO_PROXIMITY : 0;
            mProximityWakeLock.release(flags);
        }
    }

    public void onSharedPreferenceChanged(SharedPreferences prefs, String key)
    {
        getPrefs();
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
    public static boolean isBlank(final CharSequence cs)
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
    public static boolean isNotBlank(final CharSequence cs)
    {
        return !isBlank(cs);
    }

}
