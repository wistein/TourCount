package com.wmstein.tourcount

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.icu.lang.UCharacter.GraphemeClusterBreak.T
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NavUtils
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import com.wmstein.tourcount.PermissionsDialogFragment.Companion.newInstance
import com.wmstein.tourcount.PermissionsDialogFragment.PermissionsGrantedCallback
import com.wmstein.tourcount.database.CountDataSource
import com.wmstein.tourcount.database.Section
import com.wmstein.tourcount.database.SectionDataSource
import com.wmstein.tourcount.widgets.CountEditWidget
import com.wmstein.tourcount.widgets.EditNotesWidget
import com.wmstein.tourcount.widgets.EditTitleWidget
import com.wmstein.tourcount.widgets.HintWidget

/***************************************************************
 * EditSpecListActivity lets you edit the species list (change, delete, select
 * and insert new species)
 * EditSpecListActivity is called from CountingActivity
 * Uses CountEditWidget.java, HintWidget.java,
 * activity_edit_section.xml, widget_edit_count.xml.
 * Calls AddSpeciesActivity.java for adding a new species to the species list.
 *
 * Based on EditProjectActivity.java by milo on 05/05/2014.
 * Adopted, modified and enhanced for TourCount by wmstein on 2016-02-18,
 * last edited in Java on 2023-07-07,
 * converted to Kotlin on 2023-07-09,
 * last edited on 2023-07-13
 */
