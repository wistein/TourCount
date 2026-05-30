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
import android.widget.Toast

import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NavUtils
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams

import com.wmstein.tourcount.Utils.fromHtml
import com.wmstein.tourcount.database.CountDataSource
import com.wmstein.tourcount.database.IndividualsDataSource
import com.wmstein.tourcount.widgets.EditSpeciesListWidget
import com.wmstein.tourcount.widgets.EditSpeciesListHintWidget

import java.util.Locale

/*************************************************************************
 * EditSpeciesListActivity lets you edit the species list
 * (change head and species data).
 * EditSpeciesListActivity is called from CountingActivity
 * Uses EditSpeciesListHeadWidget.kt, EditSpeciesListWidget.kt, EditSpeciesListHintWidget.kt,
 * activity_edit_section.xml, widget_edit_head.xml, widget_edit_count.xml.
 *
 * Based on EditProjectActivity.java by milo on 05/05/2014.
 * Adopted, modified and enhanced for TourCount by wmstein on 2016-02-18,
 * last edited in Java on 2023-07-07,
 * converted to Kotlin on 2023-07-09,
 * last edited on 2026-05-30
 */
class EditSpeciesListActivity : AppCompatActivity() {
    // Data
    private var countDataSource: CountDataSource? = null
    private var individualsDataSource: IndividualsDataSource? = null

    // Layouts
    private var editingSpeciesArea: LinearLayout? = null
    private var editHintArea: LinearLayout? = null

    // Widgets
    private var esw: EditSpeciesListWidget? = null

    // Sorted Arraylists
    private var cmpSpeciesNames: ArrayList<String>? = null
    private var cmpSpeciesCodes: ArrayList<String>? = null
    private var savedCounts: ArrayList<EditSpeciesListWidget>? = null

    // 2 or more characters to limit selection
    private var searchChars = ""

    // Preferences
    private var prefs = TourCountApplication.getPrefs()
    private var sortPref = ""
    private var brightPref = false
    private var awakePref = false

