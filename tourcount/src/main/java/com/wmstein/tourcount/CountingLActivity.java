package com.wmstein.tourcount;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
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
import android.os.StrictMode;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.wmstein.egm.EarthGravitationalModel;
import com.wmstein.tourcount.database.Count;
import com.wmstein.tourcount.database.CountDataSource;
import com.wmstein.tourcount.database.IndividualsDataSource;
import com.wmstein.tourcount.database.Section;
import com.wmstein.tourcount.database.SectionDataSource;
import com.wmstein.tourcount.widgets.CountingWidget;
import com.wmstein.tourcount.widgets.CountingWidgetLH;
import com.wmstein.tourcount.widgets.CountingWidget_head1;
import com.wmstein.tourcount.widgets.CountingWidget_head2;
import com.wmstein.tourcount.widgets.NotesWidget;

import java.io.IOException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/****************************************************************************************
 * CountingLActivity is necessary for overcoming the limitations of spinner.
 * It is the central activity of TourCount for landscape mode. 
 * It provides the same functionality as CountingActivity that are the counters, 
 * it starts GPS-location polling, starts EditIndividualActivity, 
 * starts editSectionActivity, switches screen off when pocketed and 
 * allows taking pictures and sending notes.
 * 
 * CountingLActivity uses CountingWidgetLH.java, NotesWidget.java, 
 * activity_counting_lh.xml and widget_counting_lhi.xml
 * 
 * Basic counting functions created by milo for BeeCount on 05/05/2014.
 * Adopted, modified and enhanced for TourCount by wmstein since 2016-04-18,
 * last modification on 2018-03-26
 */
public class CountingLActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener
{
    //private static final String TAG = CountingActivity.class.getSimpleName();
    private static String TAG = "TourCountCountLAct";

    private SharedPreferences prefs;

    private int section_id;
    private int iid = 1;
    private LinearLayout count_area;

    private LinearLayout head_area2;
    private LinearLayout notes_area1;

    // the actual data
    private Count count;
    private Section section;
    private List<Count> counts;
    private List<CountingWidget> countingWidgets;
    private List<CountingWidgetLH> countingWidgetsLH;
    private Spinner spinnerL;
    private int itemPosition = 0;
    private int oldCount;

    // Location info handling
    private LocationManager locationManager;
    private LocationListener locationListener;
    private String provider;
    private double latitude, longitude, height, uncertainty;

    private PowerManager.WakeLock mProximityWakeLock;

    // preferences
    private boolean awakePref;
    private boolean brightPref;
    private String sortPref;
    private boolean fontPref;
    private boolean lhandPref; // true for lefthand mode of counting screen
    private boolean buttonSoundPref;
    private String buttonAlertSound;
    private boolean metaPref;      // option for reverse geocoding
    private String emailString = ""; // mail address for OSM query

    private String[] idArray;
    private String[] nameArray;
    private String[] codeArray;
    private Integer[] imageArray;

    // data sources
    private SectionDataSource sectionDataSource;
    private CountDataSource countDataSource;
    private IndividualsDataSource individualsDataSource;

    private int i_Id = 0;
    private String spec_name; // could be used in prepared toast in deleteIndividual()
    private int spec_count;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        Context context = this.getApplicationContext();

        sectionDataSource = new SectionDataSource(this);
        countDataSource = new CountDataSource(this);
        individualsDataSource = new IndividualsDataSource(this);

        TourCountApplication tourCount = (TourCountApplication) getApplication();
        prefs = TourCountApplication.getPrefs();
        prefs.registerOnSharedPreferenceChangeListener(this);
        getPrefs();

        if (lhandPref) // if left-handed counting page
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

        if (lhandPref) // if left-handed counting page
        {
            LinearLayout counting_screen = (LinearLayout) findViewById(R.id.countingScreenLH);
            if (counting_screen != null)
            {
                counting_screen.setBackground(tourCount.getBackground());
            }
            count_area = (LinearLayout) findViewById(R.id.countCountiLayoutLH);
            notes_area1 = (LinearLayout) findViewById(R.id.sectionNotesLayoutLH);
            head_area2 = (LinearLayout) findViewById(R.id.countHead2LayoutLH);
        }
        else
        {
            LinearLayout counting_screen = (LinearLayout) findViewById(R.id.countingScreen);
            if (counting_screen != null)
            {
                counting_screen.setBackground(tourCount.getBackground());
            }
            count_area = (LinearLayout) findViewById(R.id.countCountiLayout);
            notes_area1 = (LinearLayout) findViewById(R.id.sectionNotesLayout);
            head_area2 = (LinearLayout) findViewById(R.id.countHead2Layout);
        }

        if (savedInstanceState != null)
        {
            spinnerL.setSelection(savedInstanceState.getInt("itemPosition", 0));
            iid = savedInstanceState.getInt("count_id");
        }

        if (awakePref)
        {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

        // check for API-Level >= 21
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
        {
            PowerManager mPowerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            if (mPowerManager.isWakeLockLevelSupported(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK))
            {
                mProximityWakeLock = mPowerManager.newWakeLock(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK, "WAKE LOCK");
            }
            enableProximitySensor();
        }

        // get parameters from CountingActivity
        Bundle extras = getIntent().getExtras();
        if (extras != null)
        {
            latitude = extras.getDouble("Latitude");
            longitude = extras.getDouble("Longitude");
            height = extras.getDouble("Height");
            uncertainty = extras.getDouble("Uncert");
            section_id = extras.getInt("section_id");
        }

    } // End of onCreate

