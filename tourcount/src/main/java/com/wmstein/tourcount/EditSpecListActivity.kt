package com.wmstein.tourcount

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
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
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NavUtils
import com.google.android.material.snackbar.Snackbar
import com.wmstein.tourcount.database.CountDataSource
import com.wmstein.tourcount.database.Section
import com.wmstein.tourcount.database.SectionDataSource
import com.wmstein.tourcount.widgets.CountEditWidget
import com.wmstein.tourcount.widgets.EditHeadWidget
import com.wmstein.tourcount.widgets.HintWidget

/***************************************************************
 * EditSpecListActivity lets you edit the species list (change, delete
 * and insert new species)
 * EditSpecListActivity is called from CountingActivity
 * Uses EditHeadWidget.kt, CountEditWidget.kt, HintWidget.kt,
 * activity_edit_section.xml, widget_edit_head.xml, widget_edit_count.xml.
 * Calls AddSpeciesActivity.kt for adding a new species to the species list.
 *
 * Based on EditProjectActivity.java by milo on 05/05/2014.
 * Adopted, modified and enhanced for TourCount by wmstein on 2016-02-18,
 * last edited in Java on 2023-07-07,
 * converted to Kotlin on 2023-07-09,
 * last edited on 2024-07-02
 */
class EditSpecListActivity : AppCompatActivity() {
    private var tourCount: TourCountApplication? = null

    private var savedCounts: ArrayList<CountEditWidget>? = null
    private var countsArea: LinearLayout? = null
    private var notesArea1: LinearLayout? = null
    private var hintArea1: LinearLayout? = null
    private var ehw: EditHeadWidget? = null

    // the actual data
    private var section: Section? = null
    private var sectionDataSource: SectionDataSource? = null
    private var countDataSource: CountDataSource? = null
    private var viewMarkedForDelete: View? = null
    private var idToDelete = 0
    private var bMap: Bitmap? = null
    private var bg: BitmapDrawable? = null
    private var cmpCountNames: ArrayList<String>? = null
    private var cmpCountCodes: ArrayList<String>? = null

    // Preferences
    private var prefs = TourCountApplication.getPrefs()
    private var sortPref: String? = null
    private var brightPref = false

    private var oldname: String? = null

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        tourCount = application as TourCountApplication

        sortPref = prefs.getString("pref_sort_sp", "none")
        brightPref = prefs.getBoolean("pref_bright", true)

        setContentView(R.layout.activity_edit_section)

