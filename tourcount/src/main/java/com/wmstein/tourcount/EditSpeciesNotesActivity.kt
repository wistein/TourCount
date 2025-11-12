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

import com.wmstein.tourcount.database.Count
import com.wmstein.tourcount.database.CountDataSource
import com.wmstein.tourcount.widgets.EditSpNotesWidget

/**********************************************************
 * EditSpeciesNotesActivity
 * Edit notes for a counted species
 * uses EditSpNotesWidget.kt and activity_count_options.xml
 * Based on EditSpeciesNotesActivity.kt by milo on 05/05/2014.
 * Adopted and changed by wmstein on 18.02.2016,
 * last edited in Java on 2023-05-13,
 * converted to Kotlin on 2023-07-06,
 * last edited on 2025-10-22
 */
class EditSpeciesNotesActivity : AppCompatActivity() {
    private var speciesWidgetArea: LinearLayout? = null
    private var esw: EditSpNotesWidget? = null
    private var count: Count? = null
    private var countId = 0
    private var countDataSource: CountDataSource? = null

    // Preferences
    private var prefs = TourCountApplication.getPrefs()
    private var brightPref = false
    private var awakePref = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "49, onCreate")

        brightPref = prefs.getBoolean("pref_bright", true)
        awakePref = prefs.getBoolean("pref_awake", true)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) // SDK 35+
        {
            enableEdgeToEdge()
        }
        setContentView(R.layout.activity_count_options)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.count_options)) { v, windowInsets ->
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

        val extras = intent.extras
        if (extras != null) {
            countId = extras.getInt("count_id")
        }

        speciesWidgetArea = findViewById(R.id.species_widget_area)

        countDataSource = CountDataSource(this)

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
            Log.i(TAG, "112, onResume")

        // Clear any existing views
        speciesWidgetArea!!.removeAllViews()

        // Get the data sources
        countDataSource!!.open()
        count = countDataSource!!.getCountById(countId)

        supportActionBar!!.title = count!!.name
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        // Set the widget for species notes
        esw = EditSpNotesWidget(this, null)
        esw!!.spNotesNotes = count!!.notes
        esw!!.setSpNotesTitle(getString(R.string.titleNotesSpecies))
        esw!!.setHint(getString(R.string.notesHint))
        speciesWidgetArea!!.addView(esw)
    }
    // End of onResume()

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.species_options, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here.
        val id = item.itemId
        if (id == android.R.id.home) {
            if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                Log.i(TAG, "144, Home")
            finish()
            return true
        } else if (id == R.id.menuSaveExit) {
            if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                Log.i(TAG, "149, SaveExit")
            if (saveData())
                finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onPause() {
        super.onPause()

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "161, onPause")

        countDataSource!!.close()
    }

    override fun onStop() {
        super.onStop()

        if (awakePref) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        speciesWidgetArea!!.clearFocus()
        speciesWidgetArea!!.removeAllViews()
        speciesWidgetArea = null
    }

    private fun saveData(): Boolean {
        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "184, saveData")

        // Add species notes if the user has written some...
        val notesName = esw!!.spNotesNotes
        if (isNotEmpty(notesName))
            count!!.notes = notesName
        else {
            if (isNotEmpty(count!!.notes))
                count!!.notes = notesName
        }
        countDataSource!!.saveCountNotes(count!!)
        return true
    }

    companion object {
        private const val TAG = "SpecNotesAct"

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
            return cs.isNullOrEmpty()
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
