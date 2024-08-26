package com.wmstein.tourcount

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NavUtils
import com.wmstein.tourcount.database.Count
import com.wmstein.tourcount.database.CountDataSource
import com.wmstein.tourcount.widgets.EditSpNotesWidget

/**********************************
 * CountOptionsActivity
 * Edit notes for a counted species
 * uses EditSpNotesWidget.kt and activity_count_options.xml
 * Based on CountOptionsActivity.kt by milo on 05/05/2014.
 * Adopted and changed by wmstein on 18.02.2016,
 * last edited in Java on 2023-05-13,
 * converted to Kotlin on 2023-07-06,
 * last edited on 2024-05-11
 */
class CountOptionsActivity : AppCompatActivity() {
    private var tourCount: TourCountApplication? = null

    private var staticWidgetArea: LinearLayout? = null
    private var esw: EditSpNotesWidget? = null
    private var count: Count? = null
    private var countId = 0
    private var countDataSource: CountDataSource? = null
    private var bMap: Bitmap? = null
    private var bg: BitmapDrawable? = null

    // Preferences
    private var prefs = TourCountApplication.getPrefs()
    private var brightPref = false

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        tourCount = application as TourCountApplication
        brightPref = prefs.getBoolean("pref_bright", true)

        setContentView(R.layout.activity_count_options)
        val countingScreen = findViewById<LinearLayout>(R.id.count_options)

        // Set full brightness of screen
        if (brightPref) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            val params = window.attributes
            params.screenBrightness = 1.0f
            window.attributes = params
        }
        bMap = tourCount!!.decodeBitmap(R.drawable.edbackground, tourCount!!.width, tourCount!!.height)
        bg = BitmapDrawable(countingScreen.resources, bMap)
        countingScreen.background = bg
        staticWidgetArea = findViewById(R.id.static_widget_area)
        val extras = intent.extras
        if (extras != null) {
            countId = extras.getInt("count_id")
        }
    }

    override fun onResume() {
        super.onResume()

        // clear any existing views
        staticWidgetArea!!.removeAllViews()

        // get the data sources
        countDataSource = CountDataSource(this)
        countDataSource!!.open()
        count = countDataSource!!.getCountById(countId)
        supportActionBar!!.title = count!!.name
        esw = EditSpNotesWidget(this, null)
        esw!!.spNotesName = count!!.notes
        esw!!.setSpNotesTitle(getString(R.string.notesSpecies))
        esw!!.setHint(getString(R.string.notesHint))
        staticWidgetArea!!.addView(esw)
    }

    override fun onPause() {
        super.onPause()

        // finally, close the database
        countDataSource!!.close()
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(esw!!.windowToken, 0)
    }

    private fun saveData() {
        // don't crash if the user hasn't filled things in...
        // Toast here, as snackbar doesn't show up
        Toast.makeText(
            this@CountOptionsActivity,
            getString(R.string.sectSaving) + " " + count!!.name + "!",
            Toast.LENGTH_SHORT
        ).show()
        count!!.notes = esw!!.spNotesName
        // hide keyboard
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(esw!!.windowToken, 0)
        countDataSource!!.saveCount(count!!)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.count_options, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        val id = item.itemId
        if (id == android.R.id.home) {
            val intent = NavUtils.getParentActivityIntent(this)!!
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            NavUtils.navigateUpTo(this, intent)
        } else if (id == R.id.menuSaveExit) {
            saveData()
            super.finish()
        }
        return super.onOptionsItemSelected(item)
    }

}