        // Set full brightness of screen
        if (brightPref) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            val params = window.attributes
            params.screenBrightness = 1.0f
            window.attributes = params
        }

        savedCounts = ArrayList()
        notesArea1 = findViewById(R.id.editingNotes1Layout)
        hintArea1 = findViewById(R.id.showHintLayout)
        countsArea = findViewById(R.id.editingCountsLayout)

        // Restore any edit widgets the user has added previously
        if (savedInstanceState != null) {
            if (Build.VERSION.SDK_INT < 33) {
                @Suppress("DEPRECATION")
                if (savedInstanceState.getSerializable("savedCounts") != null) {
                    @Suppress("UNCHECKED_CAST")
                    savedCounts = savedInstanceState.getSerializable("savedCounts")
                            as ArrayList<CountEditWidget>?
                }
            } else {
                if (savedInstanceState.getSerializable("savedCounts", T::class.java) != null) {
                    @Suppress("UNCHECKED_CAST")
                    savedCounts =
                        savedInstanceState.getSerializable("savedCounts", T::class.java)
                                as ArrayList<CountEditWidget>?
                }
            }
        }

        val countingScreen = findViewById<LinearLayout>(R.id.editSect)
        bMap = tourCount!!.decodeBitmap(
            R.drawable.kbackground,
            tourCount!!.width, tourCount!!.height
        )
        bg = BitmapDrawable(countingScreen.resources, bMap)
        countingScreen.background = bg

        // setup the data sources
        sectionDataSource = SectionDataSource(this)
        countDataSource = CountDataSource(this)

        // new onBackPressed logic
        if (Build.VERSION.SDK_INT >= 33) {
            onBackPressedDispatcher.addCallback(object :
                OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (testData()) {
                        savedCounts!!.clear()

                        NavUtils.navigateUpFromSameTask(this@EditSpecListActivity)
                    } else return
                }
            })
        }
    }
    // end of onCreate

    override fun onResume() {
        super.onResume()

        prefs = TourCountApplication.getPrefs()
        brightPref = prefs.getBoolean("pref_bright", true)

        // Set full brightness of screen
        if (brightPref) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            val params = window.attributes
            params.screenBrightness = 1.0f
            window.attributes = params
        }

        // clear any existing views
        countsArea!!.removeAllViews()
        notesArea1!!.removeAllViews()
        hintArea1!!.removeAllViews()

        sectionDataSource!!.open()
        countDataSource!!.open()

        // load the sections data
        section = sectionDataSource!!.section
        oldname = section!!.name
        try {
            supportActionBar!!.title = oldname
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        } catch (e: NullPointerException) {
            if (MyDebug.LOG) Log.e(TAG, "178, NullPointerException: No section name!")
        }

        // display the section title
        ehw = EditHeadWidget(this, null)

        ehw!!.setSpListTitle(getString(R.string.titleEdit))
        ehw!!.spListName = oldname

        // display the section notes title
        ehw!!.setNotesTitle(getString(R.string.notesHere))
        ehw!!.notesName = section!!.notes
        notesArea1!!.addView(ehw)

        // display hint current species list:
        val nw = HintWidget(this, null)
        nw.setHint1(getString(R.string.presentSpecs))
        hintArea1!!.addView(nw)

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
            countsArea!!.addView(cew)
        }
        for (cew in savedCounts!!) {
            countsArea!!.addView(cew)
        }
    } // end of onResume

    override fun onPause() {
        super.onPause()

        // close the data sources
        sectionDataSource!!.close()
        countDataSource!!.close()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        // Widgets must be removed from their parent before they can be serialised.
        for (cew in savedCounts!!) {
            (cew.parent as ViewGroup).removeView(cew)
        }
        outState.putSerializable("savedCounts", savedCounts)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.edit_section, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        if (!testData()) return true

        val id = item.itemId
        if (id == android.R.id.home) {
            savedCounts!!.clear()
            val intent = NavUtils.getParentActivityIntent(this)!!
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            NavUtils.navigateUpTo(this, intent)
        } else if (id == R.id.menuSaveExit) {
            if (saveData()) {
                savedCounts!!.clear()
                super.finish()
            }
        } else if (id == R.id.newCount) {
            newCount(view = null)
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    // Compare count names for duplicates and returns name of 1. duplicate found
    private fun compCountNames(): String {
        var name: String
        var isDblName = ""
        cmpCountNames = ArrayList()
        val childcount = countsArea!!.childCount

        // for all CountEditWidgets
        for (i in 0 until childcount) {
            val cew = countsArea!!.getChildAt(i) as CountEditWidget
            name = cew.getCountName()
            if (cmpCountNames!!.contains(name)) {
                isDblName = name
                if (MyDebug.LOG) Log.d(TAG, "281, Double name = $isDblName")
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
        val childcount = countsArea!!.childCount

        // for all CountEditWidgets
        for (i in 0 until childcount) {
            val cew = countsArea!!.getChildAt(i) as CountEditWidget
            code = cew.getCountCode()
            if (cmpCountCodes!!.contains(code)) {
                isDblCode = code
                if (MyDebug.LOG) Log.d(TAG, "302, Double name = $isDblCode")
                break
            }
            cmpCountCodes!!.add(code)
        }
        return isDblCode
    }

    fun saveAndExit(view: View) {
        if (saveData()) {
            savedCounts!!.clear()
            super.finish()
        }
    }

    // test for double entries and save species list
    private fun saveData(): Boolean {
        // test for double entries and save species list
        var retValue = true

        // add title if the user has written one...
        val sectName = ehw!!.spListName
        if (MyDebug.LOG) Log.d(TAG, "324, newName: $sectName")
        if (isNotEmpty(sectName)) {
            section!!.name = sectName
        } else {
            if (isNotEmpty(section!!.name)) {
                section!!.name = sectName
            }
        }

        // add notes if the user has written some...
        val sectNotes = ehw!!.notesName
        if (isNotEmpty(sectNotes)) {
            section!!.notes = sectNotes
        } else {
            if (isNotEmpty(section!!.notes)) {
                section!!.notes = sectNotes
            }
        }

        sectionDataSource!!.saveSection(section!!)

        val isDblName: String
        val isDblCode: String
        val childcount: Int = countsArea!!.childCount //No. of species in list
        if (MyDebug.LOG) Log.d(TAG, "348, childcount: $childcount")

        // check for unique species names and codes
        isDblName = compCountNames()
        isDblCode = compCountCodes()
        if (isDblName == "" && isDblCode == "") {
            // do for all species
            for (i in 0 until childcount) {
                val cew = countsArea!!.getChildAt(i) as CountEditWidget
                retValue =
                    if (isNotEmpty(cew.getCountName()) && isNotEmpty(cew.getCountCode())) {
                        if (MyDebug.LOG) Log.d(TAG, "359, cew: "
                                    + cew.countId + ", " + cew.getCountName())

                        //updates species name and code
                        countDataSource!!.updateCountName(
                            cew.countId,
                            cew.getCountName(),
                            cew.getCountCode(),
                            cew.getCountNameG()
                        )
                        true
                    } else {
                        showSnackbarRed(getString(R.string.isempt))
                        false
                    }
            }
        } else {
            showSnackbarRed(
                getString(R.string.spname) + " " + isDblName + " " + getString(R.string.orcode) + " " + isDblCode + " "
                        + getString(R.string.isdouble)
            )
                retValue = false
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
        isDbl = compCountNames()
        if (isDbl != "") {
            showSnackbarRed(
                isDbl + " " + getString(R.string.isdouble) + " " + getString(R.string.duplicate)
            )
            retValue = false
        }
        return retValue
    }

    // Start AddSpeciesActivity to add new species to the species list
    fun newCount(view: View?) {
        Toast.makeText(applicationContext, getString(R.string.wait), Toast.LENGTH_SHORT)
            .show() // a Snackbar here comes incomplete

        // pause for 100 msec to show toast
        Handler(Looper.getMainLooper()).postDelayed({
            // add title if the user has written one...
            val sectName = ehw!!.spListName
            if (isNotEmpty(sectName)) {
                section!!.name = sectName
            } else {
                if (isNotEmpty(section!!.name)) {
                    section!!.name = sectName
                }
            }

            // add notes if the user has written some...
            val sectNotes = ehw!!.notesName
            if (isNotEmpty(sectNotes)) {
                section!!.notes = sectNotes
            } else {
                if (isNotEmpty(section!!.notes)) {
                    section!!.notes = sectNotes
                }
            }

            sectionDataSource!!.saveSection(section!!)

            // save changes so far
            if (saveData()) {
                savedCounts!!.clear()
            }

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
            countsArea!!.removeView(view.parent.parent.parent as CountEditWidget)
        } else {
            val areYouSure = AlertDialog.Builder(this)
            areYouSure.setTitle(getString(R.string.deleteCount))
            areYouSure.setMessage(getString(R.string.reallyDeleteCount))
            areYouSure.setPositiveButton(R.string.yesDeleteIt) { _: DialogInterface?, _: Int ->
                // go ahead for the delete
                countDataSource!!.deleteCountById(idToDelete)
                countsArea!!.removeView(viewMarkedForDelete!!.parent.parent.parent as CountEditWidget)
            }
            areYouSure.setNegativeButton(R.string.noCancel) { _: DialogInterface?, _: Int -> }
            areYouSure.show()
        }
    }

    // catch back button for plausi test
    @Deprecated("Deprecated in Java")
    @SuppressLint("ApplySharedPref", "MissingSuperCall")
    override fun onBackPressed() {
        if (testData()) {
            savedCounts!!.clear()
            countDataSource!!.close()
            sectionDataSource!!.close()

            NavUtils.navigateUpFromSameTask(this)
        } else return
        @Suppress("DEPRECATION")
        super.onBackPressed()
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