class EditSpecListActivity : AppCompatActivity(), OnSharedPreferenceChangeListener,
    PermissionsGrantedCallback {
    private var tourCount: TourCountApplication? = null

    private var savedCounts: ArrayList<CountEditWidget>? = null
    private var counts_area: LinearLayout? = null
    private var notes_area1: LinearLayout? = null
    private var hint_area1: LinearLayout? = null
    private var etw: EditTitleWidget? = null
    private var enw: EditNotesWidget? = null

    // the actual data
    private var section: Section? = null
    private var sectionDataSource: SectionDataSource? = null
    private var countDataSource: CountDataSource? = null
    private var viewMarkedForDelete: View? = null
    private var idToDelete = 0
    private var bMap: Bitmap? = null
    private var bg: BitmapDrawable? = null

    // Location info handling
    var latitude = 0.0
    var longitude = 0.0
    var locationService: LocationService? = null

    // Permission dispatcher mode modePerm: 
    //  1 = use location service
    //  2 = end location service
    var modePerm = 0

    // Preferences
    private var prefs = TourCountApplication.getPrefs()
    private var dupPref = false
    private var sortPref: String? = null
    private var brightPref = false

    var oldname: String? = null

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        tourCount = application as TourCountApplication
        prefs.registerOnSharedPreferenceChangeListener(this)
        dupPref = prefs.getBoolean("pref_duplicate", true)
        sortPref = prefs.getString("pref_sort_sp", "none")
        brightPref = prefs.getBoolean("pref_bright", true)

        setContentView(R.layout.activity_edit_section)
        val counting_screen = findViewById<LinearLayout>(R.id.editSect)

        // Set full brightness of screen
        if (brightPref) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            val params = window.attributes
            params.screenBrightness = 1.0f
            window.attributes = params
        }
        bMap =
            tourCount!!.decodeBitmap(R.drawable.kbackground, tourCount!!.width, tourCount!!.height)
        bg = BitmapDrawable(counting_screen.resources, bMap)
        counting_screen.background = bg
        savedCounts = ArrayList()
        notes_area1 = findViewById(R.id.editingNotes1Layout)
        hint_area1 = findViewById(R.id.showHintLayout)
        counts_area = findViewById(R.id.editingCountsLayout)

        // Restore any edit widgets the user has added previously
        if (savedInstanceState != null) {
            if (Build.VERSION.SDK_INT < 33) {
                @Suppress("DEPRECATION")
                if (savedInstanceState.getSerializable("savedCounts") != null) {
                    savedCounts =
                        savedInstanceState.getSerializable("savedCounts") as ArrayList<CountEditWidget>?
                }
            }
            else {
                if (savedInstanceState.getSerializable("savedCounts", T::class.java) != null) {
                    savedCounts =
                        savedInstanceState.getSerializable("savedCounts", T::class.java) as ArrayList<CountEditWidget>?
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()

        prefs = TourCountApplication.getPrefs()
        prefs.registerOnSharedPreferenceChangeListener(this)
        dupPref = prefs.getBoolean("pref_duplicate", true)
        brightPref = prefs.getBoolean("pref_bright", true)

        // Set full brightness of screen
        if (brightPref) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            val params = window.attributes
            params.screenBrightness = 1.0f
            window.attributes = params
        }

        // Get location with permissions check
        modePerm = 1
        permissionCaptureFragment()

        // clear any existing views
        counts_area!!.removeAllViews()
        notes_area1!!.removeAllViews()
        hint_area1!!.removeAllViews()

        // setup the data sources
        sectionDataSource = SectionDataSource(this)
        sectionDataSource!!.open()
        countDataSource = CountDataSource(this)
        countDataSource!!.open()

        // load the sections data
        section = sectionDataSource!!.section
        oldname = section!!.name
        try {
            supportActionBar!!.title = oldname
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        } catch (e: NullPointerException) {
            if (MyDebug.LOG) Log.e(TAG, "NullPointerException: No section name!")
        }

        // display the section title
        etw = EditTitleWidget(this, null)
        etw!!.sectionName = oldname
        etw!!.setWidgetTitle(getString(R.string.titleEdit))
        notes_area1!!.addView(etw)

        // display editable section notes; the same class
        enw = EditNotesWidget(this, null)
        enw!!.notesName = section!!.notes
        enw!!.setWidgetTitle(getString(R.string.notesHere))
        enw!!.setHint(getString(R.string.notesHint))
        notes_area1!!.addView(enw)

        // display hint current species list:
        val nw = HintWidget(this, null)
        nw.setHint1(getString(R.string.presentSpecs))
        hint_area1!!.addView(nw)

        // load the sorted species data
        val counts = when (sortPref) {
            "names_alpha" -> countDataSource!!.allSpeciesSrtName
            "codes" -> countDataSource!!.allSpeciesSrtCode
            else -> countDataSource!!.allSpecies
        }

        // display all the counts by adding them to CountEditWidget
        for (count in counts) {
            // widget
            val cew = CountEditWidget(this, null)
            cew.setCountName(count.name)
            cew.setCountNameG(count.name_g)
            cew.setCountCode(count.code)
            cew.setCountId(count.id)
            cew.setPSpec(count)
            counts_area!!.addView(cew)
        }
        for (cew in savedCounts!!) {
            counts_area!!.addView(cew)
        }
    } // end of Resume

    override fun onPause() {
        super.onPause()

        // close the data sources
        sectionDataSource!!.close()
        countDataSource!!.close()

        // Stop location service with permissions check
        modePerm = 2
        permissionCaptureFragment()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        /*
         * Before these widgets can be serialised they must be removed from their parent, or else
         * trying to add them to a new parent causes a crash because they've already got one.
         */
        super.onSaveInstanceState(outState)
        for (cew in savedCounts!!) {
            (cew.parent as ViewGroup).removeView(cew)
        }
        outState.putSerializable("savedCounts", savedCounts)
    }

    // Compare count names for duplicates and returns name of 1. duplicate found
    private fun compCountNames(): String {
        var name: String
        var isDbl = ""
        val cmpCountNames = ArrayList<String>()
        val childcount = counts_area!!.childCount
        // for all CountEditWidgets
        for (i in 0 until childcount) {
            val cew = counts_area!!.getChildAt(i) as CountEditWidget
            name = cew.getCountName()
            if (cmpCountNames.contains(name)) {
                isDbl = name
                if (MyDebug.LOG) Log.d(TAG, "Double name = $isDbl")
                break
            }
            cmpCountNames.add(name)
        }
        return isDbl
    }

    fun saveAndExit(view: View?) {
        if (saveData()) {
            savedCounts!!.clear()
            super.finish()
        }
    }

    private fun saveData(): Boolean {
        // test for double entries and save species list
        var retValue = true

        // add title if the user has written one...
        val section_name = etw!!.sectionName
        if (isNotEmpty(section_name)) {
            section!!.name = section_name
        } else {
            if (isNotEmpty(section!!.name)) {
                section!!.name = section_name
            }
        }

        // add notes if the user has written some...
        val section_notes = enw!!.notesName
        if (isNotEmpty(section_notes)) {
            section!!.notes = section_notes
        } else {
            if (isNotEmpty(section!!.notes)) {
                section!!.notes = section_notes
            }
        }
        sectionDataSource!!.saveSection(section!!)

        val isDbl: String
        val childcount: Int = counts_area!!.childCount //No. of species in list
        if (MyDebug.LOG) Log.d(TAG, "childcount: $childcount")

        // check for unique species names
        if (dupPref) {
            isDbl = compCountNames()
            if (isDbl == "") {
                // do for all species 
                for (i in 0 until childcount) {
                    val cew = counts_area!!.getChildAt(i) as CountEditWidget
                    if (isNotEmpty(cew.getCountName())) {
                        if (MyDebug.LOG) Log.d(
                            TAG,
                            "cew: " + cew.countId + ", " + cew.getCountName()
                        )

                        //updates species name and code
                        countDataSource!!.updateCountName(
                            cew.countId,
                            cew.getCountName(),
                            cew.getCountCode(),
                            cew.getCountNameG()
                        )
                    }
                }
            } else {
                showSnackbarRed(
                    isDbl + " " + getString(R.string.isdouble) + " "
                            + getString(R.string.duplicate)
                )
                retValue = false
            }
        }
        if (retValue) {
            // Snackbar doesn't appear, so Toast is used
            Toast.makeText(
                this@EditSpecListActivity,
                getString(R.string.sectSaving) + " " + section!!.name + "!",
                Toast.LENGTH_SHORT
            ).show()
        }
        return retValue
    }

    private fun testData(): Boolean {
        // test species list for double entry
        var retValue = true
        val isDbl: String

        // check for unique species names
        if (dupPref) {
            isDbl = compCountNames()
            if (isDbl != "") {
                showSnackbarRed(
                    isDbl + " " + getString(R.string.isdouble) + " "
                            + getString(R.string.duplicate)
                )
                retValue = false
            }
        }
        return retValue
    }

    private fun showSnackbarRed(str: String) // bold red text
    {
        val view = findViewById<View>(R.id.editingScreen)
        val sB = Snackbar.make(view, str, Snackbar.LENGTH_LONG)
        sB.setActionTextColor(Color.RED)
        val tv = sB.view.findViewById<TextView>(R.id.snackbar_text)
        tv.textAlignment = View.TEXT_ALIGNMENT_CENTER
        tv.setTypeface(tv.typeface, Typeface.BOLD)
        sB.show()
    }

    // Start AddSpeciesActivity to add a new species to the species list
    fun newCount(view: View?) {
        Toast.makeText(applicationContext, getString(R.string.wait), Toast.LENGTH_SHORT)
            .show() // a Snackbar here comes incomplete

        // pause for 100 msec to show toast
        Handler(Looper.getMainLooper()).postDelayed({

            // add title if the user has written one...
            val section_name = etw!!.sectionName
            if (isNotEmpty(section_name)) {
                section!!.name = section_name
            } else {
                if (isNotEmpty(section!!.name)) {
                    section!!.name = section_name
                }
            }

            // add notes if the user has written some...
            val section_notes = enw!!.notesName
            if (isNotEmpty(section_notes)) {
                section!!.notes = section_notes
            } else {
                if (isNotEmpty(section!!.notes)) {
                    section!!.notes = section_notes
                }
            }
            sectionDataSource!!.saveSection(section!!)
            sectionDataSource!!.close()

            val intent = Intent(this@EditSpecListActivity, AddSpeciesActivity::class.java)
            startActivity(intent)
        }, 100)
    }

    // Purging species
    fun deleteCount(view: View) {
        viewMarkedForDelete = view
        idToDelete = view.tag as Int
        if (idToDelete == 0) {
            // the actual CountEditWidget is 3 levels up from the button in which it is embedded
            counts_area!!.removeView(view.parent.parent.parent as CountEditWidget)
        } else {
            val areYouSure = AlertDialog.Builder(this)
            areYouSure.setTitle(getString(R.string.deleteCount))
            areYouSure.setMessage(getString(R.string.reallyDeleteCount))
            areYouSure.setPositiveButton(R.string.yesDeleteIt) { _: DialogInterface?, _: Int ->
                // go ahead for the delete
                countDataSource!!.deleteCountById(idToDelete)
                counts_area!!.removeView(viewMarkedForDelete!!.parent.parent.parent as CountEditWidget)
            }
            areYouSure.setNegativeButton(R.string.noCancel) { _: DialogInterface?, _: Int -> }
            areYouSure.show()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.edit_section, menu)
        return true
    }

    // catch back button for plausi test
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (testData()) {
                savedCounts!!.clear()
                val intent = NavUtils.getParentActivityIntent(this)!!
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                NavUtils.navigateUpTo(this, intent)
            } else return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        if (!testData()) return true
        val id = item.itemId
        if (id == android.R.id.home) {
            savedCounts!!.clear()
            sectionDataSource!!.close()
            val intent = NavUtils.getParentActivityIntent(this)!!
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            NavUtils.navigateUpTo(this, intent)
        } else if (id == R.id.menuSaveExit) {
            if (saveData()) {
                savedCounts!!.clear()
                sectionDataSource!!.close()
                super.finish()
            }
        } else if (id == R.id.newCount) {
            newCount(view = null)
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onSharedPreferenceChanged(prefs: SharedPreferences, key: String) {
        val counting_screen = findViewById<LinearLayout>(R.id.editSect)
        prefs.registerOnSharedPreferenceChangeListener(this)
        dupPref = prefs.getBoolean("pref_duplicate", true)
        sortPref = prefs.getString("pref_sort_sp", "none")
        brightPref = prefs.getBoolean("pref_bright", true)
        bMap = tourCount!!.decodeBitmap(R.drawable.kbackground, tourCount!!.width, tourCount!!.height)
        counting_screen.background = null
        bg = BitmapDrawable(counting_screen.resources, bMap)
        counting_screen.background = bg
    }

    override fun permissionCaptureFragment() {
        run {
            if (this.isPermissionGranted) {
                when (modePerm) {
                    1 ->  // get location
                        this.loc

                    2 ->  // stop location service
                        locationService!!.stopListener()
                }
            } else {
                if (modePerm == 1) newInstance().show(
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

    companion object {
        private const val TAG = "TourCountEditSecAct"

        /**
         * Checks if a CharSequence is empty ("") or null.
         *
         *
         * isEmpty(null)      = true
         * isEmpty("")        = true
         * isEmpty(" ")       = false
         * isEmpty("bob")     = false
         * isEmpty("  bob  ") = false
         *
         * @param cs the CharSequence to check, may be null
         * @return `true` if the CharSequence is empty or null
         */
        private fun isEmpty(cs: CharSequence?): Boolean {
            return cs == null || cs.length == 0
        }

        /**
         * Checks if a CharSequence is not empty ("") and not null.
         *
         *
         * isNotEmpty(null)      = false
         * isNotEmpty("")        = false
         * isNotEmpty(" ")       = true
         * isNotEmpty("bob")     = true
         * isNotEmpty("  bob  ") = true
         *
         * @param cs the CharSequence to check, may be null
         * @return `true` if the CharSequence is not empty and not null
         */
        private fun isNotEmpty(cs: CharSequence?): Boolean {
            return !isEmpty(cs)
        }
    }

}
