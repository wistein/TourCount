package com.wmstein.tourcount

import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.content.pm.PackageManager
import android.database.CursorIndexOutOfBoundsException
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.PowerManager.WakeLock
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.MediaStore
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast

import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams

import com.wmstein.tourcount.TourCountApplication.Companion.getPrefs
import com.wmstein.tourcount.Utils.fromHtml
import com.wmstein.tourcount.database.Count
import com.wmstein.tourcount.database.CountDataSource
import com.wmstein.tourcount.database.IndividualsDataSource
import com.wmstein.tourcount.database.Section
import com.wmstein.tourcount.database.SectionDataSource
import com.wmstein.tourcount.widgets.CountingHead1Widget
import com.wmstein.tourcount.widgets.CountingLHWidget
import com.wmstein.tourcount.widgets.CountingSpeciesNotesWidget
import com.wmstein.tourcount.widgets.CountingTourNotesWidget
import com.wmstein.tourcount.widgets.CountingWidget

import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Objects

/***************************************************************************************
 * CountingActivity is the central activity of TourCount.
 * It provides six counters, starts GPS-location polling, starts AddSpeciesActivity, DelSpeciesActivity,
 * EditSpeciesListActivity, EditSpeciesNotesActivity, EditTourNotesActivity and EditIndividualActivity,
 * switches screen off when the device is pocketed
 * and allows taking pictures and sending notes.
 *
 * CountingActivity uses CountingWidget.kt, CountingLHWidget.kt, NotesWidget.kt,
 * activity_counting.xml and activity_counting_lh.xml
 *
 * Basic counting functions created by milo for BeeCount on 2014-05-05.
 * Adopted, modified and enhanced for TourCount by wmstein since 2016-04-18,
 * last edited in Java on 2026-06-08,
 * converted to Kotlin on 2026-07-17,
 * last edited on 2026-07-21.
 */
class CountingActivity : AppCompatActivity(), OnSharedPreferenceChangeListener, SensorEventListener {
    private var countScreen: LinearLayout? = null
    private var countArea: LinearLayout? = null
    private var headArea: LinearLayout? = null
    private var notesArea: LinearLayout? = null
    
    // Data
    private var iid = 1
    private var count: Count? = null
    private var section: Section? = null
    private var spinner: Spinner? = null
    private var itemPosition = 0
    private var indID = 0 // Individuals table ID
    private var speciesName = ""
    private var specCnt = 0

    // CountingWidgets
    private lateinit var countingWidgets: MutableList<CountingWidget>
    private lateinit var countingWidgetsLH: MutableList<CountingLHWidget>

    // Proximity sensor handling screen on/off
    private var powerManager: PowerManager? = null
    private var sensorManager: SensorManager? = null
    private var proximityWakeLock: WakeLock? = null
    private var proximitySensor: Sensor? = null

    // Preferences
    private var prefs = getPrefs()
    private var editor = prefs.edit()
    private var awakePref = false // keep screen on
    private var brightPref = false // make screen bright
    private var sortPref = ""
    private var lhandPref = false // true for left hand mode of counting screen
    private var buttonSoundPref = false
    private var buttonVibPref = false
    private var specCode = "" // code of a species to be shown in the counting list
    private var proxSensorPref = ""
    private var sensorSensitivity = 0.0

    // Data sources
    private var sectionDataSource: SectionDataSource? = null
    private var countDataSource: CountDataSource? = null
    private var individualsDataSource: IndividualsDataSource? = null

    private lateinit var soundService: SoundService

    private lateinit var vibrator: Vibrator

    private var mesg = ""

