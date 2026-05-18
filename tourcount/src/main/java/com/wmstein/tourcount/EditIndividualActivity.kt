package com.wmstein.tourcount

import android.annotation.SuppressLint
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
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import com.wmstein.tourcount.TourCountApplication.Companion.heightNN
import com.wmstein.tourcount.TourCountApplication.Companion.lat
import com.wmstein.tourcount.TourCountApplication.Companion.lon
import com.wmstein.tourcount.TourCountApplication.Companion.sLocality
import com.wmstein.tourcount.TourCountApplication.Companion.uncertainty
import com.wmstein.tourcount.Utils.fromHtml
import com.wmstein.tourcount.database.Count
import com.wmstein.tourcount.database.CountDataSource
import com.wmstein.tourcount.database.Individuals
import com.wmstein.tourcount.database.IndividualsDataSource
import com.wmstein.tourcount.widgets.EditIndividualWidget

/*******************************************************************************************
 * EditIndividualActivity is called from CountingActivity and collects additional info to an
 * individual's data record
 *
 * Created by wmstein on 2016-05-15,
 * last modification in Java an 2023-07-09,
 * converted to Kotlin on 2023-07-11,
 * last edited on 2026-05-05
 */
class EditIndividualActivity : AppCompatActivity() {
    private var individuals: Individuals? = null
    private var counts: Count? = null
    private var indivArea: LinearLayout? = null
    private var eiw: EditIndividualWidget? = null

    // The actual data
    private var individualsDataSource: IndividualsDataSource? = null
    private var countDataSource: CountDataSource? = null

    // Preferences
    private val prefs = TourCountApplication.getPrefs()
    private var awakePref = false
    private var brightPref = false // option for full bright screen
    private var buttonSoundPref = false
    private var buttonVibPref = false
    private var metaPref: Boolean = false // use Reverse Geocoding

    // Prepare sound service
    private var soundService: SoundService? = null

    // Prepare vibrator service
    private var vibrator: Vibrator? = null

    // Location info handling
    private var uncert: String? = ""
    private var countId = 0
    private var indivId = 0
    private var indivAttr = 0 // 1 = ♂|♀, 2 = ♂, 3 = ♀, 4 = caterpillar, 5 = pupa, 6 = egg
    private var specName: String? = ""

    // phase123 is true for butterfly (♂|♀, ♂ or ♀), false for egg, caterpillar or pupa
    private var phase123: Boolean? = null
    private var dateStamp: String? = ""
    private var timeStamp: String? = ""
    private var code: String? = ""
    private var mesg: String? = ""

