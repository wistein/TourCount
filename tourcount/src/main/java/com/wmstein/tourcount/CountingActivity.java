package com.wmstein.tourcount;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.CursorIndexOutOfBoundsException;
import android.graphics.Color;
import android.graphics.Typeface;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.provider.MediaStore;
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

import com.google.android.material.snackbar.Snackbar;
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
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NavUtils;
import androidx.core.content.ContextCompat;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;

/****************************************************************************************
 * CountingActivity is the central activity of TourCount in portrait mode. 
 * It provides the counters, starts GPS-location polling, starts EditIndividualActivity,
 * starts EditSpecListActivity, switches screen off when pocketed 
 * and allows taking pictures and sending notes.
 <p>
 * CountingActivity uses CountingWidget.java, CountingWidgetLH.java, NotesWidget.java, 
 * activity_counting.xml and activity_counting_lh.xml
 <p>
 * Basic counting functions created by milo for BeeCount on 05/05/2014.
 * Adopted, modified and enhanced for TourCount by wmstein since 2016-04-18,
 * last modification in Java on 2023-07-11
 */
public class CountingActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener, PermissionsDialogFragment.PermissionsGrantedCallback
{
    private static final String TAG = "TourCountCountAct";

    private int iid = 1;
    private LinearLayout count_area;

    private LinearLayout head_area2;
    private LinearLayout notes_area1;
    private final Handler mHandler = new Handler();

    // the actual data
    private Count count;
    private Section section;
    private List<CountingWidget> countingWidgets;
    private List<CountingWidgetLH> countingWidgetsLH;
    private Spinner spinner;
    private int itemPosition = 0;

    // Location info handling
    private double latitude, longitude, height, uncertainty;
    LocationService locationService;

    // Permission dispatcher mode modePerm: 
    //  1 = use location service
    //  2 = end location service
    int modePerm;

    private PowerManager.WakeLock mProximityWakeLock;

    // preferences
    private SharedPreferences prefs;
    private boolean awakePref;
    private boolean brightPref;
    private String sortPref;
    private boolean fontPref;
    private boolean lhandPref; // true for lefthand mode of counting screen
    private boolean buttonSoundPref;
    private boolean buttonVibPref;
    private String buttonSound;
    private String buttonSoundMinus;
    private boolean metaPref;      // option for reverse geocoding
    private String emailString = ""; // mail address for OSM query

    // data sources
    private SectionDataSource sectionDataSource;
    private CountDataSource countDataSource;
    private IndividualsDataSource individualsDataSource;

    private int i_Id = 0; // Individuals id
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

        TourCountApplication tourCount = (TourCountApplication) getApplication();
        prefs = TourCountApplication.getPrefs();
        prefs.registerOnSharedPreferenceChangeListener(this);
        setPrefs();

        // if left-handed counting page
        if (lhandPref)
        {
            setContentView(R.layout.activity_counting_lh);
            LinearLayout counting_screen = findViewById(R.id.countingScreenLH);
            if (counting_screen != null)
            {
                counting_screen.setBackground(tourCount.getBackground());
            }
            count_area = findViewById(R.id.countCountiLayoutLH);
            notes_area1 = findViewById(R.id.sectionNotesLayoutLH);
            head_area2 = findViewById(R.id.countHead2LayoutLH);
        }
        else
        {
            setContentView(R.layout.activity_counting);
            LinearLayout counting_screen = findViewById(R.id.countingScreen);
            if (counting_screen != null)
            {
                counting_screen.setBackground(tourCount.getBackground());
            }
            count_area = findViewById(R.id.countCountiLayout);
            notes_area1 = findViewById(R.id.sectionNotesLayout);
            head_area2 = findViewById(R.id.countHead2Layout);
        }

        // Set full brightness of screen
        if (brightPref)
        {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            WindowManager.LayoutParams params = getWindow().getAttributes();
            params.screenBrightness = 1.0f;
            getWindow().setAttributes(params);
        }

        if (awakePref)
        {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

        PowerManager mPowerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        try
        {
            assert mPowerManager != null;
            if (mPowerManager.isWakeLockLevelSupported(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK))
            {
                mProximityWakeLock = mPowerManager.newWakeLock(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK, "TourCount:WAKELOCK");
            }
            enableProximitySensor();
        } catch (NullPointerException e)
        {
            // do nothing
        }

    } // End of onCreate

    // Load preferences
    private void setPrefs()
    {
        awakePref = prefs.getBoolean("pref_awake", true);      // stay awake while counting
        brightPref = prefs.getBoolean("pref_bright", true);    // bright counting page
        sortPref = prefs.getString("pref_sort_sp", "none");    // sorted species list on counting page
        fontPref = prefs.getBoolean("pref_note_font", false);  // larger font for remarks
        lhandPref = prefs.getBoolean("pref_left_hand", false); // left-handed counting page
        buttonSoundPref = prefs.getBoolean("pref_button_sound", false); // make button sound
        buttonVibPref = prefs.getBoolean("pref_button_vib", false); // make vibration
        buttonSound = prefs.getString("button_sound", null); // use standard button sound
        buttonSoundMinus = prefs.getString("button_sound_minus", null); //use deeper button sound
        metaPref = prefs.getBoolean("pref_metadata", false);   // use Reverse Geocoding
        emailString = prefs.getString("email_String", "");     // for reliable query of Nominatim service
        itemPosition = prefs.getInt("item_Position", 0);       // spinner pos.
        iid = prefs.getInt("count_id", 1);                     // species id
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        prefs = TourCountApplication.getPrefs();
        prefs.registerOnSharedPreferenceChangeListener(this);
        setPrefs();

        // get parameters from WelcomeActivity
        Bundle extras = getIntent().getExtras();
        if (extras != null)
        {
            latitude = extras.getDouble("Latitude");
            longitude = extras.getDouble("Longitude");
            height = extras.getDouble("Height");
            uncertainty = extras.getDouble("Uncertain");
        }

        // Set full brightness of screen
        if (brightPref)
        {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            WindowManager.LayoutParams params = getWindow().getAttributes();
            params.screenBrightness = 1.0f;
            getWindow().setAttributes(params);
        }

        enableProximitySensor();

        // Get location with permissions check
        modePerm = 1;
        permissionCaptureFragment();

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
                Log.e(TAG, "Problem loading section: " + e);
            showSnackbarRed(getString(R.string.getHelp));
            finish();
        }

