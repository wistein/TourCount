package com.wmstein.tourcount

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup.MarginLayoutParams
import android.view.WindowManager
import android.widget.LinearLayout

import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams

import com.wmstein.tourcount.database.Section
import com.wmstein.tourcount.database.SectionDataSource
import com.wmstein.tourcount.widgets.EditTourNotesWidget

/***********************************************************
 * EditTourNotesActivity
 * Edit notes for the tour
 * uses EditTourNotetwidget.kt and activity_tour_options.xml
 *
 * Based on EditSpeciesNotesActivity.kt.
 * Changed by wmstein on 16.09.2025,
 * last edited on 2026-03-20
 */
class EditTourNotesActivity : AppCompatActivity() {
    private var tourWidgetArea: LinearLayout? = null
    private var etw: EditTourNotesWidget? = null
    private var section: Section? = null
    private var sectionDataSource: SectionDataSource? = null

    // Preferences
    private var prefs = TourCountApplication.getPrefs()
    private var brightPref = false
    private var awakePref = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "47, onCreate")

        brightPref = prefs.getBoolean("pref_bright", true)
        awakePref = prefs.getBoolean("pref_awake", true)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) // SDK 35+
        {
            enableEdgeToEdge()
        }
        setContentView(R.layout.activity_tour_options)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.tour_options)) { v, windowInsets ->
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

        tourWidgetArea = findViewById(R.id.tour_widget_area)

        sectionDataSource = SectionDataSource(this)

        // New onBackPressed logic
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
                remove()
            }
        }
        onBackPressedDispatcher.addCallback(this, callback)
    }
    // End of onCreate()

    override fun onResume() {
        super.onResume()

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "105, onResume")

        // Clear any existing views
        tourWidgetArea!!.removeAllViews()

        // Get the data sources
        sectionDataSource!!.open()
        section = sectionDataSource!!.section

        supportActionBar!!.title = section!!.name
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        // Set the widget for tour notes
        etw = EditTourNotesWidget(this, null)
        etw!!.trNotesName = section!!.notes
        etw!!.setTrNotesTitle(getString(R.string.titleTourNotes))
        etw!!.setHint(getString(R.string.notesHint))
        tourWidgetArea!!.addView(etw)
    }
    // End of onResume()

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.tour_options, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here.
        val id = item.itemId
        if (id == android.R.id.home) {
            finish()
            return true
        } else if (id == R.id.menuSaveExit) {
            if (saveData())
                finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onPause() {
        super.onPause()

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "153, onPause")

        sectionDataSource!!.close()
    }

    override fun onStop() {
        super.onStop()

        if (awakePref) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        tourWidgetArea!!.clearFocus()
        tourWidgetArea!!.removeAllViews()
        tourWidgetArea = null
    }

    private fun saveData(): Boolean {
        // Add tour notes if the user has written some...
        val tourName = etw!!.trNotesName
        if (isNotEmpty(tourName))
            section!!.notes = tourName
        else
            if (isNotEmpty(section!!.notes))
                section!!.notes = tourName
        sectionDataSource!!.saveSection(section!!)
        return true
    }

    companion object {
        private const val TAG = "TourNotesAct"

        /**
         * Checks if a CharSequence is empty ("") or null.
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
            return cs.isNullOrEmpty()
        }

        /**
         * Checks if a CharSequence is not empty ("") and not null.
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
