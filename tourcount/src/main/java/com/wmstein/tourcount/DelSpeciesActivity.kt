package com.wmstein.tourcount

import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import android.view.WindowManager
import android.widget.EditText
import android.widget.LinearLayout

import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NavUtils
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams

import com.wmstein.tourcount.database.CountDataSource
import com.wmstein.tourcount.database.IndividualsDataSource
import com.wmstein.tourcount.database.SectionDataSource
import com.wmstein.tourcount.widgets.DeleteSpeciesHintWidget
import com.wmstein.tourcount.widgets.DeleteSpeciesWidget

import java.util.Locale

/********************************************************************
 * DelSpeciesActivity lets you delete species from the species lists.
 * It is called from CountingActivity.
 * Uses DelSpeciesWidget.kt, EditMetaTitleWidget.kt,
 * activity_del_species.xml, widget_edit_title.xml.
 *
 * Based on EditSpeciesListActivity.kt.
 * Created on 2024-08-22 by wmstein,
 * last edited on 2026-06-08
 */
class DelSpeciesActivity : AppCompatActivity() {
    // Data
    private var specCode = ""
    private var sectionDataSource: SectionDataSource? = null
    private var countDataSource: CountDataSource? = null
    private var individualsDataSource: IndividualsDataSource? = null

    // Layouts
    private var deleteArea: LinearLayout? = null
    private var delHintArea: LinearLayout? = null

    // 2 initial characters to limit selection
    private var searchChars = ""

    // Arraylists
    private var listToDelete: ArrayList<DeleteSpeciesWidget>? = null

