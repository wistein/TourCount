package com.wmstein.tourcount

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.graphics.Color
import android.graphics.Typeface
import android.hardware.Sensor
import android.hardware.SensorManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.Vibrator
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import android.widget.TextView
import android.widget.Toast

import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.view.MenuCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams

import com.google.android.material.snackbar.Snackbar
import com.wmstein.changelog.ChangeLog
import com.wmstein.filechooser.AdvFileChooser
import com.wmstein.tourcount.PermissionsLocationDialogFragment.Companion.newInstance
import com.wmstein.tourcount.TCLifecycleHandler.Companion.isApplicationVisible
import com.wmstein.tourcount.TourCountApplication.Companion.getPrefs
import com.wmstein.tourcount.Utils.fromHtml
import com.wmstein.tourcount.database.CountDataSource
import com.wmstein.tourcount.database.DbHelper
import com.wmstein.tourcount.database.Head
import com.wmstein.tourcount.database.HeadDataSource
import com.wmstein.tourcount.database.Section
import com.wmstein.tourcount.database.SectionDataSource

import org.dhatim.fastexcel.BorderSide
import org.dhatim.fastexcel.PaperSize
import org.dhatim.fastexcel.Position
import org.dhatim.fastexcel.Workbook

import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.FileReader
import java.io.FileWriter
import java.io.IOException
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Objects

import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlin.system.exitProcess

/***************************************************************************************************
 * WelcomeActivity provides the starting page with menu and buttons for
 * import/export/help/info methods and lets you call
 * EditMetaActivity, CountingActivity and ShowResultsActivity.
 * It uses further LocationService and PermissionLocationDialogFragment.
 *
 * Database handling is mainly controlled in WelcomeActivity as e.g. upgrade
 * to current DB version when importing an older DB file by importDBFile().
 *
 * Based on BeeCount's WelcomeActivity.java by milo on 05/05/2014.
 * Changes and additions for TourCount by wmstein since 2016-04-18,
 * last edited in Java on 2026-07-18,
 * converted to Kotlin on 2026-07-19,
 * last edited on 2026-08-02.
 */
class WelcomeActivity : AppCompatActivity(), OnSharedPreferenceChangeListener {
    private var tourCount: TourCountApplication? = null
    private var baseLayout: View? = null
    private var cl: ChangeLog? = null

    // Services
    var locationService: LocationService? = null
    private var locServiceOn = false // Initial location service state (WelcomeActivity only)
    private lateinit var locIntent: Intent
    var addrRequestService: AddrRequestService? = null
    var adrIntent: Intent? = null
    var soundService: SoundService? = null
    private var sndServiceOn = false // Initial sound service state (WelcomeActivity only)
    var sndIntent: Intent? = null

    // Import/export stuff
    private var inFile: File? = null
    private var outFile: File? = null
    private var localLanguage = ""
    private var mExternalStorageWriteable = false
    private val storageState = Environment.getExternalStorageState()
    private val mHandler = Handler(Looper.getMainLooper())

    // File handling with plausi check
    private val regexFilename = "[^a-zA-Z_0-9äöüÄÖÜ-]"
    private var tourName = "" // The tour name as shown
    private var tourNameForDir = "" // The tour name as part of a filename

    // Preferences
    private var prefs = getPrefs()
    private var editor = prefs.edit()
    private var outPref = "" // Output sorting
    private var buttonSoundPref = false
    private var alertSoundPref = false
    private var dataLanguage = ""
    private var metaPref = false

    // Permissions
    private var storagePermGranted = false // Storage permission state
    var stoIntent: Intent? = null
    private var fineLocationPermGranted = false // Foreground location permission state

    // DB handling, data sources
    private var database: SQLiteDatabase? = null
    private var dbHelper: DbHelper? = null
    private var sectionDataSource: SectionDataSource? = null
    private var section: Section? = null
    private var headDataSource: HeadDataSource? = null
    private var countDataSource: CountDataSource? = null