    /*
     * So preferences can be loaded at the start, and also when a change is detected.
     */
    private void getPrefs()
    {
        awakePref = prefs.getBoolean("pref_awake", true);      // stay awake while counting
        brightPref = prefs.getBoolean("pref_bright", true);    // bright counting page
        sortPref = prefs.getString("pref_sort_sp", "none");    // sorted species list on counting page
        fontPref = prefs.getBoolean("pref_note_font", false);  // larger font for remarks
        lhandPref = prefs.getBoolean("pref_left_hand", false); // left-handed counting page
        buttonSoundPref = prefs.getBoolean("pref_button_sound", false);
        buttonAlertSound = prefs.getString("alert_button_sound", null);
        metaPref = prefs.getBoolean("pref_metadata", false);   // use Reverse Geocoding
        emailString = prefs.getString("email_String", "");     // for reliable query of Nominatim service
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        prefs = TourCountApplication.getPrefs();
        prefs.registerOnSharedPreferenceChangeListener(this);
        getPrefs();

        Bundle extras = getIntent().getExtras();
        if (extras != null)
        {
            latitude = extras.getDouble("Latitude");
            longitude = extras.getDouble("Longitude");
            height = extras.getDouble("Height");
            uncertainty = extras.getDouble("Uncert");
            section_id = extras.getInt("section_id");
            iid = extras.getInt("count_id");
        }

        // Set full brightness of screen
        if (brightPref)
        {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            WindowManager.LayoutParams params = getWindow().getAttributes();
            params.screenBrightness = 1.0f;
            getWindow().setAttributes(params);
        }

        // check for API-Level >= 21
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
        {
            enableProximitySensor();
        }

        // Get LocationManager instance
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        // Request list with names of all providers
        List<String> providers = locationManager.getAllProviders();
        for (String name : providers)
        {
            LocationProvider lp = locationManager.getProvider(name);
            if (MyDebug.LOG)
            {
                Log.d(TAG, lp.getName() + " --- isProviderEnabled(): " + locationManager.isProviderEnabled(name));
                Log.d(TAG, "requiresCell(): " + lp.requiresCell());
                Log.d(TAG, "requiresNetwork(): " + lp.requiresNetwork());
                Log.d(TAG, "requiresSatellite(): " + lp.requiresSatellite());
            }
        }

        // Best possible provider
        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        // criteria.setPowerRequirement(Criteria.POWER_HIGH);
        provider = locationManager.getBestProvider(criteria, true);
        if (MyDebug.LOG)
            Log.d(TAG, "Provider: " + provider);

        // Create LocationListener object
        locationListener = new LocationListener()
        {
            @Override
            public void onStatusChanged(String provider, int status, Bundle extras)
            {
                if (MyDebug.LOG)
                    Log.d(TAG, "onStatusChanged()");
            }

            @Override
            public void onProviderEnabled(String provider)
            {
                if (MyDebug.LOG)
                    Log.d(TAG, "onProviderEnabled()");
            }

            @Override
            public void onProviderDisabled(String provider)
            {
                if (MyDebug.LOG)
                    Log.d(TAG, "onProviderDisabled()");
            }

            @Override
            public void onLocationChanged(Location location)
            {
                if (MyDebug.LOG)
                    Log.d(TAG, "onLocationChanged()");
                if (location != null)
                {
                    latitude = location.getLatitude();
                    longitude = location.getLongitude();
                    height = location.getAltitude();
                    if (height != 0)
                        height = correctHeight(latitude, longitude, height);
                    uncertainty = location.getAccuracy();
                }
            }
        };

        // get location service
        try
        {
            locationManager.requestLocationUpdates(provider, 3000, 0, locationListener);
        } catch (Exception e)
        {
            // nothing
        }

        // get reverse geocoding (todo: 1st count missing geo info)
        if (metaPref && (latitude != 0 || longitude != 0))
        {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);

            runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    URL url;
                    String urlString = "https://nominatim.openstreetmap.org/reverse?email=" + emailString + "&format=xml&lat="
                        + Double.toString(latitude) + "&lon=" + Double.toString(longitude) + "&zoom=18&addressdetails=1";
                    try
                    {
                        url = new URL(urlString);
                        RetrieveAddr getXML = new RetrieveAddr(getApplicationContext());
                        getXML.execute(url);
                    } catch (IOException e)
                    {
                        // do nothing
                    }

                }
            });
        }

        // build the counting screen
        // clear any existing views
        count_area.removeAllViews();
        notes_area1.removeAllViews();
        head_area2.removeAllViews();

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
            if (MyDebug.LOG)
                Log.e(TAG, "Problem loading section: " + e.toString());
            Toast.makeText(CountingLActivity.this, getString(R.string.getHelp), Toast.LENGTH_LONG).show();
            finish();
        }

        //noinspection ConstantConditions
        getSupportActionBar().setTitle(section.name);

        switch (sortPref)
        {
        case "names_alpha":
            idArray = countDataSource.getAllIdsSrtName();
            nameArray = countDataSource.getAllStringsSrtName("name");
            codeArray = countDataSource.getAllStringsSrtName("code");
            imageArray = countDataSource.getAllImagesSrtName();
            break;
        case "codes":
            idArray = countDataSource.getAllIdsSrtCode();
            nameArray = countDataSource.getAllStringsSrtCode("name");
            codeArray = countDataSource.getAllStringsSrtCode("code");
            imageArray = countDataSource.getAllImagesSrtCode();
            break;
        default:
            idArray = countDataSource.getAllIds();
            nameArray = countDataSource.getAllStrings("name");
            codeArray = countDataSource.getAllStrings("code");
            imageArray = countDataSource.getAllImages();
            break;
        }

        countingWidgets = new ArrayList<>();
        countingWidgetsLH = new ArrayList<>();

        // display list notes
        if (section.notes != null)
        {
            if (!section.notes.isEmpty())
            {
                NotesWidget section_notes = new NotesWidget(this, null);
                section_notes.setNotes(section.notes);
                section_notes.setFont(fontPref);
                notes_area1.addView(section_notes);
            }
        }

        // 2. Head1, species selection spinner
        if (lhandPref) // if left-handed counting page
        {
            spinnerL = findViewById(R.id.countHead1SpinnerLH);
        }
        else
        {
            spinnerL = findViewById(R.id.countHead1Spinner);
        }

        CountingWidget_head1 adapterL = new CountingWidget_head1(this,
            idArray, nameArray, codeArray, imageArray);
        spinnerL.setAdapter(adapterL);
        spinnerL.setSelection(itemPosition);
        spinnerListenerL();

        if (awakePref)
        {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    // Spinner listener
    private void spinnerListenerL()
    {
        spinnerL.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
        {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long aid)
            {
                head_area2.removeAllViews();
                count_area.removeAllViews();

                String sid = ((TextView) view.findViewById(R.id.countId)).getText().toString();
                iid = Integer.parseInt(sid);
                itemPosition = position;

                count = countDataSource.getCountById(iid);
                countingScreen(count);
                //Toast.makeText(CountingActivity.this, "1. " + count.name, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent)
            {
                // stub, necessary to make Spinner work correctly when repeatedly used
            }
        });
    }

    // Show rest of widgets for counting screen
    private void countingScreen(Count count)
    {
        // 2. Head2 with species notes and edit button
        CountingWidget_head2 head2 = new CountingWidget_head2(this, null);
        head2.setCountHead2(count);
        head2.setFont(fontPref);
        head_area2.addView(head2);

        // 3. counts
        if (lhandPref) // if left-handed counting page
        {
            CountingWidgetLH widgeti = new CountingWidgetLH(this, null);
            widgeti.setCount(count);
            countingWidgetsLH.add(widgeti);
            count_area.addView(widgeti);
        }
        else
        {
            CountingWidget widgeti = new CountingWidget(this, null);
            widgeti.setCount(count);
            countingWidgets.add(widgeti);
            count_area.addView(widgeti);
        }
    }

    @Override
    protected void onPause()
    {
        super.onPause();

        // check for API-Level >= 21
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
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

        // save section id in case it is lost on pause
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt("section_id", section_id);
        editor.putInt("count_id", iid);
        editor.apply();

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

    // Correct height with geoid offset from EarthGravitationalModel
    private double correctHeight(double latitude, double longitude, double gpsHeight)
    {
        double corrHeight;
        double nnHeight;

        EarthGravitationalModel gh = new EarthGravitationalModel();
        try
        {
            gh.load(this); // load the WGS84 correction coefficient table egm180.txt
        } catch (IOException e)
        {
            return 0;
        }

        // Calculate the offset between the ellipsoid and geoid
        try
        {
            corrHeight = gh.heightOffset(latitude, longitude, gpsHeight);
        } catch (Exception e)
        {
            return 0;
        }

        nnHeight = gpsHeight + corrHeight;
        return nnHeight;
    }

    // Triggered by count up button
    // starts EditIndividualActivity
    public void countUpf1i(View view)
    {
        buttonSound();
        // iAtt used by EditindividualActivity to decide where to store bulk count value
        int iAtt = 1; // 1 f1i, 2 f2i, 3 f3i, 4 pi, 5 li, 6 ei

        int count_id = Integer.valueOf(view.getTag().toString());
        CountingWidget widget = getCountFromId(count_id);
        if (widget != null)
        {
            widget.countUpf1i();
        }

        // check for API-Level >= 21
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
        {
            disableProximitySensor(true);
        }

        // append individual with its Id, coords, date and time
        String uncert; // uncertainty about position (m)

        // if (provider.equals("gps") && latitude != 0) 
        if (latitude != 0)
        {
            uncert = String.valueOf(uncertainty);
        }
        else
        {
            uncert = "0";
        }

        String name, datestamp, timestamp;
        //noinspection ConstantConditions
        name = widget.count.name;
        datestamp = getcurDate();
        timestamp = getcurTime();

        i_Id = individualsDataSource.saveIndividual(individualsDataSource.createIndividuals
            (count_id, name, latitude, longitude, height, uncert, datestamp, timestamp));

        // get edited info for individual and start EditIndividualActivity
        Intent intent = new Intent(CountingLActivity.this, EditIndividualActivity.class);
        intent.putExtra("count_id", count_id);
        intent.putExtra("indiv_id", i_Id);
        intent.putExtra("SName", widget.count.name);
        intent.putExtra("Latitude", latitude);
        intent.putExtra("Longitude", longitude);
        intent.putExtra("Height", height);
        intent.putExtra("indivAtt", iAtt);
        startActivity(intent);
    }

    public void countUpLHf1i(View view)
    {
        buttonSound();
        int iAtt = 1;
        int count_id = Integer.valueOf(view.getTag().toString());
        CountingWidgetLH widget = getCountFromIdLH(count_id);
        if (widget != null)
        {
            widget.countUpLHf1i();
        }

        // check for API-Level >= 21
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
        {
            disableProximitySensor(true);
        }

        // append individual with its Id, coords, date and time
        String uncert; // uncertainty about position (m)

        if (latitude != 0)
        {
            uncert = String.valueOf(uncertainty);
        }
        else
        {
            uncert = "0";
        }

        String name, datestamp, timestamp;
        //noinspection ConstantConditions
        name = widget.count.name;
        datestamp = getcurDate();
        timestamp = getcurTime();

        i_Id = individualsDataSource.saveIndividual(individualsDataSource.createIndividuals
            (count_id, name, latitude, longitude, height, uncert, datestamp, timestamp));

        // get edited info for individual and start EditIndividualActivity
        Intent intent = new Intent(CountingLActivity.this, EditIndividualActivity.class);
        intent.putExtra("count_id", count_id);
        intent.putExtra("indiv_id", i_Id);
        intent.putExtra("SName", widget.count.name);
        intent.putExtra("Latitude", latitude);
        intent.putExtra("Longitude", longitude);
        intent.putExtra("Height", height);
        intent.putExtra("indivAtt", iAtt);
        startActivity(intent);
    }

    // Triggered by count down button
    // deletes last count
    public void countDownf1i(View view)
    {
        buttonSound();
        int count_id = Integer.valueOf(view.getTag().toString());
        CountingWidget widget = getCountFromId(count_id);
        //noinspection ConstantConditions
        spec_name = widget.count.name; // set spec_name for toast in deleteIndividual
        spec_count = widget.count.count_f1i;
        if (spec_count > 0)
        {
            widget.countDownf1i();
            countDataSource.saveCountf1i(count);

            i_Id = individualsDataSource.getLastIndiv(count_id, 1);
            if (i_Id == -1)
            {
                Toast.makeText(this, getString(R.string.getHelp) + spec_name, Toast.LENGTH_SHORT).show();
                return;
            }
            int icount = individualsDataSource.getIndividualCount(i_Id);
            if (i_Id > 0 && icount < 2)
            {
                deleteIndividual(i_Id);
                i_Id--;
                return;
            }
            if (i_Id > 0 && icount > 1)
            {
                int icount1 = icount - 1;
                individualsDataSource.decreaseIndividual(i_Id, icount1);
            }
        }
    }

    // Triggered by count down button from left-hand view
    // deletes last count
    public void countDownLHf1i(View view)
    {
        buttonSound();
        int count_id = Integer.valueOf(view.getTag().toString());
        CountingWidgetLH widget = getCountFromIdLH(count_id);
        //noinspection ConstantConditions
        spec_name = widget.count.name; // set spec_name for toast in deleteIndividual
        spec_count = widget.count.count_f1i;
        if (spec_count > 0)
        {
            widget.countDownLHf1i();
            countDataSource.saveCountf1i(count);

            i_Id = individualsDataSource.getLastIndiv(count_id, 1);
            if (i_Id == -1)
            {
                Toast.makeText(this, getString(R.string.getHelp) + spec_name, Toast.LENGTH_SHORT).show();
                return;
            }
            int icount = individualsDataSource.getIndividualCount(i_Id);
            if (i_Id > 0 && icount < 2)
            {
                deleteIndividual(i_Id);
                i_Id--;
                return;
            }
            if (i_Id > 0 && icount > 1)
            {
                int icount1 = icount - 1;
                individualsDataSource.decreaseIndividual(i_Id, icount1);
            }
        }
    }

    // Triggered by count up button
    // starts EditIndividualActivity
    public void countUpf2i(View view)
    {
        buttonSound();
        // iAtt used by EditindividualActivity to decide where to store bulk count value
        int iAtt = 2; // 1 f1i, 2 f2i, 3 f3i, 4 pi, 5 li, 6 ei

        int count_id = Integer.valueOf(view.getTag().toString());
        CountingWidget widget = getCountFromId(count_id);
        if (widget != null)
        {
            widget.countUpf2i();
        }

        // check for API-Level >= 21
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
        {
            disableProximitySensor(true);
        }

        // append individual with its Id, coords, date and time
        String uncert; // uncertainty about position (m)

        // if (provider.equals("gps") && latitude != 0) 
        if (latitude != 0)
        {
            uncert = String.valueOf(uncertainty);
        }
        else
        {
            uncert = "0";
        }

        String name, datestamp, timestamp;
        //noinspection ConstantConditions
        name = widget.count.name;
        datestamp = getcurDate();
        timestamp = getcurTime();

        i_Id = individualsDataSource.saveIndividual(individualsDataSource.createIndividuals
            (count_id, name, latitude, longitude, height, uncert, datestamp, timestamp));

        // get edited info for individual and start EditIndividualActivity
        Intent intent = new Intent(CountingLActivity.this, EditIndividualActivity.class);
        intent.putExtra("count_id", count_id);
        intent.putExtra("indiv_id", i_Id);
        intent.putExtra("SName", widget.count.name);
        intent.putExtra("Latitude", latitude);
        intent.putExtra("Longitude", longitude);
        intent.putExtra("Height", height);
        intent.putExtra("indivAtt", iAtt);
        startActivity(intent);
    }

    public void countUpLHf2i(View view)
    {
        buttonSound();
        int iAtt = 2;
        int count_id = Integer.valueOf(view.getTag().toString());
        CountingWidgetLH widget = getCountFromIdLH(count_id);
        if (widget != null)
        {
            widget.countUpLHf2i();
        }

        // check for API-Level >= 21
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
        {
            disableProximitySensor(true);
        }

        // append individual with its Id, coords, date and time
        String uncert; // uncertainty about position (m)

        if (latitude != 0)
        {
            uncert = String.valueOf(uncertainty);
        }
        else
        {
            uncert = "0";
        }

        String name, datestamp, timestamp;
        //noinspection ConstantConditions
        name = widget.count.name;
        datestamp = getcurDate();
        timestamp = getcurTime();

        i_Id = individualsDataSource.saveIndividual(individualsDataSource.createIndividuals
            (count_id, name, latitude, longitude, height, uncert, datestamp, timestamp));

        // get edited info for individual and start EditIndividualActivity
        Intent intent = new Intent(CountingLActivity.this, EditIndividualActivity.class);
        intent.putExtra("count_id", count_id);
        intent.putExtra("indiv_id", i_Id);
        intent.putExtra("SName", widget.count.name);
        intent.putExtra("Latitude", latitude);
        intent.putExtra("Longitude", longitude);
        intent.putExtra("Height", height);
        intent.putExtra("indivAtt", iAtt);
        startActivity(intent);
    }

    // Triggered by count down button
    // deletes last count
    public void countDownf2i(View view)
    {
        buttonSound();
        int count_id = Integer.valueOf(view.getTag().toString());
        CountingWidget widget = getCountFromId(count_id);
        //noinspection ConstantConditions
        spec_name = widget.count.name; // set spec_name for toast in deleteIndividual
        spec_count = widget.count.count_f2i;
        if (spec_count > 0)
        {
            widget.countDownf2i();
            countDataSource.saveCountf2i(count);

            i_Id = individualsDataSource.getLastIndiv(count_id, 2);
            if (i_Id == -1)
            {
                Toast.makeText(this, getString(R.string.getHelp) + spec_name, Toast.LENGTH_SHORT).show();
                return;
            }
            int icount = individualsDataSource.getIndividualCount(i_Id);
            if (i_Id > 0 && icount < 2)
            {
                deleteIndividual(i_Id);
                i_Id--;
                return;
            }
            if (i_Id > 0 && icount > 1)
            {
                int icount1 = icount - 1;
                individualsDataSource.decreaseIndividual(i_Id, icount1);
            }
        }
    }

    // Triggered by count down button from left-hand view
    // deletes last count
    public void countDownLHf2i(View view)
    {
        buttonSound();
        int count_id = Integer.valueOf(view.getTag().toString());
        CountingWidgetLH widget = getCountFromIdLH(count_id);
        //noinspection ConstantConditions
        spec_name = widget.count.name; // set spec_name for toast in deleteIndividual
        spec_count = widget.count.count_f2i;
        if (spec_count > 0)
        {
            widget.countDownLHf2i();
            countDataSource.saveCountf2i(count);

            i_Id = individualsDataSource.getLastIndiv(count_id, 2);
            if (i_Id == -1)
            {
                Toast.makeText(this, getString(R.string.getHelp) + spec_name, Toast.LENGTH_SHORT).show();
                return;
            }
            int icount = individualsDataSource.getIndividualCount(i_Id);
            if (i_Id > 0 && icount < 2)
            {
                deleteIndividual(i_Id);
                i_Id--;
                return;
            }
            if (i_Id > 0 && icount > 1)
            {
                int icount1 = icount - 1;
                individualsDataSource.decreaseIndividual(i_Id, icount1);
            }
        }
    }

    public void countUpf3i(View view)
    {
        buttonSound();
        // iAtt used by EditindividualActivity to decide where to store bulk count value
        int iAtt = 3; // 1 f1i, 2 f2i, 3 f3i, 4 pi, 5 li, 6 ei

        int count_id = Integer.valueOf(view.getTag().toString());
        CountingWidget widget = getCountFromId(count_id);
        if (widget != null)
        {
            widget.countUpf3i();
        }

        // check for API-Level >= 21
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
        {
            disableProximitySensor(true);
        }

        // append individual with its Id, coords, date and time
        String uncert; // uncertainty about position (m)

        // if (provider.equals("gps") && latitude != 0) 
        if (latitude != 0)
        {
            uncert = String.valueOf(uncertainty);
        }
        else
        {
            uncert = "0";
        }

        String name, datestamp, timestamp;
        //noinspection ConstantConditions
        name = widget.count.name;
        datestamp = getcurDate();
        timestamp = getcurTime();

        i_Id = individualsDataSource.saveIndividual(individualsDataSource.createIndividuals
            (count_id, name, latitude, longitude, height, uncert, datestamp, timestamp));

        // get edited info for individual and start EditIndividualActivity
        Intent intent = new Intent(CountingLActivity.this, EditIndividualActivity.class);
        intent.putExtra("count_id", count_id);
        intent.putExtra("indiv_id", i_Id);
        intent.putExtra("SName", widget.count.name);
        intent.putExtra("Latitude", latitude);
        intent.putExtra("Longitude", longitude);
        intent.putExtra("Height", height);
        intent.putExtra("indivAtt", iAtt);
        startActivity(intent);
    }

    public void countUpLHf3i(View view)
    {
        buttonSound();
        int iAtt = 3;
        int count_id = Integer.valueOf(view.getTag().toString());
        CountingWidgetLH widget = getCountFromIdLH(count_id);
        if (widget != null)
        {
            widget.countUpLHf3i();
        }

        // check for API-Level >= 21
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
        {
            disableProximitySensor(true);
        }

        // append individual with its Id, coords, date and time
        String uncert; // uncertainty about position (m)

        if (latitude != 0)
        {
            uncert = String.valueOf(uncertainty);
        }
        else
        {
            uncert = "0";
        }

        String name, datestamp, timestamp;
        //noinspection ConstantConditions
        name = widget.count.name;
        datestamp = getcurDate();
        timestamp = getcurTime();

        i_Id = individualsDataSource.saveIndividual(individualsDataSource.createIndividuals
            (count_id, name, latitude, longitude, height, uncert, datestamp, timestamp));

        // get edited info for individual and start EditIndividualActivity
        Intent intent = new Intent(CountingLActivity.this, EditIndividualActivity.class);
        intent.putExtra("count_id", count_id);
        intent.putExtra("indiv_id", i_Id);
        intent.putExtra("SName", widget.count.name);
        intent.putExtra("Latitude", latitude);
        intent.putExtra("Longitude", longitude);
        intent.putExtra("Height", height);
        intent.putExtra("indivAtt", iAtt);
        startActivity(intent);
    }

    // Triggered by count down button
    // deletes last count
    public void countDownf3i(View view)
    {
        buttonSound();
        int count_id = Integer.valueOf(view.getTag().toString());
        CountingWidget widget = getCountFromId(count_id);
        //noinspection ConstantConditions
        spec_name = widget.count.name; // set spec_name for toast in deleteIndividual
        spec_count = widget.count.count_f3i;
        if (spec_count > 0)
        {
            widget.countDownf3i();
            countDataSource.saveCountf3i(count);

            i_Id = individualsDataSource.getLastIndiv(count_id, 3);
            if (i_Id == -1)
            {
                Toast.makeText(this, getString(R.string.getHelp) + spec_name, Toast.LENGTH_SHORT).show();
                return;
            }
            int icount = individualsDataSource.getIndividualCount(i_Id);
            if (i_Id > 0 && icount < 2)
            {
                deleteIndividual(i_Id);
                i_Id--;
                return;
            }
            if (i_Id > 0 && icount > 1)
            {
                int icount1 = icount - 1;
                individualsDataSource.decreaseIndividual(i_Id, icount1);
            }
        }
    }

    // Triggered by count down button from left-hand view
    // deletes last count
    public void countDownLHf3i(View view)
    {
        buttonSound();
        int count_id = Integer.valueOf(view.getTag().toString());
        CountingWidgetLH widget = getCountFromIdLH(count_id);
        //noinspection ConstantConditions
        spec_name = widget.count.name; // set spec_name for toast in deleteIndividual
        spec_count = widget.count.count_f3i;
        if (spec_count > 0)
        {
            widget.countDownLHf3i();
            countDataSource.saveCountf3i(count);

            i_Id = individualsDataSource.getLastIndiv(count_id, 3);
            if (i_Id == -1)
            {
                Toast.makeText(this, getString(R.string.getHelp) + spec_name, Toast.LENGTH_SHORT).show();
                return;
            }
            int icount = individualsDataSource.getIndividualCount(i_Id);
            if (i_Id > 0 && icount < 2)
            {
                deleteIndividual(i_Id);
                i_Id--;
                return;
            }
            if (i_Id > 0 && icount > 1)
            {
                int icount1 = icount - 1;
                individualsDataSource.decreaseIndividual(i_Id, icount1);
            }
        }
    }

    public void countUppi(View view)
    {
        buttonSound();
        // iAtt used by EditindividualActivity to decide where to store bulk count value
        int iAtt = 4; // 1 f1i, 2 f2i, 3 f3i, 4 pi, 5 li, 6 ei

        int count_id = Integer.valueOf(view.getTag().toString());
        CountingWidget widget = getCountFromId(count_id);
        if (widget != null)
        {
            widget.countUppi();
        }

        // check for API-Level >= 21
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
        {
            disableProximitySensor(true);
        }

        // append individual with its Id, coords, date and time
        String uncert; // uncertainty about position (m)

        // if (provider.equals("gps") && latitude != 0) 
        if (latitude != 0)
        {
            uncert = String.valueOf(uncertainty);
        }
        else
        {
            uncert = "0";
        }

        String name, datestamp, timestamp;
        //noinspection ConstantConditions
        name = widget.count.name;
        datestamp = getcurDate();
        timestamp = getcurTime();

        i_Id = individualsDataSource.saveIndividual(individualsDataSource.createIndividuals
            (count_id, name, latitude, longitude, height, uncert, datestamp, timestamp));

        // get edited info for individual and start EditIndividualActivity
        Intent intent = new Intent(CountingLActivity.this, EditIndividualActivity.class);
        intent.putExtra("count_id", count_id);
        intent.putExtra("indiv_id", i_Id);
        intent.putExtra("SName", widget.count.name);
        intent.putExtra("Latitude", latitude);
        intent.putExtra("Longitude", longitude);
        intent.putExtra("Height", height);
        intent.putExtra("indivAtt", iAtt);
        startActivity(intent);
    }

    public void countUpLHpi(View view)
    {
        buttonSound();
        int iAtt = 4;
        int count_id = Integer.valueOf(view.getTag().toString());
        CountingWidgetLH widget = getCountFromIdLH(count_id);
        if (widget != null)
        {
            widget.countUpLHpi();
        }

        // check for API-Level >= 21
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
        {
            disableProximitySensor(true);
        }

        // append individual with its Id, coords, date and time
        String uncert; // uncertainty about position (m)

        if (latitude != 0)
        {
            uncert = String.valueOf(uncertainty);
        }
        else
        {
            uncert = "0";
        }

        String name, datestamp, timestamp;
        //noinspection ConstantConditions
        name = widget.count.name;
        datestamp = getcurDate();
        timestamp = getcurTime();

        i_Id = individualsDataSource.saveIndividual(individualsDataSource.createIndividuals
            (count_id, name, latitude, longitude, height, uncert, datestamp, timestamp));

        // get edited info for individual and start EditIndividualActivity
        Intent intent = new Intent(CountingLActivity.this, EditIndividualActivity.class);
        intent.putExtra("count_id", count_id);
        intent.putExtra("indiv_id", i_Id);
        intent.putExtra("SName", widget.count.name);
        intent.putExtra("Latitude", latitude);
        intent.putExtra("Longitude", longitude);
        intent.putExtra("Height", height);
        intent.putExtra("indivAtt", iAtt);
        startActivity(intent);
    }

    // Triggered by count down button
    // deletes last count
    public void countDownpi(View view)
    {
        buttonSound();
        int count_id = Integer.valueOf(view.getTag().toString());
        CountingWidget widget = getCountFromId(count_id);
        //noinspection ConstantConditions
        spec_name = widget.count.name; // set spec_name for toast in deleteIndividual
        spec_count = widget.count.count_pi;
        if (spec_count > 0)
        {
            widget.countDownpi();
            countDataSource.saveCountpi(count);

            i_Id = individualsDataSource.getLastIndiv(count_id, 4);
            if (i_Id == -1)
            {
                Toast.makeText(this, getString(R.string.getHelp) + spec_name, Toast.LENGTH_SHORT).show();
                return;
            }
            int icount = individualsDataSource.getIndividualCount(i_Id);
            if (i_Id > 0 && icount < 2)
            {
                deleteIndividual(i_Id);
                i_Id--;
                return;
            }
            if (i_Id > 0 && icount > 1)
            {
                int icount1 = icount - 1;
                individualsDataSource.decreaseIndividual(i_Id, icount1);
            }
        }
    }

    // Triggered by count down button from left-hand view
    // deletes last count
    public void countDownLHpi(View view)
    {
        buttonSound();
        int count_id = Integer.valueOf(view.getTag().toString());
        CountingWidgetLH widget = getCountFromIdLH(count_id);
        //noinspection ConstantConditions
        spec_name = widget.count.name; // set spec_name for toast in deleteIndividual
        spec_count = widget.count.count_pi;
        if (spec_count > 0)
        {
            widget.countDownLHpi();
            countDataSource.saveCountpi(count);

            i_Id = individualsDataSource.getLastIndiv(count_id, 4);
            if (i_Id == -1)
            {
                Toast.makeText(this, getString(R.string.getHelp) + spec_name, Toast.LENGTH_SHORT).show();
                return;
            }
            int icount = individualsDataSource.getIndividualCount(i_Id);
            if (i_Id > 0 && icount < 2)
            {
                deleteIndividual(i_Id);
                i_Id--;
                return;
            }
            if (i_Id > 0 && icount > 1)
            {
                int icount1 = icount - 1;
                individualsDataSource.decreaseIndividual(i_Id, icount1);
            }
        }
    }

    public void countUpli(View view)
    {
        buttonSound();
        // iAtt used by EditindividualActivity to decide where to store bulk count value
        int iAtt = 5; // 1 f1i, 2 f2i, 3 f3i, 4 pi, 5 li, 6 ei

        int count_id = Integer.valueOf(view.getTag().toString());
        CountingWidget widget = getCountFromId(count_id);
        if (widget != null)
        {
            widget.countUpli();
        }

        // check for API-Level >= 21
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
        {
            disableProximitySensor(true);
        }

        // append individual with its Id, coords, date and time
        String uncert; // uncertainty about position (m)

        // if (provider.equals("gps") && latitude != 0) 
        if (latitude != 0)
        {
            uncert = String.valueOf(uncertainty);
        }
        else
        {
            uncert = "0";
        }

        String name, datestamp, timestamp;
        //noinspection ConstantConditions
        name = widget.count.name;
        datestamp = getcurDate();
        timestamp = getcurTime();

        i_Id = individualsDataSource.saveIndividual(individualsDataSource.createIndividuals
            (count_id, name, latitude, longitude, height, uncert, datestamp, timestamp));

        // get edited info for individual and start EditIndividualActivity
        Intent intent = new Intent(CountingLActivity.this, EditIndividualActivity.class);
        intent.putExtra("count_id", count_id);
        intent.putExtra("indiv_id", i_Id);
        intent.putExtra("SName", widget.count.name);
        intent.putExtra("Latitude", latitude);
        intent.putExtra("Longitude", longitude);
        intent.putExtra("Height", height);
        intent.putExtra("indivAtt", iAtt);
        startActivity(intent);
    }

    public void countUpLHli(View view)
    {
        buttonSound();
        int iAtt = 5;
        int count_id = Integer.valueOf(view.getTag().toString());
        CountingWidgetLH widget = getCountFromIdLH(count_id);
        if (widget != null)
        {
            widget.countUpLHli();
        }

        // check for API-Level >= 21
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
        {
            disableProximitySensor(true);
        }

        // append individual with its Id, coords, date and time
        String uncert; // uncertainty about position (m)

        if (latitude != 0)
        {
            uncert = String.valueOf(uncertainty);
        }
        else
        {
            uncert = "0";
        }

        String name, datestamp, timestamp;
        //noinspection ConstantConditions
        name = widget.count.name;
        datestamp = getcurDate();
        timestamp = getcurTime();

        i_Id = individualsDataSource.saveIndividual(individualsDataSource.createIndividuals
            (count_id, name, latitude, longitude, height, uncert, datestamp, timestamp));

        // get edited info for individual and start EditIndividualActivity
        Intent intent = new Intent(CountingLActivity.this, EditIndividualActivity.class);
        intent.putExtra("count_id", count_id);
        intent.putExtra("indiv_id", i_Id);
        intent.putExtra("SName", widget.count.name);
        intent.putExtra("Latitude", latitude);
        intent.putExtra("Longitude", longitude);
        intent.putExtra("Height", height);
        intent.putExtra("indivAtt", iAtt);
        startActivity(intent);
    }

    // Triggered by count down button
    // deletes last count
    public void countDownli(View view)
    {
        buttonSound();
        int count_id = Integer.valueOf(view.getTag().toString());
        CountingWidget widget = getCountFromId(count_id);
        //noinspection ConstantConditions
        spec_name = widget.count.name; // set spec_name for toast in deleteIndividual
        spec_count = widget.count.count_li;
        if (spec_count > 0)
        {
            widget.countDownli();
            countDataSource.saveCountli(count);

            i_Id = individualsDataSource.getLastIndiv(count_id, 5);
            if (i_Id == -1)
            {
                Toast.makeText(this, getString(R.string.getHelp) + spec_name, Toast.LENGTH_SHORT).show();
                return;
            }
            int icount = individualsDataSource.getIndividualCount(i_Id);
            if (i_Id > 0 && icount < 2)
            {
                deleteIndividual(i_Id);
                i_Id--;
                return;
            }
            if (i_Id > 0 && icount > 1)
            {
                int icount1 = icount - 1;
                individualsDataSource.decreaseIndividual(i_Id, icount1);
            }
        }
    }

    // Triggered by count down button from left-hand view
    // deletes last count
    public void countDownLHli(View view)
    {
        buttonSound();
        int count_id = Integer.valueOf(view.getTag().toString());
        CountingWidgetLH widget = getCountFromIdLH(count_id);
        //noinspection ConstantConditions
        spec_name = widget.count.name; // set spec_name for toast in deleteIndividual
        spec_count = widget.count.count_li;
        if (spec_count > 0)
        {
            widget.countDownLHli();
            countDataSource.saveCountli(count);

            i_Id = individualsDataSource.getLastIndiv(count_id, 5);
            if (i_Id == -1)
            {
                Toast.makeText(this, getString(R.string.getHelp) + spec_name, Toast.LENGTH_SHORT).show();
                return;
            }
            int icount = individualsDataSource.getIndividualCount(i_Id);
            if (i_Id > 0 && icount < 2)
            {
                deleteIndividual(i_Id);
                i_Id--;
                return;
            }
            if (i_Id > 0 && icount > 1)
            {
                int icount1 = icount - 1;
                individualsDataSource.decreaseIndividual(i_Id, icount1);
            }
        }
    }

    public void countUpei(View view)
    {
        buttonSound();
        // iAtt used by EditindividualActivity to decide where to store bulk count value
        int iAtt = 6; // 1 f1i, 2 f2i, 3 f3i, 4 pi, 5 li, 6 ei

        int count_id = Integer.valueOf(view.getTag().toString());
        CountingWidget widget = getCountFromId(count_id);
        if (widget != null)
        {
            widget.countUpei();
        }

        // check for API-Level >= 21
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
        {
            disableProximitySensor(true);
        }

        // append individual with its Id, coords, date and time
        String uncert; // uncertainty about position (m)

        // if (provider.equals("gps") && latitude != 0) 
        if (latitude != 0)
        {
            uncert = String.valueOf(uncertainty);
        }
        else
        {
            uncert = "0";
        }

        String name, datestamp, timestamp;
        //noinspection ConstantConditions
        name = widget.count.name;
        datestamp = getcurDate();
        timestamp = getcurTime();

        i_Id = individualsDataSource.saveIndividual(individualsDataSource.createIndividuals
            (count_id, name, latitude, longitude, height, uncert, datestamp, timestamp));

        // get edited info for individual and start EditIndividualActivity
        Intent intent = new Intent(CountingLActivity.this, EditIndividualActivity.class);
        intent.putExtra("count_id", count_id);
        intent.putExtra("indiv_id", i_Id);
        intent.putExtra("SName", widget.count.name);
        intent.putExtra("Latitude", latitude);
        intent.putExtra("Longitude", longitude);
        intent.putExtra("Height", height);
        intent.putExtra("indivAtt", iAtt);
        startActivity(intent);
    }

    public void countUpLHei(View view)
    {
        buttonSound();
        int iAtt = 6;
        int count_id = Integer.valueOf(view.getTag().toString());
        CountingWidgetLH widget = getCountFromIdLH(count_id);
        if (widget != null)
        {
            widget.countUpLHei();
        }

        // check for API-Level >= 21
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
        {
            disableProximitySensor(true);
        }

        // append individual with its Id, coords, date and time
        String uncert; // uncertainty about position (m)

        if (latitude != 0)
        {
            uncert = String.valueOf(uncertainty);
        }
        else
        {
            uncert = "0";
        }

        String name, datestamp, timestamp;
        //noinspection ConstantConditions
        name = widget.count.name;
        datestamp = getcurDate();
        timestamp = getcurTime();

        i_Id = individualsDataSource.saveIndividual(individualsDataSource.createIndividuals
            (count_id, name, latitude, longitude, height, uncert, datestamp, timestamp));

        // get edited info for individual and start EditIndividualActivity
        Intent intent = new Intent(CountingLActivity.this, EditIndividualActivity.class);
        intent.putExtra("count_id", count_id);
        intent.putExtra("indiv_id", i_Id);
        intent.putExtra("SName", widget.count.name);
        intent.putExtra("Latitude", latitude);
        intent.putExtra("Longitude", longitude);
        intent.putExtra("Height", height);
        intent.putExtra("indivAtt", iAtt);
        startActivity(intent);
    }

    // Triggered by count down button
    // deletes last count
    public void countDownei(View view)
    {
        buttonSound();
        int count_id = Integer.valueOf(view.getTag().toString());
        CountingWidget widget = getCountFromId(count_id);
        //noinspection ConstantConditions
        spec_name = widget.count.name; // set spec_name for toast in deleteIndividual
        spec_count = widget.count.count_ei;
        if (spec_count > 0)
        {
            widget.countDownei();
            countDataSource.saveCountei(count);

            i_Id = individualsDataSource.getLastIndiv(count_id, 6);
            if (i_Id == -1)
            {
                Toast.makeText(this, getString(R.string.getHelp) + spec_name, Toast.LENGTH_SHORT).show();
                return;
            }
            int icount = individualsDataSource.getIndividualCount(i_Id);
            if (i_Id > 0 && icount < 2)
            {
                deleteIndividual(i_Id);
                i_Id--;
                return;
            }
            if (i_Id > 0 && icount > 1)
            {
                int icount1 = icount - 1;
                individualsDataSource.decreaseIndividual(i_Id, icount1);
            }
        }
    }

    // Triggered by count down button from left-hand view
    // deletes last count
    public void countDownLHei(View view)
    {
        buttonSound();
        int count_id = Integer.valueOf(view.getTag().toString());
        CountingWidgetLH widget = getCountFromIdLH(count_id);
        //noinspection ConstantConditions
        spec_name = widget.count.name; // set spec_name for toast in deleteIndividual
        spec_count = widget.count.count_ei;
        if (spec_count > 0)
        {
            widget.countDownLHei();
            countDataSource.saveCountei(count);

            i_Id = individualsDataSource.getLastIndiv(count_id, 6);
            if (i_Id == -1)
            {
                Toast.makeText(this, getString(R.string.getHelp) + spec_name, Toast.LENGTH_SHORT).show();
                return;
            }
            int icount = individualsDataSource.getIndividualCount(i_Id);
            if (i_Id > 0 && icount < 2)
            {
                deleteIndividual(i_Id);
                i_Id--;
                return;
            }
            if (i_Id > 0 && icount > 1)
            {
                int icount1 = icount - 1;
                individualsDataSource.decreaseIndividual(i_Id, icount1);
            }
        }
    }

    /*
     * Get a counting widget (with reference to the associated count) from the list of widgets.
     */
    private CountingWidget getCountFromId(int id)
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
    private CountingWidgetLH getCountFromIdLH(int id)
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
    private void deleteIndividual(int id)
    {
        // Toast.makeText(CountingActivity.this, getString(R.string.indivdel1) + spec_name, Toast.LENGTH_SHORT).show();
        System.out.println(getString(R.string.indivdel) + " " + id);
        individualsDataSource.deleteIndividualById(id);
    }

    // Date for date_stamp
    @SuppressLint("SimpleDateFormat")
    private String getcurDate()
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
    private String getcurTime()
    {
        Date date = new Date();
        @SuppressLint("SimpleDateFormat")
        DateFormat dform = new SimpleDateFormat("HH:mm");
        return dform.format(date);
    }

    // Edit count options
    public void edit(View view)
    {
        // check for API-Level >= 21
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
        {
            disableProximitySensor(true);
        }

        Intent intent = new Intent(CountingLActivity.this, CountOptionsActivity.class);
        intent.putExtra("count_id", iid);
        intent.putExtra("section_id", section_id);
        intent.putExtra("itemposition", spinnerL.getSelectedItemPosition());
        startActivity(intent);
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
                    Log.e(TAG, "could not play botton sound.", e);
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

    // Handle menu selections
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            {
                disableProximitySensor(true);
            }

            Intent intent = new Intent(CountingLActivity.this, EditSectionActivity.class);
            startActivity(intent);
            return true;
        }
        else if (id == R.id.menuTakePhoto)
        {
            Intent camIntent = new Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA);

            PackageManager packageManager = getPackageManager();
            List<ResolveInfo> activities = packageManager.queryIntentActivities(camIntent,
                PackageManager.MATCH_DEFAULT_ONLY);
            boolean isIntentSafe = activities.size() > 0;

            if (isIntentSafe)
            {
                String title = getResources().getString(R.string.chooserTitle);
                Intent chooser = Intent.createChooser(camIntent, title);
                if (camIntent.resolveActivity(getPackageManager()) != null)
                {
                    try
                    {
                        startActivity(chooser);
                    } catch (Exception e)
                    {
                        Toast.makeText(CountingLActivity.this, getString(R.string.noPhotoPermit), Toast.LENGTH_SHORT).show();
                    }
                }
            }
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

    @SuppressLint("NewApi")
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

    // Save activity state for getting back to CountingActivity
    @Override
    public void onSaveInstanceState(Bundle savedInstanceState)
    {
        super.onSaveInstanceState(savedInstanceState);

        savedInstanceState.putInt("count_id", iid);
        savedInstanceState.putInt("itemPosition", spinnerL.getSelectedItemPosition());
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