    // Preferences
    private var prefs = TourCountApplication.getPrefs()
    private var awakePref = false
    private var brightPref = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "68, onCreate")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) // SDK 35+
        {
            enableEdgeToEdge()
        }
        setContentView(R.layout.activity_del_species)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.delSpec)) { v, windowInsets ->
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

        // Get value from re-entering respective getDelSearchChars()
        val extras = intent.extras
        if (extras != null)
            searchChars = extras.getString("search_Chars").toString()

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "100 onCreate, searchChars: $searchChars")

        listToDelete = ArrayList()

        delHintArea = findViewById(R.id.showHintDelLayout)
        deleteArea = findViewById(R.id.deleteSpecLayout)

        // Set up the data sources
        sectionDataSource = SectionDataSource(this)
        countDataSource = CountDataSource(this)
        individualsDataSource = IndividualsDataSource(this)

        // New onBackPressed logic
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                NavUtils.navigateUpFromSameTask(this@DelSpeciesActivity)
                remove()
            }
        }
        onBackPressedDispatcher.addCallback(this, callback)
    }
    // End of onCreate()

    override fun onResume() {
        super.onResume()

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "127 onResume")

        // Load preference
        brightPref = prefs.getBoolean("pref_bright", true)
        awakePref = prefs.getBoolean("pref_awake", true)

        // Set full brightness of screen
        if (brightPref) {
            val params = window.attributes
            params.screenBrightness = 1.0f
            window.attributes = params
        }

        if (awakePref)
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        sectionDataSource!!.open()
        countDataSource!!.open()
        individualsDataSource!!.open()

        // Clear any existing views
        deleteArea!!.removeAllViews()
        delHintArea!!.removeAllViews()

        supportActionBar!!.setTitle(R.string.delTitle)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        // Display hint: "Search string" or editable searchChars
        val hdw = DeleteSpeciesHintWidget(this, null)
        if (searchChars.length >= 2)
            hdw.setSearchD1(searchChars)
        else
            hdw.setSearchD(getString(R.string.hintSearch))
        delHintArea!!.addView(hdw)

        constructDelList()
    }
    // End of onResume()

    // Get 2 or more characters of species to select by search button
    // Parameter view is necessary for function call
    fun getDelSearchChars(view: View) {
        // Read EditText searchDel from widget_del_hint.xml
        val searchDel: EditText = findViewById(R.id.searchD)
        searchDel.findFocus()

        // Get the search characters to build a species list to select from
        searchChars = searchDel.text.toString().trim()
        if (searchChars.length == 1) {
            // Reminder: "Please, >1 characters"
            searchDel.error = getString(R.string.searchCharsL)
        } else {
            searchDel.error = null
            searchDel.clearFocus()
            searchDel.invalidate()

            // Re-enter DelSpeciesActivity for the reduced species delete-list
            val intent = Intent(this@DelSpeciesActivity, DelSpeciesActivity::class.java)
            intent.putExtra("search_Chars", searchChars)
            intent.flags = FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
            finish()
        }
    }

    // Construct a del-species-list of contained species in the current counting list
    //   and optionally reduce it by searchChars selection
    private fun constructDelList() {
        // Load the sorted species
        val countsCodesSortList = countDataSource!!.allSpeciesSrtCode

        // Get all counting list species into their CountEditWidgets and add these to the view
        if (searchChars.length >= 2) {
            searchChars = searchChars.uppercase(Locale.getDefault()) //Compare searchChars in uppercase
            var cnt = 1
            // Check name in countsCodesSortList for InitChars to reduce list
            for (count in countsCodesSortList) {
                if (count.name.uppercase(Locale.getDefault()).contains(searchChars)) {
                    val dsw = DeleteSpeciesWidget(this, null)
                    dsw.setSpecName(count.name)
                    dsw.setSpecNameG(count.name_g)
                    dsw.setSpecCode(count.code)
                    dsw.setPSpec(count)
                    dsw.setSpecId(cnt.toString()) // Index in reduced list
                    cnt++
                    deleteArea!!.addView(dsw)
                }
            }
        } else {
            for (count in countsCodesSortList) {
                val dsw = DeleteSpeciesWidget(this, null)
                dsw.setSpecName(count.name)
                dsw.setSpecNameG(count.name_g)
                dsw.setSpecCode(count.code)
                dsw.setPSpec(count)
                dsw.setSpecId(count.id.toString()) // Index in complete list
                deleteArea!!.addView(dsw)
            }
        }
    }

    // Mark the selected species and consider it for delete from the species countsCodesSortList list
    fun checkBoxDel(view: View) {
        val idToDel = view.tag as Int
        val dsw = deleteArea!!.getChildAt(idToDel) as DeleteSpeciesWidget

        val checked = dsw.getMarkSpec() // return boolean isChecked

        // Put species on add list
        if (checked) {
            listToDelete!!.add(dsw)
        } else {
            // Remove species previously added from add list
            listToDelete!!.remove(dsw)
        }
    }

    // Delete selected species from current species list
    private fun delSpecs() {
        var i = 0 // index of species list to delete
        while (i < listToDelete!!.size) {
            specCode = listToDelete!![i].getSpecCode()
            try {
                countDataSource!!.deleteCountByCode(specCode)
                individualsDataSource!!.deleteIndividualsByCode(specCode)
            } catch (_: Exception) {
                // nothing
            }
            i++
        }

        // Re-index and sort counts table for code
        countDataSource!!.sortCounts()

        // Rebuild the species list
        val countsCodesSortList = countDataSource!!.allSpeciesSrtCode

        delHintArea!!.removeAllViews()
        val hdw = DeleteSpeciesHintWidget(this, null)
        hdw.setSearchD(getString(R.string.hintSearch))
        delHintArea!!.addView(hdw)

        deleteArea!!.removeAllViews()
        for (count in countsCodesSortList) {
            val dsw = DeleteSpeciesWidget(this, null)
            dsw.setSpecName(count.name)
            dsw.setSpecNameG(count.name_g)
            dsw.setSpecCode(count.code)
            dsw.setPSpec(count)
            dsw.setSpecId(count.id.toString())
            deleteArea!!.addView(dsw)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.delete_species, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here.
        val id = item.itemId
        if (id == android.R.id.home) {
            finish()
            return true
        } else if (id == R.id.deleteSpec) {
            if (listToDelete!!.isNotEmpty())
                delSpecs()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onPause() {
        super.onPause()

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "305, onPause")

        // Close the data sources
        sectionDataSource!!.close()
        countDataSource!!.close()
        individualsDataSource!!.close()

        if (awakePref) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }

        deleteArea!!.clearFocus()
        deleteArea!!.removeAllViews()
        delHintArea!!.clearFocus()
        delHintArea!!.removeAllViews()
    }

    override fun onStop() {
        super.onStop()

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "326, onStop")

        deleteArea = null
        delHintArea = null
    }

    override fun onDestroy() {
        super.onDestroy()

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "336, onDestroy")
    }

    companion object {
        private const val TAG = "DelSpecAct"
    }

}