    // Other stuff
    private var mesg = ""
    private lateinit var alert: AlertDialog
    private var doubleBackToExitPressedTwice = false

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "167, onCreate")

        tourCount = application as TourCountApplication

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        // Initialize sound service
        buttonSoundPref = prefs.getBoolean("pref_button_sound", false) // Prepare SoundService
        alertSoundPref = prefs.getBoolean("pref_alert_sound", false)

        if (buttonSoundPref) {
            soundService = SoundService(applicationContext)
            sndIntent = Intent(applicationContext, SoundService::class.java)
            startService(sndIntent)
            sndServiceOn = true
        }

        // Proximity sensor handling in preferences menu
        val sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        val proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)

        // Gray out preferences menu item pref_prox when max. proximity sensitivity = null
        val prefProx = proximitySensor != null // true if proximity sensor is available

        // Gray out preferences menu item pref_button_vib when device has no vibrator
        val vibrator = applicationContext.getSystemService(Vibrator::class.java)
        val prefVib = vibrator.hasVibrator() // true if vibrator is available

        // Set pref_prox and pref_button_vib enabler, used in SettingsFragment
        editor.putBoolean("enable_prox", prefProx)
        editor.putBoolean("enable_vib", prefVib)
        editor.apply()

        // Set DarkMode when system is in BrightMode
        val nightModeFlags = Configuration.UI_MODE_NIGHT_MASK
        val confUi = getResources().configuration.uiMode
        if ((nightModeFlags and confUi) == Configuration.UI_MODE_NIGHT_NO) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        }

        // Use EdgeToEdge mode for Android 15+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM)  // SDK 35+
        {
            this.enableEdgeToEdge()
        }

        setContentView(R.layout.activity_welcome)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.baseLayout))
        { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updateLayoutParams<MarginLayoutParams> {
                topMargin = insets.top
                leftMargin = insets.left
                bottomMargin = insets.bottom
                rightMargin = insets.right
            }
            WindowInsetsCompat.CONSUMED
        }

        cl = ChangeLog(this, prefs)

        // Show changelog for new version
        if (cl!!.firstRun())
            Objects.requireNonNull<AlertDialog>(cl!!.logDialog).show()

        // Check initial storage permission state and if not granted provide dialog
        storagePermGranted = isStoragePermGranted()
        if (!storagePermGranted)  // in self permission
        {
            // Provide dialog for storage permission request with explanation
            requestAllFilesAccessPermission(1)

            // Prepare to ask foreground location permission only once
            editor.putBoolean("has_asked_foreground", false)
            editor.commit()
        }

        // Check DB version and upgrade if necessary
        dbHelper = DbHelper(this)
        database = dbHelper!!.writableDatabase // Make DB upgrade if necessary
        dbHelper!!.close()

        // Set up the data sources
        headDataSource = HeadDataSource(this)
        sectionDataSource = SectionDataSource(this)
        countDataSource = CountDataSource(this)

        // Get tour name and check for DB integrity
        try {
            sectionDataSource!!.open()
            section = sectionDataSource!!.section
            tourName = section!!.name
            sectionDataSource!!.close()
        } catch (_: SQLiteException) {
            sectionDataSource!!.close()

            mesg = getString(R.string.corruptDb)
            Toast.makeText(this,
                fromHtml("<font color='red'><b>$mesg</b></font>"),
                Toast.LENGTH_LONG
            ).show()

            mHandler.postDelayed({ this.finishAndRemoveTask() }, 2000)
        }

        // Prepare tourName to be part of a filename
        tourNameForDir = tourName
        if (tourNameForDir != "") tourNameForDir = tourNameForDir.replace(regexFilename.toRegex(), "")

        storagePermGranted = isStoragePermGranted()
        if (storagePermGranted) {
            // Test for existence of directory /storage/emulated/0/Documents/TourCount
            var path = Environment.getExternalStorageDirectory()
            path = File("$path/Documents/TourCount")

            // Delete an old existing tourcount0.db if it does exist
            inFile = File(path, "/tourcount0.db") // old initial basic DB
            if (inFile!!.exists())
                inFile!!.delete()

            // Create directory TourCount and write initial tourcount0_ll.db file
            localLanguage = Locale.getDefault().toString().substring(0, 2)
            inFile = File(path, "/tourcount0_$localLanguage.db") // new initial basic DB
            if (!inFile!!.exists()) {
                // Call with parameter 0: short name, local language and don't show a message
                exportBasisDb(0)
            }
        }

        // Option for use of Nominatim service for locality data
        metaPref = prefs.getBoolean("pref_metadata", false) // use Reverse Geocoding
        if (metaPref) {
            TourCountApplication.adrServiceOn = false // will be set true by addressDispatcher
            addressDispatcher(1) // Start AddrRequestService
        }

        // New onBackPressed logic
        // Different Navigation Bar modes and layouts:
        // - Classic three-button navigation: NavBarMode = 0
        // - Two-button navigation (Android P): NavBarMode = 1
        // - Full screen gesture mode (Android Q): NavBarMode = 2
        // Use onBackPressed logic only if NavBarMode = 0 or 1.
        if (this.navBarMode == 0 || this.navBarMode == 1) {
            val callback = this.onBackPressedCallback
            onBackPressedDispatcher.addCallback(this, callback)
        }
    }
    // End of onCreate()

    // Check storage self permission
    private fun isStoragePermGranted(): Boolean {
        // Check permission MANAGE_EXTERNAL_STORAGE for Android >= 11
        val storageGranted: Boolean = Environment.isExternalStorageManager()

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG,"323, ManageStoragePermission: $storageGranted")

        return storageGranted
    }

    // Request All-Files-Access Permission
    fun requestAllFilesAccessPermission(m: Int) {
        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG,"331, requestAllFilesAccessPermission")

        if (m == 1) mesg = getString(R.string.dialog_storage_message1)
        else if (m == 2) mesg = getString(R.string.dialog_storage_message2)

        // Provide dialog for storage permission request
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.dialog_storage_title))
            .setMessage(mesg)
            .setPositiveButton(
                getString(R.string.ok_button)
            ) { _: DialogInterface?, _: Int ->
                stoIntent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                val packageUri = Uri.fromParts("package", packageName, null)
                stoIntent!!.data = packageUri
                TourCountApplication.isStorPermReq = true
                startActivity(stoIntent)
            }
            .setNegativeButton(
                getString(R.string.cancelButton)
            ) { _: DialogInterface?, _: Int ->
                showSnackbarRed(getString(R.string.storage_perm_denied))
            }
            .create().show()
    }

    val navBarMode: Int
        // Check for Navigation bar (1-, 2- or 3-button mode)
        get() {
            val resources = this.getResources()

            @SuppressLint("DiscouragedApi") val resourceId = resources.getIdentifier(
                "config_navBarInteractionMode",
                "integer", "android"
            )

            // navBarMode = 0: 3-button, = 1: 2-button, = 2: gesture
            return if (resourceId > 0) resources.getInteger(resourceId) else 0
        }

    // Use onBackPressed logic for button navigation
    private val onBackPressedCallback: OnBackPressedCallback
        get() {
            val m1Handler = Handler(Looper.getMainLooper())
            val r1 = Runnable { doubleBackToExitPressedTwice = false }

            return object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (doubleBackToExitPressedTwice) {
                        m1Handler.removeCallbacks(r1)

                        // Stop button sound (SoundService)
                        if (sndServiceOn) {
                            soundService!!.releaseSoundM()
                            soundService!!.releaseSoundP()

                            stopService(sndIntent)
                            sndServiceOn = false
                        }

                        // Stop AddrRequestService
                        if (metaPref) addressDispatcher(2)

                        finish()
                        remove()
                    } else {
                        doubleBackToExitPressedTwice = true

                        mesg = getString(R.string.back_twice) // within 1.5 sec
                        Toast.makeText(applicationContext,
                            fromHtml("<font color='blue'>$mesg</font>"),
                            Toast.LENGTH_SHORT
                        ).show()
                        m1Handler.postDelayed(r1, 1500)
                    }
                }
            }
        }

    @SuppressLint("SourceLockedOrientationActivity", "ApplySharedPref")
    override fun onResume() {
        super.onResume()

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "415, onResume")

        prefs = getPrefs()
        prefs.registerOnSharedPreferenceChangeListener(this)
        editor = prefs.edit()

        outPref = prefs.getString("pref_sort_output", "names")!! // sort mode csv-export
        // Mail address for reliable query of Nominatim service
        val emailString: String = prefs.getString("email_String", "")!!
        // Option for use of Nominatim service for locality data
        metaPref = prefs.getBoolean("pref_metadata", false) // use Reverse Geocoding
        dataLanguage = prefs.getString("pref_sel_data_lang", "")!!
        if (dataLanguage.isEmpty()) dataLanguage = "--"

        // Set storagePermGranted from self permission
        storagePermGranted = isStoragePermGranted()

        if (TourCountApplication.isFirstStart && metaPref) {
            // This is to remind a missing email address for Nominatim Reverse Geocoder.
            if (emailString == "") {
                mesg = getString(R.string.missingEmail)
                Toast.makeText(this,  // orange
                    fromHtml("<font color='#ff6000'>$mesg</font>"),
                    Toast.LENGTH_SHORT
                ).show()}
            TourCountApplication.isFirstStart = false
        }

        sectionDataSource!!.open()
        countDataSource!!.open()

        baseLayout = findViewById(R.id.baseLayout)
        baseLayout!!.background = tourCount!!.setBackgr()

        // Set tour name as title from DB table section
        section = sectionDataSource!!.section
        tourName = section!!.name
        supportActionBar!!.title = tourName

        // Prepare modified tourName to be part of a filename
        tourNameForDir = tourName
        if (tourNameForDir != "") tourNameForDir = tourNameForDir.replace(regexFilename.toRegex(), "")

        // Location permissions handling:
        isFineLocationPermGranted() // Get flag fineLocationPermGranted from self permissions

        // If location permission is not yet granted prepare and query for it
        if (storagePermGranted && !fineLocationPermGranted) {
            // Get flag 'has_asked_foreground' for foreground location permission
            val hasAskedForegroundLocation = prefs.getBoolean("has_asked_foreground", false)

            if (!hasAskedForegroundLocation) {
                // Query foreground location permission first and set flag 'has_asked_foreground'
                // Ask necessary fine location permission with info in PermissionsLocationDialog
                newInstance().show(
                    supportFragmentManager,PermissionsLocationDialogFragment::class.java.name)

                editor.putBoolean("has_asked_foreground", true)
                editor.commit()
            }
        }

        // Get new location self permission state
        isFineLocationPermGranted() // set fineLocationPermGranted from self permission

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG,"481, onResume, fineLocationPermGranted: $fineLocationPermGranted")

        // Start Location Service and try to read location
        if (fineLocationPermGranted) {
            // Start location service and get 1. location
            locationDispatcher(1)
        }
    }
    // End of onResume()

    // Check initial fine location permission
    private fun isFineLocationPermGranted() {
        fineLocationPermGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    // Control location service by locationDispatcherMode:
    //  1 = start location service and periodic location requests
    //  2 = end location service when app is invisible
    private fun locationDispatcher(locationDispatcherMode: Int) {
        if (fineLocationPermGranted) {
            when (locationDispatcherMode) {
                1 -> {
                    // Get location data
                    if (!locServiceOn) {
                        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                            Log.i(TAG,"509, locationDispatcher 1")

                        locationService = LocationService(applicationContext)
                        locIntent = Intent(applicationContext, LocationService::class.java)
                        startService(locIntent)
                        locServiceOn = true
                    }

                    this.loc // Get position
                }

                2 -> {
                    // Stop location service
                    if (locServiceOn) {
                        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                            Log.i(TAG,"524, locationDispatcher 2")

                        locationService!!.stopListener()
                        locIntent = Intent(applicationContext, LocationService::class.java)
                        stopService(locIntent)
                        locServiceOn = false
                    }
                }
            }
        }
    }

    @get:SuppressLint("DefaultLocale")
    val loc: Unit
        // Get the location data and store values in global variables
        get() {
            if (locationService!!.canGetLocation()) {
                locationService!!.getLongitude()
                locationService!!.getLatitude()
                locationService!!.getAltitude()
                locationService!!.getAccuracy()
            }
        }

    // Control AddrRequestService
    private fun addressDispatcher(addrDispatcherMode: Int) {
        when (addrDispatcherMode) {
            1 -> {
                // Get address data
                if (!TourCountApplication.adrServiceOn) {
                    if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                        Log.i(TAG,"555, addressDispatcher 1")

                    TourCountApplication.adrServiceOn = true
                    addrRequestService = AddrRequestService()
                    adrIntent = Intent(applicationContext, AddrRequestService::class.java)
                    startService(adrIntent)
                    addrRequestService!!.stopTimerTask()
                }
            }

            2 -> {
                // Stop AddrRequestService
                if (TourCountApplication.adrServiceOn) {
                    if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                        Log.i(TAG,"569, addressDispatcher 2")

                    addrRequestService!!.releaseSoundA()
                    addrRequestService!!.stopTimerTask()
                    adrIntent = Intent(applicationContext, AddrRequestService::class.java)
                    stopService(adrIntent)
                    TourCountApplication.adrServiceOn = false
                }
            }
        }
    }

    // Show the action bar menu with present items.
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.welcome, menu)
        MenuCompat.setGroupDividerEnabled(menu, true) // Show dividers in menu
        return true
    }

    // Handle clicks on the action bar and its menu items here
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val intent: Intent?
        when (item.itemId) {
            R.id.action_settings -> {
                // Call SettingsActivity
                startActivity(
                    Intent(this, SettingsActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                )
                return true
            }
            R.id.exportBasisMenu -> {
                // Call exportBasisDb()
                if (storagePermGranted) {
                    exportBasisDb(2) // 2: show message + long file name
                } else {
                    requestAllFilesAccessPermission(2) // message with "... Then try again."
                }
                return true
            }
            R.id.exportMenu -> {
                // Call exportDb()
                if (storagePermGranted) {
                    exportDb()
                } else {
                    requestAllFilesAccessPermission(2)
                }
                return true
            }
            R.id.exportXLSXMenu -> {
                // Call exportDb2XLSX()
                if (storagePermGranted) {
                    exportDb2XLSX()
                } else {
                    requestAllFilesAccessPermission(2)
                }
                return true
            }
            R.id.exportCSVMenu -> {
                // Call exportDb2CSV()
                if (storagePermGranted) {
                    exportDb2CSV()
                } else {
                    requestAllFilesAccessPermission(2)
                }
                return true
            }
            R.id.exportSpeciesListMenu -> {
                // Call exportSpeciesList()
                if (storagePermGranted) {
                    exportSpeciesList()
                } else {
                    requestAllFilesAccessPermission(2)
                }
                return true
            }
            R.id.importBasisMenu -> {
                // Call importBasisDb()
                if (storagePermGranted) {
                    importBasisDb()
                } else {
                    requestAllFilesAccessPermission(2)
                }
                return true
            }
            R.id.importFileMenu -> {
                // Call  importDBFile()
                if (storagePermGranted) {
                    importDBFile()
                } else {
                    requestAllFilesAccessPermission(2)
                }
                return true
            }
            R.id.importSpeciesListMenu -> {
                // Call importSpeciesList()
                if (storagePermGranted) {
                    importSpeciesList()
                } else {
                    requestAllFilesAccessPermission(2)
                }
                return true
            }
            R.id.resetDBMenu -> {
                // Call resetToBasisDb()
                resetToBasisDb()
                return true
            }
            R.id.viewHelp -> {
                // Call ShowTextDialog with help text
                intent = Intent(this@WelcomeActivity, ShowTextDialog::class.java)
                intent.putExtra("dialog", "help")
                startActivity(intent)
                return true
            }
            R.id.changeLog -> {
                // Call ChangeLog
                Objects.requireNonNull<AlertDialog>(cl!!.fullLogDialog).show()
                return true
            }
            R.id.viewLicense -> {
                // Call ShowTextDialog with license text
                intent = Intent(this@WelcomeActivity, ShowTextDialog::class.java)
                intent.putExtra("dialog", "license")
                startActivity(intent)
                return true
            }
            R.id.editMeta -> {
                // Call EditMetaActivity
                intent = Intent(this@WelcomeActivity, EditMetaActivity::class.java)
                // Wait for 500 msec to get results from AddrRequestService
                mHandler.postDelayed(
                    { startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)) },
                    500
                )
                return true
            }
            R.id.startCounting -> {
                // Call CountingActivity
                intent = Intent(this@WelcomeActivity, CountingActivity::class.java)
                startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
                return true
            }
            R.id.showResults -> {
                // Call ShowResultsActivity
                mesg = getString(R.string.wait)
                Toast.makeText(this,
                    fromHtml("<font color='blue'>$mesg</font>"),
                    Toast.LENGTH_SHORT
                ).show()

                // Trick: Pause for 100 msec to show toast
                mHandler.postDelayed({
                    startActivity(
                        Intent(applicationContext,ShowResultsActivity::class.java
                        ).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    )
                }, 100)
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }
    // End of onOptionsItemSelected

    override fun onSharedPreferenceChanged(prefs: SharedPreferences, key: String?) {
        baseLayout = findViewById(R.id.baseLayout)
        baseLayout!!.background = tourCount!!.setBackgr()
        outPref = prefs.getString("pref_sort_output", "names")!!
        buttonSoundPref = prefs.getBoolean("pref_button_sound", false)
        alertSoundPref = prefs.getBoolean("pref_alert_sound", false)
        metaPref = prefs.getBoolean("pref_metadata", false)

        // Handle sound service when changed in settings
        if (!buttonSoundPref && sndServiceOn) {
            stopService(sndIntent)
            sndServiceOn = false
        }

        if (buttonSoundPref && !sndServiceOn) {
            startService(sndIntent)
            sndServiceOn = true
        }

        if (!alertSoundPref && fineLocationPermGranted && TourCountApplication.adrServiceOn) {
            addrRequestService!!.stopSoundA()
        }
    }

    public override fun onPause() {
        super.onPause()

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "762, onPause")

        countDataSource!!.close()
        sectionDataSource!!.close()

        prefs.unregisterOnSharedPreferenceChangeListener(this)
    }

    public override fun onStop() {
        super.onStop()

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "774, onStop")

        baseLayout!!.invalidate()

        // Stop Services when app should finish
        if (!isApplicationVisible) {
            if (TourCountApplication.adrServiceOn) {
                addrRequestService!!.releaseSoundA()
            }

            if (sndServiceOn) {
                soundService!!.releaseSoundM()
                soundService!!.releaseSoundP()
                stopService(sndIntent)
                sndServiceOn = false
            }

            locationDispatcher(2)
            TourCountApplication.lat = 0.0 // prohibits calls to Nominatim service
            TourCountApplication.lon = 0.0

            if (TourCountApplication.adrServiceOn) addrRequestService!!.stopTimerTask()
            addressDispatcher(2)

            if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                Log.i(TAG, ("799, onStop, app not visible, running services Loc, Snd, Adr: "
                        + locServiceOn + ", " + sndServiceOn + ", " + TourCountApplication.adrServiceOn))

            finishAndRemoveTask()
        }
    }

    public override fun onDestroy() {
        super.onDestroy()

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "810, onDestroy")

        exitProcess(0)
    }

    // Handle button click "Counting" here
    fun startCounting(view: View?) {
        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "818, startCounting, View: $view")

        val intent = Intent(this@WelcomeActivity, CountingActivity::class.java)
        startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
    }

    // Handle button click "Prepare Inspection" here
    fun editMeta(view: View?) {
        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "827, editMeta, View: $view")

        val intent = Intent(this@WelcomeActivity, EditMetaActivity::class.java)
        // Wait for 500 msec to get results from AddrRequestService
        mHandler.postDelayed(
            { startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
            },500)
    }

    // Start ShowResultsActivity (by button)
    fun showResults(view: View?) {
        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "839, showResults, View: $view")

        mesg = getString(R.string.wait)
        Toast.makeText(this,
            fromHtml("<font color='blue'>$mesg</font>"),
            Toast.LENGTH_SHORT
        ).show()

        // Trick: Pause for 100 msec to show toast
        mHandler.postDelayed(
            { startActivity(Intent(applicationContext,
                ShowResultsActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
            }, 100)
    }

    /***********************************************************************************************
     * The next three functions below are for importing data files.
     * They've been put here because no database should be open at this point.
     */
    // Import the basic DB
    private fun importBasisDb() {
        val fileExtension = ".db"
        val fileNameStart = "tourcount0"
        val fileHd = getString(R.string.fileHeadlineBasicDB)

        val intent = Intent(this, AdvFileChooser::class.java)
        intent.putExtra("filterFileExtension", fileExtension)
        intent.putExtra("filterFileNameStart", fileNameStart)
        intent.putExtra("fileHd", fileHd)
        myActivityResultLauncher.launch(intent)
    }
    // End of part 1 of importBasisDb()

    // Choose a tourcount db-file to load and set it to tourcount.db
    private fun importDBFile() {
        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "876, importDBFile")

        val fileExtension = ".db"
        val fileNameStart = "tourcount_"
        val fileHd = getString(R.string.fileHeadlineDB)

        val intent = Intent(this, AdvFileChooser::class.java)
        intent.putExtra("filterFileExtension", fileExtension)
        intent.putExtra("filterFileNameStart", fileNameStart)
        intent.putExtra("fileHd", fileHd)
        myActivityResultLauncher.launch(intent)
    }
    // End of part 1 of importDBFile()

    // myActivityResultLauncher is part2 of importBasisDb() and importDBFile()
    // and processes the result of AdvFileChooser
    @Suppress("UNCHECKED_CAST")
    val myActivityResultLauncher: ActivityResultLauncher<Intent?> =
        registerForActivityResult(
            StartActivityForResult() as ActivityResultContract<Intent?, ActivityResult?>
        ) { result ->
            var selectedFile = ""
            inFile = null

            if (result?.resultCode == RESULT_OK)  // has a file
            {
                val data = result.data
                if (data != null) {
                    selectedFile = data.getStringExtra("fileSelected")!!
                    inFile = if (selectedFile != "") File(selectedFile)
                    else null
                }
            } else {
                if ((result?.resultCode == RESULT_FIRST_USER)) {
                    mesg = getString(R.string.noFile)
                    Toast.makeText(applicationContext,  // orange
                        fromHtml("<font color='#ff6000'><b>$mesg</b></font>"),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

            if (inFile != null) {
                // outFile -> /data/data/com.wmstein.tourcount/databases/tourcount.db
                var destPath = applicationContext.filesDir.path
                destPath = destPath.substring(
                    0,
                    destPath.lastIndexOf("/")
                ) + "/databases/tourcount.db"
                outFile = File(destPath)

                val builder = AlertDialog.Builder(this@WelcomeActivity)
                builder.setIcon(android.R.drawable.ic_dialog_alert)
                builder.setMessage(R.string.confirmDBImport)
                builder.setCancelable(false)
                builder.setPositiveButton(
                    R.string.importButton
                ) { _: DialogInterface?, _: Int ->
                    try {
                        countDataSource!!.close()
                        sectionDataSource!!.close()

                        copy(inFile, outFile)

                        sectionDataSource!!.open()
                        countDataSource!!.open()
                        headDataSource!!.open()

                        val head = headDataSource!!.head
                        var headLanguage = head.datalanguage
                        headDataSource!!.close()
                        var hasDataLang = true

                        if (headLanguage == "") hasDataLang = false
                        else headLanguage = headLanguage.substring(0, 2)

                        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                            Log.i(TAG, "953, ImportFile, headLanguage: $headLanguage")

                        // Save values for initial count-id and itemposition
                        editor.putInt("count_id", 1)
                        editor.putInt("item_Position", 0)
                        editor.putString("pref_sel_data_lang", headLanguage)
                        editor.putBoolean("has_data_lang", hasDataLang) // data language setting
                        editor.apply()

                        // Set tour name as title from DB table section
                        section = sectionDataSource!!.section
                        tourName = section!!.name
                        supportActionBar!!.title = tourName

                        // Prepare new tourName to be part of a filename
                        tourNameForDir = tourName
                        if (tourNameForDir != "") tourNameForDir =
                            tourNameForDir.replace(regexFilename.toRegex(), "")

                        mesg = getString(R.string.importDB)
                        Toast.makeText(applicationContext,  // bright green
                            fromHtml("<font color='#008000'>$mesg</font>"),
                            Toast.LENGTH_SHORT
                        ).show()
                    } catch (_: IOException) {
                        sectionDataSource!!.open()
                        countDataSource!!.open()

                        mesg = getString(R.string.importFail)
                        Toast.makeText(applicationContext,
                            fromHtml("<font color='red'><b>$mesg</b></font>"),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
                builder.setNegativeButton(
                    R.string.cancelButton
                ) { dialog: DialogInterface?, _: Int -> dialog!!.cancel() }
                alert = builder.create()
                alert.show()
            }
        }
    // End of part2 of import of DB files

    // Select and import a species list (also from TransektCount file species_YYYY-MM-DD_hhmmss.csv)
    private fun importSpeciesList() {
        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "1000, importSpeciesList")

        val fileExtension = ".csv"
        val fileNameStart = "species_"
        val fileHd = getString(R.string.fileHeadlineCSV)

        val intent = Intent(this, AdvFileChooser::class.java)
        intent.putExtra("filterFileExtension", fileExtension)
        intent.putExtra("filterFileNameStart", fileNameStart)
        intent.putExtra("fileHd", fileHd)
        listActivityResultLauncher.launch(intent)
    }

    // listActivityResultLauncher processes the result of AdvFileChooser
    @Suppress("UNCHECKED_CAST")
    val listActivityResultLauncher: ActivityResultLauncher<Intent?> =
        registerForActivityResult(
            StartActivityForResult() as ActivityResultContract<Intent?, ActivityResult?>
        ) { result ->
            var selectedFile = ""
            inFile = null

            if (result?.resultCode == RESULT_OK) {
                val data = result.data
                if (data != null) {
                    selectedFile = data.getStringExtra("fileSelected")!!
                    inFile = if (selectedFile != "")
                        File(selectedFile)
                    else null
                }
            } else if ((result?.resultCode == RESULT_FIRST_USER)) {
                mesg = getString(R.string.noFile)
                Toast.makeText(applicationContext,  // orange
                    fromHtml("<font color='#ff6000'><b>$mesg</b></font>"),
                    Toast.LENGTH_LONG
                ).show()
            }

            if (inFile != null) {
                val csvLine: String
                var brError = false

                // Check for old version of species list
                try {
                    val br = BufferedReader(FileReader(inFile))
                    csvLine = br.readLine() // Read 1. line only
                    val specLine: Array<String?> =
                        csvLine.split(",".toRegex()).dropLastWhile { it.isEmpty() }
                            .toTypedArray()
                    mesg = if (specLine[0] == "nocode")
                        getString(R.string.confirmListImport)
                    else
                        getString(R.string.specsCommonLang) + "\n\n" + getString(R.string.confirmListImport)
                    br.close()
                } catch (_: Exception) {
                    mesg = getString(R.string.br_Error)
                    brError = true
                }

                val builder = AlertDialog.Builder(this@WelcomeActivity)
                builder.setIcon(android.R.drawable.ic_dialog_alert)
                builder.setMessage(mesg)
                if (brError) {
                    builder.setCancelable(true)
                    builder.setNegativeButton(
                        R.string.cancelButton
                    ) { dialog: DialogInterface?, _: Int -> dialog!!.cancel() }
                } else {
                    builder.setCancelable(false)
                    builder.setPositiveButton(
                        R.string.importButton
                    ) { _: DialogInterface?, _: Int ->
                        clearDBforImport()
                        readSpeciesCSV(inFile)
                    }
                    builder.setNegativeButton(
                        R.string.cancelButton
                    ) { dialog: DialogInterface?, _: Int -> dialog!!.cancel() }
                }
                alert = builder.create()
                alert.show()
            }
        }

    // Clear DB for import of external species list
    private fun clearDBforImport() {
        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "1087, clearDBforImport")

        dbHelper = DbHelper(this)
        database = dbHelper!!.writableDatabase

        var sql = "DELETE FROM " + DbHelper.COUNT_TABLE
        database!!.execSQL(sql)

        sql = "DELETE FROM " + DbHelper.INDIVIDUALS_TABLE
        database!!.execSQL(sql)

        dbHelper!!.close()

        TourCountApplication.lat = 0.0
        TourCountApplication.lon = 0.0
        TourCountApplication.heightNN = 0.0
        TourCountApplication.uncertainty = 0.0
        TourCountApplication.tLocality = ""
        TourCountApplication.isFirstLocality = true

        editor.putInt("item_Position", 0)
        editor.putInt("count_id", 1)
        editor.apply()

        // Restart Location Service and try to read location
        if (fineLocationPermGranted) {
            // Start location service and get 1. location
            locationDispatcher(1) // Start LocationService
        }
    }

    // Read an exported species list and write items to table counts
    private fun readSpeciesCSV(inFile: File?) {
        try {
            mesg = getString(R.string.waitImport)
            Toast.makeText(this,
                fromHtml("<font color='blue'>$mesg</font>"),
                Toast.LENGTH_SHORT
            ).show()

            var csvLine: String
            val codeArray: MutableList<String?> = ArrayList()
            val nameArray: MutableList<String?> = ArrayList()
            val nameGArray: MutableList<String?> = ArrayList()

            var br = BufferedReader(FileReader(inFile))
            var hasDataLang = true

            editor = prefs.edit()
            csvLine = br.readLine() // Read 1. line only
            var specLine: Array<String?> =
                csvLine.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            if (specLine[0] == "nocode") {
                dataLanguage = specLine[2]!!
                editor.putString("pref_sel_data_lang", dataLanguage)
            } else {
                hasDataLang = false
                editor.putString("pref_sel_data_lang", "--")
            }

            editor.putBoolean("has_data_lang", hasDataLang) // controls data language setting
            editor.apply()

            br.close()

            br = BufferedReader(FileReader(inFile))

            var i = 0 // index of imported list
            var iCounts = 1 // index of id in table counts

            while ((br.readLine().also { csvLine = it }) != null)  // for each csvLine
            {
                specLine =
                    csvLine.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

                // 1. line fields contain String[0]: "nocode", [1]: "language", [2]: "de"|"en"|"fr"|"it"|"es"
                if (specLine[0] == "nocode") {
                    if (hasDataLang) {
                        iCounts--
                        i--
                    }
                } else {
                    // comma-separated 0:code, 1:name, 2:nameL
                    codeArray.add(i, specLine[0])
                    nameArray.add(i, specLine[1])
                    nameGArray.add(i, specLine[2])
                    countDataSource!!.writeCountItem(
                        iCounts.toString(), codeArray[i]!!,
                        nameArray[i]!!, nameGArray[i]!!
                    )
                }
                i++
                iCounts++
            }
            br.close()

            mesg = getString(R.string.importList)
            Toast.makeText(this,  // bright green
                fromHtml("<font color='#008000'>$mesg</font>"),
                Toast.LENGTH_SHORT
            ).show()
        } catch (_: Exception) {
            mesg = getString(R.string.importListFail)
            Toast.makeText(this,
                fromHtml("<font color='red'><b>$mesg</b></font>"),
                Toast.LENGTH_LONG
            ).show()
        }
    }
    // End of importSpeciesList()

    /***********************************************************************************************
     * The next four functions below are for exporting data files.
     * They've been put here because no database should be open at this point.
     */
    // Exports Basic DB to Documents/TourCount/tourcount0_name.db
    // hasNoName indicated initial creation of tourcount0.db if it does not exist
    private fun exportBasisDb(i: Int) {
        // i = 0: don't show a message, use local language and short name
        // i = 2: show message and use data language and long name
        // inFile <- /data/data/com.wmstein.tourcount/databases/tourcount.db
        var inPath = applicationContext.filesDir.path
        inPath = inPath.substring(0, inPath.lastIndexOf("/")) + "/databases/tourcount.db"
        inFile = File(inPath)

        // tmpFile -> /data/data/com.wmstein.tourcount/files/tourcount_tmp.db
        var tmpPath = applicationContext.filesDir.path
        tmpPath = tmpPath.substring(0, tmpPath.lastIndexOf("/")) + "/files/tourcount_tmp.db"
        val tmpFile = File(tmpPath)

        // outFile in Public Directory Documents/TourCount/
        // distinguish versions (as getExternalStoragePublicDirectory is deprecated in Q, Android 10)
        var path = Environment.getExternalStorageDirectory()
        path = File("$path/Documents/TourCount")

        path.mkdirs() // just verify path, result ignored

        if (i == 0) outFile = File(path, "/tourcount0_$localLanguage.db")
        else if (i > 0) {
            dataLanguage = prefs.getString("pref_sel_data_lang", "")!!
            if (dataLanguage.isEmpty()) dataLanguage = "--"

            outFile = if (tourNameForDir == "") File(path, "/tourcount0_$dataLanguage.db")
            else File(path, "/tourcount0_$dataLanguage" + "_$tourNameForDir.db")
        }

        // Check if we can write the media
        mExternalStorageWriteable = Environment.MEDIA_MOUNTED == storageState

        if (!mExternalStorageWriteable) {
            mesg = getString(R.string.noCard)
            Toast.makeText(
                this,
                fromHtml("<font color='red'><b>$mesg</b></font>"),
                Toast.LENGTH_LONG
            ).show()
        } else {
            // Export the basic db
            try {
                // Save current db as backup db tmpFile
                copy(inFile, tmpFile)

                // Clear DB and location values for basic DB
                val resOK = clearDBValues()

                // Write Basic DB
                if (resOK) {
                    copy(inFile, outFile)
                    // Restore actual db from tmpFile
                    copy(tmpFile, inFile)
                }

                // Delete backup db
                val d0 = tmpFile.delete()

                // Show message success
                if (d0 && i == 2) {
                    mesg = getString(R.string.saveBasisDB)
                    Toast.makeText(
                        this,  // bright green
                        fromHtml("<font color='#008000'>$mesg</font>"),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (_: IOException) {
                mesg = getString(R.string.saveFail)
                Toast.makeText(
                    this,
                    fromHtml("<font color='red'><b>$mesg</b></font>"),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
    // End of exportBasisDb()

    @SuppressLint("SdCardPath", "LongLogTag")
    private fun exportDb() {
        // Public data directory for outFile: Documents/TourCount/
        var path = Environment.getExternalStorageDirectory()
        path = File("$path/Documents/TourCount")

        // Open Section table for date and start_tm
        section = sectionDataSource!!.section
        val date = section!!.date
        val startTM = section!!.start_tm

        var engl = false
        var hasDate = true
        var dbDate: String
        val dbTime: String
        val dbDateEN: String
        val dbDateEU: String

        // Get year from date
        if (date != "") {
            // "-" at position 4 of the date means EN
            if (date.substring(4, 5) == "-") engl = true
            // Create date for filename as YYYYMMDD from any format
            dbDateEU = date.substring(6, 10) + date.substring(3, 5) + date.substring(0, 2)
            dbDateEN = date.substring(0, 4) + date.substring(5, 7) + date.substring(8, 10)
        } else {
            dbDateEU = ""
            dbDateEN = ""
            hasDate = false
        }

        dbDate = if (engl) dbDateEN
        else dbDateEU

        if (startTM != "") {
            dbTime = startTM.substring(0, 2) + startTM.substring(3, 5)

            if (hasDate) dbDate = "$dbDate-$dbTime" // yyyymmdd-hhmm
        } else dbDate = "" // has only a value when both date and start time are given

        path.mkdirs() // Just verify path, result ignored

        dataLanguage = prefs.getString("pref_sel_data_lang", "")!!
        if (dataLanguage.isEmpty()) dataLanguage = "--"

        // outFile -> /storage/emulated/0/Documents/TourCount/tourcount_yyyyMMdd_HHmm.db
        outFile = if (tourNameForDir == "" && dbDate == "")
            File(path, "/tourcount_" + dataLanguage + "_" + getcurDate() + ".db")
        else if (tourNameForDir == "")
            File(path, "/tourcount_" + dataLanguage + "_" + dbDate + ".db")
        else if (dbDate == "")
            File(path,
            "/tourcount_" + dataLanguage + "_" + getcurDate() + "_" + tourNameForDir + ".db")
        else File(path,
            "/tourcount_" + dataLanguage + "_" + dbDate + "_" + tourNameForDir + ".db")

        // inFile <- /data/data/com.wmstein.tourcount/databases/tourcount.db
        var inPath = applicationContext.filesDir.path
        inPath = (inPath.substring(0, inPath.lastIndexOf("/")) + "/databases/tourcount.db")
        inFile = File(inPath)

        // Check if we can write the media
        mExternalStorageWriteable = Environment.MEDIA_MOUNTED == storageState

        if (!mExternalStorageWriteable) {
            mesg = getString(R.string.noCard)
            Toast.makeText(
                this,
                fromHtml("<font color='red'><b>$mesg</b></font>"),
                Toast.LENGTH_LONG
            ).show()
        } else {
            // Export the db
            try {
                copy(inFile, outFile)

                mesg = getString(R.string.saveDB)
                Toast.makeText(
                    this,
                    fromHtml("<font color='blue'>$mesg</font>"),
                    Toast.LENGTH_SHORT
                ).show()
            } catch (_: IOException) {
                mesg = getString(R.string.saveFail)
                Toast.makeText(
                    this,
                    fromHtml("<font color='red'><b>$mesg</b></font>"),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
    // End of exportDb()

    /***********************************************************************************************
     * Exports the DB contents as Tour_dL_tourname_yyyyMMdd_HHmm_si.csv to
     * Documents/TourCount/ with purged data set.
     * Spreadsheet programs can import this csv file with
     *  - Unicode UTF-8 filter,
     *  - comma delimiter and
     *  - "" for text recognition.
     */
    private fun exportDb2CSV() {
        // Public data directory for outFile: Documents/TourCount/
        var path = Environment.getExternalStorageDirectory()
        path = File("$path/Documents/TourCount")

        section = sectionDataSource!!.section
        val temps: Int = section!!.tmp
        val tempe: Int = section!!.tmp_end
        val winds: Int = section!!.wind
        val winde: Int = section!!.wind_end
        val clouds: Int = section!!.clouds
        val cloude: Int = section!!.clouds_end

        // Set environment data
        val date = section!!.date
        val startTM = section!!.start_tm
        val endTM = section!!.end_tm

        var engl = false
        var hasDate = true
        var csvDate: String?
        val csvTime: String?
        val csvDateEU: String?
        val csvDateEN: String?

        // Get year from date
        if (date != "") {
            // "-" at position 4 of the date means EN
            if (date.substring(4, 5) == "-") engl = true
            // Create date for filename as YYYYMMDD from any format
            csvDateEU = date.substring(6, 10) + date.substring(3, 5) + date.substring(0, 2)
            csvDateEN = date.substring(0, 4) + date.substring(5, 7) + date.substring(8, 10)
        } else {
            hasDate = false
            csvDateEN = ""
            csvDateEU = ""
        }

        csvDate = if (engl) {
            csvDateEN
        } else {
            csvDateEU
        }

        if (startTM != "") {
            csvTime = startTM.substring(0, 2) + startTM.substring(3, 5)

            if (hasDate) csvDate = "$csvDate-$csvTime" // yyyymmdd-hhmm
        } else csvDate = "" // has only a value when both date and start time are given

        path.mkdirs()
        // Just verify path, result ignored
        val sortIdent = if (outPref == "names") {
            "n"
        } else {
            "c"
        }

        dataLanguage = prefs.getString("pref_sel_data_lang", "")!!
        if (dataLanguage.isEmpty()) dataLanguage = "--"

        outFile = if (tourNameForDir == "" && csvDate == "")
            File(path,
            "/Tour_" + dataLanguage + "_" + getcurDate() + "_" + sortIdent + ".csv")
        else if (tourNameForDir == "")
            File(path,
            "/Tour_" + dataLanguage + "_" + csvDate + "_" + sortIdent + ".csv")
        else if (csvDate == "")
            File(path,
            "/Tour_" + dataLanguage + "_" + getcurDate() + "_" + tourNameForDir + "_" + sortIdent + ".csv")
        else File(path,
            "/Tour_" + dataLanguage + "_" + csvDate + "_" + tourNameForDir + "_" + sortIdent + ".csv")

        val sectName: String
        val sectNotes: String

        val head: Head?

        // Prepare metadata
        val inspecName: String // Inspector's name
        val country: String
        val bState: String
        val plz: String
        val city: String
        val place: String
        val locality: String
        var spstate: Int
        var spstate0: String
        var longi: Double
        var lati: Double
        var heigh: Double
        var uncer: Double
        val lo: Double
        val la: Double
        var loMin = 0.0
        var loMax = 0.0
        var laMin = 0.0
        var laMax = 0.0
        var uc: Double
        var uncer1 = 0.0

        // Prepare counts data
        var frst: Int
        var sum = 0
        var summf = 0
        var summ = 0
        var sumf = 0
        var sump = 0
        var suml = 0
        var sume = 0
        var sumMF = ""
        var sumM = ""
        var sumF = ""
        var sumP = ""
        var sumL = ""
        var sumE = ""

        // Check if we can write the media
        mExternalStorageWriteable = Environment.MEDIA_MOUNTED == storageState

        if (!mExternalStorageWriteable) {
            mesg = getString(R.string.noCard)
            Toast.makeText(
                this,
                fromHtml("<font color='red'><b>$mesg</b></font>"),
                Toast.LENGTH_LONG
            ).show()
        } else {
            // Get sorting mode of species list
            val sortMode = if (outPref == "names") {
                getString(R.string.sort_names)
            } else {
                getString(R.string.sort_codes)
            }

            // Export the purged count table to csv
            try {
                // Export purged db as csv
                val csvWrite = CSVWriter(FileWriter(outFile))

                // Consult Section on Head tables for head and meta info
                section = sectionDataSource!!.section

                sectName = "\"" + section!!.name + "\""
                sectNotes = "\"" + section!!.notes + "\""

                if (metaPref) {
                    country = "\"" + section!!.country + "\""
                    bState = "\"" + section!!.b_state + "\""
                    plz = "\"" + section!!.plz + "\""
                    city = "\"" + section!!.city + "\""
                    place = "\"" + section!!.place + "\""
                    locality = "\"" + section!!.st_locality + "\""
                } else {
                    country = getString(R.string.not_available)
                    bState = getString(R.string.not_available)
                    plz = getString(R.string.not_available)
                    city = getString(R.string.not_available)
                    place = getString(R.string.not_available)
                    locality = getString(R.string.not_available)
                }

                headDataSource!!.open()
                head = headDataSource!!.head
                inspecName = "\"" + head.observer + "\""
                headDataSource!!.close()

                val arrHead =
                    arrayOf<String?>(
                        getString(R.string.zList) + ": ",  // Count List:
                        sectName,  // Section name
                        "",
                        getString(R.string.inspector) + ": ",  // Inspector:
                        inspecName,  // Inspector name
                        "", "", "", "",
                        sortMode
                    )
                csvWrite.writeNext(arrHead)

                // 2nd row
                val arrRow2 =
                    arrayOf<String?>(
                        "", "", "", "", "", "", "", "", "",
                        getString(R.string.sort_time)
                    )
                csvWrite.writeNext(arrRow2)

                // Set location headline
                val arrLocHead =
                    arrayOf<String?>(
                        getString(R.string.country),
                        getString(R.string.bstate),
                        getString(R.string.plz),
                        getString(R.string.city),
                        getString(R.string.place),
                        getString(R.string.slocality),
                        getString(R.string.zlNotes)
                    )
                csvWrite.writeNext(arrLocHead)

                // Set location dataline with data of 1. location
                val arrLocation =
                    arrayOf<String?>(
                        country,
                        bState,
                        plz,
                        city,
                        place,
                        locality,
                        sectNotes
                    )
                csvWrite.writeNext(arrLocation)

                // Empty row
                val arrEmpt = arrayOf<String?>()
                csvWrite.writeNext(arrEmpt)

                // Set environment headline
                val arrEnvHead =
                    arrayOf<String?>(
                        getString(R.string.date),
                        "",
                        getString(R.string.tm),
                        getString(R.string.temperature),
                        getString(R.string.wind),
                        getString(R.string.clouds)
                    )
                csvWrite.writeNext(arrEnvHead)


                // Write environment data
                val arrEnvironment =
                    arrayOf<String?>(
                        "\"" + date + "\"",
                        getString(R.string.starttm),
                        "\"" + startTM + "\"",
                        temps.toString(),
                        winds.toString(),
                        clouds.toString()
                    )
                csvWrite.writeNext(arrEnvironment)

                // Write environment data
                val arrEnvironment2 =
                    arrayOf<String?>(
                        "",
                        getString(R.string.endtm),
                        "\"" + endTM + "\"",
                        tempe.toString(),
                        winde.toString(),
                        cloude.toString()
                    )
                csvWrite.writeNext(arrEnvironment2)

                // Empty row
                csvWrite.writeNext(arrEmpt)

                val nameSpecG = Utils.nameSpecG(dataLanguage)

                // Write counts headline
                //    Species Name, Local Name, Code, Counts, Spec.-Notes
                val arrCntHead =
                    arrayOf<String?>(
                        getString(R.string.name_spec),
                        nameSpecG,
                        getString(R.string.speccode),
                        getString(R.string.cntsmf),
                        getString(R.string.cntsm),
                        getString(R.string.cntsf),
                        getString(R.string.cntsp),
                        getString(R.string.cntsl),
                        getString(R.string.cntse),
                        getString(R.string.bema)
                    )
                csvWrite.writeNext(arrCntHead)

                // Write counts data
                dbHelper = DbHelper(this)
                database = dbHelper!!.writableDatabase

                val curCSVCnt: Cursor? // Cursor for Counts table

                // Sort mode species list
                if (outPref == "names") {
                    curCSVCnt = database!!.rawQuery(
                        ("select * from " + DbHelper.COUNT_TABLE
                                + " WHERE " + " ("
                                + DbHelper.C_NOTES + " = '0' or "
                                + DbHelper.C_COUNT_F1I + " > 0 or " + DbHelper.C_COUNT_F2I + " > 0 or "
                                + DbHelper.C_COUNT_F3I + " > 0 or " + DbHelper.C_COUNT_PI + " > 0 or "
                                + DbHelper.C_COUNT_LI + " > 0 or " + DbHelper.C_COUNT_EI + " > 0)"
                                + " order by " + DbHelper.C_NAME), null, null
                    )
                } else {
                    curCSVCnt = database!!.rawQuery(
                        ("select * from " + DbHelper.COUNT_TABLE
                                + " WHERE " + " ("
                                + DbHelper.C_NOTES + " = '0' or "
                                + DbHelper.C_COUNT_F1I + " > 0 or " + DbHelper.C_COUNT_F2I + " > 0 or "
                                + DbHelper.C_COUNT_F3I + " > 0 or " + DbHelper.C_COUNT_PI + " > 0 or "
                                + DbHelper.C_COUNT_LI + " > 0 or " + DbHelper.C_COUNT_EI + " > 0)"
                                + " order by " + DbHelper.C_CODE), null, null
                    )
                }

                // Get the number of individuals with attributes
                var cnts: Int // individuals icount
                var strcnts: String
                var cntsmf: Int // Imago male or female
                var strcntsmf: String
                var cntsm = 0 // Imago male
                var strcntsm: String
                var cntsf = 0 // Imago female
                var strcntsf: String
                var cntsp = 0 // Pupa
                var strcntsp: String
                var cntsl = 0 // Caterpillar
                var strcntsl: String
                var cntse = 0 // Egg
                var strcntse: String
                val male = "m"
                val fmale = "f"
                val stadium1 = getString(R.string.stadium_1)
                val stadium2 = getString(R.string.stadium_2)
                val stadium3 = getString(R.string.stadium_3)
                val stadium4 = getString(R.string.stadium_4)

                var spname: String
                var spcode: String

                var curCSVInd: Cursor? // Cursor for Individuals table
                while (curCSVCnt.moveToNext()) {
                    spname = curCSVCnt.getString(7) // species name from count table
                    spcode = "\"" + curCSVCnt.getString(8) + "\"" // species code from count table
                    val slct = ("SELECT * FROM " + DbHelper.INDIVIDUALS_TABLE + " WHERE "
                            + DbHelper.I_NAME + " = ? AND "
                            + DbHelper.I_SEX + " = ? AND "
                            + DbHelper.I_STADIUM + " = ?")

                    // Select male
                    curCSVInd = database!!.rawQuery(slct, arrayOf<String?>(spname, male, stadium1))
                    while (curCSVInd.moveToNext()) {
                        cnts = curCSVInd.getInt(14) // individuals icount
                        cntsm += cnts
                    }
                    curCSVInd.close()

                    // Select female
                    curCSVInd = database!!.rawQuery(slct, arrayOf<String?>(spname, fmale, stadium1))
                    while (curCSVInd.moveToNext()) {
                        cnts = curCSVInd.getInt(14) // individuals icount
                        cntsf += cnts
                    }
                    curCSVInd.close()

                    val slct1 = ("SELECT * FROM " + DbHelper.INDIVIDUALS_TABLE
                            + " WHERE " + DbHelper.I_NAME + " = ? AND " + DbHelper.I_STADIUM + " = ?")

                    // Select pupa
                    curCSVInd = database!!.rawQuery(slct1, arrayOf<String?>(spname, stadium2))
                    while (curCSVInd.moveToNext()) {
                        cnts = curCSVInd.getInt(14) // individuals icount
                        cntsp += cnts
                    }
                    curCSVInd.close()

                    // Select caterpillar
                    curCSVInd = database!!.rawQuery(slct1,arrayOf<String?>(spname, stadium3))
                    while (curCSVInd.moveToNext()) {
                        cnts = curCSVInd.getInt(14) // individuals icount
                        cntsl += cnts
                    }
                    curCSVInd.close()

                    // Select egg
                    curCSVInd = database!!.rawQuery(slct1, arrayOf<String?>(spname, stadium4))
                    while (curCSVInd.moveToNext()) {
                        cnts = curCSVInd.getInt(14) // individuals icount
                        cntse += cnts
                    }
                    curCSVInd.close()

                    cntsmf = curCSVCnt.getInt(1)
                    cntsm = curCSVCnt.getInt(2)
                    cntsf = curCSVCnt.getInt(3)
                    cntsp = curCSVCnt.getInt(4)
                    cntsl = curCSVCnt.getInt(5)
                    cntse = curCSVCnt.getInt(6)

                    // Suppress '0' in output
                    strcntsmf = if (cntsmf > 0) cntsmf.toString()
                    else ""
                    strcntsm = if (cntsm > 0) cntsm.toString()
                    else ""
                    strcntsf = if (cntsf > 0) cntsf.toString()
                    else ""
                    strcntsp = if (cntsp > 0) cntsp.toString()
                    else ""
                    strcntsl = if (cntsl > 0) cntsl.toString()
                    else ""
                    strcntse = if (cntse > 0) cntse.toString()
                    else ""
                    var spNotes: String = curCSVCnt.getString(9) // species notes
                    spNotes = "\"" + spNotes + "\""

                    // Species table
                    val arrStr =
                        arrayOf<String?>(
                            spname,    // species name
                            curCSVCnt.getString(10),  // local name
                            spcode,    // species code
                            strcntsmf, // count ♂ o. ♀
                            strcntsm,  // count ♂
                            strcntsf,  // count ♀
                            strcntsp,  // count pupa
                            strcntsl,  // count caterpillar
                            strcntse,  // count egg
                            spNotes   // species notes
                        )
                    csvWrite.writeNext(arrStr)

                    sum += cntsmf + cntsm + cntsf + cntsp + cntsl + cntse
                    summf += cntsmf
                    summ += cntsm
                    sumf += cntsf
                    sump += cntsp
                    suml += cntsl
                    sume += cntse

                    // Suppress 0 by blank
                    sumMF = if (summf == 0) ""
                    else summf.toString()

                    sumM = if (summ == 0) ""
                    else summ.toString()

                    sumF = if (sumf == 0) ""
                    else sumf.toString()

                    sumP = if (sump == 0) ""
                    else sump.toString()

                    sumL = if (suml == 0) ""
                    else suml.toString()

                    sumE = if (sume == 0) ""
                    else sume.toString()

                    cntsm = 0
                    cntsf = 0
                    cntsp = 0
                    cntsl = 0
                    cntse = 0
                }
                curCSVCnt.close()

                val sumSpec = countDataSource!!.diffSpec // get number of different species

                // Write total sum
                val arrSum =
                    arrayOf<String?>(
                        "",
                        getString(R.string.sumSpec) + " " + (sumSpec),
                        getString(R.string.sum),
                        sumMF,
                        sumM,
                        sumF,
                        sumP,
                        sumL,
                        sumE,
                        getString(R.string.sum_total) + " " + sum
                    )
                csvWrite.writeNext(arrSum)
                // End of Species table

                // Empty row
                csvWrite.writeNext(arrEmpt)

                // Individuals table
                // Write individual headline
                //    Individuals, Counts, Locality, Longitude, Latitude, Uncertainty, Height,
                //    Date, Time, Sexus, Phase, State, Indiv.-Notes 
                val arrIndHead =
                    arrayOf<String?>(
                        getString(R.string.individuals),
                        getString(R.string.cnts),
                        getString(R.string.locality),
                        getString(R.string.ycoord),
                        getString(R.string.xcoord),
                        getString(R.string.uncerti),
                        getString(R.string.zcoord),
                        getString(R.string.date),
                        getString(R.string.time),
                        getString(R.string.sex),
                        getString(R.string.stadium),
                        getString(R.string.status123),
                        getString(R.string.bems)
                    )
                csvWrite.writeNext(arrIndHead)

                // Build the sorted individuals array
                curCSVInd = database!!.rawQuery(
                    ("select * from " + DbHelper.INDIVIDUALS_TABLE
                            + " order by " + DbHelper.I_DATE_STAMP + ", " + DbHelper.I_TIME_STAMP),
                    null, null
                )

                var lngi: String
                var latit: String
                frst = 0
                while (curCSVInd.moveToNext()) {
                    longi = curCSVInd.getDouble(4)
                    lati = curCSVInd.getDouble(3)
                    uncer = round(curCSVInd.getDouble(6))
                    heigh = round(curCSVInd.getDouble(5))
                    spstate = curCSVInd.getInt(12)
                    spstate0 = if (spstate == 0) "-"
                    else spstate.toString()
                    cnts = curCSVInd.getInt(14)
                    strcnts = if (cnts > 0) cnts.toString()
                    else ""

                    lngi = try {
                        longi.toString().substring(0, 8) // longitude
                    } catch (_: StringIndexOutOfBoundsException) {
                        longi.toString()
                    }

                    latit = try {
                        lati.toString().substring(0, 8) // latitude
                    } catch (_: StringIndexOutOfBoundsException) {
                        lati.toString()
                    }

                    var iNotes = curCSVInd.getString(13)
                    if (iNotes == null) iNotes = ""
                    iNotes = "\"" + iNotes + "\""

                    val arrIndividual =
                        arrayOf<String?>(
                            curCSVInd.getString(2),  // species name
                            strcnts,  // indiv. counts
                            "\"" + curCSVInd.getString(9) + "\"",  // locality
                            lngi,  // longitude
                            latit,  // latitude
                            (uncer + 20).roundToInt().toString(),  // uncertainty + 20 m extra
                            heigh.roundToInt().toString(),  // height
                            "\"" + curCSVInd.getString(7) + "\"",  // date
                            curCSVInd.getString(8),  // time
                            curCSVInd.getString(10),  // sexus
                            curCSVInd.getString(11),  // phase
                            "\"" + spstate0 + "\"",  // status
                            iNotes // indiv. notes
                        )
                    csvWrite.writeNext(arrIndividual)

                    if (longi != 0.0)  // Has coordinates
                    {
                        if (frst == 0) {
                            loMin = longi
                            loMax = longi
                            laMin = lati
                            laMax = lati
                            uncer1 = uncer
                            frst = 1 // Just 1 with coordinates
                        } else {
                            loMin = min(loMin, longi)
                            loMax = max(loMax, longi)
                            laMin = min(laMin, lati)
                            laMax = max(laMax, lati)
                            uncer1 = max(uncer1, uncer)
                        }
                    }
                }
                curCSVInd.close()

                // Empty row
                csvWrite.writeNext(arrEmpt)

                // Write Average Coords
                val arrACoordHead =
                    arrayOf<String?>(
                        "",
                        "",
                        "",
                        getString(R.string.ycoord),
                        getString(R.string.xcoord),
                        getString(R.string.uncerti)
                    )
                csvWrite.writeNext(arrACoordHead)

                lo = (loMax + loMin) / 2 // average longitude
                la = (laMax + laMin) / 2 // average latitude

                // Simple distance calculation between 2 coordinates within the temperate zone in meters (Pythagoras):
                //   uc = (((loMax-loMin)*71500)² + ((laMax-laMin)*111300)²)½ 
                uc =
                    sqrt(((((loMax - loMin) * 71500).pow(2.0)) + (((laMax - laMin) * 111300).pow(2.0))))
                uc = round(uc / 2) + 20 // average uncertainty radius + default gps uncertainty
                if (uc <= uncer1) uc = uncer1

                lngi = try {
                    lo.toString().substring(0, 8) //longitude
                } catch (_: StringIndexOutOfBoundsException) {
                    lo.toString()
                }

                latit = try {
                    la.toString().substring(0, 8) // latitude
                } catch (_: StringIndexOutOfBoundsException) {
                    la.toString()
                }

                val arrAvCoords =
                    arrayOf<String?>(
                        "",
                        "",
                        getString(R.string.avCoords),
                        lngi,  // average longitude
                        latit,  // average latitude
                        uc.roundToInt().toString() // average uncertainty radius
                    )
                csvWrite.writeNext(arrAvCoords)

                csvWrite.close()
                dbHelper!!.close()

                mesg = getString(R.string.saveCSV)
                Toast.makeText(this,
                    fromHtml("<font color='blue'>$mesg</font>"),
                    Toast.LENGTH_SHORT
                ).show()
            } catch (_: IOException) {
                mesg = getString(R.string.saveFail)
                Toast.makeText(this,
                    fromHtml("<font color='red'><b>$mesg</b></font>"),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
    // End of exportDb2CSV()

    /***********************************************************************************************
     * Exports DB contents as Tour_dL_tourname_yyyyMMdd_HHmm_si.xlsx to
     * Documents/TourCount/ with purged data set.
     * Spreadsheet programs can directly load this .xlsx file
     */
    private fun exportDb2XLSX() {
        mesg = getString(R.string.saveXLSX)
        Toast.makeText(
            this,
            fromHtml("<font color='blue'>$mesg</font>"),
            Toast.LENGTH_SHORT
        ).show()

        section = sectionDataSource!!.section
        val temps: Int = section!!.tmp
        val tempe: Int = section!!.tmp_end
        val winds: Int = section!!.wind
        val winde: Int = section!!.wind_end
        val clouds: Int = section!!.clouds
        val cloude: Int = section!!.clouds_end

        // Get environment data from DB
        val date: String = section!!.date
        val startTM: String = section!!.start_tm
        val endTM: String = section!!.end_tm

        var engl = false
        var hasDate = true
        var xslxDate: String
        val xslxTime: String
        val xslxDateEU: String
        val xslxDateEN: String

        // Get year from date
        if (date != "") {
            // "-" at position 4 of the date means EN
            if (date.substring(4, 5) == "-") engl = true
            // Create date for filename as YYYYMMDD from any format
            xslxDateEU = date.substring(6, 10) + date.substring(3, 5) + date.substring(0, 2)
            xslxDateEN = date.substring(0, 4) + date.substring(5, 7) + date.substring(8, 10)
        } else {
            hasDate = false
            xslxDateEN = ""
            xslxDateEU = ""
        }

        xslxDate = if (engl) {
            xslxDateEN
        } else {
            xslxDateEU
        }

        if (startTM != "") {
            // has only a value when both date and start time are given
            xslxTime = startTM.substring(0, 2) + startTM.substring(3, 5)

            if (hasDate)
                xslxDate = "$xslxDate-$xslxTime" // yyyymmdd-hhmm
        } else xslxDate = ""

        // outFile -> /storage/emulated/0/Documents/TourCount/Tour_DL_tourname_yyyyMMdd_HHmm_si.xlsx
        var path = Environment.getExternalStorageDirectory()
        path = File("$path/Documents/TourCount")

        path.mkdirs()
        // Just verify path, result ignored
        val sortIdent = if (outPref == "names") {
            "n" // species name
        } else {
            "c" // species code
        }

        dataLanguage = prefs.getString("pref_sel_data_lang", "")!!
        if (dataLanguage.isEmpty()) dataLanguage = "--"

        // Create xlsx-filename
        outFile = if (tourNameForDir == "" && xslxDate == "")
            File(path,
            "/Tour_" + dataLanguage + "_" + getcurDate() + "_" + sortIdent + ".xlsx")
        else if (tourNameForDir == "")
            File(path,
            "/Tour_" + dataLanguage + "_" + xslxDate + "_" + sortIdent + ".xlsx")
        else if (xslxDate == "")
            File(path,
            "/Tour_" + dataLanguage + "_" + getcurDate() + "_" + tourNameForDir + "_" + sortIdent + ".xlsx")
        else File(path,
            "/Tour_" + dataLanguage + "_" + xslxDate + "_" + tourNameForDir + "_" + sortIdent + ".xlsx")

        val outputStream: FileOutputStream?
        try {
            outputStream = FileOutputStream(outFile)
        } catch (e: FileNotFoundException) {
            throw RuntimeException(e)
        }

        val head: Head?

        val sectName: String
        val inspecName: String
        val sectNotes: String

        val country: String
        val bState: String
        val plz: String
        val city: String
        val place: String
        val locality: String

        var spstate: Int
        var spstate0: String

        var longi: Double
        var lati: Double
        var heigh: Double
        var uncer: Double

        var frst: Int
        var sum = 0
        var summf = 0
        var summ = 0
        var sumf = 0
        var sump = 0
        var suml = 0
        var sume = 0

        val lo: Double
        val la: Double
        var loMin = 0.0
        var loMax = 0.0
        var laMin = 0.0
        var laMax = 0.0
        var uc: Double
        var uncer1 = 0.0

        // Prepare the fastexcel Workbook
        val wb = Workbook(outputStream, "TourCount", "1.0")
        wb.properties().setTitle(getString(R.string.results)) //
        wb.setGlobalDefaultFont("Arial", 11.0)

        // Prepare the fastexcel Worksheet
        val ws = wb.newWorksheet(getString(R.string.results))
        ws.paperSize(PaperSize.A4_PAPER)
        //  the freezePane topRows value is set beneath the metadata
        ws.pageOrientation("landscape")
        ws.freezePane(0, 9) // Fixed lines when scrolling
        ws.repeatRows(0, 1) // Fixed lines for printing
        ws.firstPageNumber(1)
        ws.footer("&P / &N", Position.CENTER)
        ws.fitToWidth(1.toShort())
        ws.fitToHeight(0.toShort())

        // Check if we can write the media
        mExternalStorageWriteable = Environment.MEDIA_MOUNTED == storageState

        if (!mExternalStorageWriteable) {
            mesg = getString(R.string.noCard)
            Toast.makeText(this,
                fromHtml("<font color='red'><b>$mesg</b></font>"),
                Toast.LENGTH_LONG
            ).show()
        } else {
            // Export the purged count table to the .xlsx-file

            // Get sorting mode info of species list
            val sortMode: String = if (outPref == "names") {
                getString(R.string.sort_names)
            } else {
                getString(R.string.sort_codes)
            }

            // Consult Section on Head tables for head and meta info
            section = sectionDataSource!!.section

            sectName = section!!.name
            sectNotes = section!!.notes

            if (metaPref) {
                country = section!!.country
                bState = section!!.b_state
                plz = section!!.plz
                city = section!!.city
                place = section!!.place
                locality = section!!.st_locality
            } else {
                country = getString(R.string.not_available)
                bState = getString(R.string.not_available)
                plz = getString(R.string.not_available)
                city = getString(R.string.not_available)
                place = getString(R.string.not_available)
                locality = getString(R.string.not_available)
            }

            headDataSource!!.open()
            head = headDataSource!!.head
            inspecName = head.observer
            headDataSource!!.close()

            //**************************
            // Start creating xlsx table
            // Row 0: Headline
            ws.value(0, 0, getString(R.string.zList) + ": " + sectName)
            ws.value(0, 4, getString(R.string.inspector) + ": " + inspecName)
            ws.value(0, 10, sortMode)
            ws.style(0, 10).horizontalAlignment("right").set()
            ws.range(0, 0, 0, 8).style().fillColor("CCCCCC").fontSize(12)
                .bold().set()
            ws.range(0, 0, 0, 2).merge()
            ws.range(0, 4, 0, 8).merge()
            ws.range(0, 10, 0, 13).merge()

            // Row 1
            ws.value(1, 10, getString(R.string.sort_time))
            ws.style(1, 10).horizontalAlignment("right").set()
            ws.range(1, 10, 1, 13).merge()

            // Row 2: Set location headline
            ws.value(2, 0, getString(R.string.country))
            ws.value(2, 1, getString(R.string.bstate))
            ws.value(2, 3, getString(R.string.plz))
            ws.value(2, 4, getString(R.string.city))
            ws.value(2, 6, getString(R.string.place))
            ws.value(2, 8, getString(R.string.slocality))
            ws.range(2, 0, 2, 13).style().borderStyle(BorderSide.BOTTOM, "thin")
                .bold().set()
            ws.range(2, 1, 2, 2).merge()
            ws.range(2, 4, 2, 5).merge()
            ws.range(2, 6, 2, 7).merge()
            ws.range(2, 8, 2, 13).merge()

            // Row 3: Set location data line with data of 1. location
            ws.value(3, 0, country)
            ws.value(3, 1, bState)
            ws.value(3, 3, plz)
            ws.value(3, 4, city)
            ws.value(3, 6, place)
            ws.value(3, 8, locality)
            ws.range(3, 1, 3, 2).merge()
            ws.range(3, 4, 3, 5).merge()
            ws.range(3, 6, 3, 7).merge()
            ws.range(3, 8, 3, 13).merge()

            // Row 5: Set environment headline
            ws.value(5, 0, getString(R.string.date)) // date
            ws.value(5, 1, "")
            ws.value(5, 2, getString(R.string.tm)) // time
            ws.value(5, 3, getString(R.string.temperature)) // temperature
            ws.value(5, 4, getString(R.string.wind)) // wind
            ws.value(5, 5, getString(R.string.clouds)) // clouds
            ws.value(5, 10, getString(R.string.zlNotes))
            ws.range(5, 0, 5, 1).style().borderStyle(BorderSide.BOTTOM, "thin")
                .bold().set()
            ws.range(5, 2, 5, 5).style().borderStyle(BorderSide.BOTTOM, "thin")
                .bold().horizontalAlignment("center").set()
            ws.range(5, 6, 5, 13).style().borderStyle(BorderSide.BOTTOM, "thin")
                .bold().set()
            ws.range(5, 10, 5, 13).merge()

            // Row 6: Write 1. line of environment data
            ws.value(6, 0, date)
            ws.value(6, 1, getString(R.string.from))
            ws.value(6, 2, startTM)
            ws.value(6, 3, temps.toString())
            ws.value(6, 4, winds.toString())
            ws.value(6, 5, clouds.toString())
            ws.value(6, 10, sectNotes)
            ws.style(6, 1).horizontalAlignment("right").set()
            ws.range(6, 2, 6, 5).style().horizontalAlignment("center").set()
            ws.range(6, 10, 6, 13).merge()

            // Row 7: Write 2. line of environment data
            ws.value(7, 1, getString(R.string.endtm))
            ws.value(7, 2, endTM)
            ws.value(7, 3, tempe.toString())
            ws.value(7, 4, winde.toString())
            ws.value(7, 5, cloude.toString())
            ws.style(7, 1).horizontalAlignment("right").set()
            ws.range(7, 2, 7, 5).style().horizontalAlignment("center").set()

            val nameSpecG = Utils.nameSpecG(dataLanguage)

            // Row 9: Write counts table headline
            //    Species Name, Local Name, Code, Counts, Spec.-Notes
            ws.value(9, 0, getString(R.string.name_spec)) // species name
            ws.value(9, 1, nameSpecG) // local name
            ws.value(9, 3, getString(R.string.speccode)) // code
            ws.value(9, 4, getString(R.string.cntsmf)) // counts
            ws.value(9, 5, getString(R.string.cntsm))
            ws.value(9, 6, getString(R.string.cntsf))
            ws.value(9, 7, getString(R.string.cntsp))
            ws.value(9, 8, getString(R.string.cntsl))
            ws.value(9, 9, getString(R.string.cntse))
            ws.value(9, 10, getString(R.string.bema)) // notes

            // Set styles for counts table headline
            ws.range(9, 1, 9, 2).merge()
            ws.range(9, 0, 9, 2).style().borderStyle(BorderSide.BOTTOM, "thin")
                .bold().set()
            ws.range(9, 3, 9, 9).style().borderStyle(BorderSide.BOTTOM, "thin")
                .bold().horizontalAlignment("center").set()
            ws.range(9, 10, 9, 13).style().borderStyle(BorderSide.BOTTOM, "thin")
                .bold().set()
            ws.range(9, 10, 9, 13).merge()
            ws.style(9, 4).borderStyle(BorderSide.LEFT, "thin")
                .borderStyle(BorderSide.BOTTOM, "thin").set()
            ws.style(9, 10).bold().borderStyle(BorderSide.LEFT, "thin")
                .borderStyle(BorderSide.BOTTOM, "thin").set()

            // Write counts data
            dbHelper = DbHelper(this)
            database = dbHelper!!.writableDatabase

            val curXLSXCnt: Cursor? // Cursor for Counts table

            // Sort mode species list
            if (outPref == "names") {
                curXLSXCnt = database!!.rawQuery(
                    ("select * from " + DbHelper.COUNT_TABLE
                            + " WHERE " + " ("
                            + DbHelper.C_NOTES + " = '0' or "
                            + DbHelper.C_COUNT_F1I + " > 0 or " + DbHelper.C_COUNT_F2I + " > 0 or "
                            + DbHelper.C_COUNT_F3I + " > 0 or " + DbHelper.C_COUNT_PI + " > 0 or "
                            + DbHelper.C_COUNT_LI + " > 0 or " + DbHelper.C_COUNT_EI + " > 0)"
                            + " order by " + DbHelper.C_NAME), null, null
                )
            } else {
                curXLSXCnt = database!!.rawQuery(
                    ("select * from " + DbHelper.COUNT_TABLE
                            + " WHERE " + " ("
                            + DbHelper.C_NOTES + " = '0' or "
                            + DbHelper.C_COUNT_F1I + " > 0 or " + DbHelper.C_COUNT_F2I + " > 0 or "
                            + DbHelper.C_COUNT_F3I + " > 0 or " + DbHelper.C_COUNT_PI + " > 0 or "
                            + DbHelper.C_COUNT_LI + " > 0 or " + DbHelper.C_COUNT_EI + " > 0)"
                            + " order by " + DbHelper.C_CODE), null, null
                )
            }

            // Get the number of individuals with attributes
            var cnts: Int // individuals icount
            var cntsmf: Int // imago male or female
            var cntsm = 0 // imago male
            var cntsf = 0 // imago female
            var cntsp = 0 // pupa
            var cntsl = 0 // caterpillar
            var cntse = 0 // egg

            val male = "m"
            val fmale = "f"
            val stadium1 = getString(R.string.stadium_1)
            val stadium2 = getString(R.string.stadium_2)
            val stadium3 = getString(R.string.stadium_3)
            val stadium4 = getString(R.string.stadium_4)

            var spname: String
            var spcode: String

            // Prepare species and individuals data
            var curXLSXInd: Cursor? // Cursor for Individuals table
            var specIndex = 0
            while (curXLSXCnt.moveToNext()) {
                spname = curXLSXCnt.getString(7) // species name from count table
                spcode = curXLSXCnt.getString(8) // species code from count table
                val slct = ("SELECT * FROM " + DbHelper.INDIVIDUALS_TABLE + " WHERE "
                        + DbHelper.I_NAME + " = ? AND "
                        + DbHelper.I_SEX + " = ? AND "
                        + DbHelper.I_STADIUM + " = ?")

                // Select male
                curXLSXInd = database!!.rawQuery(slct, arrayOf<String?>(spname, male, stadium1))
                while (curXLSXInd.moveToNext()) {
                    cnts = curXLSXInd.getInt(14)
                    cntsm += cnts
                }
                curXLSXInd.close()

                // Select female
                curXLSXInd = database!!.rawQuery(slct, arrayOf<String?>(spname, fmale, stadium1))
                while (curXLSXInd.moveToNext()) {
                    cnts = curXLSXInd.getInt(14)
                    cntsf += cnts
                }
                curXLSXInd.close()

                val slct1 = ("SELECT * FROM " + DbHelper.INDIVIDUALS_TABLE + " WHERE "
                        + DbHelper.I_NAME + " = ? AND "
                        + DbHelper.I_STADIUM + " = ?")

                // Select pupa
                curXLSXInd = database!!.rawQuery(slct1, arrayOf<String?>(spname, stadium2))
                while (curXLSXInd.moveToNext()) {
                    cnts = curXLSXInd.getInt(14)
                    cntsp += cnts
                }
                curXLSXInd.close()

                // Select caterpillar
                curXLSXInd = database!!.rawQuery(slct1,arrayOf<String?>(spname, stadium3))
                while (curXLSXInd.moveToNext()) {
                    cnts = curXLSXInd.getInt(14)
                    cntsl += cnts
                }
                curXLSXInd.close()

                // Select egg
                curXLSXInd = database!!.rawQuery(slct1, arrayOf<String?>(spname, stadium4))
                while (curXLSXInd.moveToNext()) {
                    cnts = curXLSXInd.getInt(14)
                    cntse += cnts
                }
                curXLSXInd.close()

                cntsmf = curXLSXCnt.getInt(1)
                cntsm = curXLSXCnt.getInt(2)
                cntsf = curXLSXCnt.getInt(3)
                cntsp = curXLSXCnt.getInt(4)
                cntsl = curXLSXCnt.getInt(5)
                cntse = curXLSXCnt.getInt(6)

                val spNotes = curXLSXCnt.getString(9) // species notes

                val even = specIndex % 2 == 0

                // Species table entry with counts,
                // Row 10 (+ index of species), alternating gray line background
                ws.value(10 + specIndex, 0, spname) // species name
                ws.value(10 + specIndex, 1, curXLSXCnt.getString(10)) // local name
                ws.value(10 + specIndex, 3, spcode) // species code

                // Alignment for column code
                ws.style(10 + specIndex, 3).horizontalAlignment("center").set()

                if (cntsmf == 0) ws.value(10 + specIndex, 4, "") // count ♂|♀
                else ws.value(10 + specIndex, 4, cntsmf)
                if (cntsm == 0) ws.value(10 + specIndex, 5, "") // count ♂
                else ws.value(10 + specIndex, 5, cntsm)
                if (cntsf == 0) ws.value(10 + specIndex, 6, "") // count ♀
                else ws.value(10 + specIndex, 6, cntsf)
                if (cntsp == 0) ws.value(10 + specIndex, 7, "") // count pupa
                else ws.value(10 + specIndex, 7, cntsp)
                if (cntsl == 0) ws.value(10 + specIndex, 8, "") // count caterpillar
                else ws.value(10 + specIndex, 8, cntsl)
                if (cntse == 0) ws.value(10 + specIndex, 9, "") // count egg
                else ws.value(10 + specIndex, 9, cntse)

                ws.value(10 + specIndex, 10, spNotes) // species notes

                ws.style(10 + specIndex, 4).borderStyle(BorderSide.LEFT, "thin").set()
                ws.style(10 + specIndex, 10).borderStyle(BorderSide.LEFT, "thin").set()
                ws.range(10 + specIndex, 1, 10 + specIndex, 2).merge()
                ws.range(10 + specIndex, 10, 10 + specIndex, 13).merge()

                // Conditional species notes, if "0" -> red
                if (spNotes == "0") ws.style(10 + specIndex, 10).fontColor("FF0000").set()

                // Alighnment and conditional red color for value > 0
                ws.range(10 + specIndex, 4, 10 + specIndex, 9)
                    .style().horizontalAlignment("center").fontColor("FF0000").set()

                if (!even) { // Background light gray
                    ws.range(10 + specIndex, 0, 10 + specIndex, 13)
                        .style().fillColor("DDDDDD").set()
                }

                sum += cntsmf + cntsm + cntsf + cntsp + cntsl + cntse
                summf += cntsmf
                summ += cntsm
                sumf += cntsf
                sump += cntsp
                suml += cntsl
                sume += cntse

                cntsm = 0
                cntsf = 0
                cntsp = 0
                cntsl = 0
                cntse = 0
                specIndex++
            }
            curXLSXCnt.close()
            // End of Species table

            val sumSpec = countDataSource!!.diffSpec // get number of different species

            // Write totals line
            // Row 10 + specIndex
            ws.value(10 + specIndex, 0, getString(R.string.sumSpec) + " ")
            ws.style(10 + specIndex, 0).horizontalAlignment("right").bold().set()
            ws.value(10 + specIndex, 1, sumSpec)
            ws.style(10 + specIndex, 1).horizontalAlignment("center").bold().set()
            ws.value(10 + specIndex, 3, getString(R.string.sum) + " ")
            ws.style(10 + specIndex, 3).horizontalAlignment("right").bold().set()
            ws.value(10 + specIndex, 4, summf)
            ws.value(10 + specIndex, 5, summ)
            ws.value(10 + specIndex, 6, sumf)
            ws.value(10 + specIndex, 7, sump)
            ws.value(10 + specIndex, 8, suml)
            ws.value(10 + specIndex, 9, sume)
            ws.range(10 + specIndex, 4, 10 + specIndex, 9)
                .style().horizontalAlignment("center").bold().set()
            ws.value(10 + specIndex, 10, getString(R.string.sum_total) + " ")
            ws.style(10 + specIndex, 10).horizontalAlignment("right").bold().set()
            ws.value(10 + specIndex, 11, sum)
            ws.style(10 + specIndex, 11).horizontalAlignment("center").bold().set()

            if (summf == 0) ws.style(10 + specIndex, 4).fontColor("00FFFFFF").set() // transparent
            if (summ == 0) ws.style(10 + specIndex, 5).fontColor("00FFFFFF").set() // transparent
            if (sumf == 0) ws.style(10 + specIndex, 6).fontColor("00FFFFFF").set() // transparent
            if (sump == 0) ws.style(10 + specIndex, 7).fontColor("00FFFFFF").set() // transparent
            if (suml == 0) ws.style(10 + specIndex, 8).fontColor("00FFFFFF").set() // transparent
            if (sume == 0) ws.style(10 + specIndex, 9).fontColor("00FFFFFF").set() // transparent

            // Set borders for totals
            ws.range(10 + specIndex, 0, 10 + specIndex, 13).style()
                .borderStyle(BorderSide.TOP, "thin").set()

            ws.style(10 + specIndex, 4)
                .borderStyle(BorderSide.LEFT, "thin")
                .borderStyle(BorderSide.TOP, "thin").set()

            ws.style(10 + specIndex, 10)
                .borderStyle(BorderSide.LEFT, "thin")
                .borderStyle(BorderSide.TOP, "thin").set()
            // End of totals

            // Individuals table headline
            //    Individuals, Counts, Locality, Longitude, Latitude, Uncertainty, Height,
            //    Date, Time, Sexus, Phase, State, Indiv.-Notes
            // Row 12 + specIndex
            ws.value(12 + specIndex, 0, getString(R.string.individuals))
            ws.value(12 + specIndex, 1, getString(R.string.locality))
            ws.value(12 + specIndex, 3, getString(R.string.cnts))
            ws.value(12 + specIndex, 4, getString(R.string.ycoord))
            ws.value(12 + specIndex, 5, getString(R.string.xcoord))
            ws.value(12 + specIndex, 6, getString(R.string.uncerti))
            ws.value(12 + specIndex, 7, getString(R.string.zcoord))
            ws.value(12 + specIndex, 8, getString(R.string.date))
            ws.value(12 + specIndex, 9, getString(R.string.time))
            ws.value(12 + specIndex, 10, getString(R.string.sex))
            ws.value(12 + specIndex, 11, getString(R.string.stadium))
            ws.value(12 + specIndex, 12, getString(R.string.status123))
            ws.value(12 + specIndex, 13, getString(R.string.bems))
            // Merge cells 1 + 2
            ws.range(12 + specIndex, 1, 12 + specIndex, 2).style().merge().set()
            // Cells 0 + 1 bottom line and bold
            ws.range(12 + specIndex, 0, 12 + specIndex, 2).style()
                .borderStyle(BorderSide.BOTTOM, "thin").bold().set()
            // Cells 3 - 12 bottom line, center and bold
            ws.range(12 + specIndex, 3, 12 + specIndex, 12).style().borderStyle(
                BorderSide.BOTTOM, "thin"
            ).bold().horizontalAlignment("center").set()
            // Cell 13 bottom line and bold
            ws.style(12 + specIndex, 13).borderStyle(BorderSide.BOTTOM, "thin").bold().set()

            // Build the sorted individuals array
            curXLSXInd = database!!.rawQuery(
                ("select * from " + DbHelper.INDIVIDUALS_TABLE
                        + " order by " + DbHelper.I_DATE_STAMP + ", " + DbHelper.I_TIME_STAMP),
                null, null
            )

            var lngi: String
            var latit: String
            frst = 0
            var indIndex = specIndex
            while (curXLSXInd.moveToNext()) {
                longi = curXLSXInd.getDouble(4)
                lati = curXLSXInd.getDouble(3)
                uncer = round(curXLSXInd.getDouble(6))
                heigh = round(curXLSXInd.getDouble(5))
                spstate = curXLSXInd.getInt(12)
                spstate0 = if (spstate == 0) "-"
                else spstate.toString()
                cnts = curXLSXInd.getInt(14)

                lngi = try {
                    longi.toString().substring(0, 8) // longitude
                } catch (_: StringIndexOutOfBoundsException) {
                    longi.toString()
                }

                latit = try {
                    lati.toString().substring(0, 8) // latitude
                } catch (_: StringIndexOutOfBoundsException) {
                    lati.toString()
                }

                val iNotes = curXLSXInd.getString(13)

                val even = indIndex % 2 == 0

                // Individuals table entries
                if (even) {
                    ws.value(13 + indIndex, 0, curXLSXInd.getString(2)) // species name
                    ws.value(13 + indIndex, 1, curXLSXInd.getString(9)) // locality
                    ws.value(13 + indIndex, 3, cnts) // indiv. counts
                    ws.value(13 + indIndex, 4, lngi) // longitude
                    ws.value(13 + indIndex, 5, latit) // latitude
                    ws.value(
                        13 + indIndex,
                        6,
                        (uncer + 20).roundToInt().toString()
                    ) // uncertainty + 20 m extra
                    ws.value(13 + indIndex, 7, heigh.roundToInt().toString()) // height
                    ws.value(13 + indIndex, 8, curXLSXInd.getString(7)) // date
                    ws.value(13 + indIndex, 9, curXLSXInd.getString(8)) // time
                    ws.value(13 + indIndex, 10, curXLSXInd.getString(10)) // sexus
                    ws.value(13 + indIndex, 11, curXLSXInd.getString(11)) // phase
                    ws.value(13 + indIndex, 12, spstate0) // status
                    ws.value(13 + indIndex, 13, iNotes) // indiv. notes
                    // Merge cells 1 + 2
                    ws.range(13 + indIndex, 1, 13 + indIndex, 2).style().merge().set()
                    // Cells 3 - 12 center
                    ws.range(13 + indIndex, 3, 13 + indIndex, 12)
                        .style().horizontalAlignment("center").set()
                } else {
                    ws.value(13 + indIndex, 0, curXLSXInd.getString(2)) // species name
                    ws.value(13 + indIndex, 1, curXLSXInd.getString(9)) // locality
                    ws.value(13 + indIndex, 3, cnts) // indiv. counts
                    ws.value(13 + indIndex, 4, lngi) // longitude
                    ws.value(13 + indIndex, 5, latit) // latitude
                    ws.value(
                        13 + indIndex,
                        6,
                        (uncer + 20).roundToInt().toString()
                    ) // uncertainty + 20 m extra
                    ws.value(13 + indIndex, 7, heigh.roundToInt().toString()) // height
                    ws.value(13 + indIndex, 8, curXLSXInd.getString(7)) // date
                    ws.value(13 + indIndex, 9, curXLSXInd.getString(8)) // time
                    ws.value(13 + indIndex, 10, curXLSXInd.getString(10)) // sexus
                    ws.value(13 + indIndex, 11, curXLSXInd.getString(11)) // phase
                    ws.value(13 + indIndex, 12, spstate0) // status
                    ws.value(13 + indIndex, 13, iNotes) // indiv. notes
                    // Merge cells 1 + 2
                    ws.range(13 + indIndex, 1, 13 + indIndex, 2).style().merge().set()
                    // Cells 0 - 2 color "DDDDDD"
                    ws.range(13 + indIndex, 0, 13 + indIndex, 2).style().fillColor("DDDDDD").set()
                    // Cells 3 - 12 center
                    ws.range(13 + indIndex, 3, 13 + indIndex, 12)
                        .style().fillColor("DDDDDD").horizontalAlignment("center").set()
                    ws.style(13 + indIndex, 13).fillColor("DDDDDD").set()
                }

                if (longi != 0.0)  // Has coordinates
                {
                    if (frst == 0) {
                        loMin = longi
                        loMax = longi
                        laMin = lati
                        laMax = lati
                        uncer1 = uncer
                        frst = 1 // Just 1 with coordinates
                    } else {
                        loMin = min(loMin, longi)
                        loMax = max(loMax, longi)
                        laMin = min(laMin, lati)
                        laMax = max(laMax, lati)
                        uncer1 = max(uncer1, uncer)
                    }
                }
                indIndex++ // Last increment adds an empty row
            }
            curXLSXInd.close()

            // Write Average Coords headline
            ws.value(14 + indIndex, 4, getString(R.string.ycoord))
            ws.value(14 + indIndex, 5, getString(R.string.xcoord))
            ws.value(14 + indIndex, 6, getString(R.string.uncerti))

            // Merge cells 2 + 3
            ws.range(14 + indIndex, 2, 14 + indIndex, 3).style().merge().set()
            // Set for cells 4 - 6 bottom line, center and bold
            ws.range(14 + indIndex, 2, 14 + indIndex, 6)
                .style().borderStyle(BorderSide.BOTTOM, "thin")
                .horizontalAlignment("center").bold().set()
            // Set left line in cell (14 + indIndex, 4)
            ws.style(14 + indIndex, 4).borderStyle(BorderSide.BOTTOM, "thin")
                .borderStyle(BorderSide.LEFT, "thin").set()
            // Set left line in cell (15 + indIndex, 4)
            ws.style(15 + indIndex, 4).borderStyle(BorderSide.LEFT, "thin").set()

            lo = (loMax + loMin) / 2 // average longitude
            la = (laMax + laMin) / 2 // average latitude

            // Simple distance calculation between 2 coordinates within the temperate zone in meters (Pythagoras):
            //   uc = (((loMax-loMin)*71500)² + ((laMax-laMin)*111300)²)½
            uc = sqrt(((((loMax - loMin) * 71500).pow(2.0)) + (((laMax - laMin) * 111300).pow(2.0))))
            uc = round(uc / 2) + 20 // average uncertainty radius + default gps uncertainty
            if (uc <= uncer1) uc = uncer1

            lngi = try {
                lo.toString().substring(0, 8) //longitude
            } catch (_: StringIndexOutOfBoundsException) {
                lo.toString()
            }

            latit = try {
                la.toString().substring(0, 8) // latitude
            } catch (_: StringIndexOutOfBoundsException) {
                la.toString()
            }

            // Write Average Coords
            ws.value(15 + indIndex, 1, getString(R.string.avCoords))
            ws.value(15 + indIndex, 4, lngi)
            ws.value(15 + indIndex, 5, latit)
            ws.value(15 + indIndex, 6, uc.roundToInt().toString()) // average uncertainty radius

            ws.range(15 + indIndex, 1, 15 + indIndex, 3).style().merge()
                .horizontalAlignment("right").bold().set()
            ws.range(15 + indIndex, 4, 15 + indIndex, 6)
                .style().horizontalAlignment("center").set()

            dbHelper!!.close()

            try {
                // Export purged db as xlsx
                wb.finish()
                outputStream.close()
                wb.close()
            } catch (_: IOException) {
                mesg = getString(R.string.saveFail)
                Toast.makeText(this,
                    fromHtml("<font color='red'><b>$mesg</b></font>"),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
    // End of exportDb2XLSX()

    // Export current species list species_ll_tourname_YYYYMMDD_hhmmss.csv to both data directories
    //  /Documents/TransektCount and /Documents/TourCount
    private fun exportSpeciesList() {
        val outFileTour: File
        val outFileTransect: File

        val pathTransect =
            File(Environment.getExternalStorageDirectory().toString() + "/Documents/TransektCount")
        val pathTour =
            File(Environment.getExternalStorageDirectory().toString() + "/Documents/TourCount")

        // Check if we can write the media
        mExternalStorageWriteable = Environment.MEDIA_MOUNTED == storageState

        if (!mExternalStorageWriteable) {
            mesg = getString(R.string.noCard)
            Toast.makeText(this,
                fromHtml("<font color='red'><b>$mesg</b></font>"),
                Toast.LENGTH_LONG
            ).show()
        } else {
            // Export species list into species_ll_tour_Tourname_yyyyMMdd_HHmmss.csv
            dataLanguage = prefs.getString("pref_sel_data_lang", "")!!
            if (dataLanguage.isEmpty()) dataLanguage = "--"

            val codeArray = countDataSource!!.getAllStringsSrtCode("code")
            val nameArray = countDataSource!!.getAllStringsSrtCode("name")
            val nameArrayL = countDataSource!!.getAllStringsSrtCode("name_g")

            val specNum = codeArray.size

            pathTransect.mkdirs() // Just verify pathTransect, result ignored
            pathTour.mkdirs() // Just verify pathTour, result ignored

            when (dataLanguage) {
                "de" -> {
                    if (tourNameForDir == "") {
                        outFileTransect = File(
                            pathTransect, ("/species_de_"
                                    + getcurDate() + ".csv")
                        )
                        outFileTour = File(
                            pathTour, ("/species_de_"
                                    + getcurDate() + ".csv")
                        )
                    } else {
                        outFileTransect = File(
                            pathTransect, ("/species_de_"
                                    + tourNameForDir + "_" + getcurDate() + ".csv")
                        )
                        outFileTour = File(
                            pathTour, ("/species_de_"
                                    + tourNameForDir + "_" + getcurDate() + ".csv")
                        )
                    }
                }

                "en" -> {
                    if (tourNameForDir == "") {
                        outFileTransect = File(
                            pathTransect, ("/species_en_"
                                    + getcurDate() + ".csv")
                        )
                        outFileTour = File(
                            pathTour, ("/species_en_"
                                    + getcurDate() + ".csv")
                        )
                    } else {
                        outFileTransect = File(
                            pathTransect, ("/species_en_"
                                    + tourNameForDir + "_" + getcurDate() + ".csv")
                        )
                        outFileTour = File(
                            pathTour, ("/species_en_"
                                    + tourNameForDir + "_" + getcurDate() + ".csv")
                        )
                    }
                }

                "fr" -> {
                    if (tourNameForDir == "") {
                        outFileTransect = File(
                            pathTransect, ("/species_fr_"
                                    + getcurDate() + ".csv")
                        )
                        outFileTour = File(
                            pathTour, ("/species_fr_"
                                    + getcurDate() + ".csv")
                        )
                    } else {
                        outFileTransect = File(
                            pathTransect, ("/species_fr_"
                                    + tourNameForDir + "_" + getcurDate() + ".csv")
                        )
                        outFileTour = File(
                            pathTour, ("/species_fr_"
                                    + tourNameForDir + "_" + getcurDate() + ".csv")
                        )
                    }
                }

                "it" -> {
                    if (tourNameForDir == "") {
                        outFileTransect = File(
                            pathTransect, ("/species_it_"
                                    + getcurDate() + ".csv")
                        )
                        outFileTour = File(
                            pathTour, ("/species_it_"
                                    + getcurDate() + ".csv")
                        )
                    } else {
                        outFileTransect = File(
                            pathTransect, ("/species_it_"
                                    + tourNameForDir + "_" + getcurDate() + ".csv")
                        )
                        outFileTour = File(
                            pathTour, ("/species_it_"
                                    + tourNameForDir + "_" + getcurDate() + ".csv")
                        )
                    }
                }

                "es" -> {
                    if (tourNameForDir == "") {
                        outFileTransect = File(
                            pathTransect, ("/species_es_"
                                    + getcurDate() + ".csv")
                        )
                        outFileTour = File(
                            pathTour, ("/species_es_"
                                    + getcurDate() + ".csv")
                        )
                    } else {
                        outFileTransect = File(
                            pathTransect, ("/species_es_"
                                    + tourNameForDir + "_" + getcurDate() + ".csv")
                        )
                        outFileTour = File(
                            pathTour, ("/species_es_"
                                    + tourNameForDir + "_" + getcurDate() + ".csv")
                        )
                    }
                }

                else -> {
                    // No data language given
                    if (tourNameForDir == "") {
                        outFileTransect = File(
                            pathTransect, ("/species_--_"
                                    + getcurDate() + ".csv")
                        )
                        outFileTour = File(
                            pathTour, ("/species_--_"
                                    + getcurDate() + ".csv")
                        )
                    } else {
                        outFileTransect = File(
                            pathTransect, ("/species_--_"
                                    + tourNameForDir + "_" + getcurDate() + ".csv")
                        )
                        outFileTour = File(
                            pathTour, ("/species_--_"
                                    + tourNameForDir + "_" + getcurDate() + ".csv")
                        )
                    }
                }
            }

            // If TransektCount is installed export to /Documents/TransektCount
            if (pathTransect.exists() && pathTransect.isDirectory) {
                try {
                    val csvWrite = CSVWriter(FileWriter(outFileTransect))

                    // 1. line of species list contains
                    //   field 0: String "nocode",
                    //   field 1: String "language",
                    //   field 2: String "de"|"en"|"fr"|"it"|"es"
                    val specLine1 = arrayOf<String?>("nocode,language,$dataLanguage")
                    csvWrite.writeNext(specLine1)

                    var i = 0
                    while (i < specNum) {
                        val specLine =
                            arrayOf(
                                codeArray[i],
                                nameArray[i],
                                nameArrayL[i]
                            )
                        i++
                        csvWrite.writeNext(specLine)
                    }
                    csvWrite.close()
                } catch (_: Exception) {
                    mesg = getString(R.string.saveFailListTransect)
                    Toast.makeText(this,
                        fromHtml("<font color='red'><b>$mesg</b></font>"),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

            // Export to /Documents/TourCount
            if (pathTour.exists() && pathTour.isDirectory) {
                try {
                    val csvWrite = CSVWriter(FileWriter(outFileTour))

                    // 1. line with nocode, language, de|en|fr|it|es
                    val specLine1 = arrayOf<String?>("nocode,language,$dataLanguage")
                    csvWrite.writeNext(specLine1)

                    var i = 0
                    while (i < specNum) {
                        val specLine =
                            arrayOf(
                                codeArray[i],
                                nameArray[i],
                                nameArrayL[i]
                            )
                        i++
                        csvWrite.writeNext(specLine)
                    }
                    csvWrite.close()

                    mesg = getString(R.string.saveList)
                    Toast.makeText(
                        this,
                        fromHtml("<font color='blue'>$mesg</font>"),
                        Toast.LENGTH_SHORT
                    ).show()
                } catch (_: Exception) {
                    mesg = getString(R.string.saveFailList)
                    Toast.makeText(
                        this,
                        fromHtml("<font color='red'><b>$mesg</b></font>"),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
    // End of exportSpeciesList()

    // Clear all relevant DB values, reset to basic DB
    private fun resetToBasisDb() {
        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "3002, resetToBasisDb")

        // Confirm dialogue before anything else takes place
        val builder = AlertDialog.Builder(this)
        builder.setIcon(android.R.drawable.ic_dialog_alert)
        builder.setMessage(R.string.confirmResetDB)
        builder.setCancelable(false)
        builder.setPositiveButton(
            R.string.deleteButton
        ) { _: DialogInterface?, _: Int ->
            val resOK = clearDBValues()
            if (resOK) {
                mesg = getString(R.string.reset2basic)
                Toast.makeText(this,  // bright green
                    fromHtml("<font color='#008000'>$mesg</font>"),
                    Toast.LENGTH_SHORT
                ).show()
            }
            supportActionBar!!.title = ""
        }
        builder.setNegativeButton(
            R.string.cancelButton
        ) { dialog: DialogInterface?, _: Int -> dialog!!.cancel() }
        alert = builder.create()
        alert.show()
    }

    // Clear DB and location values for basic DB
    private fun clearDBValues(): Boolean {
        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "3032, clearDBValues")

        dbHelper = DbHelper(this)
        database = dbHelper!!.writableDatabase
        var resOK = true

        try {
            var sql = ("UPDATE " + DbHelper.COUNT_TABLE + " SET "
                    + DbHelper.C_COUNT_F1I + " = 0, "
                    + DbHelper.C_COUNT_F2I + " = 0, "
                    + DbHelper.C_COUNT_F3I + " = 0, "
                    + DbHelper.C_COUNT_PI + " = 0, "
                    + DbHelper.C_COUNT_LI + " = 0, "
                    + DbHelper.C_COUNT_EI + " = 0, "
                    + DbHelper.C_NOTES + " = '';")
            database!!.execSQL(sql)

            sql = ("UPDATE " + DbHelper.SECTION_TABLE + " SET "
                    + DbHelper.S_NAME + " = '', "
                    + DbHelper.S_COUNTRY + " = '', "
                    + DbHelper.S_PLZ + " = '', "
                    + DbHelper.S_CITY + " = '', "
                    + DbHelper.S_PLACE + " = '', "
                    + DbHelper.S_TEMPE + " = 0, "
                    + DbHelper.S_WIND + " = 0, "
                    + DbHelper.S_CLOUDS + " = 0, "
                    + DbHelper.S_TEMPE_END + " = 0, "
                    + DbHelper.S_WIND_END + " = 0, "
                    + DbHelper.S_CLOUDS_END + " = 0, "
                    + DbHelper.S_DATE + " = '', "
                    + DbHelper.S_START_TM + " = '', "
                    + DbHelper.S_END_TM + " = '', "
                    + DbHelper.S_NOTES + " = '', "
                    + DbHelper.S_STATE + " = '', "
                    + DbHelper.S_ST_LOCALITY + " = '';")
            database!!.execSQL(sql)

            sql = "DELETE FROM " + DbHelper.INDIVIDUALS_TABLE
            database!!.execSQL(sql)
        } catch (_: Exception) {
            mesg = getString(R.string.resetFail)
            Toast.makeText(this,
                fromHtml("<font color='red'><b>$mesg</b></font>"),
                Toast.LENGTH_LONG
            ).show()

            resOK = false
        }
        dbHelper!!.close()

        TourCountApplication.lat = 0.0
        TourCountApplication.lon = 0.0
        TourCountApplication.heightNN = 0.0
        TourCountApplication.uncertainty = 0.0
        TourCountApplication.tLocality = ""
        TourCountApplication.isFirstLocality = true

        // Restart Location Service and try to read location
        if (fineLocationPermGranted) {
            // Start location service and get 1. location
            locationDispatcher(1) // Start LocationService
        }
        return resOK
    }
    // End of resetToBasisDb()

    // Red warning message
    private fun showSnackbarRed(str: String) {
        baseLayout = findViewById(R.id.baseLayout)
        val sB = Snackbar.make(baseLayout!!, str, Snackbar.LENGTH_LONG)
        val tv = sB.getView().findViewById<TextView>(R.id.snackbar_text)
        tv.setTypeface(tv.typeface, Typeface.BOLD)
        tv.setTextColor(Color.RED)
        tv.gravity = Gravity.CENTER
        sB.show()
    }

    // Date for filename of exported data
    private fun getcurDate(): String {
        val date = Date()
        @SuppressLint("SimpleDateFormat") val dform: DateFormat =
            SimpleDateFormat("yyyyMMdd-HHmm")
        return dform.format(date)
    }

    // Copy file block-wise
    @Throws(IOException::class)
    private fun copy(src: File?, dst: File?) {
        val fIS = FileInputStream(src)
        val fOS = FileOutputStream(dst)

        // Transfer bytes from in to out
        val buf = ByteArray(1024)
        var len: Int
        while ((fIS.read(buf).also { len = it }) > 0) {
            fOS.write(buf, 0, len)
        }
        fIS.close()
        fOS.close()
    }

    companion object {
        private const val TAG = "WelcomeAct"
    }

}
