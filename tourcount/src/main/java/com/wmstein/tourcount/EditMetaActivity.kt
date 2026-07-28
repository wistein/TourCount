package com.wmstein.tourcount

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.app.DatePickerDialog.OnDateSetListener
import android.app.TimePickerDialog
import android.app.TimePickerDialog.OnTimeSetListener
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import android.widget.DatePicker
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.TimePicker
import android.widget.Toast

import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams

import com.wmstein.tourcount.TourCountApplication.Companion.getPrefs
import com.wmstein.tourcount.Utils.fromHtml
import com.wmstein.tourcount.database.Head
import com.wmstein.tourcount.database.HeadDataSource
import com.wmstein.tourcount.database.Section
import com.wmstein.tourcount.database.SectionDataSource
import com.wmstein.tourcount.widgets.EditMetaLocationWidget
import com.wmstein.tourcount.widgets.EditMetaTitleWidget
import com.wmstein.tourcount.widgets.EditMetaWidget

import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/*********************************************************************************
 * EditMetaActivity collects, partly edits and shows metadata for the current tour
 * 
 * Created by wmstein on 2016-04-19,
 * last edit in Java on 2026-07-01,
 * converted to kotlin on 2026-07-17,
 * last edited on 2026-07-21.
 */
class EditMetaActivity : AppCompatActivity() {
    // Preferences
    private val prefs = getPrefs()

    // Data from DB tables
    private var head: Head? = null
    private var section: Section? = null
    private var headDataSource: HeadDataSource? = null
    private var sectionDataSource: SectionDataSource? = null

    private var pdate: Calendar? = null
    private var ptime: Calendar? = null

    private var headArea: LinearLayout? = null
    private var sDate: TextView? = null
    private var sTime: TextView? = null
    private var eTime: TextView? = null