    private val mHandler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "132, onCreate")

        val tourCount = application as TourCountApplication
        setPrefVariables() // set all stored preferences into their variables

        sectionDataSource = SectionDataSource(this)
        countDataSource = CountDataSource(this)
        individualsDataSource = IndividualsDataSource(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM)  // SDK 35+
            this.enableEdgeToEdge()

        // Distinguish between left-/ right-handed counting page layout
        if (lhandPref) {
            setContentView(R.layout.activity_counting_lh)
            countScreen = findViewById(R.id.countingScreenLH)
            countScreen!!.background = tourCount.setBackgr()
            countArea = findViewById(R.id.countCountiLayoutLH)
            notesArea = findViewById(R.id.tourNotesLayoutLH)
            headArea = findViewById(R.id.countHead2LayoutLH)

            ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.countingScreenLH))
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
        } else {
            setContentView(R.layout.activity_counting)
            countScreen = findViewById(R.id.countingScreen)
            countScreen!!.background = tourCount.setBackgr()
            countArea = findViewById(R.id.countCountiLayout)
            notesArea = findViewById(R.id.tourNotesLayout)
            headArea = findViewById(R.id.countHead2Layout)

            ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.countingScreen))
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
        }

        // Proximity sensor handling screen on/off
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        proximitySensor = sensorManager!!.getDefaultSensor(Sensor.TYPE_PROXIMITY)

        powerManager = this.getSystemService(POWER_SERVICE) as PowerManager
        proximityWakeLock = 
            if (powerManager!!.isWakeLockLevelSupported(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK)) 
                powerManager!!.newWakeLock(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK,
                "TourCount:WAKELOCK")
        else null

        // Get sensitivity range of proximity sensor
        val sensorSensitivityMax: Double =
            if (proximitySensor != null)
                proximitySensor!!.maximumRange.toDouble()
            else 0.0

        // Get proximity sensitivity selection from preferences
        if (sensorSensitivityMax != 0.0) {
            // Set sensorSensitivity proportional to value for max. sensitivity
            when (proxSensorPref) {
                "Off" -> sensorSensitivity = 0.0
                "Medium" -> sensorSensitivity = sensorSensitivityMax / 2
                "High" -> sensorSensitivity = sensorSensitivityMax - 0.1
            }
        }

        // Start button sound service
        if (buttonSoundPref) soundService = SoundService(applicationContext)

        // new onBackPressed logic
        // Different Navigation Bar modes and layouts:
        // - Classic three-button navigation: NavBarMode = 0
        // - Two-button navigation (Android P): NavBarMode = 1
        // - Full screen gesture mode (Android Q): NavBarMode = 2
        // Use only if NavBarMode = 0 or 1.
        if (this.navBarMode == 0 || this.navBarMode == 1) {
            val callback: OnBackPressedCallback = object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    disableProximitySensor()
                    finish()
                    remove()
                }
            }
            onBackPressedDispatcher.addCallback(this, callback)
        }
    }
    // End of onCreate()

    val navBarMode: Int
        get() {
            val resources = this.getResources()

            @SuppressLint("DiscouragedApi") val resourceId = resources.getIdentifier(
                "config_navBarInteractionMode",
                "integer", "android"
            )

            return if (resourceId > 0) resources.getInteger(resourceId) else 0
        }

    // Load preferences at start, and also when a change is detected
    private fun setPrefVariables() {
        awakePref = prefs.getBoolean("pref_awake", true) // keep screen on while counting
        brightPref = prefs.getBoolean("pref_bright", true) // bright counting page
        sortPref = prefs.getString("pref_sort_sp", "none")!! // sort mode of species list
        lhandPref = prefs.getBoolean("pref_left_hand", false) // left-handed counting page
        buttonSoundPref = prefs.getBoolean("pref_button_sound", false) // make button sound
        buttonVibPref = prefs.getBoolean("pref_button_vib", false) // make vibration
        itemPosition = prefs.getInt("item_Position", 0) // item position in spinner
        iid = prefs.getInt("count_id", 1) // species id
        proxSensorPref = prefs.getString("pref_prox", "Off")!!
    }

    @SuppressLint("DiscouragedApi")
    override fun onResume() {
        super.onResume()

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "264, onResume")

        sensorManager!!.registerListener(this,
            proximitySensor, SensorManager.SENSOR_DELAY_NORMAL)

        prefs = getPrefs()
        prefs.registerOnSharedPreferenceChangeListener(this)
        setPrefVariables() // set prefs into their variables

        // Prepare vibrator service
        if (buttonVibPref) vibrator =
            applicationContext.getSystemService(Vibrator::class.java)

        // Set full brightness of screen
        if (brightPref) {
            val params = window.attributes
            params.screenBrightness = 1.0f
            window.attributes = params
        }

        if (awakePref) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Prepare the counting screen
        //   Clear any existing views
        countArea!!.removeAllViews()
        notesArea!!.removeAllViews()
        headArea!!.removeAllViews()

        // Set up the data sources
        sectionDataSource!!.open()
        countDataSource!!.open()
        individualsDataSource!!.open()

        // Load the section data
        try {
            section = sectionDataSource!!.section
        } catch (_: CursorIndexOutOfBoundsException) {
            mesg = getString(R.string.getHelp)
            Toast.makeText(this,
                fromHtml("<font color='red'><b>$mesg</b></font>"),
                Toast.LENGTH_LONG).show()
            disableProximitySensor()
            finish()
        }

        // Load and show the data,
        //   set title in ActionBar
        supportActionBar!!.title = section!!.name
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        val idArray: Array<String?>?
        val nameArray: Array<String?>?
        val nameArrayG: Array<String?>?
        val codeArray: Array<String?>?

        when (sortPref) {
            "names_alpha" -> {
                idArray = countDataSource!!.allIdsSrtName
                nameArray = countDataSource!!.getAllStringsSrtName("name")
                codeArray = countDataSource!!.getAllStringsSrtName("code")
                nameArrayG = countDataSource!!.getAllStringsSrtName("name_g")
            }

            "codes" -> {
                idArray = countDataSource!!.allIdsSrtCode
                nameArray = countDataSource!!.getAllStringsSrtCode("name")
                codeArray = countDataSource!!.getAllStringsSrtCode("code")
                nameArrayG = countDataSource!!.getAllStringsSrtCode("name_g")
            }

            else -> {
                idArray = countDataSource!!.allIds
                nameArray = countDataSource!!.getAllStrings("name")
                codeArray = countDataSource!!.getAllStrings("code")
                nameArrayG = countDataSource!!.getAllStrings("name_g")
            }
        }

        var rName: String
        var resId: Int
        val resId0 = getResources().getIdentifier(
            "p00000", "drawable",this.packageName)

        val imageArray = arrayOfNulls<Int>(codeArray.size)
        for ((iPic, code) in codeArray.withIndex()) {
            rName = "p$code"
            resId = getResources().getIdentifier(
                rName, "drawable",
                this.packageName
            )
            if (resId != 0) imageArray[iPic] = resId
            else imageArray[iPic] = resId0
        }

        countingWidgets = ArrayList()
        countingWidgetsLH = ArrayList()

        // Show head1: Species with spinner to select
        spinner = if (lhandPref)  // if left-handed counting page
            findViewById(R.id.countHead1SpinnerLH)
        else findViewById(R.id.countHead1Spinner)

        // Get itemPosition of the species to be shown from sharedPreference
        if (prefs.getString("new_spec_code", "")!! != "") {
            specCode = prefs.getString("new_spec_code", "")!!
            editor.putString("new_spec_code", "") // clear prefs value after use
            editor.apply()
        }

        if (specCode != "") {
            var i = 0
            while (i <= codeArray.size) {
                if (specCode == codeArray[i]) {
                    itemPosition = i
                    break
                }
                i++
            }
            specCode = ""
        }

        // Set spinner part of the counting screen by itemPosition
        @Suppress("UNCHECKED_CAST") val adapter = CountingHead1Widget(
            this,
            idArray, nameArray, nameArrayG, codeArray, imageArray as Array<Int>
        )
        spinner!!.adapter = adapter
        spinner!!.setSelection(itemPosition)
        spinnerListener()
    }
    // End of onResume()

    // Watch proximity sensor
    override fun onSensorChanged(event: SensorEvent) {
        if (proximitySensor != null) {
            if (event.sensor.type == Sensor.TYPE_PROXIMITY) {

                // if ([0|5] >= [0|-2.5|-4.9] && [0|5] < [0|2.5|4.9])
                val sensi = event.values[0]
                if (sensi >= -sensorSensitivity && sensi < sensorSensitivity) {
                    // near
                    if (proximityWakeLock == null) 
                        proximityWakeLock = powerManager!!.newWakeLock(
                        PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK,
                        "TourCount:WAKELOCK")

                    if (!proximityWakeLock!!.isHeld)
                        proximityWakeLock!!.acquire(30 * 60 * 1000L) // 30 minutes
                } else {
                    // far
                    disableProximitySensor()
                }
            }
        }
    }

    // Necessary for SensorEventListener
    override fun onAccuracyChanged(sensor: Sensor?, i: Int) {
    }

    private fun disableProximitySensor() // far
    {
        if (proximityWakeLock == null) return
        if (proximityWakeLock!!.isHeld) {
            val flags = PowerManager.RELEASE_FLAG_WAIT_FOR_NO_PROXIMITY
            proximityWakeLock!!.release(flags)
            proximityWakeLock = null
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.counting, menu)
        return true
    }

    // Handle menu selections
    @SuppressLint("QueryPermissionsNeeded")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home  // back button in actionBar
                -> {
                disableProximitySensor()

                finish()
                return true
            }

            R.id.menuAddSpecies -> {
                disableProximitySensor()

                val intent = Intent(
                    this@CountingActivity,
                    AddSpeciesActivity::class.java
                )
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                startActivity(intent)
                return true
            }

            R.id.menuDelSpecies -> {
                disableProximitySensor()

                mesg = getString(R.string.wait)
                Toast.makeText(
                    this,
                    fromHtml("<font color='blue'>$mesg</font>"),
                    Toast.LENGTH_SHORT
                ).show()

                val intent = Intent(this@CountingActivity, DelSpeciesActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)

                // Trick: Pause for 100 msec to show toast
                mHandler.postDelayed({ startActivity(intent) }, 100)
                return true
            }

            R.id.menuEditSection -> {
                disableProximitySensor()

                mesg = getString(R.string.wait)
                Toast.makeText(
                    this,
                    fromHtml("<font color='blue'>$mesg</font>"),
                    Toast.LENGTH_SHORT
                ).show()

                val intent = Intent(this@CountingActivity, EditSpeciesListActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)

                // Trick: Pause for 100 msec to show toast
                mHandler.postDelayed({ startActivity(intent) }, 100)
                return true
            }

            R.id.menuTakePhoto -> {
                val camIntent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)

                val packageManager = getPackageManager()
                val activities = packageManager.queryIntentActivities(
                    camIntent,
                    PackageManager.MATCH_DEFAULT_ONLY
                )

                // Select from available camera apps
                val isIntentSafe = !activities.isEmpty()
                if (isIntentSafe) {
                    val title = getResources().getString(R.string.chooserTitle)
                    val chooser = Intent.createChooser(camIntent, title)
                    if (camIntent.resolveActivity(getPackageManager()) != null) {
                        try {
                            startActivity(chooser)
                        } catch (_: Exception) {
                            mesg = getString(R.string.noPhotoPermit)
                            Toast.makeText(
                                this,
                                fromHtml("<font color='red'><b>$mesg</b></font>"),
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                } else {
                    // Only default camera available
                    startActivity(camIntent)
                }
                return true
            }

            R.id.action_share -> {
                val sendIntent = Intent()
                sendIntent.action = Intent.ACTION_SEND
                sendIntent.putExtra(Intent.EXTRA_SUBJECT, "TourCount")
                sendIntent.putExtra(Intent.EXTRA_TITLE, "Message of TourCount")
                sendIntent.putExtra(Intent.EXTRA_TEXT, section!!.name + ": ")
                sendIntent.type = "text/plain"
                startActivity(Intent.createChooser(sendIntent, getResources().getText(R.string.send_to)))
                return true
            }

            else -> return super.onOptionsItemSelected(item)
        }
    }
    // End of onOptionsItemSelected()

    // Edit species notes by EditSpeciesNotesActivity by button in widget_counting_species_notes.xml
    fun editSpeciesNotes(view: View?) {
        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "552, View: $view")

        disableProximitySensor()

        val intent = Intent(this@CountingActivity, EditSpeciesNotesActivity::class.java)
        intent.putExtra("count_id", iid)
        startActivity(intent)
    }

    // Edit tour notes by EditTourNotesActivity by button in widget_counting_tour_notes.xml
    fun editTourNotes(view: View?) {
        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "564, View: $view")

        disableProximitySensor()

        val intent = Intent(this@CountingActivity, EditTourNotesActivity::class.java)
        intent.putExtra("count_id", iid)
        startActivity(intent)
    }

    override fun onSharedPreferenceChanged(prefs: SharedPreferences, key: String?) {
        setPrefVariables()
    }

    override fun onPause() {
        super.onPause()

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "581, onPause")

        disableProximitySensor()

        // save current count id in case it is lost on pause
        editor.putInt("count_id", iid)
        editor.putInt("item_Position", itemPosition)
        editor.apply()

        // close the data sources
        sectionDataSource!!.close()
        countDataSource!!.close()
        individualsDataSource!!.close()

        // On some Custom ROMS a wakelock might not be held, if wakelock permission is not granted
        if (awakePref) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }

        prefs.unregisterOnSharedPreferenceChangeListener(this)
        sensorManager!!.unregisterListener(this)
    }
    // End of onPause()

    public override fun onStop() {
        super.onStop()

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "609, onStop")

        countScreen!!.invalidate()

        // Stop sound service when denied in settings
        if (buttonSoundPref) {
            soundService.releaseSoundM()
            soundService.releaseSoundP()
        }
    }

    public override fun onDestroy() {
        super.onDestroy()

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "624, onDestroy")
    }

    // Spinner listener
    private fun spinnerListener() {
        spinner!!.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View,
                position: Int,
                aid: Long
            ) {
                try {
                    headArea!!.removeAllViews()
                    countArea!!.removeAllViews()
                    notesArea!!.removeAllViews()

                    // Get species id
                    val sid =
                        (view.findViewById<View?>(R.id.countId) as TextView).text.toString()
                    iid = sid.toInt()
                    itemPosition = position

                    count = countDataSource!!.getCountById(iid)
                    countingScreen(count!!)
                    if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                        Log.i(TAG, ("650, SpinnerListener, count id: " + count!!.id
                                + ", code: " + count!!.code + ", name: " + count!!.name))
                } catch (e: Exception) {
                    // Exception may occur when permissions are changed while activity is paused
                    //  or when spinner is rapidly repeatedly pressed
                    if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                        Log.e(TAG,"656, SpinnerListener: $e"
                    )
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // stub, necessary to make Spinner work correctly when repeatedly used
            }
        }
    }

    // Show rest of widgets for counting screen
    private fun countingScreen(count: Count) {
        // 1. Species line with Spinner is set by CountingHead1Widget in onResume

        // 2. Counting Area
        if (lhandPref) { // if left-handed counting page
            val widgetc = CountingLHWidget(this, null)
            widgetc.setCount(count)
            countingWidgetsLH.add(widgetc)
            countArea!!.addView(widgetc)
        } else {
            val widgetc = CountingWidget(this, null)
            widgetc.setCount(count)
            countingWidgets.add(widgetc)
            countArea!!.addView(widgetc)
        }

        // 3. Species notes with edit button
        val widgets = CountingSpeciesNotesWidget(this, null)
        widgets.setCountHead2(count)
        headArea!!.addView(widgets)

        // 4. Tour notes with edit button
        val widgett = CountingTourNotesWidget(this, null)
        widgett.setTourNotes(section!!)
        notesArea!!.addView(widgett)
    }
    // End of countingScreen

    // Get the referenced counting widgets
    // CountingWidget (right-handed)
    private fun getCountFromId(id: Int): CountingWidget? {
        for (widget in countingWidgets) {
            checkNotNull(widget.count)
            if (widget.count!!.id == id) return widget
        }
        return null
    }

    // CountingWidget (left-handed)
    private fun getCountFromIdLH(id: Int): CountingLHWidget? {
        for (widget in countingWidgetsLH) {
            checkNotNull(widget.count)
            if (widget.count!!.id == id) return widget
        }
        return null
    }

    /********************************************************
     * The functions below are triggered by the count buttons
     * on the righthand/lefthand (LH) views.
     *
     * For up-counting they get locality and start EditIndividualActivity,
     * down-counting is done directly
     */
    // Triggered by count up button for ♂|♀
    fun countUpf1i(view: View) {
        val tempCountId = view.tag.toString().toInt()
        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "726, countUpf1i, count Id: $tempCountId")

        // iAtt used by EditIndividualActivity to decide where to store bulk count value
        val iAtt = 1 // 1 f1i, 2 f2i, 3 f3i, 4 pi, 5 li, 6 ei

        val widget = getCountFromId(tempCountId)
        widget?.countUpf1i()

        disableProximitySensor() // for EditIndividualActivity

        Objects.requireNonNull<CountingWidget>(widget).count

        // Provide edited info for individual and start EditIndividualActivity
        val intent = Intent(this@CountingActivity, EditIndividualActivity::class.java)
        intent.putExtra("count_id", tempCountId)
        intent.putExtra("SName", widget!!.count!!.name)
        intent.putExtra("SCode", widget.count!!.code)
        intent.putExtra("date", getcurDate())
        intent.putExtra("time", getcurTime())
        intent.putExtra("indivAtt", iAtt)
        startActivity(intent)
    }

    // Triggered by count up button from left-hand view for ♂|♀
    fun countUpLHf1i(view: View) {
        val iAtt = 1
        val tempCountId = view.tag.toString().toInt()
        val widget = getCountFromIdLH(tempCountId)
        widget?.countUpLHf1i()

        disableProximitySensor() // for EditIndividualActivity

        Objects.requireNonNull<CountingLHWidget>(widget).count

        val intent = Intent(this@CountingActivity, EditIndividualActivity::class.java)
        intent.putExtra("count_id", tempCountId)
        intent.putExtra("SName", widget!!.count!!.name)
        intent.putExtra("SCode", widget.count!!.code)
        intent.putExtra("date", getcurDate())
        intent.putExtra("time", getcurTime())
        intent.putExtra("indivAtt", iAtt)
        startActivity(intent)
    }

    // Triggered by count down button to decrease count for ♂|♀ if > 0
    fun countDownf1i(view: View) {
        if (buttonSoundPref) soundService.soundMinusButtonSound()
        buttonVib()

        val tempCountId = view.tag.toString().toInt()
        val widget = getCountFromId(tempCountId)
        Objects.requireNonNull<CountingWidget>(widget).count

        speciesName = widget!!.count!!.name // set speciesName for toast in deleteIndividual
        specCnt = widget.count!!.count_f1i
        if (specCnt > 0) {
            widget.countDownf1i() // decrease species counter
            countDataSource!!.saveCountf1i(count!!)

            // get last individual of category 1 (♂|♀)
            indID = individualsDataSource!!.getLastIndiv(tempCountId, 1)
            if (indID == -1) {
                mesg = getString(R.string.getHelp) + speciesName
                Toast.makeText(this,
                    fromHtml("<font color='red'><b>$mesg</b></font>"),
                    Toast.LENGTH_LONG).show()
                return
            }
            val icount = individualsDataSource!!.getIndividualCount(indID)
            if (indID > 0 && icount < 2) {
                individualsDataSource!!.deleteIndividualById(indID)
                indID--
                return
            }
            if (indID > 0) {
                val icount1 = icount - 1
                individualsDataSource!!.decreaseIndividual(indID, icount1)
            }
        }
    }

    // Triggered by count down button from left-hand view to decrease count for ♂|♀ if > 0
    fun countDownLHf1i(view: View) {
        if (buttonSoundPref) soundService.soundMinusButtonSound()
        buttonVib()

        val tempCountId = view.tag.toString().toInt()
        val widget = getCountFromIdLH(tempCountId)
        Objects.requireNonNull<CountingLHWidget>(widget).count

        speciesName = widget!!.count!!.name // set speciesName for toast in deleteIndividual
        specCnt = widget.count!!.count_f1i
        if (specCnt > 0) {
            widget.countDownLHf1i()
            countDataSource!!.saveCountf1i(count!!)

            indID = individualsDataSource!!.getLastIndiv(tempCountId, 1)
            if (indID == -1) {
                mesg = getString(R.string.getHelp) + speciesName
                Toast.makeText(this,
                    fromHtml("<font color='red'><b>$mesg</b></font>"),
                    Toast.LENGTH_LONG).show()
                return
            }
            val icount = individualsDataSource!!.getIndividualCount(indID)
            if (indID > 0 && icount < 2) {
                individualsDataSource!!.deleteIndividualById(indID)
                indID--
                return
            }
            if (indID > 0) {
                val icount1 = icount - 1
                individualsDataSource!!.decreaseIndividual(indID, icount1)
            }
        }
    }

    // Triggered by count up button for ♂ and starts EditIndividualActivity
    fun countUpf2i(view: View) {
        // iAtt used by EditIndividualActivity to decide where to store bulk count value
        val iAtt = 2 // 1 f1i, 2 f2i, 3 f3i, 4 pi, 5 li, 6 ei

        val tempCountId = view.tag.toString().toInt()
        val widget = getCountFromId(tempCountId)
        widget?.countUpf2i()

        disableProximitySensor()

        Objects.requireNonNull<CountingWidget>(widget).count

        // get edited info for individual and start EditIndividualActivity
        val intent = Intent(this@CountingActivity, EditIndividualActivity::class.java)
        intent.putExtra("count_id", tempCountId)
        intent.putExtra("SName", widget!!.count!!.name)
        intent.putExtra("SCode", widget.count!!.code)
        intent.putExtra("date", getcurDate())
        intent.putExtra("time", getcurTime())
        intent.putExtra("indivAtt", iAtt)
        startActivity(intent)
    }

    // Triggered by count up button from left-hand view for ♂ and starts EditIndividualActivity
    fun countUpLHf2i(view: View) {
        val iAtt = 2
        val tempCountId = view.tag.toString().toInt()
        val widget = getCountFromIdLH(tempCountId)
        widget?.countUpLHf2i()

        disableProximitySensor()

        Objects.requireNonNull<CountingLHWidget>(widget).count

        // get edited info for individual and start EditIndividualActivity
        val intent = Intent(this@CountingActivity, EditIndividualActivity::class.java)
        intent.putExtra("count_id", tempCountId)
        intent.putExtra("SName", widget!!.count!!.name)
        intent.putExtra("SCode", widget.count!!.code)
        intent.putExtra("date", getcurDate())
        intent.putExtra("time", getcurTime())
        intent.putExtra("indivAtt", iAtt)
        startActivity(intent)
    }

    // Triggered by count down button to decrease count for ♂ if > 0
    fun countDownf2i(view: View) {
        if (buttonSoundPref) soundService.soundMinusButtonSound()
        buttonVib()

        val tempCountId = view.tag.toString().toInt()
        val widget = getCountFromId(tempCountId)
        Objects.requireNonNull<CountingWidget>(widget).count

        speciesName = widget!!.count!!.name // set speciesName for toast in deleteIndividual
        specCnt = widget.count!!.count_f2i

        if (specCnt > 0) {
            widget.countDownf2i()
            countDataSource!!.saveCountf2i(count!!)

            indID = individualsDataSource!!.getLastIndiv(tempCountId, 2)
            if (indID == -1) {
                mesg = getString(R.string.getHelp) + speciesName
                Toast.makeText(this,
                    fromHtml("<font color='red'><b>$mesg</b></font>"),
                    Toast.LENGTH_LONG).show()
                return
            }
            val icount = individualsDataSource!!.getIndividualCount(indID)
            if (indID > 0 && icount < 2) {
                individualsDataSource!!.deleteIndividualById(indID)
                indID--
                return
            }
            if (indID > 0) {
                val icount1 = icount - 1
                individualsDataSource!!.decreaseIndividual(indID, icount1)
            }
        }
    }

    // Triggered by count down button from left-hand view to decrease count for ♂ if > 0
    fun countDownLHf2i(view: View) {
        if (buttonSoundPref) soundService.soundMinusButtonSound()
        buttonVib()

        val tempCountId = view.tag.toString().toInt()
        val widget = getCountFromIdLH(tempCountId)
        Objects.requireNonNull<CountingLHWidget>(widget).count

        speciesName = widget!!.count!!.name // set speciesName for toast in deleteIndividual
        specCnt = widget.count!!.count_f2i

        if (specCnt > 0) {
            widget.countDownLHf2i()
            countDataSource!!.saveCountf2i(count!!)

            indID = individualsDataSource!!.getLastIndiv(tempCountId, 2)
            if (indID == -1) {
                mesg = getString(R.string.getHelp) + speciesName
                Toast.makeText(this,
                    fromHtml("<font color='red'><b>$mesg</b></font>"),
                    Toast.LENGTH_LONG).show()
                return
            }
            val icount = individualsDataSource!!.getIndividualCount(indID)
            if (indID > 0 && icount < 2) {
                individualsDataSource!!.deleteIndividualById(indID)
                indID--
                return
            }
            if (indID > 0) {
                val icount1 = icount - 1
                individualsDataSource!!.decreaseIndividual(indID, icount1)
            }
        }
    }

    // Triggered by count up button for ♀ and starts EditIndividualActivity
    fun countUpf3i(view: View) {
        // iAtt used by EditIndividualActivity to decide where to store bulk count value
        val iAtt = 3 // 1 f1i, 2 f2i, 3 f3i, 4 pi, 5 li, 6 ei

        val tempCountId = view.tag.toString().toInt()
        val widget = getCountFromId(tempCountId)
        widget?.countUpf3i()

        disableProximitySensor()

        Objects.requireNonNull<CountingWidget>(widget).count

        // get edited info for individual and start EditIndividualActivity
        val intent = Intent(this@CountingActivity, EditIndividualActivity::class.java)
        intent.putExtra("count_id", tempCountId)
        intent.putExtra("SName", widget!!.count!!.name)
        intent.putExtra("SCode", widget.count!!.code)
        intent.putExtra("date", getcurDate())
        intent.putExtra("time", getcurTime())
        intent.putExtra("indivAtt", iAtt)
        startActivity(intent)
    }

    // Triggered by count up button from left-hand view for ♀ and starts EditIndividualActivity
    fun countUpLHf3i(view: View) {
        val iAtt = 3
        val tempCountId = view.tag.toString().toInt()
        val widget = getCountFromIdLH(tempCountId)
        widget?.countUpLHf3i()

        disableProximitySensor()

        Objects.requireNonNull<CountingLHWidget>(widget).count

        // get edited info for individual and start EditIndividualActivity
        val intent = Intent(this@CountingActivity, EditIndividualActivity::class.java)
        intent.putExtra("count_id", tempCountId)
        intent.putExtra("SName", widget!!.count!!.name)
        intent.putExtra("SCode", widget.count!!.code)
        intent.putExtra("date", getcurDate())
        intent.putExtra("time", getcurTime())
        intent.putExtra("indivAtt", iAtt)
        startActivity(intent)
    }

    // Triggered by count down button to decrease count for ♀ if > 0
    fun countDownf3i(view: View) {
        if (buttonSoundPref) soundService.soundMinusButtonSound()
        buttonVib()

        val tempCountId = view.tag.toString().toInt()
        val widget = getCountFromId(tempCountId)
        Objects.requireNonNull<CountingWidget>(widget).count

        speciesName = widget!!.count!!.name // set speciesName for toast in deleteIndividual
        specCnt = widget.count!!.count_f3i

        if (specCnt > 0) {
            widget.countDownf3i()
            countDataSource!!.saveCountf3i(count!!)

            indID = individualsDataSource!!.getLastIndiv(tempCountId, 3)
            if (indID == -1) {
                mesg = getString(R.string.getHelp) + speciesName
                Toast.makeText(this,
                    fromHtml("<font color='red'><b>$mesg</b></font>"),
                    Toast.LENGTH_LONG).show()
                return
            }
            val icount = individualsDataSource!!.getIndividualCount(indID)
            if (indID > 0 && icount < 2) {
                individualsDataSource!!.deleteIndividualById(indID)
                indID--
                return
            }
            if (indID > 0) {
                val icount1 = icount - 1
                individualsDataSource!!.decreaseIndividual(indID, icount1)
            }
        }
    }

    // Triggered by count down button from left-hand view to decrease count for ♀ if > 0
    fun countDownLHf3i(view: View) {
        if (buttonSoundPref) soundService.soundMinusButtonSound()
        buttonVib()

        val tempCountId = view.tag.toString().toInt()
        val widget = getCountFromIdLH(tempCountId)
        Objects.requireNonNull<CountingLHWidget>(widget).count

        speciesName = widget!!.count!!.name // set speciesName for toast in deleteIndividual
        specCnt = widget.count!!.count_f3i

        if (specCnt > 0) {
            widget.countDownLHf3i()
            countDataSource!!.saveCountf3i(count!!)

            indID = individualsDataSource!!.getLastIndiv(tempCountId, 3)
            if (indID == -1) {
                mesg = getString(R.string.getHelp) + speciesName
                Toast.makeText(this,
                    fromHtml("<font color='red'><b>$mesg</b></font>"),
                    Toast.LENGTH_LONG).show()
                return
            }
            val icount = individualsDataSource!!.getIndividualCount(indID)
            if (indID > 0 && icount < 2) {
                individualsDataSource!!.deleteIndividualById(indID)
                indID--
                return
            }
            if (indID > 0) {
                val icount1 = icount - 1
                individualsDataSource!!.decreaseIndividual(indID, icount1)
            }
        }
    }

    // Triggered by count up button for pupa and starts EditIndividualActivity
    fun countUppi(view: View) {
        // iAtt used by EditIndividualActivity to decide where to store bulk count value
        val iAtt = 4 // 1 f1i, 2 f2i, 3 f3i, 4 pi, 5 li, 6 ei

        val tempCountId = view.tag.toString().toInt()
        val widget = getCountFromId(tempCountId)
        widget?.countUppi()

        disableProximitySensor()

        Objects.requireNonNull<CountingWidget>(widget).count

        // get edited info for individual and start EditIndividualActivity
        val intent = Intent(this@CountingActivity, EditIndividualActivity::class.java)
        intent.putExtra("count_id", tempCountId)
        intent.putExtra("SName", widget!!.count!!.name)
        intent.putExtra("SCode", widget.count!!.code)
        intent.putExtra("date", getcurDate())
        intent.putExtra("time", getcurTime())
        intent.putExtra("indivAtt", iAtt)
        startActivity(intent)
    }

    // Triggered by count up button from left-hand view for pupa and starts EditIndividualActivity
    fun countUpLHpi(view: View) {
        val iAtt = 4
        val tempCountId = view.tag.toString().toInt()
        val widget = getCountFromIdLH(tempCountId)
        widget?.countUpLHpi()

        disableProximitySensor()

        Objects.requireNonNull<CountingLHWidget>(widget).count

        // get edited info for individual and start EditIndividualActivity
        val intent = Intent(this@CountingActivity, EditIndividualActivity::class.java)
        intent.putExtra("count_id", tempCountId)
        intent.putExtra("SName", widget!!.count!!.name)
        intent.putExtra("SCode", widget.count!!.code)
        intent.putExtra("date", getcurDate())
        intent.putExtra("time", getcurTime())
        intent.putExtra("indivAtt", iAtt)
        startActivity(intent)
    }

    // Triggered by count down button to decrease count for pupa if > 0
    fun countDownpi(view: View) {
        if (buttonSoundPref) soundService.soundMinusButtonSound()
        buttonVib()

        val tempCountId = view.tag.toString().toInt()
        val widget = getCountFromId(tempCountId)
        Objects.requireNonNull<CountingWidget>(widget).count

        speciesName = widget!!.count!!.name // set speciesName for toast in deleteIndividual
        specCnt = widget.count!!.count_pi

        if (specCnt > 0) {
            widget.countDownpi()
            countDataSource!!.saveCountpi(count!!)

            indID = individualsDataSource!!.getLastIndiv(tempCountId, 4)
            if (indID == -1) {
                mesg = getString(R.string.getHelp) + speciesName
                Toast.makeText(this,
                    fromHtml("<font color='red'><b>$mesg</b></font>"),
                    Toast.LENGTH_LONG).show()
                return
            }
            val icount = individualsDataSource!!.getIndividualCount(indID)
            if (indID > 0 && icount < 2) {
                individualsDataSource!!.deleteIndividualById(indID)
                indID--
                return
            }
            if (indID > 0) {
                val icount1 = icount - 1
                individualsDataSource!!.decreaseIndividual(indID, icount1)
            }
        }
    }

    // Triggered by count down button from left-hand view to decrease count for pupa if > 0
    fun countDownLHpi(view: View) {
        if (buttonSoundPref) soundService.soundMinusButtonSound()
        buttonVib()

        val tempCountId = view.tag.toString().toInt()
        val widget = getCountFromIdLH(tempCountId)
        Objects.requireNonNull<CountingLHWidget>(widget).count

        speciesName = widget!!.count!!.name // set speciesName for toast in deleteIndividual
        specCnt = widget.count!!.count_pi

        if (specCnt > 0) {
            widget.countDownLHpi()
            countDataSource!!.saveCountpi(count!!)

            indID = individualsDataSource!!.getLastIndiv(tempCountId, 4)
            if (indID == -1) {
                mesg = getString(R.string.getHelp) + speciesName
                Toast.makeText(this,
                    fromHtml("<font color='red'><b>$mesg</b></font>"),
                    Toast.LENGTH_LONG).show()
                return
            }
            val icount = individualsDataSource!!.getIndividualCount(indID)
            if (indID > 0 && icount < 2) {
                individualsDataSource!!.deleteIndividualById(indID)
                indID--
                return
            }
            if (indID > 0) {
                val icount1 = icount - 1
                individualsDataSource!!.decreaseIndividual(indID, icount1)
            }
        }
    }

    // Triggered by count up button for larva and starts EditIndividualActivity
    fun countUpli(view: View) {
        // iAtt used by EditIndividualActivity to decide where to store bulk count value
        val iAtt = 5 // 1 f1i, 2 f2i, 3 f3i, 4 pi, 5 li, 6 ei

        val tempCountId = view.tag.toString().toInt()
        val widget = getCountFromId(tempCountId)
        widget?.countUpli()

        disableProximitySensor()

        Objects.requireNonNull<CountingWidget>(widget).count

        // get edited info for individual and start EditIndividualActivity
        val intent = Intent(this@CountingActivity, EditIndividualActivity::class.java)
        intent.putExtra("count_id", tempCountId)
        intent.putExtra("SName", widget!!.count!!.name)
        intent.putExtra("SCode", widget.count!!.code)
        intent.putExtra("date", getcurDate())
        intent.putExtra("time", getcurTime())
        intent.putExtra("indivAtt", iAtt)
        startActivity(intent)
    }

    // Triggered by count up button from left-hand view for larva and starts EditIndividualActivity
    fun countUpLHli(view: View) {
        val iAtt = 5
        val tempCountId = view.tag.toString().toInt()
        val widget = getCountFromIdLH(tempCountId)
        widget?.countUpLHli()

        disableProximitySensor()

        Objects.requireNonNull<CountingLHWidget>(widget).count

        // get edited info for individual and start EditIndividualActivity
        val intent = Intent(this@CountingActivity, EditIndividualActivity::class.java)
        intent.putExtra("count_id", tempCountId)
        intent.putExtra("SName", widget!!.count!!.name)
        intent.putExtra("SCode", widget.count!!.code)
        intent.putExtra("date", getcurDate())
        intent.putExtra("time", getcurTime())
        intent.putExtra("indivAtt", iAtt)
        startActivity(intent)
    }

    // Triggered by count down button to decrease count for larva if > 0
    fun countDownli(view: View) {
        if (buttonSoundPref) soundService.soundMinusButtonSound()
        buttonVib()

        val tempCountId = view.tag.toString().toInt()
        val widget = getCountFromId(tempCountId)
        Objects.requireNonNull<CountingWidget>(widget).count

        speciesName = widget!!.count!!.name // set speciesName for toast in deleteIndividual
        specCnt = widget.count!!.count_li

        if (specCnt > 0) {
            widget.countDownli()
            countDataSource!!.saveCountli(count!!)

            indID = individualsDataSource!!.getLastIndiv(tempCountId, 5)
            if (indID == -1) {
                mesg = getString(R.string.getHelp) + speciesName
                Toast.makeText(this,
                    fromHtml("<font color='red'><b>$mesg</b></font>"),
                    Toast.LENGTH_LONG).show()
                return
            }
            val icount = individualsDataSource!!.getIndividualCount(indID)
            if (indID > 0 && icount < 2) {
                individualsDataSource!!.deleteIndividualById(indID)
                indID--
                return
            }
            if (indID > 0) {
                val icount1 = icount - 1
                individualsDataSource!!.decreaseIndividual(indID, icount1)
            }
        }
    }

    // Triggered by count down button from left-hand view to decrease count for larva if > 0
    fun countDownLHli(view: View) {
        if (buttonSoundPref) soundService.soundMinusButtonSound()
        buttonVib()

        val tempCountId = view.tag.toString().toInt()
        val widget = getCountFromIdLH(tempCountId)
        Objects.requireNonNull<CountingLHWidget>(widget).count

        speciesName = widget!!.count!!.name // set speciesName for toast in deleteIndividual
        specCnt = widget.count!!.count_li

        if (specCnt > 0) {
            widget.countDownLHli()
            countDataSource!!.saveCountli(count!!)

            indID = individualsDataSource!!.getLastIndiv(tempCountId, 5)
            if (indID == -1) {
                mesg = getString(R.string.getHelp) + speciesName
                Toast.makeText(this,
                    fromHtml("<font color='red'><b>$mesg</b></font>"),
                    Toast.LENGTH_LONG).show()
                return
            }
            val icount = individualsDataSource!!.getIndividualCount(indID)
            if (indID > 0 && icount < 2) {
                individualsDataSource!!.deleteIndividualById(indID)
                indID--
                return
            }
            if (indID > 0) {
                val icount1 = icount - 1
                individualsDataSource!!.decreaseIndividual(indID, icount1)
            }
        }
    }

    // Triggered by count up button for egg and starts EditIndividualActivity
    fun countUpei(view: View) {
        // iAtt used by EditIndividualActivity to decide where to store bulk count value
        val iAtt = 6 // 1 f1i, 2 f2i, 3 f3i, 4 pi, 5 li, 6 ei

        val tempCountId = view.tag.toString().toInt()
        val widget = getCountFromId(tempCountId)
        widget?.countUpei()

        disableProximitySensor()

        Objects.requireNonNull<CountingWidget>(widget).count

        // Get edited info for individual and start EditIndividualActivity
        val intent = Intent(this@CountingActivity, EditIndividualActivity::class.java)
        intent.putExtra("count_id", tempCountId)
        intent.putExtra("SName", widget!!.count!!.name)
        intent.putExtra("SCode", widget.count!!.code)
        intent.putExtra("date", getcurDate())
        intent.putExtra("time", getcurTime())
        intent.putExtra("indivAtt", iAtt)
        startActivity(intent)
    }

    // Triggered by count up button from left-hand view for egg and starts EditIndividualActivity
    fun countUpLHei(view: View) {
        val iAtt = 6
        val tempCountId = view.tag.toString().toInt()
        val widget = getCountFromIdLH(tempCountId)
        widget?.countUpLHei()

        disableProximitySensor()

        Objects.requireNonNull<CountingLHWidget>(widget).count

        // get edited info for individual and start EditIndividualActivity
        val intent = Intent(this@CountingActivity, EditIndividualActivity::class.java)
        intent.putExtra("count_id", tempCountId)
        intent.putExtra("SName", widget!!.count!!.name)
        intent.putExtra("SCode", widget.count!!.code)
        intent.putExtra("date", getcurDate())
        intent.putExtra("time", getcurTime())
        intent.putExtra("indivAtt", iAtt)
        startActivity(intent)
    }

    // Triggered by count down button to decrease count for egg if > 0
    fun countDownei(view: View) {
        if (buttonSoundPref) soundService.soundMinusButtonSound()
        buttonVib()

        val tempCountId = view.tag.toString().toInt()
        val widget = getCountFromId(tempCountId)
        Objects.requireNonNull<CountingWidget>(widget).count

        speciesName = widget!!.count!!.name // set speciesName for toast in deleteIndividual
        specCnt = widget.count!!.count_ei

        if (specCnt > 0) {
            widget.countDownei()
            countDataSource!!.saveCountei(count!!)

            indID = individualsDataSource!!.getLastIndiv(tempCountId, 6)
            if (indID == -1) {
                mesg = getString(R.string.getHelp) + speciesName
                Toast.makeText(this,
                    fromHtml("<font color='red'><b>$mesg</b></font>"),
                    Toast.LENGTH_LONG).show()
                return
            }
            val icount = individualsDataSource!!.getIndividualCount(indID)
            if (indID > 0 && icount < 2) {
                individualsDataSource!!.deleteIndividualById(indID)
                indID--
                return
            }
            if (indID > 0) {
                val icount1 = icount - 1
                individualsDataSource!!.decreaseIndividual(indID, icount1)
            }
        }
    }

    // Triggered by count down button from left-hand view to decrease count for egg if > 0
    fun countDownLHei(view: View) {
        if (buttonSoundPref) soundService.soundMinusButtonSound()
        buttonVib()

        val tempCountId = view.tag.toString().toInt()

        val widget = getCountFromIdLH(tempCountId)
        Objects.requireNonNull<CountingLHWidget>(widget).count

        speciesName = widget!!.count!!.name // set speciesName for toast in deleteIndividual
        specCnt = widget.count!!.count_ei

        if (specCnt > 0) {
            widget.countDownLHei()
            countDataSource!!.saveCountei(count!!)

            indID = individualsDataSource!!.getLastIndiv(tempCountId, 6)
            if (indID == -1) {
                mesg = getString(R.string.getHelp) + speciesName
                Toast.makeText(this,
                    fromHtml("<font color='red'><b>$mesg</b></font>"),
                    Toast.LENGTH_LONG).show()
                return
            }
            val icount = individualsDataSource!!.getIndividualCount(indID)
            if (indID > 0 && icount < 2) {
                individualsDataSource!!.deleteIndividualById(indID)
                indID--
                return
            }
            if (indID > 0) {
                val icount1 = icount - 1
                individualsDataSource!!.decreaseIndividual(indID, icount1)
            }
        }
    }
    // End of counters

    // Date for date_stamp
    @SuppressLint("SimpleDateFormat")
    private fun getcurDate(): String {
        val date = Date()
        val dform: DateFormat?
        val lng = Locale.getDefault().toString().substring(0, 2)

        dform = when (lng) {
            "en" -> SimpleDateFormat("yyyy-MM-dd")
            "de" -> SimpleDateFormat("dd.MM.yyyy")
            else  // for fr, it and es
                -> SimpleDateFormat("dd/MM/yyyy")
        }
        return dform.format(date)
    }

    // Date for time_stamp
    private fun getcurTime(): String {
        val date = Date()
        @SuppressLint("SimpleDateFormat") val dform: DateFormat = SimpleDateFormat("HH:mm:ss")
        return dform.format(date)
    }

    private fun buttonVib() {
        if (buttonVibPref) {
            if (Build.VERSION.SDK_INT >= 31) { // S, Android 12
                vibrator.vibrate(VibrationEffect.createPredefined(
                    VibrationEffect.EFFECT_CLICK))
            } else {
                vibrator.vibrate(VibrationEffect.createOneShot(
                        450,
                        VibrationEffect.DEFAULT_AMPLITUDE
                    )
                )
                vibrator.cancel()
            }
        }
    }

    companion object {
        private const val TAG = "CountAct"
    }

}

