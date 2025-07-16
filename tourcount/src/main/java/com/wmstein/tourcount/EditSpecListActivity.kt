package com.wmstein.tourcount

import android.content.Intent
import android.icu.lang.UCharacter.GraphemeClusterBreak.T
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.view.WindowManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NavUtils
import androidx.core.text.HtmlCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import com.wmstein.tourcount.database.CountDataSource
import com.wmstein.tourcount.database.Section
import com.wmstein.tourcount.database.SectionDataSource
import com.wmstein.tourcount.widgets.EditHeadWidget
import com.wmstein.tourcount.widgets.EditSpeciesWidget
import com.wmstein.tourcount.widgets.HintEditWidget

/*************************************************************************
 * EditSpecListActivity lets you edit the species list
 * (change head and species data).
 * EditSpecListActivity is called from CountingActivity
 * Uses EditHeadWidget.kt, EditSpeciesWidget.kt, HintEditWidget.kt,
 * activity_edit_section.xml, widget_edit_head.xml, widget_edit_count.xml.
 *
 * Based on EditProjectActivity.java by milo on 05/05/2014.
 * Adopted, modified and enhanced for TourCount by wmstein on 2016-02-18,
 * last edited in Java on 2023-07-07,
 * converted to Kotlin on 2023-07-09,
 * last edited on 2025-06-30
 */
class EditSpecListActivity : AppCompatActivity() {
    private var tourCount: TourCountApplication? = null

    // Data
    private var section: Section? = null
    private var sectionDataSource: SectionDataSource? = null
    private var countDataSource: CountDataSource? = null

    // Layouts
    private var editingCountsArea: LinearLayout? = null
    private var speciesNotesArea: LinearLayout? = null
    private var hintArea1: LinearLayout? = null

    // Widgets
    private var ehw: EditHeadWidget? = null
    private var esw: EditSpeciesWidget? = null

    // Arraylists
    private var cmpCountNames: ArrayList<String>? = null
    private var cmpCountCodes: ArrayList<String>? = null
    private var savedCounts: ArrayList<EditSpeciesWidget>? = null

    // 2 initial characters to limit selection
    private var initChars: String = ""

    // Preferences
    private var prefs = TourCountApplication.getPrefs()
    private var sortPref: String? = null
    private var brightPref = false
    private var oldname: String? = null
    private var mesg: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (MyDebug.DLOG) Log.i(TAG, "80, onCreate")

        tourCount = application as TourCountApplication

        sortPref = prefs.getString("pref_sort_sp", "none")
        brightPref = prefs.getBoolean("pref_bright", true)

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