    private var emtw: EditMetaTitleWidget? = null
    private var emlw: EditMetaLocationWidget? = null
    private var emw: EditMetaWidget? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "78, onCreate")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM)  // SDK 35+
        {
            this.enableEdgeToEdge()
        }
        setContentView(R.layout.activity_edit_meta)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.editHeadScreen))
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

        // Option for full bright screen
        val brightPref = prefs.getBoolean("pref_bright", true)

        // Set full brightness of screen
        if (brightPref) {
            val params = window.attributes
            params.screenBrightness = 1.0f
            window.attributes = params
        }

        headArea = findViewById(R.id.edit_head)

        supportActionBar!!.title = getString(R.string.editHeadTitle)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        headDataSource = HeadDataSource(this)
        sectionDataSource = SectionDataSource(this)

        // New onBackPressed logic
        if (this.navBarMode == 0 || this.navBarMode == 1) {
            val callback: OnBackPressedCallback = object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                        Log.i(TAG,"121, handleOnBackPressed")

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

    override fun onResume() {
        super.onResume()

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "146, onResume")

        // Setup data sources
        headDataSource!!.open()
        sectionDataSource!!.open()

        // Build the Edit Metadata screen
        // Clear existing view
        headArea!!.removeAllViews()

        // Load head and metadata
        head = headDataSource!!.head
        section = sectionDataSource!!.section

        // Display editable list title, observer name and notes by EditMetaTitleWidget
        emtw = EditMetaTitleWidget(this, null)
        emtw!!.setWidgetTitle(getString(R.string.titleEdit))
        emtw!!.setWidgetOName1(getString(R.string.inspector) + ": ")
        emtw!!.setWidgetONotes1(getString(R.string.titleTourNotes))

        emtw!!.widgetName = section!!.name
        emtw!!.widgetOName2 = head!!.observer
        emtw!!.widgetONotes2 = section!!.notes
        headArea!!.addView(emtw)

        // Display the editable location data by EditMetaLocationWidget
        emlw = EditMetaLocationWidget(this, null)
        emlw!!.setWidgetCo1(getString(R.string.country) + ":")
        emlw!!.setWidgetState1(getString(R.string.bstate) + ":")
        emlw!!.setWidgetPlz1(getString(R.string.plz) + ":")
        emlw!!.setWidgetCity1(getString(R.string.city) + ":")
        emlw!!.setWidgetPlace1(getString(R.string.place) + ":")
        emlw!!.setWidgetLocality1(getString(R.string.slocality) + ":")

        emlw!!.widgetCo2 = section!!.country
        emlw!!.widgetState2 = section!!.b_state
        emlw!!.widgetPlz2 = section!!.plz
        emlw!!.widgetCity2 = section!!.city
        emlw!!.widgetPlace2 = section!!.place
        emlw!!.widgetLocality2 = section!!.st_locality
        headArea!!.addView(emlw)

        // Display the editable metadata by EditMetaWidget
        emw = EditMetaWidget(this, null)
        emw!!.setWidgetDate1(getString(R.string.date) + ":")
        emw!!.setWidgetStartTm1(getString(R.string.starttm))
        emw!!.setWidgetEndTm1(getString(R.string.endtm))

        emw!!.widgetDate2 = section!!.date
        emw!!.widgetStartTm2 = section!!.start_tm
        emw!!.widgetEndTm2 = section!!.end_tm

        emw!!.setWidgetTemp1(getString(R.string.temperature) + ":")
        emw!!.setWidgetWind1(getString(R.string.wind) + ":")
        emw!!.setWidgetClouds1(getString(R.string.clouds) + ":")

        emw!!.widgetTemp2 = section!!.tmp
        emw!!.widgetTemp3 = section!!.tmp_end
        emw!!.widgetWind2 = section!!.wind
        emw!!.widgetWind3 = section!!.wind_end
        emw!!.widgetClouds2 = section!!.clouds
        emw!!.widgetClouds3 = section!!.clouds_end
        headArea!!.addView(emw)

        // Check for focus
        val sName = section!!.name
        if (sName != "") {
            emw!!.requestFocus()
        } else {
            emtw!!.requestFocus()
        }

        pdate = Calendar.getInstance()
        ptime = Calendar.getInstance()

        sDate = findViewById(R.id.widgetDate2)
        sTime = findViewById(R.id.widgetStartTm2)
        eTime = findViewById(R.id.widgetEndTm2)

        // Get current date by click
        sDate!!.setOnClickListener { _: View? ->
            val date = Date()
            sDate!!.text = getformDate(date)
        }

        // Get date picker result
        val dpd =
            OnDateSetListener { _: DatePicker?, year: Int, monthOfYear: Int, dayOfMonth: Int ->
                pdate!!.set(Calendar.YEAR, year)
                pdate!!.set(Calendar.MONTH, monthOfYear)
                pdate!!.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                val date = pdate!!.getTime()
                sDate!!.text = getformDate(date)
            }

        // Select date by long click
        sDate!!.setOnLongClickListener { _: View? ->
            DatePickerDialog(
                this@EditMetaActivity, dpd,
                pdate!!.get(Calendar.YEAR),
                pdate!!.get(Calendar.MONTH),
                pdate!!.get(Calendar.DAY_OF_MONTH)
            ).show()
            true
        }

        // Get current start time
        sTime!!.setOnClickListener { _: View? ->
            val date = Date()
            sTime!!.text = getformTime(date)
        }

        // Get start time picker result
        val stpd = OnTimeSetListener { _: TimePicker?, hourOfDay: Int, minute: Int ->
            ptime!!.set(Calendar.HOUR_OF_DAY, hourOfDay)
            ptime!!.set(Calendar.MINUTE, minute)
            val date = ptime!!.getTime()
            sTime!!.text = getformTime(date)
        }

        // Select start time
        sTime!!.setOnLongClickListener { _: View? ->
            TimePickerDialog(
                this@EditMetaActivity, stpd,
                ptime!!.get(Calendar.HOUR_OF_DAY),
                ptime!!.get(Calendar.MINUTE),
                true
            ).show()
            true
        }

        // Get current end time
        eTime!!.setOnClickListener { _: View? ->
            val date = Date()
            eTime!!.text = getformTime(date)
        }

        // Get start time picker result
        val etpd = OnTimeSetListener { _: TimePicker?, hourOfDay: Int, minute: Int ->
            ptime!!.set(Calendar.HOUR_OF_DAY, hourOfDay)
            ptime!!.set(Calendar.MINUTE, minute)
            val date = ptime!!.getTime()
            eTime!!.text = getformTime(date)
        }

        // Select end time
        eTime!!.setOnLongClickListener { _: View? ->
            TimePickerDialog(
                this@EditMetaActivity, etpd,
                ptime!!.get(Calendar.HOUR_OF_DAY),
                ptime!!.get(Calendar.MINUTE),
                true
            ).show()
            true
        }
    }
    // End of onResume()

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.edit_meta, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here.
        val id = item.itemId
        if (id == android.R.id.home)  // back button in actionBar
        {
            if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                Log.i(TAG, "316, MenuItem home")

            finish()
            return true
        }

        if (id == R.id.menuSaveExit) {
            if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                Log.i(TAG, "324, MenuItem saveExit")

            if (saveData()) finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onPause() {
        super.onPause()

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "336, onPause")

        headDataSource!!.close()
        sectionDataSource!!.close()

        sDate!!.setOnClickListener(null)
        sDate!!.setOnLongClickListener(null)
        sTime!!.setOnClickListener(null)
        sTime!!.setOnLongClickListener(null)
        eTime!!.setOnClickListener(null)
        eTime!!.setOnLongClickListener(null)

        headArea!!.clearFocus()
        headArea!!.removeAllViews()
    }

    override fun onStop() {
        super.onStop()

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "356, onStop")

        headArea = null
    }

    override fun onDestroy() {
        super.onDestroy()

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "365, onDestroy")
    }

    private fun saveData(): Boolean {
        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "370 saveData")

        // Save head data
        head!!.observer = emtw!!.widgetOName2
        headDataSource!!.saveHead(head!!)

        var mesg: String

        // Save section data
        section!!.name = emtw!!.widgetName
        section!!.notes = emtw!!.widgetONotes2

        section!!.country = emlw!!.widgetCo2
        section!!.b_state = emlw!!.widgetState2
        section!!.city = emlw!!.widgetCity2
        section!!.place = emlw!!.widgetPlace2
        section!!.st_locality = emlw!!.widgetLocality2
        section!!.plz = emlw!!.widgetPlz2

        section!!.tmp = emw!!.widgetTemp2
        section!!.tmp_end = emw!!.widgetTemp3

        // Check plausi for temperature
        if (section!!.tmp !in 0..50 || section!!.tmp_end !in 0..50) {
            mesg = getString(R.string.valTemp)
            Toast.makeText(this,  //orange
                fromHtml("<font color='#ff6000'><b>$mesg</b></font>"),
                Toast.LENGTH_LONG).show()
            return false
        }

        section!!.wind = emw!!.widgetWind2
        section!!.wind_end = emw!!.widgetWind3

        // Check plausi for wind
        if (section!!.wind !in 0..4 || section!!.wind_end !in 0..4) {
            mesg = getString(R.string.valWind)
            Toast.makeText(this,  // orange
                fromHtml("<font color='#ff6000'><b>$mesg</b></font>"),
                Toast.LENGTH_LONG).show()
            return false
        }

        section!!.clouds = emw!!.widgetClouds2
        section!!.clouds_end = emw!!.widgetClouds3

        // Check plausi for clouds
        if (section!!.clouds !in 0..100 || section!!.clouds_end !in 0..100) {
            mesg = getString(R.string.valClouds)
            Toast.makeText(this,  // orange
                fromHtml("<font color='#ff6000'><b>$mesg</b></font>"),
                Toast.LENGTH_LONG).show()
            return false
        }

        section!!.date = emw!!.widgetDate2
        section!!.start_tm = emw!!.widgetStartTm2
        section!!.end_tm = emw!!.widgetEndTm2

        sectionDataSource!!.saveSection(section!!)
        return true
    }
    // End of saveData()

    companion object {
        private const val TAG = "EditMetaAct"

        // Formatted date
        fun getformDate(date: Date): String {
            val dform: DateFormat?
            val lng = Locale.getDefault().toString().substring(0, 2)

            dform = when (lng) {
                "de" -> SimpleDateFormat("dd.MM.yyyy", Locale.GERMAN)
                "en" -> SimpleDateFormat("yyyy-MM-dd", Locale.US)
                else  // for fr, it and es
                    -> SimpleDateFormat("dd/MM/yyyy", Locale.FRENCH)
            }
            return dform.format(date)
        }

        // Derive start_tm and end_tm from date
        fun getformTime(date: Date): String {
            val dform: DateFormat = SimpleDateFormat("HH:mm", Locale.US)
            return dform.format(date)
        }
    }

}
