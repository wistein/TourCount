package com.wmstein.tourcount

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.work.Data
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.WorkRequest
import com.google.android.material.snackbar.Snackbar
import com.wmstein.egm.EarthGravitationalModel
import com.wmstein.tourcount.PermissionsDialogFragment.Companion.newInstance
import com.wmstein.tourcount.PermissionsDialogFragment.PermissionsGrantedCallback
import com.wmstein.tourcount.database.Count
import com.wmstein.tourcount.database.CountDataSource
import com.wmstein.tourcount.database.Individuals
import com.wmstein.tourcount.database.IndividualsDataSource
import com.wmstein.tourcount.database.Temp
import com.wmstein.tourcount.database.TempDataSource
import com.wmstein.tourcount.widgets.EditIndividualWidget
import java.io.IOException

/*******************************************************************************************
 * EditIndividualActivity is called from CountingActivity and collects additional info to an
 * individual's data record
 * Copyright 2016-2023 wmstein
 * created on 2016-05-15,
 * last modification in Java an 2023-07-09,
 * converted to Kotlin on 2023-07-11,
 * last edited on 2024-07-17
 */
class EditIndividualActivity : AppCompatActivity(), OnSharedPreferenceChangeListener,
    PermissionsGrantedCallback {
    private var tourCount: TourCountApplication? = null

    private var individuals: Individuals? = null
    private var tmp: Temp? = null
    private var counts: Count? = null
    private var indivArea: LinearLayout? = null
    private var eiw: EditIndividualWidget? = null

    // The actual data
    private var individualsDataSource: IndividualsDataSource? = null
    private var tempDataSource: TempDataSource? = null
    private var countDataSource: CountDataSource? = null
    private var bMap: Bitmap? = null
    private var bg: BitmapDrawable? = null

    // Preferences
    private val prefs = TourCountApplication.getPrefs()
    private var brightPref = false // option for full bright screen
    private var metaPref = false // option for reverse geocoding
    private var emailString: String? = "" // mail address for OSM query

    // Location info handling
    private var latitude = 0.0
    private var longitude = 0.0
    private var height = 0.0
    private var uncertainty: String? = ""
    private var locationService: LocationService? = null
    private var sLocality: String? = ""

    // Permission dispatcher mode locationPermissionDispatcherMode:
    //  1 = use location service
    //  2 = end location service
    private var locationPermissionDispatcherMode = 0
    private var countId = 0
    private var indivId = 0
    private var indivAttr = 0 // 1 = ♂♀, 2 = ♂, 3 = ♀, 4 = caterpillar, 5 = pupa, 6 = egg
    private var specName: String? = null
    private var sdata : Boolean? = null // true: data saved already
    private var phase123 : Boolean? = null // true for butterfly (♂♀, ♂ or ♀), false for egg, caterpillar or pupa
    private var datestamp : String? = ""
    private var timestamp : String? = ""

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        tourCount = application as TourCountApplication
        prefs.registerOnSharedPreferenceChangeListener(this)
        brightPref = prefs.getBoolean("pref_bright", true)
        metaPref = prefs.getBoolean("pref_metadata", false) // use Reverse Geocoding
        emailString = prefs.getString("email_String", "") // for reliable query of Nominatim service

        // Set full brightness of screen
        if (brightPref) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            val params = window.attributes
            params.screenBrightness = 1.0f
            window.attributes = params
        }
        setContentView(R.layout.activity_edit_individual)
        val indivScreen = findViewById<ScrollView>(R.id.editIndividualScreen)
        bMap = tourCount!!.decodeBitmap(R.drawable.kbackground, tourCount!!.width, tourCount!!.height)
        bg = BitmapDrawable(indivScreen.resources, bMap)
        indivScreen.background = bg
        indivArea = findViewById(R.id.edit_individual)

        // get parameters from CountingActivity
        val extras = intent.extras
        if (extras != null) {
            countId = extras.getInt("count_id")
            specName = extras.getString("SName")
            datestamp = extras.getString("date")
            timestamp = extras.getString("time")
            indivAttr = extras.getInt("indivAtt")
        }
        sdata = false
    }

    @SuppressLint("LongLogTag", "DefaultLocale")
    override fun onResume() {
        super.onResume()

        // Get location with permissions check
        locationPermissionDispatcherMode = 1
        locationCaptureFragment()

        // clear any existing views
        indivArea!!.removeAllViews()

        // setup the data sources
        individualsDataSource = IndividualsDataSource(this)
        individualsDataSource!!.open()

        // get last found locality from tmp
        tempDataSource = TempDataSource(this)
        tempDataSource!!.open()
        tmp = tempDataSource!!.tmp
        sLocality = if (tmp!!.temp_loc != null) tmp!!.temp_loc else ""
        countDataSource = CountDataSource(this)
        countDataSource!!.open()

        // set title
        try {
            supportActionBar!!.title = specName
        } catch (e: NullPointerException) {
            if (MyDebug.LOG) Log.e(TAG, "157, NullPointerException: No species name!")
        }
        counts = countDataSource!!.getCountById(countId)

        // display the editable data
        eiw = EditIndividualWidget(this, null)
        eiw!!.setWidgetLocality1(getString(R.string.locality)+":")
        eiw!!.widgetLocality2 = sLocality!!
        eiw!!.setWidgetZCoord1(getString(R.string.zcoord)+":")
        eiw!!.setWidgetZCoord2(String.format("%.1f", height)+":")
        eiw!!.setWidgetStadium1(getString(R.string.stadium)+":")
        when (indivAttr) {
            1, 2, 3 -> {
                eiw!!.widgetStadium2 = getString(R.string.stadium_1)
                phase123 = true
            }

            4 -> { // Pupa
                eiw!!.widgetStadium2 = getString(R.string.stadium_2)
                phase123 = false
            }

            5 -> { // Larva
                eiw!!.widgetStadium2 = getString(R.string.stadium_3)
                phase123 = false
            }

            6 -> { // Egg
                eiw!!.widgetStadium2 = getString(R.string.stadium_4)
                phase123 = false
            }
        }
        if (phase123!!) {
            eiw!!.widgetState1(true) // headline state
            eiw!!.setWidgetState1(getString(R.string.state)+":")
            eiw!!.widgetState2(true) // state
            eiw!!.setWidgetState2("")
        } else {
            eiw!!.widgetState1(false)
            eiw!!.setWidgetState2("-")
            eiw!!.widgetState2(false)
        }
        eiw!!.setWidgetCount1(getString(R.string.count1)+":") // icount
        eiw!!.widgetCount2 = 1
        eiw!!.setWidgetIndivNote1(getString(R.string.note)+":")
        eiw!!.widgetIndivNote2 = ""
        eiw!!.setWidgetXCoord1(getString(R.string.xcoord))
        eiw!!.setWidgetXCoord2(String.format("%.6f", latitude))
        eiw!!.setWidgetYCoord1(getString(R.string.ycoord))
        eiw!!.setWidgetYCoord2(String.format("%.6f", longitude))
        indivArea!!.addView(eiw)
    }
    // end of onResume()

    override fun onPause() {
        super.onPause()

        // close the data sources
        individualsDataSource!!.close()
        tempDataSource!!.close()
        countDataSource!!.close()

        // Stop location service with permissions check
        locationPermissionDispatcherMode = 2
        locationCaptureFragment()
    }

    private fun saveData(): Boolean {
        // save individual data
        indivId = individualsDataSource!!.saveIndividual(
            individualsDataSource!!.createIndividuals(
                countId,
                specName,
                latitude,
                longitude,
                height,
                uncertainty,
                datestamp,
                timestamp
            )
        )
        individuals = individualsDataSource!!.getIndividual(indivId)

        // Locality (from reverse geocoding in CountingActivity or manual input)
        individuals!!.locality = eiw!!.widgetLocality2

        // Uncertainty
        if (latitude != 0.0) {
            individuals!!.uncert = uncertainty
        } else {
            individuals!!.uncert = "0"
        }
        tmp!!.temp_loc = eiw!!.widgetLocality2

        // Stadium
        individuals!!.stadium = eiw!!.widgetStadium2

        // State_1-6
        val newstate0 = eiw!!.widgetState2
        if (newstate0 == "-" || newstate0 == "")
            individuals!!.state_1_6 = 0
        else {
            val newstate = newstate0.toInt()
            if (newstate in 0..6) {
                individuals!!.state_1_6 = newstate
            } else {
                showSnackbarRed(getString(R.string.valState))
                return false
            }
        }

        // number of individuals
        val newcount = eiw!!.widgetCount2
        if (newcount > 0) // valid positive newcount
        {
            when (indivAttr) {
                1 -> {
                    // ♂♀
                    counts!!.count_f1i = counts!!.count_f1i + newcount
                    individuals!!.icount = newcount
                    individuals!!.sex = "-"
                    individuals!!.icategory = 1
                    countDataSource!!.saveCountf1i(counts!!)
                }

                2 -> {
                    // ♂
                    counts!!.count_f2i = counts!!.count_f2i + newcount
                    individuals!!.icount = newcount
                    individuals!!.sex = "m"
                    individuals!!.icategory = 2
                    countDataSource!!.saveCountf2i(counts!!)
                }

                3 -> {
                    // ♀
                    counts!!.count_f3i = counts!!.count_f3i + newcount
                    individuals!!.icount = newcount
                    individuals!!.sex = "f"
                    individuals!!.icategory = 3
                    countDataSource!!.saveCountf3i(counts!!)
                }

                4 -> {
                    // pupa
                    counts!!.count_pi = counts!!.count_pi + newcount
                    individuals!!.icount = newcount
                    individuals!!.sex = "-"
                    individuals!!.icategory = 4
                    countDataSource!!.saveCountpi(counts!!)
                }

                5 -> {
                    // larva
                    counts!!.count_li = counts!!.count_li + newcount
                    individuals!!.icount = newcount
                    individuals!!.sex = "-"
                    individuals!!.icategory = 5
                    countDataSource!!.saveCountli(counts!!)
                }

                6 -> {
                    // eggs
                    counts!!.count_ei = counts!!.count_ei + newcount
                    individuals!!.icount = newcount
                    individuals!!.sex = "-"
                    individuals!!.icategory = 6
                    countDataSource!!.saveCountei(counts!!)
                }
            }

            // Notes
            val newnotes = eiw!!.widgetIndivNote2
            if (newnotes != "") {
                individuals!!.notes = newnotes
            }
            individualsDataSource!!.saveIndividual(individuals)
        } else  // newcount is <= 0
        {
            showSnackbarRed(getString(R.string.warnCount))
            return false // forces input newcount > 0
        }

        tempDataSource!!.saveTempLoc(tmp!!)
        sdata = true
        return true
    }

    private fun showSnackbarRed(str: String) {
        val view = findViewById<View>(R.id.editIndividualScreen)
        val sB = Snackbar.make(view, str, Snackbar.LENGTH_LONG)
        sB.setActionTextColor(Color.RED)
        val tv = sB.view.findViewById<TextView>(R.id.snackbar_text)
        tv.textAlignment = View.TEXT_ALIGNMENT_CENTER
        tv.setTypeface(tv.typeface, Typeface.BOLD)
        sB.show()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.edit_individual, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        val id = item.itemId
        if (id == R.id.menuSaveExit) {
            if (saveData()) {
                super.finish()
                // close the data sources
                individualsDataSource!!.close()
                tempDataSource!!.close()
                countDataSource!!.close()
            }
            return true
        }
        if (id == android.R.id.home)
        {
            val intent = Intent(this@EditIndividualActivity, CountingActivity::class.java)
            startActivity(intent)
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onSharedPreferenceChanged(prefs: SharedPreferences?, key: String?) {
        val indivScreen = findViewById<ScrollView>(R.id.editIndividualScreen)
        indivScreen.background = null
        brightPref = prefs!!.getBoolean("pref_bright", true)
        metaPref = prefs.getBoolean("pref_metadata", false) // use Reverse Geocoding
        emailString = prefs.getString("email_String", "") // for reliable query of Nominatim service
        bMap = tourCount!!.decodeBitmap(R.drawable.kbackground, tourCount!!.width, tourCount!!.height)
        bg = BitmapDrawable(indivScreen.resources, bMap)
        indivScreen.background = bg
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
                height = locationService!!.altitude
                if (height != 0.0) height = correctHeight(latitude, longitude, height)
                uncertainty = locationService!!.accuracy.toString()
            }

            // get reverse geocoding
            if (locationService!!.canGetLocation() && metaPref && (latitude != 0.0 || longitude != 0.0)) {
                val urlString = ("https://nominatim.openstreetmap.org/reverse?email=" + emailString
                        + "&format=xml&lat=" + latitude + "&lon=" + longitude + "&zoom=18&addressdetails=1")

                // Trial with WorkManager
                val retrieveAddrWorkRequest: WorkRequest = OneTimeWorkRequest.Builder(RetrieveAddrWorker::class.java)
                    .setInputData(
                        Data.Builder()
                            .putString("URL_STRING", urlString)
                            .build()
                    )
                    .build()
                WorkManager
                    .getInstance(this)
                    .enqueue(retrieveAddrWorkRequest)
            }
        }

    // Correct height with geoid offset from a simplified EarthGravitationalModel
    private fun correctHeight(latitude: Double, longitude: Double, gpsHeight: Double): Double {
        val corrHeight: Double
        val gh = EarthGravitationalModel()
        try {
            gh.load(this) // load the WGS84 correction coefficient table egm180.txt
        } catch (e: IOException) {
            return 0.0
        }

        // Calculate the offset between the ellipsoid and geoid
        corrHeight = try {
            gh.heightOffset(latitude, longitude, gpsHeight)
        } catch (e: Exception) {
            return 0.0
        }
        return gpsHeight + corrHeight
    }

    companion object {
        private const val TAG = "EditIndivAct"
    }

}
