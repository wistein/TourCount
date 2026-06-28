package com.wmstein.tourcount

import android.annotation.SuppressLint
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
import androidx.core.content.edit
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams

import com.wmstein.tourcount.Utils.fromHtml
import com.wmstein.tourcount.database.CountDataSource
import com.wmstein.tourcount.widgets.AddSpeciesHintWidget
import com.wmstein.tourcount.widgets.AddSpeciesWidget

import java.util.Locale

/**********************************************************************
 * AddSpeciesActivity lets you insert new species into the species list
 * AddSpeciesActivity is called from CountingActivity
 * Uses AddSpeciesWidget.kt, widget_add_spec.xml.
 *
 * The sorting order of the species to add cannot be changed, as it is
 * determined by 3 interdependent and correlated arrays in arrays.xml
 *
 * Created for TourCount by wmstein on 2019-04-12,
 * last edited in Java on 2023-05-13,
 * converted to Kotlin on 2023-05-26
 * last edited on 2026-06-28
 */
class AddSpeciesActivity : AppCompatActivity() {
    private var addArea: LinearLayout? = null
    private var addHintArea: LinearLayout? = null

    // Count data
    private var countDataSource: CountDataSource? = null

    // ID-list of not yet included species
    private lateinit var remainingIdArrayList: Array<String?>

    // 3 ArrayLists (for names, namesL and codes) of all species from arrays.xml
    // will get reduced to lists of not yet included Species
    private var namesCompleteArrayList: ArrayList<String>? = null
    private var namesReducedArrayList: ArrayList<String>? = null
    private var namesLCompleteArrayList: ArrayList<String>? = null
    private var namesLReducedArrayList: ArrayList<String>? = null
    private var codesCompleteArrayList: ArrayList<String?>? = null
    private var codesReducedArrayList: ArrayList<String?>? = null

    private var specName = ""
    private var specNameG = ""
    private var specCode = ""
    private var posSpec = 0

    // 2 initial characters to limit selection
    private var searchChars = ""

    // List of selected species to add
    private var listToAdd: ArrayList<AddSpeciesWidget>? = null