        Objects.requireNonNull(getSupportActionBar()).setTitle(section.name);

        String[] idArray;
        String[] nameArray;
        String[] nameArrayG;
        String[] codeArray;
        Integer[] imageArray;

        switch (sortPref)
        {
        case "names_alpha" ->
        {
            idArray = countDataSource.getAllIdsSrtName();
            nameArray = countDataSource.getAllStringsSrtName("name");
            codeArray = countDataSource.getAllStringsSrtName("code");
            nameArrayG = countDataSource.getAllStringsSrtName("name_g");
            imageArray = countDataSource.getAllImagesSrtName();
        }
        case "codes" ->
        {
            idArray = countDataSource.getAllIdsSrtCode();
            nameArray = countDataSource.getAllStringsSrtCode("name");
            codeArray = countDataSource.getAllStringsSrtCode("code");
            nameArrayG = countDataSource.getAllStringsSrtCode("name_g");
            imageArray = countDataSource.getAllImagesSrtCode();
        }
        default ->
        {
            idArray = countDataSource.getAllIds();
            nameArray = countDataSource.getAllStrings("name");
            codeArray = countDataSource.getAllStrings("code");
            nameArrayG = countDataSource.getAllStrings("name_g");
            imageArray = countDataSource.getAllImages();
        }
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
            spinner = findViewById(R.id.countHead1SpinnerLH);
        }
        else
        {
            spinner = findViewById(R.id.countHead1Spinner);
        }

        CountingWidget_head1 adapter = new CountingWidget_head1(this,
            idArray, nameArray, codeArray, imageArray, nameArrayG);
        spinner.setAdapter(adapter);
        spinner.setSelection(itemPosition);
        spinnerListener();

        if (awakePref)
        {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    } // end of onResume

    // Part of permission handling
    @Override
    public void permissionCaptureFragment()
    {
        if (isPermissionGranted())
        {
            switch (modePerm)
            {
            case 1 -> // get location
                getLoc();
            case 2 -> // stop location service
                locationService.stopListener();
            }
        }
        else
        {
            if (modePerm == 1)
                PermissionsDialogFragment.newInstance().show(getSupportFragmentManager(), PermissionsDialogFragment.class.getName());
        }
    }