        // Set full brightness of screen
        if (brightPref) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            val params = window.attributes
            params.screenBrightness = 1.0f
            window.attributes = params
        }

        savedCounts = ArrayList()

        speciesNotesArea = findViewById(R.id.editingNotes1Layout)
        hintArea1 = findViewById(R.id.showHintLayout)
        editingCountsArea = findViewById(R.id.editingCountsLayout)

        // Restore any edit widgets the user has added previously
        if (savedInstanceState != null) {
            if (Build.VERSION.SDK_INT < 33) {
                @Suppress("DEPRECATION")
                if (savedInstanceState.getSerializable("savedCounts") != null) {
                    @Suppress("UNCHECKED_CAST")
                    savedCounts = savedInstanceState.getSerializable("savedCounts")
                            as ArrayList<EditSpeciesWidget>?
                }
            } else {
                if (savedInstanceState.getSerializable("savedCounts", T::class.java) != null) {
                    @Suppress("UNCHECKED_CAST")
                    savedCounts =
                        savedInstanceState.getSerializable("savedCounts", T::class.java)
                                as ArrayList<EditSpeciesWidget>? // produces error without cast
                }
            }
        }

        //  Note variables to restore them
        val extras = intent.extras
        if (extras != null) {
            initChars = extras.getString("init_Chars").toString()
        }

        // Setup the data sources
        sectionDataSource = SectionDataSource(this)
        countDataSource = CountDataSource(this)

        // New onBackPressed logic
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                NavUtils.navigateUpFromSameTask(this@EditSpecListActivity)
                remove()
            }
        }
        onBackPressedDispatcher.addCallback(this, callback)
    }
    // End of onCreate()

    override fun onResume() {
        super.onResume()

        // Load preferences
        brightPref = prefs.getBoolean("pref_bright", true)

        // Set full brightness of screen
        if (brightPref) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            val params = window.attributes
            params.screenBrightness = 1.0f
            window.attributes = params
        }

        // // Build the EditSpeciesList screen
        editingCountsArea!!.removeAllViews()
        speciesNotesArea!!.removeAllViews()
        hintArea1!!.removeAllViews()

        sectionDataSource!!.open()
        countDataSource!!.open()

        supportActionBar!!.setTitle(R.string.editTitle)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        // Load the tour data
        section = sectionDataSource!!.section
        oldname = section!!.name

        // Edit the tour title
        ehw = EditHeadWidget(this, null)
        ehw!!.setSpListTitle(getString(R.string.titleEdit))
        ehw!!.spListName = oldname

        // Display the tour notes title
        ehw!!.setNotesTitle(getString(R.string.notesHere))
        ehw!!.notesName = section!!.notes
        speciesNotesArea!!.addView(ehw)

        // Display hint: Current species list
        val hew = HintEditWidget(this, null)
        if (initChars.length == 2)
            hew.setSearchE(initChars)
        else
            hew.setSearchE(getString(R.string.hintSearch))
        hintArea1!!.addView(hew)

        constructEditList()
    }
    // End of onResume()

    // Get initial 2 characters of species to select by search button
    fun getInitialChars(view: View) {
        // Read EditText searchEdit from widget_edit_hint.xml
        val searchEdit: EditText = findViewById(R.id.searchE)

        // Get the initial characters of species to select from
        initChars = searchEdit.text.toString().trim()
        if (initChars.length == 1) {
            // Reminder: "Please, 2 characters"
            searchEdit.error = getString(R.string.initCharsL)
        } else {
            initChars = initChars.substring(0,2)
            searchEdit.error = null

            if (MyDebug.DLOG) Log.d(TAG, "231, initChars: $initChars")

            // Call DummyActivity to reenter EditSectionListActivity for reduced add list
            val intent = Intent(this@EditSpecListActivity, DummyActivity::class.java)
            intent.putExtra("init_Chars", initChars)
            intent.putExtra("is_Flag", "isEdit")
            startActivity(intent)
        }
    }

    // Construct edit-species-list of contained species in the counting list
    //   and optionally reduce it by initChar selection
    private fun constructEditList() {
        // Load the sorted species data
        val counts = when (sortPref) {
            "names_alpha" -> countDataSource!!.allSpeciesSrtName
            "codes" -> countDataSource!!.allSpeciesSrtCode
            else -> countDataSource!!.allSpecies
        }

        // Display all the counts by adding them to editingCountsArea
        // Get the counting list species into their EditSpeciesWidget and add them to the view
        if (initChars.length == 2) {
            // Check name in counts for InitChars to reduce list
            var cnt = 1
            for (count in counts) {
                if (count.name?.substring(0, 2) == initChars) {
                    esw = EditSpeciesWidget(this, null)
                    esw!!.setCountName(count.name)
                    esw!!.setCountNameG(count.name_g)
                    esw!!.setCountCode(count.code)
                    esw!!.setPSpec(count)
                    esw!!.setCountId(count.id)
                    editingCountsArea!!.addView(esw)
                    cnt++
                }
            }
        } else {

            for (count in counts) {
                esw = EditSpeciesWidget(this, null)
                esw!!.setCountName(count.name)
                esw!!.setCountNameG(count.name_g)
                esw!!.setCountCode(count.code)
                esw!!.setPSpec(count)
                esw!!.setCountId(count.id)
                editingCountsArea!!.addView(esw)
            }
        }
    }

    override fun onPause() {
        super.onPause()

        // Close the data sources
        sectionDataSource!!.close()
        countDataSource!!.close()
    }

    override fun onDestroy() {
        super.onDestroy()

        speciesNotesArea = null
        editingCountsArea = null
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        // Widgets must be removed from their parent before they can be serialised.
        for (esw in savedCounts!!) {
            (esw.parent as ViewGroup).removeView(esw)
        }
        outState.putSerializable("savedCounts", savedCounts)
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
            }
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    // Test for double entries and save species list
    private fun saveData(): Boolean {
        // test for double entries and save species list
        var retValue = true

        // Add title if the user has written one
        val sectName = ehw!!.spListName
        if (MyDebug.DLOG) Log.d(TAG, "336, newName: $sectName")

        if (isNotEmpty(sectName)) {
            section!!.name = sectName
        } else {
            if (isNotEmpty(section!!.name)) {
                section!!.name = sectName
            }
        }

        // Add notes if the user has written some...
        val sectNotes = ehw!!.notesName
        if (isNotEmpty(sectNotes)) {
            section!!.notes = sectNotes
        } else {
            if (isNotEmpty(section!!.notes)) {
                section!!.notes = sectNotes
            }
        }
        sectionDataSource!!.saveSection(section!!)

        val childcount: Int = editingCountsArea!!.childCount //No. of species in list
        if (MyDebug.DLOG) Log.d(TAG, "358, childcount: $childcount")

        // Check for unique species names and codes
        val isDblName: String = compCountNames()
        val isDblCode: String = compCountCodes()
        if (isDblName == "" && isDblCode == "") {
            // For all species
            for (i in 0 until childcount) {
                val esw = editingCountsArea!!.getChildAt(i) as EditSpeciesWidget
                retValue =
                    if (isNotEmpty(esw.getCountName()) && isNotEmpty(esw.getCountCode())) {
                        if (MyDebug.DLOG) Log.d(TAG, "369, esw: "
                                    + esw.countId + ", " + esw.getCountName()
                        )

                        // Update species names and code
                        countDataSource!!.updateCountItem(
                            esw.countId,
                            esw.getCountName(),
                            esw.getCountCode(),
                            esw.getCountNameG()
                        )
                        true
                    } else {
                        mesg = getString(R.string.isempt)
                        Toast.makeText(
                            applicationContext,
                            HtmlCompat.fromHtml(
                                "<font color='red'><b>" + mesg + "</b></font>",
                                HtmlCompat.FROM_HTML_MODE_LEGACY
                            ), Toast.LENGTH_LONG
                        ).show()
                        false
                    }
            }
        } else {
            mesg = getString(R.string.spname) + " " + isDblName +
                    " " + getString(R.string.orcode) + " " + isDblCode +
                    " " + getString(R.string.isdouble)
            Toast.makeText(
                applicationContext,
                HtmlCompat.fromHtml(
                    "<font color='red'><b>" + mesg + "</b></font>",
                    HtmlCompat.FROM_HTML_MODE_LEGACY
                ), Toast.LENGTH_LONG
            ).show()
            retValue = false
        }

        if (retValue) {
            mesg = getString(R.string.sectSaving) + " " + section!!.name + "!"
            Toast.makeText(
                applicationContext,
                HtmlCompat.fromHtml(
                    "<font color='#008000'>" + mesg + "</font>",
                    HtmlCompat.FROM_HTML_MODE_LEGACY
                ), Toast.LENGTH_SHORT
            ).show()
        }
        return retValue
    }
    // End of saveData()

    // Test species list for double entry
    private fun testData(): Boolean {
        var retValue = true
        val isDbl: String

        // Check for unique species names
        isDbl = compCountNames()
        if (isDbl != "") {
            mesg = isDbl + " " + getString(R.string.isdouble) + " " + getString(R.string.duplicate)
            Toast.makeText(
                applicationContext,
                HtmlCompat.fromHtml(
                    "<font color='red'><b>" + mesg + "</b></font>",
                    HtmlCompat.FROM_HTML_MODE_LEGACY
                ), Toast.LENGTH_LONG
            ).show()
            retValue = false
        }
        return retValue
    }

    // Compare count names for duplicates and returns name of 1. duplicate found
    private fun compCountNames(): String {
        var name: String
        var isDblName = ""
        cmpCountNames = ArrayList()
        val childcount = editingCountsArea!!.childCount

        // For all CountEditWidgets
        for (i in 0 until childcount) {
            val esw = editingCountsArea!!.getChildAt(i) as EditSpeciesWidget
            name = esw.getCountName()
            if (cmpCountNames!!.contains(name)) {
                isDblName = name
                if (MyDebug.DLOG) Log.d(TAG, "455, Double name = $isDblName")
                break
            }
            cmpCountNames!!.add(name)
        }
        return isDblName
    }

    // Compare count codes for duplicates and returns name of 1. duplicate found
    private fun compCountCodes(): String {
        var code: String
        var isDblCode = ""
        cmpCountCodes = ArrayList()
        val childcount = editingCountsArea!!.childCount

        // For all CountEditWidgets
        for (i in 0 until childcount) {
            val esw = editingCountsArea!!.getChildAt(i) as EditSpeciesWidget
            code = esw.getCountCode()
            if (cmpCountCodes!!.contains(code)) {
                isDblCode = code
                if (MyDebug.DLOG) Log.d(TAG, "476, Double name = $isDblCode")
                break
            }
            cmpCountCodes!!.add(code)
        }
        return isDblCode
    }

    companion object {
        private const val TAG = "EditSpecListAct"

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
