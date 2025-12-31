package com.wmstein.tourcount

import android.annotation.SuppressLint
import android.content.Context
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
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
import com.wmstein.tourcount.database.Count
import com.wmstein.tourcount.database.CountDataSource
import com.wmstein.tourcount.database.Individuals
import com.wmstein.tourcount.database.IndividualsDataSource
import com.wmstein.tourcount.database.Temp
import com.wmstein.tourcount.database.TempDataSource
import com.wmstein.tourcount.widgets.EditIndividualWidget

/*******************************************************************************************
 * EditIndividualActivity is called from CountingActivity and collects additional info to an
 * individual's data record
 * Copyright 2016-2023 wmstein
 * created on 2016-05-15,
 * last modification in Java an 2023-07-09,
 * converted to Kotlin on 2023-07-11,
 * last edited on 2025-12-31
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
    private var awakePref = false
    private var brightPref = false // option for full bright screen
    private var metaPref = false // option for reverse geocoding
    private var emailString: String? = "" // mail address for OSM query
    private var buttonSound: String = ""
    private var buttonSoundPref = false
    private var buttonVibPref = false

    private var rToneP: MediaPlayer? = null

    // Prepare vibrator service
    private var vibrator: Vibrator? = null

    // Location info handling
    private var latitude = 0.0
    private var longitude = 0.0
    private var height = 0.0
    private var uncertainty: String? = ""
    private var sLocality: String? = ""

    private var countId = 0
    private var indivId = 0
    private var indivAttr = 0 // 1 = ♂|♀, 2 = ♂, 3 = ♀, 4 = caterpillar, 5 = pupa, 6 = egg
    private var specName: String? = null

    // phase123 is true for butterfly (♂|♀, ♂ or ♀), false for egg, caterpillar or pupa
    private var phase123: Boolean? = null
    private var dateStamp: String? = ""
    private var timeStamp: String? = ""
    private var code: String? = ""
    private var mesg: String? = null
    private var audioAttributionContext: Context = this

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "95, onCreate")

        audioAttributionContext =
            if (Build.VERSION.SDK_INT >= 30)
                ContextCompat.createAttributionContext(this, "ringSound")
            else this

        awakePref = prefs.getBoolean("pref_awake", true)
        brightPref = prefs.getBoolean("pref_bright", true)
        metaPref = prefs.getBoolean("pref_metadata", false) // use Reverse Geocoding
        emailString = prefs.getString("email_String", "") // for reliable query of Nominatim service
        buttonSoundPref = prefs.getBoolean("pref_button_sound", false)
        buttonVibPref = prefs.getBoolean("pref_button_vib", false)
        buttonSound = prefs.getString("button_sound", null).toString()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) // SDK 35+
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
            val params = window.attributes
            params.screenBrightness = 1.0f
            window.attributes = params
        }

        if (awakePref)
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        indivArea = findViewById(R.id.edit_individual)

        soundButtonSound() // sound for (+)-button

        // Prepare vibrator service
        if (buttonVibPref) {
            vibrator = applicationContext.getSystemService(Vibrator::class.java)
            buttonVib()
        }

        // Get parameters from CountingActivity
        val extras = intent.extras
        if (extras != null) {
            countId = extras.getInt("count_id")
            specName = extras.getString("SName")
            code = extras.getString("SCode")
            dateStamp = extras.getString("date")
            timeStamp = extras.getString("time")
            indivAttr = extras.getInt("indivAtt")
            latitude = extras.getDouble("cLatitude")
            longitude = extras.getDouble("cLongitude")
            height = extras.getDouble("cHeight")
            uncertainty = extras.getString("cUncert")
        }
    }
    // End of onCreate()

    @SuppressLint("LongLogTag", "DefaultLocale")
    override fun onResume() {
        super.onResume()

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
            if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                Log.e(TAG, "197, NullPointerException: No species name!")
        }
        counts = countDataSource!!.getCountById(countId)

        // Display the editable data
        eiw = EditIndividualWidget(this, null)
        eiw!!.setWidgetLocality1(getString(R.string.locality) + ":")
        eiw!!.widgetLocality2 = sLocality!!
        eiw!!.setWidgetZCoord1(getString(R.string.zcoord))
        eiw!!.setWidgetZCoord2(
            String.format(
                "%.0f",
                height
            )
        ) // set string with integer value from double
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

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "260, onPause")

        // Close the data sources
        individualsDataSource!!.close()
        tempDataSource!!.close()
        countDataSource!!.close()
    }

    override fun onStop() {
        super.onStop()

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "272, onStop")

        if (buttonSoundPref) {
            if (rToneP != null) {
                if (rToneP!!.isPlaying) {
                    rToneP!!.stop() // stop media player
                }
                rToneP!!.release()
                rToneP = null
            }
        }

        if (awakePref) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "293, onDestroy")
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
                dateStamp,
                timeStamp,
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
        val newState0 = eiw!!.widgetState2
        if (newState0 == "-" || newState0 == "")
            individuals!!.state_1_6 = 0
        else {
            val newState = newState0.toInt()
            if (newState in 0..6) {
                individuals!!.state_1_6 = newState
            } else {
                mesg = getString(R.string.valState)
                Toast.makeText(
                    applicationContext,
                    HtmlCompat.fromHtml(
                        "<font color='red'><b>$mesg</b></font>",
                        HtmlCompat.FROM_HTML_MODE_LEGACY
                    ), Toast.LENGTH_LONG
                ).show()
                return false
            }
        }

        // Number of individuals
        val newCount = eiw!!.widgetCount2
        if (newCount > 0) // valid positive newCount
        {
            when (indivAttr) {
                1 -> {
                    // ♂|♀
                    counts!!.count_f1i = counts!!.count_f1i + newCount
                    individuals!!.icount = newCount
                    individuals!!.sex = "-"
                    individuals!!.icategory = 1
                    countDataSource!!.saveCountf1i(counts!!)
                }

                2 -> {
                    // ♂
                    counts!!.count_f2i = counts!!.count_f2i + newCount
                    individuals!!.icount = newCount
                    individuals!!.sex = "m"
                    individuals!!.icategory = 2
                    countDataSource!!.saveCountf2i(counts!!)
                }

                3 -> {
                    // ♀
                    counts!!.count_f3i = counts!!.count_f3i + newCount
                    individuals!!.icount = newCount
                    individuals!!.sex = "f"
                    individuals!!.icategory = 3
                    countDataSource!!.saveCountf3i(counts!!)
                }

                4 -> {
                    // Pupa
                    counts!!.count_pi = counts!!.count_pi + newCount
                    individuals!!.icount = newCount
                    individuals!!.sex = "-"
                    individuals!!.icategory = 4
                    countDataSource!!.saveCountpi(counts!!)
                }

                5 -> {
                    // Larva
                    counts!!.count_li = counts!!.count_li + newCount
                    individuals!!.icount = newCount
                    individuals!!.sex = "-"
                    individuals!!.icategory = 5
                    countDataSource!!.saveCountli(counts!!)
                }

                6 -> {
                    // Eggs
                    counts!!.count_ei = counts!!.count_ei + newCount
                    individuals!!.icount = newCount
                    individuals!!.sex = "-"
                    individuals!!.icategory = 6
                    countDataSource!!.saveCountei(counts!!)
                }
            }

            // Notes
            val newNotes = eiw!!.widgetIndivNote2
            if (newNotes != "") {
                individuals!!.notes = newNotes
            }
            individualsDataSource!!.saveIndividual(individuals)
        } else  // newCount is <= 0
        {
            mesg = getString(R.string.warnCount)
            Toast.makeText(
                this,
                HtmlCompat.fromHtml(
                    "<font color='red'><b>$mesg</b></font>",
                    HtmlCompat.FROM_HTML_MODE_LEGACY
                ), Toast.LENGTH_LONG
            ).show()
            return false // forces input newCount > 0
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

    private fun soundButtonSound() {
        if (buttonSoundPref) {
            val rtUri = if (buttonSound.isNotBlank())
                buttonSound.toUri()
            else
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            rToneP = MediaPlayer.create(audioAttributionContext, rtUri)
            if (rToneP!!.isPlaying) {
                rToneP!!.stop()
                rToneP!!.release()
            }
            rToneP!!.start()
        }
    }

    private fun buttonVib() {
        if (Build.VERSION.SDK_INT >= 31) {
            vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
        } else {
            if (Build.VERSION.SDK_INT >= 26) {
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
