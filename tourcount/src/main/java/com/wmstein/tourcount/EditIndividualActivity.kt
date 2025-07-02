package com.wmstein.tourcount

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup.MarginLayoutParams
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.text.HtmlCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.work.Data
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.WorkRequest
import com.wmstein.egm.EarthGravitationalModel
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
 * last edited on 2025-06-28
 */
class EditIndividualActivity : AppCompatActivity() {
    private var individuals: Individuals? = null
    private var tmp: Temp? = null
    private var counts: Count? = null
    private var indivArea: LinearLayout? = null
    private var eiw: EditIndividualWidget? = null

    // The actual data
    private var individualsDataSource: IndividualsDataSource? = null
    private var tempDataSource: TempDataSource? = null
    private var countDataSource: CountDataSource? = null

    // Preferences
    private val prefs = TourCountApplication.getPrefs()
    private var brightPref = false // option for full bright screen
    private var metaPref = false // option for reverse geocoding
    private var emailString: String? = "" // mail address for OSM query
    private var buttonSound: String = ""
    private var buttonSoundPref = false
    private var buttonVibPref = false

    private val mHandler = Handler(Looper.getMainLooper())
    private var r: Ringtone? = null

    // Prepare vibrator service
    private var vibrator: Vibrator? = null

    // Location info handling
    private var latitude = 0.0
    private var longitude = 0.0
    private var height = 0.0
    private var uncertainty: String? = ""
    private var locationService: LocationService? = null
    private var sLocality: String? = ""

    // locationDispatcherMode:
    //  1 = use location service
    //  2 = end location service
    private var locationDispatcherMode = 0
    private var countId = 0
    private var indivId = 0
    private var indivAttr = 0 // 1 = ♂|♀, 2 = ♂, 3 = ♀, 4 = caterpillar, 5 = pupa, 6 = egg
    private var specName: String? = null

    // phase123 is true for butterfly (♂|♀, ♂ or ♀), false for egg, caterpillar or pupa
    private var phase123: Boolean? = null
    private var datestamp: String? = ""
    private var timestamp: String? = ""
    private var code: String? = ""
    private var mesg: String? = null

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (MyDebug.DLOG) Log.i(TAG, "109, onCreate")

        brightPref = prefs.getBoolean("pref_bright", true)
        metaPref = prefs.getBoolean("pref_metadata", false) // use Reverse Geocoding
        emailString = prefs.getString("email_String", "") // for reliable query of Nominatim service
        buttonSoundPref = prefs.getBoolean("pref_button_sound", false)
        buttonVibPref = prefs.getBoolean("pref_button_vib", false)
        buttonSound = prefs.getString("button_sound", null).toString()

