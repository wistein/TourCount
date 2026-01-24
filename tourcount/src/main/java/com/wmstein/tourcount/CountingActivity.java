package com.wmstein.tourcount;

import static com.wmstein.tourcount.Utils.fromHtml;

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
import android.media.MediaPlayer;
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
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;

import com.wmstein.egm.EarthGravitationalModel;
import com.wmstein.tourcount.database.Count;
import com.wmstein.tourcount.database.CountDataSource;
import com.wmstein.tourcount.database.IndividualsDataSource;
import com.wmstein.tourcount.database.Section;
import com.wmstein.tourcount.database.SectionDataSource;
import com.wmstein.tourcount.widgets.CountingWidget;
import com.wmstein.tourcount.widgets.CountingWidgetHead1;
import com.wmstein.tourcount.widgets.CountingWidgetSpeciesNotes;
import com.wmstein.tourcount.widgets.CountingWidgetLH;
import com.wmstein.tourcount.widgets.CountingWidgetTourNotes;

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
 * starts EditSpeciesListActivity, switches screen off when device is pocketed
 * and allows taking pictures and sending notes.
 <p>
 * CountingActivity uses CountingWidget.kt, CountingWidgetLH.kt, NotesWidget.kt,
 * activity_counting.xml and activity_counting_lh.xml
 <p>
 * Basic counting functions created by milo for BeeCount on 2014-05-05.
 * Adopted, modified and enhanced for TourCount by wmstein since 2016-04-18,
 * last edited in Java on 2026-01-15
 */
