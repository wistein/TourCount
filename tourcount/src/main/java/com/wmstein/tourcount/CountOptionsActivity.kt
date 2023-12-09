package com.wmstein.tourcount

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NavUtils
import androidx.core.content.ContextCompat
import com.wmstein.tourcount.PermissionsDialogFragment.Companion.newInstance
import com.wmstein.tourcount.PermissionsDialogFragment.PermissionsGrantedCallback
import com.wmstein.tourcount.database.Count
import com.wmstein.tourcount.database.CountDataSource
import com.wmstein.tourcount.widgets.EditNotesWidget

/**********************************
 * CountOptionsActivity
 * Edit notes for counting species
 * uses EditNotesWidget.java and activity_count_options.xml
 * Based on CountOptionsActivity.java by milo on 05/05/2014.
 * Adopted and changed by wmstein on 18.02.2016,
 * last edited in Java on 2023-05-13,
 * converted to Kotlin on 2023-07-06,
 * last edited on 2023-11-29
 */
class CountOptionsActivity : AppCompatActivity(), OnSharedPreferenceChangeListener,
    PermissionsGrantedCallback {
    private var tourCount: TourCountApplication? = null

    private var staticWidgetArea: LinearLayout? = null
    private var enw: EditNotesWidget? = null
    private var count: Count? = null
    private var countId = 0
    private var countDataSource: CountDataSource? = null
    private var bMap: Bitmap? = null
    private var bg: BitmapDrawable? = null

    // Preferences
    private var prefs = TourCountApplication.getPrefs()
    private var brightPref = false
    private var metaPref = false // option for reverse geocoding
    private var emailString: String? = "" // mail address for OSM query

    // Location info handling
    private var latitude = 0.0
    private var longitude = 0.0
    private var locationService: LocationService? = null

    // Permission dispatcher mode locationPermissionDispatcherMode: 
    //  1 = use location service
    //  2 = end location service
    private var locationPermissionDispatcherMode = 0

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        tourCount = application as TourCountApplication
        prefs.registerOnSharedPreferenceChangeListener(this)
        brightPref = prefs.getBoolean("pref_bright", true)
        metaPref = prefs.getBoolean("pref_metadata", false) // use Reverse Geocoding
        emailString = prefs.getString("email_String", "") // for reliable query of Nominatim service

        setContentView(R.layout.activity_count_options)
        val countingScreen = findViewById<LinearLayout>(R.id.count_options)

        // Set full brightness of screen
        if (brightPref) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            val params = window.attributes
            params.screenBrightness = 1.0f
            window.attributes = params
        }
        bMap = tourCount!!.decodeBitmap(R.drawable.kbackground, tourCount!!.width, tourCount!!.height)
        bg = BitmapDrawable(countingScreen.resources, bMap)
        countingScreen.background = bg
        staticWidgetArea = findViewById(R.id.static_widget_area)
        val extras = intent.extras
        if (extras != null) {
            countId = extras.getInt("count_id")
        }
    }

    override fun onResume() {
        super.onResume()

        // clear any existing views
        staticWidgetArea!!.removeAllViews()

        // Get location with permissions check
        locationPermissionDispatcherMode = 1
        locationCaptureFragment()

        // get the data sources
        countDataSource = CountDataSource(this)
        countDataSource!!.open()
        count = countDataSource!!.getCountById(countId)
        supportActionBar!!.title = count!!.name
        enw = EditNotesWidget(this, null)
        enw!!.notesName = count!!.notes
        enw!!.setWidgetTitle(getString(R.string.notesSpecies))
        enw!!.setHint(getString(R.string.notesHint))
        staticWidgetArea!!.addView(enw)
    }

    override fun onPause() {
        super.onPause()

        // finally, close the database
        countDataSource!!.close()
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(enw!!.windowToken, 0)

        // Stop location service with permissions check
        locationPermissionDispatcherMode = 2
        locationCaptureFragment()
    }

    private fun saveData() {
        // don't crash if the user hasn't filled things in...
        // Toast here, as snackbar doesn't show up
        Toast.makeText(
            this@CountOptionsActivity,
            getString(R.string.sectSaving) + " " + count!!.name + "!",
            Toast.LENGTH_SHORT
        ).show()
        count!!.notes = enw!!.notesName
        // hide keyboard
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(enw!!.windowToken, 0)
        countDataSource!!.saveCount(count!!)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.count_options, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        val id = item.itemId
        if (id == android.R.id.home) {
            val intent = NavUtils.getParentActivityIntent(this)!!
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            NavUtils.navigateUpTo(this, intent)
        } else if (id == R.id.menuSaveExit) {
            saveData()
            super.finish()
        }
        return super.onOptionsItemSelected(item)
    }

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onSharedPreferenceChanged(prefs: SharedPreferences?, key: String?) {
        val countingScreen = findViewById<LinearLayout>(R.id.count_options)
        countingScreen.background = null
        prefs?.registerOnSharedPreferenceChangeListener(this)
        brightPref = prefs!!.getBoolean("pref_bright", true)
        metaPref = prefs.getBoolean("pref_metadata", false) // use Reverse Geocoding
        emailString = prefs.getString("email_String", "")   // for reliable query of Nominatim service
        bMap = tourCount!!.decodeBitmap(R.drawable.kbackground, tourCount!!.width, tourCount!!.height)
        bg = BitmapDrawable(countingScreen.resources, bMap)
        countingScreen.background = bg
    }

    override fun locationCaptureFragment() {
        run {
            if (this.isPermissionGranted) {
                when (locationPermissionDispatcherMode) {
                    1 ->  // get location
                        this.loc

                    2 ->  // stop location service
                        locationService!!.stopListener()
                }
            } else {
                if (locationPermissionDispatcherMode == 1) newInstance().show(
                    supportFragmentManager,
                    PermissionsDialogFragment::class.java.name
                )
            }
        }
    }

    // if API level > 23 test for permissions granted
    private val isPermissionGranted: Boolean
        get() = (ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED)

    // get the location data
    private val loc: Unit
        get() {
            locationService = LocationService(this)
            if (locationService!!.canGetLocation()) {
                longitude = locationService!!.getLongitude()
                latitude = locationService!!.getLatitude()
            }
        }
}