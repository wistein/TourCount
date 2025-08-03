package com.wmstein.tourcount;

import static android.os.Build.VERSION.SDK_INT;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.database.CursorIndexOutOfBoundsException;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.text.HtmlCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.wmstein.egm.EarthGravitationalModel;
import com.wmstein.tourcount.database.Count;
import com.wmstein.tourcount.database.CountDataSource;
import com.wmstein.tourcount.database.IndividualsDataSource;
import com.wmstein.tourcount.database.Section;
import com.wmstein.tourcount.database.SectionDataSource;
import com.wmstein.tourcount.widgets.CountingWidget;
import com.wmstein.tourcount.widgets.CountingWidgetHead1;
import com.wmstein.tourcount.widgets.CountingWidgetHead2;
import com.wmstein.tourcount.widgets.CountingWidgetLH;
import com.wmstein.tourcount.widgets.NotesWidget;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/***************************************************************************************
 * CountingActivity is the central activity of TourCount.
 * It provides the counters, starts GPS-location polling, starts EditIndividualActivity,
 * starts EditSpecListActivity, switches screen off when device is pocketed
 * and allows taking pictures and sending notes.
 <p>
 * CountingActivity uses CountingWidget.kt, CountingWidgetLH.kt, NotesWidget.kt,
 * activity_counting.xml and activity_counting_lh.xml
 <p>
 * Basic counting functions created by milo for BeeCount on 2014-05-05.
 * Adopted, modified and enhanced for TourCount by wmstein since 2016-04-18,
 * last edited in Java on 2025-07-22
 */
