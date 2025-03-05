package com.wmstein.tourcount

import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.wmstein.tourcount.database.Count
import com.wmstein.tourcount.database.CountDataSource
import com.wmstein.tourcount.widgets.EditSpNotesWidget

/**********************************************************
 * CountOptionsActivity
 * Edit notes for a counted species
 * uses EditSpNotesWidget.kt and activity_count_options.xml
 * Based on CountOptionsActivity.kt by milo on 05/05/2014.
 * Adopted and changed by wmstein on 18.02.2016,
 * last edited in Java on 2023-05-13,
 * converted to Kotlin on 2023-07-06,
 * last edited on 2025-02-10
 */
class CountOptionsActivity : AppCompatActivity() {
    private var staticWidgetArea: LinearLayout? = null
    private var esw: EditSpNotesWidget? = null
    private var count: Count? = null
    private var countId = 0
    private var countDataSource: CountDataSource? = null

    // Preferences
    private var prefs = TourCountApplication.getPrefs()
    private var brightPref = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (MyDebug.DLOG) Log.i(TAG, "40, onCreate")

        brightPref = prefs.getBoolean("pref_bright", true)

        setContentView(R.layout.activity_count_options)

        // Set full brightness of screen
        if (brightPref) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            val params = window.attributes
            params.screenBrightness = 1.0f
            window.attributes = params
        }

        val extras = intent.extras
        if (extras != null) {
            countId = extras.getInt("count_id")
        }

        staticWidgetArea = findViewById(R.id.static_widget_area)

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

        if (MyDebug.DLOG) Log.i(TAG, "77, onResume")

        // Clear any existing views
        staticWidgetArea!!.removeAllViews()

        // Get the data sources
        countDataSource!!.open()
        count = countDataSource!!.getCountById(countId)

        supportActionBar!!.title = count!!.name
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        // Set the widget for species notes
        esw = EditSpNotesWidget(this, null)
        esw!!.spNotesName = count!!.notes
        esw!!.setSpNotesTitle(getString(R.string.notesSpecies))
        esw!!.setHint(getString(R.string.notesHint))
        staticWidgetArea!!.addView(esw)
    }
    // End of onResume()

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.count_options, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here.
        val id = item.itemId
        if (id == android.R.id.home) {
            if (MyDebug.DLOG) Log.i(TAG, "108, Home")
            finish()
            return true
        } else if (id == R.id.menuSaveExit) {
            if (MyDebug.DLOG) Log.i(TAG, "112, SaveExit")
            if (saveData())
                finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onPause() {
        super.onPause()

        if (MyDebug.DLOG) Log.i(TAG, "123, onPause")

        countDataSource!!.close()
    }

    private fun saveData(): Boolean {
        if (MyDebug.DLOG) Log.i(TAG, "129, saveData")

        // Toast here, as snackbar doesn't show up
        Toast.makeText(this@CountOptionsActivity, getString(R.string.sectSaving)
                + " " + count!!.name + "!", Toast.LENGTH_SHORT).show()

        count!!.notes = esw!!.spNotesName

        countDataSource!!.saveCountNotes(count!!)
        return true
    }

    companion object {
        private const val TAG = "CntOptAct"
    }

}