        if (SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) // SDK 35+
        {
            enableEdgeToEdge()
        }
        setContentView(R.layout.activity_edit_individual)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.editIndividualScreen))
        { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            // Apply the insets as a margin to the view. This solution sets
            // only the bottom, left, and right dimensions, but you can apply whichever
            // insets are appropriate to your layout. You can also update the view padding
            // if that's more appropriate.
            v.updateLayoutParams<MarginLayoutParams> {
                topMargin = insets.top
                leftMargin = insets.left
                bottomMargin = insets.bottom
                rightMargin = insets.right
            }

            // Return CONSUMED if you don't want the window insets to keep passing
            // down to descendant views.
            WindowInsetsCompat.CONSUMED
        }

        // Set full brightness of screen
        if (brightPref) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            val params = window.attributes
            params.screenBrightness = 1.0f
            window.attributes = params
        }

        indivArea = findViewById(R.id.edit_individual)

        soundButtonSound()

        // Prepare vibrator service
        vibrator = applicationContext.getSystemService(Vibrator::class.java)

        if (buttonVibPref && vibrator!!.hasVibrator()) {
            buttonVib()
        }

        // Get parameters from CountingActivity
        val extras = intent.extras
        if (extras != null) {
            countId = extras.getInt("count_id")
            specName = extras.getString("SName")
            code = extras.getString("SCode")
            datestamp = extras.getString("date")
            timestamp = extras.getString("time")
            indivAttr = extras.getInt("indivAtt")
        }
    }
    // End of onCreate()

    @SuppressLint("LongLogTag", "DefaultLocale")
    override fun onResume() {
        super.onResume()

        // Get location with permissions check
        locationDispatcherMode = 1
        locationDispatcher()

        // Clear any existing views
        indivArea!!.removeAllViews()

        // setup the data sources
        individualsDataSource = IndividualsDataSource(this)
        individualsDataSource!!.open()

        // Get last found locality from tmp
        tempDataSource = TempDataSource(this)
        tempDataSource!!.open()
        tmp = tempDataSource!!.tmp
        sLocality = if (tmp!!.temp_loc != null) tmp!!.temp_loc else ""

        countDataSource = CountDataSource(this)
        countDataSource!!.open()

        // Set title
        try {
            supportActionBar!!.title = specName
        } catch (_: NullPointerException) {
            if (MyDebug.DLOG) Log.e(TAG, "203, NullPointerException: No species name!")
        }
        counts = countDataSource!!.getCountById(countId)

        // Display the editable data
        eiw = EditIndividualWidget(this, null)
        eiw!!.setWidgetLocality1(getString(R.string.locality) + ":")
        eiw!!.widgetLocality2 = sLocality!!
        eiw!!.setWidgetZCoord1(getString(R.string.zcoord))
        eiw!!.setWidgetZCoord2(String.format("%.1f", height))
        eiw!!.setWidgetStadium1(getString(R.string.stadium) + ":")
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
            eiw!!.widgetState1(true) // headline status
            eiw!!.setWidgetState1(getString(R.string.status123) + ":")
            eiw!!.widgetState2(true) // state
            eiw!!.setWidgetState2("")
        } else {
            eiw!!.widgetState1(false)
            eiw!!.setWidgetState2("-")
            eiw!!.widgetState2(false)
        }
        eiw!!.setWidgetCount1(getString(R.string.count1) + ":") // icount
        eiw!!.widgetCount2 = 1
        eiw!!.setWidgetIndivNote1(getString(R.string.note) + ":")
        eiw!!.widgetIndivNote2 = ""
        eiw!!.setWidgetXCoord1(getString(R.string.xcoord))
        eiw!!.setWidgetXCoord2(String.format("%.6f", latitude))
        eiw!!.setWidgetYCoord1(getString(R.string.ycoord))
        eiw!!.setWidgetYCoord2(String.format("%.6f", longitude))
        indivArea!!.addView(eiw)
    }
    // End of onResume()

    override fun onPause() {
        super.onPause()

        // Close the data sources
        individualsDataSource!!.close()
        tempDataSource!!.close()
        countDataSource!!.close()

        // Stop RetrieveAddrWorker
        WorkManager.getInstance(this).cancelAllWork()

        // Stop location service with permissions check
        locationDispatcherMode = 2
        locationDispatcher()

        if (r != null)
            r!!.stop() // stop media player
    }

    private fun saveData(): Boolean {
        // Save individual data
        indivId = individualsDataSource!!.saveIndividual(
            individualsDataSource!!.createIndividuals(
                countId,
                specName,
                latitude,
                longitude,
                height,
                uncertainty,
                datestamp,
                timestamp,
                sLocality,
                code
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
                mesg = getString(R.string.valState)
                Toast.makeText(
                    applicationContext,
                    HtmlCompat.fromHtml(
                        "<font color='red'><b>" + mesg + "</b></font>",
                        HtmlCompat.FROM_HTML_MODE_LEGACY
                    ), Toast.LENGTH_LONG
                ).show()
                return false
            }
        }

        // Number of individuals
        val newcount = eiw!!.widgetCount2
        if (newcount > 0) // valid positive newcount
        {
            when (indivAttr) {
                1 -> {
                    // ♂|♀
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
                    // Pupa
                    counts!!.count_pi = counts!!.count_pi + newcount
                    individuals!!.icount = newcount
                    individuals!!.sex = "-"
                    individuals!!.icategory = 4
                    countDataSource!!.saveCountpi(counts!!)
                }

                5 -> {
                    // Larva
                    counts!!.count_li = counts!!.count_li + newcount
                    individuals!!.icount = newcount
                    individuals!!.sex = "-"
                    individuals!!.icategory = 5
                    countDataSource!!.saveCountli(counts!!)
                }

                6 -> {
                    // Eggs
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
            mesg = getString(R.string.warnCount)
            Toast.makeText(
                applicationContext,
                HtmlCompat.fromHtml(
                    "<font color='red'><b>" + mesg + "</b></font>",
                    HtmlCompat.FROM_HTML_MODE_LEGACY
                ), Toast.LENGTH_LONG
            ).show()
            return false // forces input newcount > 0
        }

        tempDataSource!!.saveTempLoc(tmp!!)
        return true
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.edit_individual, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here.
        val id = item.itemId
        if (id == R.id.menuSaveExit) {
            if (saveData()) {
                finish()
            }
            return true
        } else if (id == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    fun locationDispatcher() {
        run {
            if (this.isFineLocationPermGranted) {
                when (locationDispatcherMode) {
                    1 ->  // Get location
                        this.loc

                    2 ->  // Stop location service
                        locationService!!.stopListener()
                }
            }
        }
    }

    private val isFineLocationPermGranted: Boolean
        get() = (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED)

    // Get the location data
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

            // Get reverse geocoding
            if (locationService!!.canGetLocation() && metaPref && (latitude != 0.0 || longitude != 0.0)) {
                val urlString = ("https://nominatim.openstreetmap.org/reverse?email=" + emailString
                        + "&format=xml&lat=" + latitude + "&lon=" + longitude + "&zoom=18&addressdetails=1")

                // Implementation with WorkManager
                val retrieveAddrWorkRequest: WorkRequest = OneTimeWorkRequest
                    .Builder(RetrieveAddrWorker::class.java)
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
        } catch (_: IOException) {
            return 0.0
        }

        // Calculate the offset between the ellipsoid and geoid
        corrHeight = try {
            gh.heightOffset(latitude, longitude, gpsHeight)
        } catch (_: Exception) {
            return 0.0
        }
        return gpsHeight + corrHeight
    }

    private fun soundButtonSound() {
        if (buttonSoundPref) {
            try {
                var notification: Uri?
                if (buttonSound.isNotBlank()) notification =
                    buttonSound.toUri()
                else notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                r = RingtoneManager.getRingtone(applicationContext, notification)
                r!!.play()
                mHandler.postDelayed({ r!!.stop() }, 400)
            } catch (e: java.lang.Exception) {
                if (MyDebug.DLOG) Log.e(TAG, "513, could not play button sound.", e)
            }
        }
    }

    private fun buttonVib() {
        if (SDK_INT >= 31) {
            vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
        } else {
            if (SDK_INT >= 26) {
                vibrator?.vibrate(
                    VibrationEffect.createOneShot(
                        200,
                        VibrationEffect.DEFAULT_AMPLITUDE
                    )
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(200)
            }
            vibrator?.cancel()
        }
    }

    companion object {
        private const val TAG = "EditIndivAct"
    }

}