    // preferences
    private var prefs = TourCountApplication.getPrefs()
    private var brightPref = false
    private var awakePref = false
    private var dataLanguage = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "88, onCreate")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) // SDK 35+
        {
            enableEdgeToEdge()
        }
        setContentView(R.layout.activity_add_species)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.addSpec))
        { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            // Apply the insets as a margin to the view. You can also update the view padding
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

        // Get value from re-entering respective getAddSearchChars()
        val extras = intent.extras
        if (extras != null)
            searchChars = extras.getString("search_Chars").toString()

        listToAdd = ArrayList()

        addHintArea = findViewById(R.id.showHintAddLayout)
        addArea = findViewById(R.id.addSpecLayout)

        // Set up the data sources
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
            Log.i(TAG, "141, onResume")

        // Load preferences
        brightPref = prefs.getBoolean("pref_bright", true)
        awakePref = prefs.getBoolean("pref_awake", true)
        dataLanguage = prefs.getString("pref_sel_data_lang", "").toString()

        // Set full brightness of screen
        if (brightPref) {
            val params = window.attributes
            params.screenBrightness = 1.0f
            window.attributes = params
        }

        if (awakePref)
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        countDataSource!!.open()

        // Load complete species ArrayList from arrays.xml (lists are sorted by code)
        namesCompleteArrayList = ArrayList(listOf(*resources.getStringArray(R.array.selSpecs)))
        codesCompleteArrayList = ArrayList(listOf(*resources.getStringArray(R.array.selCodes)))
        when (dataLanguage) {
            "de" -> namesLCompleteArrayList = ArrayList(listOf(*resources.getStringArray(R.array.selSpecs_de)))
            "en" -> namesLCompleteArrayList = ArrayList(listOf(*resources.getStringArray(R.array.selSpecs_en)))
            "fr" -> namesLCompleteArrayList = ArrayList(listOf(*resources.getStringArray(R.array.selSpecs_fr)))
            "it" -> namesLCompleteArrayList = ArrayList(listOf(*resources.getStringArray(R.array.selSpecs_it)))
            "es" -> namesLCompleteArrayList = ArrayList(listOf(*resources.getStringArray(R.array.selSpecs_es)))
            else -> {
                namesLCompleteArrayList = ArrayList(listOf(*resources.getStringArray(R.array.selSpecs_de)))
                val mesg = getString(R.string.specsCommonLang)
                Toast.makeText(
                    this,
                    fromHtml("<font color='red'><b>$mesg</b></font>"),
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        // clear any existing views
        addArea!!.removeAllViews()
        addHintArea!!.removeAllViews()

        supportActionBar!!.setTitle(R.string.addTitle)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        // Display hint: "Search string" or editable searchChars
        val haw = AddSpeciesHintWidget(this, null)
        if (searchChars.length >= 2)
            haw.setSearchA1(searchChars)
        else
            haw.setSearchA(getString(R.string.hintSearch))
        addHintArea!!.addView(haw)

        // Toast hint for duration of list calculation
        val mesg = getString(R.string.wait)
        Toast.makeText(
            applicationContext,
            fromHtml("<font color='blue'>$mesg</font>"),
            Toast.LENGTH_SHORT
        ).show()

        // Delay necessary for Toast to show and not break activity
        Handler(Looper.getMainLooper()).postDelayed({
            constructAddList()
        }, 100)
    }
    // End of onResume()

    // Get 2 or more characters of species to select by search button
    // Parameter view is necessary for function call
    fun getAddSearchChars(view: View) {
        // Read EditText searchAdd from widget_add_hint.xml
        val searchAdd: EditText = findViewById(R.id.searchA)
        searchAdd.findFocus()

        // Get the search characters to build a species list to select from
        searchChars = searchAdd.text.toString().trim()
        if (searchChars.length == 1) {
            // Reminder: "Please, >1 characters"
            searchAdd.error = getString(R.string.searchCharsL)
        } else {
            searchAdd.error = null
            searchAdd.clearFocus()
            searchAdd.invalidate()

            // Re-enter AddSpeciesActivity for the reduced species-add-list
            val intent = Intent(this@AddSpeciesActivity, AddSpeciesActivity::class.java)
            intent.putExtra("search_Chars", searchChars)
            intent.flags = FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
            finish()
        }
    }

    // Construct an add-species-list of not already contained species in the current counting list
    //   and optionally reduce it by searchChars selection
    private fun constructAddList() {
        // 1. Build list of codes of contained species in counting list
        val specCodesContainedList = ArrayList<String?>()

        // Get sorted species of the counting list
        val countsCodesSortList = countDataSource!!.allSpeciesSrtCode

        // Build ArrayList of codes of already contained species
        for (count in countsCodesSortList) {
            specCodesContainedList.add(count.code)
        }

        // 2. Build lists of all yet missing species
        val specCodesContainedListSize = specCodesContainedList.size

        // Reduce complete arraylists for already contained species
        for (i in 0 until specCodesContainedListSize) {
            if (codesCompleteArrayList!!.contains(specCodesContainedList[i])) {
                // Remove species with specCode[i] from missing species lists.
                // Prerequisites: Exactly correlated arrays of selCodes, selSpecs and selSpecs_l
                specCode = specCodesContainedList[i]!!
                posSpec = codesCompleteArrayList!!.indexOf(specCode)

                namesCompleteArrayList!!.removeAt(posSpec)
                namesLCompleteArrayList!!.removeAt(posSpec)
                codesCompleteArrayList!!.removeAt(posSpec)
            }
        }

        // Copy ...CompleteArrayLists to ...ReducedArrayLists
        namesReducedArrayList = namesCompleteArrayList
        namesLReducedArrayList = namesLCompleteArrayList
        codesReducedArrayList = codesCompleteArrayList

        // 3. Further, optionally reduce the complete Arraylists for all but searchChars species
        if (searchChars.length >= 2) {
            searchChars = searchChars.uppercase(Locale.getDefault()) //Compare searchChars in uppercase

            // Empty ...ReducedArrayLists
            namesReducedArrayList = arrayListOf()
            namesLReducedArrayList = arrayListOf()
            codesReducedArrayList = arrayListOf()

            for (i in 0 until namesCompleteArrayList!!.size) {
                if (namesCompleteArrayList!![i].uppercase(Locale.getDefault()).contains(searchChars)) {
                    specName = namesCompleteArrayList!![i]
                    specNameG = namesLCompleteArrayList!![i]
                    specCode = codesCompleteArrayList!![i]!!

                    // Assemble remaining ReducedArrayLists for all Species with searchChars
                    namesReducedArrayList!!.add(specName)
                    namesLReducedArrayList!!.add(specNameG)
                    codesReducedArrayList!!.add(specCode)
                }
            }
        }

        // Create remainingIdArrayList for all remaining species of codesReducedArrayList
        remainingIdArrayList = arrayOfNulls(codesReducedArrayList!!.size)
        var i = 0
        while (i < codesReducedArrayList!!.size) {
            remainingIdArrayList[i] = (i + 1).toString()
            i++
        }

        // Load the data of the remaining species into the AddSpeciesWidget
        i = 0
        while (i < codesReducedArrayList!!.size) {
            val asw = AddSpeciesWidget(this, null)
            asw.setSpecName(namesReducedArrayList!![i])
            asw.setSpecNameG(namesLReducedArrayList!![i])
            asw.setSpecCode(codesReducedArrayList!![i]!!)
            asw.setPSpec(codesReducedArrayList!![i]!!)
            asw.setSpecId(remainingIdArrayList[i]!!)
            asw.setMarkSpec(false)
            addArea!!.addView(asw)
            i++
        }

        prefs.edit(commit = true) {
            putString("is_Add", "")
        }
    }

    // Mark the selected species and consider it for the species counts list
    fun checkBoxAdd(view: View) {
        val idToAdd = view.tag as Int
        val asw = addArea!!.getChildAt(idToAdd) as AddSpeciesWidget

        val checked = asw.getMarkSpec() // return boolean isChecked

        // Put species on the add-list
        if (checked) {
            listToAdd!!.add(asw)
        } else {
            // Remove species previously added from the add-list
            listToAdd!!.remove(asw)
        }
    }

    // Update COUNT_TABLE with the species to add
    @SuppressLint("ApplySharedPref")
    private fun addSpecs() {
        // Append the species from the add-list to the COUNT_TABLE
        var i = 0
        while (i < listToAdd!!.size) {
            specName = listToAdd!![i].getSpecName()
            specCode = listToAdd!![i].getSpecCode()
            specNameG = listToAdd!![i].getSpecNameG()
            try {
                countDataSource!!.createCount(specName, specCode, specNameG)
            } catch (_: Exception) {
                // nothing
            }
            i++
        }

        // Re-index and sort the counts table for code
        countDataSource!!.sortCounts()

        // Store code of last selected species in sharedPreferences
        //  for Spinner in CountingActivity
        if (i > 0) {
            prefs.edit(commit = true) {
                putString("new_spec_code", specCode)
            }
        }

        // Re-enter AddSpeciesActivity to rebuild the species list
        val intent = Intent(this@AddSpeciesActivity, AddSpeciesActivity::class.java)
        intent.putExtra("search_Chars", "")
        intent.flags = FLAG_ACTIVITY_CLEAR_TOP
        startActivity(intent)
        finish()
    }

    // Inflate the menu; this adds items to the action bar if it is present.
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.add_species, menu)
        return true
    }

    // Handle action bar item clicks here.
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here
        val id = item.itemId
        if (id == android.R.id.home) {
            finish()
            return true
        } else if (id == R.id.addSpecs) {
            if (listToAdd!!.isNotEmpty())
                addSpecs()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onPause() {
        super.onPause()

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "400, onPause")

        countDataSource!!.close()

        if (awakePref) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

            addArea!!.clearFocus()
            addArea!!.removeAllViews()
            addHintArea!!.clearFocus()
            addHintArea!!.removeAllViews()
        }
    }

    override fun onStop() {
        super.onStop()

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "418, onStop")

        addArea = null
        addHintArea = null
    }

    override fun onDestroy() {
        super.onDestroy()

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "428, onDestroy")
    }

    companion object {
        private const val TAG = "AddSpecAct"
    }

}