public class CountingActivity
        extends AppCompatActivity
        implements SharedPreferences.OnSharedPreferenceChangeListener, SensorEventListener {
    private static final String TAG = "CountAct";

    private int iid = 1;
    private LinearLayout counting_screen;
    private LinearLayout count_area;

    private LinearLayout head_area2;
    private LinearLayout tour_notes_area;
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
    private String uncertainty;
    LocationService locationService;
    private boolean locServiceOn = false;

    // Proximity sensor handling screen on/off
    private PowerManager powerManager;
    private SensorManager sensorManager;
    private PowerManager.WakeLock proximityWakeLock;
    private Sensor proximitySensor;

    // Preferences
    private SharedPreferences prefs;
    private SharedPreferences.Editor editor;
    private boolean awakePref;
    private boolean brightPref;
    private String sortPref;
    private boolean lhandPref;       // true for left hand mode of counting screen
    private boolean buttonSoundPref;
    private String buttonSoundMinus; // (+)-buttonSound is handled in EditIndividualActivity
    private boolean buttonVibPref;
    private String specCode = "";
    private String proxSensorPref;
    private double sensorSensitivity = 0.0;
    private boolean metaPref;        // option for OSM reverse geocoding
    private String emailString = ""; // mail address for OSM query

    // Data sources
    private SectionDataSource sectionDataSource;
    private CountDataSource countDataSource;
    private IndividualsDataSource individualsDataSource;

    private Context audioAttributionContext;
    private MediaPlayer rToneM;

    // Prepare vibrator service
    private Vibrator vibrator;

    private String mesg = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "156, onCreate");

        audioAttributionContext = (Build.VERSION.SDK_INT >= 30) ?
                createAttributionContext("ringSound") :
                this;

        TourCountApplication tourCount = (TourCountApplication) getApplication();
        prefs = TourCountApplication.getPrefs();
        setPrefVariables(); // set all stored preferences into their variables

        sectionDataSource = new SectionDataSource(this);
        countDataSource = new CountDataSource(this);
        individualsDataSource = new IndividualsDataSource(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) // SDK 35+
            EdgeToEdge.enable(this);

        // Distinguish between left-/ right-handed counting page layout
        if (lhandPref) {
            setContentView(R.layout.activity_counting_lh);
            counting_screen = findViewById(R.id.countingScreenLH);
            counting_screen.setBackground(tourCount.setBackgr());
            count_area = findViewById(R.id.countCountiLayoutLH);
            tour_notes_area = findViewById(R.id.tourNotesLayoutLH);
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
            counting_screen = findViewById(R.id.countingScreen);
            counting_screen.setBackground(tourCount.setBackgr());
            count_area = findViewById(R.id.countCountiLayout);
            tour_notes_area = findViewById(R.id.tourNotesLayout);
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
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);

        powerManager = (PowerManager) this.getSystemService(Context.POWER_SERVICE);
        if (powerManager.isWakeLockLevelSupported(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK)) {
            proximityWakeLock = powerManager.newWakeLock(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK,
                    "TourCount:WAKELOCK");
        }
        else {
            proximityWakeLock = null;
        }

        // Get sensitivity range of proximity sensor
        double sensorSensitivityMax;
        if (proximitySensor != null)
            sensorSensitivityMax = proximitySensor.getMaximumRange();
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

                    if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                        Log.i(TAG, "258, handleOnBackPressed");
                    finish();
                    remove();
                }
            };
            getOnBackPressedDispatcher().addCallback(this, callback);
        }
        locationService = new LocationService(getApplicationContext());
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
        awakePref = prefs.getBoolean("pref_awake", true);       // keep screen on while counting
        brightPref = prefs.getBoolean("pref_bright", true);     // bright counting page
        sortPref = prefs.getString("pref_sort_sp", "none");    // sort mode of species list
        lhandPref = prefs.getBoolean("pref_left_hand", false);  // left-handed counting page
        buttonSoundPref = prefs.getBoolean("pref_button_sound", false); // make button sound
        buttonVibPref = prefs.getBoolean("pref_button_vib", false); // make vibration
        buttonSoundMinus = prefs.getString("button_sound_minus", null); // use deeper button sound
        itemPosition = prefs.getInt("item_Position", 0);        // item position in spinner
        iid = prefs.getInt("count_id", 1);                      // species id
        proxSensorPref = prefs.getString("pref_prox", "Off");
        locServiceOn = prefs.getBoolean("loc_srv_on", false);
        emailString = prefs.getString("email_String", ""); // for reliable query of Nominatim service
        metaPref = prefs.getBoolean("pref_metadata", false); // use Reverse Geocoding
    }

    @SuppressLint("DiscouragedApi")
    @Override
    protected void onResume() {
        super.onResume();

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "303, onResume");

        sensorManager.registerListener(this, proximitySensor, SensorManager.SENSOR_DELAY_NORMAL);

        prefs = TourCountApplication.getPrefs();
        prefs.registerOnSharedPreferenceChangeListener(this);
        setPrefVariables(); // set prefs into their variables

        // Prepare vibrator service
        if (buttonVibPref)
            vibrator = getApplicationContext().getSystemService(Vibrator.class);

        // Get location with permissions check
        locationDispatcher(1);

        // Set full brightness of screen
        if (brightPref) {
            WindowManager.LayoutParams params = getWindow().getAttributes();
            params.screenBrightness = 1.0f;
            getWindow().setAttributes(params);
        }

        if (awakePref)
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Prepare button sounds
        if (buttonSoundPref) {
            Uri uriM;
            if (isNotBlank(buttonSoundMinus) && buttonSoundMinus != null)
                uriM = Uri.parse(buttonSoundMinus);
            else
                uriM = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

            if (rToneM == null)
                rToneM = MediaPlayer.create(audioAttributionContext, uriM);
        }

        // build the counting screen
        // clear any existing views
        count_area.removeAllViews();
        tour_notes_area.removeAllViews();
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
                    fromHtml("<font color='red'><b>" + mesg + "</b></font>"),
                    Toast.LENGTH_LONG).show();
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

        // Show head1: Species with spinner to select
        if (lhandPref) // if left-handed counting page
            spinner = findViewById(R.id.countHead1SpinnerLH);
        else
            spinner = findViewById(R.id.countHead1Spinner);

        // Get itemPosition of added species by specCode from sharedPreference
        if (!Objects.equals(prefs.getString("new_spec_code", ""), "")) {
            specCode = prefs.getString("new_spec_code", "");
            editor = prefs.edit();
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
    }
    // End of onResume()

    // Watch proximity sensor
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (proximitySensor != null) {
            if (event.sensor.getType() == Sensor.TYPE_PROXIMITY) {
                float sensi = event.values[0];
                if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                    Log.d(TAG, "462, Prox.Sensor Value0: " + sensi + ", " + "Sensitivity: "
                            + sensorSensitivity);

                // if ([0|5] >= [0|-2.5|-4.9] && [0|5] < [0|2.5|4.9])
                if (sensi >= -sensorSensitivity && sensi < sensorSensitivity) {
                    // near
                    if (proximityWakeLock == null)
                        proximityWakeLock = powerManager.newWakeLock(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK,
                                "TourCount:WAKELOCK");

                    if (!proximityWakeLock.isHeld())
                        proximityWakeLock.acquire(30 * 60 * 1000L); // 30 minutes
                } else {
                    // far
                    disableProximitySensor();
                }
            }
        }
    }

    // Necessary for SensorEventListener
    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
    }

    private void disableProximitySensor() // far
    {
        if (proximityWakeLock == null)
            return;
        if (proximityWakeLock.isHeld()) {
            int flags = PowerManager.RELEASE_FLAG_WAIT_FOR_NO_PROXIMITY;
            proximityWakeLock.release(flags);
            proximityWakeLock = null;
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
        } else if (id == R.id.menuAddSpecies) {
            disableProximitySensor();

            Intent intent = new Intent(CountingActivity.this,
                    AddSpeciesActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            return true;
        } else if (id == R.id.menuDelSpecies) {
            disableProximitySensor();

            mesg = getString(R.string.wait);
            Toast.makeText(this, // bright green
                    fromHtml("<font color='#008000'>" + mesg + "</font>"),
                    Toast.LENGTH_SHORT).show();

            Intent intent = new Intent(CountingActivity.this,
                    DelSpeciesActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

            // Trick: Pause for 100 msec to show toast
            mHandler.postDelayed(() ->
                    startActivity(intent), 100);
            return true;
        } else if (id == R.id.menuEditSection) {
            disableProximitySensor();

            mesg = getString(R.string.wait);
            Toast.makeText(this, // bright green
                    fromHtml("<font color='#008000'>" + mesg + "</font>"),
                    Toast.LENGTH_SHORT).show();

            Intent intent = new Intent(CountingActivity.this,
                    EditSpeciesListActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

            // Trick: Pause for 100 msec to show toast
            mHandler.postDelayed(() ->
                    startActivity(intent), 100);
            return true;
        } else if (id == R.id.menuTakePhoto) {
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
                                fromHtml("<font color='red'><b>" + mesg + "</b></font>"),
                                Toast.LENGTH_LONG).show();
                    }
                }
            } else {
                // Only default camera available
                startActivity(camIntent);
            }
            return true;
        } else if (id == R.id.action_share) {
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

    // Edit count options by EditSpeciesNotesActivity by button in widget_counting_species_notes.xml
    public void editOptions(View view) {
        Intent intent = new Intent(CountingActivity.this, EditSpeciesNotesActivity.class);
        intent.putExtra("count_id", iid);
        startActivity(intent);
    }

    // Edit count options by EditSpeciesNotesActivity by button in widget_counting_species_notes.xml
    public void editTourNotes(View view) {
        Intent intent = new Intent(CountingActivity.this, EditTourNotesActivity.class);
        intent.putExtra("count_id", iid);
        startActivity(intent);
    }

    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        setPrefVariables();
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "621, onPause");

        disableProximitySensor();

        // save current count id in case it is lost on pause
        editor = prefs.edit();
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
        locationDispatcher(2);

        prefs.unregisterOnSharedPreferenceChangeListener(this);
        sensorManager.unregisterListener(this);
    }
    // End of onPause()

    @Override
    public void onStop() {
        super.onStop();

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "655, onStop");

        counting_screen.invalidate();

        if (buttonSoundPref) {
            if (rToneM != null) {
                if (rToneM.isPlaying()) {
                    rToneM.stop(); // stop media player
                }
                rToneM.release();
                rToneM = null;
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "675, onDestroy");
    }

    // Control location service
    // locationDispatcherMode:
    //  1 = start and use location service
    //  2 = end location service
    public void locationDispatcher(int locationDispatcherMode) {
        if (isFineLocationPermGranted()) {
            editor = prefs.edit();
            switch (locationDispatcherMode) {
                case 1 -> // start location service and get location
                {
                    if (!locServiceOn) {
                        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                            Log.i(TAG, "690 locationDispatcher 1");
                        Intent sIntent = new Intent(getApplicationContext(), LocationService.class);
                        startService(sIntent);
                        locServiceOn = true;

                        editor.putBoolean("loc_srv_on", true);
                        editor.commit();
                    }
                    getLoc();
                }
                case 2 -> // stop location service
                {
                    if (locServiceOn) {
                        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                            Log.i(TAG, "704, locationDispatcher 2");
                        locationService.stopListener();
                        Intent sIntent = new Intent(getApplicationContext(), LocationService.class);
                        stopService(sIntent);
                        locServiceOn = false;

                        editor.putBoolean("loc_srv_on", false);
                        editor.commit();
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
    @SuppressLint("DefaultLocale")
    public void getLoc() {
        if (locationService.canGetLocation()) {
            longitude = locationService.getLongitude();
            latitude = locationService.getLatitude();
            height = locationService.getAltitude();
            if (height != 0.0)
                height = correctHeight(latitude, longitude, height);
            uncertainty = String.format("%f", locationService.getAccuracy());
        }

        // Get reverse geocoding and store data to Section and Temp table
        if (metaPref && latitude != 0) {
            String urlString;
            if (Objects.equals(emailString, "")) {
                urlString = "https://nominatim.openstreetmap.org/reverse?" +
                        "email=test@temp.test" + "format=xml&lat="
                        + latitude + "&lon=" + longitude + "&zoom=18&addressdetails=1";
            } else {
                urlString = "https://nominatim.openstreetmap.org/reverse?" +
                        "email=" + emailString + "&format=xml&lat="
                        + latitude + "&lon=" + longitude + "&zoom=18&addressdetails=1";
            }
            WorkRequest retrieveAddrWorkRequest =
                    new OneTimeWorkRequest.Builder(RetrieveAddrWorker.class)
                            .setInputData(new Data.Builder()
                                    .putString("URL_STRING", urlString)
                                    .build()
                            )
                            .build();
            WorkManager.getInstance(this).enqueue(retrieveAddrWorkRequest);
        }
    }

    // Correct height with geoid offset from simplified EarthGravitationalModel
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
                    tour_notes_area.removeAllViews();

                    String sid = ((TextView) view.findViewById(R.id.countId)).getText().toString();
                    iid = Integer.parseInt(sid); // get species id
                    itemPosition = position;

                    count = countDataSource.getCountById(iid);
                    countingScreen(count);
                    if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                        Log.d(TAG, "798, SpinnerListener, count id: "
                                + count.id + ", code: " + count.code + ", name: " + count.name);
                } catch (Exception e) {
                    // Exception may occur when permissions are changed while activity is paused
                    //  or when spinner is rapidly repeatedly pressed
                    if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                        Log.e(TAG, "804, SpinnerListener: " + e);
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
        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "818, countingScreen");

        // 1. Species line with Spinner is set by CountingWidgetHead1 in onResume

        // 2. counts
        if (lhandPref) // if left-handed counting page
        {
            CountingWidgetLH widgetc = new CountingWidgetLH(this, null);
            widgetc.setCount(count);
            countingWidgetsLH.add(widgetc);
            count_area.addView(widgetc);
        } else {
            CountingWidget widgetc = new CountingWidget(this, null);
            widgetc.setCount(count);
            countingWidgets.add(widgetc);
            count_area.addView(widgetc);
        }

        // 3. Species notes with edit button
        CountingWidgetSpeciesNotes widgets = new CountingWidgetSpeciesNotes(this, null);
        widgets.setCountHead2(count);
        head_area2.addView(widgets);

        // 4. Tour notes with edit button
        CountingWidgetTourNotes widgett = new CountingWidgetTourNotes(this, null);
        widgett.setTourNotes(section);
        tour_notes_area.addView(widgett);
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

        locationDispatcher(1); // update location
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
        intent.putExtra("cLatitude", latitude);
        intent.putExtra("cLongitude", longitude);
        intent.putExtra("cHeight", height);
        intent.putExtra("cUncert", uncertainty);
        startActivity(intent);
    }

    public void countUpLHf1i(View view) {
        int iAtt = 1;
        int count_id = Integer.parseInt(view.getTag().toString());
        CountingWidgetLH widget = getCountFromIdLH(count_id);
        if (widget != null)
            widget.countUpLHf1i();

        locationDispatcher(1); // update location
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
        intent.putExtra("cLatitude", latitude);
        intent.putExtra("cLongitude", longitude);
        intent.putExtra("cHeight", height);
        intent.putExtra("cUncert", uncertainty);
        startActivity(intent);
    }

    // Triggered by count down button
    // deletes last count
    public void countDownf1i(View view) {
        soundMinusButtonSound();
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
                        fromHtml("<font color='red'><b>" + mesg + "</b></font>"),
                        Toast.LENGTH_LONG).show();
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
        soundMinusButtonSound();
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
                        fromHtml("<font color='red'><b>" + mesg + "</b></font>"),
                        Toast.LENGTH_LONG).show();
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

        locationDispatcher(1); // update location
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
        intent.putExtra("cLatitude", latitude);
        intent.putExtra("cLongitude", longitude);
        intent.putExtra("cHeight", height);
        intent.putExtra("cUncert", uncertainty);
        startActivity(intent);
    }

    public void countUpLHf2i(View view) {
        int iAtt = 2;
        int count_id = Integer.parseInt(view.getTag().toString());
        CountingWidgetLH widget = getCountFromIdLH(count_id);
        if (widget != null)
            widget.countUpLHf2i();

        locationDispatcher(1); // update location
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
        intent.putExtra("cLatitude", latitude);
        intent.putExtra("cLongitude", longitude);
        intent.putExtra("cHeight", height);
        intent.putExtra("cUncert", uncertainty);
        startActivity(intent);
    }

    // Triggered by count down button
    // deletes last count
    public void countDownf2i(View view) {
        soundMinusButtonSound();
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
                        fromHtml("<font color='red'><b>" + mesg + "</b></font>"),
                        Toast.LENGTH_LONG).show();
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
        soundMinusButtonSound();
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
                        fromHtml("<font color='red'><b>" + mesg + "</b></font>"),
                        Toast.LENGTH_LONG).show();
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

        locationDispatcher(1); // update location
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
        intent.putExtra("cLatitude", latitude);
        intent.putExtra("cLongitude", longitude);
        intent.putExtra("cHeight", height);
        intent.putExtra("cUncert", uncertainty);
        startActivity(intent);
    }

    public void countUpLHf3i(View view) {
        int iAtt = 3;
        int count_id = Integer.parseInt(view.getTag().toString());
        CountingWidgetLH widget = getCountFromIdLH(count_id);
        if (widget != null)
            widget.countUpLHf3i();

        locationDispatcher(1); // update location
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
        intent.putExtra("cLatitude", latitude);
        intent.putExtra("cLongitude", longitude);
        intent.putExtra("cHeight", height);
        intent.putExtra("cUncert", uncertainty);
        startActivity(intent);
    }

    // Triggered by count down button
    // deletes last count
    public void countDownf3i(View view) {
        soundMinusButtonSound();
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
                        fromHtml("<font color='red'><b>" + mesg + "</b></font>"),
                        Toast.LENGTH_LONG).show();
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
        soundMinusButtonSound();
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
                        fromHtml("<font color='red'><b>" + mesg + "</b></font>"),
                        Toast.LENGTH_LONG).show();
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

        locationDispatcher(1); // update location
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
        intent.putExtra("cLatitude", latitude);
        intent.putExtra("cLongitude", longitude);
        intent.putExtra("cHeight", height);
        intent.putExtra("cUncert", uncertainty);
        startActivity(intent);
    }

    public void countUpLHpi(View view) {
        int iAtt = 4;
        int count_id = Integer.parseInt(view.getTag().toString());
        CountingWidgetLH widget = getCountFromIdLH(count_id);
        if (widget != null)
            widget.countUpLHpi();

        locationDispatcher(1); // update location
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
        intent.putExtra("cLatitude", latitude);
        intent.putExtra("cLongitude", longitude);
        intent.putExtra("cHeight", height);
        intent.putExtra("cUncert", uncertainty);
        startActivity(intent);
    }

    // Triggered by count down button
    // deletes last count
    public void countDownpi(View view) {
        soundMinusButtonSound();
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
                        fromHtml("<font color='red'><b>" + mesg + "</b></font>"),
                        Toast.LENGTH_LONG).show();
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
        soundMinusButtonSound();
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
                        fromHtml("<font color='red'><b>" + mesg + "</b></font>"),
                        Toast.LENGTH_LONG).show();
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

        locationDispatcher(1); // update location
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
        intent.putExtra("cLatitude", latitude);
        intent.putExtra("cLongitude", longitude);
        intent.putExtra("cHeight", height);
        intent.putExtra("cUncert", uncertainty);
        startActivity(intent);
    }

    public void countUpLHli(View view) {
        int iAtt = 5;
        int count_id = Integer.parseInt(view.getTag().toString());
        CountingWidgetLH widget = getCountFromIdLH(count_id);
        if (widget != null)
            widget.countUpLHli();

        locationDispatcher(1); // update location
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
        intent.putExtra("cLatitude", latitude);
        intent.putExtra("cLongitude", longitude);
        intent.putExtra("cHeight", height);
        intent.putExtra("cUncert", uncertainty);
        startActivity(intent);
    }

    // Triggered by count down button
    // deletes last count
    public void countDownli(View view) {
        soundMinusButtonSound();
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
                        fromHtml("<font color='red'><b>" + mesg + "</b></font>"),
                        Toast.LENGTH_LONG).show();
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
        soundMinusButtonSound();
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
                        fromHtml("<font color='red'><b>" + mesg + "</b></font>"),
                        Toast.LENGTH_LONG).show();
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

        locationDispatcher(1); // update location
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
        intent.putExtra("cLatitude", latitude);
        intent.putExtra("cLongitude", longitude);
        intent.putExtra("cHeight", height);
        intent.putExtra("cUncert", uncertainty);
        startActivity(intent);
    }

    public void countUpLHei(View view) {
        int iAtt = 6;
        int count_id = Integer.parseInt(view.getTag().toString());
        CountingWidgetLH widget = getCountFromIdLH(count_id);
        if (widget != null) {
            widget.countUpLHei();
        }

        locationDispatcher(1); // update location
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
        intent.putExtra("cLatitude", latitude);
        intent.putExtra("cLongitude", longitude);
        intent.putExtra("cHeight", height);
        intent.putExtra("cUncert", uncertainty);
        startActivity(intent);
    }

    // Triggered by count down button
    // deletes last count
    public void countDownei(View view) {
        soundMinusButtonSound();
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
                        fromHtml("<font color='red'><b>" + mesg + "</b></font>"),
                        Toast.LENGTH_LONG).show();
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
        soundMinusButtonSound();
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
                        fromHtml("<font color='red'><b>" + mesg + "</b></font>"),
                        Toast.LENGTH_LONG).show();
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
        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "1651, " + getString(R.string.indivDel) + " " + id);
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

    private void soundMinusButtonSound() {
        if (buttonSoundPref) {
            if (rToneM.isPlaying()) {
                rToneM.stop();
                rToneM.release();
            }
            rToneM.start();
        }
    }

    private void buttonVib() {
        if (buttonVibPref) {
            if (Build.VERSION.SDK_INT >= 31) // S, Android 12
            {
                vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK));
            } else {
                if (Build.VERSION.SDK_INT >= 26) {
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
     * @return true if the CharSequence is
     * not empty and not null and not whitespace
     * <p>
     * Derived from package org.apache.commons.lang3/StringUtils
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