    @SuppressLint("SourceLockedOrientationActivity", "DefaultLocale")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "84, onCreate")

        awakePref = prefs.getBoolean("pref_awake", true) // keep screen on
        brightPref = prefs.getBoolean("pref_bright", true)
        buttonSoundPref = prefs.getBoolean("pref_button_sound", false)
        buttonVibPref = prefs.getBoolean("pref_button_vib", false)
        metaPref = prefs.getBoolean("pref_metadata", false) // use Reverse Geocoding

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

        // Make (+)-button sound
        if (buttonSoundPref) {
            soundService = SoundService(applicationContext)
            soundService!!.soundPlusButtonSound() // sound for (+)-button
        }

        // Prepare vibrator service
        if (buttonVibPref) {
            vibrator = applicationContext.getSystemService(Vibrator::class.java)
            buttonVib()
        }

        // Get parameters from CountingActivity
        val extras = intent.extras
        if (extras != null) {
            countId = extras.getInt("count_id")
            if (extras.getString("SName") != "")
                specName = extras.getString("SName")
            code = extras.getString("SCode")
            dateStamp = extras.getString("date")
            timeStamp = extras.getString("time")
            indivAttr = extras.getInt("indivAtt")
        }

        uncert = String.format("%f", uncertainty)
    }
    // End of onCreate()

    override fun onResume() {
        super.onResume()

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "161, onResume")

        // Clear any existing views
        indivArea!!.removeAllViews()

        // Set up the data sources
        individualsDataSource = IndividualsDataSource(this)
        individualsDataSource!!.open()

        countDataSource = CountDataSource(this)
        countDataSource!!.open()

        // Set title
        supportActionBar!!.title = specName

        counts = countDataSource!!.getCountById(countId)

        displayData()
    }
    // End of onResume()

    // Display the editable data
    @SuppressLint("DefaultLocale")
    fun displayData() {
        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG) {
            Log.i(TAG, "186, displayData: $sLocality")
        }

        eiw = EditIndividualWidget(this, null)
        eiw!!.setWidgetZCoord1(getString(R.string.zcoord))

        // Height value from GPS_PROVIDER
        if (heightNN != 0.0) {
            // Set string with value from double for heightNN
            eiw!!.setWidgetZCoord2(
                String.format(
                    "%.0f",
                    heightNN
                )
            )
        } else { // No height value from NETWORK_PROVIDER
            // Set "-" as there is no height value
            eiw!!.setWidgetZCoord2("-")
        }
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
        eiw!!.setWidgetXCoord2(String.format("%.6f", lat))
        eiw!!.setWidgetYCoord1(getString(R.string.ycoord))
        eiw!!.setWidgetYCoord2(String.format("%.6f", lon))
        eiw!!.setWidgetLocality1(getString(R.string.locality) + ":")

        if (metaPref)
            eiw!!.widgetLocality2 = sLocality
        else
            eiw!!.widgetLocality2 = "-"
        indivArea!!.addView(eiw)
    }
    // End of displayData()

    override fun onPause() {
        super.onPause()

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "259, onPause")

        // Close the data sources
        individualsDataSource!!.close()
        countDataSource!!.close()
    }

    override fun onStop() {
        super.onStop()

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "270, onStop")

        if (buttonSoundPref)
            soundService!!.releaseSoundP()

        if (awakePref) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "284, onDestroy")
    }

    private fun saveData(): Boolean {
        // Save individual data
        indivId = individualsDataSource!!.saveIndividual(
            individualsDataSource!!.createIndividuals(
                countId,
                specName,
                lat,
                lon,
                heightNN,
                uncert,
                dateStamp,
                timeStamp,
                eiw!!.widgetLocality2,
                code
            )
        )
        individuals = individualsDataSource!!.getIndividual(indivId)

        // Locality (from reverse geocoding in CountingActivity or manual input)
        individuals!!.locality = eiw!!.widgetLocality2

        // Uncertainty
        if (lat != 0.0) {
            individuals!!.uncert = uncert
        } else {
            individuals!!.uncert = "0"
        }

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
                Toast.makeText( // orange
                    applicationContext,
                    fromHtml("<font color='#ff6000'><b>$mesg</b></font>"),
                    Toast.LENGTH_LONG
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
                    counts!!.count_f1i += newCount
                    individuals!!.icount = newCount
                    individuals!!.sex = "-"
                    individuals!!.icategory = 1
                    countDataSource!!.saveCountf1i(counts!!)
                }

                2 -> {
                    // ♂
                    counts!!.count_f2i += newCount
                    individuals!!.icount = newCount
                    individuals!!.sex = "m"
                    individuals!!.icategory = 2
                    countDataSource!!.saveCountf2i(counts!!)
                }

                3 -> {
                    // ♀
                    counts!!.count_f3i += newCount
                    individuals!!.icount = newCount
                    individuals!!.sex = "f"
                    individuals!!.icategory = 3
                    countDataSource!!.saveCountf3i(counts!!)
                }

                4 -> {
                    // Pupa
                    counts!!.count_pi += newCount
                    individuals!!.icount = newCount
                    individuals!!.sex = "-"
                    individuals!!.icategory = 4
                    countDataSource!!.saveCountpi(counts!!)
                }

                5 -> {
                    // Larva
                    counts!!.count_li += newCount
                    individuals!!.icount = newCount
                    individuals!!.sex = "-"
                    individuals!!.icategory = 5
                    countDataSource!!.saveCountli(counts!!)
                }

                6 -> {
                    // Egg
                    counts!!.count_ei += newCount
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
            Toast.makeText( // orange
                this,
                fromHtml("<font color='#ff6000'><b>$mesg</b></font>"),
                Toast.LENGTH_LONG
            ).show()
            return false // forces input newCount > 0
        }
        return true
    }
    // End of saveData()

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
