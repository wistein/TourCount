package com.wmstein.tourcount

import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NavUtils
import com.wmstein.tourcount.database.Count
import com.wmstein.tourcount.database.CountDataSource
import com.wmstein.tourcount.widgets.SpeciesAddWidget

/************************************************************************
 * AddSpeciesActivity lets you insert a new species into the species list
 * AddSpeciesActivity is called from EditSpecListActivity
 * Uses SpeciesAddWidget.java, widget_add_spec.xml.
 *
 * The sorting order of the species to add cannot be changed, as it is determined
 * by 3 interdependent and correlated arrays in arrays.xml
 *
 * Created for TourCount by wmstein on 2019-04-12,
 * last edited in Java on 2023-05-13
 * converted to Kotlin on 2023-07-06
 * last edited on 2023-07-13
 */
class AddSpeciesActivity : AppCompatActivity(), OnSharedPreferenceChangeListener {
    private var tourCount: TourCountApplication? = null

    private var add_area: LinearLayout? = null

    // the actual count data
    private var countDataSource: CountDataSource? = null

    // Id list of missing species
    private lateinit var idArray: Array<String?>

    // complete ArrayLists of species
    private var namesCompleteArrayList: ArrayList<String>? = null
    private var namesGCompleteArrayList: ArrayList<String>? = null
    private var codesCompleteArrayList: ArrayList<String?>? = null
    private var specName: String? = null
    private var specCode: String? = null
    private var specNameG : String? = null // selected species
    private var bMap: Bitmap? = null
    private var bg: BitmapDrawable? = null

    // preferences
    private var prefs = TourCountApplication.getPrefs()
    private var brightPref = false

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        tourCount = application as TourCountApplication
        prefs.registerOnSharedPreferenceChangeListener(this)
        brightPref = prefs.getBoolean("pref_bright", true)

        setContentView(R.layout.activity_add_species)
        val add_screen = findViewById<ScrollView>(R.id.addScreen)

        // Set full brightness of screen
        if (brightPref) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            val params = window.attributes
            params.screenBrightness = 1.0f
            window.attributes = params
        }
        bMap = tourCount!!.decodeBitmap(R.drawable.abackground, tourCount!!.width, tourCount!!.height)
        bg = BitmapDrawable(add_screen.resources, bMap)
        add_screen.background = bg
        add_area = findViewById(R.id.addSpecLayout)

        // Load complete species ArrayList from arrays.xml (lists are sorted by code)
        namesCompleteArrayList =
            ArrayList(listOf(*resources.getStringArray(R.array.selSpecs)))
        namesGCompleteArrayList =
            ArrayList(listOf(*resources.getStringArray(R.array.selSpecs_g)))
        codesCompleteArrayList =
            ArrayList(listOf(*resources.getStringArray(R.array.selCodes)))
    }

    override fun onResume() {
        super.onResume()

        prefs = TourCountApplication.getPrefs()
        prefs.registerOnSharedPreferenceChangeListener(this)
        brightPref = prefs.getBoolean("pref_bright", true)

        // Set full brightness of screen
        if (brightPref) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            val params = window.attributes
            params.screenBrightness = 1.0f
            window.attributes = params
        }

        // clear any existing views
        add_area!!.removeAllViews()

        // setup the data sources
        countDataSource = CountDataSource(this)
        countDataSource!!.open()
        supportActionBar!!.setTitle(R.string.addTitle)

        // get the list of only new species not already contained in the species counting list

        // code list of contained species
        val specCodesContainedList = ArrayList<String?>()

        // get species of the counting list
        val counts: List<Count> = countDataSource!!.allSpeciesSrtCode

        // build code ArrayList of already contained species
        for (count in counts) {
            specCodesContainedList.add(count.code)
        }

        // build lists of missing species
        val specCodesContainedListSize = specCodesContainedList.size
        var posSpec: Int

        // for already contained species reduce complete arraylists
        for (i in 0 until specCodesContainedListSize) {
            if (codesCompleteArrayList!!.contains(specCodesContainedList[i])) {
                // Remove species with code x from missing species lists.
                // Prerequisites: exactly correlated arrays of selCodes, selSpecs and selSpecs_g
                //   for all localisations
                specCode = specCodesContainedList[i]
                posSpec = codesCompleteArrayList!!.indexOf(specCode)
                namesCompleteArrayList!!.removeAt(posSpec)
                namesGCompleteArrayList!!.removeAt(posSpec)
                codesCompleteArrayList!!.remove(specCode)
            }
        }
        idArray = setIdsSelSpecs(codesCompleteArrayList) // create idArray from codeArray

        // load the species data into the widgets
        var i = 0
        while (i < codesCompleteArrayList!!.size) {
            val saw = SpeciesAddWidget(this, null)
            saw.setSpecName(namesCompleteArrayList!![i])
            saw.setSpecNameG(namesGCompleteArrayList!![i])
            saw.setSpecCode(codesCompleteArrayList!![i])
            saw.setPSpec(codesCompleteArrayList!![i]!!)
            saw.setSpecId(idArray[i]!!)
            add_area!!.addView(saw)
            i++
        }
    } // end of Resume

    // create idArray from codeArray
    private fun setIdsSelSpecs(speccodesm: ArrayList<String?>?): Array<String?> {
        idArray = arrayOfNulls(speccodesm!!.size)
        var i = 0
        while (i < speccodesm.size) {
            idArray[i] = (i + 1).toString()
            i++
        }
        return idArray
    }

    override fun onPause() {
        super.onPause()

        countDataSource!!.close()
    }

    fun saveAndExit(view: View) {
        if (saveData(view)) {
            super.finish()
        }
    }

    private fun saveData(view: View): Boolean {
        // save added species to species list
        var retValue = true
        val idToAdd = view.tag as Int
        val saw1 = add_area!!.getChildAt(idToAdd) as SpeciesAddWidget
        specName = saw1.getSpecName()
        specCode = saw1.getSpecCode()
        specNameG = saw1.getSpecNameG()
        try {
            countDataSource!!.createCount(specName, specCode, specNameG)
        } catch (e: Exception) {

            retValue = false
        }
        countDataSource!!.close()
        return retValue
    }

    // Add the selected species to the species list
    fun addCount(view: View) {
        if (saveData(view)) {
            super.finish()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.add_species, menu)
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
        }
        return super.onOptionsItemSelected(item)
    }

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onSharedPreferenceChanged(prefs: SharedPreferences, key: String) {
        val add_screen = findViewById<ScrollView>(R.id.addScreen)
        prefs.registerOnSharedPreferenceChangeListener(this)
        brightPref = prefs.getBoolean("pref_bright", true)
        bMap = tourCount!!.decodeBitmap(R.drawable.abackground, tourCount!!.width, tourCount!!.height)
        add_screen.background = null
        bg = BitmapDrawable(add_screen.resources, bMap)
        add_screen.background = bg
    }

}