    // if API level > 23 test for permissions granted
    private boolean isPermissionGranted()
    {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
            && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    // get the location data
    public void getLoc()
    {
        locationService = new LocationService(this);

        if (locationService.canGetLocation())
        {
            longitude = locationService.getLongitude();
            latitude = locationService.getLatitude();
            height = locationService.getAltitude();
            if (height != 0)
                height = correctHeight(latitude, longitude, height);
            uncertainty = locationService.getAccuracy();
        }

        // get reverse geocoding
        if (locationService.canGetLocation() && metaPref && (latitude != 0 || longitude != 0))
        {
            String urlString = "https://nominatim.openstreetmap.org/reverse?email=" + emailString
                + "&format=xml&lat=" + latitude + "&lon=" + longitude + "&zoom=18&addressdetails=1";

            // Trial with WorkManager
            WorkRequest retrieveAddrWorkRequest =
                new OneTimeWorkRequest.Builder(RetrieveAddrWorker.class)
                    .setInputData(new Data.Builder()
                            .putString("URL_STRING", urlString)
                            .build()
                                 )
                    .build();

            WorkManager
                .getInstance(this)
                .enqueue(retrieveAddrWorkRequest);
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

    // Spinner listener
    private void spinnerListener()
    {
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
        {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long aid)
            {
                try
                {
                    head_area2.removeAllViews();
                    count_area.removeAllViews();

                    String sid = ((TextView) view.findViewById(R.id.countId)).getText().toString();
                    iid = Integer.parseInt(sid); // get species id
                    itemPosition = position;

                    count = countDataSource.getCountById(iid);
                    countingScreen(count);
                    //Toast.makeText(CountingActivity.this, "1. " + count.name, Toast.LENGTH_SHORT).show();
                } catch (Exception e)
                {
                    // Exception may occur when permissions are changed while activity is paused
                    //  or when spinner is rapidly repeatedly pressed
                    if (MyDebug.LOG)
                        Log.e(TAG, "SpinnerListener: " + e);
                }
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

        disableProximitySensor();

        // save current count id in case it is lost on pause
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt("count_id", iid);
        editor.putInt("item_Position", itemPosition);
        editor.apply();

        // close the data sources
        sectionDataSource.close();
        countDataSource.close();
        individualsDataSource.close();

        // Stop location service with permissions check
        modePerm = 2;
        permissionCaptureFragment();

        // N.B. a wakelock might not be held, e.g. if someone is using Cyanogenmod and
        // has denied wakelock permission to TourCount
        if (awakePref)
        {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    // Triggered by count-up-button
    // starts EditIndividualActivity
    public void countUpf1i(View view)
    {
        buttonSound();
        buttonVib();

        // iAtt used by EditIndividualActivity to decide where to store bulk count value
        int iAtt = 1; // 1 f1i, 2 f2i, 3 f3i, 4 pi, 5 li, 6 ei

        int count_id = Integer.parseInt(view.getTag().toString());
        CountingWidget widget = getCountFromId(count_id);
        if (widget != null)
        {
            widget.countUpf1i();
        }

        disableProximitySensor();

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
        assert Objects.requireNonNull(widget).count != null;
        assert widget.count != null;
        name = widget.count.name;
        datestamp = getcurDate();
        timestamp = getcurTime();

        i_Id = individualsDataSource.saveIndividual(individualsDataSource.createIndividuals
            (count_id, name, latitude, longitude, height, uncert, datestamp, timestamp));

        if (i_Id != 0) // i_Id is set to 0 in case of doubleclick on count button
        {
            // get edited info for individual and start EditIndividualActivity
            Intent intent = new Intent(CountingActivity.this, EditIndividualActivity.class);
            intent.putExtra("count_id", count_id);
            intent.putExtra("indiv_id", i_Id);
            intent.putExtra("SName", widget.count.name);
            intent.putExtra("Latitude", latitude);
            intent.putExtra("Longitude", longitude);
            intent.putExtra("Height", height);
            intent.putExtra("Uncert", uncert);
            intent.putExtra("indivAtt", iAtt);
            startActivity(intent);
        }
    }

    public void countUpLHf1i(View view)
    {
        buttonSound();
        buttonVib();

        int iAtt = 1;
        int count_id = Integer.parseInt(view.getTag().toString());
        CountingWidgetLH widget = getCountFromIdLH(count_id);
        if (widget != null)
        {
            widget.countUpLHf1i();
        }

        disableProximitySensor();

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
        assert Objects.requireNonNull(widget).count != null;
        assert widget.count != null;
        name = widget.count.name;
        datestamp = getcurDate();
        timestamp = getcurTime();

        i_Id = individualsDataSource.saveIndividual(individualsDataSource.createIndividuals
            (count_id, name, latitude, longitude, height, uncert, datestamp, timestamp));

        if (i_Id != 0)
        {
            // get edited info for individual and start EditIndividualActivity
            Intent intent = new Intent(CountingActivity.this, EditIndividualActivity.class);
            intent.putExtra("count_id", count_id);
            intent.putExtra("indiv_id", i_Id);
            intent.putExtra("SName", widget.count.name);
            intent.putExtra("Latitude", latitude);
            intent.putExtra("Longitude", longitude);
            intent.putExtra("Height", height);
            intent.putExtra("Uncert", uncert);
            intent.putExtra("indivAtt", iAtt);
            startActivity(intent);
        }
    }

    // Triggered by count down button
    // deletes last count
    public void countDownf1i(View view)
    {
        buttonSoundMinus();
        buttonVibLong();

        int count_id = Integer.parseInt(view.getTag().toString());
        CountingWidget widget = getCountFromId(count_id);
        assert Objects.requireNonNull(widget).count != null;
        assert widget.count != null;
        spec_name = widget.count.name; // set spec_name for toast in deleteIndividual
        spec_count = widget.count.count_f1i;
        if (spec_count > 0)
        {
            widget.countDownf1i(); // decrease species counter
            countDataSource.saveCountf1i(count);

            // get last individual of category 1 (♂♀)
            i_Id = individualsDataSource.getLastIndiv(count_id, 1);
            if (i_Id == -1)
            {
//                Toast.makeText(this, getString(R.string.getHelp) + spec_name, Toast.LENGTH_SHORT).show();
                showSnackbarRed(getString(R.string.getHelp) + spec_name);
                return;
            }
            int icount = individualsDataSource.getIndividualCount(i_Id);
            if (i_Id > 0 && icount < 2)
            {
                deleteIndividual(i_Id);
                i_Id--;
                return;
            }
            if (i_Id > 0)
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
        buttonSoundMinus();
        buttonVibLong();

        int count_id = Integer.parseInt(view.getTag().toString());
        CountingWidgetLH widget = getCountFromIdLH(count_id);
        assert Objects.requireNonNull(widget).count != null;
        assert widget.count != null;
        spec_name = widget.count.name; // set spec_name for toast in deleteIndividual
        spec_count = widget.count.count_f1i;
        if (spec_count > 0)
        {
            widget.countDownLHf1i();
            countDataSource.saveCountf1i(count);

            i_Id = individualsDataSource.getLastIndiv(count_id, 1);
            if (i_Id == -1)
            {
//                Toast.makeText(this, getString(R.string.getHelp) + spec_name, Toast.LENGTH_SHORT).show();
                showSnackbarRed(getString(R.string.getHelp) + spec_name);
                return;
            }
            int icount = individualsDataSource.getIndividualCount(i_Id);
            if (i_Id > 0 && icount < 2)
            {
                deleteIndividual(i_Id);
                i_Id--;
                return;
            }
            if (i_Id > 0)
            {
                int icount1 = icount - 1;
                individualsDataSource.decreaseIndividual(i_Id, icount1);
            }
        }
    }

    // Triggered by count up button for ♂
    // starts EditIndividualActivity
    public void countUpf2i(View view)
    {
        buttonSound();
        buttonVib();

        // iAtt used by EditIndividualActivity to decide where to store bulk count value
        int iAtt = 2; // 1 f1i, 2 f2i, 3 f3i, 4 pi, 5 li, 6 ei

        int count_id = Integer.parseInt(view.getTag().toString());
        CountingWidget widget = getCountFromId(count_id);
        if (widget != null)
        {
            widget.countUpf2i();
        }

        disableProximitySensor();

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
        assert Objects.requireNonNull(widget).count != null;
        assert widget.count != null;
        name = widget.count.name;
        datestamp = getcurDate();
        timestamp = getcurTime();

        i_Id = individualsDataSource.saveIndividual(individualsDataSource.createIndividuals
            (count_id, name, latitude, longitude, height, uncert, datestamp, timestamp));

        if (i_Id != 0)
        {
            // get edited info for individual and start EditIndividualActivity
            Intent intent = new Intent(CountingActivity.this, EditIndividualActivity.class);
            intent.putExtra("count_id", count_id);
            intent.putExtra("indiv_id", i_Id);
            intent.putExtra("SName", widget.count.name);
            intent.putExtra("Latitude", latitude);
            intent.putExtra("Longitude", longitude);
            intent.putExtra("Height", height);
            intent.putExtra("Uncert", uncert);
            intent.putExtra("indivAtt", iAtt);
            startActivity(intent);
        }
    }

    public void countUpLHf2i(View view)
    {
        buttonSound();
        buttonVib();

        int iAtt = 2;
        int count_id = Integer.parseInt(view.getTag().toString());
        CountingWidgetLH widget = getCountFromIdLH(count_id);
        if (widget != null)
        {
            widget.countUpLHf2i();
        }

        disableProximitySensor();

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
        assert Objects.requireNonNull(widget).count != null;
        assert widget.count != null;
        name = widget.count.name;
        datestamp = getcurDate();
        timestamp = getcurTime();

        i_Id = individualsDataSource.saveIndividual(individualsDataSource.createIndividuals
            (count_id, name, latitude, longitude, height, uncert, datestamp, timestamp));

        if (i_Id != 0)
        {
            // get edited info for individual and start EditIndividualActivity
            Intent intent = new Intent(CountingActivity.this, EditIndividualActivity.class);
            intent.putExtra("count_id", count_id);
            intent.putExtra("indiv_id", i_Id);
            intent.putExtra("SName", widget.count.name);
            intent.putExtra("Latitude", latitude);
            intent.putExtra("Longitude", longitude);
            intent.putExtra("Height", height);
            intent.putExtra("Uncert", uncert);
            intent.putExtra("indivAtt", iAtt);
            startActivity(intent);
        }
    }

    // Triggered by count down button
    // deletes last count
    public void countDownf2i(View view)
    {
        buttonSoundMinus();
        buttonVibLong();

        int count_id = Integer.parseInt(view.getTag().toString());
        CountingWidget widget = getCountFromId(count_id);
        assert Objects.requireNonNull(widget).count != null;
        assert widget.count != null;
        spec_name = widget.count.name; // set spec_name for toast in deleteIndividual
        spec_count = widget.count.count_f2i;
        if (spec_count > 0)
        {
            widget.countDownf2i();
            countDataSource.saveCountf2i(count);

            i_Id = individualsDataSource.getLastIndiv(count_id, 2);
            if (i_Id == -1)
            {
//                Toast.makeText(this, getString(R.string.getHelp) + spec_name, Toast.LENGTH_SHORT).show();
                showSnackbarRed(getString(R.string.getHelp) + spec_name);
                return;
            }
            int icount = individualsDataSource.getIndividualCount(i_Id);
            if (i_Id > 0 && icount < 2)
            {
                deleteIndividual(i_Id);
                i_Id--;
                return;
            }
            if (i_Id > 0)
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
        buttonSoundMinus();
        buttonVibLong();

        int count_id = Integer.parseInt(view.getTag().toString());
        CountingWidgetLH widget = getCountFromIdLH(count_id);
        assert Objects.requireNonNull(widget).count != null;
        assert widget.count != null;
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
            if (i_Id > 0)
            {
                int icount1 = icount - 1;
                individualsDataSource.decreaseIndividual(i_Id, icount1);
            }
        }
    }

    public void countUpf3i(View view)
    {
        buttonSound();
        buttonVib();

        // iAtt used by EditIndividualActivity to decide where to store bulk count value
        int iAtt = 3; // 1 f1i, 2 f2i, 3 f3i, 4 pi, 5 li, 6 ei

        int count_id = Integer.parseInt(view.getTag().toString());
        CountingWidget widget = getCountFromId(count_id);
        if (widget != null)
        {
            widget.countUpf3i();
        }

        disableProximitySensor();

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
        assert Objects.requireNonNull(widget).count != null;
        assert widget.count != null;
        name = widget.count.name;
        datestamp = getcurDate();
        timestamp = getcurTime();

        i_Id = individualsDataSource.saveIndividual(individualsDataSource.createIndividuals
            (count_id, name, latitude, longitude, height, uncert, datestamp, timestamp));

        if (i_Id != 0)
        {
            // get edited info for individual and start EditIndividualActivity
            Intent intent = new Intent(CountingActivity.this, EditIndividualActivity.class);
            intent.putExtra("count_id", count_id);
            intent.putExtra("indiv_id", i_Id);
            intent.putExtra("SName", widget.count.name);
            intent.putExtra("Latitude", latitude);
            intent.putExtra("Longitude", longitude);
            intent.putExtra("Height", height);
            intent.putExtra("Uncert", uncert);
            intent.putExtra("indivAtt", iAtt);
            startActivity(intent);
        }
    }

    public void countUpLHf3i(View view)
    {
        buttonSound();
        buttonVib();

        int iAtt = 3;
        int count_id = Integer.parseInt(view.getTag().toString());
        CountingWidgetLH widget = getCountFromIdLH(count_id);
        if (widget != null)
        {
            widget.countUpLHf3i();
        }

        disableProximitySensor();

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
        assert Objects.requireNonNull(widget).count != null;
        assert widget.count != null;
        name = widget.count.name;
        datestamp = getcurDate();
        timestamp = getcurTime();

        i_Id = individualsDataSource.saveIndividual(individualsDataSource.createIndividuals
            (count_id, name, latitude, longitude, height, uncert, datestamp, timestamp));

        if (i_Id != 0)
        {
            // get edited info for individual and start EditIndividualActivity
            Intent intent = new Intent(CountingActivity.this, EditIndividualActivity.class);
            intent.putExtra("count_id", count_id);
            intent.putExtra("indiv_id", i_Id);
            intent.putExtra("SName", widget.count.name);
            intent.putExtra("Latitude", latitude);
            intent.putExtra("Longitude", longitude);
            intent.putExtra("Height", height);
            intent.putExtra("Uncert", uncert);
            intent.putExtra("indivAtt", iAtt);
            startActivity(intent);
        }
    }

    // Triggered by count down button
    // deletes last count
    public void countDownf3i(View view)
    {
        buttonSoundMinus();
        buttonVibLong();

        int count_id = Integer.parseInt(view.getTag().toString());
        CountingWidget widget = getCountFromId(count_id);
        assert Objects.requireNonNull(widget).count != null;
        assert widget.count != null;
        spec_name = widget.count.name; // set spec_name for toast in deleteIndividual
        spec_count = widget.count.count_f3i;
        if (spec_count > 0)
        {
            widget.countDownf3i();
            countDataSource.saveCountf3i(count);

            i_Id = individualsDataSource.getLastIndiv(count_id, 3);
            if (i_Id == -1)
            {
//                Toast.makeText(this, getString(R.string.getHelp) + spec_name, Toast.LENGTH_SHORT).show();
                showSnackbarRed(getString(R.string.getHelp) + spec_name);
                return;
            }
            int icount = individualsDataSource.getIndividualCount(i_Id);
            if (i_Id > 0 && icount < 2)
            {
                deleteIndividual(i_Id);
                i_Id--;
                return;
            }
            if (i_Id > 0)
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
        buttonSoundMinus();
        buttonVibLong();

        int count_id = Integer.parseInt(view.getTag().toString());
        CountingWidgetLH widget = getCountFromIdLH(count_id);
        assert Objects.requireNonNull(widget).count != null;
        assert widget.count != null;
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
            if (i_Id > 0)
            {
                int icount1 = icount - 1;
                individualsDataSource.decreaseIndividual(i_Id, icount1);
            }
        }
    }

    public void countUppi(View view)
    {
        buttonSound();
        buttonVib();

        // iAtt used by EditIndividualActivity to decide where to store bulk count value
        int iAtt = 4; // 1 f1i, 2 f2i, 3 f3i, 4 pi, 5 li, 6 ei

        int count_id = Integer.parseInt(view.getTag().toString());
        CountingWidget widget = getCountFromId(count_id);
        if (widget != null)
        {
            widget.countUppi();
        }

        disableProximitySensor();

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
        assert Objects.requireNonNull(widget).count != null;
        assert widget.count != null;
        name = widget.count.name;
        datestamp = getcurDate();
        timestamp = getcurTime();

        i_Id = individualsDataSource.saveIndividual(individualsDataSource.createIndividuals
            (count_id, name, latitude, longitude, height, uncert, datestamp, timestamp));

        if (i_Id != 0)
        {
            // get edited info for individual and start EditIndividualActivity
            Intent intent = new Intent(CountingActivity.this, EditIndividualActivity.class);
            intent.putExtra("count_id", count_id);
            intent.putExtra("indiv_id", i_Id);
            intent.putExtra("SName", widget.count.name);
            intent.putExtra("Latitude", latitude);
            intent.putExtra("Longitude", longitude);
            intent.putExtra("Height", height);
            intent.putExtra("Uncert", uncert);
            intent.putExtra("indivAtt", iAtt);
            startActivity(intent);
        }
    }

    public void countUpLHpi(View view)
    {
        buttonSound();
        buttonVib();

        int iAtt = 4;
        int count_id = Integer.parseInt(view.getTag().toString());
        CountingWidgetLH widget = getCountFromIdLH(count_id);
        if (widget != null)
        {
            widget.countUpLHpi();
        }

        disableProximitySensor();

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
        assert Objects.requireNonNull(widget).count != null;
        assert widget.count != null;
        name = widget.count.name;
        datestamp = getcurDate();
        timestamp = getcurTime();

        i_Id = individualsDataSource.saveIndividual(individualsDataSource.createIndividuals
            (count_id, name, latitude, longitude, height, uncert, datestamp, timestamp));

        if (i_Id != 0)
        {
            // get edited info for individual and start EditIndividualActivity
            Intent intent = new Intent(CountingActivity.this, EditIndividualActivity.class);
            intent.putExtra("count_id", count_id);
            intent.putExtra("indiv_id", i_Id);
            intent.putExtra("SName", widget.count.name);
            intent.putExtra("Latitude", latitude);
            intent.putExtra("Longitude", longitude);
            intent.putExtra("Height", height);
            intent.putExtra("Uncert", uncert);
            intent.putExtra("indivAtt", iAtt);
            startActivity(intent);
        }
    }

    // Triggered by count down button
    // deletes last count
    public void countDownpi(View view)
    {
        buttonSoundMinus();
        buttonVibLong();

        int count_id = Integer.parseInt(view.getTag().toString());
        CountingWidget widget = getCountFromId(count_id);
        assert Objects.requireNonNull(widget).count != null;
        assert widget.count != null;
        spec_name = widget.count.name; // set spec_name for toast in deleteIndividual
        spec_count = widget.count.count_pi;
        if (spec_count > 0)
        {
            widget.countDownpi();
            countDataSource.saveCountpi(count);

            i_Id = individualsDataSource.getLastIndiv(count_id, 4);
            if (i_Id == -1)
            {
//                Toast.makeText(this, getString(R.string.getHelp) + spec_name, Toast.LENGTH_SHORT).show();
                showSnackbarRed(getString(R.string.getHelp) + spec_name);
                return;
            }
            int icount = individualsDataSource.getIndividualCount(i_Id);
            if (i_Id > 0 && icount < 2)
            {
                deleteIndividual(i_Id);
                i_Id--;
                return;
            }
            if (i_Id > 0)
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
        buttonSoundMinus();
        buttonVibLong();

        int count_id = Integer.parseInt(view.getTag().toString());
        CountingWidgetLH widget = getCountFromIdLH(count_id);
        assert Objects.requireNonNull(widget).count != null;
        assert widget.count != null;
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
            if (i_Id > 0)
            {
                int icount1 = icount - 1;
                individualsDataSource.decreaseIndividual(i_Id, icount1);
            }
        }
    }

    public void countUpli(View view)
    {
        buttonSound();
        buttonVib();

        // iAtt used by EditIndividualActivity to decide where to store bulk count value
        int iAtt = 5; // 1 f1i, 2 f2i, 3 f3i, 4 pi, 5 li, 6 ei

        int count_id = Integer.parseInt(view.getTag().toString());
        CountingWidget widget = getCountFromId(count_id);
        if (widget != null)
        {
            widget.countUpli();
        }

        disableProximitySensor();

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
        assert Objects.requireNonNull(widget).count != null;
        assert widget.count != null;
        name = widget.count.name;
        datestamp = getcurDate();
        timestamp = getcurTime();

        i_Id = individualsDataSource.saveIndividual(individualsDataSource.createIndividuals
            (count_id, name, latitude, longitude, height, uncert, datestamp, timestamp));

        if (i_Id != 0)
        {
            // get edited info for individual and start EditIndividualActivity
            Intent intent = new Intent(CountingActivity.this, EditIndividualActivity.class);
            intent.putExtra("count_id", count_id);
            intent.putExtra("indiv_id", i_Id);
            intent.putExtra("SName", widget.count.name);
            intent.putExtra("Latitude", latitude);
            intent.putExtra("Longitude", longitude);
            intent.putExtra("Height", height);
            intent.putExtra("Uncert", uncert);
            intent.putExtra("indivAtt", iAtt);
            startActivity(intent);
        }
    }

    public void countUpLHli(View view)
    {
        buttonSound();
        buttonVib();

        int iAtt = 5;
        int count_id = Integer.parseInt(view.getTag().toString());
        CountingWidgetLH widget = getCountFromIdLH(count_id);
        if (widget != null)
        {
            widget.countUpLHli();
        }

        disableProximitySensor();

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
        assert Objects.requireNonNull(widget).count != null;
        assert widget.count != null;
        name = widget.count.name;
        datestamp = getcurDate();
        timestamp = getcurTime();

        i_Id = individualsDataSource.saveIndividual(individualsDataSource.createIndividuals
            (count_id, name, latitude, longitude, height, uncert, datestamp, timestamp));

        if (i_Id != 0)
        {
            // get edited info for individual and start EditIndividualActivity
            Intent intent = new Intent(CountingActivity.this, EditIndividualActivity.class);
            intent.putExtra("count_id", count_id);
            intent.putExtra("indiv_id", i_Id);
            intent.putExtra("SName", widget.count.name);
            intent.putExtra("Latitude", latitude);
            intent.putExtra("Longitude", longitude);
            intent.putExtra("Height", height);
            intent.putExtra("Uncert", uncert);
            intent.putExtra("indivAtt", iAtt);
            startActivity(intent);
        }
    }

    // Triggered by count down button
    // deletes last count
    public void countDownli(View view)
    {
        buttonSoundMinus();
        buttonVibLong();

        int count_id = Integer.parseInt(view.getTag().toString());
        CountingWidget widget = getCountFromId(count_id);
        assert Objects.requireNonNull(widget).count != null;
        assert widget.count != null;
        spec_name = widget.count.name; // set spec_name for toast in deleteIndividual
        spec_count = widget.count.count_li;
        if (spec_count > 0)
        {
            widget.countDownli();
            countDataSource.saveCountli(count);

            i_Id = individualsDataSource.getLastIndiv(count_id, 5);
            if (i_Id == -1)
            {
//                Toast.makeText(this, getString(R.string.getHelp) + spec_name, Toast.LENGTH_SHORT).show();
                showSnackbarRed(getString(R.string.getHelp) + spec_name);
                return;
            }
            int icount = individualsDataSource.getIndividualCount(i_Id);
            if (i_Id > 0 && icount < 2)
            {
                deleteIndividual(i_Id);
                i_Id--;
                return;
            }
            if (i_Id > 0)
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
        buttonSoundMinus();
        buttonVibLong();

        int count_id = Integer.parseInt(view.getTag().toString());
        CountingWidgetLH widget = getCountFromIdLH(count_id);
        assert Objects.requireNonNull(widget).count != null;
        assert widget.count != null;
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
            if (i_Id > 0)
            {
                int icount1 = icount - 1;
                individualsDataSource.decreaseIndividual(i_Id, icount1);
            }
        }
    }

    public void countUpei(View view)
    {
        buttonSound();
        buttonVib();

        // iAtt used by EditIndividualActivity to decide where to store bulk count value
        int iAtt = 6; // 1 f1i, 2 f2i, 3 f3i, 4 pi, 5 li, 6 ei

        int count_id = Integer.parseInt(view.getTag().toString());
        CountingWidget widget = getCountFromId(count_id);
        if (widget != null)
        {
            widget.countUpei();
        }

        disableProximitySensor();

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
        assert Objects.requireNonNull(widget).count != null;
        assert widget.count != null;
        name = widget.count.name;
        datestamp = getcurDate();
        timestamp = getcurTime();

        i_Id = individualsDataSource.saveIndividual(individualsDataSource.createIndividuals
            (count_id, name, latitude, longitude, height, uncert, datestamp, timestamp));

        if (i_Id != 0)
        {
            // get edited info for individual and start EditIndividualActivity
            Intent intent = new Intent(CountingActivity.this, EditIndividualActivity.class);
            intent.putExtra("count_id", count_id);
            intent.putExtra("indiv_id", i_Id);
            intent.putExtra("SName", widget.count.name);
            intent.putExtra("Latitude", latitude);
            intent.putExtra("Longitude", longitude);
            intent.putExtra("Height", height);
            intent.putExtra("Uncert", uncert);
            intent.putExtra("indivAtt", iAtt);
            startActivity(intent);
        }
    }

    public void countUpLHei(View view)
    {
        buttonSound();
        buttonVib();

        int iAtt = 6;
        int count_id = Integer.parseInt(view.getTag().toString());
        CountingWidgetLH widget = getCountFromIdLH(count_id);
        if (widget != null)
        {
            widget.countUpLHei();
        }

        disableProximitySensor();

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
        assert Objects.requireNonNull(widget).count != null;
        assert widget.count != null;
        name = widget.count.name;
        datestamp = getcurDate();
        timestamp = getcurTime();

        i_Id = individualsDataSource.saveIndividual(individualsDataSource.createIndividuals
            (count_id, name, latitude, longitude, height, uncert, datestamp, timestamp));

        if (i_Id != 0)
        {
            // get edited info for individual and start EditIndividualActivity
            Intent intent = new Intent(CountingActivity.this, EditIndividualActivity.class);
            intent.putExtra("count_id", count_id);
            intent.putExtra("indiv_id", i_Id);
            intent.putExtra("SName", widget.count.name);
            intent.putExtra("Latitude", latitude);
            intent.putExtra("Longitude", longitude);
            intent.putExtra("Height", height);
            intent.putExtra("Uncert", uncert);
            intent.putExtra("indivAtt", iAtt);
            startActivity(intent);
        }
    }

    // Triggered by count down button
    // deletes last count
    public void countDownei(View view)
    {
        buttonSoundMinus();
        buttonVibLong();

        int count_id = Integer.parseInt(view.getTag().toString());
        CountingWidget widget = getCountFromId(count_id);
        assert Objects.requireNonNull(widget).count != null;
        assert widget.count != null;
        spec_name = widget.count.name; // set spec_name for toast in deleteIndividual
        spec_count = widget.count.count_ei;
        if (spec_count > 0)
        {
            widget.countDownei();
            countDataSource.saveCountei(count);

            i_Id = individualsDataSource.getLastIndiv(count_id, 6);
            if (i_Id == -1)
            {
//                Toast.makeText(this, getString(R.string.getHelp) + spec_name, Toast.LENGTH_SHORT).show();
                showSnackbarRed(getString(R.string.getHelp) + spec_name);
                return;
            }
            int icount = individualsDataSource.getIndividualCount(i_Id);
            if (i_Id > 0 && icount < 2)
            {
                deleteIndividual(i_Id);
                i_Id--;
                return;
            }
            if (i_Id > 0)
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
        buttonSoundMinus();
        buttonVibLong();

        int count_id = Integer.parseInt(view.getTag().toString());
        CountingWidgetLH widget = getCountFromIdLH(count_id);
        assert Objects.requireNonNull(widget).count != null;
        assert widget.count != null;
        spec_name = widget.count.name; // set spec_name for toast in deleteIndividual
        spec_count = widget.count.count_ei;
        if (spec_count > 0)
        {
            widget.countDownLHei();
            countDataSource.saveCountei(count);

            i_Id = individualsDataSource.getLastIndiv(count_id, 6);
            if (i_Id == -1)
            {
                showSnackbarRed(getString(R.string.getHelp) + spec_name);
                return;
            }
            int icount = individualsDataSource.getIndividualCount(i_Id);
            if (i_Id > 0 && icount < 2)
            {
                deleteIndividual(i_Id);
                i_Id--;
                return;
            }
            if (i_Id > 0)
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
            assert widget.count != null;
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
            assert widget.count != null;
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
        disableProximitySensor();

        Intent intent = new Intent(CountingActivity.this, CountOptionsActivity.class);
        intent.putExtra("count_id", iid);
        startActivity(intent);
    }

    private void buttonSound()
    {
        if (buttonSoundPref)
        {
            try
            {
                Uri notification;
                if (isNotBlank(buttonSound) && buttonSound != null)
                {
                    notification = Uri.parse(buttonSound);
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

    private void buttonSoundMinus()
    {
        if (buttonSoundPref)
        {
            try
            {
                Uri notification;
                if (isNotBlank(buttonSoundMinus) && buttonSoundMinus != null)
                {
                    notification = Uri.parse(buttonSoundMinus);
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

    private void buttonVib()
    {
        if (buttonVibPref)
        {
            try
            {
                Vibrator vibrator;
                VibratorManager vibratorManager;
                if (Build.VERSION.SDK_INT >= 31)
                {
                     vibratorManager = (VibratorManager) getSystemService(VIBRATOR_MANAGER_SERVICE);
                     vibratorManager.getDefaultVibrator();
                }
                else
                {
                    vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
                    if (Build.VERSION.SDK_INT >= 26)
                    {
                        vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE));
                    }
                    else
                    {
                        vibrator.vibrate(100);
                    }
                }
            } catch (Exception e)
            {
                if (MyDebug.LOG)
                    Log.e(TAG, "could not vibrate.", e);
            }
        }
    }

    private void buttonVibLong()
    {
        if (buttonVibPref)
        {
            try
            {
                Vibrator vibrator;
                VibratorManager vibratorManager;
                if (Build.VERSION.SDK_INT >= 31)
                {
                    vibratorManager = (VibratorManager) getSystemService(VIBRATOR_MANAGER_SERVICE);
                    vibratorManager.getDefaultVibrator();
                }
                else
                {
                    vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
                    if (Build.VERSION.SDK_INT >= 26)
                    {
                        vibrator.vibrate(VibrationEffect.createOneShot(450, VibrationEffect.DEFAULT_AMPLITUDE));
                    }
                    else
                    {
                        vibrator.vibrate(450);
                    }
                }
            } catch (Exception e)
            {
                if (MyDebug.LOG)
                    Log.e(TAG, "could not vibrate.", e);
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
    @SuppressLint("QueryPermissionsNeeded")
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.menuEditSection)
        {
            disableProximitySensor();

            Toast.makeText(getApplicationContext(), getString(R.string.wait), Toast.LENGTH_SHORT).show(); // a Snackbar here comes incomplete

            // pause for 100 msec to show toast
            mHandler.postDelayed(() ->
            {
                Intent intent = new Intent(CountingActivity.this, EditSpecListActivity.class);
                startActivity(intent);
            }, 100);
            return true;
        }
        else if (id == R.id.menuTakePhoto)
        {
            Intent camIntent = new Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA);

            PackageManager packageManager = getPackageManager();
            @SuppressLint("QueryPermissionsNeeded") List<ResolveInfo> activities = packageManager.queryIntentActivities(camIntent,
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
                        showSnackbarRed(getString(R.string.noPhotoPermit));
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
            mProximityWakeLock.acquire(30 * 60 * 1000L /*30 minutes*/);
        }
    }

    @SuppressLint("NewApi")
    private void disableProximitySensor()
    {
        if (mProximityWakeLock == null)
        {
            return;
        }
        if (mProximityWakeLock.isHeld())
        {
            int flags = PowerManager.RELEASE_FLAG_WAIT_FOR_NO_PROXIMITY;
            mProximityWakeLock.release(flags);
        }
    }

    // puts up function to back button
    @Override
    public void onBackPressed()
    {
        NavUtils.navigateUpFromSameTask(this);
        super.onBackPressed();
    }

    public void onSharedPreferenceChanged(SharedPreferences prefs, String key)
    {
//        prefs.registerOnSharedPreferenceChangeListener(this);
        setPrefs();
    }

    private void showSnackbarRed(String str)
    {
        View view;
        if (lhandPref) // if left-handed counting page
        {
            view = findViewById(R.id.countingScreenLH);
        }
        else
        {
            view = findViewById(R.id.countingScreen);
        }
        Snackbar sB = Snackbar.make(view, str, Snackbar.LENGTH_LONG);
        sB.setActionTextColor(Color.RED);
        TextView tv = sB.getView().findViewById(R.id.snackbar_text);
        tv.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        tv.setTypeface(tv.getTypeface(), Typeface.BOLD);
        sB.show();
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