public class CountingActivity
        extends AppCompatActivity
        implements SharedPreferences.OnSharedPreferenceChangeListener, SensorEventListener {
    private static final String TAG = "CountAct";

    private int iid = 1;
    private LinearLayout count_area;

    private LinearLayout head_area2;
    private LinearLayout notes_area1;
    private final Handler mHandler = new Handler(Looper.getMainLooper());

    // Data
    private Count count;
    private Section section;
    private List<CountingWidget> countingWidgets;
    private List<CountingWidgetLH> countingWidgetsLH;
    private Spinner spinner;
    private int itemPosition = 0;
    private int i_Id = 0; // Individuals id
    private String spec_name;
    private int specCnt;

    // Location info handling
    private double latitude, longitude, height;
    LocationService locationService;

    // locationDispatcherMode:
    //  1 = use location service
    //  2 = end location service
    private int locationDispatcherMode;

    private boolean locServiceOn = false;

    // Proximity sensor handling screen on/off
    private PowerManager mPowerManager;
    private PowerManager.WakeLock mProximityWakeLock;

    // Preferences
    private SharedPreferences prefs;
    private boolean awakePref;
    private boolean brightPref;
    private String sortPref;
    private boolean fontPref;
    private boolean lhandPref;       // true for left hand mode of counting screen
    private boolean buttonSoundPref;
    private String buttonSoundMinus; // buttonSound is handled in EditIndividualActivity
    private boolean buttonVibPref;
    private String specCode = "";
    private String proxSensorPref;
    private double sensorSensitivity = 0.0;

    // Data sources
    private SectionDataSource sectionDataSource;
    private CountDataSource countDataSource;
    private IndividualsDataSource individualsDataSource;

    private Ringtone r;

    // Prepare vibrator service
    private Vibrator vibrator;

    // Prepare proximity sensor usage
    private SensorManager mSensorManager;
    private Sensor mProximity;
    private String mesg = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (MyDebug.DLOG) Log.i(TAG, "155, onCreate");

        TourCountApplication tourCount = (TourCountApplication) getApplication();
        prefs = TourCountApplication.getPrefs();
        setPrefVariables(); // set all stored preferences into their variables

        // get parameters from WelcomeActivity
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            latitude = extras.getDouble("Latitude");
            longitude = extras.getDouble("Longitude");
            height = extras.getDouble("Height");
        }

        sectionDataSource = new SectionDataSource(this);
        countDataSource = new CountDataSource(this);
        individualsDataSource = new IndividualsDataSource(this);

        // Set full brightness of screen
        if (brightPref) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            WindowManager.LayoutParams params = getWindow().getAttributes();
            params.screenBrightness = 1.0f;
            getWindow().setAttributes(params);
        }

        if (SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) // SDK 35+
            EdgeToEdge.enable(this);

        // Distinguish between left-/ right-handed counting page layout
        if (lhandPref) {
            setContentView(R.layout.activity_counting_lh);
            LinearLayout counting_screen = findViewById(R.id.countingScreenLH);
            counting_screen.setBackground(tourCount.setBackgr());
            count_area = findViewById(R.id.countCountiLayoutLH);
            notes_area1 = findViewById(R.id.tourNotesLayoutLH);
            head_area2 = findViewById(R.id.countHead2LayoutLH);
            ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.countingScreenLH),
                    (v, windowInsets) -> {
                        Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
                        ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
                        mlp.topMargin = insets.top;
                        mlp.bottomMargin = insets.bottom;
                        mlp.leftMargin = insets.left;
                        mlp.rightMargin = insets.right;
                        v.setLayoutParams(mlp);
                        return WindowInsetsCompat.CONSUMED;
                    });
        } else {
            setContentView(R.layout.activity_counting);
            LinearLayout counting_screen = findViewById(R.id.countingScreen);
            counting_screen.setBackground(tourCount.setBackgr());
            count_area = findViewById(R.id.countCountiLayout);
            notes_area1 = findViewById(R.id.tourNotesLayout);
            head_area2 = findViewById(R.id.countHead2Layout);
            ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.countingScreen),
                    (v, windowInsets) -> {
                        Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
                        ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
                        mlp.topMargin = insets.top;
                        mlp.bottomMargin = insets.bottom;
                        mlp.leftMargin = insets.left;
                        mlp.rightMargin = insets.right;
                        v.setLayoutParams(mlp);
                        return WindowInsetsCompat.CONSUMED;
                    });
        }

        // Proximity sensor handling screen on/off
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mProximity = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        mPowerManager = (PowerManager) this.getSystemService(Context.POWER_SERVICE);
        if (mPowerManager.isWakeLockLevelSupported(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK))
            mProximityWakeLock = mPowerManager.newWakeLock(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK,
                    "TourCount:WAKELOCK");
        else
            mProximityWakeLock = null;

        // Get max. proximity sensitivity
        double sensorSensitivityMax;
        if (mProximity != null)
            sensorSensitivityMax = mProximity.getMaximumRange();
        else {
            sensorSensitivityMax = 0;
        }

        // Get proximity sensitivity selection from preferences
        if (sensorSensitivityMax != 0) {
            // Set sensorSensitivity proportional to value for max. sensitivity
            if (Objects.equals(proxSensorPref, "Off"))
                sensorSensitivity = 0;
            else if (Objects.equals(proxSensorPref, "Medium")) {
                sensorSensitivity = sensorSensitivityMax / 2;
            } else if (Objects.equals(proxSensorPref, "High")) {
                sensorSensitivity = sensorSensitivityMax - 0.1;
            }
        }

        // new onBackPressed logic
        // Different Navigation Bar modes and layouts:
        // - Classic three-button navigation: NavBarMode = 0
        // - Two-button navigation (Android P): NavBarMode = 1
        // - Full screen gesture mode (Android Q): NavBarMode = 2
        // Use only if NavBarMode = 0 or 1.
        if (getNavBarMode() == 0 || getNavBarMode() == 1) {
            OnBackPressedCallback callback = new OnBackPressedCallback(true) {
                @Override
                public void handleOnBackPressed() {
                    disableProximitySensor();

                    if (MyDebug.DLOG) Log.i(TAG, "265, handleOnBackPressed");
                    finish();
                    remove();
                }
            };
            getOnBackPressedDispatcher().addCallback(this, callback);
        }
    }
    // End of onCreate()

    // Check for Navigation bar 1-, 2- or 3-button mode
    public int getNavBarMode() {
        Resources resources = this.getResources();
        @SuppressLint("DiscouragedApi")
        int resourceId = resources.getIdentifier("config_navBarInteractionMode",
                "integer", "android");
        return resourceId > 0 ? resources.getInteger(resourceId) : 0;
    }

    // Load preferences at start, and also when a change is detected
    private void setPrefVariables() {
        awakePref = prefs.getBoolean("pref_awake", true);      // stay awake while counting
        brightPref = prefs.getBoolean("pref_bright", true);    // bright counting page
        sortPref = prefs.getString("pref_sort_sp", "none");    // sorted species list on counting page
        fontPref = prefs.getBoolean("pref_note_font", false);  // larger font for remarks
        lhandPref = prefs.getBoolean("pref_left_hand", false); // left-handed counting page
        buttonSoundPref = prefs.getBoolean("pref_button_sound", false); // make button sound
        buttonVibPref = prefs.getBoolean("pref_button_vib", false); // make vibration
        buttonSoundMinus = prefs.getString("button_sound_minus", null); //use deeper button sound
        itemPosition = prefs.getInt("item_Position", 0);        // spinner pos.
        iid = prefs.getInt("count_id", 1);                      // species id
        proxSensorPref = prefs.getString("pref_prox", "Off");
    }

    @SuppressLint("DiscouragedApi")
    @Override
    protected void onResume() {
        super.onResume();

        if (MyDebug.DLOG) Log.i(TAG, "304, onResume");

        mSensorManager.registerListener(this, mProximity, SensorManager.SENSOR_DELAY_NORMAL);

        // Prepare vibrator service
        vibrator = getApplicationContext().getSystemService(Vibrator.class);

        // Get location with permissions check
        locationDispatcherMode = 1;
        locationDispatcher();

        prefs = TourCountApplication.getPrefs();
        prefs.registerOnSharedPreferenceChangeListener(this);
        setPrefVariables(); // set prefs into their variables

        // Set full brightness of screen
        if (brightPref) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            WindowManager.LayoutParams params = getWindow().getAttributes();
            params.screenBrightness = 1.0f;
            getWindow().setAttributes(params);
        }

        // build the counting screen
        // clear any existing views
        count_area.removeAllViews();
        notes_area1.removeAllViews();
        head_area2.removeAllViews();

        // Setup the data sources
        sectionDataSource.open();
        countDataSource.open();
        individualsDataSource.open();

        // Load the section data
        try {
            section = sectionDataSource.getSection();
        } catch (CursorIndexOutOfBoundsException e) {
            mesg = getString(R.string.getHelp);
            Toast.makeText(this,
                    HtmlCompat.fromHtml("<font color='red'><b>" + mesg + "</b></font>",
                            HtmlCompat.FROM_HTML_MODE_LEGACY), Toast.LENGTH_LONG).show();
            finish();
        }

        // Load and show the data, set title in ActionBar
        try {
            Objects.requireNonNull(getSupportActionBar()).setTitle(section.name);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        } catch (NullPointerException e) {
            // nothing
        }

        String[] idArray;
        String[] nameArray;
        String[] nameArrayG;
        String[] codeArray;

        switch (sortPref) {
            case "names_alpha" -> {
                idArray = countDataSource.getAllIdsSrtName();
                nameArray = countDataSource.getAllStringsSrtName("name");
                codeArray = countDataSource.getAllStringsSrtName("code");
                nameArrayG = countDataSource.getAllStringsSrtName("name_g");
            }

            case "codes" -> {
                idArray = countDataSource.getAllIdsSrtCode();
                nameArray = countDataSource.getAllStringsSrtCode("name");
                codeArray = countDataSource.getAllStringsSrtCode("code");
                nameArrayG = countDataSource.getAllStringsSrtCode("name_g");
            }

            default -> {
                idArray = countDataSource.getAllIds();
                nameArray = countDataSource.getAllStrings("name");
                codeArray = countDataSource.getAllStrings("code");
                nameArrayG = countDataSource.getAllStrings("name_g");
            }
        }

        String ucode, rName;
        int resId, resId0;
        int z = 0;
        resId0 = getResources().getIdentifier("p00000", "drawable",
                this.getPackageName());

        Integer[] imageArray = new Integer[codeArray.length];
        for (String code : codeArray) {
            ucode = code;
            rName = "p" + ucode;
            resId = getResources().getIdentifier(rName, "drawable",
                    this.getPackageName());
            if (resId != 0)
                imageArray[z] = resId;
            else
                imageArray[z] = resId0;
            z++;
        }

        countingWidgets = new ArrayList<>();
        countingWidgetsLH = new ArrayList<>();

        // Display list notes
        if (section.notes != null) {
            if (!section.notes.isEmpty()) {
                NotesWidget section_notes = new NotesWidget(this, null);
                section_notes.setNotes(section.notes);
                section_notes.setFont(fontPref);
                notes_area1.addView(section_notes);
            }
        }

        // Show head1: Species with spinner to select
        if (lhandPref) // if left-handed counting page
            spinner = findViewById(R.id.countHead1SpinnerLH);
        else
            spinner = findViewById(R.id.countHead1Spinner);

        // Get itemPosition of added species by specCode from sharedPreference
        if (!Objects.equals(prefs.getString("new_spec_code", ""), "")) {
            specCode = prefs.getString("new_spec_code", "");
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("new_spec_code", ""); // clear prefs value after use
            editor.apply();
        }
        if (!Objects.equals(specCode, "")) {
            int i = 0;
            while (i <= codeArray.length) {
                assert specCode != null;
                if (specCode.equals(codeArray[i])) {
                    itemPosition = i;
                    break;
                }
                i++;
            }
            specCode = "";
        }

        // Set part of counting screen
        CountingWidgetHead1 adapter = new CountingWidgetHead1(this,
                idArray, nameArray, nameArrayG, codeArray, imageArray);
        spinner.setAdapter(adapter);
        spinner.setSelection(itemPosition);
        spinnerListener();

        if (awakePref)
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }
    // End of onResume()

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_PROXIMITY) {
            if (MyDebug.DLOG)
                Log.d(TAG, "459 Value0: " + event.values[0] + ", " + "Sensitivity: "
                        + (sensorSensitivity));

            // if ([0|5] >= [-0|-2.5|-4.9] && [0|5] < [0|2.5|4.9])
            if (event.values[0] >= -sensorSensitivity && event.values[0] < sensorSensitivity) {
                // near
                if (mProximityWakeLock == null)
                    mProximityWakeLock = mPowerManager.newWakeLock(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK,
                            "TourCount:WAKELOCK");

                if (!mProximityWakeLock.isHeld())
                    mProximityWakeLock.acquire(30 * 60 * 1000L); // 30 minutes
            } else {
                // far
                disableProximitySensor();
            }
        }
    }

    // Necessary for SensorEventListener
    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
    }

    private void disableProximitySensor() // far
    {
        if (mProximityWakeLock == null)
            return;
        if (mProximityWakeLock.isHeld()) {
            int flags = PowerManager.RELEASE_FLAG_WAIT_FOR_NO_PROXIMITY;
            mProximityWakeLock.release(flags);
            mProximityWakeLock = null;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.counting, menu);
        return true;
    }

    // Handle menu selections
    @SuppressLint("QueryPermissionsNeeded")
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) // back button in actionBar
        {
            disableProximitySensor();

            finish();
            return true;
        }

        else if (id == R.id.menuAddSpecies) {
            disableProximitySensor();

            Intent intent = new Intent(CountingActivity.this,
                    AddSpeciesActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            return true;
        }

        else if (id == R.id.menuDelSpecies) {
            disableProximitySensor();

            mesg = getString(R.string.wait);
            Toast.makeText(this,
                    HtmlCompat.fromHtml("<font color='#008000'>" + mesg + "</font>",
                            HtmlCompat.FROM_HTML_MODE_LEGACY), Toast.LENGTH_SHORT).show();

            Intent intent = new Intent(CountingActivity.this,
                    DelSpeciesActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

            // Trick: Pause for 100 msec to show toast
            mHandler.postDelayed(() ->
                    startActivity(intent), 100);
            return true;
        }

        else if (id == R.id.menuEditSection) {
            disableProximitySensor();

            mesg = getString(R.string.wait);
            Toast.makeText(this,
                    HtmlCompat.fromHtml("<font color='#008000'>" + mesg + "</font>",
                            HtmlCompat.FROM_HTML_MODE_LEGACY), Toast.LENGTH_SHORT).show();

            Intent intent = new Intent(CountingActivity.this,
                    EditSpecListActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

            // Trick: Pause for 100 msec to show toast
            mHandler.postDelayed(() ->
                    startActivity(intent), 100);
            return true;
        }

        else if (id == R.id.menuTakePhoto) {
            Intent camIntent = new Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA);

            PackageManager packageManager = getPackageManager();
            List<ResolveInfo> activities = packageManager.queryIntentActivities(camIntent,
                    PackageManager.MATCH_DEFAULT_ONLY);

            // Select from available camera apps
            boolean isIntentSafe = !activities.isEmpty();
            if (isIntentSafe) {
                String title = getResources().getString(R.string.chooserTitle);
                Intent chooser = Intent.createChooser(camIntent, title);
                if (camIntent.resolveActivity(getPackageManager()) != null) {
                    try {
                        startActivity(chooser);
                    } catch (Exception e) {
                        mesg = getString(R.string.noPhotoPermit);
                        Toast.makeText(this,
                                HtmlCompat.fromHtml("<font color='red'><b>" + mesg + "</b></font>",
                                        HtmlCompat.FROM_HTML_MODE_LEGACY), Toast.LENGTH_LONG).show();
                    }
                }
            } else {
                // Only default camera available
                startActivity(camIntent);
            }
            return true;
        }

        else if (id == R.id.action_share) {
            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_SUBJECT, "TourCount");
            sendIntent.putExtra(Intent.EXTRA_TITLE, "Message of TourCount");
            sendIntent.putExtra(Intent.EXTRA_TEXT, section.name + ": ");
            sendIntent.setType("text/plain");
            startActivity(Intent.createChooser(sendIntent, getResources().getText(R.string.send_to)));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    // End of onOptionsItemSelected()

    // Edit count options by CountOptionsActivity by button in widget_counting_head2.xml
    public void editOptions(View view) {
        Intent intent = new Intent(CountingActivity.this, CountOptionsActivity.class);
        intent.putExtra("count_id", iid);
        startActivity(intent);
    }

    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        setPrefVariables();
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (MyDebug.DLOG) Log.i(TAG, "619, onPause");

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

        // N.B. a wakelock might not be held, e.g. if someone is using Cyanogenmod and
        // has denied wakelock permission to TourCount
        if (awakePref) {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

        // Stop location service with permissions check
        locationDispatcherMode = 2;
        locationDispatcher();

        prefs.unregisterOnSharedPreferenceChangeListener(this);
        mSensorManager.unregisterListener(this);
    }
    // End of onPause()

    @Override
    public void onStop() {
        super.onStop();

        if (MyDebug.DLOG) Log.i(TAG, "653, onStop");

        if (r != null)
            r.stop(); // stop media player
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (MyDebug.DLOG) Log.i(TAG, "663, onDestroy");
    }

    public void locationDispatcher() {
        if (isFineLocationPermGranted()) {
            switch (locationDispatcherMode) {
                case 1 -> // get location
                        getLoc();
                case 2 -> // stop location service
                {
                    if (locServiceOn) {
                        locationService.stopListener();
                        Intent sIntent = new Intent(this, LocationService.class);
                        stopService(sIntent);
                        locServiceOn = false;
                    }
                }
            }
        }
    }

    private boolean isFineLocationPermGranted() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    // Get the location data
    public void getLoc() {
        locationService = new LocationService(this);
        Intent sIntent = new Intent(this, LocationService.class);
        startService(sIntent);
        locServiceOn = true;

        if (locationService.canGetLocation()) {
            longitude = locationService.getLongitude();
            latitude = locationService.getLatitude();
            height = locationService.getAltitude();
            if (height != 0)
                height = correctHeight(latitude, longitude, height);
        }
    }

    // Correct height with geoid offset from EarthGravitationalModel
    private double correctHeight(double latitude, double longitude, double gpsHeight) {
        double corrHeight;
        double nnHeight;

        EarthGravitationalModel gh = new EarthGravitationalModel();
        try {
            gh.load(this); // load the WGS84 correction coefficient table egm180.txt
        } catch (IOException e) {
            return 0;
        }

        // Calculate the offset between the ellipsoid and geoid
        try {
            corrHeight = gh.heightOffset(latitude, longitude, gpsHeight);
        } catch (Exception e) {
            return 0;
        }

        nnHeight = gpsHeight + corrHeight;
        return nnHeight;
    }

    // Spinner listener
    private void spinnerListener() {
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long aid) {
                try {
                    head_area2.removeAllViews();
                    count_area.removeAllViews();

                    String sid = ((TextView) view.findViewById(R.id.countId)).getText().toString();
                    iid = Integer.parseInt(sid); // get species id
                    itemPosition = position;

                    count = countDataSource.getCountById(iid);
                    countingScreen(count);
                    if (MyDebug.DLOG) Log.d(TAG, "752, SpinnerListener, count id: "
                            + count.id + ", code: " + count.code + ", name: " + count.name);
                } catch (Exception e) {
                    // Exception may occur when permissions are changed while activity is paused
                    //  or when spinner is rapidly repeatedly pressed
                    if (MyDebug.DLOG) Log.e(TAG, "757, SpinnerListener: " + e);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // stub, necessary to make Spinner work correctly when repeatedly used
            }
        });
    }

    // Show rest of widgets for counting screen
    private void countingScreen(Count count) {
        if (MyDebug.DLOG) Log.i(TAG, "770, countingScreen");

        // 1. Species line is set by CountingWidgetHead1 in onResume, Spinner
        // 2. Head2 with species notes and edit button
        CountingWidgetHead2 head2 = new CountingWidgetHead2(this, null);
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
        } else {
            CountingWidget widgeti = new CountingWidget(this, null);
            widgeti.setCount(count);
            countingWidgets.add(widgeti);
            count_area.addView(widgeti);
        }
    }
    // End of countingScreen

    // Get the referenced counting widgets
    // CountingWidget (right-handed)
    private CountingWidget getCountFromId(int id) {
        for (CountingWidget widget : countingWidgets) {
            assert widget.count != null;
            if (widget.count.id == id)
                return widget;
        }
        return null;
    }

    // CountingWidget (left-handed)
    private CountingWidgetLH getCountFromIdLH(int id) {
        for (CountingWidgetLH widget : countingWidgetsLH) {
            assert widget.count != null;
            if (widget.count.id == id)
                return widget;
        }
        return null;
    }

    /*****************************************************************
     * The functions below are triggered by the count buttons
     * on the righthand/lefthand (LH) views.
     * <p>
     * For up-counting they start EditIndividualActivity
     */
    public void countUpf1i(View view) {
        // iAtt used by EditIndividualActivity to decide where to store bulk count value
        int iAtt = 1; // 1 f1i, 2 f2i, 3 f3i, 4 pi, 5 li, 6 ei

        int count_id = Integer.parseInt(view.getTag().toString());
        CountingWidget widget = getCountFromId(count_id);
        if (widget != null)
            widget.countUpf1i();

        disableProximitySensor(); // for EditIndividualActivity

        assert Objects.requireNonNull(widget).count != null;

        // get edited info for individual and start EditIndividualActivity
        Intent intent = new Intent(CountingActivity.this, EditIndividualActivity.class);
        intent.putExtra("count_id", count_id);
        intent.putExtra("SName", widget.count.name);
        intent.putExtra("SCode", widget.count.code);
        intent.putExtra("date", getcurDate());
        intent.putExtra("time", getcurTime());
        intent.putExtra("indivAtt", iAtt);
        startActivity(intent);
    }

    public void countUpLHf1i(View view) {
        int iAtt = 1;
        int count_id = Integer.parseInt(view.getTag().toString());
        CountingWidgetLH widget = getCountFromIdLH(count_id);
        if (widget != null)
            widget.countUpLHf1i();

        disableProximitySensor(); // for EditIndividualActivity

        assert Objects.requireNonNull(widget).count != null;

        // get edited info for individual and start EditIndividualActivity
        Intent intent = new Intent(CountingActivity.this, EditIndividualActivity.class);
        intent.putExtra("count_id", count_id);
        intent.putExtra("SName", widget.count.name);
        intent.putExtra("SCode", widget.count.code);
        intent.putExtra("date", getcurDate());
        intent.putExtra("time", getcurTime());
        intent.putExtra("indivAtt", iAtt);
        startActivity(intent);
    }

    // Triggered by count down button
    // deletes last count
    public void countDownf1i(View view) {
        soundButtonSoundMinus();
        buttonVib();

        int count_id = Integer.parseInt(view.getTag().toString());
        CountingWidget widget = getCountFromId(count_id);
        assert Objects.requireNonNull(widget).count != null;
        spec_name = widget.count.name; // set spec_name for toast in deleteIndividual
        specCnt = widget.count.count_f1i;
        if (specCnt > 0) {
            widget.countDownf1i(); // decrease species counter
            countDataSource.saveCountf1i(count);

            // get last individual of category 1 (♂|♀)
            i_Id = individualsDataSource.getLastIndiv(count_id, 1);
            if (i_Id == -1) {
                mesg = getString(R.string.getHelp) + spec_name;
                Toast.makeText(this,
                        HtmlCompat.fromHtml("<font color='red'><b>" + mesg + "</b></font>",
                                HtmlCompat.FROM_HTML_MODE_LEGACY), Toast.LENGTH_LONG).show();
                return;
            }
            int icount = individualsDataSource.getIndividualCount(i_Id);
            if (i_Id > 0 && icount < 2) {
                deleteIndividual(i_Id);
                i_Id--;
                return;
            }
            if (i_Id > 0) {
                int icount1 = icount - 1;
                individualsDataSource.decreaseIndividual(i_Id, icount1);
            }
        }
    }

    // Triggered by count down button from left-hand view
    // deletes last count
    public void countDownLHf1i(View view) {
        soundButtonSoundMinus();
        buttonVib();

        int count_id = Integer.parseInt(view.getTag().toString());
        CountingWidgetLH widget = getCountFromIdLH(count_id);
        assert Objects.requireNonNull(widget).count != null;
        spec_name = widget.count.name; // set spec_name for toast in deleteIndividual
        specCnt = widget.count.count_f1i;
        if (specCnt > 0) {
            widget.countDownLHf1i();
            countDataSource.saveCountf1i(count);

            i_Id = individualsDataSource.getLastIndiv(count_id, 1);
            if (i_Id == -1) {
                mesg = getString(R.string.getHelp) + spec_name;
                Toast.makeText(this,
                        HtmlCompat.fromHtml("<font color='red'><b>" + mesg + "</b></font>",
                                HtmlCompat.FROM_HTML_MODE_LEGACY), Toast.LENGTH_LONG).show();
                return;
            }
            int icount = individualsDataSource.getIndividualCount(i_Id);
            if (i_Id > 0 && icount < 2) {
                deleteIndividual(i_Id);
                i_Id--;
                return;
            }
            if (i_Id > 0) {
                int icount1 = icount - 1;
                individualsDataSource.decreaseIndividual(i_Id, icount1);
            }
        }
    }

    // Triggered by count up button for ♂
    // starts EditIndividualActivity
    public void countUpf2i(View view) {
        // iAtt used by EditIndividualActivity to decide where to store bulk count value
        int iAtt = 2; // 1 f1i, 2 f2i, 3 f3i, 4 pi, 5 li, 6 ei

        int count_id = Integer.parseInt(view.getTag().toString());
        CountingWidget widget = getCountFromId(count_id);
        if (widget != null)
            widget.countUpf2i();

        disableProximitySensor();

        assert Objects.requireNonNull(widget).count != null;

        // get edited info for individual and start EditIndividualActivity
        Intent intent = new Intent(CountingActivity.this, EditIndividualActivity.class);
        intent.putExtra("count_id", count_id);
        intent.putExtra("SName", widget.count.name);
        intent.putExtra("SCode", widget.count.code);
        intent.putExtra("date", getcurDate());
        intent.putExtra("time", getcurTime());
        intent.putExtra("indivAtt", iAtt);
        startActivity(intent);
    }

    public void countUpLHf2i(View view) {
        int iAtt = 2;
        int count_id = Integer.parseInt(view.getTag().toString());
        CountingWidgetLH widget = getCountFromIdLH(count_id);
        if (widget != null)
            widget.countUpLHf2i();

        disableProximitySensor();

        assert Objects.requireNonNull(widget).count != null;

        // get edited info for individual and start EditIndividualActivity
        Intent intent = new Intent(CountingActivity.this, EditIndividualActivity.class);
        intent.putExtra("count_id", count_id);
        intent.putExtra("SName", widget.count.name);
        intent.putExtra("SCode", widget.count.code);
        intent.putExtra("date", getcurDate());
        intent.putExtra("time", getcurTime());
        intent.putExtra("indivAtt", iAtt);
        startActivity(intent);
    }

    // Triggered by count down button
    // deletes last count
    public void countDownf2i(View view) {
        soundButtonSoundMinus();
        buttonVib();

        int count_id = Integer.parseInt(view.getTag().toString());
        CountingWidget widget = getCountFromId(count_id);
        assert Objects.requireNonNull(widget).count != null;
        spec_name = widget.count.name; // set spec_name for toast in deleteIndividual
        specCnt = widget.count.count_f2i;
        if (specCnt > 0) {
            widget.countDownf2i();
            countDataSource.saveCountf2i(count);

            i_Id = individualsDataSource.getLastIndiv(count_id, 2);
            if (i_Id == -1) {
                mesg = getString(R.string.getHelp) + spec_name;
                Toast.makeText(this,
                        HtmlCompat.fromHtml("<font color='red'><b>" + mesg + "</b></font>",
                                HtmlCompat.FROM_HTML_MODE_LEGACY), Toast.LENGTH_LONG).show();
                return;
            }
            int icount = individualsDataSource.getIndividualCount(i_Id);
            if (i_Id > 0 && icount < 2) {
                deleteIndividual(i_Id);
                i_Id--;
                return;
            }
            if (i_Id > 0) {
                int icount1 = icount - 1;
                individualsDataSource.decreaseIndividual(i_Id, icount1);
            }
        }
    }

    // Triggered by count down button from left-hand view
    // deletes last count
    public void countDownLHf2i(View view) {
        soundButtonSoundMinus();
        buttonVib();

        int count_id = Integer.parseInt(view.getTag().toString());
        CountingWidgetLH widget = getCountFromIdLH(count_id);
        assert Objects.requireNonNull(widget).count != null;
        spec_name = widget.count.name; // set spec_name for toast in deleteIndividual
        specCnt = widget.count.count_f2i;
        if (specCnt > 0) {
            widget.countDownLHf2i();
            countDataSource.saveCountf2i(count);

            i_Id = individualsDataSource.getLastIndiv(count_id, 2);
            if (i_Id == -1) {
                mesg = getString(R.string.getHelp) + spec_name;
                Toast.makeText(this,
                        HtmlCompat.fromHtml("<font color='red'><b>" + mesg + "</b></font>",
                                HtmlCompat.FROM_HTML_MODE_LEGACY), Toast.LENGTH_LONG).show();
                return;
            }
            int icount = individualsDataSource.getIndividualCount(i_Id);
            if (i_Id > 0 && icount < 2) {
                deleteIndividual(i_Id);
                i_Id--;
                return;
            }
            if (i_Id > 0) {
                int icount1 = icount - 1;
                individualsDataSource.decreaseIndividual(i_Id, icount1);
            }
        }
    }

    public void countUpf3i(View view) {
        // iAtt used by EditIndividualActivity to decide where to store bulk count value
        int iAtt = 3; // 1 f1i, 2 f2i, 3 f3i, 4 pi, 5 li, 6 ei

        int count_id = Integer.parseInt(view.getTag().toString());
        CountingWidget widget = getCountFromId(count_id);
        if (widget != null)
            widget.countUpf3i();

        disableProximitySensor();

        assert Objects.requireNonNull(widget).count != null;

        // get edited info for individual and start EditIndividualActivity
        Intent intent = new Intent(CountingActivity.this, EditIndividualActivity.class);
        intent.putExtra("count_id", count_id);
        intent.putExtra("SName", widget.count.name);
        intent.putExtra("SCode", widget.count.code);
        intent.putExtra("date", getcurDate());
        intent.putExtra("time", getcurTime());
        intent.putExtra("indivAtt", iAtt);
        startActivity(intent);
    }

    public void countUpLHf3i(View view) {
        int iAtt = 3;
        int count_id = Integer.parseInt(view.getTag().toString());
        CountingWidgetLH widget = getCountFromIdLH(count_id);
        if (widget != null)
            widget.countUpLHf3i();

        disableProximitySensor();

        assert Objects.requireNonNull(widget).count != null;

        // get edited info for individual and start EditIndividualActivity
        Intent intent = new Intent(CountingActivity.this, EditIndividualActivity.class);
        intent.putExtra("count_id", count_id);
        intent.putExtra("SName", widget.count.name);
        intent.putExtra("SCode", widget.count.code);
        intent.putExtra("date", getcurDate());
        intent.putExtra("time", getcurTime());
        intent.putExtra("indivAtt", iAtt);
        startActivity(intent);
    }

    // Triggered by count down button
    // deletes last count
    public void countDownf3i(View view) {
        soundButtonSoundMinus();
        buttonVib();

        int count_id = Integer.parseInt(view.getTag().toString());
        CountingWidget widget = getCountFromId(count_id);
        assert Objects.requireNonNull(widget).count != null;
        spec_name = widget.count.name; // set spec_name for toast in deleteIndividual
        specCnt = widget.count.count_f3i;
        if (specCnt > 0) {
            widget.countDownf3i();
            countDataSource.saveCountf3i(count);

            i_Id = individualsDataSource.getLastIndiv(count_id, 3);
            if (i_Id == -1) {
                mesg = getString(R.string.getHelp) + spec_name;
                Toast.makeText(this,
                        HtmlCompat.fromHtml("<font color='red'><b>" + mesg + "</b></font>",
                                HtmlCompat.FROM_HTML_MODE_LEGACY), Toast.LENGTH_LONG).show();
                return;
            }
            int icount = individualsDataSource.getIndividualCount(i_Id);
            if (i_Id > 0 && icount < 2) {
                deleteIndividual(i_Id);
                i_Id--;
                return;
            }
            if (i_Id > 0) {
                int icount1 = icount - 1;
                individualsDataSource.decreaseIndividual(i_Id, icount1);
            }
        }
    }

    // Triggered by count down button from left-hand view
    // deletes last count
    public void countDownLHf3i(View view) {
        soundButtonSoundMinus();
        buttonVib();

        int count_id = Integer.parseInt(view.getTag().toString());
        CountingWidgetLH widget = getCountFromIdLH(count_id);
        assert Objects.requireNonNull(widget).count != null;
        spec_name = widget.count.name; // set spec_name for toast in deleteIndividual
        specCnt = widget.count.count_f3i;
        if (specCnt > 0) {
            widget.countDownLHf3i();
            countDataSource.saveCountf3i(count);

            i_Id = individualsDataSource.getLastIndiv(count_id, 3);
            if (i_Id == -1) {
                mesg = getString(R.string.getHelp) + spec_name;
                Toast.makeText(this,
                        HtmlCompat.fromHtml("<font color='red'><b>" + mesg + "</b></font>",
                                HtmlCompat.FROM_HTML_MODE_LEGACY), Toast.LENGTH_LONG).show();
                return;
            }
            int icount = individualsDataSource.getIndividualCount(i_Id);
            if (i_Id > 0 && icount < 2) {
                deleteIndividual(i_Id);
                i_Id--;
                return;
            }
            if (i_Id > 0) {
                int icount1 = icount - 1;
                individualsDataSource.decreaseIndividual(i_Id, icount1);
            }
        }
    }

    public void countUppi(View view) {
        // iAtt used by EditIndividualActivity to decide where to store bulk count value
        int iAtt = 4; // 1 f1i, 2 f2i, 3 f3i, 4 pi, 5 li, 6 ei

        int count_id = Integer.parseInt(view.getTag().toString());
        CountingWidget widget = getCountFromId(count_id);
        if (widget != null)
            widget.countUppi();

        disableProximitySensor();

        assert Objects.requireNonNull(widget).count != null;

        // get edited info for individual and start EditIndividualActivity
        Intent intent = new Intent(CountingActivity.this, EditIndividualActivity.class);
        intent.putExtra("count_id", count_id);
        intent.putExtra("SName", widget.count.name);
        intent.putExtra("SCode", widget.count.code);
        intent.putExtra("date", getcurDate());
        intent.putExtra("time", getcurTime());
        intent.putExtra("indivAtt", iAtt);
        startActivity(intent);
    }

    public void countUpLHpi(View view) {
        int iAtt = 4;
        int count_id = Integer.parseInt(view.getTag().toString());
        CountingWidgetLH widget = getCountFromIdLH(count_id);
        if (widget != null)
            widget.countUpLHpi();

        disableProximitySensor();

        assert Objects.requireNonNull(widget).count != null;

        // get edited info for individual and start EditIndividualActivity
        Intent intent = new Intent(CountingActivity.this, EditIndividualActivity.class);
        intent.putExtra("count_id", count_id);
        intent.putExtra("SName", widget.count.name);
        intent.putExtra("SCode", widget.count.code);
        intent.putExtra("date", getcurDate());
        intent.putExtra("time", getcurTime());
        intent.putExtra("indivAtt", iAtt);
        startActivity(intent);
    }

    // Triggered by count down button
    // deletes last count
    public void countDownpi(View view) {
        soundButtonSoundMinus();
        buttonVib();

        int count_id = Integer.parseInt(view.getTag().toString());
        CountingWidget widget = getCountFromId(count_id);
        assert Objects.requireNonNull(widget).count != null;
        spec_name = widget.count.name; // set spec_name for toast in deleteIndividual
        specCnt = widget.count.count_pi;
        if (specCnt > 0) {
            widget.countDownpi();
            countDataSource.saveCountpi(count);

            i_Id = individualsDataSource.getLastIndiv(count_id, 4);
            if (i_Id == -1) {
                mesg = getString(R.string.getHelp) + spec_name;
                Toast.makeText(this,
                        HtmlCompat.fromHtml("<font color='red'><b>" + mesg + "</b></font>",
                                HtmlCompat.FROM_HTML_MODE_LEGACY), Toast.LENGTH_LONG).show();
                return;
            }
            int icount = individualsDataSource.getIndividualCount(i_Id);
            if (i_Id > 0 && icount < 2) {
                deleteIndividual(i_Id);
                i_Id--;
                return;
            }
            if (i_Id > 0) {
                int icount1 = icount - 1;
                individualsDataSource.decreaseIndividual(i_Id, icount1);
            }
        }
    }

    // Triggered by count down button from left-hand view
    // deletes last count
    public void countDownLHpi(View view) {
        soundButtonSoundMinus();
        buttonVib();

        int count_id = Integer.parseInt(view.getTag().toString());
        CountingWidgetLH widget = getCountFromIdLH(count_id);
        assert Objects.requireNonNull(widget).count != null;
        spec_name = widget.count.name; // set spec_name for toast in deleteIndividual
        specCnt = widget.count.count_pi;
        if (specCnt > 0) {
            widget.countDownLHpi();
            countDataSource.saveCountpi(count);

            i_Id = individualsDataSource.getLastIndiv(count_id, 4);
            if (i_Id == -1) {
                mesg = getString(R.string.getHelp) + spec_name;
                Toast.makeText(this,
                        HtmlCompat.fromHtml("<font color='red'><b>" + mesg + "</b></font>",
                                HtmlCompat.FROM_HTML_MODE_LEGACY), Toast.LENGTH_LONG).show();
                return;
            }
            int icount = individualsDataSource.getIndividualCount(i_Id);
            if (i_Id > 0 && icount < 2) {
                deleteIndividual(i_Id);
                i_Id--;
                return;
            }
            if (i_Id > 0) {
                int icount1 = icount - 1;
                individualsDataSource.decreaseIndividual(i_Id, icount1);
            }
        }
    }

    public void countUpli(View view) {
        // iAtt used by EditIndividualActivity to decide where to store bulk count value
        int iAtt = 5; // 1 f1i, 2 f2i, 3 f3i, 4 pi, 5 li, 6 ei

        int count_id = Integer.parseInt(view.getTag().toString());
        CountingWidget widget = getCountFromId(count_id);
        if (widget != null)
            widget.countUpli();

        disableProximitySensor();

        assert Objects.requireNonNull(widget).count != null;

        // get edited info for individual and start EditIndividualActivity
        Intent intent = new Intent(CountingActivity.this, EditIndividualActivity.class);
        intent.putExtra("count_id", count_id);
        intent.putExtra("SName", widget.count.name);
        intent.putExtra("SCode", widget.count.code);
        intent.putExtra("date", getcurDate());
        intent.putExtra("time", getcurTime());
        intent.putExtra("indivAtt", iAtt);
        startActivity(intent);
    }

    public void countUpLHli(View view) {
        int iAtt = 5;
        int count_id = Integer.parseInt(view.getTag().toString());
        CountingWidgetLH widget = getCountFromIdLH(count_id);
        if (widget != null)
            widget.countUpLHli();

        disableProximitySensor();

        assert Objects.requireNonNull(widget).count != null;

        // get edited info for individual and start EditIndividualActivity
        Intent intent = new Intent(CountingActivity.this, EditIndividualActivity.class);
        intent.putExtra("count_id", count_id);
        intent.putExtra("SName", widget.count.name);
        intent.putExtra("SCode", widget.count.code);
        intent.putExtra("date", getcurDate());
        intent.putExtra("time", getcurTime());
        intent.putExtra("indivAtt", iAtt);
        startActivity(intent);
    }

    // Triggered by count down button
    // deletes last count
    public void countDownli(View view) {
        soundButtonSoundMinus();
        buttonVib();

        int count_id = Integer.parseInt(view.getTag().toString());
        CountingWidget widget = getCountFromId(count_id);
        assert Objects.requireNonNull(widget).count != null;
        spec_name = widget.count.name; // set spec_name for toast in deleteIndividual
        specCnt = widget.count.count_li;
        if (specCnt > 0) {
            widget.countDownli();
            countDataSource.saveCountli(count);

            i_Id = individualsDataSource.getLastIndiv(count_id, 5);
            if (i_Id == -1) {
                mesg = getString(R.string.getHelp) + spec_name;
                Toast.makeText(this,
                        HtmlCompat.fromHtml("<font color='red'><b>" + mesg + "</b></font>",
                                HtmlCompat.FROM_HTML_MODE_LEGACY), Toast.LENGTH_LONG).show();
                return;
            }
            int icount = individualsDataSource.getIndividualCount(i_Id);
            if (i_Id > 0 && icount < 2) {
                deleteIndividual(i_Id);
                i_Id--;
                return;
            }
            if (i_Id > 0) {
                int icount1 = icount - 1;
                individualsDataSource.decreaseIndividual(i_Id, icount1);
            }
        }
    }

    // Triggered by count down button from left-hand view
    // deletes last count
    public void countDownLHli(View view) {
        soundButtonSoundMinus();
        buttonVib();

        int count_id = Integer.parseInt(view.getTag().toString());
        CountingWidgetLH widget = getCountFromIdLH(count_id);
        assert Objects.requireNonNull(widget).count != null;
        spec_name = widget.count.name; // set spec_name for toast in deleteIndividual
        specCnt = widget.count.count_li;
        if (specCnt > 0) {
            widget.countDownLHli();
            countDataSource.saveCountli(count);

            i_Id = individualsDataSource.getLastIndiv(count_id, 5);
            if (i_Id == -1) {
                mesg = getString(R.string.getHelp) + spec_name;
                Toast.makeText(this,
                        HtmlCompat.fromHtml("<font color='red'><b>" + mesg + "</b></font>",
                                HtmlCompat.FROM_HTML_MODE_LEGACY), Toast.LENGTH_LONG).show();
                return;
            }
            int icount = individualsDataSource.getIndividualCount(i_Id);
            if (i_Id > 0 && icount < 2) {
                deleteIndividual(i_Id);
                i_Id--;
                return;
            }
            if (i_Id > 0) {
                int icount1 = icount - 1;
                individualsDataSource.decreaseIndividual(i_Id, icount1);
            }
        }
    }

    public void countUpei(View view) {
        // iAtt used by EditIndividualActivity to decide where to store bulk count value
        int iAtt = 6; // 1 f1i, 2 f2i, 3 f3i, 4 pi, 5 li, 6 ei

        int count_id = Integer.parseInt(view.getTag().toString());
        CountingWidget widget = getCountFromId(count_id);
        if (widget != null)
            widget.countUpei();

        disableProximitySensor();

        assert Objects.requireNonNull(widget).count != null;

        // get edited info for individual and start EditIndividualActivity
        Intent intent = new Intent(CountingActivity.this, EditIndividualActivity.class);
        intent.putExtra("count_id", count_id);
        intent.putExtra("SName", widget.count.name);
        intent.putExtra("SCode", widget.count.code);
        intent.putExtra("date", getcurDate());
        intent.putExtra("time", getcurTime());
        intent.putExtra("indivAtt", iAtt);
        startActivity(intent);
    }

    public void countUpLHei(View view) {
        int iAtt = 6;
        int count_id = Integer.parseInt(view.getTag().toString());
        CountingWidgetLH widget = getCountFromIdLH(count_id);
        if (widget != null) {
            widget.countUpLHei();
        }

        disableProximitySensor();

        assert Objects.requireNonNull(widget).count != null;

        // get edited info for individual and start EditIndividualActivity
        Intent intent = new Intent(CountingActivity.this, EditIndividualActivity.class);
        intent.putExtra("count_id", count_id);
        intent.putExtra("SName", widget.count.name);
        intent.putExtra("SCode", widget.count.code);
        intent.putExtra("date", getcurDate());
        intent.putExtra("time", getcurTime());
        intent.putExtra("indivAtt", iAtt);
        startActivity(intent);
    }

    // Triggered by count down button
    // deletes last count
    public void countDownei(View view) {
        soundButtonSoundMinus();
        buttonVib();

        int count_id = Integer.parseInt(view.getTag().toString());
        CountingWidget widget = getCountFromId(count_id);
        assert Objects.requireNonNull(widget).count != null;
        spec_name = widget.count.name; // set spec_name for toast in deleteIndividual
        specCnt = widget.count.count_ei;
        if (specCnt > 0) {
            widget.countDownei();
            countDataSource.saveCountei(count);

            i_Id = individualsDataSource.getLastIndiv(count_id, 6);
            if (i_Id == -1) {
                mesg = getString(R.string.getHelp) + spec_name;
                Toast.makeText(this,
                        HtmlCompat.fromHtml("<font color='red'><b>" + mesg + "</b></font>",
                                HtmlCompat.FROM_HTML_MODE_LEGACY), Toast.LENGTH_LONG).show();
                return;
            }
            int icount = individualsDataSource.getIndividualCount(i_Id);
            if (i_Id > 0 && icount < 2) {
                deleteIndividual(i_Id);
                i_Id--;
                return;
            }
            if (i_Id > 0) {
                int icount1 = icount - 1;
                individualsDataSource.decreaseIndividual(i_Id, icount1);
            }
        }
    }

    // Triggered by count down button from left-hand view
    // deletes last count
    public void countDownLHei(View view) {
        soundButtonSoundMinus();
        buttonVib();

        int count_id = Integer.parseInt(view.getTag().toString());
        CountingWidgetLH widget = getCountFromIdLH(count_id);
        assert Objects.requireNonNull(widget).count != null;
        spec_name = widget.count.name; // set spec_name for toast in deleteIndividual
        specCnt = widget.count.count_ei;
        if (specCnt > 0) {
            widget.countDownLHei();
            countDataSource.saveCountei(count);

            i_Id = individualsDataSource.getLastIndiv(count_id, 6);
            if (i_Id == -1) {
                mesg = getString(R.string.getHelp) + spec_name;
                Toast.makeText(this,
                        HtmlCompat.fromHtml("<font color='red'><b>" + mesg + "</b></font>",
                                HtmlCompat.FROM_HTML_MODE_LEGACY), Toast.LENGTH_LONG).show();
                return;
            }
            int icount = individualsDataSource.getIndividualCount(i_Id);
            if (i_Id > 0 && icount < 2) {
                deleteIndividual(i_Id);
                i_Id--;
                return;
            }
            if (i_Id > 0) {
                int icount1 = icount - 1;
                individualsDataSource.decreaseIndividual(i_Id, icount1);
            }
        }
    }
    // End of counters

    // Delete individual for count_id
    private void deleteIndividual(int id) {
        System.out.println(getString(R.string.indivdel) + " " + id);
        individualsDataSource.deleteIndividualById(id);
    }

    // Date for date_stamp
    @SuppressLint("SimpleDateFormat")
    private String getcurDate() {
        Date date = new Date();
        DateFormat dform;
        String lng = Locale.getDefault().toString().substring(0, 2);

        if (lng.equals("de"))
            dform = new SimpleDateFormat("dd.MM.yyyy");
        else
            dform = new SimpleDateFormat("yyyy-MM-dd");
        return dform.format(date);
    }

    // Date for time_stamp
    private String getcurTime() {
        Date date = new Date();
        @SuppressLint("SimpleDateFormat")
        DateFormat dform = new SimpleDateFormat("HH:mm:ss");
        return dform.format(date);
    }

    private void soundButtonSoundMinus() {
        if (buttonSoundPref) {
            try {
                Uri notification;
                if (isNotBlank(buttonSoundMinus) && buttonSoundMinus != null)
                    notification = Uri.parse(buttonSoundMinus);
                else
                    notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                r = RingtoneManager.getRingtone(this, notification);
                r.play();
                mHandler.postDelayed(r::stop, 420);
            } catch (Exception e) {
                if (MyDebug.DLOG) Log.e(TAG, "1575, could not play button sound.", e);
            }
        }
    }

    private void buttonVib() {
        if (buttonVibPref && vibrator.hasVibrator()) {
            if (SDK_INT >= 31) // S, Android 12
            {
                vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK));
            } else {
                if (SDK_INT >= 26) {
                    vibrator.vibrate(VibrationEffect.createOneShot(450,
                            VibrationEffect.DEFAULT_AMPLITUDE));
                    vibrator.cancel();
                }
            }
        }
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
    private static boolean isNotBlank(final CharSequence cs) {
        return !isBlank(cs);
    }

    private static boolean isBlank(final CharSequence cs) {
        int strLen;
        if (cs == null || (strLen = cs.length()) == 0)
            return true;

        for (int i = 0; i < strLen; i++) {
            if (!Character.isWhitespace(cs.charAt(i)))
                return false;
        }
        return true;
    }

}