    private var mesg = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "78, onCreate")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) // SDK 35+
        {
            enableEdgeToEdge()
        }
        setContentView(R.layout.activity_edit_species_list)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.editSpecList))
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

        savedCounts = ArrayList() // for EditSpeciesListWidget: countName, countNameG, countCode, pSpecies

        editHintArea = findViewById(R.id.showHintLayout)
        editingSpeciesArea = findViewById(R.id.editingSpeciesLayout)

        //  Note variables to restore them
        val extras = intent.extras
        if (extras != null) {
            searchChars = extras.getString("search_Chars").toString()
        }

        // Set up the data sources
        countDataSource = CountDataSource(this)
        individualsDataSource = IndividualsDataSource(this)

        // New onBackPressed logic
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                NavUtils.navigateUpFromSameTask(this@EditSpeciesListActivity)
                remove()
            }
        }
        onBackPressedDispatcher.addCallback(this, callback)
    }
    // End of onCreate()

    override fun onResume() {
        super.onResume()

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "135 onResume")

        // Load preferences
        brightPref = prefs.getBoolean("pref_bright", true)
        awakePref = prefs.getBoolean("pref_awake", true)
        sortPref = prefs.getString("pref_sort_sp", "none").toString()

        // Set full brightness of screen
        if (brightPref) {
            val params = window.attributes
            params.screenBrightness = 1.0f
            window.attributes = params
        }
        if (awakePref)
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        countDataSource!!.open()
        individualsDataSource!!.open()

        // Build the EditSpeciesList screen
        editingSpeciesArea!!.removeAllViews()
        editHintArea!!.removeAllViews()

        supportActionBar!!.setTitle(R.string.editTitle)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        // Display hint: "Search string" or editable searchChars
        val hew = EditSpeciesListHintWidget(this, null)
        if (searchChars.length >= 2)
            hew.setSearchE1(searchChars)
        else
            hew.setSearchE(getString(R.string.hintSearch))
        editHintArea!!.addView(hew)

        constructEditList()
    }
    // End of onResume()

    // Get 2 or more characters of species to select by search button
    fun getEditSearchChars(view: View) {
        // Read EditText searchEdit from widget_edit_hint.xml
        val searchEdit: EditText = findViewById(R.id.searchE)
        searchEdit.findFocus()

        // Get the initial characters of species to select from
        searchChars = searchEdit.text.toString().trim()
        if (searchChars.length == 1) {
            // Reminder: "Please, >1 chars"
            searchEdit.error = getString(R.string.searchCharsL)
        } else {
            searchEdit.error = null
            searchEdit.clearFocus()
            searchEdit.invalidate()

            // Re-enter EditSpeciesListActivity for edited list
            val intent = Intent(this@EditSpeciesListActivity, EditSpeciesListActivity::class.java)
            intent.putExtra("search_Chars", searchChars)
            intent.flags = FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
            finish()
        }
    }

    // Construct edit-species-list of contained species in the counting list
    //   and optionally reduce it to searchChars selection
    private fun constructEditList() {
        // Load the sorted species data
        val countsToEdit = when (sortPref) {
            "names_alpha" -> countDataSource!!.allSpeciesSrtName
            "codes" -> countDataSource!!.allSpeciesSrtCode
            else -> countDataSource!!.allSpecies
        }

        // Display all the countsToEdit by adding them to editingSpeciesArea
        // Get the counting list species into their EditSpeciesListWidget and add them to the view
        if (searchChars.length >= 2) {
            searchChars = searchChars.uppercase(Locale.getDefault()) //Compare searchChars in uppercase

            // Check name in countsToEdit for searchChars to reduce list
            for (count in countsToEdit) {
                if (count.name.uppercase(Locale.getDefault()).contains(searchChars)) {
                    esw = EditSpeciesListWidget(this, null)
                    esw!!.setCountName(count.name)
                    esw!!.setCountNameG(count.name_g)
                    esw!!.setCountCode(count.code)
                    esw!!.setPSpec(count)
                    esw!!.setCountId(count.id)
                    editingSpeciesArea!!.addView(esw)
                }
            }
        } else {
            for (count in countsToEdit) {
                esw = EditSpeciesListWidget(this, null)
                esw!!.setCountName(count.name)
                esw!!.setCountNameG(count.name_g)
                esw!!.setCountCode(count.code)
                esw!!.setPSpec(count)
                esw!!.setCountId(count.id)
                editingSpeciesArea!!.addView(esw)
            }
        }
    }

    // Test for double entries and save species list
    private fun saveData(): Boolean {
        // test for double entries and save species list
        var retValue = true

        val childcount: Int = editingSpeciesArea!!.childCount // No. of species in list

        // Check for unique species names and codes
        val isDblName: String = compSpeciesNames()
        val isDblCode: String = compSpeciesCodes()

        if (isDblName == "" && isDblCode == "") {
            // For all species
            for (i in 0 until childcount) {
                val esaw = editingSpeciesArea!!.getChildAt(i) as EditSpeciesListWidget
                retValue =
                    if (isNotEmpty(esaw.getCountName()) && isNotEmpty(esaw.getCountCode())) {
                        // Update species names and codes
                        countDataSource!!.updateCountItem(
                            esaw.countId,
                            esaw.getCountName(),
                            esaw.getCountCode(),
                            esaw.getCountNameG()
                        )

                        // Change individual names and codes
                        individualsDataSource!!.updateIndivItem(
                            esaw.countId,
                            esaw.getCountName(),
                            esaw.getCountCode()
                        )
                        true
                    } else {
                        mesg = getString(R.string.isEmpt)
                        Toast.makeText(
                            applicationContext,
                            fromHtml("<font color='red'><b>$mesg</b></font>"),
                            Toast.LENGTH_LONG
                        ).show()
                        false
                    }
            }
            mesg = getString(R.string.savedChange)
            Toast.makeText(
                applicationContext,
                fromHtml("<font color='#008800'>$mesg</font>"), // green
                Toast.LENGTH_SHORT
            ).show()
        } else {
            mesg = getString(R.string.spname) + " " + isDblName +
                    " " + getString(R.string.orCode) + " " + isDblCode +
                    " " + getString(R.string.isDouble)
            Toast.makeText(
                applicationContext,
                fromHtml("<font color='red'><b>$mesg</b></font>"),
                Toast.LENGTH_LONG
            ).show()
            retValue = false
        }

        return retValue
    }
    // End of saveData()

    // Test species list for double entry
    private fun testData(): Boolean {
        var retValue = true

        // Check for unique species names
        val isDbl: String = compSpeciesNames()
        if (isDbl != "") {
            mesg = isDbl + " " + getString(R.string.isDouble) + " " + getString(R.string.duplicate)
            Toast.makeText(
                applicationContext,
                fromHtml("<font color='red'><b>$mesg</b></font>"),
                Toast.LENGTH_LONG
            ).show()
            retValue = false
        }
        return retValue
    }

    // Compare count names for duplicates and returns name of 1. duplicate found
    private fun compSpeciesNames(): String {
        var name: String
        var isDblName = ""
        cmpSpeciesNames = ArrayList()
        val childcount = editingSpeciesArea!!.childCount

        // For all CountEditWidgets
        for (i in 0 until childcount) {
            val esaw = editingSpeciesArea!!.getChildAt(i) as EditSpeciesListWidget
            name = esaw.getCountName()
            if (cmpSpeciesNames!!.contains(name)) {
                isDblName = name
                if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                    Log.i(TAG, "334, Double name = $isDblName")
                break
            }
            cmpSpeciesNames!!.add(name)
        }
        return isDblName
    }

    // Compare count codes for duplicates and returns name of 1. duplicate found
    private fun compSpeciesCodes(): String {
        var code: String
        var isDblCode = ""
        cmpSpeciesCodes = ArrayList()
        val childcount = editingSpeciesArea!!.childCount

        // For all CountEditWidgets
        for (i in 0 until childcount) {
            val esaw = editingSpeciesArea!!.getChildAt(i) as EditSpeciesListWidget
            code = esaw.getCountCode()
            if (cmpSpeciesCodes!!.contains(code)) {
                isDblCode = code
                if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                    Log.i(TAG, "356, Double code = $isDblCode")
                break
            }
            cmpSpeciesCodes!!.add(code)
        }
        return isDblCode
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.edit_section, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here.
        val id = item.itemId

        if (id == android.R.id.home) {
            finish()
            return true
        } else if (id == R.id.editSpecL) {
            if (testData()) {
                if (saveData())
                    savedCounts!!.clear()
                finish()
            }
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onPause() {
        super.onPause()

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "392, onPause")

        countDataSource!!.close()
        individualsDataSource!!.close()

        if (awakePref) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }

        editingSpeciesArea!!.clearFocus()
        editingSpeciesArea!!.removeAllViews()
        editHintArea!!.clearFocus()
        editHintArea!!.removeAllViews()
    }

    override fun onStop() {
        super.onStop()

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "411, onStop")

        editingSpeciesArea = null
        editHintArea = null
    }

    override fun onDestroy() {
        super.onDestroy()

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "421, onDestroy")
    }

    companion object {
        private const val TAG = "EditSpecListAct"

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
